package cn.lunalhx.ai.kilnai.domain.learning.graph;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ExplainScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedExplainGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedResponseVerificationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ExplainApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.PracticeApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionRejectionReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
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
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * The whole-graph contract of the Learning Flow command seam: a learner runs
 * the success path — Diagnostic pass, Independent Test pass, Review 1 — and a
 * failed Diagnostic opens the remediation cycle through the Explain teaching
 * boundary, a Continue into fresh Apply Practice, and a conclusive Practice
 * pass into a fresh Independent Test. Every Learner Interaction Boundary is a
 * resumable checkpoint. All model ports are scripted and the stores are
 * durable in-memory, so the test crosses the guarded Learning StateGraph
 * without prompt or live-model variance. Replay of a completed command returns
 * the original interaction and never re-runs a committed transition.
 */
class LearningFlowGraphContractTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);

    private static final UUID LEARNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final UUID CONCEPT_ID = DiagnosticApplyFixture.CONCEPT_ID;

    @Test
    void startCommitsTheFirstInteractionBoundaryWithCheckpointAndCommand() {
        Harness harness = harness();
        UUID key = UUID.randomUUID();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
        ApplyFlowInteraction interaction = started.interaction();
        assertEquals(1, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.DIAGNOSTIC, interaction.stage());
        assertEquals(AttemptPurpose.DIAGNOSTIC, interaction.attemptPurpose());
        assertNotNull(interaction.attemptId());
        assertNotNull(interaction.learnerProjection());
        assertEquals(ApplyScriptData.TASK_TEXT, interaction.learnerProjection().taskText());
        LearningFlowStore.FlowRecord flow = harness.flowStore().findFlow(interaction.flowId()).orElseThrow();
        assertEquals(LEARNER_ID, flow.learnerId());
        assertEquals(CONCEPT_ID, flow.conceptId());
        assertEquals(1, harness.artifacts().allPackages().size(), "the delivered package must be persisted");
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(interaction.attemptId()).orElseThrow().status());
        assertEquals(1, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size());
        assertEquals(1, harness.flowStore().exposedSolutionFingerprints(interaction.flowId()).size());
        SourceArtifact source = harness.artifacts().findSource("openstax-calculus-v1-3.3").orElseThrow();
        assertEquals("1.0.0", source.version());
        assertEquals(interaction, harness.flowStore().latestInteraction(interaction.flowId()).orElseThrow());
        ApplyCheckpoint checkpoint = harness.flowStore().latestCheckpoint(interaction.flowId()).orElseThrow();
        assertEquals(1, checkpoint.interactionVersion(), "the first boundary must commit a checkpoint");
        assertEquals(interaction, harness.flowStore().findCommand(key).orElseThrow().response(),
                "the start command must be persisted with its original result");
    }

    @Test
    void aReplayedStartReturnsTheOriginalInteractionWithoutASecondAttempt() {
        Harness harness = harness();
        UUID key = UUID.randomUUID();
        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
        assertEquals(first.interaction(), replay.interaction());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a replayed start must never create a second Task Package or Attempt");
        assertEquals(1, harness.flowStore().latestInteraction(first.interaction().flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void aReusedKeyWithADifferentPayloadConflicts() {
        Harness harness = harness();
        UUID key = UUID.randomUUID();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
        ApplicationException conflict = assertThrows(ApplicationException.class,
                () -> harness.useCase().start(UUID.randomUUID(), key));
        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a conflicting key must never create a second Task Package");
        assertEquals(started.interaction(), harness.flowStore().latestInteraction(
                started.interaction().flowId()).orElseThrow(),
                "a conflicting key must never advance the original flow");
    }

    @Test
    void aPassingDiagnosticMovesThroughTheNeutralTransitionToAFreshIndependentBoundary() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();
        UUID diagnosticAttemptId = started.interaction().attemptId();
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, submitKey, diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        ApplyFlowInteraction interaction = transitioned.interaction();
        assertEquals(2, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.INDEPENDENT_TEST, interaction.stage());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, interaction.attemptPurpose());
        assertEquals(DiagnosticFlow.NEUTRAL_TRANSITION_MESSAGE, interaction.learnerMessage());
        assertEquals(ApplyScriptData.INDEPENDENT_TASK_TEXT, interaction.learnerProjection().taskText());
        assertEquals(AttemptStatus.SUBMITTED,
                harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow().status());
        assertEquals(2, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size(),
                "the displayed Independent package must also be exposed");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a passing Diagnostic must never create Evidence");
        assertEquals(2, harness.flowStore().latestCheckpoint(interaction.flowId()).orElseThrow().interactionVersion(),
                "the second boundary must commit its own checkpoint");
        assertEquals(interaction, harness.flowStore().findCommand(submitKey).orElseThrow().response(),
                "the submission result must be persisted with its idempotency key");
    }

    @Test
    void aReplayedDiagnosticSubmissionReturnsTheOriginalInteractionWithoutASecondEvaluation() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(first.interaction(), replay.interaction(), "a replayed key must return the original result");
        assertEquals(2, harness.generation().calls().size(), "a replay must never trigger a third generation");
        assertEquals(2, harness.artifacts().allPackages().size(),
                "a replay must never create a second Independent package");
    }

    @Test
    void anIndependentPassAcceptsExactlyOneEvidenceAndSchedulesReviewOne() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, submitKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        ApplyFlowInteraction interaction = completed.interaction();
        assertEquals(3, interaction.interactionVersion());
        assertEquals(FlowStatus.TERMINAL, interaction.status());
        assertEquals(IndependentSubmissionFlow.INDEPENDENT_COMPLETE_MESSAGE, interaction.learnerMessage());
        assertEquals(1, harness.flowStore().allEvidence().size(), "exactly one Evidence record may be accepted");
        AcceptedLearningEvidence evidence = harness.flowStore().allEvidence().get(0);
        assertEquals(independentAttemptId, evidence.taskAttemptId());
        assertEquals(started.interaction().flowId(), evidence.flowId());
        assertEquals(CONCEPT_ID, evidence.conceptId());
        assertEquals(LEARNER_ID, evidence.learnerId());
        assertEquals(LearningResult.PASS, evidence.result());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, evidence.attemptPurpose());
        assertEquals(0, evidence.highestHintLevel(), "a no-hint Independent success must never use a hint");
        assertTrue(evidence.assistanceTrace().isEmpty());
        List<ReviewTask> reviews = harness.flowStore().unfinishedReviewsFor(LEARNER_ID);
        assertEquals(1, reviews.size(), "the Independent pass must schedule the unique Review 1");
        ReviewTask review = reviews.get(0);
        assertEquals(1, review.reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, review.status());
        assertEquals(evidence.acceptedAt().plus(ReviewTaskScheduler.FIRST_REVIEW_DELAY), review.dueAt());
        ConceptProgress progress =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone());
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
        assertEquals(LearningStage.DELAYED_REVIEW, progress.currentStage());
        assertEquals(3, harness.flowStore().latestCheckpoint(interaction.flowId()).orElseThrow().interactionVersion(),
                "the final boundary must commit its checkpoint");
        assertEquals(interaction, harness.flowStore().findCommand(submitKey).orElseThrow().response());
    }

    @Test
    void aReplayedIndependentSubmissionReturnsTheOriginalInteractionWithoutASecondEvaluationOrEvidence() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, submitKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, submitKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(first.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replayed key after completion must never accept a second Evidence");
        assertEquals(1, harness.artifacts().assessmentsFor(independentAttemptId).size(),
                "the isolated assessment record must be persisted exactly once");
    }

    @Test
    void aCommittedOutcomeResubmittedWithANewKeyIsIgnoredWithoutASecondEvaluationOrReview() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        harness.useCase().submitAnswer(started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        ApplyFlowResult.SubmissionIgnored duplicate = (ApplyFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 3, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(SubmissionIgnoreReason.ALREADY_SUBMITTED, duplicate.reason());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "an already-produced outcome with a new key must never accept a second Evidence");
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "an already-produced outcome must never stack a second Review");
        assertEquals(1, harness.artifacts().assessmentsFor(independentAttemptId).size(),
                "an already-produced outcome must never run a second evaluation");
    }

    @Test
    void aReviewPurposeAttemptIsIgnoredAsWrongPurposeWithoutEvaluation() {
        Harness harness = harness();
        ApplyDeliveryResult reviewDelivery = reviewDelivery(harness);
        assertInstanceOf(ApplyDeliveryResult.Delivered.class, reviewDelivery);
        UUID reviewAttemptId = ((ApplyDeliveryResult.Delivered) reviewDelivery).attempt().attemptId();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.SubmissionIgnored wrongPurpose = (ApplyFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), reviewAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE, wrongPurpose.reason());
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(reviewAttemptId).orElseThrow().status(),
                "a wrong-purpose submission must never close the attempt");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aFreshInstanceResumesFromTheCommittedCheckpointAndReplaysExactlyOnce() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowCommandUseCase fresh = harness.newUseCase();
        assertEquals(transitioned.interaction(), fresh.query(started.interaction().flowId()),
                "a fresh instance must recover the latest interaction and checkpoint exactly");
        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) fresh.submitAnswer(
                started.interaction().flowId(), 2, submitKey, transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(3, completed.interaction().interactionVersion());
        assertEquals(1, harness.flowStore().allEvidence().size());
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) fresh.submitAnswer(
                started.interaction().flowId(), 2, submitKey, transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(completed.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replayed key after recovery must never accept a second Evidence");
    }

    @Test
    void aCrashBetweenClosingAndCommittingResumesFromTheSavedAttempt() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        harness.artifacts().closeAttempt(independentAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the crash must leave the closed Attempt without committed Evidence");
        UUID retryKey = UUID.randomUUID();
        ApplyFlowResult.Boundary recovered = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, retryKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(3, recovered.interaction().interactionVersion());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must resume the evaluation of the saved submission exactly once");
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "the resumed transition must still schedule Review 1");
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, retryKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(recovered.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size());
    }

    @Test
    void aStaleInteractionVersionConflictsAndDoesNotAdvance() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplicationException conflict = assertThrows(ApplicationException.class, () -> harness.useCase().submitAnswer(
                started.interaction().flowId(), 0, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null));
        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void anUnknownFlowConflictsAndAnUnknownAttemptIsIgnored() {
        Harness harness = harness();
        ApplicationException missing = assertThrows(ApplicationException.class, () -> harness.useCase().submitAnswer(
                UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID(), "x", "x", null));
        assertEquals(ErrorCode.FLOW_NOT_FOUND, missing.errorCode());
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.SubmissionIgnored unknownAttempt = (ApplyFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), UUID.randomUUID(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND, unknownAttempt.reason());
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void aRejectedSubmissionLeavesTheAttemptOpenAndDoesNotAdvanceTheInteraction() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID attemptId = started.interaction().attemptId();
        ApplyFlowResult.SubmissionRejected rejected = (ApplyFlowResult.SubmissionRejected) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), attemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL + " + 1", null);
        assertEquals(SubmissionRejectionReason.CONFIRMATION_MISMATCH, rejected.reason());
        assertEquals(AttemptStatus.OPEN, harness.artifacts().findAttempt(attemptId).orElseThrow().status(),
                "a rejected submission must leave the attempt open for correction");
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
        ApplyFlowResult.Boundary corrected = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), attemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(2, corrected.interaction().interactionVersion(),
                "a corrected confirmed submission must still advance the flow");
    }

    @Test
    void aClosedDiagnosticAttemptWithANewKeyIsIgnoredWithoutASecondEvaluation() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        harness.useCase().submitAnswer(started.interaction().flowId(), 1, UUID.randomUUID(), diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        ApplyFlowResult.SubmissionIgnored duplicate = (ApplyFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(SubmissionIgnoreReason.ALREADY_SUBMITTED, duplicate.reason());
        assertEquals(2, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion(),
                "an ignored submission must not advance the interaction");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aFailingDiagnosticDeliversAnExplainTeachingBoundaryThatContinueOpensWithPractice() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        ApplyFlowInteraction interaction = explained.interaction();
        assertEquals(2, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertNull(interaction.attemptId(), "Explain must never open a Task Attempt");
        assertNull(interaction.learnerProjection(), "Explain is teaching content, not a task");
        TeachingProjection teaching = interaction.teachingProjection();
        assertNotNull(teaching, "the teaching boundary must carry the learner-visible explanation");
        assertEquals(ExplainScriptData.PRINCIPLE_SUMMARY, teaching.principleSummary());
        assertEquals(ExplainScriptData.EXPLAIN_PROBLEM, teaching.workedExample().problem());
        assertEquals(4, teaching.workedExample().steps().size(), "exactly one complete worked example");
        assertEquals(ExplainScriptData.EXPLAIN_FINAL_RESULT, teaching.workedExample().finalResult());
        assertEquals(List.of(ApplyLearnerEvent.CONTINUE_REQUESTED, ApplyLearnerEvent.CLARIFICATION_ASKED,
                ApplyLearnerEvent.FLOW_CONTROL), teaching.allowedEvents());
        assertEquals(ExplainFlow.EXPLAIN_START_MESSAGE, interaction.learnerMessage());
        assertEquals(AttemptStatus.SUBMITTED,
                harness.artifacts().findAttempt(started.interaction().attemptId()).orElseThrow().status(),
                "a failed submitted Diagnostic stays closed and is never retroactively converted");
        assertEquals(1, harness.generation().calls().size(),
                "the Explain boundary must not generate a Practice task yet");
        assertEquals(1, harness.explainGeneration().calls().size(),
                "the Explain node must generate exactly one teaching artifact");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "Explain must never create a Task Package");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "Explain must never create Learning Evidence");
        assertEquals(1, harness.flowStore().exposedExampleFingerprints(interaction.flowId()).size(),
                "the displayed worked example must be recorded for novelty");
        assertEquals(1, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size(),
                "Explain must not record a task fingerprint");

        ApplyFlowResult.Boundary practice = (ApplyFlowResult.Boundary) harness.useCase().continueRequested(
                interaction.flowId(), interaction.interactionVersion(), UUID.randomUUID());
        ApplyFlowInteraction practiceInteraction = practice.interaction();
        assertEquals(3, practiceInteraction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, practiceInteraction.status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, practiceInteraction.stage());
        assertEquals(AttemptPurpose.PRACTICE, practiceInteraction.attemptPurpose());
        assertEquals(ApplyScriptData.PRACTICE_TASK_TEXT, practiceInteraction.learnerProjection().taskText());
        assertEquals(PracticeSubmissionFlow.PRACTICE_START_MESSAGE, practiceInteraction.learnerMessage());
        assertEquals(2, harness.generation().calls().size(),
                "Continue must deliver exactly one fresh Practice task");
        assertEquals(1, harness.artifacts().verificationsFor(harness.artifacts()
                        .findAttempt(practiceInteraction.attemptId()).orElseThrow().taskPackageId()).size(),
                "the displayed Practice task must be verified before delivery");
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(practiceInteraction.attemptId()).orElseThrow().status());
        assertEquals(2, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "neither Explain nor its Continue may create Evidence");
        assertEquals(3, harness.flowStore().latestCheckpoint(interaction.flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void aReplayedDiagnosticFailureReturnsTheOriginalExplainBoundaryWithoutRegeneration() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID failKey = UUID.randomUUID();
        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, failKey, started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, failKey, started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(first.interaction(), replay.interaction(), "a replayed key must return the original result");
        assertEquals(1, harness.explainGeneration().calls().size(),
                "a replayed failure must never regenerate the teaching artifact");
        assertEquals(1, harness.generation().calls().size(),
                "a replayed failure must never generate a Practice task");
    }

    @Test
    void aReplayedContinueReturnsTheOriginalPracticeBoundaryWithoutRegeneration() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        UUID continueKey = UUID.randomUUID();
        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().continueRequested(
                explained.interaction().flowId(), 2, continueKey);
        assertEquals(3, first.interaction().interactionVersion());
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().continueRequested(
                explained.interaction().flowId(), 2, continueKey);
        assertEquals(first.interaction(), replay.interaction());
        assertEquals(2, harness.generation().calls().size(),
                "a replayed Continue must never regenerate the Practice task");
        assertEquals(3, harness.flowStore().latestInteraction(explained.interaction().flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void aContinueOnANonTeachingBoundaryIsIgnoredWithoutStateChange() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.SubmissionIgnored ignored = (ApplyFlowResult.SubmissionIgnored) harness.useCase()
                .continueRequested(started.interaction().flowId(), 1, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.CONTINUE_NOT_LEGAL, ignored.reason());
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "an illegal Continue must never create a Task Package");
    }

    @Test
    void anExplainSourceGapCommitsOnlyATerminalBoundaryAndContinueIsIgnored() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainSourceGapJson())));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary unavailable = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(2, unavailable.interaction().interactionVersion());
        assertEquals(FlowStatus.TERMINAL, unavailable.interaction().status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, unavailable.interaction().stage());
        assertEquals(ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.interaction().learnerMessage());
        assertNull(unavailable.interaction().attemptId());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a Source Gap must create no Practice Package");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
        assertEquals(0, harness.flowStore().exposedExampleFingerprints(started.interaction().flowId()).size());
        ApplyFlowResult.SubmissionIgnored continueIgnored = (ApplyFlowResult.SubmissionIgnored) harness.useCase()
                .continueRequested(unavailable.interaction().flowId(), 2, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.CONTINUE_NOT_LEGAL, continueIgnored.reason());
        assertEquals(2, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void aSecondInvalidExplainOutputCommitsOnlyATerminalBoundary() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(
                        "{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary unavailable = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(FlowStatus.TERMINAL, unavailable.interaction().status());
        assertEquals(ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.interaction().learnerMessage());
        assertEquals(2, harness.explainGeneration().calls().size(),
                "two invalid candidates must exhaust the single repair");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a failed Explain must leave the Diagnostic Package as the only one");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aPracticePassAcceptsAssistedEvidenceAndDeliversAFreshIndependentTestThatRejoinsReviewOne() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(),
                        conclusivePracticeJudgment(), conclusivePracticeJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary independent = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        ApplyFlowInteraction interaction = independent.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.INDEPENDENT_TEST, interaction.stage(),
                "only a conclusive Practice PASS makes the fresh Independent Test legal");
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, interaction.attemptPurpose());
        assertEquals(ApplyScriptData.INDEPENDENT_TASK_TEXT, interaction.learnerProjection().taskText());
        assertEquals(PracticeSubmissionFlow.INDEPENDENT_READY_MESSAGE, interaction.learnerMessage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the Practice pass must accept exactly one Evidence record");
        AcceptedLearningEvidence evidence = harness.flowStore().allEvidence().get(0);
        assertEquals(practiceAttemptId, evidence.taskAttemptId());
        assertEquals(LearningResult.PASS, evidence.result());
        assertEquals(AttemptPurpose.PRACTICE, evidence.attemptPurpose());
        assertEquals(0, evidence.highestHintLevel());
        assertTrue(evidence.assistanceTrace().isEmpty());
        assertEquals(3, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size(),
                "the fresh Independent task must exclude every exposed Diagnostic and Practice task");
        ConceptProgress progress =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.LEARNING, progress.currentMilestone(),
                "a Practice pass raises the Concept to Learning without pretending Independent");
        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                interaction.flowId(), 4, submitKey, interaction.attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(5, completed.interaction().interactionVersion());
        assertEquals(2, harness.flowStore().allEvidence().size(),
                "the Independent pass after remediation must accept its own Evidence");
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "the post-remediation Independent pass must schedule the unique Review 1");
    }

    @Test
    void aPracticeFailAcceptsFailEvidenceAndDeliversAFreshPracticeTaskNeverAnIndependentOne() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), conclusivePracticeJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary replacement = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        ApplyFlowInteraction interaction = replacement.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, interaction.learnerProjection().taskText());
        assertEquals(PracticeSubmissionFlow.PRACTICE_REPLACEMENT_MESSAGE, interaction.learnerMessage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the Practice fail must accept exactly one Evidence record");
        AcceptedLearningEvidence evidence = harness.flowStore().allEvidence().get(0);
        assertEquals(practiceAttemptId, evidence.taskAttemptId());
        assertEquals(LearningResult.FAIL, evidence.result());
        assertEquals(AttemptPurpose.PRACTICE, evidence.attemptPurpose());
        assertEquals(3, harness.generation().calls().size(),
                "a failing Practice must never generate an Independent task");
        assertEquals(3, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size());
    }

    @Test
    void anInconclusivePracticeCreatesNoEvidenceAndDeliversAFreshReplacement() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), inconclusiveJudgment())),
                new ScriptedResponseVerificationModel(List.of(inconclusiveJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary replacement = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        ApplyFlowInteraction interaction = replacement.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, interaction.learnerProjection().taskText());
        assertEquals(3, harness.generation().calls().size(),
                "an Inconclusive Practice must only deliver a fresh Practice replacement, never an Independent task");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "an Inconclusive Practice judgment must never create Evidence");
        assertEquals(3, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size());
    }

    @Test
    void onlyAQualifyingPracticePassInTheCurrentRemediationCycleDeliversAFreshIndependentTest() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson(),
                        independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(),
                        conclusivePracticeJudgment(), conclusivePracticeJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        ApplyFlowResult.Boundary afterFail = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practice.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, afterFail.interaction().stage(),
                "a conclusive fail must not make the Independent Test legal");
        assertEquals(AttemptPurpose.PRACTICE, afterFail.interaction().attemptPurpose());
        ApplyFlowResult.Boundary afterPass = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                afterFail.interaction().flowId(), 4, UUID.randomUUID(), afterFail.interaction().attemptId(),
                ApplyScriptData.SECOND_PRACTICE_CORRECT_DERIVATIVE,
                ApplyScriptData.SECOND_PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(LearningStage.INDEPENDENT_TEST, afterPass.interaction().stage(),
                "only the current cycle's conclusive Practice PASS can reopen Independent testing");
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, afterPass.interaction().attemptPurpose());
        assertEquals(2, harness.flowStore().allEvidence().size());
        assertTrue(harness.flowStore().allEvidence().stream().allMatch(
                        item -> item.attemptPurpose() == AttemptPurpose.PRACTICE),
                "the cycle may hold a FAIL and a PASS, both as assisted Practice Evidence");
    }

    @Test
    void aReplayedPracticeSubmissionReturnsTheOriginalInteractionWithoutASecondEvaluationOrEvidence() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), conclusivePracticeJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, submitKey, practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, submitKey, practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(first.interaction(), replay.interaction(), "a replayed key must return the original result");
        assertEquals(3, harness.generation().calls().size(),
                "a replay must never regenerate the Independent task");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replayed key must never accept a second Evidence");
        assertEquals(1, harness.artifacts().assessmentsFor(practiceAttemptId).size(),
                "the isolated assessment record must be persisted exactly once");
    }

    @Test
    void aCommittedPracticeOutcomeResubmittedWithANewKeyIsIgnoredAndNeverRewritesTheAttempt() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), conclusivePracticeJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary independent = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        ApplyFlowResult.SubmissionIgnored duplicate = (ApplyFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                independent.interaction().flowId(), 4, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(SubmissionIgnoreReason.ALREADY_SUBMITTED, duplicate.reason());
        TaskAttempt attempt = harness.artifacts().findAttempt(practiceAttemptId).orElseThrow();
        assertEquals(AttemptStatus.SUBMITTED, attempt.status());
        assertEquals(ApplyScriptData.PRACTICE_CORRECT_CANONICAL,
                attempt.submission().finalDerivative().confirmedCanonical(),
                "a closed Attempt must never be rewritten by a later submission");
        assertEquals(1, harness.flowStore().allEvidence().size());
        assertEquals(4, harness.flowStore().latestInteraction(independent.interaction().flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void aCrashBetweenClosingAndCommittingAPracticeSubmissionResumesExactlyOnce() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), conclusivePracticeJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        harness.artifacts().closeAttempt(practiceAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE,
                                ApplyScriptData.PRACTICE_CORRECT_CANONICAL, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the crash must leave the closed Attempt without committed Evidence");
        UUID retryKey = UUID.randomUUID();
        ApplyFlowResult.Boundary recovered = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, retryKey, practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(4, recovered.interaction().interactionVersion());
        assertEquals(LearningStage.INDEPENDENT_TEST, recovered.interaction().stage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must resume the evaluation of the saved submission exactly once");
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, retryKey, practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(recovered.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size());
    }

    @Test
    void aFailedIndependentDeliveryAfterAPracticePassKeepsNoEvidenceAndARetryRecovers() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), ApplyScriptData.sourceGapJson(),
                        independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(),
                        conclusivePracticeJudgment(), conclusivePracticeJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary unavailable = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(4, unavailable.interaction().interactionVersion());
        assertEquals(FlowStatus.TERMINAL, unavailable.interaction().status());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a failed follow-up generation must not accept Practice Evidence");
        assertEquals(3, harness.generation().calls().size());
        ApplyFlowResult.Boundary recovered = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                unavailable.interaction().flowId(), 4, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(5, recovered.interaction().interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, recovered.interaction().status());
        assertEquals(LearningStage.INDEPENDENT_TEST, recovered.interaction().stage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must recover the original outcome exactly once");
        assertEquals(4, harness.generation().calls().size());
    }

    @Test
    void aSourceGapStartCommitsOnlyATerminalBoundaryWithoutAnAttempt() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.sourceGapJson()));
        Harness harness = harness(generation, new ScriptedTaskVerifier(List.of()),
                new ScriptedAssessmentModel(List.of()));
        UUID startKey = UUID.randomUUID();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, startKey);
        assertEquals(1, started.interaction().interactionVersion());
        assertEquals(FlowStatus.TERMINAL, started.interaction().status());
        assertEquals("暂时无法准备一道可验证的题目。请稍后重试。", started.interaction().learnerMessage());
        assertTrue(harness.artifacts().allPackages().isEmpty(), "no Task Package may be created");
        assertEquals(1, harness.flowStore().latestCheckpoint(started.interaction().flowId())
                .orElseThrow().interactionVersion(), "a terminal boundary still commits its checkpoint");
        assertEquals(started.interaction(),
                harness.flowStore().findCommand(startKey).orElseThrow().response());
    }

    @Test
    void anIndependentWrongAnswerNeverAcceptsEvidence() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        ApplyFlowResult.Boundary failed = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(FlowStatus.TERMINAL, failed.interaction().status());
        assertEquals(IndependentSubmissionFlow.SAFE_END_MESSAGE, failed.interaction().learnerMessage());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a proven wrong Independent answer must never create Evidence");
        assertTrue(harness.artifacts().assessmentsFor(independentAttemptId).isEmpty(),
                "a proven non-equivalence must never invoke a model judgment");
    }

    private Harness harness() {
        return harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED))),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())));
    }
    /**
     * Delivers one open Review Attempt through the Apply Profile executor so
     * the graph seam can be probed with a valid-but-unrouted Attempt purpose.
     */

    private ApplyDeliveryResult reviewDelivery(Harness harness) {
        ApplyProfileExecutor executor = new ApplyProfileExecutor(ReferenceBundles.stack(),
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson(
                        ApplyScriptData.REVIEW_TASK_TEXT, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())), harness.artifacts());
        return executor.deliver(ReviewApplyFixture.reviewContext());
    }
    /**
     * Runs the Diagnostic fail through its fresh verified Practice boundary,
     * the deterministic remediation entry of the Learning StateGraph.
     */

    private static ApplyFlowResult.Boundary reachPracticeBoundary(Harness harness) {
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        return (ApplyFlowResult.Boundary) harness.useCase().continueRequested(
                explained.interaction().flowId(), explained.interaction().interactionVersion(), UUID.randomUUID());
    }

    private static String practiceTaskJson() {
        return ApplyScriptData.taskReadyJson(
                ApplyScriptData.PRACTICE_TASK_TEXT, ApplyScriptData.PRACTICE_EXPECTED_EXPRESSION);
    }

    private static String secondPracticeTaskJson() {
        return ApplyScriptData.taskReadyJson(
                ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, ApplyScriptData.SECOND_PRACTICE_EXPECTED_EXPRESSION);
    }

    private static String independentTaskJson() {
        return ApplyScriptData.taskReadyJson(
                ApplyScriptData.INDEPENDENT_TASK_TEXT, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION);
    }

    private static ResponseAssessment diagnosticFailJudgment() {
        return ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_APPLICABLE);
    }

    private static ResponseAssessment conclusivePracticeJudgment() {
        return ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED);
    }

    private static ResponseAssessment inconclusiveJudgment() {
        return ApplyScriptData.responseAssessment(FinalExpressionJudgment.INCONCLUSIVE, RationaleJudgment.INCONCLUSIVE);
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment
    ) {
        return harness(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification
    ) {
        return harness(generation, verifier, assessment, verification,
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification,
            ScriptedExplainGenerationModel explainGeneration
    ) {
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        ReviewTaskScheduler reviewScheduler = new ReviewTaskScheduler(flowStore);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(ReferenceBundles.stack(), generation, verifier, artifacts);
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, artifacts, flowStore, assessment, verification,
                DiagnosticApplyFixture.diagnosticContext(), IndependentApplyFixture.independentContext(), CLOCK);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                artifacts, flowStore, assessment, verification, reviewScheduler, CLOCK);
        PracticeSubmissionFlow practiceFlow = new PracticeSubmissionFlow(
                executor, artifacts, flowStore, assessment, verification,
                PracticeApplyFixture.practiceContext(), IndependentApplyFixture.independentContext(), CLOCK);
        ExplainFlow explainFlow = new ExplainFlow(
                new ExplainProfileExecutor(ReferenceBundles.explainStack(), explainGeneration),
                artifacts, flowStore, ExplainApplyFixture.explainContext());
        LearningStateGraph graph = new LearningStateGraph(
                artifacts, flowStore, diagnosticFlow, independentFlow, practiceFlow, explainFlow, CLOCK);
        LearningFlowCommandUseCase useCase = new LearningFlowCommandUseCase(
                artifacts, flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), CLOCK);
        return new Harness(artifacts, flowStore, generation, useCase, graph, explainGeneration);
    }

    private record Harness(
            ArtifactStore artifacts,
            InMemoryLearningFlowStore flowStore,
            ScriptedApplyGenerationModel generation,
            LearningFlowCommandUseCase useCase,
            LearningStateGraph graph,
            ScriptedExplainGenerationModel explainGeneration
    ) {
        LearningFlowCommandUseCase newUseCase() {
            return new LearningFlowCommandUseCase(
                    artifacts, flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), CLOCK);
        }
    }
}
