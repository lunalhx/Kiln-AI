package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;

import java.time.Instant;
import java.util.List;
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

    List<ReviewTask> unfinishedReviewsFor(UUID learnerId);
}
