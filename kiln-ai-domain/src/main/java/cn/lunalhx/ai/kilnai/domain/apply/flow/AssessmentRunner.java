package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.CommittedEvaluationResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalEquivalenceCheck;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractAudit;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentDecider;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.List;
import java.util.Objects;

/**
 * Runs the isolated response assessment for one closed attempt: builds the
 * shared context from the same raw and learner-confirmed inputs, invokes
 * Assessment, adds independent Response Verification only when the
 * deterministic Mathematical Equivalence Check returns Cannot Decide, and
 * returns the closed deterministic AssessmentOutcome. It never accepts
 * Evidence or mutates Flow State.
 */
public final class AssessmentRunner {

    private final AssessmentPort assessmentPort;
    private final ResponseVerificationPort verificationPort;
    private final ArtifactStore artifactStore;

    public AssessmentRunner(
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            ArtifactStore artifactStore
    ) {
        this.assessmentPort = Objects.requireNonNull(assessmentPort, "assessmentPort must not be null");
        this.verificationPort = Objects.requireNonNull(verificationPort, "verificationPort must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
    }

    AssessmentOutcome run(ModelProfile profile, TaskAttempt closedAttempt, TaskPackage taskPackage) {
        Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
        if (closedAttempt.purpose() == AttemptPurpose.DIAGNOSTIC) {
            throw new IllegalArgumentException("Diagnostic assessment requires the explicit Diagnostic policy");
        }
        return run(profile, closedAttempt, taskPackage, false);
    }

    /**
     * Runs the current Diagnostic policy. The Blueprint must explicitly opt
     * into corroborated rationale rescue and declare the deterministic
     * Mathematical Equivalence Check before this path can be used.
     */
    public AssessmentOutcome runDiagnostic(
            ModelProfile profile,
            TaskAttempt closedAttempt,
            TaskPackage taskPackage,
            ApplyExecutionContext.TaskBlueprint blueprint
    ) {
        Objects.requireNonNull(blueprint, "blueprint must not be null");
        if (blueprint.attemptPurpose() != AttemptPurpose.DIAGNOSTIC
                || !ApplyExecutionContext.TaskBlueprint.DIAGNOSTIC_PRIMARY_OR_CORROBORATED_RATIONALE_POLICY
                .equals(blueprint.assessmentPolicyRef())
                || !ApplyExecutionContext.TaskBlueprint.MATHEMATICAL_EQUIVALENCE_CHECK
                .equals(blueprint.trustedPrimaryAnswerCheckRef())) {
            throw new IllegalArgumentException(
                    "Diagnostic requires the primary-or-corroborated-rationale policy and a Trusted Primary-Answer Check");
        }
        return run(profile, closedAttempt, taskPackage, true);
    }

    private AssessmentOutcome run(
            ModelProfile profile,
            TaskAttempt closedAttempt,
            TaskPackage taskPackage,
            boolean diagnosticPrimaryRouting
    ) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
        Objects.requireNonNull(taskPackage, "taskPackage must not be null");
        TaskSubmission submission = Objects.requireNonNull(closedAttempt.submission(),
                "a closed attempt must retain its submission");
        String expected = taskPackage.privateAssessorProjection().canonicalExpectedAnswer().expression();
        List<String> variables = taskPackage.privateAssessorProjection().canonicalExpectedAnswer().variables();
        String confirmed = submission.finalDerivative().confirmedCanonical();
        EquivalenceOutcome deterministic = MathematicalEquivalenceCheck.check(confirmed, expected, variables);
        ResponseAssessmentContext context = new ResponseAssessmentContext(
                taskPackage.learnerProjection().taskText(),
                expected,
                confirmed,
                submission.finalDerivative().raw(),
                submission.rationale() == null ? "" : submission.rationale(),
                closedAttempt.purpose(),
                deterministic);

        if (outcomeIsDeterminedWithoutModel(closedAttempt.purpose(), deterministic)) {
            return ResponseAssessmentDecider.decide(context, null, null);
        }
        if (diagnosticPrimaryRouting
                && deterministic == EquivalenceOutcome.PROVEN_NOT_EQUIVALENT
                && isMissingRationale(submission.rationale())) {
            return new AssessmentOutcome.Failed(null, null);
        }
        ResponseAssessment assessment = loadOrCommit(
                closedAttempt, context, CommittedEvaluationResult.RESPONSE_ASSESSMENT,
                ModelContractAudit.ASSESSMENT,
                () -> assessmentPort.assess(profile, context));
        if (assessment == null) {
            return new AssessmentOutcome.Inconclusive(null, null);
        }
        ResponseAssessment verification = null;
        if (deterministic == EquivalenceOutcome.CANNOT_DECIDE) {
            verification = loadOrCommit(
                    closedAttempt, context, CommittedEvaluationResult.RESPONSE_VERIFICATION,
                    ModelContractAudit.RESPONSE_VERIFICATION,
                    () -> verificationPort.verify(profile, context));
            if (verification == null) {
                return new AssessmentOutcome.Inconclusive(assessment, null);
            }
        }
        return ResponseAssessmentDecider.decide(context, assessment, verification);
    }

    private static boolean isMissingRationale(String rationale) {
        return rationale == null || rationale.isBlank();
    }

    private ResponseAssessment loadOrCommit(
            TaskAttempt closedAttempt,
            ResponseAssessmentContext context,
            String responsibility,
            String auditResponsibility,
            java.util.function.Supplier<ResponseAssessment> evaluator
    ) {
        return artifactStore.findCommittedEvaluationResult(
                        closedAttempt.attemptId(), responsibility, CommittedEvaluationResult.EVALUATION_VERSION)
                .map(committed -> ResponseAssessment.parse(committed.resultPayload()))
                .orElseGet(() -> {
                    ResponseAssessment evaluated = ModelContractRepair.once(
                            evaluator,
                            artifactStore, null, closedAttempt.attemptId(), closedAttempt.taskPackageId(),
                            auditResponsibility);
                    if (evaluated == null) {
                        return null;
                    }
                    CommittedEvaluationResult committed = artifactStore.saveOrReturnCommittedEvaluationResult(
                            closedAttempt.attemptId(), responsibility,
                            CommittedEvaluationResult.EVALUATION_VERSION,
                            evaluated.schema(), ApplyJson.writeContract(evaluated));
                    return ResponseAssessment.parse(committed.resultPayload());
                });
    }

    private static boolean outcomeIsDeterminedWithoutModel(AttemptPurpose purpose, EquivalenceOutcome deterministic) {
        return purpose == AttemptPurpose.DIAGNOSTIC && deterministic == EquivalenceOutcome.PROVEN_EQUIVALENT
                || purpose == AttemptPurpose.INDEPENDENT_TEST
                && deterministic == EquivalenceOutcome.PROVEN_NOT_EQUIVALENT;
    }

    /**
     * The sanitized closed error-dimension reason codes carried by one
     * AssessmentOutcome. Only the closed reason-code lists of the isolated
     * judgments are exposed — never the raw learner answer, the expected
     * answer, or any hidden reasoning — so they are safe to project into the
     * Pedagogy Agent's Feedback Facts.
     */
    public static List<String> errorDimensions(AssessmentOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome must not be null");
        List<String> dimensions = new java.util.ArrayList<>();
        ResponseAssessment assessment = switch (outcome) {
            case AssessmentOutcome.Passed passed -> passed.assessment();
            case AssessmentOutcome.Failed failed -> failed.assessment();
            case AssessmentOutcome.Inconclusive inconclusive -> inconclusive.assessment();
            case AssessmentOutcome.Blocked blocked -> blocked.assessment();
        };
        if (assessment != null) {
            dimensions.addAll(assessment.reasonCodes());
        }
        ResponseAssessment verification = switch (outcome) {
            case AssessmentOutcome.Passed passed -> passed.verification();
            case AssessmentOutcome.Failed failed -> failed.verification();
            case AssessmentOutcome.Inconclusive inconclusive -> inconclusive.verification();
            case AssessmentOutcome.Blocked blocked -> blocked.verification();
        };
        if (verification != null) {
            dimensions.addAll(verification.reasonCodes());
        }
        return List.copyOf(dimensions);
    }

}
