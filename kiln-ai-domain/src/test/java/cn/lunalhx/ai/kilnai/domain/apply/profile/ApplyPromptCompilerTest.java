package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleSlot;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyPromptCompilerTest {

    private final ApplyPromptCompiler compiler = new ApplyPromptCompiler();

    private BundleStack referenceStack() {
        return ReferenceBundles.stack();
    }

    @Test
    void compiledPromptKeepsProfileBundlesAndResponseContractNamespaced() {
        String prompt = compiler.compile(referenceStack());

        assertTrue(prompt.contains("# Apply Profile"));
        assertTrue(prompt.contains("You operate inside Kiln-AI's Apply Profile."));
        assertTrue(prompt.contains("[bundle:action:apply.task-first@0.1.0]"));
        assertTrue(prompt.contains("[bundle:reasoning:reasoning.rule-application@0.1.0]"));
        assertTrue(prompt.contains("[bundle:representation:representation.formal-expression@0.1.0]"));
        assertTrue(prompt.contains("[bundle:verification:verification.structured-task-contract@0.1.0]"));
        assertTrue(prompt.contains("[bundle:subject:subject.calculus-notation@0.1.0]"));
        assertTrue(prompt.contains("# Response Contract"));
        assertTrue(prompt.contains("apply_generation/v1"));
        assertTrue(prompt.length() <= ApplyPromptCompiler.INSTRUCTION_BUDGET);
    }

    @Test
    void compiledPromptNeverMixesExecutionDataIntoInstructions() {
        String prompt = compiler.compile(referenceStack());
        ApplyExecutionContext context = DiagnosticApplyFixture.diagnosticContext();
        String sourcePassage = context.conceptSourcePack().passages().get(0).content();

        assertFalse(prompt.contains(sourcePassage));
        assertFalse(prompt.contains(context.conceptSourcePack().id()));
        assertFalse(prompt.contains("zh-CN"));
    }

    @Test
    void executionContextIsSeparateClosedJson() {
        String contextJson = compiler.serializeContext(DiagnosticApplyFixture.diagnosticContext());
        cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson.readTree(contextJson);

        assertTrue(contextJson.contains("\"schema\":\"apply_execution_context/v1\""));
        assertTrue(contextJson.contains("\"learner_locale\":\"zh-CN\""));
        assertTrue(contextJson.contains("\"attempt_purpose\":\"diagnostic\""));
        assertFalse(contextJson.contains("# Apply Profile"));
    }

    @Test
    void rejectsNonActionBundlesThatContributeDraftFields() {
        SkillBundle reasoning = ReferenceBundles.bundle("reasoning.rule-application@0.1.0");
        SkillBundle reasoningWithContribution = ReferenceBundles.rewrap(
                withContribution(reasoning.manifest(), List.of("learner_task_text")), reasoning);
        BundleStack stack = new BundleStack(List.of(
                ReferenceBundles.bundle("apply.task-first@0.1.0"), reasoningWithContribution,
                ReferenceBundles.bundle("representation.formal-expression@0.1.0"),
                ReferenceBundles.bundle("verification.structured-task-contract@0.1.0"),
                ReferenceBundles.bundle("subject.calculus-notation@0.1.0")));
        assertThrows(CapabilityGap.class, () -> compiler.compile(stack));
    }

    @Test
    void stackRejectsConflictingSlots() {
        SkillBundle action = ReferenceBundles.bundle("apply.task-first@0.1.0");
        SkillBundle secondAction = ReferenceBundles.rewrap(
                new BundleManifest(
                        "kiln.skill/v1", "apply.task-first.copy", "0.1.0", BundleSlot.ACTION,
                        "copy", List.of("concept_contract"), List.of("learner_task_text"),
                        new BundleManifest.Permissions(List.of()),
                        new BundleManifest.Compatibility(List.of("apply"), "apply_generation/v1"),
                        List.of()),
                action);
        List<SkillBundle> conflicting = new ArrayList<>(List.of(action, secondAction));
        ReferenceBundles.all().stream()
                .filter(bundle -> bundle.manifest().slot() != BundleSlot.ACTION)
                .forEach(conflicting::add);
        assertThrows(IllegalArgumentException.class, () -> new BundleStack(conflicting));
    }

    @Test
    void rejectsBundlesThatDeclareTools() {
        SkillBundle action = ReferenceBundles.bundle("apply.task-first@0.1.0");
        SkillBundle reasoning = ReferenceBundles.bundle("reasoning.rule-application@0.1.0");
        SkillBundle tooled = ReferenceBundles.rewrap(
                withTools(reasoning.manifest(), List.of("calculator@1")), reasoning);
        BundleStack stack = new BundleStack(List.of(
                action, tooled,
                ReferenceBundles.bundle("representation.formal-expression@0.1.0"),
                ReferenceBundles.bundle("verification.structured-task-contract@0.1.0"),
                ReferenceBundles.bundle("subject.calculus-notation@0.1.0")));
        assertThrows(CapabilityGap.class, () -> compiler.compile(stack));
    }

    @Test
    void rejectsBundlesIncompatibleWithTheApplyProfile() {
        SkillBundle action = ReferenceBundles.bundle("apply.task-first@0.1.0");
        SkillBundle reasoning = ReferenceBundles.bundle("reasoning.rule-application@0.1.0");
        SkillBundle incompatible = ReferenceBundles.rewrap(
                withCompatibility(reasoning.manifest(), List.of("explain")), reasoning);
        BundleStack stack = new BundleStack(List.of(
                action, incompatible,
                ReferenceBundles.bundle("representation.formal-expression@0.1.0"),
                ReferenceBundles.bundle("verification.structured-task-contract@0.1.0"),
                ReferenceBundles.bundle("subject.calculus-notation@0.1.0")));
        assertThrows(CapabilityGap.class, () -> compiler.compile(stack));
    }

    @Test
    void fixedProfileStackIsExactlyTheFivePinnedBundles() {
        assertEquals(List.of(
                "apply.task-first@0.1.0",
                "reasoning.rule-application@0.1.0",
                "representation.formal-expression@0.1.0",
                "verification.structured-task-contract@0.1.0",
                "subject.calculus-notation@0.1.0"), ApplyProfile.FIXED_STACK);
    }

    private BundleManifest withContribution(
            BundleManifest manifest,
            List<String> contribution) {
        return new BundleManifest(
                manifest.schema(), manifest.id(), manifest.version(), manifest.slot(), manifest.summary(),
                manifest.requiresContext(), new ArrayList<>(contribution), manifest.permissions(),
                manifest.compatibility(), manifest.resources());
    }

    private BundleManifest withTools(
            BundleManifest manifest,
            List<String> tools) {
        return new BundleManifest(
                manifest.schema(), manifest.id(), manifest.version(), manifest.slot(), manifest.summary(),
                manifest.requiresContext(), manifest.outputContribution(),
                new BundleManifest.Permissions(tools),
                manifest.compatibility(), manifest.resources());
    }

    private BundleManifest withCompatibility(
            BundleManifest manifest,
            List<String> profiles) {
        return new BundleManifest(
                manifest.schema(), manifest.id(), manifest.version(), manifest.slot(), manifest.summary(),
                manifest.requiresContext(), manifest.outputContribution(), manifest.permissions(),
                new BundleManifest.Compatibility(
                        profiles, manifest.compatibility().responseDraft()),
                manifest.resources());
    }
}
