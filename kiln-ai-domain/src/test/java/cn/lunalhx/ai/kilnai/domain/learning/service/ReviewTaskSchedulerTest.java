package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor.PreparedDelivery;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewTaskSchedulerTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
    private static final UUID LEARNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CONCEPT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant FIRST_INDEPENDENT_AT = Instant.parse("2026-08-15T10:00:00Z");

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

    @Test
    void aStartedReviewOnePassCompletesItAndSchedulesReviewTwoThreeDaysAfterTheActualCompletion() {
        Harness harness = harness();
        ReviewTask started = harness.startNextDueReview();

        AcceptedLearningEvidence evidence = reviewEvidence(Instant.parse("2026-08-16T10:00:00Z"));
        Optional<ReviewTaskStore.ReviewAdvance> advance =
                harness.scheduler().acceptEvidenceAndAdvanceReview(evidence);

        assertTrue(advance.isPresent());
        ReviewTaskStore.ReviewAdvance value = advance.get();
        assertEquals(evidence, value.evidence());
        ReviewTask completed = value.completedReview();
        assertEquals(started.reviewId(), completed.reviewId());
        assertEquals(ReviewTaskStatus.COMPLETED, completed.status());
        assertEquals(Instant.parse("2026-08-16T10:00:00Z"), completed.completedAt(),
                "the Review must be completed at the actual evidence acceptance time");
        ReviewTask successor = value.successor();
        assertNotNull(successor);
        assertEquals(2, successor.reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, successor.status());
        assertEquals(Instant.parse("2026-08-19T10:00:00Z"), successor.dueAt(),
                "Review 2 must be due exactly 3 days after the actual Review 1 completion");
        assertEquals(1, harness.store().allEvidence().stream()
                        .filter(AcceptedLearningEvidence::isReviewSuccess).count(),
                "exactly one Review PASS evidence must be accepted");
        assertEquals(1, harness.store().unfinishedReviewsFor(LEARNER).size(),
                "only the successor Review may remain unfinished");
    }

    @Test
    void successfulReviewsChainThroughThreeSevenAndTwentyOneDayIntervalsAndTheFourthStops() {
        Harness harness = harness();

        harness.startNextDueReview();
        Instant firstCompletion = Instant.parse("2026-08-17T08:00:00Z");
        ReviewTaskStore.ReviewAdvance first = advance(harness, firstCompletion);
        assertEquals(2, first.successor().reviewNumber());
        assertEquals(firstCompletion.plus(Duration.ofDays(3)), first.successor().dueAt());

        harness.startNextDueReview();
        Instant secondCompletion = Instant.parse("2026-08-20T09:00:00Z");
        ReviewTaskStore.ReviewAdvance second = advance(harness, secondCompletion);
        assertEquals(3, second.successor().reviewNumber());
        assertEquals(secondCompletion.plus(Duration.ofDays(7)), second.successor().dueAt());

        harness.startNextDueReview();
        Instant thirdCompletion = Instant.parse("2026-08-27T10:00:00Z");
        ReviewTaskStore.ReviewAdvance third = advance(harness, thirdCompletion);
        assertEquals(4, third.successor().reviewNumber());
        assertEquals(thirdCompletion.plus(Duration.ofDays(21)), third.successor().dueAt());

        harness.startNextDueReview();
        Instant fourthCompletion = Instant.parse("2026-09-17T11:00:00Z");
        ReviewTaskStore.ReviewAdvance fourth = advance(harness, fourthCompletion);
        assertNull(fourth.successor(), "Review 4 must schedule no successor");
        assertEquals(4, fourth.completedReview().reviewNumber());
        assertEquals(ReviewTaskStatus.COMPLETED, fourth.completedReview().status());
        assertEquals(4, harness.store().allEvidence().stream()
                        .filter(AcceptedLearningEvidence::isReviewSuccess).count(),
                "the full cadence must accept exactly four Review PASS records");
        assertTrue(harness.store().unfinishedReviewsFor(LEARNER).isEmpty(),
                "Durable ends the cadence with no unfinished Review work");
    }

    @Test
    void aLateReviewCompletionNeverCompressesTheNextInterval() {
        Harness harness = harness();
        ReviewTask started = harness.startNextDueReview();
        Instant scheduledCompletion = Instant.parse("2026-08-17T10:00:00Z");
        harness.scheduler().acceptEvidenceAndAdvanceReview(reviewEvidence(scheduledCompletion));

        ReviewTask late = harness.startNextDueReview();
        assertEquals(2, late.reviewNumber());
        Instant lateCompletion = Instant.parse("2026-08-20T14:00:00Z");
        ReviewTaskStore.ReviewAdvance advance =
                harness.scheduler().acceptEvidenceAndAdvanceReview(reviewEvidence(lateCompletion)).orElseThrow();

        assertEquals(lateCompletion.plus(Duration.ofDays(7)), advance.successor().dueAt(),
                "the next due time must be measured from the late actual completion, never the scheduled time");
    }

    @Test
    void repeatedAdvanceOfTheSameTaskAttemptNeverDuplicatesEvidenceCompletionOrSuccessor() {
        Harness harness = harness();
        ReviewTask started = harness.startNextDueReview();
        AcceptedLearningEvidence evidence = reviewEvidence(Instant.parse("2026-08-16T10:00:00Z"));

        harness.scheduler().acceptEvidenceAndAdvanceReview(evidence);
        Optional<ReviewTaskStore.ReviewAdvance> replayed =
                harness.scheduler().acceptEvidenceAndAdvanceReview(evidence);

        assertTrue(replayed.isEmpty(), "a repeated advance of the same attempt must write nothing");
        assertEquals(1, harness.store().allEvidence().stream()
                        .filter(AcceptedLearningEvidence::isReviewSuccess).count(),
                "a repeated advance must never duplicate Evidence");
        assertEquals(ReviewTaskStatus.COMPLETED,
                harness.store().findReview(started.reviewId()).orElseThrow().status());
        assertEquals(1, harness.store().unfinishedReviewsFor(LEARNER).size(),
                "a repeated advance must never create a second successor");
        ReviewTask successor = harness.store().unfinishedReviewsFor(LEARNER).get(0);
        assertEquals(2, successor.reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, successor.status());
    }

    @Test
    void anAdvanceWithoutAStartedReviewWritesNothing() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTaskScheduler scheduler = new ReviewTaskScheduler(store);
        scheduler.acceptEvidenceAndScheduleFirstReview(independentEvidence(FIRST_INDEPENDENT_AT));

        Optional<ReviewTaskStore.ReviewAdvance> advance =
                scheduler.acceptEvidenceAndAdvanceReview(reviewEvidence(Instant.parse("2026-08-16T10:00:00Z")));

        assertTrue(advance.isEmpty(), "no STARTED Review means the cadence cannot advance");
        assertTrue(store.allEvidence().stream()
                        .noneMatch(AcceptedLearningEvidence::isReviewSuccess),
                "no Evidence may be accepted without a started Review");
        assertEquals(1, store.unfinishedReviewsFor(LEARNER).size(),
                "the untouched Scheduled Review must survive");
        assertEquals(ReviewTaskStatus.SCHEDULED,
                store.unfinishedReviewsFor(LEARNER).get(0).status());
    }

    private ReviewTaskStore.ReviewAdvance advance(Harness harness, Instant acceptedAt) {
        return harness.scheduler().acceptEvidenceAndAdvanceReview(reviewEvidence(acceptedAt)).orElseThrow();
    }

    private static AcceptedLearningEvidence reviewEvidence(Instant acceptedAt) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CONCEPT, LEARNER,
                LearningResult.PASS, AttemptPurpose.REVIEW, 0, List.of(), acceptedAt);
    }

    private Harness harness() {
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK, artifacts);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(
                ReferenceBundles.stack(),
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                artifacts);
        return new Harness(store, executor);
    }

    /**
     * The store-level start path used to reach a STARTED Review: schedule the
     * successor through the previous advance, mark it Due, and bind one fresh
     * generated Review Package.
     */
    private record Harness(InMemoryLearningFlowStore store, ApplyProfileExecutor executor) {

        ReviewTask startNextDueReview() {
            List<ReviewTask> unfinished = store.unfinishedReviewsFor(LEARNER);
            ReviewTask due = unfinished.isEmpty()
                    ? store.acceptEvidenceAndScheduleFirstReview(
                            independentEvidence(FIRST_INDEPENDENT_AT),
                            FIRST_INDEPENDENT_AT.plus(Duration.ofHours(24)))
                    : unfinished.get(0);
            store.markDueReviewsDue(due.dueAt());
            PreparedDelivery delivery = executor.prepareTask(ReviewApplyFixture.reviewContext());
            TaskPackage base = ((PreparedDelivery.TaskReady) delivery).taskPackage();
            TaskPackage fresh = new TaskPackage(
                    TaskPackage.SCHEMA, UUID.randomUUID(), AttemptPurpose.REVIEW,
                    base.learnerProjection(), base.privateAssessorProjection());
            store.bindReviewAttempt(new ReviewTaskStore.ReviewStartBind(
                    due.reviewId(), CLOCK.instant(), due.flowId(), fresh,
                    1, UUID.randomUUID(), "hash"));
            return store.findReview(due.reviewId()).orElseThrow();
        }

        ReviewTaskScheduler scheduler() {
            return new ReviewTaskScheduler(store);
        }
    }

    private static AcceptedLearningEvidence independentEvidence(Instant acceptedAt) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CONCEPT, LEARNER,
                LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0, List.of(), acceptedAt);
    }
}
