package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.Objects;
import java.util.UUID;

/**
 * The closed result of one Review start command. A successful start returns
 * the durable learner interaction of the original Apply Flow in Delayed
 * Review; an unavailable generation outcome returns only the shared neutral
 * message and leaves the Review Task Due. Deterministic failures such as an
 * unknown or non-startable Review Task are raised as typed application
 * exceptions instead.
 */
public sealed interface ReviewStartResult
        permits ReviewStartResult.Boundary, ReviewStartResult.Unavailable {

    record Boundary(ApplyFlowInteraction interaction) implements ReviewStartResult {
        public Boundary {
            Objects.requireNonNull(interaction, "interaction must not be null");
        }
    }

    record Unavailable(TaskUnavailableReason reason, UUID flowId, String learnerMessage)
            implements ReviewStartResult {
        public Unavailable {
            Objects.requireNonNull(reason, "reason must not be null");
            Objects.requireNonNull(flowId, "flowId must not be null");
            Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }
}
