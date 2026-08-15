package cn.lunalhx.ai.kilnai.domain.learning.model.entity;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The durable scheduled work item that makes one Concept's next Delayed
 * Review discoverable. It is coordination state, never a mastery claim:
 * milestones are projected only from accepted Learning Evidence.
 */
public record ReviewTask(
        UUID reviewId,
        UUID learnerId,
        UUID conceptId,
        UUID flowId,
        int reviewNumber,
        ReviewTaskStatus status,
        Instant dueAt,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt
) {
    public ReviewTask {
        Objects.requireNonNull(reviewId, "reviewId must not be null");
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(dueAt, "dueAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (reviewNumber < 1 || reviewNumber > 4) {
            throw new IllegalArgumentException("reviewNumber must be between 1 and 4");
        }
    }

    @JsonIgnore
    public boolean isUnfinished() {
        return status == ReviewTaskStatus.SCHEDULED
                || status == ReviewTaskStatus.DUE
                || status == ReviewTaskStatus.STARTED;
    }
}
