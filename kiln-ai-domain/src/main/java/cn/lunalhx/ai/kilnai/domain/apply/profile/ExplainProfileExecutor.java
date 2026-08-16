package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import cn.lunalhx.ai.kilnai.domain.apply.gate.ExplainGatePolicy;
import cn.lunalhx.ai.kilnai.domain.apply.gate.ExplainGenerationDraftGatePolicy;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExplainGenerationPort;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.util.Objects;
import java.util.Optional;

/**
 * The bounded Explain node executor: one initial generation and at most one
 * same-plan repair under the frozen Skill Stack. Each candidate is parsed
 * against the closed {@code explain_generation/v1} contract, validated by the
 * draft gate, assembled into the teaching artifact, and validated by the
 * Explain Output Gate. A Source Gap ends the cycle immediately; a repeated
 * invalid result becomes Node Execution Failed and persists nothing. No model
 * Task Verifier runs for Explain.
 */
public final class ExplainProfileExecutor {

    private static final int MAX_GENERATION_CYCLES = 2;

    private final BundleStack stack;
    private final ExplainGenerationPort generationPort;
    private final ExplainPromptCompiler compiler;
    private final ExplainArtifactAssembler assembler;
    private final TypedArtifactGatePipeline gatePipeline;

    public ExplainProfileExecutor(BundleStack stack, ExplainGenerationPort generationPort) {
        this.stack = Objects.requireNonNull(stack, "stack must not be null");
        this.generationPort = Objects.requireNonNull(generationPort, "generationPort must not be null");
        this.compiler = new ExplainPromptCompiler();
        this.assembler = new ExplainArtifactAssembler();
        this.gatePipeline = new TypedArtifactGatePipeline();
    }

    public ExplainDeliveryResult deliver(ExplainExecutionContext context) {
        PreparedExplain prepared = prepareTeaching(context);
        return switch (prepared) {
            case PreparedExplain.TeachingReady ready ->
                    new ExplainDeliveryResult.Delivered(ready.artifact());
            case PreparedExplain.Unavailable unavailable -> new ExplainDeliveryResult.Unavailable(
                    unavailable.reason(), unavailable.learnerMessage());
        };
    }

    /**
     * Runs the bounded generation and Output Gate cycles without persisting
     * anything. A ready candidate returns the validated teaching artifact so
     * the caller can durably bind it to its own state transition; Source Gap
     * or exhausted cycles return an unavailable outcome.
     */
    public PreparedExplain prepareTeaching(ExplainExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        validateContextCoverage(context, stack);
        String systemPrompt = compiler.compile(stack);
        String contextJson = compiler.serializeContext(context);
        for (int cycle = 1; cycle <= MAX_GENERATION_CYCLES; cycle++) {
            String raw = generationPort.generate(systemPrompt, contextJson);
            Optional<PreparedExplain> outcome = handleCandidate(context, stack, raw);
            if (outcome.isPresent()) {
                return outcome.get();
            }
        }
        return new PreparedExplain.Unavailable(
                ExplainUnavailableReason.NODE_EXECUTION_FAILED,
                ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE);
    }

    private Optional<PreparedExplain> handleCandidate(
            ExplainExecutionContext context,
            BundleStack stack,
            String raw
    ) {
        ExplainGenerationDraft draft;
        try {
            draft = ExplainGenerationDraft.parse(raw);
        } catch (ApplyDraftException exception) {
            return Optional.empty();
        }
        if (draft instanceof ExplainGenerationDraft.SourceGap) {
            return Optional.of(new PreparedExplain.Unavailable(
                    ExplainUnavailableReason.SOURCE_GAP,
                    ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE));
        }
        ExplainGenerationDraft.TeachingReady teachingReady = (ExplainGenerationDraft.TeachingReady) draft;
        GateResult<ExplainGenerationDraft.TeachingReady> draftGate = gatePipeline.validate(
                teachingReady, new ExplainGenerationDraftGatePolicy(context), GateContext.empty());
        if (draftGate.outcome() != GateOutcome.PASSED) {
            return Optional.empty();
        }
        Optional<ExplainTeachingArtifact> artifact = assembler.assemble(context, teachingReady, stack);
        if (artifact.isEmpty()) {
            return Optional.empty();
        }
        GateResult<ExplainTeachingArtifact> artifactGate = gatePipeline.validate(
                artifact.get(), new ExplainGatePolicy(context, stack.pinnedIds()), GateContext.empty());
        if (artifactGate.outcome() != GateOutcome.PASSED) {
            return Optional.empty();
        }
        return Optional.of(new PreparedExplain.TeachingReady(artifact.get()));
    }

    private void validateContextCoverage(ExplainExecutionContext context, BundleStack stack) {
        java.util.Set<String> provided = java.util.Set.of(
                "concept_contract",
                "mastery_rubric",
                "pedagogy_intent",
                "concept_source_pack",
                "novelty_exclusions",
                "learner_locale"
        );
        for (SkillBundle bundle : stack.bundles()) {
            if (!provided.containsAll(bundle.manifest().requiresContext())) {
                throw new CapabilityGap(
                        "bundle " + bundle.pinnedId() + " requires context not supplied by the execution context");
            }
        }
    }

    /**
     * The closed outcome of {@link #prepareTeaching(ExplainExecutionContext)}:
     * a validated teaching artifact, or an unavailable reason with the shared
     * neutral learner message. Nothing is persisted for the unavailable
     * outcome.
     */
    public sealed interface PreparedExplain
            permits PreparedExplain.TeachingReady, PreparedExplain.Unavailable {

        record TeachingReady(ExplainTeachingArtifact artifact) implements PreparedExplain {

            public TeachingReady {
                Objects.requireNonNull(artifact, "artifact must not be null");
            }
        }

        record Unavailable(ExplainUnavailableReason reason, String learnerMessage) implements PreparedExplain {

            public Unavailable {
                Objects.requireNonNull(reason, "reason must not be null");
                Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
            }
        }
    }
}
