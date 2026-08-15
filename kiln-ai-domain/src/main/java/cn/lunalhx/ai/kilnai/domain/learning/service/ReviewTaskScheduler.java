package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;

import java.time.Duration;
import java.util.Objects;

/**
 * The domain policy for one cadence transition: a fresh accepted Independent
 * pass cancels any stale unfinished Review of the same learner and Concept
 * and atomically schedules the unique Review 1 due 24 hours after the
 * acceptance time. The scheduler owns the fixed Phase 0 cadence, never a
 * model; the store guarantees the atomic commit.
 */
public final class ReviewTaskScheduler {

    public static final Duration FIRST_REVIEW_DELAY = Duration.ofHours(24);

    private final ReviewTaskStore reviewStore;

    public ReviewTaskScheduler(ReviewTaskStore reviewStore) {
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
    }

    public ReviewTask acceptEvidenceAndScheduleFirstReview(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        return reviewStore.acceptEvidenceAndScheduleFirstReview(
                evidence, evidence.acceptedAt().plus(FIRST_REVIEW_DELAY));
    }
}
