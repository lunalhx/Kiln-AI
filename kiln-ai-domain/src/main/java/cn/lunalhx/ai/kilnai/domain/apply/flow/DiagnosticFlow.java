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
import cn.lunalhx.ai.kilnai.domain.apply.profile.RationaleEvaluationProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticFinding;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlan;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticRoutingDecision;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The Diagnostic submission flow: one formal submission atomically closes the
 * Diagnostic Attempt and records a Flow-scoped Finding. The Diagnostic Routing
 * Decision — not the last Attempt alone — then authorizes a Neutral Transition
 * to a fresh Independent task, Target Learning with a learner-safe Summary, or
 * a neutral Target Learning entry. It never creates Evidence.
 */
public final class DiagnosticFlow {

    public static final String NEUTRAL_TRANSITION_MESSAGE = "接下来是一道新的独立练习题，请独立完成。";
    public static final String SAFE_END_MESSAGE = "本次诊断已结束，请继续下一步学习。";

    private final ApplyProfileExecutor executor;
    private final LearningFlowStore flowStore;
    private final ArtifactStore artifactStore;
    private final AssessmentRunner assessmentRunner;
    private final RationaleEvaluationProfileExecutor rationaleAssessmentExecutor;
    private final RationaleEvaluationProfileExecutor rationaleSufficiencyExecutor;
    private final SubmissionCloser submissionCloser;
    private final Clock clock;
    private final ApplyExecutionContext diagnosticContext;
    private final ApplyExecutionContext independentContextTemplate;

    public DiagnosticFlow(
            ApplyProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            RationaleEvaluationProfileExecutor rationaleAssessmentExecutor,
            RationaleEvaluationProfileExecutor rationaleSufficiencyExecutor,
            ApplyExecutionContext diagnosticContext,
            ApplyExecutionContext independentContextTemplate,
            Clock clock
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.assessmentRunner = new AssessmentRunner(
                Objects.requireNonNull(assessmentPort, "assessmentPort must not be null"),
                Objects.requireNonNull(verificationPort, "verificationPort must not be null"),
                artifactStore);
        this.rationaleAssessmentExecutor = Objects.requireNonNull(
                rationaleAssessmentExecutor, "rationaleAssessmentExecutor must not be null");
        this.rationaleSufficiencyExecutor = Objects.requireNonNull(
                rationaleSufficiencyExecutor, "rationaleSufficiencyExecutor must not be null");
        this.submissionCloser = new SubmissionCloser(artifactStore, clock);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
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
        boolean successorAlreadyExposed = flowStore.exposedTaskPackageIds(flowId).stream()
                .map(artifactStore::findPackage)
                .flatMap(Optional::stream)
                .anyMatch(taskPackage -> taskPackage.attemptPurpose() != AttemptPurpose.DIAGNOSTIC);
        if (successorAlreadyExposed) {
            return new DiagnosticSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return assess(flowId, profile, closedAttempt);
    }

    private DiagnosticSubmissionResult assess(UUID flowId, ModelProfile profile, TaskAttempt closedAttempt) {
        AssessmentOutcome outcome = assessmentRunner.runDiagnostic(
                profile, closedAttempt, packageOf(closedAttempt), diagnosticContext,
                rationaleAssessmentExecutor, rationaleSufficiencyExecutor);
        DiagnosticSubmissionResult assessed = switch (outcome) {
            case AssessmentOutcome.Passed passed ->
                    new DiagnosticSubmissionResult.PassedAttempt(closedAttempt, passingFacts(), null);
            case AssessmentOutcome.Inconclusive inconclusive ->
                    new DiagnosticSubmissionResult.Unconfirmed(closedAttempt, neutralFacts(), null);
            case AssessmentOutcome.Unconfirmed unconfirmed ->
                    new DiagnosticSubmissionResult.Unconfirmed(closedAttempt, neutralFacts(), null);
            case AssessmentOutcome.Failed failed ->
                    new DiagnosticSubmissionResult.Failed(closedAttempt, failureFacts(closedAttempt, outcome), null);
            case AssessmentOutcome.Blocked blocked -> throw new IllegalStateException(
                    "a contradictory rationale is only valid for an Independent Test");
        };
        DiagnosticFinding.Kind kind = switch (assessed) {
            case DiagnosticSubmissionResult.PassedAttempt passed -> DiagnosticFinding.Kind.PASSING_OBSERVATION;
            case DiagnosticSubmissionResult.Failed failed -> DiagnosticFinding.Kind.CONCLUSIVE_GAP;
            case DiagnosticSubmissionResult.Unconfirmed unconfirmed -> DiagnosticFinding.Kind.UNCONFIRMED_PERFORMANCE;
            default -> throw new IllegalStateException("unexpected assessed result: " + assessed);
        };
        FeedbackFacts facts = switch (assessed) {
            case DiagnosticSubmissionResult.PassedAttempt passed -> passed.facts();
            case DiagnosticSubmissionResult.Failed failed -> failed.facts();
            case DiagnosticSubmissionResult.Unconfirmed unconfirmed -> unconfirmed.facts();
            default -> throw new IllegalStateException("unexpected assessed result: " + assessed);
        };
        DiagnosticPlan plan = flowStore.diagnosticPlan(flowId).orElseThrow(
                () -> new IllegalStateException("Diagnostic requires a frozen Plan"));
        List<String> covered = kind == DiagnosticFinding.Kind.PASSING_OBSERVATION
                ? facts.satisfiedCriteria()
                : plan.targetReadinessCriterionIds();
        DiagnosticFinding finding = new DiagnosticFinding(
                UUID.randomUUID(), flowId, closedAttempt.attemptId(), kind, covered,
                facts.missingCriteria(), facts.errorDimensions(), clock.instant());
        List<DiagnosticFinding> accumulated = new java.util.ArrayList<>(flowStore.diagnosticFindings(flowId));
        accumulated.add(finding);
        DiagnosticSubmissionResult routed = switch (DiagnosticRoutingDecision.decide(plan, accumulated)) {
            case FRESH_INDEPENDENT_TEST -> deliverIndependent(flowId, profile, closedAttempt);
            case TARGET_LEARNING_WITH_SUMMARY, TARGET_LEARNING_NEUTRAL -> assessed;
        };
        if (routed instanceof DiagnosticSubmissionResult.IndependentUnavailable) {
            return routed;
        }
        return withFinding(routed, finding);
    }

    private static DiagnosticSubmissionResult withFinding(
            DiagnosticSubmissionResult result,
            DiagnosticFinding finding
    ) {
        return switch (result) {
            case DiagnosticSubmissionResult.Passed passed -> new DiagnosticSubmissionResult.Passed(
                    passed.closedDiagnosticAttempt(), passed.neutralTransitionMessage(),
                    passed.independentAttempt(), passed.independentLearnerProjection(), finding);
            case DiagnosticSubmissionResult.PassedAttempt passed ->
                    new DiagnosticSubmissionResult.PassedAttempt(passed.closedDiagnosticAttempt(), passed.facts(), finding);
            case DiagnosticSubmissionResult.Failed failed ->
                    new DiagnosticSubmissionResult.Failed(failed.closedDiagnosticAttempt(), failed.facts(), finding);
            case DiagnosticSubmissionResult.Unconfirmed unconfirmed ->
                    new DiagnosticSubmissionResult.Unconfirmed(
                            unconfirmed.closedDiagnosticAttempt(), unconfirmed.facts(), finding);
            default -> result;
        };
    }

    public DiagnosticSubmissionResult deliverIndependent(
            UUID flowId,
            ModelProfile profile,
            TaskAttempt closedDiagnosticAttempt
    ) {
        ApplyExecutionContext independentContext = independentContextTemplate.withNoveltyExclusions(
                flowStore.noveltyExclusions(flowId));
        ApplyDeliveryResult result = executor.deliver(profile, independentContext);
        if (result instanceof ApplyDeliveryResult.Delivered delivered) {
            recordExposure(flowId, delivered.attempt().taskPackageId());
            return new DiagnosticSubmissionResult.Passed(
                    closedDiagnosticAttempt, NEUTRAL_TRANSITION_MESSAGE,
                    delivered.attempt(), delivered.learnerProjection(), null);
        }
        ApplyDeliveryResult.Unavailable unavailable = (ApplyDeliveryResult.Unavailable) result;
        return new DiagnosticSubmissionResult.IndependentUnavailable(
                unavailable.reason(), unavailable.learnerMessage());
    }

    private FeedbackFacts passingFacts() {
        return new FeedbackFacts(criterionIds(), List.of(), List.of(), 0, List.of(), false);
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

    private FeedbackFacts neutralFacts() {
        return new FeedbackFacts(List.of(), List.of(), List.of(), 0, List.of(), false);
    }

    private List<String> criterionIds() {
        return diagnosticContext.masteryRubric().criteria().stream()
                .map(criterion -> criterion.id())
                .toList();
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
