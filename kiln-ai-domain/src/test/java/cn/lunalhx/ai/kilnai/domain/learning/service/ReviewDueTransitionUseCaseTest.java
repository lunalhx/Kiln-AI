package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
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

class ReviewDueTransitionUseCaseTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-16T11:00:00Z"), ZoneOffset.UTC);
    private static final UUID LEARNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID LEARNER_TWO = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CONCEPT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CONCEPT_TWO = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Test
    void aScheduledReviewWhoseDueTimeHasArrivedBecomesDue() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTask scheduled = schedule(store, LEARNER, CONCEPT, Instant.parse("2026-08-16T10:00:00Z"));

        int transitions = new ReviewDueTransitionUseCase(store, CLOCK).markDueReviewsDue();

        assertEquals(1, transitions);
        ReviewTask review = store.findReview(scheduled.reviewId()).orElseThrow();
        assertEquals(ReviewTaskStatus.DUE, review.status());
        assertEquals(Instant.parse("2026-08-16T10:00:00Z"), review.dueAt(),
                "the due time itself must never change");
        assertTrue(review.isUnfinished());
    }

    @Test
    void aPreDueScheduledReviewStaysScheduled() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTask scheduled = schedule(store, LEARNER, CONCEPT, Instant.parse("2026-08-16T12:00:00Z"));

        int transitions = new ReviewDueTransitionUseCase(store, CLOCK).markDueReviewsDue();

        assertEquals(0, transitions);
        assertEquals(ReviewTaskStatus.SCHEDULED,
                store.findReview(scheduled.reviewId()).orElseThrow().status());
    }

    @Test
    void repeatedTicksAreIdempotentAndAnOverdueDueReviewStaysDue() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTask scheduled = schedule(store, LEARNER, CONCEPT, Instant.parse("2026-08-16T10:00:00Z"));
        ReviewDueTransitionUseCase useCase = new ReviewDueTransitionUseCase(store, CLOCK);

        useCase.markDueReviewsDue();
        int secondTick = useCase.markDueReviewsDue();
        int thirdTick = useCase.markDueReviewsDue();

        assertEquals(0, secondTick, "a repeated tick must not re-transition anything");
        assertEquals(0, thirdTick);
        ReviewTask review = store.findReview(scheduled.reviewId()).orElseThrow();
        assertEquals(ReviewTaskStatus.DUE, review.status(),
                "an overdue Due Review must remain Due, never Scheduled again");
        assertEquals(1, store.unfinishedReviewsFor(LEARNER).size(),
                "the at-most-one unfinished invariant must survive repeated ticks");
    }

    @Test
    void theTransitionNeverCreatesPackagesAttemptsEvidenceExposureOrFlowWork() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        UUID flowId = UUID.randomUUID();
        ReviewTask scheduled = schedule(store, LEARNER, CONCEPT, Instant.parse("2026-08-16T10:00:00Z"));
        int evidenceBefore = store.allEvidence().size();

        new ReviewDueTransitionUseCase(store, CLOCK).markDueReviewsDue();

        assertEquals(ReviewTaskStatus.DUE, store.findReview(scheduled.reviewId()).orElseThrow().status());
        assertEquals(evidenceBefore, store.allEvidence().size(),
                "a scheduler tick must never accept Evidence");
        assertTrue(store.exposedTaskFingerprints(flowId).isEmpty(),
                "a scheduler tick must never record Exposure");
        assertTrue(store.exposedSolutionFingerprints(flowId).isEmpty());
        assertTrue(store.findCommand(UUID.randomUUID()).isEmpty(),
                "a scheduler tick must never process a learner command");
        assertTrue(store.findFlow(flowId).isEmpty(),
                "a scheduler tick must never create a Learning Flow");
    }

    @Test
    void reviewsOfEveryLearnerAreTransitionedTogetherAndOneUnfinishedRemainsPerConcept() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTask dueOne = schedule(store, LEARNER, CONCEPT, Instant.parse("2026-08-16T10:00:00Z"));
        schedule(store, LEARNER_TWO, CONCEPT, Instant.parse("2026-08-16T10:30:00Z"));
        schedule(store, LEARNER_TWO, CONCEPT_TWO, Instant.parse("2026-08-17T00:00:00Z"));

        int transitions = new ReviewDueTransitionUseCase(store, CLOCK).markDueReviewsDue();

        assertEquals(2, transitions);
        assertEquals(ReviewTaskStatus.DUE, store.findReview(dueOne.reviewId()).orElseThrow().status());
        assertEquals(1, store.unfinishedReviewsFor(LEARNER).size());
        assertEquals(2, store.unfinishedReviewsFor(LEARNER_TWO).size(),
                "different Concepts may each keep one unfinished Review");
        assertEquals(1,
                store.unfinishedReviewsFor(LEARNER_TWO).stream()
                        .filter(review -> review.conceptId().equals(CONCEPT)).count());
        assertEquals(1,
                store.unfinishedReviewsFor(LEARNER_TWO).stream()
                        .filter(review -> review.conceptId().equals(CONCEPT_TWO)).count());
    }

    private ReviewTask schedule(InMemoryLearningFlowStore store, UUID learner, UUID concept, Instant dueAt) {
        return store.acceptEvidenceAndScheduleFirstReview(
                new AcceptedLearningEvidence(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), concept, learner,
                        LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0, List.of(),
                        Instant.parse("2026-08-15T10:00:00Z")),
                dueAt);
    }
}
