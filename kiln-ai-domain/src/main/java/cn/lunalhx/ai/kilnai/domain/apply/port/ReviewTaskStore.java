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
     * Atomically binds a successful Review start in one commit: it claims the
     * Due Review with the conditional DUE to STARTED transition, opens the
     * Review Attempt from the generated Task Package, records the exposure,
     * persists the Flow's next Delayed Review interaction with its checkpoint
     * and the processed idempotent command, and returns the durable
     * interaction. Returns empty when the Review is no longer Due, in which
     * case nothing at all is written, so a losing or racing start can never
     * create a Package, Attempt, Exposure, or interaction.
     */
    Optional<ApplyFlowInteraction> bindStartedReview(ReviewStartBind bind);

    /**
     * The complete domain-owned specification of one successful Review start.
     * The store builds the open Attempt, the learner interaction, its
     * checkpoint, and the processed command from these fields, so the whole
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
}
