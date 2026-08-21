package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.FlowCommandReplay;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.AcceptedDiagnosticPlanPort;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlan;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ActiveWorkConflictException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The public domain Learning Flow command seam (the first-half of the single
 * command surface the Learning/Practice reference restores). Every command
 * reuses the shared {@link FlowCommandReplay} boundary: a replayed
 * Idempotency-Key returns the original committed interaction, a key reused
 * with a different payload conflicts, and the graph step itself runs only
 * when the command was never processed. Start creates the durable Flow and
 * asks the Learning StateGraph for its first Graph Run; answer submission is
 * delegated to the graph's routed node execution. Learner-visible responses
 * are always projections of committed durable state.
 */
public final class LearningFlowCommandUseCase {

    private final LearningFlowStore flowStore;
    private final LearningStateGraph graph;
    private final ApplyExecutionContext diagnosticContext;
    private final AcceptedDiagnosticPlanPort acceptedDiagnosticPlanPort;
    private final OperatorModelProfilePort modelProfilePort;

    public LearningFlowCommandUseCase(
            LearningFlowStore flowStore,
            LearningStateGraph graph,
            ApplyExecutionContext diagnosticContext,
            AcceptedDiagnosticPlanPort acceptedDiagnosticPlanPort,
            OperatorModelProfilePort modelProfilePort
    ) {
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.graph = Objects.requireNonNull(graph, "graph must not be null");
        this.diagnosticContext = Objects.requireNonNull(diagnosticContext, "diagnosticContext must not be null");
        this.acceptedDiagnosticPlanPort = Objects.requireNonNull(
                acceptedDiagnosticPlanPort, "acceptedDiagnosticPlanPort must not be null");
        this.modelProfilePort = Objects.requireNonNull(modelProfilePort, "modelProfilePort must not be null");
    }

    public LearningFlowResult start(UUID learnerId, UUID idempotencyKey) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        String hash = ApplyHash.sha256HexDelimited("start", learnerId);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new LearningFlowResult.Boundary(interaction),
                () -> {
                    UUID conceptId = DiagnosticApplyFixture.CONCEPT_ID;
                    Optional<UUID> activeWork = flowStore.activeWorkFlowId(learnerId, conceptId);
                    if (activeWork.isPresent()) {
                        throw new ActiveWorkConflictException(activeWork.get());
                    }
                    DiagnosticPlan diagnosticPlan = acceptedDiagnosticPlanPort.acceptedFor(conceptId)
                            .orElseThrow(() -> new ApplicationException(
                                    ErrorCode.SERVICE_UNAVAILABLE, LearningStateGraph.START_UNAVAILABLE_MESSAGE));
                    ModelProfile profile = resolveProfile();
                    UUID flowId = UUID.randomUUID();
                    // Starting a Learning Flow freezes the operator's current
                    // Model Profile onto the Flow (ADR-0035, ADR-0037): the
                    // resolved snapshot is recorded with the Flow and every
                    // later model call uses it, never the current defaults.
                    // The Flow, Source Pack, Package, Attempt, exposure,
                    // checkpoint, interaction, and processed command commit
                    // atomically only after the Diagnostic was fully prepared
                    // (ADR-0063); a failed preparation persists nothing and
                    // the client reuses the original Idempotency-Key.
                    return graph.start(flowId, learnerId, conceptId, profile, diagnosticPlan,
                            sourceArtifact(), idempotencyKey, hash);
                });
    }

    public LearningFlowResult submitAnswer(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(rawDerivative, "rawDerivative must not be null");
        String hash = ApplyHash.sha256HexDelimited("submit", flowId, interactionVersion, attemptId,
                rawDerivative, confirmedCanonical, rationale);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new LearningFlowResult.Boundary(interaction),
                () -> graph.submitAnswer(flowId, interactionVersion, attemptId, rawDerivative,
                        confirmedCanonical, rationale, idempotencyKey, hash));
    }

    public LearningFlowResult continueRequested(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey
    ) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(flowId, "flowId must not be null");
        String hash = ApplyHash.sha256HexDelimited("continue", flowId, interactionVersion);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new LearningFlowResult.Boundary(interaction),
                () -> graph.continueRequested(flowId, interactionVersion, idempotencyKey, hash));
    }

    public LearningFlowInteraction query(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        return flowStore.latestInteraction(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
    }

    /**
     * The hint-requested command of the closed learning command surface: it
     * reuses the shared idempotency-replay boundary and the graph's Hint node.
     * An explicit request for the answer jumps to H5; a regular request
     * reveals the next persisted level of the attempt's stable ladder.
     */
    public LearningFlowResult requestHint(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            boolean answerRequested,
            UUID idempotencyKey
    ) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        String hash = ApplyHash.sha256HexDelimited(
                "hint", flowId, interactionVersion, attemptId, answerRequested);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new LearningFlowResult.Boundary(interaction),
                () -> graph.requestHint(flowId, interactionVersion, attemptId, answerRequested,
                        idempotencyKey, hash));
    }

    /**
     * The clarification-asked command of the closed learning command surface:
     * the Clarification Gate classifies the free-form message and the graph
     * routes the answer — a direct procedural restatement, a refusal without
     * teaching content, a temporary Explain inside the open Practice Attempt,
     * or an assistance-consent request over an open Independent or Review
     * Attempt. The {@code attemptId} is nullable because a standalone Explain
     * clarification addresses the current Interaction Boundary without an
     * Attempt ID (spec).
     */
    public LearningFlowResult clarificationAsked(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            String message,
            UUID idempotencyKey
    ) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(message, "message must not be null");
        String hash = ApplyHash.sha256HexDelimited(
                "clarify", flowId, interactionVersion, attemptId, message);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new LearningFlowResult.Boundary(interaction),
                () -> graph.clarificationAsked(flowId, interactionVersion, attemptId, message,
                        idempotencyKey, hash));
    }

    /**
     * The assistance-decided command of the closed learning command surface:
     * the learner's explicit accept or refuse of an assistance-consent
     * request over an open Independent or Review Attempt. Refusal preserves
     * the attempt unchanged; acceptance converts it one-way to Practice
     * before any assistance content is exposed, cancelling the started
     * Review Task when the attempt was a Review.
     */
    public LearningFlowResult assistanceDecided(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            boolean accept,
            UUID idempotencyKey
    ) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        String hash = ApplyHash.sha256HexDelimited(
                "assist", flowId, interactionVersion, attemptId, accept);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new LearningFlowResult.Boundary(interaction),
                () -> graph.assistanceDecided(flowId, interactionVersion, attemptId, accept,
                        idempotencyKey, hash));
    }

    /**
     * The flow-control-requested command of the closed learning command
     * surface: the learner explicitly leaves the Flow. Any open Attempt is
     * closed as Abandoned (ADR-0015) — no submission, Assessment, Evidence,
     * or Milestone change — and the run stops at the terminal leave
     * transition boundary. The command reuses the shared idempotency-replay
     * boundary, so a replayed leave always returns its original committed
     * transition.
     */
    public LearningFlowResult flowControlRequested(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey
    ) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(flowId, "flowId must not be null");
        String hash = ApplyHash.sha256HexDelimited("flow-control", flowId, interactionVersion);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new LearningFlowResult.Boundary(interaction),
                () -> graph.flowControlRequested(flowId, interactionVersion, idempotencyKey, hash));
    }

    /**
     * The retry-requested command of the closed learning command surface: it
     * is legal only on an {@code unavailable} Interaction Boundary, carries
     * no learner answer or original command body, and resumes the durable
     * Pending Operation saved by the server (ADR-0069). A new Idempotency-Key
     * identifies each explicit retry; a replayed key returns the original
     * committed interaction.
     */
    public LearningFlowResult retryRequested(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey
    ) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(flowId, "flowId must not be null");
        String hash = ApplyHash.sha256HexDelimited("retry", flowId, interactionVersion);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new LearningFlowResult.Boundary(interaction),
                () -> graph.retryRequested(flowId, interactionVersion, idempotencyKey, hash));
    }

    /**
     * Resolves the operator-owned Model Profile for a new Flow. Any
     * configuration or provider failure is an initial preparation failure:
     * it persists nothing and surfaces as the generic 503, never an
     * implementation detail.
     */
    private ModelProfile resolveProfile() {
        try {
            return modelProfilePort.resolve();
        } catch (RuntimeException exception) {
            throw new ApplicationException(
                    ErrorCode.SERVICE_UNAVAILABLE, LearningStateGraph.START_UNAVAILABLE_MESSAGE);
        }
    }

    private SourceArtifact sourceArtifact() {
        ApplyExecutionContext.ConceptSourcePack pack = diagnosticContext.conceptSourcePack();
        return new SourceArtifact(pack.id(), pack.version(), pack.passages());
    }

    private void requireUuidKey(UUID key) {
        if (key == null) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key is required");
        }
    }
}
