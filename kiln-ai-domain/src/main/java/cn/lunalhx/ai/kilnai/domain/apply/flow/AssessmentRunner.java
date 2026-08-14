package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalEquivalenceCheck;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
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
import java.util.UUID;

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

    public AssessmentRunner(AssessmentPort assessmentPort, ResponseVerificationPort verificationPort) {
        this.assessmentPort = Objects.requireNonNull(assessmentPort, "assessmentPort must not be null");
        this.verificationPort = Objects.requireNonNull(verificationPort, "verificationPort must not be null");
    }

    public AssessmentOutcome run(TaskAttempt closedAttempt, TaskPackage taskPackage) {
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
        ResponseAssessment assessment = assessmentPort.assess(context);
        ResponseAssessment verification = null;
        if (deterministic == EquivalenceOutcome.CANNOT_DECIDE) {
            verification = verificationPort.verify(context);
        }
        return ResponseAssessmentDecider.decide(context, assessment, verification);
    }

    private static boolean outcomeIsDeterminedWithoutModel(AttemptPurpose purpose, EquivalenceOutcome deterministic) {
        return purpose == AttemptPurpose.DIAGNOSTIC && deterministic == EquivalenceOutcome.PROVEN_EQUIVALENT
                || purpose == AttemptPurpose.INDEPENDENT_TEST
                && deterministic == EquivalenceOutcome.PROVEN_NOT_EQUIVALENT;
    }

    /**
     * Appends every non-null isolated judgment carried by the outcome to the
     * Artifact Store. Duplicate recordings are audit records, never state.
     */
    public static void recordAssessments(
            ArtifactStore artifactStore,
            UUID attemptId,
            AssessmentOutcome outcome
    ) {
        ResponseAssessment assessment = switch (outcome) {
            case AssessmentOutcome.Passed passed -> passed.assessment();
            case AssessmentOutcome.Failed failed -> failed.assessment();
            case AssessmentOutcome.Inconclusive inconclusive -> inconclusive.assessment();
            case AssessmentOutcome.Blocked blocked -> blocked.assessment();
        };
        if (assessment != null) {
            artifactStore.recordResponseAssessment(attemptId, assessment);
        }
        ResponseAssessment verification = switch (outcome) {
            case AssessmentOutcome.Passed passed -> passed.verification();
            case AssessmentOutcome.Failed failed -> failed.verification();
            case AssessmentOutcome.Inconclusive inconclusive -> inconclusive.verification();
            case AssessmentOutcome.Blocked blocked -> blocked.verification();
        };
        if (verification != null) {
            artifactStore.recordResponseAssessment(attemptId, verification);
        }
    }
}
