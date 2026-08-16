package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.HintFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintView;
import cn.lunalhx.ai.kilnai.domain.apply.model.IndependentSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PracticeSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.ProcessedCommand;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The plain Java Learning StateGraph runner (ADR-0066). It owns one Graph Run
 * per learner command: rehydrates the Learning State from the durable records
 * and the saved checkpoint, runs the deterministic input gate, routes the
 * command through the single legal node (Diagnostic, Explain, Practice,
 * Independent submission, or Hint), and stops at the next Learner Interaction
 * Boundary by committing the learner interaction, its checkpoint, and the
 * processed command atomically. An accepted Diagnostic failure opens the
 * remediation cycle with the Explain teaching boundary; a Continue command
 * resumes it with the fresh Apply Practice node, whose open Attempt also
 * accepts Hint requests. The graph carries no in-memory state across runs; a
 * fresh instance resumes exactly from the last committed boundary, and a
 * crash between the two committed halves of a submission or hint exposure
 * resumes from the saved Attempt or saved hint request through the reused
 * node capability. Profiles, Assessment, and the Pedagogy Agent never write
 * Learning State; only this runner and its nodes own the store.
 */
public final class LearningStateGraph {

    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final DiagnosticFlow diagnosticFlow;
    private final IndependentSubmissionFlow independentFlow;
    private final PracticeSubmissionFlow practiceFlow;
    private final ExplainFlow explainFlow;
    private final HintFlow hintFlow;
    private final TeachBackFlow teachBackFlow;
    private final Clock clock;

    public LearningStateGraph(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            PracticeSubmissionFlow practiceFlow,
            ExplainFlow explainFlow,
            HintFlow hintFlow,
            TeachBackFlow teachBackFlow,
            Clock clock
    ) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.diagnosticFlow = Objects.requireNonNull(diagnosticFlow, "diagnosticFlow must not be null");
        this.independentFlow = Objects.requireNonNull(independentFlow, "independentFlow must not be null");
        this.practiceFlow = Objects.requireNonNull(practiceFlow, "practiceFlow must not be null");
        this.explainFlow = Objects.requireNonNull(explainFlow, "explainFlow must not be null");
        this.hintFlow = Objects.requireNonNull(hintFlow, "hintFlow must not be null");
        this.teachBackFlow = Objects.requireNonNull(teachBackFlow, "teachBackFlow must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * The Graph Run of a flow start: the Diagnostic node delivers the first
     * task and the run stops at the first Learner Interaction Boundary — or at
     * a terminal unavailable boundary when no task can be prepared.
     */
    public ApplyFlowResult start(UUID flowId, UUID idempotencyKey, String requestHash) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        ApplyDeliveryResult delivery = diagnosticFlow.startDiagnostic(flowId);
        ApplyFlowInteraction interaction = switch (delivery) {
            case ApplyDeliveryResult.Delivered delivered -> new ApplyFlowInteraction(
                    flowId, 1, FlowStatus.AWAITING_LEARNER_INPUT, LearningStage.DIAGNOSTIC,
                    delivered.attempt().attemptId(), delivered.attempt().purpose(),
                    delivered.learnerProjection(), null, null, null);
            case ApplyDeliveryResult.Unavailable unavailable -> new ApplyFlowInteraction(
                    flowId, 1, FlowStatus.TERMINAL, LearningStage.DIAGNOSTIC,
                    null, null, null, unavailable.learnerMessage(), null, null);
        };
        return commitBoundary(interaction, idempotencyKey, requestHash);
    }

    /**
     * The Graph Run of an answer-submitted command: rehydrate the committed
     * state, reject a stale interaction version or an unknown Attempt, route
     * through the single legal Apply node for the Attempt's purpose, and stop
     * at the next committed Learner Interaction Boundary.
     */
    public ApplyFlowResult submitAnswer(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        AttemptPurpose purpose = artifactStore.findAttempt(attemptId)
                .map(TaskAttempt::purpose)
                .orElse(null);
        if (purpose == null) {
            return new ApplyFlowResult.SubmissionIgnored(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        }
        return switch (purpose) {
            case DIAGNOSTIC -> submitDiagnostic(state, attemptId, rawDerivative, confirmedCanonical, rationale,
                    idempotencyKey, requestHash);
            case INDEPENDENT_TEST -> submitIndependent(state, attemptId, rawDerivative, confirmedCanonical, rationale,
                    idempotencyKey, requestHash);
            case PRACTICE -> isTeachBackAttempt(attemptId)
                    ? submitTeachBack(state, attemptId, rawDerivative, confirmedCanonical,
                            idempotencyKey, requestHash)
                    : submitPractice(state, attemptId, rawDerivative, confirmedCanonical, rationale,
                            idempotencyKey, requestHash);
            default -> new ApplyFlowResult.SubmissionIgnored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
        };
    }

    /**
     * The Teach-back Attempt is Practice-purpose but lives behind a
     * teach-back task package; the graph discriminates by the package type so
     * an Apply Practice submission is never routed into the Teach-back node
     * and vice versa.
     */
    private boolean isTeachBackAttempt(UUID attemptId) {
        return artifactStore.findAttempt(attemptId)
                .map(TaskAttempt::taskPackageId)
                .flatMap(artifactStore::findTeachBackPackage)
                .isPresent();
    }

    /**
     * The Graph Run of a continue-requested command: rehydrate the committed
     * state, reject a stale interaction version, and only a teaching boundary
     * may be continued — the next legal move is the fresh Apply Practice
     * node. Continue on any other boundary is ignored without state change.
     */
    public ApplyFlowResult continueRequested(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        if (state.latestInteraction().teachingProjection() == null) {
            return new ApplyFlowResult.SubmissionIgnored(SubmissionIgnoreReason.CONTINUE_NOT_LEGAL);
        }
        return deliverPracticeBoundary(state, idempotencyKey, requestHash);
    }

    /**
     * The Graph Run of a hint-requested command: the Hint node generates and
     * gates the private ladder on the first request, exposes only the
     * requested legal level, and the run stops at the next committed Learner
     * Interaction Boundary. H1-H4 keep the Practice Attempt open for a later
     * formal submission; an H5 reveal atomically closes the Attempt as
     * Solution Revealed without Assessment or Evidence and delivers a fresh
     * verified Apply Practice task. A failed ladder exposes nothing and keeps
     * the open Attempt at a safe message boundary. Hints are never legal for
     * Diagnostic, Independent, Review, or Teach-back Attempts.
     */
    public ApplyFlowResult requestHint(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            boolean answerRequested,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        Optional<TaskAttempt> maybeAttempt = artifactStore.findAttempt(attemptId);
        if (maybeAttempt.isEmpty()) {
            return new ApplyFlowResult.HintIgnored(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        }
        HintResult result = hintFlow.requestHint(maybeAttempt.get(), answerRequested, idempotencyKey);
        return switch (result) {
            case HintResult.Revealed revealed -> revealBoundary(state, revealed, idempotencyKey, requestHash);
            case HintResult.Unavailable unavailable -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, attemptId, AttemptPurpose.PRACTICE,
                    projectionOf(attemptId), unavailable.learnerMessage(), null,
                    idempotencyKey, requestHash);
            case HintResult.Ignored ignored -> new ApplyFlowResult.HintIgnored(ignored.reason());
        };
    }

    private ApplyFlowResult revealBoundary(
            LearningState state,
            HintResult.Revealed revealed,
            UUID idempotencyKey,
            String requestHash
    ) {
        TaskAttempt attempt = revealed.attempt();
        if (attempt.isOpen()) {
            return boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, attempt.attemptId(), attempt.purpose(),
                    projectionOf(attempt), null, revealed.hint(), idempotencyKey, requestHash);
        }
        // The H5 reveal closes the attempt as Solution Revealed and becomes
        // the most recently exposed eligible Teach-back anchor; the graph
        // records it durably (idempotent per anchor id) before the Teach-back
        // node delivers the anchored short-text task.
        flowStore.recordAnchor(state.flow().flowId(),
                new TeachBackAnchor(
                        TeachBackAnchor.TeachBackAnchorKind.H5_SOLUTION_REVEAL,
                        attempt.attemptId(),
                        clock.instant()));
        return deliverTeachBackAfterReveal(state, revealed.hint(), idempotencyKey, requestHash);
    }

    /**
     * After an H5 reveal the closed Solution Revealed Attempt never becomes
     * Assessment or Evidence; the graph delivers the anchored Teach-back task
     * (the deterministic continuation of an H5 reveal), so the learner can
     * explain the shown method. A failed Teach-back delivery stops at a safe
     * boundary carrying the reveal and the neutral message.
     */
    private ApplyFlowResult deliverTeachBackAfterReveal(
            LearningState state,
            HintView hint,
            UUID idempotencyKey,
            String requestHash
    ) {
        TeachBackDeliveryResult delivery = teachBackFlow.deliverTeachBack(state.flow().flowId());
        return switch (delivery) {
            case TeachBackDeliveryResult.Delivered delivered -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, delivered.attempt().attemptId(),
                    delivered.attempt().purpose(), delivered.learnerProjection(),
                    TeachBackFlow.TEACH_BACK_AFTER_REVEAL_MESSAGE, hint,
                    idempotencyKey, requestHash);
            case TeachBackDeliveryResult.Unavailable unavailable -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, null, null, null,
                    unavailable.learnerMessage(), hint, idempotencyKey, requestHash);
        };
    }

    private LearnerProjection projectionOf(UUID attemptId) {
        return projectionOf(artifactStore.findAttempt(attemptId).orElseThrow());
    }

    private LearnerProjection projectionOf(TaskAttempt attempt) {
        return packageOf(attempt).learnerProjection();
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return artifactStore.findPackage(attempt.taskPackageId()).orElseThrow();
    }

    private ApplyFlowResult submitDiagnostic(
            LearningState state,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            UUID idempotencyKey,
            String requestHash
    ) {
        DiagnosticSubmissionResult result = diagnosticFlow.submitDiagnostic(
                state.flow().flowId(), attemptId, rawDerivative, confirmedCanonical, rationale);
        return switch (result) {
            // The Neutral Transition message is learner-visible content
            // (CONTEXT.md) and is projected on the boundary; the legacy Apply
            // seam dropped it at the interaction level. It states only the
            // next interaction and carries no feedback.
            case DiagnosticSubmissionResult.Passed passed -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, passed.independentAttempt().attemptId(),
                    passed.independentAttempt().purpose(), passed.independentLearnerProjection(),
                    passed.neutralTransitionMessage(), null, idempotencyKey, requestHash);
            case DiagnosticSubmissionResult.Inconclusive inconclusive -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, inconclusive.independentAttempt().attemptId(),
                    inconclusive.independentAttempt().purpose(), inconclusive.independentLearnerProjection(),
                    inconclusive.neutralTransitionMessage(), null, idempotencyKey, requestHash);
            case DiagnosticSubmissionResult.Failed failed ->
                    // A failed submitted Diagnostic stays closed and is never
                    // retroactively converted; the remediation cycle opens
                    // with the Explain teaching boundary, and a Continue
                    // command then delivers the fresh Apply Practice task.
                    deliverExplainBoundary(state, idempotencyKey, requestHash);
            case DiagnosticSubmissionResult.IndependentUnavailable unavailable -> boundary(
                    state, LearningStage.DIAGNOSTIC, null, null, null, unavailable.learnerMessage(), null,
                    idempotencyKey, requestHash);
            case DiagnosticSubmissionResult.NotSubmittable notSubmittable ->
                    new ApplyFlowResult.SubmissionRejected(notSubmittable.reason());
            case DiagnosticSubmissionResult.Ignored ignored ->
                    new ApplyFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    private ApplyFlowResult submitIndependent(
            LearningState state,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            UUID idempotencyKey,
            String requestHash
    ) {
        IndependentSubmissionResult result = independentFlow.submitIndependent(
                state.flow(), attemptId, rawDerivative, confirmedCanonical, rationale);
        return switch (result) {
            case IndependentSubmissionResult.EvidenceAccepted accepted -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, null, null, null,
                    accepted.learnerMessage(), null, idempotencyKey, requestHash);
            case IndependentSubmissionResult.NoEvidence noEvidence -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, null, null, null,
                    noEvidence.learnerMessage(), null, idempotencyKey, requestHash);
            case IndependentSubmissionResult.NotSubmittable notSubmittable ->
                    new ApplyFlowResult.SubmissionRejected(notSubmittable.reason());
            case IndependentSubmissionResult.Ignored ignored ->
                    new ApplyFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    /**
     * The Apply Practice node of one submission: a conclusive pass delivers
     * the fresh Independent Test — the only outcome that makes fresh
     * Independent testing legal in the current remediation cycle — a
     * conclusive fail or an Inconclusive judgment delivers a fresh Practice
     * task, and a failed follow-up generation stops at a terminal unavailable
     * boundary that keeps the accepted Evidence (or none) exactly as the
     * transition left it.
     */
    private ApplyFlowResult submitPractice(
            LearningState state,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            UUID idempotencyKey,
            String requestHash
    ) {
        PracticeSubmissionResult result = practiceFlow.submitPractice(
                state.flow(), attemptId, rawDerivative, confirmedCanonical, rationale);
        return switch (result) {
            case PracticeSubmissionResult.PracticePassed passed -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, passed.independentAttempt().attemptId(),
                    passed.independentAttempt().purpose(), passed.independentLearnerProjection(),
                    passed.learnerMessage(), null, idempotencyKey, requestHash);
            case PracticeSubmissionResult.PracticeFailed failed -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, failed.practiceAttempt().attemptId(),
                    failed.practiceAttempt().purpose(), failed.practiceLearnerProjection(),
                    failed.learnerMessage(), null, idempotencyKey, requestHash);
            case PracticeSubmissionResult.PracticeInconclusive inconclusive -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, inconclusive.practiceAttempt().attemptId(),
                    inconclusive.practiceAttempt().purpose(), inconclusive.practiceLearnerProjection(),
                    inconclusive.learnerMessage(), null, idempotencyKey, requestHash);
            case PracticeSubmissionResult.PracticeUnavailable unavailable -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, null, null, null,
                    unavailable.learnerMessage(), null, idempotencyKey, requestHash);
            case PracticeSubmissionResult.NotSubmittable notSubmittable ->
                    new ApplyFlowResult.SubmissionRejected(notSubmittable.reason());
            case PracticeSubmissionResult.Ignored ignored ->
                    new ApplyFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    /**
     * The deterministic remediation entry of an accepted Diagnostic failure:
     * the Explain node delivers one source-grounded teaching interaction, or a
     * terminal unavailable boundary when no teaching content can be prepared.
     * It never creates a Task Package, Attempt, Assessment, or Evidence. The
     * delivered worked example becomes the most recently exposed eligible
     * Teach-back anchor.
     */
    private ApplyFlowResult deliverExplainBoundary(
            LearningState state,
            UUID idempotencyKey,
            String requestHash
    ) {
        ExplainDeliveryResult delivery = explainFlow.deliverExplain(state.flow().flowId());
        return switch (delivery) {
            case ExplainDeliveryResult.Delivered delivered -> {
                flowStore.recordAnchor(state.flow().flowId(),
                        new TeachBackAnchor(
                                TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                                delivered.artifact().artifactId(),
                                clock.instant()));
                yield teachingBoundary(
                        state, delivered.artifact().learnerProjection(), ExplainFlow.EXPLAIN_START_MESSAGE,
                        idempotencyKey, requestHash);
            }
            case ExplainDeliveryResult.Unavailable unavailable -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, null, null, null,
                    unavailable.learnerMessage(), null, idempotencyKey, requestHash);
        };
    }

    /**
     * The Teach-back node of one submission: a conclusive pass or fail
     * accepts exactly one understanding-dimension Evidence record and
     * delivers a fresh verified Apply Practice task — never a fresh
     * Independent Test, because a Teach-back pass does not satisfy Apply
     * Practice readiness — while an Inconclusive judgment creates no Evidence
     * and delivers a fresh Teach-back replacement.
     */
    private ApplyFlowResult submitTeachBack(
            LearningState state,
            UUID attemptId,
            String rawText,
            String confirmedText,
            UUID idempotencyKey,
            String requestHash
    ) {
        TeachBackSubmissionResult result = teachBackFlow.submitTeachBack(
                state.flow(), attemptId, rawText, confirmedText);
        return switch (result) {
            case TeachBackSubmissionResult.Passed passed -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, passed.followUpAttempt().attemptId(),
                    passed.followUpAttempt().purpose(), passed.followUpLearnerProjection(),
                    passed.learnerMessage(), null, idempotencyKey, requestHash);
            case TeachBackSubmissionResult.Failed failed -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, failed.followUpAttempt().attemptId(),
                    failed.followUpAttempt().purpose(), failed.followUpLearnerProjection(),
                    failed.learnerMessage(), null, idempotencyKey, requestHash);
            case TeachBackSubmissionResult.Inconclusive inconclusive -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE,
                    inconclusive.replacementAttempt().attemptId(),
                    inconclusive.replacementAttempt().purpose(),
                    inconclusive.replacementLearnerProjection(),
                    inconclusive.learnerMessage(), null, idempotencyKey, requestHash);
            case TeachBackSubmissionResult.Unavailable unavailable -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, null, null, null,
                    unavailable.learnerMessage(), null, idempotencyKey, requestHash);
            case TeachBackSubmissionResult.Ignored ignored ->
                    new ApplyFlowResult.SubmissionIgnored(ignored.reason());
            case TeachBackSubmissionResult.NotSubmittable notSubmittable ->
                    new ApplyFlowResult.SubmissionRejected(notSubmittable.reason());
        };
    }

    /**
     * The Apply Practice node entry used after an Explain teaching boundary
     * has been continued: it delivers a fresh verified task over the frozen
     * Practice Blueprint, or a terminal unavailable boundary when no task can
     * be prepared.
     */
    private ApplyFlowResult deliverPracticeBoundary(
            LearningState state,
            UUID idempotencyKey,
            String requestHash
    ) {
        ApplyDeliveryResult delivery = practiceFlow.deliverPractice(state.flow().flowId());
        return switch (delivery) {
            case ApplyDeliveryResult.Delivered delivered -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, delivered.attempt().attemptId(),
                    delivered.attempt().purpose(), delivered.learnerProjection(),
                    PracticeSubmissionFlow.PRACTICE_START_MESSAGE, null, idempotencyKey, requestHash);
            case ApplyDeliveryResult.Unavailable unavailable -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, null, null, null,
                    unavailable.learnerMessage(), null, idempotencyKey, requestHash);
        };
    }

    private ApplyFlowResult.Boundary boundary(
            LearningState state,
            LearningStage stage,
            UUID attemptId,
            AttemptPurpose purpose,
            LearnerProjection learnerProjection,
            String learnerMessage,
            HintView hint,
            UUID idempotencyKey,
            String requestHash
    ) {
        FlowStatus status = learnerProjection == null ? FlowStatus.TERMINAL : FlowStatus.AWAITING_LEARNER_INPUT;
        ApplyFlowInteraction interaction = new ApplyFlowInteraction(
                state.flow().flowId(), state.latestInteraction().interactionVersion() + 1, status, stage,
                attemptId, purpose, learnerProjection, learnerMessage, null, hint);
        return commitBoundary(interaction, idempotencyKey, requestHash);
    }

    /**
     * The teaching Learner Interaction Boundary of an Explain node: the flow
     * pauses awaiting learner input with the learner-visible teaching
     * projection, no Task Attempt, and the Explain start message.
     */
    private ApplyFlowResult.Boundary teachingBoundary(
            LearningState state,
            TeachingProjection teachingProjection,
            String learnerMessage,
            UUID idempotencyKey,
            String requestHash
    ) {
        ApplyFlowInteraction interaction = new ApplyFlowInteraction(
                state.flow().flowId(), state.latestInteraction().interactionVersion() + 1,
                FlowStatus.AWAITING_LEARNER_INPUT, LearningStage.LEARNING_AND_PRACTICE,
                null, null, null, learnerMessage, teachingProjection, null);
        return commitBoundary(interaction, idempotencyKey, requestHash);
    }

    /**
     * The single durable commit of one Learner Interaction Boundary: the
     * learner-visible interaction, its checkpoint, and the processed command
     * persist atomically, so a replay always returns the original result and a
     * restart resumes exactly from this point.
     */
    private ApplyFlowResult.Boundary commitBoundary(
            ApplyFlowInteraction interaction,
            UUID idempotencyKey,
            String requestHash
    ) {
        flowStore.commitBoundary(
                interaction,
                new ApplyCheckpoint(UUID.randomUUID(), interaction.flowId(),
                        interaction.interactionVersion(), clock.instant()),
                new ProcessedCommand(idempotencyKey, requestHash, interaction.flowId(),
                        interaction, clock.instant()));
        return new ApplyFlowResult.Boundary(interaction);
    }
}
