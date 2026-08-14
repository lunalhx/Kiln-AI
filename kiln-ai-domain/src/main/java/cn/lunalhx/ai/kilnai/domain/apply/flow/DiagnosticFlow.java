package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.CanonicalExpressionResolver;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalEquivalenceCheck;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExposureLedger;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The Diagnostic submission flow: one formal submission atomically closes the
 * Diagnostic Attempt, a passing Diagnostic moves through a Neutral Transition
 * to a fresh verified Independent task, and a failing Diagnostic ends safely.
 */
public final class DiagnosticFlow {

    public static final String NEUTRAL_TRANSITION_MESSAGE = "接下来是一道新的独立练习题，请独立完成。";
    public static final String SAFE_END_MESSAGE = "本次诊断已结束，请继续下一步学习。";

    private final ApplyProfileExecutor executor;
    private final TaskAttemptStore attemptStore;
    private final ExposureLedger exposureLedger;
    private final AssessmentPort assessmentPort;
    private final ApplyExecutionContext diagnosticContext;
    private final ApplyExecutionContext independentContextTemplate;
    private final Clock clock;

    public DiagnosticFlow(
            ApplyProfileExecutor executor,
            TaskAttemptStore attemptStore,
            ExposureLedger exposureLedger,
            AssessmentPort assessmentPort,
            ApplyExecutionContext diagnosticContext,
            ApplyExecutionContext independentContextTemplate,
            Clock clock
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.attemptStore = Objects.requireNonNull(attemptStore, "attemptStore must not be null");
        this.exposureLedger = Objects.requireNonNull(exposureLedger, "exposureLedger must not be null");
        this.assessmentPort = Objects.requireNonNull(assessmentPort, "assessmentPort must not be null");
        this.diagnosticContext = Objects.requireNonNull(diagnosticContext, "diagnosticContext must not be null");
        this.independentContextTemplate = Objects.requireNonNull(
                independentContextTemplate, "independentContextTemplate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ApplyDeliveryResult startDiagnostic() {
        ApplyDeliveryResult result = executor.deliver(diagnosticContext);
        if (result instanceof ApplyDeliveryResult.Delivered delivered) {
            recordExposure(delivered.attempt().taskPackageId());
        }
        return result;
    }

    public DiagnosticSubmissionResult submitDiagnostic(
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Optional<TaskAttempt> maybeAttempt = attemptStore.findAttempt(attemptId);
        if (maybeAttempt.isEmpty()) {
            return new DiagnosticSubmissionResult.Ignored(
                    DiagnosticSubmissionResult.IgnoreReason.ATTEMPT_NOT_FOUND);
        }
        TaskAttempt attempt = maybeAttempt.get();
        if (attempt.purpose() != AttemptPurpose.DIAGNOSTIC) {
            return new DiagnosticSubmissionResult.Ignored(
                    DiagnosticSubmissionResult.IgnoreReason.NOT_A_DIAGNOSTIC_ATTEMPT);
        }
        Optional<CanonicalExpressionResolver.Resolution> resolution = CanonicalExpressionResolver.resolve(
                rawDerivative, diagnosticContext.answerRepresentationContract().variables());
        if (resolution.isEmpty()) {
            return new DiagnosticSubmissionResult.NotSubmittable(
                    DiagnosticSubmissionResult.RejectionReason.UNPARSEABLE_RAW);
        }
        if (!sameCanonical(resolution.get().canonical(), confirmedCanonical)) {
            return new DiagnosticSubmissionResult.NotSubmittable(
                    DiagnosticSubmissionResult.RejectionReason.CONFIRMATION_MISMATCH);
        }

        TaskSubmission submission = new TaskSubmission(
                new MathematicalAnswer(rawDerivative, confirmedCanonical, resolution.get().family()),
                rationale,
                clock.instant());
        AttemptCloseOutcome closeOutcome = attemptStore.closeAttempt(attemptId, submission);
        if (closeOutcome.result() != AttemptCloseOutcome.Result.CLOSED) {
            return new DiagnosticSubmissionResult.Ignored(
                    DiagnosticSubmissionResult.IgnoreReason.ALREADY_SUBMITTED);
        }
        TaskAttempt closedAttempt = closeOutcome.attempt();

        if (!passed(closedAttempt, confirmedCanonical, rationale)) {
            return new DiagnosticSubmissionResult.Failed(closedAttempt, SAFE_END_MESSAGE);
        }
        return deliverIndependent(closedAttempt);
    }

    private static boolean sameCanonical(String derivedCanonical, String confirmedCanonical) {
        if (confirmedCanonical == null) {
            return false;
        }
        return derivedCanonical.trim().replaceAll("\\s+", " ")
                .equals(confirmedCanonical.trim().replaceAll("\\s+", " "));
    }

    private boolean passed(TaskAttempt closedAttempt, String confirmedCanonical, String rationale) {        if (finalDerivativePasses(closedAttempt, confirmedCanonical)) {
            return true;
        }
        return rationaleApplies(closedAttempt, confirmedCanonical, rationale);
    }

    private boolean finalDerivativePasses(TaskAttempt attempt, String confirmedCanonical) {
        TaskPackage taskPackage = packageOf(attempt);
        String expected = taskPackage.privateAssessorProjection().canonicalExpectedAnswer().expression();
        List<String> variables = taskPackage.privateAssessorProjection().canonicalExpectedAnswer().variables();
        return MathematicalEquivalenceCheck.check(confirmedCanonical, expected, variables)
                == EquivalenceOutcome.PROVEN_EQUIVALENT;
    }

    private boolean rationaleApplies(TaskAttempt attempt, String confirmedCanonical, String rationale) {
        TaskPackage taskPackage = packageOf(attempt);
        RationaleAssessmentContext context = new RationaleAssessmentContext(
                taskPackage.learnerProjection().taskText(),
                taskPackage.privateAssessorProjection().canonicalExpectedAnswer().expression(),
                confirmedCanonical,
                rationale == null ? "" : rationale);
        return assessmentPort.judgeDiagnosticRationale(context) == RationaleJudgment.APPLICABLE;
    }

    private DiagnosticSubmissionResult deliverIndependent(TaskAttempt closedDiagnosticAttempt) {
        ApplyExecutionContext independentContext = independentContextTemplate.withNoveltyExclusions(
                exposureLedger.exposedTaskFingerprints(),
                exposureLedger.exposedSolutionFingerprints());
        ApplyDeliveryResult result = executor.deliver(independentContext);
        if (result instanceof ApplyDeliveryResult.Delivered delivered) {
            recordExposure(delivered.attempt().taskPackageId());
            return new DiagnosticSubmissionResult.Passed(
                    closedDiagnosticAttempt,
                    NEUTRAL_TRANSITION_MESSAGE,
                    delivered.attempt(),
                    delivered.learnerProjection());
        }
        ApplyDeliveryResult.Unavailable unavailable = (ApplyDeliveryResult.Unavailable) result;
        return new DiagnosticSubmissionResult.IndependentUnavailable(
                unavailable.reason(), unavailable.learnerMessage());
    }

    private void recordExposure(UUID taskPackageId) {
        exposureLedger.recordTaskExposure(packageOf(taskPackageId));
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return packageOf(attempt.taskPackageId());
    }

    private TaskPackage packageOf(UUID taskPackageId) {
        return attemptStore.findPackage(taskPackageId).orElseThrow();
    }
}
