package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewTaskSchedulerTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
    private static final UUID LEARNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CONCEPT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void aFreshIndependentPassSchedulesOneScheduledReviewOneDue24HoursAfterAcceptance() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTaskScheduler scheduler = new ReviewTaskScheduler(store);

        AcceptedLearningEvidence evidence = independentEvidence(Instant.parse("2026-08-15T10:00:00Z"));
        ReviewTask review = scheduler.acceptEvidenceAndScheduleFirstReview(evidence);

        assertEquals(1, review.reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, review.status());
        assertEquals(Instant.parse("2026-08-16T10:00:00Z"), review.dueAt(),
                "Review 1 must be due exactly 24 hours after the actual acceptance time");
        assertEquals(LEARNER, review.learnerId());
        assertEquals(CONCEPT, review.conceptId());
        assertEquals(1, store.allEvidence().size(),
                "the Independent evidence must be accepted atomically with the review");
        assertEquals(1, store.unfinishedReviewsFor(LEARNER).size());
        assertTrue(store.findReview(review.reviewId()).isPresent(),
                "the scheduled Review must be durably stored");
    }

    @Test
    void aNewIndependentPassCancelsTheStaleUnfinishedReviewAndSchedulesAFreshReviewOne() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTaskScheduler scheduler = new ReviewTaskScheduler(store);

        AcceptedLearningEvidence first = independentEvidence(Instant.parse("2026-08-15T10:00:00Z"));
        ReviewTask stale = scheduler.acceptEvidenceAndScheduleFirstReview(first);

        AcceptedLearningEvidence second = independentEvidence(Instant.parse("2026-08-15T12:00:00Z"));
        ReviewTask fresh = scheduler.acceptEvidenceAndScheduleFirstReview(second);

        assertEquals(ReviewTaskStatus.CANCELLED, store.findReview(stale.reviewId()).orElseThrow().status(),
                "the stale unfinished Review must be cancelled");
        assertEquals(1, fresh.reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, fresh.status());
        assertEquals(Instant.parse("2026-08-16T12:00:00Z"), fresh.dueAt());
        assertEquals(2, store.allEvidence().size(),
                "both Independent passes remain accepted Evidence");
        assertEquals(List.of(fresh.reviewId()),
                store.unfinishedReviewsFor(LEARNER).stream().map(ReviewTask::reviewId).toList(),
                "at most one unfinished Review must remain");
    }

    @Test
    void repeatedEvidenceForTheSameTaskAttemptNeverDuplicatesTheUnfinishedReview() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTaskScheduler scheduler = new ReviewTaskScheduler(store);
        AcceptedLearningEvidence evidence = independentEvidence(Instant.parse("2026-08-15T10:00:00Z"));

        scheduler.acceptEvidenceAndScheduleFirstReview(evidence);
        scheduler.acceptEvidenceAndScheduleFirstReview(evidence);

        assertEquals(1, store.allEvidence().size(),
                "accepting the same task attempt twice must never duplicate Evidence");
        assertEquals(1, store.unfinishedReviewsFor(LEARNER).size(),
                "the unfinished Review invariant must hold after a repeated accept");
    }

    private AcceptedLearningEvidence independentEvidence(Instant acceptedAt) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CONCEPT, LEARNER,
                LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0, List.of(), acceptedAt);
    }
}
