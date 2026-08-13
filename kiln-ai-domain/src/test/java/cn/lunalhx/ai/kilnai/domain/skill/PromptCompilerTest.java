package cn.lunalhx.ai.kilnai.domain.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptCompilerTest {

    @Test
    void compiledPromptKeepsNamespacedSections() {
        SkillManifest action = new SkillManifest(
                "apply.worked-example", 1, SkillSlot.ACTION,
                cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction.APPLY,
                Set.of(), Set.of(), List.of(), List.of(), Set.of(), true, 10
        );
        SkillManifest capability = new SkillManifest(
                "capability.quantitative", 1, SkillSlot.REASONING, null,
                Set.of("quantitative"), Set.of(), List.of(), List.of(), Set.of(), false, 5
        );
        SkillStack stack = new SkillStack(action, List.of(capability));
        PromptCompiler compiler = new PromptCompiler();

        String prompt = compiler.compile(stack, compiler.isolate(stack, "solve with a worked example", "use exact arithmetic"));

        assertTrue(prompt.contains("[action:apply.worked-example@1]"));
        assertTrue(prompt.contains("[capability:capability.quantitative@1]"));
        assertTrue(prompt.contains("solve with a worked example"));
        assertFalse(prompt.contains("capability.quantitative@1]\nsolve with a worked example"));
    }
}
