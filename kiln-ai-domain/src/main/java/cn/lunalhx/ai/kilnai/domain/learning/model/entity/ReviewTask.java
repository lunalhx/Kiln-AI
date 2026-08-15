package cn.lunalhx.ai.kilnai.domain.learning.model.entity;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The durable scheduled work item that makes one Concept's next Delayed
 * Review discoverable. It is coordination state, never a mastery claim:
 * milestones are projected only from accepted Learning Evidence. The open
 * attempt id points at the single OPEN Review Attempt of a Started Review and
 * is null while the Review is Due, Completed, Cancelled, or resumable after an
 * inconclusive submission, so at most one OPEN Attempt can ever be bound.
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
        UUID openAttemptId,
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

    /**
     * A Due Review can be started, and a Started Review can be resumed
     * through the same start endpoint exactly when it holds no open Attempt —
     * the state an Inconclusive submission with an unprepared replacement
     * leaves behind. A Started Review with an open (or failed, closed)
     * Attempt never is.
     */
    @JsonIgnore
    public boolean isStartable() {
        return status == ReviewTaskStatus.DUE
                || status == ReviewTaskStatus.STARTED && openAttemptId == null;
    }

    public ReviewTask withOpenAttempt(UUID attemptId) {
        return new ReviewTask(reviewId, learnerId, conceptId, flowId, reviewNumber, status, dueAt,
                createdAt, startedAt, attemptId, completedAt, cancelledAt);
    }
}
