package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;

import java.time.Clock;
import java.util.Objects;

/**
 * The deterministic due-transition use case invoked by the trigger-layer
 * scheduler: every Scheduled Review whose {@code dueAt} has arrived is
 * durably changed to Due in one concurrency-safe store operation. It never
 * calls a model, generates a Task Package, creates an Attempt, records
 * Exposure or Evidence, or resumes a Learning Flow; a missed task simply
 * remains Due.
 */
public final class ReviewDueTransitionUseCase {

    private final ReviewTaskStore reviewStore;
    private final Clock clock;

    public ReviewDueTransitionUseCase(ReviewTaskStore reviewStore, Clock clock) {
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Transitions every eligible Scheduled Review to Due and returns the
     * number of transitions made. Repeated ticks are idempotent: already-Due
     * work stays Due and returns nothing further.
     */
    public int markDueReviewsDue() {
        return reviewStore.markDueReviewsDue(clock.instant());
    }
}
