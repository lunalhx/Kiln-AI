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
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The Diagnostic submission flow: one formal submission atomically closes the
 * Diagnostic Attempt and records a Flow-scoped Finding. The StateGraph applies
 * the Diagnostic Routing Decision to choose the next committed interaction.
 * This flow never creates Evidence.
 */
public final class DiagnosticFlow {

    public static final String NEUTRAL_TRANSITION_MESSAGE = "接下来是一道新的独立练习题，请独立完成。";
    public static final String DIAGNOSTIC_CONTINUATION_MESSAGE = "诊断进度已保存，请继续。";

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

    public ApplyDeliveryResult startDiagnostic(UUID flowId, ModelProfile profile, DiagnosticPlan plan) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(plan, "plan must not be null");
        ApplyDeliveryResult result = executor.deliver(
                profile, diagnosticContextFor(plan, flowStore.noveltyExclusions(flowId)));
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
    public ApplyProfileExecutor.PreparedDelivery prepareDiagnostic(
            ModelProfile profile,
            DiagnosticPlan plan
    ) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(plan, "plan must not be null");
        return executor.prepareTask(
                profile, diagnosticContextFor(plan, diagnosticContext.noveltyExclusions()), false);
    }

    /**
     * Prepares a fresh Diagnostic task against the Flow's committed novelty
     * ledger. Nothing is persisted until the Graph binds the returned package
     * with the continuation command.
     */
    public ApplyProfileExecutor.PreparedDelivery prepareDiagnostic(UUID flowId, ModelProfile profile) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        DiagnosticPlan plan = flowStore.diagnosticPlan(flowId).orElseThrow(
                () -> new IllegalStateException("Diagnostic requires a frozen Plan"));
        return executor.prepareTask(
                profile,
                diagnosticContextFor(plan, flowStore.noveltyExclusions(flowId)),
                false);
    }

    /**
     * Prepares the fresh equivalent Independent successor without opening an
     * Attempt. The Graph binds it only after the neutral Diagnostic transition
     * has been committed.
     */
    public ApplyProfileExecutor.PreparedDelivery prepareIndependent(UUID flowId, ModelProfile profile) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        return executor.prepareTask(
                profile,
                independentContextTemplate.withNoveltyExclusions(flowStore.noveltyExclusions(flowId)),
                false);
    }

    /**
     * Closes and assesses one Diagnostic Attempt without selecting or
     * generating a successor. The Learning StateGraph owns the route and
     * commits the neutral boundary before any successor preparation.
     */
    public DiagnosticSubmissionResult submitDiagnosticAttempt(
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
            case SubmissionCloser.CloseResult.Closed closedAttempt ->
                    assessAttempt(flowId, profile, closedAttempt.attempt());
            case SubmissionCloser.CloseResult.Recovered recovered ->
                    recoverDiagnosticAttempt(flowId, profile, recovered.attempt());
        };
    }

    private DiagnosticSubmissionResult recoverDiagnosticAttempt(
            UUID flowId,
            ModelProfile profile,
            TaskAttempt closedAttempt
    ) {
        if (flowStore.diagnosticFindings(flowId).stream()
                .anyMatch(finding -> finding.attemptId().equals(closedAttempt.attemptId()))) {
            return new DiagnosticSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return assessAttempt(flowId, profile, closedAttempt);
    }

    private DiagnosticSubmissionResult assessAttempt(
            UUID flowId,
            ModelProfile profile,
            TaskAttempt closedAttempt
    ) {
        DiagnosticPlan plan = flowStore.diagnosticPlan(flowId).orElseThrow(
                () -> new IllegalStateException("Diagnostic requires a frozen Plan"));
        AssessmentOutcome outcome = assessmentRunner.runDiagnostic(
                profile,
                closedAttempt,
                packageOf(closedAttempt),
                diagnosticContextFor(plan, diagnosticContext.noveltyExclusions()),
                rationaleAssessmentExecutor, rationaleSufficiencyExecutor);
        DiagnosticSubmissionResult assessed = switch (outcome) {
            case AssessmentOutcome.Passed passed ->
                    new DiagnosticSubmissionResult.PassedAttempt(
                            closedAttempt, passingFacts(packageOf(closedAttempt)), null);
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
        List<String> targetCriteria = plan.targetReadinessCriterionIds();
        List<String> covered = facts.satisfiedCriteria().stream()
                .filter(targetCriteria::contains)
                .toList();
        List<String> missing = facts.missingCriteria().stream()
                .filter(targetCriteria::contains)
                .toList();
        DiagnosticFinding finding = new DiagnosticFinding(
                UUID.randomUUID(), flowId, closedAttempt.attemptId(), kind, covered,
                missing, facts.errorDimensions(), clock.instant());
        return withFinding(assessed, finding);
    }

    private static DiagnosticSubmissionResult withFinding(
            DiagnosticSubmissionResult result,
            DiagnosticFinding finding
    ) {
        return switch (result) {
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

    private FeedbackFacts passingFacts(TaskPackage taskPackage) {
        return new FeedbackFacts(criterionIds(taskPackage), List.of(), List.of(), 0, List.of(), false);
    }

    private FeedbackFacts failureFacts(TaskAttempt closedAttempt, AssessmentOutcome outcome) {
        return new FeedbackFacts(
                List.of(),
                criterionIds(packageOf(closedAttempt)),
                AssessmentRunner.errorDimensions(outcome),
                closedAttempt.highestHintLevel(),
                closedAttempt.assistanceTraceStrings(),
                false);
    }

    private FeedbackFacts neutralFacts() {
        return new FeedbackFacts(List.of(), List.of(), List.of(), 0, List.of(), false);
    }

    private List<String> criterionIds(TaskPackage taskPackage) {
        return taskPackage.privateAssessorProjection().rubricMapping().stream()
                .map(mapping -> mapping.masteryCriterionId())
                .toList();
    }

    private ApplyExecutionContext diagnosticContextFor(
            DiagnosticPlan plan,
            ApplyExecutionContext.NoveltyExclusions noveltyExclusions
    ) {
        List<ApplyExecutionContext.RubricCriterion> targetCriteria = plan.targetReadinessCriterionIds().stream()
                .map(targetId -> diagnosticContext.masteryRubric().criteria().stream()
                        .filter(criterion -> criterion.id().equals(targetId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "Diagnostic Plan target criterion is absent from the Diagnostic Rubric: " + targetId)))
                .toList();
        ApplyExecutionContext.MasteryRubric targetRubric = new ApplyExecutionContext.MasteryRubric(
                diagnosticContext.masteryRubric().id(),
                diagnosticContext.masteryRubric().version(),
                targetCriteria);
        return new ApplyExecutionContext(
                diagnosticContext.schema(),
                diagnosticContext.conceptContract(),
                targetRubric,
                diagnosticContext.taskBlueprint(),
                diagnosticContext.conceptSourcePack(),
                noveltyExclusions,
                diagnosticContext.answerRepresentationContract(),
                diagnosticContext.learnerLocale());
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
