package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.util.Objects;
import java.util.UUID;

/**
 * The Explain node flow: a pure teaching action that delivers one validated
 * teaching artifact, persists it durably, and records its example Fingerprint
 * in the Flow's exposure ledger. It creates no Task Package, Attempt,
 * Assessment, or Learning Evidence; a Source Gap or a repeated invalid
 * envelope persists nothing and returns the neutral unavailable message so the
 * graph stops at a safe terminal boundary. Each invocation carries the
 * guarded decision's intent and sanitized Feedback Facts, which are projected
 * into the execution context's pedagogy intent without exposing raw answers,
 * expected answers, or assessment reasoning.
 */
public final class ExplainFlow {

    public static final String EXPLAIN_START_MESSAGE = "请先阅读下面的原则讲解与完整例题。";

    private final ExplainProfileExecutor executor;
    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final ExplainExecutionContext contextTemplate;

    public ExplainFlow(
            ExplainProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            ExplainExecutionContext contextTemplate
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.contextTemplate = Objects.requireNonNull(contextTemplate, "contextTemplate must not be null");
    }

    /**
     * Delivers a fresh teaching artifact excluding every worked example
     * already exposed in the Flow, persists the artifact, and records its
     * example Fingerprint for later freshness checks. Called by the Graph
     * when the guarded decision selects Explain after an accepted
     * Diagnostic, Practice, or Teach-back failure.
     */
    public ExplainDeliveryResult deliverExplain(UUID flowId, String intent, FeedbackFacts facts) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(intent, "intent must not be null");
        Objects.requireNonNull(facts, "facts must not be null");
        ExplainExecutionContext context = contextTemplate
                .withPedagogyIntent(new ExplainExecutionContext.PedagogyIntent(
                        intent,
                        facts.satisfiedCriteria(),
                        facts.missingCriteria(),
                        facts.errorDimensions()))
                .withNoveltyExclusions(new ExplainExecutionContext.NoveltyExclusions(
                        flowStore.exposedExampleFingerprints(flowId),
                        flowStore.exposedHintLadderFingerprints(flowId),
                        flowStore.exposedRevealedSolutionFingerprints(flowId)));
        ExplainDeliveryResult result = executor.deliver(context);
        if (result instanceof ExplainDeliveryResult.Delivered delivered) {
            artifactStore.saveExplainArtifact(flowId, delivered.artifact());
            flowStore.recordExampleExposure(flowId, delivered.artifact().exampleFingerprint().value());
        }
        return result;
    }
}
