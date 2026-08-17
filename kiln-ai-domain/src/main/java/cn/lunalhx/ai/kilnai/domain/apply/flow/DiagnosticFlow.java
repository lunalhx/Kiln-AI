package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The Diagnostic submission flow: one formal submission atomically closes the
 * Diagnostic Attempt, a passing Diagnostic moves through a Neutral Transition
 * to a fresh verified Independent task, an inconclusive Diagnostic does the
 * same without failure feedback, and a failing Diagnostic ends safely. It
 * never creates Evidence. Every displayed package and assessment artifact is
 * persisted durably; the flow carries no in-memory state across calls.
 */
public final class DiagnosticFlow {

    public static final String NEUTRAL_TRANSITION_MESSAGE = "接下来是一道新的独立练习题，请独立完成。";
    public static final String SAFE_END_MESSAGE = "本次诊断已结束，请继续下一步学习。";

    private final ApplyProfileExecutor executor;
    private final LearningFlowStore flowStore;
    private final ArtifactStore artifactStore;
    private final AssessmentRunner assessmentRunner;
    private final SubmissionCloser submissionCloser;
    private final ApplyExecutionContext diagnosticContext;
    private final ApplyExecutionContext independentContextTemplate;

    public DiagnosticFlow(
            ApplyProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            ApplyExecutionContext diagnosticContext,
            ApplyExecutionContext independentContextTemplate,
            Clock clock
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.assessmentRunner = new AssessmentRunner(
                Objects.requireNonNull(assessmentPort, "assessmentPort must not be null"),
                Objects.requireNonNull(verificationPort, "verificationPort must not be null"));
        this.submissionCloser = new SubmissionCloser(artifactStore, clock);
        this.diagnosticContext = Objects.requireNonNull(diagnosticContext, "diagnosticContext must not be null");
        this.independentContextTemplate = Objects.requireNonNull(
                independentContextTemplate, "independentContextTemplate must not be null");
    }

    public ApplyDeliveryResult startDiagnostic(UUID flowId, ModelProfile profile) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        ApplyDeliveryResult result = executor.deliver(profile, diagnosticContext);
        if (result instanceof ApplyDeliveryResult.Delivered delivered) {
            recordExposure(flowId, delivered.attempt().taskPackageId());
        }
        return result;
    }

    /**
     * The atomic Start preparation of the Learning command surface: the
     * bounded generation, Output Gate, and Task Verification cycles run to
     * completion without persisting any Flow, Source Pack, Package, Attempt,
     * Exposure, or verification audit. A ready package is bound durably by
     * the Start itself; an unavailable outcome leaves nothing behind and the
     * command reports the generic 503.
     */
    public ApplyProfileExecutor.PreparedDelivery prepareDiagnostic(ModelProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        return executor.prepareTask(profile, diagnosticContext, false);
    }

    /**
     * One formal Diagnostic submission: atomically closes the Attempt and
     * saves the submission, then runs isolated Assessment. A recovered closed
     * Attempt resumes from the database-saved submission; the request body
     * cannot replace it. A successor Independent already exposed is a
     * duplicate and is ignored.
     */
    public DiagnosticSubmissionResult submitDiagnostic(
            UUID flowId,
            ModelProfile profile,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        SubmissionCloser.CloseResult closed = submissionCloser.close(
                attemptId, AttemptPurpose.DIAGNOSTIC, rawDerivative, confirmedCanonical, rationale);
        return switch (closed) {
            case SubmissionCloser.CloseResult.Ignored ignored ->
                    new DiagnosticSubmissionResult.Ignored(ignored.reason());
            case SubmissionCloser.CloseResult.NotSubmittable notSubmittable ->
                    new DiagnosticSubmissionResult.NotSubmittable(notSubmittable.reason());
            case SubmissionCloser.CloseResult.Closed closedAttempt -> assess(flowId, profile, closedAttempt.attempt());
            case SubmissionCloser.CloseResult.Recovered recovered ->
                    recoverOrIgnore(flowId, profile, recovered.attempt());
        };
    }

    /**
     * An already-closed Diagnostic Attempt carries its saved submission. When
     * a successor Independent task was already exposed, the command is a
     * duplicate whose outcome exists and nothing is re-run; otherwise the
     * process crashed between closing and committing, and Assessment resumes
     * from the database-saved submission. The exposed Independent package is
     * the exactly-once guard: Diagnostic creates no Evidence.
     */
    private DiagnosticSubmissionResult recoverOrIgnore(
            UUID flowId,
            ModelProfile profile,
            TaskAttempt closedAttempt
    ) {
        boolean independentAlreadyExposed = flowStore.exposedTaskPackageIds(flowId).stream()
                .map(artifactStore::findPackage)
                .flatMap(Optional::stream)
                .anyMatch(taskPackage -> taskPackage.attemptPurpose() == AttemptPurpose.INDEPENDENT_TEST);
        if (independentAlreadyExposed) {
            return new DiagnosticSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return assess(flowId, profile, closedAttempt);
    }

    private DiagnosticSubmissionResult assess(UUID flowId, ModelProfile profile, TaskAttempt closedAttempt) {
        AssessmentOutcome outcome = assessmentRunner.run(profile, closedAttempt, packageOf(closedAttempt));
        AssessmentRunner.recordAssessments(artifactStore, closedAttempt.attemptId(), outcome);
        return switch (outcome) {
            case AssessmentOutcome.Passed passed -> deliverIndependent(flowId, profile, closedAttempt, outcome);
            case AssessmentOutcome.Inconclusive inconclusive -> deliverIndependent(flowId, profile, closedAttempt, outcome);
            // A failed submitted Diagnostic stays closed and is never
            // retroactively converted. The next learner-visible move is not
            // chosen here: the Learning StateGraph derives the legal
            // remediation actions through the Workflow Guard and Pedagogy
            // Agent, which receive only the sanitized Feedback Facts.
            case AssessmentOutcome.Failed failed ->
                    new DiagnosticSubmissionResult.Failed(closedAttempt, failureFacts(closedAttempt, outcome));
            case AssessmentOutcome.Blocked blocked -> throw new IllegalStateException(
                    "a contradictory rationale is only valid for an Independent Test");
        };
    }

    private FeedbackFacts failureFacts(TaskAttempt closedAttempt, AssessmentOutcome outcome) {
        return new FeedbackFacts(
                List.of(),
                criterionIds(),
                AssessmentRunner.errorDimensions(outcome),
                closedAttempt.highestHintLevel(),
                closedAttempt.assistanceTraceStrings(),
                false);
    }

    private List<String> criterionIds() {
        return diagnosticContext.masteryRubric().criteria().stream()
                .map(criterion -> criterion.id())
                .toList();
    }

    private DiagnosticSubmissionResult deliverIndependent(
            UUID flowId,
            ModelProfile profile,
            TaskAttempt closedDiagnosticAttempt,
            AssessmentOutcome outcome
    ) {
        ApplyExecutionContext independentContext = independentContextTemplate.withNoveltyExclusions(
                flowStore.noveltyExclusions(flowId));
        ApplyDeliveryResult result = executor.deliver(profile, independentContext);
        if (result instanceof ApplyDeliveryResult.Delivered delivered) {
            recordExposure(flowId, delivered.attempt().taskPackageId());
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

    private void recordExposure(UUID flowId, UUID taskPackageId) {
        flowStore.recordTaskExposure(flowId, packageOf(taskPackageId));
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return packageOf(attempt.taskPackageId());
    }

    private TaskPackage packageOf(UUID taskPackageId) {
        return artifactStore.findPackage(taskPackageId).orElseThrow();
    }
}
