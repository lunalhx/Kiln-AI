package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The domain policy for the Review cadence: a fresh accepted Independent pass
 * cancels any stale unfinished Review of the same learner and Concept and
 * atomically schedules the unique Review 1 due 24 hours after the acceptance
 * time, an accepted qualifying Review pass completes its started Review
 * and schedules the successor 3, 7, and then 21 days after the actual
 * acceptance time — Review 4 schedules nothing — and an accepted conclusive
 * no-hint Review failure completes its started Review, cancels any other
 * unfinished Review defensively, and schedules no successor. The scheduler
 * owns the fixed Phase 0 cadence, never a model; the store guarantees the
 * atomic commit.
 */
public final class ReviewTaskScheduler {

    public static final Duration FIRST_REVIEW_DELAY = Duration.ofHours(24);

    /**
     * The successor interval after completing Review 1, 2, and 3 respectively,
     * measured from the actual Review completion (evidence acceptance) time.
     */
    public static final List<Duration> SUCCESSOR_DELAYS = List.of(
            Duration.ofDays(3),
            Duration.ofDays(7),
            Duration.ofDays(21));

    private final ReviewTaskStore reviewStore;

    public ReviewTaskScheduler(ReviewTaskStore reviewStore) {
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
    }

    /**
     * Atomically accepts one qualifying Independent pass and schedules the
     * unique Review 1 due 24 hours after the acceptance time, cancelling any
     * stale unfinished Review of the same learner and Concept. Returns empty
     * when the task attempt already has Evidence, in which case nothing at
     * all is written.
     */
    public Optional<ReviewTask> acceptEvidenceAndScheduleFirstReview(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        return reviewStore.acceptEvidenceAndScheduleFirstReview(
                evidence, evidence.acceptedAt().plus(FIRST_REVIEW_DELAY));
    }

    /**
     * Advances the cadence by one qualifying Review pass: completes the
     * STARTED Review of the evidence's learner and Concept and schedules the
     * successor after the fixed interval measured from the actual acceptance
     * time. Returns empty when no Review is STARTED or when the evidence does
     * not come from the STARTED Review's own open attempt — a stale or
     * cancelled Review attempt can never advance or stop a cadence — in which
     * case nothing at all is written.
     */
    public Optional<ReviewTaskStore.ReviewAdvance> acceptEvidenceAndAdvanceReview(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        Optional<ReviewTask> started = reviewStore.findStartedReview(
                evidence.learnerId(), evidence.conceptId());
        if (started.isEmpty()) {
            return Optional.empty();
        }
        ReviewTask current = started.get();
        if (!evidence.taskAttemptId().equals(current.openAttemptId())) {
            return Optional.empty();
        }
        Instant nextDueAt = current.reviewNumber() < ConceptProgressProjector.QUALIFYING_REVIEW_COUNT
                ? evidence.acceptedAt().plus(SUCCESSOR_DELAYS.get(current.reviewNumber() - 1))
                : null;
        return reviewStore.acceptEvidenceAndAdvanceReview(evidence, current.reviewId(), nextDueAt);
    }

    /**
     * Stops the cadence by one conclusive no-hint Review failure: completes
     * the STARTED Review of the evidence's learner and Concept at the actual
     * acceptance time, cancels any other unfinished Review defensively, and
     * schedules no successor. Returns empty when no Review is STARTED or when
     * the evidence does not come from the STARTED Review's own open attempt,
     * in which case nothing at all is written.
     */
    public Optional<ReviewTaskStore.ReviewStop> acceptEvidenceAndFailReview(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        Optional<ReviewTask> started = reviewStore.findStartedReview(
                evidence.learnerId(), evidence.conceptId());
        if (started.isEmpty()) {
            return Optional.empty();
        }
        ReviewTask current = started.get();
        if (!evidence.taskAttemptId().equals(current.openAttemptId())) {
            return Optional.empty();
        }
        return reviewStore.acceptEvidenceAndFailReview(evidence, current.reviewId());
    }
}
