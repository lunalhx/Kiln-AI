package cn.lunalhx.ai.kilnai.domain.skill;

import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SkillManifest(
        String name,
        int version,
        SkillSlot slot,
        TeachingAction teachingAction,
        Set<String> capabilityTags,
        Set<String> strategyTags,
        List<String> dependencies,
        List<String> conflicts,
        Set<String> requiredTools,
        boolean defaultAction,
        int priority
) {
    public SkillManifest {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        Objects.requireNonNull(capabilityTags, "capabilityTags must not be null");
        Objects.requireNonNull(strategyTags, "strategyTags must not be null");
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        Objects.requireNonNull(conflicts, "conflicts must not be null");
        Objects.requireNonNull(requiredTools, "requiredTools must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
        capabilityTags = Set.copyOf(capabilityTags);
        strategyTags = Set.copyOf(strategyTags);
        dependencies = List.copyOf(dependencies);
        conflicts = List.copyOf(conflicts);
        requiredTools = Set.copyOf(requiredTools);
    }

    public String id() {
        return name + "@" + version;
    }
}
