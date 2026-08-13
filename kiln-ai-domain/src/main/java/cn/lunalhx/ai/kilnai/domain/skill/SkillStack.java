package cn.lunalhx.ai.kilnai.domain.skill;

import java.util.List;
import java.util.Objects;

public record SkillStack(SkillManifest actionSkill, List<SkillManifest> capabilitySkills) {
    public SkillStack {
        Objects.requireNonNull(actionSkill, "actionSkill must not be null");
        Objects.requireNonNull(capabilitySkills, "capabilitySkills must not be null");
        if (actionSkill.slot() != SkillSlot.ACTION) {
            throw new IllegalArgumentException("action slot is required");
        }
        capabilitySkills = List.copyOf(capabilitySkills);
    }
}
