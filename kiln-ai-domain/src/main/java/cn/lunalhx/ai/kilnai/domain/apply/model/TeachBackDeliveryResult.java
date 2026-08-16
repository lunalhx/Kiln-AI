package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed result of one Teach-back delivery. A {@link Delivered} outcome
 * carries the open Practice-purpose Attempt and its learner projection; an
 * {@link Unavailable} outcome exposes nothing and opens no Attempt.
 */
public sealed interface TeachBackDeliveryResult
        permits TeachBackDeliveryResult.Delivered, TeachBackDeliveryResult.Unavailable {

    String UNAVAILABLE_LEARNER_MESSAGE = "暂时无法准备一道可验证的题目。请稍后重试。";

    record Delivered(TaskAttempt attempt, LearnerProjection learnerProjection) implements TeachBackDeliveryResult {
        public Delivered {
            java.util.Objects.requireNonNull(attempt, "attempt must not be null");
            java.util.Objects.requireNonNull(learnerProjection, "learnerProjection must not be null");
        }
    }

    record Unavailable(TeachBackUnavailableReason reason, String learnerMessage) implements TeachBackDeliveryResult {
        public Unavailable {
            java.util.Objects.requireNonNull(reason, "reason must not be null");
            java.util.Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }
}
