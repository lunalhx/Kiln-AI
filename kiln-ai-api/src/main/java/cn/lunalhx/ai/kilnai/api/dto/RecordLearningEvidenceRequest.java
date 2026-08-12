package cn.lunalhx.ai.kilnai.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record RecordLearningEvidenceRequest(
        @NotNull UUID userId,
        @NotNull String eventType,
        @NotNull String result,
        @Min(0) @Max(4) int hintLevel,
        boolean delayedReview,
        boolean transfer,
        @NotNull Instant occurredAt,
        @Min(1) @Max(5) Integer confidence,
        @Size(max = 100) String errorTag
) {
}
