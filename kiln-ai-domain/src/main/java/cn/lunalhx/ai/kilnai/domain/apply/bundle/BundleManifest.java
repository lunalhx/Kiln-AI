package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

public record BundleManifest(
        String schema,
        String id,
        String version,
        BundleSlot slot,
        String summary,
        List<String> requiresContext,
        List<String> outputContribution,
        Permissions permissions,
        Compatibility compatibility,
        List<String> resources
) {

    public BundleManifest {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        Objects.requireNonNull(requiresContext, "requiresContext must not be null");
        Objects.requireNonNull(outputContribution, "outputContribution must not be null");
        Objects.requireNonNull(permissions, "permissions must not be null");
        Objects.requireNonNull(compatibility, "compatibility must not be null");
        Objects.requireNonNull(resources, "resources must not be null");
        requiresContext = List.copyOf(requiresContext);
        outputContribution = List.copyOf(outputContribution);
        resources = List.copyOf(resources);
        if (!"kiln.skill/v1".equals(schema)) {
            throw new IllegalArgumentException("unsupported manifest schema: " + schema);
        }
    }

    public String pinnedId() {
        return id + "@" + version;
    }

    @JsonCreator
    public static BundleManifest create(
            @JsonProperty("schema") String schema,
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("slot") String slot,
            @JsonProperty("summary") String summary,
            @JsonProperty("requires_context") List<String> requiresContext,
            @JsonProperty("output_contribution") List<String> outputContribution,
            @JsonProperty("permissions") Permissions permissions,
            @JsonProperty("compatibility") Compatibility compatibility,
            @JsonProperty("resources") List<String> resources
    ) {
        return new BundleManifest(
                schema, id, version, BundleSlot.valueOf(slot.toUpperCase()), summary,
                requiresContext, outputContribution, permissions, compatibility, resources
        );
    }

    public record Permissions(@JsonProperty("tools") List<String> tools) {
        public Permissions {
            tools = List.copyOf(tools == null ? List.of() : tools);
        }
    }

    public record Compatibility(@JsonProperty("profiles") List<String> profiles,
                                @JsonProperty("response_draft") String responseDraft) {
        public Compatibility {
            profiles = List.copyOf(profiles == null ? List.of() : profiles);
        }
    }
}
