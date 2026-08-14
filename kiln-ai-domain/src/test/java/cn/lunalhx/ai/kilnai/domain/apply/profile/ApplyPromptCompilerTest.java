package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleLoader;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleRegistry;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleSlot;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundleSource;
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

    private final BundleLoader loader = new BundleLoader();
    private final ApplyPromptCompiler compiler = new ApplyPromptCompiler();

    private BundleRegistry referenceRegistry() {
        BundleRegistry registry = new BundleRegistry();
        for (String bundleId : List.of(
                "apply.task-first",
                "reasoning.rule-application",
                "representation.formal-expression",
                "verification.structured-task-contract",
                "subject.calculus-notation")) {
            registry.register(loader.load(bundleId));
        }
        return registry;
    }

    private BundleStack referenceStack() {
        return new BundleStack(referenceRegistry().all().stream().toList());
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
        SkillBundleSource action = loader.load("apply.task-first");
        SkillBundleSource reasoning = new SkillBundleSource(
                withContribution(loader.load("reasoning.rule-application").manifest(),
                        List.of("learner_task_text")),
                loader.load("reasoning.rule-application").coreMarkdown(),
                loader.load("reasoning.rule-application").fullFileContent());
        BundleStack stack = new BundleStack(List.of(
                register(action), register(reasoning),
                register(loader.load("representation.formal-expression")),
                register(loader.load("verification.structured-task-contract")),
                register(loader.load("subject.calculus-notation"))));
        assertThrows(CapabilityGap.class, () -> compiler.compile(stack));
    }

    @Test
    void stackRejectsConflictingSlots() {
        BundleRegistry registry = referenceRegistry();
        SkillBundle action = registry.resolve("apply.task-first", "0.1.0");
        SkillBundle secondAction = new SkillBundle(
                new cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest(
                        "kiln.skill/v1", "apply.task-first.copy", "0.1.0", BundleSlot.ACTION,
                        "copy", List.of("concept_contract"), List.of("learner_task_text"),
                        new cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest.Permissions(List.of()),
                        new cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest.Compatibility(
                                List.of("apply"), "apply_generation/v1"),
                        List.of()),
                "body", "hash");
        List<SkillBundle> conflicting = new ArrayList<>(List.of(action, secondAction));
        registry.all().stream()
                .filter(bundle -> bundle.manifest().slot() != BundleSlot.ACTION)
                .forEach(conflicting::add);
        assertThrows(IllegalArgumentException.class, () -> new BundleStack(conflicting));
    }

    @Test
    void rejectsBundlesThatDeclareTools() {
        SkillBundleSource action = loader.load("apply.task-first");
        SkillBundleSource tooled = new SkillBundleSource(
                withTools(loader.load("reasoning.rule-application").manifest(), List.of("calculator@1")),
                loader.load("reasoning.rule-application").coreMarkdown(),
                loader.load("reasoning.rule-application").fullFileContent());
        BundleStack stack = new BundleStack(List.of(
                register(action), register(tooled),
                register(loader.load("representation.formal-expression")),
                register(loader.load("verification.structured-task-contract")),
                register(loader.load("subject.calculus-notation"))));
        assertThrows(CapabilityGap.class, () -> compiler.compile(stack));
    }

    @Test
    void rejectsBundlesIncompatibleWithTheApplyProfile() {
        SkillBundleSource action = loader.load("apply.task-first");
        SkillBundleSource incompatible = new SkillBundleSource(
                withCompatibility(loader.load("reasoning.rule-application").manifest(), List.of("explain")),
                loader.load("reasoning.rule-application").coreMarkdown(),
                loader.load("reasoning.rule-application").fullFileContent());
        BundleStack stack = new BundleStack(List.of(
                register(action), register(incompatible),
                register(loader.load("representation.formal-expression")),
                register(loader.load("verification.structured-task-contract")),
                register(loader.load("subject.calculus-notation"))));
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

    private SkillBundle register(SkillBundleSource source) {
        return new BundleRegistry().register(source);
    }

    private cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest withContribution(
            cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest manifest,
            List<String> contribution) {
        return new cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest(
                manifest.schema(), manifest.id(), manifest.version(), manifest.slot(), manifest.summary(),
                manifest.requiresContext(), new ArrayList<>(contribution), manifest.permissions(),
                manifest.compatibility(), manifest.resources());
    }

    private cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest withTools(
            cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest manifest,
            List<String> tools) {
        return new cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest(
                manifest.schema(), manifest.id(), manifest.version(), manifest.slot(), manifest.summary(),
                manifest.requiresContext(), manifest.outputContribution(),
                new cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest.Permissions(tools),
                manifest.compatibility(), manifest.resources());
    }

    private cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest withCompatibility(
            cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest manifest,
            List<String> profiles) {
        return new cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest(
                manifest.schema(), manifest.id(), manifest.version(), manifest.slot(), manifest.summary(),
                manifest.requiresContext(), manifest.outputContribution(), manifest.permissions(),
                new cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest.Compatibility(
                        profiles, manifest.compatibility().responseDraft()),
                manifest.resources());
    }
}
