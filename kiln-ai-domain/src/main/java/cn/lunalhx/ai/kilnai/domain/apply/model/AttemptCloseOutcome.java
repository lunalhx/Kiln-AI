package cn.lunalhx.ai.kilnai.domain.apply.model;

public record AttemptCloseOutcome(Result result, TaskAttempt attempt) {

    public enum Result {
        CLOSED,
        ALREADY_CLOSED,
        NOT_FOUND
    }
}
