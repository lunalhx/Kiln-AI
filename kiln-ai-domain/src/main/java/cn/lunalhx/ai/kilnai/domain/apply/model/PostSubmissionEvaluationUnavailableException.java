package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Internal control signal for a technical failure in one submitted Attempt's
 * evaluation responsibility. The graph converts it into a durable
 * Unavailable Interaction; no provider or contract detail is exposed to the
 * learner or copied into the Pending Operation.
 */
public final class PostSubmissionEvaluationUnavailableException extends RuntimeException {

    private final UUID attemptId;
    private final String responsibility;
    private final String evaluationVersion;

    public PostSubmissionEvaluationUnavailableException(
            UUID attemptId,
            String responsibility,
            String evaluationVersion
    ) {
        super("post-submission evaluation unavailable");
        this.attemptId = Objects.requireNonNull(attemptId, "attemptId must not be null");
        this.responsibility = Objects.requireNonNull(responsibility, "responsibility must not be null");
        this.evaluationVersion = Objects.requireNonNull(evaluationVersion, "evaluationVersion must not be null");
        if (responsibility.isBlank() || evaluationVersion.isBlank()) {
            throw new IllegalArgumentException("evaluation identity must not be blank");
        }
    }

    public UUID attemptId() {
        return attemptId;
    }

    public String responsibility() {
        return responsibility;
    }

    public String evaluationVersion() {
        return evaluationVersion;
    }
}
