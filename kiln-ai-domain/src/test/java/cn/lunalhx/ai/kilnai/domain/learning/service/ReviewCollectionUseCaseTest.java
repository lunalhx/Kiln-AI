package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.MasteryMilestone;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewCollectionUseCaseTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
    private static final UUID LEARNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CONCEPT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void unfinishedReviewsAreReturnedSortedByDueTimeWithSafeProgress() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTaskScheduler scheduler = new ReviewTaskScheduler(store);

        AcceptedLearningEvidence first = independentEvidence(Instant.parse("2026-08-15T10:00:00Z"));
        scheduler.acceptEvidenceAndScheduleFirstReview(first);
        AcceptedLearningEvidence second = independentEvidence(Instant.parse("2026-08-15T12:00:00Z"));
        scheduler.acceptEvidenceAndScheduleFirstReview(second);

        List<ReviewCollectionUseCase.ReviewTaskView> views = new ReviewCollectionUseCase(store, store)
                .unfinishedFor(LEARNER);

        assertEquals(1, views.size(), "only the fresh unfinished Review remains");
        ReviewCollectionUseCase.ReviewTaskView view = views.get(0);
        assertEquals(CONCEPT, view.conceptId());
        assertEquals(ReviewTaskStatus.SCHEDULED, view.status());
        assertEquals(1, view.reviewNumber());
        assertEquals(Instant.parse("2026-08-16T12:00:00Z"), view.dueAt());
        assertFalse(view.startable(), "Scheduled work is upcoming and never startable");
        assertEquals(MasteryMilestone.INDEPENDENT, view.progress().currentMilestone());
        assertEquals(MasteryMilestone.INDEPENDENT, view.progress().highestMilestoneReached());
        assertEquals(LearningStage.DELAYED_REVIEW, view.progress().currentStage());
    }

    @Test
    void aDueReviewIsMarkedStartableAndCollectionStaysOrderedByDueTime() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        UUID laterConcept = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID earlierConcept = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        store.acceptEvidenceAndScheduleFirstReview(
                independentEvidenceFor(Instant.parse("2026-08-15T10:00:00Z"), earlierConcept),
                Instant.parse("2026-08-16T10:00:00Z"));
        store.acceptEvidenceAndScheduleFirstReview(
                independentEvidenceFor(Instant.parse("2026-08-15T10:00:00Z"), laterConcept),
                Instant.parse("2026-08-16T14:00:00Z"));

        Clock dueClock = Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC);
        new ReviewDueTransitionUseCase(store, dueClock).markDueReviewsDue();

        List<ReviewCollectionUseCase.ReviewTaskView> views = new ReviewCollectionUseCase(store, store)
                .unfinishedFor(LEARNER);
        assertEquals(List.of(earlierConcept, laterConcept),
                views.stream().map(ReviewCollectionUseCase.ReviewTaskView::conceptId).toList(),
                "unfinished Reviews must remain ordered by due time");
        assertTrue(views.get(0).startable(), "the arrived Review must be startable");
        assertEquals(ReviewTaskStatus.DUE, views.get(0).status());
        assertFalse(views.get(1).startable(), "the pre-due Review must stay non-startable");
        assertEquals(ReviewTaskStatus.SCHEDULED, views.get(1).status());
    }

    @Test
    void cancelledReviewsNeverAppearInTheCollection() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTaskScheduler scheduler = new ReviewTaskScheduler(store);
        scheduler.acceptEvidenceAndScheduleFirstReview(independentEvidence(Instant.parse("2026-08-15T10:00:00Z")));
        scheduler.acceptEvidenceAndScheduleFirstReview(independentEvidence(Instant.parse("2026-08-15T12:00:00Z")));

        List<ReviewCollectionUseCase.ReviewTaskView> views = new ReviewCollectionUseCase(store, store)
                .unfinishedFor(LEARNER);

        assertEquals(1, views.size());
        assertEquals(ReviewTaskStatus.SCHEDULED, views.get(0).status());
    }

    @Test
    void aLearnerWithoutReviewsGetsAnEmptyCollection() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);

        List<ReviewCollectionUseCase.ReviewTaskView> views = new ReviewCollectionUseCase(store, store)
                .unfinishedFor(LEARNER);

        assertEquals(List.of(), views);
    }

    private AcceptedLearningEvidence independentEvidence(Instant acceptedAt) {
        return independentEvidenceFor(acceptedAt, CONCEPT);
    }

    private AcceptedLearningEvidence independentEvidenceFor(Instant acceptedAt, UUID concept) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), concept, LEARNER,
                LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0, List.of(), acceptedAt);
    }
}
