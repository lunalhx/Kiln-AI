package cn.lunalhx.ai.kilnai.api.dto;

import java.time.Instant;
import java.util.UUID;

public record LearningProgressResponse(
        UUID userId,
        UUID conceptId,
        String state,
        String nextAction,
        Instant nextReviewDueAt
) {
}
