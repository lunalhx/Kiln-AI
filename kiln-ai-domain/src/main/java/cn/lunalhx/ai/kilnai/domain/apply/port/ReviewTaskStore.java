package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
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
     * The single STARTED Review of one learner and Concept, when one exists.
     * The at-most-one-unfinished invariant guarantees at most one row.
     */
    Optional<ReviewTask> findStartedReview(UUID learnerId, UUID conceptId);

    /**
     * Atomically accepts one qualifying Review PASS evidence, completes the
     * given started Review at the evidence acceptance time, and schedules the
     * successor Review Task due at {@code nextDueAt} — or schedules nothing
     * when {@code nextDueAt} is null (Review 4). The transition is exactly
     * once per Task Attempt: repeating it for an attempt that already has
     * Evidence writes nothing, and it writes nothing at all when the Review is
     * not STARTED. The caller computes {@code nextDueAt} from its cadence
     * policy; the store owns only the atomic transition and the unfinished
     * Review invariant.
     */
    Optional<ReviewAdvance> acceptEvidenceAndAdvanceReview(
            AcceptedLearningEvidence evidence,
            UUID completedReviewId,
            Instant nextDueAt);

    /**
     * The durable outcome of one accepted Review pass: the accepted evidence,
     * the completed Review Task, and the scheduled successor (null when the
     * cadence ended at Review 4).
     */
    record ReviewAdvance(
            AcceptedLearningEvidence evidence,
            ReviewTask completedReview,
            ReviewTask successor
    ) {

        public ReviewAdvance {
            Objects.requireNonNull(evidence, "evidence must not be null");
            Objects.requireNonNull(completedReview, "completedReview must not be null");
        }
    }

    /**
     * Atomically accepts one conclusive no-hint Review FAIL evidence,
     * completes the given started Review at the evidence acceptance time, and
     * defensively cancels any other unfinished Review of the same learner and
     * Concept. The transition is exactly once per Task Attempt: repeating it
     * for an attempt that already has Evidence writes nothing, and it writes
     * nothing at all when the Review is not STARTED. No successor is ever
     * scheduled; the cadence stops.
     */
    Optional<ReviewStop> acceptEvidenceAndFailReview(
            AcceptedLearningEvidence evidence,
            UUID completedReviewId);

    /**
     * The durable outcome of one accepted Review failure: the accepted
     * evidence and the Review Task completed by that failure.
     */
    record ReviewStop(
            AcceptedLearningEvidence evidence,
            ReviewTask completedReview
    ) {

        public ReviewStop {
            Objects.requireNonNull(evidence, "evidence must not be null");
            Objects.requireNonNull(completedReview, "completedReview must not be null");
        }
    }

    /**
     * Atomically binds one Review Attempt in one commit: it claims the Review
     * with a conditional transition — a Due Review becomes Started, or a
     * Started Review that currently holds no open Attempt is resumed — opens
     * the Review Attempt from the generated Task Package, records the
     * exposure, persists the Flow's next Delayed Review interaction with its
     * checkpoint and the processed idempotent command, and returns the durable
     * interaction. Returns empty when the claim fails, in which case nothing at
     * all is written, so a losing, racing, or duplicate start can never create
     * a Package, Attempt, Exposure, or interaction and at most one OPEN
     * Attempt can ever exist per Review Task.
     */
    Optional<ApplyFlowInteraction> bindReviewAttempt(ReviewStartBind bind);

    /**
     * The complete domain-owned specification of one successful Review start
     * or resume. The store builds the open Attempt, the learner interaction,
     * its checkpoint, and the processed command from these fields, so the whole
     * binding is one atomic write.
     */
    record ReviewStartBind(
            UUID reviewId,
            Instant startedAt,
            UUID flowId,
            TaskPackage taskPackage,
            int interactionVersion,
            UUID idempotencyKey,
            String requestHash
    ) {

        public ReviewStartBind {
            Objects.requireNonNull(reviewId, "reviewId must not be null");
            Objects.requireNonNull(startedAt, "startedAt must not be null");
            Objects.requireNonNull(flowId, "flowId must not be null");
            Objects.requireNonNull(taskPackage, "taskPackage must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(requestHash, "requestHash must not be null");
        }
    }

    /**
     * Atomically resolves one closed inconclusive Review submission in one
     * commit: it claims the Started Review whose open Attempt is the submitted
     * one, binds the prepared replacement Package as the Review's single new
     * OPEN Attempt (recording exposure) when one was prepared, or clears the
     * open Attempt leaving the Review resumable when none was, and persists the
     * Flow's next interaction with its checkpoint and the processed idempotent
     * submission command. Returns empty when the claim fails, in which case
     * nothing at all is written, so replay, concurrency, and duplicate
     * submissions can never create a duplicate replacement.
     */
    Optional<ApplyFlowInteraction> resolveInconclusiveSubmission(ResolveInconclusiveBind bind);

    /**
     * The complete domain-owned specification of one inconclusive Review
     * submission resolution. The replacement Package is null exactly when no
     * verified fresh task could be prepared, in which case the Review stays
     * Started with no open Attempt and remains resumable through the start
     * endpoint.
     */
    record ResolveInconclusiveBind(
            UUID reviewId,
            UUID closedAttemptId,
            TaskPackage replacementPackage,
            int interactionVersion,
            String learnerMessage,
            UUID idempotencyKey,
            String requestHash
    ) {

        public ResolveInconclusiveBind {
            Objects.requireNonNull(reviewId, "reviewId must not be null");
            Objects.requireNonNull(closedAttemptId, "closedAttemptId must not be null");
            Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(requestHash, "requestHash must not be null");
        }
    }
}
