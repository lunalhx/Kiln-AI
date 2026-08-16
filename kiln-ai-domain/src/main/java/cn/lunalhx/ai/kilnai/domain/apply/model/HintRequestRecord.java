package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The durable record of one hint request on one Task Attempt. It is written
 * atomically with the exposure, so a command that crashed between exposing a
 * hint and committing its boundary resumes the same exposed level instead of
 * revealing the next one.
 */
public record HintRequestRecord(
        UUID attemptId,
        UUID commandKey,
        int requestedLevel,
        int exposedLevel,
        Instant exposedAt
) {
    public HintRequestRecord {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(commandKey, "commandKey must not be null");
        Objects.requireNonNull(exposedAt, "exposedAt must not be null");
        HintLevel.of(requestedLevel);
        HintLevel.of(exposedLevel);
    }
}
