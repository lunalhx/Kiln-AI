package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.Objects;

/**
 * The closed outcome of one Explain node delivery: a validated teaching
 * artifact, or an unavailable reason with the neutral learner message. A
 * failed generation persists nothing and returns control to the graph for a
 * safe retry or Flow Control.
 */
public sealed interface ExplainDeliveryResult
        permits ExplainDeliveryResult.Delivered, ExplainDeliveryResult.Unavailable {

    String UNAVAILABLE_LEARNER_MESSAGE = "暂时无法准备讲解内容。请稍后重试。";

    record Delivered(ExplainTeachingArtifact artifact) implements ExplainDeliveryResult {

        public Delivered {
            Objects.requireNonNull(artifact, "artifact must not be null");
        }
    }

    record Unavailable(ExplainUnavailableReason reason, String learnerMessage) implements ExplainDeliveryResult {

        public Unavailable {
            Objects.requireNonNull(reason, "reason must not be null");
            Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }
}
