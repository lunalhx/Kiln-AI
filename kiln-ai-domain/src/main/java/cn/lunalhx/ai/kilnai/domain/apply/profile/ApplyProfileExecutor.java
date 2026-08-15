package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import cn.lunalhx.ai.kilnai.domain.apply.gate.ApplyGenerationDraftGatePolicy;
import cn.lunalhx.ai.kilnai.domain.apply.gate.ApplyTaskPackageGatePolicy;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyGenerationDraft;
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

    public ApplyDeliveryResult deliver(ApplyExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        validateContextCoverage(context, stack);
        String systemPrompt = compiler.compile(stack);
        String contextJson = compiler.serializeContext(context);
        for (int cycle = 1; cycle <= MAX_GENERATION_CYCLES; cycle++) {
            String raw = generationPort.generate(systemPrompt, contextJson);
            Optional<ApplyDeliveryResult> outcome = handleCandidate(context, stack, raw);
            if (outcome.isPresent()) {
                return outcome.get();
            }
        }
        return unavailable(TaskUnavailableReason.TASK_GENERATION_EXHAUSTED);
    }

    private Optional<ApplyDeliveryResult> handleCandidate(
            ApplyExecutionContext context,
            BundleStack stack,
            String raw
    ) {
        ApplyGenerationDraft draft;
        try {
            draft = ApplyGenerationDraft.parse(raw);
        } catch (ApplyDraftException exception) {
            return Optional.empty();
        }
        if (draft instanceof ApplyGenerationDraft.SourceGap) {
            return Optional.of(unavailable(TaskUnavailableReason.SOURCE_GAP));
        }
        ApplyGenerationDraft.TaskReady taskReady = (ApplyGenerationDraft.TaskReady) draft;
        GateResult<ApplyGenerationDraft.TaskReady> draftGate = gatePipeline.validate(
                taskReady, new ApplyGenerationDraftGatePolicy(context), GateContext.empty());
        if (draftGate.outcome() != GateOutcome.PASSED) {
            return Optional.empty();
        }
        Optional<TaskPackage> assembled = assembler.assemble(context, taskReady, stack);
        if (assembled.isEmpty()) {
            return Optional.empty();
        }
        TaskPackage taskPackage = assembled.get();
        GateResult<TaskPackage> packageGate = gatePipeline.validate(
                taskPackage,
                new ApplyTaskPackageGatePolicy(context, stack.pinnedIds()),
                GateContext.empty());
        if (packageGate.outcome() != GateOutcome.PASSED) {
            return Optional.empty();
        }
        TaskVerificationVerdict verdict = verifierPort.verify(taskPackage, context);
        artifactStore.recordTaskVerification(taskPackage.taskPackageId(), verdict);
        if (!verdict.passed()) {
            return Optional.empty();
        }
        TaskAttempt attempt = artifactStore.openAttempt(taskPackage);
        return Optional.of(new ApplyDeliveryResult.Delivered(attempt, taskPackage.learnerProjection()));
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

    private ApplyDeliveryResult unavailable(TaskUnavailableReason reason) {
        return new ApplyDeliveryResult.Unavailable(reason, ApplyDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE);
    }
}
