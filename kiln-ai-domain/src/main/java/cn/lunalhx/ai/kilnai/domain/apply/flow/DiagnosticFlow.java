package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExposureLedger;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * The Diagnostic submission flow: one formal submission atomically closes the
 * Diagnostic Attempt, a passing Diagnostic moves through a Neutral Transition
 * to a fresh verified Independent task, an inconclusive Diagnostic does the
 * same without failure feedback, and a failing Diagnostic ends safely. It
 * never creates Evidence.
 */
public final class DiagnosticFlow {

    public static final String NEUTRAL_TRANSITION_MESSAGE = "接下来是一道新的独立练习题，请独立完成。";
    public static final String SAFE_END_MESSAGE = "本次诊断已结束，请继续下一步学习。";

    private final ApplyProfileExecutor executor;
    private final ExposureLedger exposureLedger;
    private final TaskAttemptStore attemptStore;
    private final AssessmentRunner assessmentRunner;
    private final SubmissionCloser submissionCloser;
    private final ApplyExecutionContext diagnosticContext;
    private final ApplyExecutionContext independentContextTemplate;

    public DiagnosticFlow(
            ApplyProfileExecutor executor,
            TaskAttemptStore attemptStore,
            ExposureLedger exposureLedger,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            ApplyExecutionContext diagnosticContext,
            ApplyExecutionContext independentContextTemplate,
            Clock clock
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.exposureLedger = Objects.requireNonNull(exposureLedger, "exposureLedger must not be null");
        this.attemptStore = Objects.requireNonNull(attemptStore, "attemptStore must not be null");
        this.assessmentRunner = new AssessmentRunner(
                Objects.requireNonNull(assessmentPort, "assessmentPort must not be null"),
                Objects.requireNonNull(verificationPort, "verificationPort must not be null"));
        this.submissionCloser = new SubmissionCloser(attemptStore, clock);
        this.diagnosticContext = Objects.requireNonNull(diagnosticContext, "diagnosticContext must not be null");
        this.independentContextTemplate = Objects.requireNonNull(
                independentContextTemplate, "independentContextTemplate must not be null");
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
        SubmissionCloser.CloseResult closed = submissionCloser.close(
                attemptId, AttemptPurpose.DIAGNOSTIC, rawDerivative, confirmedCanonical, rationale);
        return switch (closed) {
            case SubmissionCloser.CloseResult.Ignored ignored ->
                    new DiagnosticSubmissionResult.Ignored(ignored.reason());
            case SubmissionCloser.CloseResult.NotSubmittable notSubmittable ->
                    new DiagnosticSubmissionResult.NotSubmittable(notSubmittable.reason());
            case SubmissionCloser.CloseResult.Closed closedAttempt -> assess(closedAttempt.attempt());
        };
    }

    private DiagnosticSubmissionResult assess(TaskAttempt closedAttempt) {
        AssessmentOutcome outcome = assessmentRunner.run(closedAttempt, packageOf(closedAttempt));
        return switch (outcome) {
            case AssessmentOutcome.Passed passed -> deliverIndependent(closedAttempt, outcome);
            case AssessmentOutcome.Inconclusive inconclusive -> deliverIndependent(closedAttempt, outcome);
            case AssessmentOutcome.Failed failed ->
                    new DiagnosticSubmissionResult.Failed(closedAttempt, SAFE_END_MESSAGE);
            case AssessmentOutcome.Blocked blocked -> throw new IllegalStateException(
                    "a contradictory rationale is only valid for an Independent Test");
        };
    }

    private DiagnosticSubmissionResult deliverIndependent(TaskAttempt closedDiagnosticAttempt, AssessmentOutcome outcome) {
        ApplyExecutionContext independentContext = independentContextTemplate.withNoveltyExclusions(
                exposureLedger.exposedTaskFingerprints(),
                exposureLedger.exposedSolutionFingerprints());
        ApplyDeliveryResult result = executor.deliver(independentContext);
        if (result instanceof ApplyDeliveryResult.Delivered delivered) {
            recordExposure(delivered.attempt().taskPackageId());
            return switch (outcome) {
                case AssessmentOutcome.Passed passed ->
                        new DiagnosticSubmissionResult.Passed(
                                closedDiagnosticAttempt, NEUTRAL_TRANSITION_MESSAGE,
                                delivered.attempt(), delivered.learnerProjection());
                case AssessmentOutcome.Inconclusive inconclusive ->
                        new DiagnosticSubmissionResult.Inconclusive(
                                closedDiagnosticAttempt, NEUTRAL_TRANSITION_MESSAGE,
                                delivered.attempt(), delivered.learnerProjection());
                default -> throw new IllegalStateException(
                        "only a passing or inconclusive assessment delivers an Independent task: " + outcome);
            };
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
