package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The durable checkpoint for one successful post-submission evaluation
 * responsibility. Its identity is the closed Task Attempt, the responsibility
 * that produced the result, and the immutable evaluation version.
 */
public record CommittedEvaluationResult(
        UUID resultId,
        UUID attemptId,
        String responsibility,
        String evaluationVersion,
        String resultSchema,
        String resultPayload,
        Instant createdAt
) {

    public static final String EVALUATION_VERSION = "1.0.0";
    public static final String RESPONSE_ASSESSMENT = "assessment";
    public static final String RESPONSE_VERIFICATION = "response_verification";
    public static final String RATIONALE_ASSESSMENT = "rationale_assessment";
    public static final String TEACH_BACK_ASSESSMENT = "teach_back_assessment";

    public CommittedEvaluationResult {
        Objects.requireNonNull(resultId, "resultId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(responsibility, "responsibility must not be null");
        Objects.requireNonNull(evaluationVersion, "evaluationVersion must not be null");
        Objects.requireNonNull(resultSchema, "resultSchema must not be null");
        Objects.requireNonNull(resultPayload, "resultPayload must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (responsibility.isBlank()) {
            throw new IllegalArgumentException("responsibility must not be blank");
        }
        if (evaluationVersion.isBlank()) {
            throw new IllegalArgumentException("evaluationVersion must not be blank");
        }
        if (resultSchema.isBlank()) {
            throw new IllegalArgumentException("resultSchema must not be blank");
        }
        if (resultPayload.isBlank()) {
            throw new IllegalArgumentException("resultPayload must not be blank");
        }
    }
}
