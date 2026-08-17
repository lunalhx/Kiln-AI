package cn.lunalhx.ai.kilnai.api.response;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The learner-safe error envelope. {@code flowId} carries the existing Flow id
 * for the Active Learning Work Start conflict (ADR-0070) and is null for every
 * other error, so the conflict body exposes only the recovery id.
 */
public record ApiErrorResponse(String code, String message, Instant timestamp, UUID flowId) {

    public ApiErrorResponse {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public ApiErrorResponse(String code, String message, Instant timestamp) {
        this(code, message, timestamp, null);
    }
}
