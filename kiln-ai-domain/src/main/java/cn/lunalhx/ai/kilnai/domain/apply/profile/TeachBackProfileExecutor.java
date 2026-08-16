package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import cn.lunalhx.ai.kilnai.domain.apply.gate.TeachBackTaskPackageGatePolicy;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackTaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.util.Objects;
import java.util.Optional;

/**
 * The bounded Teach-back node executor: one initial generation and at most
 * one same-plan repair through the strict {@code teach_back_generation/v1}
 * parser and the Teach-back Output Gate, then isolated Task Verification. A
 * Source Gap ends the cycle immediately; a verifier rejection or inconclusive
 * result discards the candidate and permits one fresh second candidate;
 * exhaustion produces Task Generation Exhausted with no Attempt, exposure, or
 * Evidence. Only a fully validated package opens its Practice-purpose Attempt
 * through the caller's durable store.
 */
public final class TeachBackProfileExecutor {

    private final BundleStack stack;
    private final TeachBackGenerationPort generationPort;
    private final TeachBackTaskVerifierPort verifierPort;
    private final ArtifactStore artifactStore;
    private final TeachBackPromptCompiler compiler;
    private final TeachBackTaskAssembler assembler;
    private final TypedArtifactGatePipeline gatePipeline;

    public TeachBackProfileExecutor(
            BundleStack stack,
            TeachBackGenerationPort generationPort,
            TeachBackTaskVerifierPort verifierPort,
            ArtifactStore artifactStore
    ) {
        this.stack = Objects.requireNonNull(stack, "stack must not be null");
        this.generationPort = Objects.requireNonNull(generationPort, "generationPort must not be null");
        this.verifierPort = Objects.requireNonNull(verifierPort, "verifierPort must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.compiler = new TeachBackPromptCompiler();
        this.assembler = new TeachBackTaskAssembler();
        this.gatePipeline = new TypedArtifactGatePipeline();
    }

    /**
     * Delivers one verified Teach-back task and durably opens its Attempt
     * through the caller's store, or returns an unavailable outcome that
     * persists nothing but the verification audit records of rejected
     * candidates.
     */
    public TeachBackDeliveryResult deliver(TeachBackExecutionContext context) {
        PreparedDelivery prepared = prepareTask(context);
        return switch (prepared) {
            case PreparedDelivery.TaskReady ready -> {
                TaskAttempt attempt = artifactStore.openAttempt(ready.taskPackage());
                yield new TeachBackDeliveryResult.Delivered(attempt, ready.taskPackage().learnerProjection());
            }
            case PreparedDelivery.Unavailable unavailable -> new TeachBackDeliveryResult.Unavailable(
                    unavailable.reason(), unavailable.learnerMessage());
        };
    }

    /**
     * Runs the full bounded generation, Output Gate, and isolated Task
     * Verification cycles without persisting a Task Package or opening a Task
     * Attempt. A ready candidate returns the verified package so the caller
     * can durably bind it to its own state transition. The spec grants two
     * distinct bounded retries: one same-plan repair for a rejected draft and
     * one fresh second candidate after a verifier rejection; either may be
     * used, and exhaustion after both produces Task Generation Exhausted.
     */
    public PreparedDelivery prepareTask(TeachBackExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        validateContextCoverage(context, stack);
        String systemPrompt = compiler.compile(stack);
        String contextJson = compiler.serializeContext(context);
        boolean repairUsed = false;
        boolean freshCandidateUsed = false;
        while (true) {
            String raw = generationPort.generate(systemPrompt, contextJson);
            Outcome outcome = handleCandidate(context, raw);
            if (outcome instanceof Outcome.Ready ready) {
                return new PreparedDelivery.TaskReady(ready.taskPackage());
            }
            if (outcome instanceof Outcome.Unavailable unavailable) {
                return new PreparedDelivery.Unavailable(unavailable.reason(), unavailable.learnerMessage());
            }
            Outcome.Rejected rejected = (Outcome.Rejected) outcome;
            if (rejected.verifierRejected() && !freshCandidateUsed) {
                freshCandidateUsed = true;
                continue;
            }
            if (!rejected.verifierRejected() && !repairUsed) {
                repairUsed = true;
                continue;
            }
            return new PreparedDelivery.Unavailable(
                    TeachBackUnavailableReason.TASK_GENERATION_EXHAUSTED,
                    TeachBackDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE);
        }
    }

    private Outcome handleCandidate(TeachBackExecutionContext context, String raw) {
        TeachBackGenerationDraft draft;
        try {
            draft = TeachBackGenerationDraft.parse(raw);
        } catch (ApplyDraftException exception) {
            return new Outcome.Rejected(false);
        }
        if (draft instanceof TeachBackGenerationDraft.SourceGap) {
            return new Outcome.Unavailable(
                    TeachBackUnavailableReason.SOURCE_GAP,
                    TeachBackDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE);
        }
        TeachBackGenerationDraft.TaskReady taskReady = (TeachBackGenerationDraft.TaskReady) draft;
        TeachBackTaskPackage taskPackage = assembler.assemble(taskReady, stack, context.learnerLocale());
        GateResult<TeachBackTaskPackage> packageGate = gatePipeline.validate(
                taskPackage,
                new TeachBackTaskPackageGatePolicy(context, stack.pinnedIds()),
                GateContext.empty());
        if (packageGate.outcome() != GateOutcome.PASSED) {
            return new Outcome.Rejected(false);
        }
        TaskVerificationVerdict verdict = verifierPort.verify(taskPackage, context);
        artifactStore.recordTaskVerification(taskPackage.taskPackageId(), verdict);
        if (!verdict.passed()) {
            return new Outcome.Rejected(true);
        }
        return new Outcome.Ready(taskPackage);
    }

    private sealed interface Outcome permits Outcome.Ready, Outcome.Rejected, Outcome.Unavailable {

        record Ready(TeachBackTaskPackage taskPackage) implements Outcome {
        }

        record Rejected(boolean verifierRejected) implements Outcome {
        }

        record Unavailable(TeachBackUnavailableReason reason, String learnerMessage) implements Outcome {
        }
    }

    private void validateContextCoverage(TeachBackExecutionContext context, BundleStack stack) {
        java.util.Set<String> provided = java.util.Set.of(
                "concept_contract",
                "mastery_rubric",
                "pedagogy_intent",
                "anchor",
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
     * The closed outcome of {@link #prepareTask(TeachBackExecutionContext)}:
     * a verified ready Teach-back task package, or an unavailable reason with
     * the shared neutral learner message. Nothing is persisted for the
     * unavailable outcome, and the caller owns opening the attempt for a
     * ready package.
     */
    public sealed interface PreparedDelivery
            permits PreparedDelivery.TaskReady, PreparedDelivery.Unavailable {

        record TaskReady(TeachBackTaskPackage taskPackage) implements PreparedDelivery {

            public TaskReady {
                Objects.requireNonNull(taskPackage, "taskPackage must not be null");
            }
        }

        record Unavailable(TeachBackUnavailableReason reason, String learnerMessage) implements PreparedDelivery {

            public Unavailable {
                Objects.requireNonNull(reason, "reason must not be null");
                Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
            }
        }
    }
}
