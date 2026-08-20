package cn.lunalhx.ai.kilnai.domain.apply.flow;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedModelProfile;

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
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
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
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
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
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The deterministic Inconclusive Review contract (ticket 06): when response
 * assessment cannot conclude, the submitted Review Attempt is closed but no
 * Evidence is accepted, no milestone or cadence position changes, and the
 * learner is never shown as failed. The system prepares a new verified Fresh
 * Equivalent attempt for the same Started Review Task, and when that
 * replacement cannot be prepared the Review stays Started and the same start
 * endpoint safely resumes it. A restarted instance recovers the exact open
 * Review interaction, and replay, concurrency, and duplicate submissions never
 * create duplicate replacements or Evidence.
 */
class InconclusiveReviewReplacementContractTest {
    private static OperatorModelProfilePort profilePort() {
        return () -> ScriptedModelProfile.PROFILE;
    }


    private static final UUID LEARNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final Instant START = Instant.parse("2026-08-14T00:00:00Z");

    private static final String REPLACEMENT_TASK = "设 m(x) = 2x⁵ − x + 4，求 m'(x)。";
    private static final String REPLACEMENT_EXPECTED = "10*x^4 - 1";
    private static final String RESUME_TASK = "设 n(x) = 4x⁵ − 3x + 2，求 n'(x)。";
    private static final String RESUME_EXPECTED = "20*x^4 - 3";

    @Test
    void anInconclusiveSubmissionClosesTheAttemptAndBindsAFreshVerifiedReplacementToTheSameStartedReview() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.makeDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        UUID firstAttemptId = started.interaction().attemptId();
        Instant startedAt = harness.review(review1.reviewId()).startedAt();
        UUID submitKey = UUID.randomUUID();

        LearningFlowResult.Boundary replaced = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), submitKey, firstAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);

        LearningFlowInteraction interaction = replaced.interaction();
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status(),
                "an inconclusive submission continues with a replacement task");
        assertEquals(LearningStage.DELAYED_REVIEW, interaction.stage());
        assertEquals(AttemptPurpose.REVIEW, interaction.attemptPurpose());
        assertNotEquals(firstAttemptId, interaction.attemptId(),
                "the replacement must be a new Attempt, not the closed one");
        assertEquals(REPLACEMENT_TASK, interaction.learnerProjection().taskText());
        assertTrue(interaction.learnerMessage().contains("未能确定"),
                "the learner must see the system-uncertainty notice");
        assertFalse(interaction.learnerMessage().contains("失败"),
                "system uncertainty must never be shown as learner failure");
        assertFalse(interaction.learnerProjection().taskText().contains(REPLACEMENT_EXPECTED),
                "the replacement expected answer must never reach the learner");
        assertFalse(interaction.learnerProjection().taskText().contains("fingerprint"));
        assertFalse(interaction.learnerProjection().taskText().contains("openstax"));

        TaskAttempt closedFirst = harness.attempt(firstAttemptId);
        assertEquals(AttemptStatus.SUBMITTED, closedFirst.status(),
                "the inconclusive submission must close the submitted Attempt");
        assertNotNull(closedFirst.submission());

        TaskAttempt replacement = harness.attempt(interaction.attemptId());
        assertEquals(AttemptStatus.OPEN, replacement.status());
        assertEquals(AttemptPurpose.REVIEW, replacement.purpose());
        TaskPackage replacementPackage = harness.artifacts().findPackage(replacement.taskPackageId()).orElseThrow();
        assertEquals(1, harness.artifacts().verificationsFor(replacementPackage.taskPackageId()).size(),
                "the replacement task must pass isolated Task Verification before exposure");
        assertTrue(harness.artifacts().verificationsFor(replacementPackage.taskPackageId()).get(0).passed());

        ReviewTask review = harness.review(review1.reviewId());
        assertEquals(ReviewTaskStatus.STARTED, review.status(),
                "the same Started Review Task carries the replacement");
        assertEquals(interaction.attemptId(), review.openAttemptId(),
                "the Review Task must point at its single open Attempt");
        assertEquals(startedAt, review.startedAt(),
                "a replacement must not reset the Review start time");

        assertEquals(4, harness.artifacts().allPackages().size(),
                "the replacement must create exactly one new Package");
        assertEquals(2, harness.artifacts().allPackages().stream()
                .filter(package_ -> package_.attemptPurpose() == AttemptPurpose.REVIEW).count());
        assertNotEquals(
                harness.artifacts().findPackage(closedFirst.taskPackageId()).orElseThrow()
                        .privateAssessorProjection().taskFingerprint(),
                replacementPackage.privateAssessorProjection().taskFingerprint(),
                "the replacement must be a fresh task, never the exposed one");
        assertEquals(4, harness.flowStore().exposedTaskFingerprints(flowId).size(),
                "the replacement exposure must be recorded in the Exposure Ledger");
        assertEquals(4, harness.flowStore().exposedSolutionFingerprints(flowId).size());
        assertTrue(harness.generation().lastContextJson().contains(
                        harness.flowStore().exposedTaskFingerprints(flowId).get(2)),
                "the replacement generation must exclude the previously exposed Review task fingerprint");
        assertFalse(harness.generation().lastContextJson().contains(
                        ApplyScriptData.UNDECIDABLE_DERIVATIVE),
                "the learner answer must never reach the replacement generation");

        assertTrue(harness.reviewEvidence().isEmpty(),
                "an inconclusive submission must accept no Review Evidence");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the accepted Independent evidence is untouched");
        ConceptProgress progress = harness.progress();
        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "an inconclusive submission must not change the milestone");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
        assertEquals(1, harness.unfinishedReviews().size(),
                "an inconclusive submission must not advance the cadence");
        assertEquals(review1.reviewId(), harness.unfinishedReviews().get(0).reviewId());
        assertEquals(1, harness.unfinishedReviews().get(0).reviewNumber());

        LearningFlowResult.Boundary replayed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), submitKey, firstAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        assertEquals(replaced.interaction(), replayed.interaction(),
                "a replayed submission key must return the original replacement result");
        assertEquals(4, harness.artifacts().allPackages().size(),
                "a replay must never create a duplicate replacement");

        assertThrows(ApplicationException.class, () -> harness.useCase().submitAnswer(
                        flowId, started.interaction().interactionVersion(), UUID.randomUUID(), firstAttemptId,
                        ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null),
                "a different-key duplicate of the closed attempt must conflict without writing");
        assertEquals(4, harness.artifacts().allPackages().size());
        assertThrows(ApplicationException.class, () -> harness.reviewStart().start(
                        review1.reviewId(), UUID.randomUUID()),
                "a Started Review with an open Attempt is never startable again");
        assertEquals(4, harness.artifacts().allPackages().size(),
                "a refused start must never create a second replacement");
    }

    @Test
    void anInconclusiveSubmissionWhoseReplacementCannotBePreparedStaysStartedAndResumesThroughTheStartEndpoint() {
        Harness harness = harness(new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                        ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                ApplyScriptData.sourceGapJson(),
                ApplyScriptData.taskReadyJson(RESUME_TASK, RESUME_EXPECTED))));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.makeDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());

        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        assertEquals(FlowStatus.TERMINAL, unavailable.interaction().status(),
                "an unavailable replacement must not leave an answerable interaction");
        assertTrue(unavailable.interaction().learnerMessage().contains("未能确定"));
        assertTrue(unavailable.interaction().learnerMessage().contains("继续"),
                "the learner must be told the Review can be continued");
        assertFalse(unavailable.interaction().learnerMessage().contains("失败"));

        ReviewTask review = harness.review(review1.reviewId());
        assertEquals(ReviewTaskStatus.STARTED, review.status(),
                "an unavailable replacement must keep the Review Started");
        assertNull(review.openAttemptId(),
                "with no open Attempt the Review must be resumable");
        assertEquals(3, harness.artifacts().allPackages().size(),
                "an unavailable replacement must create no Package or Attempt");
        assertEquals(3, harness.flowStore().exposedTaskFingerprints(flowId).size(),
                "an unavailable replacement must create no Exposure");
        assertTrue(harness.reviewEvidence().isEmpty(),
                "an unavailable replacement must accept no Evidence");
        assertEquals(MasteryMilestone.INDEPENDENT, harness.progress().currentMilestone());

        ReviewCollectionUseCase.ReviewTaskView view = harness.collection().unfinishedFor(LEARNER_ID).get(0);
        assertEquals(ReviewTaskStatus.STARTED, view.status());
        assertTrue(view.startable(),
                "a resumable Started Review must advertise the continuation action");

        UUID resumeKey = UUID.randomUUID();
        ReviewStartResult.Boundary resumed = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), resumeKey);
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, resumed.interaction().status());
        assertEquals(AttemptPurpose.REVIEW, resumed.interaction().attemptPurpose());
        assertEquals(RESUME_TASK, resumed.interaction().learnerProjection().taskText());
        assertNotNull(resumed.interaction().attemptId());
        assertEquals(resumed.interaction().attemptId(), harness.review(review1.reviewId()).openAttemptId(),
                "the resumed Review must be bound to its single open Attempt");
        assertTrue(harness.generation().lastContextJson().contains(
                        harness.flowStore().exposedTaskFingerprints(flowId).get(2)),
                "the resumed generation must still exclude every previously exposed fingerprint");
        assertEquals(4, harness.artifacts().allPackages().size(),
                "the resume must create exactly one new Package");
        assertEquals(4, harness.flowStore().exposedTaskFingerprints(flowId).size());

        ReviewStartResult.Boundary replayedResume = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), resumeKey);
        assertEquals(resumed.interaction(), replayedResume.interaction(),
                "a replayed resume key must return the original interaction");
        assertEquals(4, harness.artifacts().allPackages().size());

        assertThrows(ApplicationException.class, () -> harness.reviewStart().start(
                        review1.reviewId(), UUID.randomUUID()),
                "a second resume with a new key must conflict without a duplicate replacement");
        assertEquals(4, harness.artifacts().allPackages().size());

        LearningFlowResult.Boundary passed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, resumed.interaction().interactionVersion(), UUID.randomUUID(),
                resumed.interaction().attemptId(), RESUME_EXPECTED, RESUME_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, passed.interaction().status());
        assertEquals(1, harness.reviewEvidence().size(),
                "the resumed attempt can complete the Review with exactly one PASS evidence");
        assertEquals(ReviewTaskStatus.COMPLETED, harness.review(review1.reviewId()).status());
        assertEquals(2, harness.unfinishedReviews().get(0).reviewNumber(),
                "the resumed completion schedules the cadence successor");
    }

    @Test
    void aRestartedInstanceRecoversTheExactReplacementInteractionWithoutDuplicatingAnything() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.makeDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        LearningFlowResult.Boundary replaced = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);

        LearningFlowCommandUseCase recovered = harness.newUseCase();
        assertEquals(replaced.interaction(), recovered.query(flowId),
                "a fresh instance must recover the exact open replacement interaction");

        LearningFlowResult.Boundary passed = (LearningFlowResult.Boundary) recovered.submitAnswer(
                flowId, replaced.interaction().interactionVersion(), UUID.randomUUID(),
                replaced.interaction().attemptId(), REPLACEMENT_EXPECTED, REPLACEMENT_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, passed.interaction().status());
        assertEquals(1, harness.reviewEvidence().size(),
                "recovery must never re-accept Review Evidence");
        assertEquals(ReviewTaskStatus.COMPLETED, harness.review(review1.reviewId()).status());
        assertEquals(1, harness.unfinishedReviews().size(),
                "recovery must never duplicate the successor");
        assertEquals(2, harness.unfinishedReviews().get(0).reviewNumber());
        assertEquals(4, harness.artifacts().allPackages().size(),
                "recovery must never generate a replacement");
    }

    @Test
    void aReplacementWhoseVerificationRejectsEveryCandidateLeavesTheReviewStartedAndResumable() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(REPLACEMENT_TASK, REPLACEMENT_EXPECTED),
                        ApplyScriptData.taskReadyJson(RESUME_TASK, RESUME_EXPECTED),
                        ApplyScriptData.taskReadyJson(RESUME_TASK, RESUME_EXPECTED))),
                new ScriptedAssessmentModel(List.of(
                        ApplyScriptData.responseAssessment(
                                FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED),
                        ApplyScriptData.responseAssessment(
                                FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED),
                        ApplyScriptData.responseAssessment(
                                FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED))),
                new ScriptedTaskVerifier(List.of(
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(),
                        ApplyScriptData.rejectVerdict(),
                        ApplyScriptData.rejectVerdict(),
                        ApplyScriptData.passVerdict())));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.makeDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());

        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        assertEquals(FlowStatus.TERMINAL, unavailable.interaction().status(),
                "a verification-rejected replacement must not leave an answerable interaction");
        assertTrue(unavailable.interaction().learnerMessage().contains("未能确定"));

        ReviewTask review = harness.review(review1.reviewId());
        assertEquals(ReviewTaskStatus.STARTED, review.status(),
                "verification rejection must keep the Review Started");
        assertNull(review.openAttemptId(),
                "with no open Attempt the Review must be resumable");
        assertEquals(3, harness.artifacts().allPackages().size(),
                "rejected replacement candidates must never be persisted as Packages or Attempts");
        assertEquals(3, harness.flowStore().exposedTaskFingerprints(flowId).size(),
                "rejected replacement candidates must never be exposed");
        assertTrue(harness.reviewEvidence().isEmpty(),
                "verification rejection must accept no Evidence");
        assertTrue(harness.collection().unfinishedFor(LEARNER_ID).get(0).startable(),
                "the Review must advertise the continuation action");

        ReviewStartResult.Boundary resumed = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, resumed.interaction().status());
        assertEquals(RESUME_TASK, resumed.interaction().learnerProjection().taskText());
        assertEquals(resumed.interaction().attemptId(), harness.review(review1.reviewId()).openAttemptId());

        LearningFlowResult.Boundary passed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, resumed.interaction().interactionVersion(), UUID.randomUUID(),
                resumed.interaction().attemptId(), RESUME_EXPECTED, RESUME_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, passed.interaction().status());
        assertEquals(1, harness.reviewEvidence().size(),
                "the resumed attempt can complete the Review with exactly one PASS evidence");
        assertEquals(ReviewTaskStatus.COMPLETED, harness.review(review1.reviewId()).status());
    }

    @Test
    void aConclusiveFailureIsNeitherReplacedNorResumable() {
        Harness harness = harness(new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                        ApplyScriptData.REVIEW_EXPECTED_EXPRESSION))),
                new ScriptedAssessmentModel(List.of(
                        ApplyScriptData.responseAssessment(
                                FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED),
                        ApplyScriptData.responseAssessment(
                                FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED))));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review1 = harness.onlyUnfinishedReview();
        harness.makeDue();

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review1.reviewId(), UUID.randomUUID());

        LearningFlowResult.Boundary failed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(FlowStatus.TERMINAL, failed.interaction().status(),
                "a conclusive failure must end the interaction");
        assertEquals(ReviewSubmissionFlow.SAFE_END_MESSAGE, failed.interaction().learnerMessage());

        ReviewTask review = harness.review(review1.reviewId());
        assertEquals(ReviewTaskStatus.COMPLETED, review.status(),
                "a conclusive failure completes the Review and stops the cadence");
        assertNull(review.openAttemptId());
        assertEquals(3, harness.artifacts().allPackages().size(),
                "a conclusive failure must create no replacement");
        assertEquals(1, harness.reviewEvidence().size(),
                "a conclusive failure accepts exactly one Review FAIL evidence");
        assertEquals(LearningResult.FAIL, harness.reviewEvidence().get(0).result());
        assertEquals(MasteryMilestone.LEARNING, harness.progress().currentMilestone(),
                "a conclusive failure projects Current Milestone back to Learning");
        assertEquals(MasteryMilestone.INDEPENDENT, harness.progress().highestMilestoneReached(),
                "a conclusive failure never erases the highest milestone reached");

        ApplicationException conflict = assertThrows(ApplicationException.class,
                () -> harness.reviewStart().start(review1.reviewId(), UUID.randomUUID()));
        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
        assertEquals(3, harness.artifacts().allPackages().size(),
                "the start endpoint must never resurrect a failed Review");
        assertTrue(harness.unfinishedReviews().isEmpty(),
                "a failed Review is terminal work, never advertised as continuable");
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
        return harness(new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                        ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(REPLACEMENT_TASK, REPLACEMENT_EXPECTED))),
                new ScriptedAssessmentModel(List.of(
                        ApplyScriptData.responseAssessment(
                                FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED),
                        ApplyScriptData.responseAssessment(
                                FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED),
                        ApplyScriptData.responseAssessment(
                                FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED))));
    }

    private Harness harness(ScriptedApplyGenerationModel generation) {
        return harness(generation, new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED),
                ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED))));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedAssessmentModel assessment
    ) {
        return harness(generation, assessment, new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(),
                ApplyScriptData.passVerdict(),
                ApplyScriptData.passVerdict(),
                ApplyScriptData.passVerdict(),
                ApplyScriptData.passVerdict())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedAssessmentModel assessment,
            ScriptedTaskVerifier verifier
    ) {
        MutableClock clock = new MutableClock(START);
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(clock);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(clock, artifacts);
        ReviewTaskScheduler reviewScheduler = new ReviewTaskScheduler(flowStore);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(
                ReferenceBundles.stack(), generation, verifier, artifacts);
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_EQUIVALENT,
                        RationaleJudgment.NOT_PROVIDED)));
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, artifacts, flowStore, assessment, verification,
                new cn.lunalhx.ai.kilnai.domain.apply.profile.RationaleEvaluationProfileExecutor(
                        ReferenceBundles.rationaleEvaluationStack(),
                        (profile, prompt, contextJson) ->
                                cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationResult.notApplicable(
                                        List.of(cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationResult.ReasonCode.MATERIAL_GAP))),
                new cn.lunalhx.ai.kilnai.domain.apply.profile.RationaleEvaluationProfileExecutor(
                        ReferenceBundles.counterexampleReviewStack(),
                        (profile, prompt, contextJson) ->
                                cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationResult.notApplicable(
                                        List.of(cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationResult.ReasonCode.MATERIAL_GAP)),
                        cn.lunalhx.ai.kilnai.domain.apply.profile.CounterexampleReviewProfile.BASE_SYSTEM_PROMPT),
                DiagnosticApplyFixture.diagnosticContext(), IndependentApplyFixture.independentContext(), clock);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                artifacts, flowStore, assessment, verification, reviewScheduler, clock);
        ReviewSubmissionFlow reviewSubmissionFlow = new ReviewSubmissionFlow(
                artifacts, flowStore, assessment, verification, reviewScheduler,
                executor, flowStore, ReviewApplyFixture.reviewContext(), clock);
        PracticeSubmissionFlow practiceFlow = new PracticeSubmissionFlow(
                executor, artifacts, flowStore, assessment, verification,
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
                flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), profilePort());
        ReviewStartFlow reviewStart = new ReviewStartFlow(
                executor, flowStore, flowStore, ReviewApplyFixture.reviewContext(), clock);
        return new Harness(artifacts, flowStore, clock, useCase, reviewStart, reviewSubmissionFlow, generation, graph);
    }

    private record Harness(
            ArtifactStore artifacts,
            InMemoryLearningFlowStore flowStore,
            MutableClock clock,
            LearningFlowCommandUseCase useCase,
            ReviewStartFlow reviewStart,
            ReviewSubmissionFlow reviewSubmissionFlow,
            ScriptedApplyGenerationModel generation,
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

        TaskAttempt attempt(UUID attemptId) {
            return artifacts.findAttempt(attemptId).orElseThrow();
        }

        void makeDue() {
            clock.set(START.plus(Duration.ofHours(24)).plus(Duration.ofHours(1)));
            new ReviewDueTransitionUseCase(flowStore, clock).markDueReviewsDue();
        }

        ReviewCollectionUseCase collection() {
            return new ReviewCollectionUseCase(flowStore, flowStore);
        }

        List<AcceptedLearningEvidence> reviewEvidence() {
            return flowStore.allEvidence().stream()
                    .filter(item -> item.attemptPurpose() == AttemptPurpose.REVIEW)
                    .toList();
        }

        ConceptProgress progress() {
            return new ConceptProgressProjector().project(LEARNER_ID,
                    DiagnosticApplyFixture.CONCEPT_ID, flowStore.allEvidence());
        }

        LearningFlowCommandUseCase newUseCase() {
            return new LearningFlowCommandUseCase(
                    flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), profilePort());
        }
    }
}
