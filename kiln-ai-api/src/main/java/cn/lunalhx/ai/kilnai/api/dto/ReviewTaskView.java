package cn.lunalhx.ai.kilnai.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The learner-safe Review collection entry. Only the Review Task id, Concept
 * id, status, review number, due time, startability, and the safe Concept
 * Progress projection are exposed; learner UUIDs, private assessor facts,
 * evidence records, and audit identifiers never appear.
 */
public record ReviewTaskView(
        UUID reviewId,
        UUID conceptId,
        String status,
        int reviewNumber,
        Instant dueAt,
        boolean startable,
        ApplyFlowResponse.ProgressView progress
) {
}
