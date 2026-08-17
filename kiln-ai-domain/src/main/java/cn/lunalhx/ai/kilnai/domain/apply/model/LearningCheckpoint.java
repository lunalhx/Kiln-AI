package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The durable graph checkpoint of one Learning Flow at one Learner
 * Interaction Boundary. It is written atomically with its
 * {@link LearningFlowInteraction} and marks the exact point at which the flow
 * paused awaiting learner input.
 */
public record LearningCheckpoint(
        UUID checkpointId,
        UUID flowId,
        int interactionVersion,
        Instant createdAt
) {

    public LearningCheckpoint {
        Objects.requireNonNull(checkpointId, "checkpointId must not be null");
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
