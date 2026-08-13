package cn.lunalhx.ai.kilnai.domain.tool;

import java.util.Objects;
import java.util.Set;

public record ToolPermissionSet(Set<String> allowedToolIds) {
    public ToolPermissionSet {
        Objects.requireNonNull(allowedToolIds, "allowedToolIds must not be null");
        allowedToolIds = Set.copyOf(allowedToolIds);
    }
}
