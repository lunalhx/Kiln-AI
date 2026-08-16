package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import cn.lunalhx.ai.kilnai.domain.apply.gate.ApplyGenerationDraftGatePolicy;
import cn.lunalhx.ai.kilnai.domain.apply.gate.ApplyTaskPackageGatePolicy;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.util.Objects;
import java.util.Optional;

public final class ApplyProfileExecutor {

    private static final int MAX_GENERATION_CYCLES = 2;

    private final BundleStack stack;
    private final ApplyGenerationPort generationPort;
    private final TaskVerifierPort verifierPort;
    private final ArtifactStore artifactStore;
    private final ApplyPromptCompiler compiler;
    private final TaskPackageAssembler assembler;
    private final TypedArtifactGatePipeline gatePipeline;

    public ApplyProfileExecutor(
            BundleStack stack,
            ApplyGenerationPort generationPort,
            TaskVerifierPort verifierPort,
            ArtifactStore artifactStore
    ) {
        this.stack = Objects.requireNonNull(stack, "stack must not be null");
        this.generationPort = Objects.requireNonNull(generationPort, "generationPort must not be null");
        this.verifierPort = Objects.requireNonNull(verifierPort, "verifierPort must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.compiler = new ApplyPromptCompiler();
        this.assembler = new TaskPackageAssembler();
        this.gatePipeline = new TypedArtifactGatePipeline();
    }

    public ApplyDeliveryResult deliver(ModelProfile profile, ApplyExecutionContext context) {
        PreparedDelivery prepared = prepareTask(profile, context);
        return switch (prepared) {
            case PreparedDelivery.TaskReady ready -> {
                TaskAttempt attempt = artifactStore.openAttempt(ready.taskPackage());
                yield new ApplyDeliveryResult.Delivered(attempt, ready.taskPackage().learnerProjection());
            }
            case PreparedDelivery.Unavailable unavailable -> new ApplyDeliveryResult.Unavailable(
                    unavailable.reason(), unavailable.learnerMessage());
        };
    }

    /**
     * Runs the full bounded generation, Output Gate, and Task Verification
     * cycles without persisting a Task Package or opening a Task Attempt. A
     * ready candidate returns the verified Task Package so the caller can
     * durably bind it to its own state transition; Source Gap or exhausted
     * generation returns an unavailable outcome and persists nothing but the
     * verification audit records of rejected candidates.
     */
    public PreparedDelivery prepareTask(ModelProfile profile, ApplyExecutionContext context) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(context, "context must not be null");
        validateContextCoverage(context, stack);
        String systemPrompt = compiler.compile(stack);
        String contextJson = compiler.serializeContext(context);
        for (int cycle = 1; cycle <= MAX_GENERATION_CYCLES; cycle++) {
            String raw = generationPort.generate(profile, systemPrompt, contextJson);
            Optional<PreparedDelivery> outcome = handleCandidate(profile, context, stack, raw, cycle - 1);
            if (outcome.isPresent()) {
                return outcome.get();
            }
        }
        return new PreparedDelivery.Unavailable(
                TaskUnavailableReason.TASK_GENERATION_EXHAUSTED, ApplyDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE);
    }

    private Optional<PreparedDelivery> handleCandidate(
            ModelProfile profile,
            ApplyExecutionContext context,
            BundleStack stack,
            String raw,
            int repairCount
    ) {
        ApplyGenerationDraft draft;
        try {
            draft = ApplyGenerationDraft.parse(raw);
        } catch (ApplyDraftException exception) {
            return Optional.empty();
        }
        if (draft instanceof ApplyGenerationDraft.SourceGap) {
            return Optional.of(new PreparedDelivery.Unavailable(
                    TaskUnavailableReason.SOURCE_GAP, ApplyDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE));
        }
        ApplyGenerationDraft.TaskReady taskReady = (ApplyGenerationDraft.TaskReady) draft;
        GateResult<ApplyGenerationDraft.TaskReady> draftGate = gatePipeline.validate(
                taskReady, new ApplyGenerationDraftGatePolicy(context), GateContext.empty());
        if (draftGate.outcome() != GateOutcome.PASSED) {
            return Optional.empty();
        }
        Optional<TaskPackage> assembled = assembler.assemble(context, taskReady, stack,
                ModelExecution.from(profile, ApplyPromptCompiler.INSTRUCTION_BUDGET, repairCount));
        if (assembled.isEmpty()) {
            return Optional.empty();
        }
        TaskPackage taskPackage = assembled.get();
        GateResult<TaskPackage> packageGate = gatePipeline.validate(
                taskPackage,
                new ApplyTaskPackageGatePolicy(context, stack.pinnedIds(), profile),
                GateContext.empty());
        if (packageGate.outcome() != GateOutcome.PASSED) {
            return Optional.empty();
        }
        TaskVerificationVerdict verdict = verifierPort.verify(profile, taskPackage, context);
        artifactStore.recordTaskVerification(taskPackage.taskPackageId(), verdict);
        if (!verdict.passed()) {
            return Optional.empty();
        }
        return Optional.of(new PreparedDelivery.TaskReady(taskPackage));
    }

    private void validateContextCoverage(ApplyExecutionContext context, BundleStack stack) {
        java.util.Set<String> provided = java.util.Set.of(
                "concept_contract",
                "mastery_rubric",
                "task_blueprint",
                "concept_source_pack",
                "novelty_exclusions",
                "answer_representation_contract",
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
     * The closed outcome of {@link #prepareTask(ApplyExecutionContext)}: a
     * verified ready Task Package, or an unavailable reason with the shared
     * neutral learner message. Nothing is persisted for the unavailable
     * outcome, and the caller owns opening the attempt for a ready package.
     */
    public sealed interface PreparedDelivery
            permits PreparedDelivery.TaskReady, PreparedDelivery.Unavailable {

        record TaskReady(TaskPackage taskPackage) implements PreparedDelivery {
            public TaskReady {
                Objects.requireNonNull(taskPackage, "taskPackage must not be null");
            }
        }

        record Unavailable(TaskUnavailableReason reason, String learnerMessage) implements PreparedDelivery {
            public Unavailable {
                Objects.requireNonNull(reason, "reason must not be null");
                Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
            }
        }
    }
}
