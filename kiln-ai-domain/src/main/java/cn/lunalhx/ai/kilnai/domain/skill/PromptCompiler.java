package cn.lunalhx.ai.kilnai.domain.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PromptCompiler {

    public static final int INSTRUCTION_BUDGET = 4_000;

    public String compile(SkillStack stack, Map<String, String> namespacedSections) {
        Objects.requireNonNull(stack, "stack must not be null");
        Objects.requireNonNull(namespacedSections, "namespacedSections must not be null");
        StringBuilder compiled = new StringBuilder();
        compiled.append("[action:").append(stack.actionSkill().id()).append("]\n");
        compiled.append(namespacedSections.getOrDefault("action", "")).append('\n');
        stack.capabilitySkills().forEach(skill -> {
            compiled.append("[capability:").append(skill.id()).append("]\n");
            compiled.append(namespacedSections.getOrDefault(skill.slot().name().toLowerCase(), "")).append('\n');
        });
        if (compiled.length() > INSTRUCTION_BUDGET) {
            throw new CapabilityGap("prompt budget exceeded");
        }
        return compiled.toString();
    }

    public Map<String, String> isolate(SkillStack stack, String actionInstructions, String capabilityInstructions) {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("action", actionInstructions);
        if (!stack.capabilitySkills().isEmpty()) {
            sections.put("reasoning", capabilityInstructions);
        }
        return Map.copyOf(sections);
    }
}
