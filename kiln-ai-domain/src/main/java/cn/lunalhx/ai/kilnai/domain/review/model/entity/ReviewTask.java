package cn.lunalhx.ai.kilnai.domain.review.model.entity;

import cn.lunalhx.ai.kilnai.domain.review.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.domain.review.model.valobj.ReviewTaskType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReviewTask(
        UUID id,
        UUID userId,
        UUID conceptId,
        ReviewTaskType taskType,
        ReviewTaskStatus status,
        Instant dueAt,
        Instant createdAt
) {
    public ReviewTask {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(dueAt, "dueAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
