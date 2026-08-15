package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The typed store of durable Review Tasks plus the single atomic transition
 * that accepts Independent evidence and schedules the first Review in one
 * commit. The database additionally enforces at most one unfinished Review
 * per learner and Concept, and the domain implementations enforce the same
 * invariant so the in-memory and PostgreSQL paths behave identically.
 */
public interface ReviewTaskStore {

    /**
     * Atomically accepts one item of Learning Evidence, cancels any stale
     * unfinished Review of the same learner and Concept, and schedules the
     * unique Review 1 due at the given {@code dueAt}, all in one commit.
     * The caller computes {@code dueAt} from its cadence policy; the store
     * only guarantees the atomic transition and the at-most-one-unfinished
     * invariant.
     */
    ReviewTask acceptEvidenceAndScheduleFirstReview(AcceptedLearningEvidence evidence, Instant dueAt);

    /**
     * Atomically changes every Scheduled Review whose due time has arrived
     * (inclusive) to Due and returns the number of transitions made. The
     * transition is a conditional, concurrency-safe update that performs no
     * model call and creates no Package, Attempt, Exposure, Evidence, or
     * Flow work; repeated ticks are idempotent and overdue Due work stays
     * Due.
     */
    int markDueReviewsDue(Instant now);

    List<ReviewTask> unfinishedReviewsFor(UUID learnerId);

    Optional<ReviewTask> findReview(UUID reviewId);

    /**
     * Atomically claims a Due Review for start: the conditional DUE to
     * STARTED transition stamped with the given started time. Returns the
     * claimed Review or empty when the task is no longer Due (not yet due,
     * already started, completed, or cancelled). Only the winning caller may
     * proceed to bind an attempt; a losing or racing start creates nothing.
     */
    Optional<ReviewTask> claimReviewStarted(UUID reviewId, Instant startedAt);

    /**
     * Atomically releases a claimed Review back to Due when start generation,
     * Source Gap, or Task Verification made the Review unavailable. Only the
     * original claimant, identified by the matching started time, may
     * release, so a different process cannot free someone else's claim.
     */
    Optional<ReviewTask> releaseReviewToDue(UUID reviewId, Instant startedAt);
}
