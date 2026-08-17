package cn.lunalhx.ai.kilnai.domain.apply.flow;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedClarificationClassifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedExplainGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedHintGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedResponseVerificationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ExplainApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.PracticeApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.TeachBackApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.HintFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningFlowCommandUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningStateGraph;
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
    private static OperatorModelProfilePort profilePort() {
        return () -> ScriptedModelProfile.PROFILE;
    }


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
        LearningFlowResult.Boundary finalBoundary = harness.passReview(4);
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
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());

        LearningFlowResult.Boundary replayed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(completed.interaction(), replayed.interaction(),
                "a replayed submission key must return the original result");

        assertThrows(ApplicationException.class, () -> harness.useCase().submitAnswer(
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
        harness.useCase().submitAnswer(flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);

        LearningFlowCommandUseCase recovered = harness.newUseCase();
        LearningFlowInteraction queried = recovered.query(flowId);
        assertEquals(FlowStatus.TERMINAL, queried.status(),
                "a fresh instance must recover the terminal Review result without regenerating");

        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) recovered.submitAnswer(
                flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(queried, replay.interaction());

        assertThrows(ApplicationException.class, () -> recovered.submitAnswer(
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

    @Test
    void aConclusiveNoHintReviewFailAcceptsOneFailEvidenceCompletesTheReviewAndStopsTheCadence() {
        Harness harness = harness(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        Instant failureAt = review1.dueAt().plus(Duration.ofHours(1));
        harness.clock().set(failureAt);
        harness.dueTransition().markDueReviewsDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        LearningFlowResult.Boundary failed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);

        assertEquals(FlowStatus.TERMINAL, failed.interaction().status());
        assertTrue(failed.interaction().learnerMessage().contains("复习已结束"),
                "a conclusive Review failure must end with the safe learner outcome");
        assertFalse(failed.interaction().learnerMessage().contains(ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                "a Review failure must never leak the expected answer");
        assertFalse(failed.interaction().learnerMessage().contains("assessment"));
        assertFalse(failed.interaction().learnerMessage().contains("fingerprint"));

        List<AcceptedLearningEvidence> reviewEvidence = harness.reviewEvidence();
        assertEquals(1, reviewEvidence.size(), "a conclusive Review failure must accept exactly one FAIL evidence");
        AcceptedLearningEvidence evidence = reviewEvidence.get(0);
        assertEquals(LearningResult.FAIL, evidence.result());
        assertEquals(AttemptPurpose.REVIEW, evidence.attemptPurpose());
        assertEquals(0, evidence.highestHintLevel());
        assertTrue(evidence.assistanceTrace().isEmpty());
        assertEquals(failureAt, evidence.acceptedAt());

        ReviewTask completed = harness.review(review1.reviewId());
        assertEquals(ReviewTaskStatus.COMPLETED, completed.status());
        assertEquals(failureAt, completed.completedAt(), "the Review must complete at the actual failure acceptance");

        assertTrue(harness.unfinishedReviews().isEmpty(),
                "a Review failure must leave no actionable Review behind");
        assertNull(harness.collection().unfinishedFor(LEARNER_ID).stream()
                .findAny().map(view -> view.reviewId()).orElse(null),
                "a Review failure must not schedule any successor");

        ConceptProgress progress = harness.progress();
        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone(),
                "a conclusive Review failure must drop Current Milestone to Learning");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached(),
                "a Review failure must preserve Highest Milestone Reached");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, progress.currentStage());
    }

    @Test
    void aRationaleContradictionOnReviewIsAConclusiveNoHintFailWithTheInconsistencyMessage() {
        Harness harness = harness(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED,
                        RationaleJudgment.CLEARLY_CONTRADICTORY)));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        Instant failureAt = review1.dueAt().plus(Duration.ofHours(1));
        harness.clock().set(failureAt);
        harness.dueTransition().markDueReviewsDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        LearningFlowResult.Boundary failed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION,
                ApplyScriptData.CONTRADICTORY_RATIONALE);

        assertEquals(FlowStatus.TERMINAL, failed.interaction().status());
        assertTrue(failed.interaction().learnerMessage().contains("最终答案与给出的理由不一致"),
                "a rationale contradiction must clearly tell the learner that the final answer contradicts their rationale");
        assertFalse(failed.interaction().learnerMessage().contains(ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                "the contradiction message must never leak the expected answer");
        assertFalse(failed.interaction().learnerMessage().contains("assessment"));
        assertFalse(failed.interaction().learnerMessage().contains("fingerprint"));

        List<AcceptedLearningEvidence> reviewEvidence = harness.reviewEvidence();
        assertEquals(1, reviewEvidence.size(),
                "a Review Blocked by a clearly contradictory rationale must accept exactly one FAIL evidence");
        assertEquals(LearningResult.FAIL, reviewEvidence.get(0).result());
        assertEquals(0, reviewEvidence.get(0).highestHintLevel());

        assertEquals(ReviewTaskStatus.COMPLETED, harness.review(review1.reviewId()).status());
        assertTrue(harness.unfinishedReviews().isEmpty(), "a Blocked Review must stop the cadence like a FAIL");
        ConceptProgress progress = harness.progress();
        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone());
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
    }

    @Test
    void afterAReviewFailureAFreshIndependentPassRestartsTheCadenceFromReviewOne() {
        Harness harness = harness(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)),
                List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.clock().set(review1.dueAt().plus(Duration.ofHours(1)));
        harness.dueTransition().markDueReviewsDue();
        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        harness.useCase().submitAnswer(flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(MasteryMilestone.LEARNING, harness.progress().currentMilestone());
        assertTrue(harness.unfinishedReviews().isEmpty());

        Instant restartAt = review1.dueAt().plus(Duration.ofDays(2));
        harness.clock().set(restartAt);
        harness.completeIndependentPass();

        List<ReviewTask> unfinished = harness.unfinishedReviews();
        assertEquals(1, unfinished.size(), "a fresh Independent pass must restart exactly one Review cadence");
        ReviewTask restarted = unfinished.get(0);
        assertEquals(1, restarted.reviewNumber(), "the restarted cadence must begin again at Review 1");
        assertEquals(ReviewTaskStatus.SCHEDULED, restarted.status());
        assertEquals(restartAt.plus(Duration.ofHours(24)), restarted.dueAt(),
                "the restarted Review 1 must be due 24 hours after the fresh Independent acceptance");
        assertFalse(restarted.reviewId().equals(review1.reviewId()),
                "a restart must create a new Review Task rather than resurrect the failed one");
        assertEquals(ReviewTaskStatus.COMPLETED, harness.review(review1.reviewId()).status(),
                "the failed Review must remain completed and never become actionable again");

        ConceptProgress progress = harness.progress();
        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone());
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
        assertEquals(LearningStage.DELAYED_REVIEW, progress.currentStage());
    }

    @Test
    void submittingTheOpenAttemptOfACancelledReviewAfterARestartCreatesNoEvidenceAndNoError() {
        Harness harness = harness(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)),
                List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.clock().set(review1.dueAt().plus(Duration.ofHours(1)));
        harness.dueTransition().markDueReviewsDue();
        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());

        Instant restartAt = review1.dueAt().plus(Duration.ofDays(2));
        harness.clock().set(restartAt);
        harness.completeIndependentPass();
        assertEquals(ReviewTaskStatus.CANCELLED, harness.review(review1.reviewId()).status(),
                "a fresh Independent pass must cancel the STARTED Review and restart the cadence");
        assertEquals(1, harness.unfinishedReviews().size());

        LearningFlowResult.Boundary outcome = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);

        assertEquals(FlowStatus.TERMINAL, outcome.interaction().status(),
                "a submission on a cancelled Review must end safely instead of erroring");
        assertTrue(outcome.interaction().learnerMessage().contains("复习已结束"));
        assertTrue(harness.reviewEvidence().isEmpty(),
                "a cancelled Review must never accept Review Evidence, PASS or FAIL");
        assertEquals(MasteryMilestone.INDEPENDENT, harness.progress().currentMilestone(),
                "a cancelled Review submission must not change the restarted milestone");
        assertEquals(1, harness.unfinishedReviews().size(),
                "a cancelled Review submission must not disturb the restarted cadence");
        assertEquals(ReviewTaskStatus.CANCELLED, harness.review(review1.reviewId()).status());
    }

    @Test
    void aStaleAttemptOfACancelledReviewCanNeverAdvanceTheRestartedCadence() {
        Harness harness = harness(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)),
                List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION)));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.clock().set(review1.dueAt().plus(Duration.ofHours(1)));
        harness.dueTransition().markDueReviewsDue();
        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());

        Instant restartAt = review1.dueAt().plus(Duration.ofDays(2));
        harness.clock().set(restartAt);
        harness.completeIndependentPass();
        ReviewTask fresh = harness.onlyUnfinishedReview();
        harness.clock().set(fresh.dueAt().plus(Duration.ofHours(1)));
        harness.dueTransition().markDueReviewsDue();
        ReviewStartResult.Boundary freshStarted = (ReviewStartResult.Boundary) harness.reviewStart().start(
                fresh.reviewId(), UUID.randomUUID());

        LearningFlowResult.Boundary outcome = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);

        assertEquals(FlowStatus.TERMINAL, outcome.interaction().status(),
                "a stale attempt of a cancelled Review must end safely instead of erroring");
        assertTrue(outcome.interaction().learnerMessage().contains("复习已结束"));
        assertTrue(harness.reviewEvidence().isEmpty(),
                "a stale attempt must never accept Review Evidence, PASS or FAIL");
        ReviewTask freshAfter = harness.review(fresh.reviewId());
        assertEquals(ReviewTaskStatus.STARTED, freshAfter.status(),
                "the restarted Review must remain the one cadence work item");
        assertEquals(freshStarted.interaction().attemptId(), freshAfter.openAttemptId(),
                "the restarted Review must keep its own open attempt");
        assertEquals(1, harness.unfinishedReviews().size(),
                "a stale attempt must never schedule a successor or cancel the fresh cadence");
        assertEquals(MasteryMilestone.INDEPENDENT, harness.progress().currentMilestone());
        assertEquals(MasteryMilestone.INDEPENDENT, harness.progress().highestMilestoneReached());
    }

    @Test
    void anIndependentSubmissionReplayedAfterTheCrashBetweenCloseAndBoundaryRecoversTheSavedEvaluation() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(
                LEARNER_ID, UUID.randomUUID());
        UUID flowId = started.interaction().flowId();
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID attemptId = transitioned.interaction().attemptId();
        UUID submitKey = UUID.randomUUID();
        // crash after the close: the Attempt is closed durably, the outcome
        // boundary is not committed yet.
        new SubmissionCloser(harness.artifacts(), harness.clock()).close(
                attemptId, AttemptPurpose.INDEPENDENT_TEST,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);

        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, 2, submitKey, attemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);

        assertEquals(FlowStatus.TERMINAL, recovered.interaction().status(),
                "the replayed submission must recover the terminal result instead of reporting already-submitted");
        assertEquals(1, harness.flowStore().allEvidence().stream()
                        .filter(item -> item.attemptPurpose() == AttemptPurpose.INDEPENDENT_TEST).count(),
                "the recovered evaluation must accept exactly one Independent Evidence record");
        assertEquals(1, harness.unfinishedReviews().size(),
                "the recovered evaluation must schedule exactly one Review 1");
        assertEquals(ReviewTaskStatus.SCHEDULED, harness.unfinishedReviews().get(0).status());
        assertEquals(recovered.interaction(), harness.useCase().query(flowId),
                "the recovered boundary must be durably committed");
    }

    @Test
    void aReviewSubmissionReplayedAfterTheCrashBetweenCloseAndBoundaryRecoversTheSavedEvaluation() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.clock().set(review1.dueAt().plus(Duration.ofHours(1)));
        harness.dueTransition().markDueReviewsDue();
        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        UUID attemptId = started.interaction().attemptId();
        UUID submitKey = UUID.randomUUID();
        // crash after the close: the Attempt is closed durably, the outcome
        // boundary is not committed yet.
        new SubmissionCloser(harness.artifacts(), harness.clock()).close(
                attemptId, AttemptPurpose.REVIEW,
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);

        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), submitKey, attemptId,
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);

        assertEquals(FlowStatus.TERMINAL, recovered.interaction().status(),
                "the replayed submission must recover the terminal result instead of reporting already-submitted");
        assertEquals(1, harness.reviewEvidence().size(),
                "the recovered evaluation must accept exactly one Review PASS");
        assertEquals(ReviewTaskStatus.COMPLETED, harness.review(review1.reviewId()).status(),
                "the recovered evaluation must complete the started Review");
        assertEquals(1, harness.unfinishedReviews().size(),
                "the recovered evaluation must schedule exactly one successor");
        assertEquals(2, harness.unfinishedReviews().get(0).reviewNumber());
    }

    @Test
    void aDifferentKeyDuplicateOfACommittedReviewSubmissionConflictsWithoutWriting() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.clock().set(review1.dueAt().plus(Duration.ofHours(1)));
        harness.dueTransition().markDueReviewsDue();
        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        harness.useCase().submitAnswer(flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);

        LearningFlowResult duplicate = harness.useCase().submitAnswer(
                flowId, harness.useCase().query(flowId).interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);

        assertTrue(duplicate instanceof LearningFlowResult.SubmissionIgnored,
                "a different-key duplicate of a committed Review submission must keep the conflict behavior");
        assertEquals(1, harness.reviewEvidence().size(),
                "a duplicate must never accept a second Review Evidence record");
        assertEquals(1, harness.unfinishedReviews().size(),
                "a duplicate must never create a second successor");
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
        return harness(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)));
    }

    private Harness harness(List<ResponseAssessment> assessmentJudgments) {
        return harness(assessmentJudgments, List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                        ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(REVIEW_TASK_2, REVIEW_EXPECTED_2),
                ApplyScriptData.taskReadyJson(REVIEW_TASK_3, REVIEW_EXPECTED_3),
                ApplyScriptData.taskReadyJson(REVIEW_TASK_4, REVIEW_EXPECTED_4)));
    }

    private Harness harness(List<ResponseAssessment> assessmentJudgments, List<String> generationScripts) {
        MutableClock clock = new MutableClock(START);
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(clock);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(clock, artifacts);
        ReviewTaskScheduler reviewScheduler = new ReviewTaskScheduler(flowStore);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(
                ReferenceBundles.stack(),
                new ScriptedApplyGenerationModel(generationScripts),
                new ScriptedTaskVerifier(
                        generationScripts.stream().map(script -> ApplyScriptData.passVerdict()).toList()),
                artifacts);
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(assessmentJudgments);
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                DiagnosticApplyFixture.diagnosticContext(), IndependentApplyFixture.independentContext(), clock);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                reviewScheduler, clock);
        ReviewSubmissionFlow reviewSubmissionFlow = new ReviewSubmissionFlow(
                artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                reviewScheduler, executor, flowStore, ReviewApplyFixture.reviewContext(), clock);
        PracticeSubmissionFlow practiceFlow = new PracticeSubmissionFlow(
                executor, artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                PracticeApplyFixture.practiceContext(), IndependentApplyFixture.independentContext(), clock);
        ExplainFlow explainFlow = new ExplainFlow(
                new ExplainProfileExecutor(ReferenceBundles.explainStack(),
                        new ScriptedExplainGenerationModel(List.of())),
                artifacts, flowStore, ExplainApplyFixture.explainContext());
        HintFlow hintFlow = new HintFlow(
                new ScriptedHintGenerationModel(List.of()), artifacts,
                PracticeApplyFixture.practiceContext().conceptSourcePack());
        TeachBackFlow teachBackFlow = new TeachBackFlow(
                new TeachBackProfileExecutor(ReferenceBundles.teachBackStack(),
                        new ScriptedTeachBackGenerationModel(List.of()),
                        new ScriptedTeachBackTaskVerifier(List.of()), artifacts),
                artifacts, flowStore, new ScriptedTeachBackAssessmentModel(List.of()),
                TeachBackApplyFixture.teachBackContext(), clock);
        LearningStateGraph graph = new LearningStateGraph(
                artifacts, flowStore, flowStore, diagnosticFlow, independentFlow, practiceFlow,
                reviewSubmissionFlow, explainFlow, hintFlow, teachBackFlow,
                new ScriptedPedagogyModel(), new ScriptedClarificationClassifier(), clock);
        LearningFlowCommandUseCase useCase = new LearningFlowCommandUseCase(
                artifacts, flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), profilePort(), clock);
        ReviewStartFlow reviewStart = new ReviewStartFlow(
                executor, flowStore, flowStore, ReviewApplyFixture.reviewContext(), clock);
        return new Harness(artifacts, flowStore, clock, reviewScheduler, useCase, reviewStart,
                diagnosticFlow, independentFlow, reviewSubmissionFlow, graph);
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
            LearningFlowCommandUseCase useCase,
            ReviewStartFlow reviewStart,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            ReviewSubmissionFlow reviewSubmissionFlow,
            LearningStateGraph graph
    ) {

        UUID completeIndependentPass() {
            LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) useCase.start(
                    LEARNER_ID, UUID.randomUUID());
            UUID flowId = started.interaction().flowId();
            LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) useCase.submitAnswer(
                    flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                    ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
            useCase.submitAnswer(flowId, 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
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

        LearningFlowResult.Boundary passReview(int reviewNumber) {
            ReviewTask due = unfinishedReviews().stream()
                    .filter(review -> review.reviewNumber() == reviewNumber)
                    .findFirst().orElseThrow();
            dueTransition().markDueReviewsDue();
            ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) reviewStart().start(
                    due.reviewId(), UUID.randomUUID());
            String expected = expectedFor(reviewNumber);
            return (LearningFlowResult.Boundary) useCase.submitAnswer(
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

        LearningFlowCommandUseCase newUseCase() {
            return new LearningFlowCommandUseCase(
                    artifacts, flowStore, graph, DiagnosticApplyFixture.diagnosticContext(),
                    profilePort(), clock);
        }
    }
}
