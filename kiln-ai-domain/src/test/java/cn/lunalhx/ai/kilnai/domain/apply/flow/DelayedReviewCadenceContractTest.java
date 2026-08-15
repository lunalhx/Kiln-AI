package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedResponseVerificationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.MasteryMilestone;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCollectionUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewDueTransitionUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The deterministic Delayed Review loop contract: the complete 1/3/7/21-day
 * cadence through the public durable Apply and Review use cases with scripted
 * model ports and a controllable Clock. One Independent pass schedules Review
 * 1; each conclusive no-hint Review pass completes its Review and schedules
 * the next 3, 7, and then 21 days after the actual completion; the fourth
 * pass projects Durable and schedules nothing further. Lateness never
 * compresses the next interval, and a replayed or duplicate submission can
 * never duplicate Evidence, completion, or successor work.
 */
class DelayedReviewCadenceContractTest {

    private static final UUID LEARNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final Instant START = Instant.parse("2026-08-14T00:00:00Z");

    @Test
    void theFullOneThreeSevenTwentyOneDayCadenceReachesDurableAndEndsWithNoUnfinishedWork() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();

        ReviewTask review1 = harness.onlyUnfinishedReview();
        assertEquals(1, review1.reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, review1.status());
        assertEquals(START.plus(Duration.ofHours(24)), review1.dueAt(),
                "Review 1 must be due 24 hours after the Independent acceptance");

        Instant firstCompletion = START.plus(Duration.ofHours(26));
        harness.clock().set(firstCompletion);
        ReviewTask review2 = harness.passNextReview(1, firstCompletion);
        assertEquals(2, review2.reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, review2.status());
        assertEquals(firstCompletion.plus(Duration.ofDays(3)), review2.dueAt(),
                "Review 2 must be due 3 days after the actual Review 1 completion");

        Instant secondCompletion = firstCompletion.plus(Duration.ofDays(3)).plus(Duration.ofHours(2));
        harness.clock().set(secondCompletion);
        ReviewTask review3 = harness.passNextReview(2, secondCompletion);
        assertEquals(3, review3.reviewNumber());
        assertEquals(secondCompletion.plus(Duration.ofDays(7)), review3.dueAt(),
                "Review 3 must be due 7 days after the actual Review 2 completion");

        Instant thirdCompletion = secondCompletion.plus(Duration.ofDays(7)).plus(Duration.ofHours(1));
        harness.clock().set(thirdCompletion);
        ReviewTask review4 = harness.passNextReview(3, thirdCompletion);
        assertEquals(4, review4.reviewNumber());
        assertEquals(thirdCompletion.plus(Duration.ofDays(21)), review4.dueAt(),
                "Review 4 must be due 21 days after the actual Review 3 completion");

        Instant fourthCompletion = thirdCompletion.plus(Duration.ofDays(21)).plus(Duration.ofHours(3));
        harness.clock().set(fourthCompletion);
        ApplyFlowResult.Boundary finalBoundary = harness.passReview(4);
        assertEquals(FlowStatus.TERMINAL, finalBoundary.interaction().status());
        assertEquals(LearningStage.DELAYED_REVIEW, finalBoundary.interaction().stage());
        assertFalse(finalBoundary.interaction().learnerMessage().contains("8*x^3 - 6*x"),
                "no expected answer may appear in the terminal message");
        assertFalse(finalBoundary.interaction().learnerMessage().contains("assessment"));
        assertFalse(finalBoundary.interaction().learnerMessage().contains("fingerprint"));

        List<AcceptedLearningEvidence> reviewEvidence = harness.reviewEvidence();
        assertEquals(4, reviewEvidence.size(),
                "the full cadence must accept exactly four Review PASS evidence records");
        assertEquals(4, reviewEvidence.stream().map(AcceptedLearningEvidence::taskAttemptId).distinct().count(),
                "each Review PASS must belong to its own attempt");
        assertTrue(reviewEvidence.stream().allMatch(item ->
                        item.result() == LearningResult.PASS
                                && item.highestHintLevel() == 0
                                && item.assistanceTrace().isEmpty()),
                "every qualifying Review pass must be conclusive, no-hint, and unaided");

        ReviewTask completed1 = harness.review(review1.reviewId());
        ReviewTask completed2 = harness.review(review2.reviewId());
        ReviewTask completed3 = harness.review(review3.reviewId());
        ReviewTask completed4 = harness.review(review4.reviewId());
        for (ReviewTask completed : List.of(completed1, completed2, completed3, completed4)) {
            assertEquals(ReviewTaskStatus.COMPLETED, completed.status());
            assertNotNull(completed.completedAt(), "every completed Review must record its completion time");
        }
        assertEquals(firstCompletion, completed1.completedAt());
        assertEquals(secondCompletion, completed2.completedAt());
        assertEquals(thirdCompletion, completed3.completedAt());
        assertEquals(fourthCompletion, completed4.completedAt());

        assertTrue(harness.unfinishedReviews().isEmpty(),
                "Durable must leave no unfinished Review work behind");
        assertNull(harness.collection().unfinishedFor(LEARNER_ID).stream()
                .findAny().map(view -> view.reviewId()).orElse(null));

        ConceptProgress progress = harness.progress();
        assertEquals(MasteryMilestone.DURABLE, progress.currentMilestone());
        assertEquals(MasteryMilestone.DURABLE, progress.highestMilestoneReached());
        assertEquals(LearningStage.DELAYED_REVIEW, progress.currentStage());
    }

    @Test
    void aLateReviewCompletionNeverCompressesTheNextInterval() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();

        Instant scheduledDue = review1.dueAt();
        Instant lateCompletion = scheduledDue.plus(Duration.ofHours(9));
        harness.clock().set(lateCompletion);
        ReviewTask review2 = harness.passNextReview(1, lateCompletion);

        assertEquals(lateCompletion.plus(Duration.ofDays(3)), review2.dueAt(),
                "the successor must be measured from the late actual completion, not the scheduled due time");
        assertFalse(review2.dueAt().equals(scheduledDue.plus(Duration.ofDays(3))),
                "a missed due time must never be silently treated as the completion time");
    }

    @Test
    void aReplayedOrDuplicateReviewSubmissionNeverDuplicatesEvidenceCompletionOrSuccessor() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.clock().set(review1.dueAt().plus(Duration.ofHours(1)));
        harness.dueTransition().markDueReviewsDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) harness.useCase().submit(
                flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());

        ApplyFlowResult.Boundary replayed = (ApplyFlowResult.Boundary) harness.useCase().submit(
                flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(completed.interaction(), replayed.interaction(),
                "a replayed submission key must return the original result");

        assertThrows(ApplicationException.class, () -> harness.useCase().submit(
                        flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                        started.interaction().attemptId(),
                        ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null),
                "a different-key duplicate of the closed Review attempt must conflict without writing");

        assertEquals(1, harness.reviewEvidence().size(),
                "a replay or duplicate must never accept a second Review PASS evidence");
        assertEquals(ReviewTaskStatus.COMPLETED, harness.review(review1.reviewId()).status());
        assertEquals(1, harness.unfinishedReviews().size(),
                "exactly one successor Review must survive a replay or duplicate");
        assertEquals(2, harness.unfinishedReviews().get(0).reviewNumber());
    }

    @Test
    void aFreshInstanceRecoversTheSavedReviewSubmissionEvaluationWithoutRepeatingIt() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.clock().set(review1.dueAt().plus(Duration.ofHours(1)));
        harness.dueTransition().markDueReviewsDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();
        harness.useCase().submit(flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);

        ApplyFlowUseCase recovered = harness.newUseCase();
        ApplyFlowInteraction queried = recovered.query(flowId);
        assertEquals(FlowStatus.TERMINAL, queried.status(),
                "a fresh instance must recover the terminal Review result without regenerating");

        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) recovered.submit(
                flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(queried, replay.interaction());

        assertThrows(ApplicationException.class, () -> recovered.submit(
                        flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                        started.interaction().attemptId(),
                        ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null),
                "a different-key duplicate after recovery must conflict without writing");

        assertEquals(1, harness.reviewEvidence().size(),
                "recovery must never re-accept Review Evidence");
        assertEquals(1, harness.unfinishedReviews().size(),
                "recovery must never duplicate the successor");
        assertEquals(ReviewTaskStatus.COMPLETED, harness.review(review1.reviewId()).status());
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private Harness harness() {
        MutableClock clock = new MutableClock(START);
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(clock);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(clock, artifacts);
        ReviewTaskScheduler reviewScheduler = new ReviewTaskScheduler(flowStore);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(
                ReferenceBundles.stack(),
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(REVIEW_TASK_2, REVIEW_EXPECTED_2),
                        ApplyScriptData.taskReadyJson(REVIEW_TASK_3, REVIEW_EXPECTED_3),
                        ApplyScriptData.taskReadyJson(REVIEW_TASK_4, REVIEW_EXPECTED_4))),
                new ScriptedTaskVerifier(List.of(
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                artifacts);
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)));
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                DiagnosticApplyFixture.diagnosticContext(), IndependentApplyFixture.independentContext(), clock);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                reviewScheduler, clock);
        ReviewSubmissionFlow reviewSubmissionFlow = new ReviewSubmissionFlow(
                artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                reviewScheduler, clock);
        ApplyFlowUseCase useCase = new ApplyFlowUseCase(
                artifacts, flowStore, diagnosticFlow, independentFlow, reviewSubmissionFlow,
                DiagnosticApplyFixture.diagnosticContext(), clock);
        ReviewStartFlow reviewStart = new ReviewStartFlow(
                executor, flowStore, flowStore, ReviewApplyFixture.reviewContext(), clock);
        return new Harness(artifacts, flowStore, clock, reviewScheduler, useCase, reviewStart,
                diagnosticFlow, independentFlow, reviewSubmissionFlow);
    }

    private static final String REVIEW_TASK_2 = "设 p(x) = 3x⁵ − 4x + 2，求 p'(x)。";
    private static final String REVIEW_EXPECTED_2 = "15*x^4 - 4";
    private static final String REVIEW_TASK_3 = "设 q(x) = 6x⁴ + 5x² − 3，求 q'(x)。";
    private static final String REVIEW_EXPECTED_3 = "24*x^3 + 10*x";
    private static final String REVIEW_TASK_4 = "设 r(x) = 7x³ − 2x + 9，求 r'(x)。";
    private static final String REVIEW_EXPECTED_4 = "21*x^2 - 2";

    private record Harness(
            ArtifactStore artifacts,
            InMemoryLearningFlowStore flowStore,
            MutableClock clock,
            ReviewTaskScheduler reviewScheduler,
            ApplyFlowUseCase useCase,
            ReviewStartFlow reviewStart,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            ReviewSubmissionFlow reviewSubmissionFlow
    ) {

        UUID completeIndependentPass() {
            ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(
                    LEARNER_ID, UUID.randomUUID());
            UUID flowId = started.interaction().flowId();
            ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                    flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                    ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
            useCase.submit(flowId, 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                    ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                    ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
            return flowId;
        }

        /** Passes the currently due {@code reviewNumber} and returns the scheduled successor. */
        ReviewTask passNextReview(int reviewNumber, Instant completion) {
            passReview(reviewNumber);
            ReviewTask successor = unfinishedReviews().get(0);
            assertNotNull(successor);
            return successor;
        }

        ApplyFlowResult.Boundary passReview(int reviewNumber) {
            ReviewTask due = unfinishedReviews().stream()
                    .filter(review -> review.reviewNumber() == reviewNumber)
                    .findFirst().orElseThrow();
            dueTransition().markDueReviewsDue();
            ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) reviewStart().start(
                    due.reviewId(), UUID.randomUUID());
            String expected = expectedFor(reviewNumber);
            return (ApplyFlowResult.Boundary) useCase.submit(
                    started.interaction().flowId(), started.interaction().interactionVersion(),
                    UUID.randomUUID(), started.interaction().attemptId(),
                    expected, expected, null);
        }

        private String expectedFor(int reviewNumber) {
            return switch (reviewNumber) {
                case 1 -> ApplyScriptData.REVIEW_EXPECTED_EXPRESSION;
                case 2 -> REVIEW_EXPECTED_2;
                case 3 -> REVIEW_EXPECTED_3;
                case 4 -> REVIEW_EXPECTED_4;
                default -> throw new IllegalArgumentException("unknown review number " + reviewNumber);
            };
        }

        ReviewDueTransitionUseCase dueTransition() {
            return new ReviewDueTransitionUseCase(flowStore, clock);
        }

        ReviewCollectionUseCase collection() {
            return new ReviewCollectionUseCase(flowStore, flowStore);
        }

        ReviewTask onlyUnfinishedReview() {
            List<ReviewTask> unfinished = unfinishedReviews();
            assertEquals(1, unfinished.size());
            return unfinished.get(0);
        }

        List<ReviewTask> unfinishedReviews() {
            return flowStore.unfinishedReviewsFor(LEARNER_ID);
        }

        ReviewTask review(UUID reviewId) {
            return flowStore.findReview(reviewId).orElseThrow();
        }

        List<AcceptedLearningEvidence> reviewEvidence() {
            return flowStore.allEvidence().stream()
                    .filter(item -> item.attemptPurpose() == AttemptPurpose.REVIEW)
                    .toList();
        }

        ConceptProgress progress() {
            List<AcceptedLearningEvidence> evidence = flowStore.allEvidence();
            return new ConceptProgressProjector().project(LEARNER_ID,
                    DiagnosticApplyFixture.CONCEPT_ID, evidence);
        }

        ApplyFlowUseCase newUseCase() {
            return new ApplyFlowUseCase(
                    artifacts, flowStore, diagnosticFlow, independentFlow, reviewSubmissionFlow,
                    DiagnosticApplyFixture.diagnosticContext(), clock);
        }
    }
}
