package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.time.Instant;
import java.util.Objects;

/**
 * The single formal submission of one Task Attempt: the confirmed
 * final-derivative mathematical answer plus the optional rationale.
 */
public record TaskSubmission(
        MathematicalAnswer finalDerivative,
        String rationale,
        Instant submittedAt
) {

    public TaskSubmission {
        Objects.requireNonNull(finalDerivative, "finalDerivative must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
    }
}
