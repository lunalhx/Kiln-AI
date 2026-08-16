package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.Objects;

/**
 * The frozen model-runtime facts recorded in an artifact's execution trace:
 * the Strong model identity that produced the artifact, the Small model
 * identity frozen on the Flow, the operator-owned output-token ceiling, the
 * prompt compiler's instruction cap, and the repair count actually used by
 * the node: zero for an accepted first candidate, one for a single allowed
 * same-plan repair, and two only when a node also used its one fresh second
 * candidate after a verifier rejection (Teach-back). The trace makes the
 * node's enforced model budget auditable; the gate validates it against the
 * Flow-frozen profile.
 */
public record ModelExecution(
        String strongModel,
        String smallModel,
        int outputTokenCeiling,
        int instructionCap,
        int repairCount
) {

    public ModelExecution {
        Objects.requireNonNull(strongModel, "strongModel must not be null");
        Objects.requireNonNull(smallModel, "smallModel must not be null");
        if (outputTokenCeiling <= 0) {
            throw new IllegalArgumentException("outputTokenCeiling must be positive: " + outputTokenCeiling);
        }
        if (instructionCap <= 0) {
            throw new IllegalArgumentException("instructionCap must be positive: " + instructionCap);
        }
        if (repairCount < 0) {
            throw new IllegalArgumentException("repairCount must not be negative: " + repairCount);
        }
    }

    public static ModelExecution from(ModelProfile profile, int instructionCap, int repairCount) {
        Objects.requireNonNull(profile, "profile must not be null");
        return new ModelExecution(
                profile.strong().identity(),
                profile.small().identity(),
                profile.outputTokenCeiling(),
                instructionCap,
                repairCount);
    }

    /**
     * The frozen bindings recorded on the node must be the profile the Flow
     * froze at start; a mismatched identity or ceiling means the node did not
     * use the frozen configuration.
     */
    public boolean usesFrozenProfile(ModelProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        return strongModel.equals(profile.strong().identity())
                && smallModel.equals(profile.small().identity())
                && outputTokenCeiling == profile.outputTokenCeiling();
    }
}
