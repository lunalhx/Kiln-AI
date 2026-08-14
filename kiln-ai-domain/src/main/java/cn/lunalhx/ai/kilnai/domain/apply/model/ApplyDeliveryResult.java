package cn.lunalhx.ai.kilnai.domain.apply.model;

public sealed interface ApplyDeliveryResult {

    String UNAVAILABLE_LEARNER_MESSAGE = "暂时无法准备一道可验证的题目。请稍后重试。";

    record Delivered(TaskAttempt attempt, LearnerProjection learnerProjection) implements ApplyDeliveryResult {
    }

    record Unavailable(TaskUnavailableReason reason, String learnerMessage) implements ApplyDeliveryResult {
    }
}
