package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.FlowCommandReplay;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.util.Objects;
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

    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final LearningStateGraph graph;
    private final ApplyExecutionContext diagnosticContext;
    private final Clock clock;

    public LearningFlowCommandUseCase(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            LearningStateGraph graph,
            ApplyExecutionContext diagnosticContext,
            Clock clock
    ) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.graph = Objects.requireNonNull(graph, "graph must not be null");
        this.diagnosticContext = Objects.requireNonNull(diagnosticContext, "diagnosticContext must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ApplyFlowResult start(UUID learnerId, UUID idempotencyKey) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        String hash = ApplyHash.sha256HexDelimited("start", learnerId);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new ApplyFlowResult.Boundary(interaction),
                () -> {
                    UUID flowId = UUID.randomUUID();
                    flowStore.insertFlow(new LearningFlowStore.FlowRecord(
                            flowId, learnerId, DiagnosticApplyFixture.CONCEPT_ID,
                            FlowStatus.READY, LearningStage.DIAGNOSTIC, clock.instant()));
                    saveSourcePack();
                    return graph.start(flowId, idempotencyKey, hash);
                });
    }

    public ApplyFlowResult submitAnswer(
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
                interaction -> new ApplyFlowResult.Boundary(interaction),
                () -> graph.submitAnswer(flowId, interactionVersion, attemptId, rawDerivative,
                        confirmedCanonical, rationale, idempotencyKey, hash));
    }

    public ApplyFlowInteraction query(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        return flowStore.latestInteraction(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
    }

    private void saveSourcePack() {
        ApplyExecutionContext.ConceptSourcePack pack = diagnosticContext.conceptSourcePack();
        artifactStore.saveSource(new SourceArtifact(pack.id(), pack.version(), pack.passages()));
    }

    private void requireUuidKey(UUID key) {
        if (key == null) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key is required");
        }
    }
}
