package cn.lunalhx.ai.kilnai.domain.tool;

import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ToolResolver {

    public List<ToolHandle> resolve(
            ToolPermissionSet profileAllowlist,
            Set<String> skillRequirements,
            Set<ToolHandle> runtimeAvailability,
            boolean withinBudget
    ) {
        Objects.requireNonNull(profileAllowlist, "profileAllowlist must not be null");
        Objects.requireNonNull(skillRequirements, "skillRequirements must not be null");
        Objects.requireNonNull(runtimeAvailability, "runtimeAvailability must not be null");
        if (!withinBudget) {
            throw new CapabilityGap("tool budget exhausted");
        }
        Set<String> availableIds = runtimeAvailability.stream()
                .map(ToolHandle::qualifiedId)
                .collect(Collectors.toSet());
        List<ToolHandle> resolved = new ArrayList<>();
        for (String required : skillRequirements) {
            if (!profileAllowlist.allowedToolIds().contains(required)) {
                throw new CapabilityGap("tool not allowed by profile: " + required);
            }
            if (!availableIds.contains(required)) {
                throw new CapabilityGap("required tool unavailable: " + required);
            }
            runtimeAvailability.stream()
                    .filter(handle -> handle.qualifiedId().equals(required))
                    .findFirst()
                    .ifPresent(resolved::add);
        }
        return List.copyOf(resolved);
    }
}
