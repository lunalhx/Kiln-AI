package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.Objects;

/**
 * The operator-owned Model Profile (CONTEXT.md) copied onto a Learning Flow
 * at start as a resolved snapshot of protocol, endpoint, and model identity
 * for the Strong and Small slots, then frozen for that flow's lifetime. The
 * operator-owned output-token ceiling is part of the frozen budget; secrets
 * stay in the environment and are never copied into the profile. Editing the
 * operator catalog or defaults affects only new flows.
 */
public record ModelProfile(
        ModelBinding strong,
        ModelBinding small,
        int outputTokenCeiling
) {

    public ModelProfile {
        Objects.requireNonNull(strong, "strong must not be null");
        Objects.requireNonNull(small, "small must not be null");
        if (outputTokenCeiling <= 0) {
            throw new IllegalArgumentException("outputTokenCeiling must be positive: " + outputTokenCeiling);
        }
    }

    /**
     * The resolved snapshot of one model-producing slot: protocol, endpoint,
     * and model identity written as {@code providerId/modelId}. It references
     * the environment secret variable but never carries the secret value.
     */
    public record ModelBinding(
            String protocol,
            String endpoint,
            String providerId,
            String modelId,
            String secretEnvVar
    ) {

        public ModelBinding {
            Objects.requireNonNull(protocol, "protocol must not be null");
            Objects.requireNonNull(endpoint, "endpoint must not be null");
            Objects.requireNonNull(providerId, "providerId must not be null");
            Objects.requireNonNull(modelId, "modelId must not be null");
            Objects.requireNonNull(secretEnvVar, "secretEnvVar must not be null");
        }

        public String identity() {
            return providerId + "/" + modelId;
        }
    }
}
