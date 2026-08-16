package cn.lunalhx.ai.kilnai.domain.learning.graph;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ExplainScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.HintScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedExplainGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedHintGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedResponseVerificationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.TeachBackScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ExplainApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.PracticeApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.TeachBackApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.HintFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExposureOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionRejectionReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
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
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.TeachingAction;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(ScriptedPedagogyModel.DEFAULT_FEEDBACK, interaction.learnerMessage(),
                "the guarded Explain decision carries the validated plan's feedback");
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
        assertEquals(ScriptedPedagogyModel.DEFAULT_FEEDBACK, practiceInteraction.learnerMessage(),
                "the Explain-completion decision carries the validated plan's feedback");
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
        assertEquals(ScriptedPedagogyModel.DEFAULT_FEEDBACK, interaction.learnerMessage(),
                "the readiness decision carries the validated plan's feedback");
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
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                ScriptedPedagogyModel.scripted(
                        TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE, TeachingAction.APPLY_PRACTICE));
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
        assertEquals(ScriptedPedagogyModel.DEFAULT_FEEDBACK, interaction.learnerMessage(),
                "the Practice-failure decision carries the validated plan's feedback");
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
                        conclusivePracticeJudgment(), conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                ScriptedPedagogyModel.scripted(
                        TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE,
                        TeachingAction.APPLY_PRACTICE, TeachingAction.INDEPENDENT_TEST));
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

    @Test
    void aFirstHintRequestGeneratesTheStableLadderAndRevealsH1KeepingTheAttemptOpen() {
        Harness harness = practiceHarness();
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary h1 = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        ApplyFlowInteraction interaction = h1.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(practiceAttemptId, interaction.attemptId(),
                "H1 must keep the Practice Attempt open for a later formal submission");
        assertEquals(1, harness.hintGeneration().calls().size(),
                "the first request must make exactly one ladder generation call");
        assertEquals(HintScriptData.H1_ORIENT, interaction.hint().learnerContent());
        assertEquals(1, interaction.hint().level());
        assertEquals("orient", interaction.hint().disclosureKind());
        assertNull(interaction.hint().reasoningSteps());
        assertNull(interaction.hint().proposedFinalAnswer());
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status());
        assertTrue(harness.artifacts().findLadder(practiceAttemptId).isPresent(),
                "the validated ladder must be persisted for later deterministic reveal");
        TaskAttempt attempt = harness.artifacts().findAttempt(practiceAttemptId).orElseThrow();
        assertEquals(1, attempt.assistanceTrace().size(),
                "only the actually exposed level may be recorded in the Assistance Trace");
        assertEquals(1, attempt.highestHintLevel());
    }

    @Test
    void repeatedHintRequestsRevealPersistedLevelsWithoutAnotherModelCall() {
        Harness harness = practiceHarness();
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary h1 = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        ApplyFlowResult.Boundary h2 = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                h1.interaction().flowId(), h1.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        assertEquals(5, h2.interaction().interactionVersion());
        assertEquals(2, h2.interaction().hint().level());
        assertEquals("cue", h2.interaction().hint().disclosureKind());
        assertEquals(1, harness.hintGeneration().calls().size(),
                "later requests must reveal the persisted ladder without another model call");
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status());
        assertEquals(2, harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().assistanceTrace().size());
    }

    @Test
    void aHintAssistedPracticeSubmissionRecordsOnlyExposedLevelsInEvidence() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), conclusivePracticeJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary h1 = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practiceAttemptId, false, UUID.randomUUID());
        ApplyFlowResult.Boundary h2 = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                h1.interaction().flowId(), h1.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        ApplyFlowResult.Boundary submitted = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                h2.interaction().flowId(), h2.interaction().interactionVersion(), UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(LearningStage.INDEPENDENT_TEST, submitted.interaction().stage(),
                "an H1-H4-assisted Practice pass still counts toward readiness");
        AcceptedLearningEvidence evidence = harness.flowStore().allEvidence().get(0);
        assertEquals(2, evidence.highestHintLevel(),
                "the Evidence must record the highest actually exposed hint level");
        assertEquals(List.of("H1:orient", "H2:cue"), evidence.assistanceTrace(),
                "only actually exposed levels may appear in the Evidence Assistance Trace");
    }

    @Test
    void anAnswerRequestJumpsToH5ClosingTheAttemptAsSolutionRevealedWithoutAssessmentOrEvidence() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary revealed = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, true, UUID.randomUUID());
        ApplyFlowInteraction interaction = revealed.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(5, interaction.hint().level());
        assertEquals("reveal", interaction.hint().disclosureKind());
        assertEquals(HintScriptData.H5_LEARNER_CONTENT, interaction.hint().learnerContent());
        assertEquals(4, interaction.hint().reasoningSteps().size());
        assertEquals("18*x^2-4", interaction.hint().proposedFinalAnswer());
        assertEquals(ScriptedPedagogyModel.DEFAULT_FEEDBACK, interaction.learnerMessage(),
                "the H5 reveal decision carries the validated plan's feedback");
        assertEquals(AttemptStatus.SOLUTION_REVEALED,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status(),
                "the H5 reveal must close the attempt as Solution Revealed");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "an H5 reveal must never create Evidence");
        assertTrue(harness.artifacts().assessmentsFor(practiceAttemptId).isEmpty(),
                "an H5 reveal must never trigger Assessment");
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, interaction.learnerProjection().taskText(),
                "the anchored Teach-back task follows the reveal");
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(practiceAttemptId,
                harness.flowStore().latestAnchor(interaction.flowId()).orElseThrow().anchorId(),
                "the H5 reveal must be recorded as the eligible Teach-back anchor");
        assertEquals(TeachBackAnchor.TeachBackAnchorKind.H5_SOLUTION_REVEAL,
                harness.flowStore().latestAnchor(interaction.flowId()).orElseThrow().kind());
        assertEquals(1, harness.teachBackGeneration().calls().size(),
                "the Teach-back task must be generated exactly once for the reveal");
        assertEquals(2, harness.generation().calls().size(),
                "no fresh Apply Practice task may follow the reveal while the Teach-back is offered");
        assertEquals(2, harness.artifacts().allPackages().size(),
                "the Teach-back task is a short-text package, not an Apply package");
        assertFalse(interaction.learnerProjection().allowedEvents().contains(ApplyLearnerEvent.HINT_REQUESTED),
                "the Teach-back task never permits a Hint event");
        ApplyFlowResult.HintIgnored later = (ApplyFlowResult.HintIgnored) harness.useCase().requestHint(
                interaction.flowId(), interaction.interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.ALREADY_SUBMITTED, later.reason(),
                "a closed Solution Revealed attempt never takes another hint");
        ApplyFlowResult.HintIgnored teachBackHint = (ApplyFlowResult.HintIgnored) harness.useCase().requestHint(
                interaction.flowId(), interaction.interactionVersion(), interaction.attemptId(), false, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE, teachBackHint.reason(),
                "hints are never legal on an open Teach-back Attempt (ADR-0065)");
        assertEquals(1, harness.hintGeneration().calls().size(),
                "only the practice ladder generation may ever happen, never one for the Teach-back Attempt");
    }

    @Test
    void hintsAreIgnoredForDiagnosticIndependentAndUnknownAttempts() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.HintIgnored diagnostic = (ApplyFlowResult.HintIgnored) harness.useCase().requestHint(
                started.interaction().flowId(), 1, started.interaction().attemptId(), false, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE, diagnostic.reason());
        assertEquals(0, harness.hintGeneration().calls().size(),
                "no model call may ever happen for a wrong-purpose hint request");
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion(), "an ignored hint must not advance the interaction");
        ApplyFlowResult.HintIgnored unknown = (ApplyFlowResult.HintIgnored) harness.useCase().requestHint(
                started.interaction().flowId(), 1, UUID.randomUUID(), false, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND, unknown.reason());
    }

    @Test
    void aSourceGapLadderExposesNothingAndKeepsTheAttemptOpen() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedHintGenerationModel(List.of(HintScriptData.sourceGapJson())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary unavailable = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        ApplyFlowInteraction interaction = unavailable.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(practiceAttemptId, interaction.attemptId(),
                "a failed ladder must leave the open Attempt exactly as it was");
        assertNull(interaction.hint(), "a failed ladder must expose no partial content");
        assertEquals(HintFlow.HINT_UNAVAILABLE_MESSAGE, interaction.learnerMessage());
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status());
        assertTrue(harness.artifacts().findLadder(practiceAttemptId).isEmpty(),
                "a failed generation must leave no persisted ladder");
        assertTrue(harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().assistanceTrace().isEmpty());
    }

    @Test
    void aRepeatedlyInvalidLadderAfterTheAllowedRepairExposesNothing() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedHintGenerationModel(List.of(
                        invalidLadderJson(), invalidLadderJson())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary unavailable = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        assertNull(unavailable.interaction().hint(),
                "a repeated invalid ladder after the one allowed repair must expose no partial content");
        assertEquals(HintFlow.HINT_UNAVAILABLE_MESSAGE, unavailable.interaction().learnerMessage());
        assertEquals(2, harness.hintGeneration().calls().size(),
                "the gate permits one same-plan repair and no more");
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status());
        assertTrue(harness.artifacts().findLadder(practiceAttemptId).isEmpty());
    }

    @Test
    void aReplayedHintCommandReturnsTheOriginalInteractionWithoutRegeneration() {
        Harness harness = practiceHarness();
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID key = UUID.randomUUID();
        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, key);
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, key);
        assertEquals(first.interaction(), replay.interaction(),
                "a replayed key must return the original committed hint interaction");
        assertEquals(1, harness.hintGeneration().calls().size(),
                "a replay must never regenerate the ladder");
        assertEquals(1, harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().assistanceTrace().size());
    }

    @Test
    void aCrashBetweenHintExposureAndBoundaryCommitResumesTheSameExposedLevel() {
        Harness harness = practiceHarness();
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID h1Key = UUID.randomUUID();
        ApplyFlowResult.Boundary h1 = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, h1Key);
        UUID crashKey = UUID.randomUUID();
        HintLadder ladder = harness.artifacts().findLadder(practiceAttemptId).orElseThrow();
        HintExposureOutcome exposed = harness.artifacts().exposeHint(practiceAttemptId, ladder, 2, crashKey);
        assertInstanceOf(HintExposureOutcome.Exposed.class, exposed);
        ApplyFlowResult.Boundary recovered = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                h1.interaction().flowId(), h1.interaction().interactionVersion(), practiceAttemptId, false, crashKey);
        assertEquals(5, recovered.interaction().interactionVersion());
        assertEquals(2, recovered.interaction().hint().level(),
                "the retried command must resume the same exposed level, never the next one");
        assertEquals(List.of(1, 2), harness.artifacts().findAttempt(practiceAttemptId).orElseThrow()
                .assistanceTrace().stream().map(entry -> entry.level().level()).toList(),
                "the resumed exposure must not duplicate a trace entry");
        ApplyFlowResult.Boundary next = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                recovered.interaction().flowId(), recovered.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        assertEquals(3, next.interaction().hint().level(),
                "a later request continues monotonically from the resumed trace");
        assertEquals(1, harness.hintGeneration().calls().size(),
                "the whole sequence must never regenerate the stable ladder");
    }

    @Test
    void aCrashBetweenTheH5RevealAndItsBoundaryCommitResumesTheRevealExactlyOnce() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID crashKey = UUID.randomUUID();
        HintLadder ladder = HintLadder.from(practiceAttemptId,
                (HintGenerationDraft.LadderReady) HintGenerationDraft.parse(HintScriptData.ladderReadyJson()));
        assertInstanceOf(HintExposureOutcome.Exposed.class,
                harness.artifacts().exposeHint(practiceAttemptId, ladder, 5, crashKey));
        assertEquals(AttemptStatus.SOLUTION_REVEALED,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status(),
                "the crashed run already closed the attempt as Solution Revealed");
        ApplyFlowResult.Boundary recovered = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, true, crashKey);
        assertEquals(4, recovered.interaction().interactionVersion());
        assertEquals(5, recovered.interaction().hint().level(),
                "the retried answer request must resume the same H5 reveal, not a fresh generation");
        assertEquals("18*x^2-4", recovered.interaction().hint().proposedFinalAnswer());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the resumed H5 reveal must never create Evidence");
        assertTrue(harness.artifacts().assessmentsFor(practiceAttemptId).isEmpty());
        assertEquals(0, harness.hintGeneration().calls().size(),
                "a resumed reveal must never call the model again");
    }

    @Test
    void aTeachBackPassAcceptsUnderstandingEvidenceAndDeliversAFreshPracticeTaskNotAnIndependentTest() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        ApplyFlowResult.Boundary followUp = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBackAttemptId,
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        ApplyFlowInteraction interaction = followUp.interaction();
        assertEquals(5, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage(),
                "a Teach-back pass alone must never reopen fresh Independent testing");
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose(),
                "the follow-up is a fresh Apply Practice task, never an Independent Test");
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, interaction.learnerProjection().taskText());
        assertEquals(ScriptedPedagogyModel.DEFAULT_FEEDBACK, interaction.learnerMessage(),
                "the Teach-back pass decision carries the validated plan's feedback");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the Teach-back pass must accept exactly one understanding Evidence record");
        AcceptedLearningEvidence evidence = harness.flowStore().allEvidence().get(0);
        assertEquals(teachBackAttemptId, evidence.taskAttemptId());
        assertEquals(LearningResult.PASS, evidence.result());
        assertEquals(AttemptPurpose.PRACTICE, evidence.attemptPurpose());
        assertEquals(0, evidence.highestHintLevel(),
                "Teach-back evidence carries no assistance because no hint is ever exposed");
        assertTrue(evidence.assistanceTrace().isEmpty());
        assertEquals(3, harness.generation().calls().size(),
                "the follow-up must be a fresh Apply Practice generation, never an Independent one");
        assertEquals(1, harness.teachBackAssessment().contexts().size());
        assertEquals(TeachBackScriptData.LEARNER_PROMPT,
                harness.teachBackAssessment().contexts().get(0).taskText());
        assertTrue(harness.teachBackAssessment().contexts().get(0).anchorContent()
                .contains("18*x^2-4"), "the assessor sees the already exposed H5 anchor content");
        assertEquals(TeachBackScriptData.PASS_EXPLANATION,
                harness.teachBackAssessment().contexts().get(0).learnerResponse());
        assertEquals(1, harness.artifacts().teachBackAssessmentsFor(teachBackAttemptId).size(),
                "the isolated Teach-back Assessment must be recorded for audit");
        assertEquals(AttemptStatus.SUBMITTED,
                harness.artifacts().findAttempt(teachBackAttemptId).orElseThrow().status(),
                "one formal submission closes the Teach-back Attempt");
    }

    @Test
    void aTeachBackFailAcceptsFailEvidenceAndDeliversAFreshPracticeTask() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.failAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                ScriptedPedagogyModel.scripted(
                        TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE,
                        TeachingAction.TEACH_BACK, TeachingAction.APPLY_PRACTICE));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        ApplyFlowResult.Boundary followUp = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBackAttemptId,
                "用了乘积法则，但没解释为什么。", "用了乘积法则，但没解释为什么。", null);
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, followUp.interaction().stage());
        assertEquals(AttemptPurpose.PRACTICE, followUp.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, followUp.interaction().learnerProjection().taskText());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the Teach-back fail must accept exactly one understanding FAIL Evidence record");
        assertEquals(LearningResult.FAIL, harness.flowStore().allEvidence().get(0).result());
        assertEquals(teachBackAttemptId, harness.flowStore().allEvidence().get(0).taskAttemptId());
    }

    @Test
    void anInconclusiveTeachBackCreatesNoEvidenceAndDeliversAFreshTeachBackReplacement() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(
                        TeachBackScriptData.taskReadyJson(), TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.inconclusiveAssessment())));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        ApplyFlowResult.Boundary replacement = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBackAttemptId,
                "说不清为什么幂法则适用。", "说不清为什么幂法则适用。", null);
        ApplyFlowInteraction interaction = replacement.interaction();
        assertEquals(5, interaction.interactionVersion());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, interaction.learnerProjection().taskText(),
                "an Inconclusive judgment must deliver a fresh Teach-back task");
        assertEquals(TeachBackFlow.TEACH_BACK_REPLACEMENT_MESSAGE, interaction.learnerMessage());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "an Inconclusive Teach-back judgment must never create Evidence");
        assertEquals(2, harness.teachBackGeneration().calls().size(),
                "the replacement requires one fresh Teach-back generation");
        assertEquals(2, harness.artifacts().allPackages().size(),
                "the replacement is a Teach-back package, not an Apply package");
        assertEquals(AttemptStatus.SUBMITTED,
                harness.artifacts().findAttempt(teachBackAttemptId).orElseThrow().status());
    }

    @Test
    void aReplayedTeachBackSubmissionReturnsTheOriginalInteractionWithoutASecondAssessment() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID key = UUID.randomUUID();
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                key, teachBackAttemptId,
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                key, teachBackAttemptId,
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        assertEquals(first.interaction(), replay.interaction(), "a replayed key must return the original result");
        assertEquals(1, harness.teachBackAssessment().contexts().size(),
                "a replay must never run a second Teach-back Assessment");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replay must never accept a second Evidence");
        assertEquals(1, harness.artifacts().teachBackAssessmentsFor(teachBackAttemptId).size());
    }

    @Test
    void aCrashBetweenClosingAndCommittingATeachBackSubmissionResumesExactlyOnce() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        harness.artifacts().closeAttempt(teachBackAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(TeachBackScriptData.PASS_EXPLANATION,
                                TeachBackScriptData.PASS_EXPLANATION, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the crash must leave the closed Attempt without committed Evidence");
        UUID retryKey = UUID.randomUUID();
        ApplyFlowResult.Boundary recovered = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                retryKey, teachBackAttemptId,
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        assertEquals(5, recovered.interaction().interactionVersion());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, recovered.interaction().stage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must resume the assessment of the saved submission exactly once");
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                retryKey, teachBackAttemptId,
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        assertEquals(recovered.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size());
    }

    @Test
    void theGuardOffersTeachBackOnlyWithAnEligibleAnchorAndNeverCallsTheModelWithoutOne() {
        Harness harness = practiceHarness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        assertEquals(0, harness.flowStore().latestAnchor(started.interaction().flowId()).map(anchor -> 1).orElse(0),
                "a freshly started Flow carries no eligible anchor yet");
        TeachBackDeliveryResult.Unavailable guarded =
                (TeachBackDeliveryResult.Unavailable) harness.teachBackFlow().deliverTeachBack(started.interaction().flowId());
        assertEquals(TeachBackUnavailableReason.NO_ELIGIBLE_ANCHOR, guarded.reason(),
                "without an eligible anchor the Guard must not offer Teach-back");
        assertEquals(0, harness.teachBackGeneration().calls().size(),
                "no model call may ever happen without an eligible anchor");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "no Teach-back package may open without an eligible anchor");

        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        TeachBackAnchor anchor = harness.flowStore().latestAnchor(explained.interaction().flowId()).orElseThrow();
        assertEquals(TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE, anchor.kind(),
                "the Explain worked example must be recorded as the eligible Teach-back anchor");
        assertEquals(0, harness.teachBackGeneration().calls().size(),
                "no Teach-back task may be generated by the Explain boundary itself");
    }

    @Test
    void theFullRemediationLoopReopensIndependentTestingOnlyAfterAnApplyPracticePass() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson(),
                        independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(),
                        conclusivePracticeJudgment(), conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        ApplyFlowResult.Boundary afterTeachBackPass = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBack.interaction().attemptId(),
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, afterTeachBackPass.interaction().stage(),
                "a Teach-back pass must not reopen Independent testing by itself");
        assertEquals(1, harness.flowStore().allEvidence().size());
        ApplyFlowResult.Boundary practicePass = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                afterTeachBackPass.interaction().flowId(), afterTeachBackPass.interaction().interactionVersion(),
                UUID.randomUUID(), afterTeachBackPass.interaction().attemptId(),
                ApplyScriptData.SECOND_PRACTICE_CORRECT_DERIVATIVE,
                ApplyScriptData.SECOND_PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(LearningStage.INDEPENDENT_TEST, practicePass.interaction().stage(),
                "only the current cycle's conclusive Apply Practice PASS can reopen Independent testing");
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, practicePass.interaction().attemptPurpose());
        assertEquals(2, harness.flowStore().allEvidence().size(),
                "the understanding Evidence and the assisted Practice Evidence are both accepted");
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practicePass.interaction().flowId(), practicePass.interaction().interactionVersion(),
                UUID.randomUUID(), practicePass.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(3, harness.flowStore().allEvidence().size());
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "the post-remediation Independent pass rejoins the established Review 1 cadence");
    }

    @Test
    void thePedagogyAgentSelectsTheNextActionAmongLegalCandidates() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of(ScriptedPedagogyModel.planJson(
                        TeachingAction.APPLY_PRACTICE, "好的，请完成一道练习题。"))));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary practice = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, practice.interaction().stage(),
                "the agent's selected Apply Practice is the legal next move after a Diagnostic failure");
        assertEquals(AttemptPurpose.PRACTICE, practice.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.PRACTICE_TASK_TEXT, practice.interaction().learnerProjection().taskText());
        assertEquals("好的，请完成一道练习题。", practice.interaction().learnerMessage(),
                "the learner message is the validated plan's feedback summary");
        assertEquals(0, harness.explainGeneration().calls().size(),
                "the agent's Practice choice must not deliver the Explain node");
        assertEquals(2, harness.generation().calls().size(),
                "the agent's Practice choice delivers the fresh verified Practice task");
        assertEquals(1, harness.pedagogy().calls().size());
    }

    @Test
    void thePedagogyAgentReceivesOnlySanitizedFactsAndTheClosedLegalSet() {
        Harness harness = practiceHarness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        String contextJson = harness.pedagogy().calls().get(0).contextJson();
        assertTrue(contextJson.contains("\"legal_actions\""), "the context must carry the closed legal-action set");
        assertTrue(contextJson.contains("\"explain\""), "Explain must be offered after a Diagnostic failure");
        assertTrue(contextJson.contains("\"apply_practice\""), "fresh Apply Practice must be offered after a Diagnostic failure");
        assertTrue(contextJson.contains("differentiate-polynomial"),
                "the sanitized missing rubric criterion is a Feedback Fact");
        assertFalse(contextJson.contains(ApplyScriptData.WRONG_DERIVATIVE),
                "the raw learner answer must never reach the Pedagogy Agent");
        assertFalse(contextJson.contains("我猜的"), "the raw rationale must never reach the Pedagogy Agent");
        assertFalse(contextJson.contains(ApplyScriptData.EXPECTED_EXPRESSION),
                "the private expected answer must never reach the Pedagogy Agent");
        assertFalse(contextJson.contains("apply.task-first"), "Skill ids must never reach the Pedagogy Agent");
        assertFalse(contextJson.contains("openstax"), "source identities must never reach the Pedagogy Agent");
        assertTrue(harness.pedagogy().calls().get(0).systemPrompt().length() <= 16_000,
                "the compiled pedagogy prompt stays within the instruction cap");
    }

    @Test
    void anInvalidPlanTwiceAfterADiagnosticFailureFallsBackToExplain() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of("{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(2, harness.pedagogy().calls().size(),
                "one initial plan and at most one same-plan repair, then the fallback");
        assertNotNull(explained.interaction().teachingProjection(),
                "the deterministic fallback after a Diagnostic failure is Explain");
        assertEquals(ExplainFlow.EXPLAIN_START_MESSAGE, explained.interaction().learnerMessage(),
                "the invalid output is discarded and neutral deterministic feedback is shown");
        assertEquals(1, harness.explainGeneration().calls().size(),
                "the fallback Explain runs exactly one generation");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "Explain must never create a Task Package");
    }

    @Test
    void aWellFormedButIllegalActionPlanIsDiscardedAndTheDeterministicFallbackRuns() {
        String illegalFeedback = "现在直接进入独立测试。";
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of(
                        ScriptedPedagogyModel.planJson(TeachingAction.INDEPENDENT_TEST, illegalFeedback),
                        ScriptedPedagogyModel.planJson(TeachingAction.INDEPENDENT_TEST, illegalFeedback))));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(2, harness.pedagogy().calls().size(),
                "a well-formed plan outside the legal set is as invalid as malformed output");
        assertNotNull(explained.interaction().teachingProjection(),
                "the illegal Independent Test plan must never be routed before readiness");
        assertEquals(ExplainFlow.EXPLAIN_START_MESSAGE, explained.interaction().learnerMessage());
        assertFalse(explained.interaction().learnerMessage().contains(illegalFeedback),
                "the discarded plan's feedback must never reach the learner");
        assertFalse(explained.interaction().learnerProjection() != null
                        && explained.interaction().learnerProjection().taskText() != null,
                "no Independent task may be delivered by the discarded plan");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "the discarded plan must leave no new Task Package");
    }

    @Test
    void anInvalidPlanTwiceAfterExplainCompletionFallsBackToFreshPractice() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of(
                        ScriptedPedagogyModel.planJson(TeachingAction.EXPLAIN, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        "{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        ApplyFlowResult.Boundary practice = (ApplyFlowResult.Boundary) harness.useCase().continueRequested(
                explained.interaction().flowId(), 2, UUID.randomUUID());
        assertEquals(3, harness.pedagogy().calls().size(),
                "one valid Diagnostic plan plus one plan and one repair for the Explain completion");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, practice.interaction().stage());
        assertEquals(AttemptPurpose.PRACTICE, practice.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.PRACTICE_TASK_TEXT, practice.interaction().learnerProjection().taskText());
        assertEquals(PracticeSubmissionFlow.PRACTICE_START_MESSAGE, practice.interaction().learnerMessage(),
                "the Explain-completion fallback is the fresh Apply Practice task with neutral feedback");
        assertEquals(2, harness.generation().calls().size());
    }

    @Test
    void anInvalidPlanTwiceAfterAnH5RevealFallsBackToTeachBack() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of(
                        ScriptedPedagogyModel.planJson(TeachingAction.EXPLAIN, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        ScriptedPedagogyModel.planJson(TeachingAction.APPLY_PRACTICE, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        "{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        assertEquals(4, harness.pedagogy().calls().size(),
                "the H5 reveal decision runs one plan and one repair before the fallback");
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, teachBack.interaction().learnerProjection().taskText(),
                "the deterministic H5 fallback is the anchored Teach-back task");
        assertEquals(TeachBackFlow.TEACH_BACK_AFTER_REVEAL_MESSAGE, teachBack.interaction().learnerMessage());
        TeachBackAnchor anchor = harness.flowStore().latestAnchor(teachBack.interaction().flowId()).orElseThrow();
        assertEquals(AttemptStatus.SOLUTION_REVEALED,
                harness.artifacts().findAttempt(anchor.anchorId()).orElseThrow().status(),
                "the reveal already closed the attempt before the decision");
        assertEquals(1, harness.teachBackGeneration().calls().size());
    }

    @Test
    void anInvalidPlanTwiceAfterAPracticePassFallsBackToAFreshIndependentTest() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(),
                        conclusivePracticeJudgment(), conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of(
                        ScriptedPedagogyModel.planJson(TeachingAction.EXPLAIN, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        ScriptedPedagogyModel.planJson(TeachingAction.APPLY_PRACTICE, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        "{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        ApplyFlowResult.Boundary independent = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practice.interaction().attemptId(),
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(4, harness.pedagogy().calls().size());
        assertEquals(LearningStage.INDEPENDENT_TEST, independent.interaction().stage(),
                "the qualifying Practice pass falls back to the fresh Independent Test");
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, independent.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.INDEPENDENT_TASK_TEXT, independent.interaction().learnerProjection().taskText());
        assertEquals(PracticeSubmissionFlow.INDEPENDENT_READY_MESSAGE, independent.interaction().learnerMessage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the fallback route still accepts the assisted Practice pass Evidence exactly once");
        assertEquals(LearningResult.PASS, harness.flowStore().allEvidence().get(0).result());
    }

    @Test
    void anInvalidPlanTwiceAfterAPracticeFailFallsBackToExplain() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson(),
                        secondExplainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of(
                        ScriptedPedagogyModel.planJson(TeachingAction.EXPLAIN, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        ScriptedPedagogyModel.planJson(TeachingAction.APPLY_PRACTICE, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        "{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practice.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(4, harness.pedagogy().calls().size());
        assertNotNull(explained.interaction().teachingProjection(),
                "the Practice-failure fallback is Explain");
        assertEquals(ExplainFlow.EXPLAIN_START_MESSAGE, explained.interaction().learnerMessage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the fallback route still accepts the assisted Practice fail Evidence exactly once");
        assertEquals(LearningResult.FAIL, harness.flowStore().allEvidence().get(0).result());
        assertEquals(2, harness.explainGeneration().calls().size(),
                "the Explain fallback runs its own generation after the remediation Explain");
        assertEquals(2, harness.flowStore().exposedExampleFingerprints(explained.interaction().flowId()).size(),
                "the repeated Explain delivers a novel worked example that is also exposed");
    }

    @Test
    void anInvalidPlanTwiceAfterATeachBackPassFallsBackToFreshPractice() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of(
                        ScriptedPedagogyModel.planJson(TeachingAction.EXPLAIN, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        ScriptedPedagogyModel.planJson(TeachingAction.APPLY_PRACTICE, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        ScriptedPedagogyModel.planJson(TeachingAction.TEACH_BACK, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        "{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        ApplyFlowResult.Boundary followUp = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBack.interaction().attemptId(),
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        assertEquals(5, harness.pedagogy().calls().size());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, followUp.interaction().stage(),
                "a Teach-back pass without a qualifying Practice pass falls back to fresh Apply Practice");
        assertEquals(AttemptPurpose.PRACTICE, followUp.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, followUp.interaction().learnerProjection().taskText());
        assertEquals(TeachBackFlow.TEACH_BACK_FOLLOW_UP_MESSAGE, followUp.interaction().learnerMessage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the fallback route still accepts the understanding Evidence exactly once");
        assertEquals(LearningResult.PASS, harness.flowStore().allEvidence().get(0).result());
        assertEquals(3, harness.generation().calls().size(),
                "the follow-up is a fresh Apply Practice generation, never an Independent one");
    }

    @Test
    void anInvalidPlanTwiceAfterATeachBackFailFallsBackToExplain() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson(),
                        secondExplainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.failAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of(
                        ScriptedPedagogyModel.planJson(TeachingAction.EXPLAIN, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        ScriptedPedagogyModel.planJson(TeachingAction.APPLY_PRACTICE, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        ScriptedPedagogyModel.planJson(TeachingAction.TEACH_BACK, ScriptedPedagogyModel.DEFAULT_FEEDBACK),
                        "{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBack.interaction().attemptId(),
                "用了乘积法则，但没解释为什么。", "用了乘积法则，但没解释为什么。", null);
        assertEquals(5, harness.pedagogy().calls().size());
        assertNotNull(explained.interaction().teachingProjection(),
                "the Teach-back-failure fallback is Explain");
        assertEquals(ExplainFlow.EXPLAIN_START_MESSAGE, explained.interaction().learnerMessage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the fallback route still accepts the understanding FAIL Evidence exactly once");
        assertEquals(LearningResult.FAIL, harness.flowStore().allEvidence().get(0).result());
        assertEquals(2, harness.explainGeneration().calls().size());
        assertEquals(2, harness.flowStore().exposedExampleFingerprints(explained.interaction().flowId()).size(),
                "the repeated Explain delivers a novel worked example that is also exposed");
    }

    @Test
    void anUnavailableFallbackProjectsASafeTerminalBoundaryWithoutPartialOutput() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainSourceGapJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(List.of("{not valid json", "{not valid json")));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary unavailable = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(FlowStatus.TERMINAL, unavailable.interaction().status(),
                "an unavailable fallback node projects a real safe boundary, never fabricated content");
        assertEquals(ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.interaction().learnerMessage());
        assertNull(unavailable.interaction().teachingProjection());
        assertEquals(2, harness.pedagogy().calls().size());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "no new Task Package may appear from a failed fallback");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "no Evidence may be accepted by a failed transition");
        assertEquals(0, harness.flowStore().exposedExampleFingerprints(started.interaction().flowId()).size(),
                "no teaching artifact may be exposed by a failed fallback");
    }

    @Test
    void aSingleCandidateInconclusiveDecisionBypassesThePedagogyModel() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(), inconclusiveJudgment())),
                new ScriptedResponseVerificationModel(List.of(inconclusiveJudgment())));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        assertEquals(2, harness.pedagogy().calls().size(),
                "the Diagnostic failure and Explain completion decisions both had several legal moves");
        ApplyFlowResult.Boundary replacement = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                UUID.randomUUID(), practice.interaction().attemptId(),
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        assertEquals(2, harness.pedagogy().calls().size(),
                "an Inconclusive Practice judgment has exactly one legal move and must never invoke the model");
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, replacement.interaction().learnerProjection().taskText(),
                "the mandated fresh Practice replacement is the single legal move");
        assertEquals(PracticeSubmissionFlow.PRACTICE_REPLACEMENT_MESSAGE, replacement.interaction().learnerMessage());
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void anInconclusiveTeachBackBypassesThePedagogyModel() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(
                        TeachBackScriptData.taskReadyJson(), TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.inconclusiveAssessment())));
        ApplyFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        assertEquals(3, harness.pedagogy().calls().size(),
                "the three earlier decisions all had several legal moves");
        ApplyFlowResult.Boundary replacement = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBack.interaction().attemptId(),
                "说不清为什么幂法则适用。", "说不清为什么幂法则适用。", null);
        assertEquals(3, harness.pedagogy().calls().size(),
                "an Inconclusive Teach-back judgment has exactly one legal move and must never invoke the model");
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, replacement.interaction().learnerProjection().taskText(),
                "the mandated fresh Teach-back replacement is the single legal move");
        assertEquals(TeachBackFlow.TEACH_BACK_REPLACEMENT_MESSAGE, replacement.interaction().learnerMessage());
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void afterReadinessTheAgentMayChooseMoreLearningOverTheFreshIndependentTest() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(),
                        conclusivePracticeJudgment(), conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                ScriptedPedagogyModel.scripted(
                        TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE, TeachingAction.APPLY_PRACTICE));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        ApplyFlowResult.Boundary moreLearning = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practice.interaction().attemptId(),
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, moreLearning.interaction().stage(),
                "even after readiness the agent may recommend more learning over the fresh Independent Test");
        assertEquals(AttemptPurpose.PRACTICE, moreLearning.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, moreLearning.interaction().learnerProjection().taskText());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the readiness pass Evidence is still accepted exactly once");
        assertEquals(LearningResult.PASS, harness.flowStore().allEvidence().get(0).result());
        assertEquals(3, harness.generation().calls().size(),
                "the chosen move is a fresh Practice generation, never an Independent one");
        assertEquals(0, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "no Review is scheduled because the fresh Independent Test was not delivered");
    }

    @Test
    void theAgentMayChooseFreshPracticeAfterAnH5Reveal() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                ScriptedPedagogyModel.scripted(
                        TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE, TeachingAction.APPLY_PRACTICE));
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID revealedAttemptId = practice.interaction().attemptId();
        ApplyFlowResult.Boundary freshPractice = (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                revealedAttemptId, true, UUID.randomUUID());
        assertEquals(5, freshPractice.interaction().hint().level(),
                "the H5 reveal content is still shown on the chosen continuation boundary");
        assertEquals(AttemptPurpose.PRACTICE, freshPractice.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT,
                freshPractice.interaction().learnerProjection().taskText(),
                "the agent's choice after the reveal is a fresh verified Practice task");
        assertEquals(0, harness.teachBackGeneration().calls().size(),
                "the agent's Practice choice must never deliver the Teach-back node");
        assertEquals(AttemptStatus.SOLUTION_REVEALED,
                harness.artifacts().findAttempt(revealedAttemptId).orElseThrow().status(),
                "the revealed Attempt stays closed as Solution Revealed, never assessed");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
        assertTrue(harness.artifacts().assessmentsFor(revealedAttemptId).isEmpty());
    }

    @Test
    void aTemporaryExplainInsideAnOpenPracticeAttemptReturnsToTheSamePracticeInteraction() {
        Harness harness = practiceHarness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary explained = (ApplyFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertNotNull(explained.interaction().teachingProjection(),
                "the learner stands at the Explain teaching boundary");
        // A temporary Explain shown inside an open Apply Practice Attempt: the
        // committed artifact store carries the open Attempt, exposed in this
        // Flow's ledger exactly like any delivered task.
        ApplyProfileExecutor executor = new ApplyProfileExecutor(ReferenceBundles.stack(),
                new ScriptedApplyGenerationModel(List.of(practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())), harness.artifacts());
        ApplyDeliveryResult injected = executor.deliver(PracticeApplyFixture.practiceContext());
        assertInstanceOf(ApplyDeliveryResult.Delivered.class, injected);
        UUID openAttemptId = ((ApplyDeliveryResult.Delivered) injected).attempt().attemptId();
        TaskPackage openPackage = harness.artifacts()
                .findPackage(((ApplyDeliveryResult.Delivered) injected).attempt().taskPackageId()).orElseThrow();
        harness.flowStore().recordTaskExposure(explained.interaction().flowId(), openPackage);
        assertEquals(1, harness.pedagogy().calls().size(),
                "the Explain decision was the only model-driven decision so far");
        ApplyFlowResult.Boundary resumed = (ApplyFlowResult.Boundary) harness.useCase().continueRequested(
                explained.interaction().flowId(), 2, UUID.randomUUID());
        assertEquals(3, resumed.interaction().interactionVersion());
        assertEquals(openAttemptId, resumed.interaction().attemptId(),
                "the single legal move returns to the SAME open Practice interaction");
        assertEquals(AttemptPurpose.PRACTICE, resumed.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.PRACTICE_TASK_TEXT, resumed.interaction().learnerProjection().taskText());
        assertEquals(LearningStateGraph.RESUME_PRACTICE_MESSAGE, resumed.interaction().learnerMessage());
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(openAttemptId).orElseThrow().status(),
                "the open Attempt is not replaced or closed by the resume");
        assertEquals(1, harness.pedagogy().calls().size(),
                "the single-move resume must never invoke the Pedagogy Agent");
        assertEquals(1, harness.generation().calls().size(),
                "the resume must never generate a fresh task");
    }

    private static String invalidLadderJson() {
        return HintScriptData.ladderReadyJson(
                HintScriptData.H4_SCAFFOLD, HintScriptData.H5_LEARNER_CONTENT,
                HintScriptData.H5_STEPS, "6*x^2-4");
    }

    /**
     * A second materially different complete worked example for the novelty
     * exclusions of a repeated Explain delivery in one Flow.
     */
    private static String secondExplainReadyJson() {
        return ExplainScriptData.explainReadyJson(
                ExplainScriptData.PRINCIPLE_SUMMARY,
                "设 f(x) = 4x² + 3x − 5，求 f'(x)。",
                "8x + 3");
    }

    /**
     * A harness whose scripted Apply generation covers the Diagnostic and one
     * fresh Practice task, with the diagnostic judged as a conclusive failure
     * so the remediation cycle opens the Practice boundary.
     */
    private Harness practiceHarness() {
        return harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
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

    /**
     * Runs the Diagnostic fail through the Explain teaching boundary, the
     * fresh Practice boundary, and an H5 answer reveal, so the learner stands
     * at the anchored Teach-back task boundary.
     */
    private static ApplyFlowResult.Boundary reachTeachBackBoundary(Harness harness) {
        ApplyFlowResult.Boundary practice = reachPracticeBoundary(harness);
        return (ApplyFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practice.interaction().attemptId(), true, UUID.randomUUID());
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

    private static TaskVerificationVerdict passVerdict() {
        return new TaskVerificationVerdict(
                TaskVerificationVerdict.SCHEMA,
                TaskVerificationVerdict.Verdict.PASS,
                Map.of(
                        "answer_clarity", TaskVerificationVerdict.CheckResult.PASS,
                        "rubric_alignment", TaskVerificationVerdict.CheckResult.PASS,
                        "source_grounding", TaskVerificationVerdict.CheckResult.PASS,
                        "anchor_grounding", TaskVerificationVerdict.CheckResult.PASS,
                        "learner_boundary", TaskVerificationVerdict.CheckResult.PASS),
                List.of());
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
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification,
            ScriptedExplainGenerationModel explainGeneration
    ) {
        return harness(generation, verifier, assessment, verification, explainGeneration,
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification,
            ScriptedHintGenerationModel hintGeneration
    ) {
        return harness(generation, verifier, assessment, verification,
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                hintGeneration);
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedHintGenerationModel hintGeneration
    ) {
        return harness(generation, verifier, assessment, verification, explainGeneration, hintGeneration,
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedHintGenerationModel hintGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment
    ) {
        return harness(generation, verifier, assessment, verification, explainGeneration, hintGeneration,
                teachBackGeneration, teachBackAssessment,
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedHintGenerationModel hintGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment,
            ScriptedTeachBackTaskVerifier teachBackVerifier
    ) {
        return harness(generation, verifier, assessment, verification, explainGeneration, hintGeneration,
                teachBackGeneration, teachBackAssessment, teachBackVerifier, new ScriptedPedagogyModel());
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedHintGenerationModel hintGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment,
            ScriptedTeachBackTaskVerifier teachBackVerifier,
            ScriptedPedagogyModel pedagogy
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
        HintFlow hintFlow = new HintFlow(
                hintGeneration, artifacts, PracticeApplyFixture.practiceContext().conceptSourcePack());
        TeachBackFlow teachBackFlow = new TeachBackFlow(
                new TeachBackProfileExecutor(
                        ReferenceBundles.teachBackStack(), teachBackGeneration, teachBackVerifier, artifacts),
                artifacts, flowStore, teachBackAssessment,
                TeachBackApplyFixture.teachBackContext(), CLOCK);
        LearningStateGraph graph = new LearningStateGraph(
                artifacts, flowStore, diagnosticFlow, independentFlow, practiceFlow,
                explainFlow, hintFlow, teachBackFlow, pedagogy, CLOCK);
        LearningFlowCommandUseCase useCase = new LearningFlowCommandUseCase(
                artifacts, flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), CLOCK);
        return new Harness(artifacts, flowStore, generation, hintGeneration, useCase, graph,
                explainGeneration, teachBackGeneration, teachBackAssessment, teachBackFlow, pedagogy);
    }

    private record Harness(
            ArtifactStore artifacts,
            InMemoryLearningFlowStore flowStore,
            ScriptedApplyGenerationModel generation,
            ScriptedHintGenerationModel hintGeneration,
            LearningFlowCommandUseCase useCase,
            LearningStateGraph graph,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment,
            TeachBackFlow teachBackFlow,
            ScriptedPedagogyModel pedagogy
    ) {
        LearningFlowCommandUseCase newUseCase() {
            return new LearningFlowCommandUseCase(
                    artifacts, flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), CLOCK);
        }
    }
}
