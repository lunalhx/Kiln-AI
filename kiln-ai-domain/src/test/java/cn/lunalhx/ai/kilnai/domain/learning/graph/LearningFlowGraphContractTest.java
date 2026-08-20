package cn.lunalhx.ai.kilnai.domain.learning.graph;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ExplainScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.HintScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedClarificationClassifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedModelProfile;
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
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.CommittedEvaluationResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.PendingOperation;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceTraceEntry;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExposureOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.InteractionKind;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractAudit;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionRejectionReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
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
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCancellationResult;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCancellationUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ActiveWorkConflictException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
        LearningFlowInteraction interaction = started.interaction();
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
        LearningCheckpoint checkpoint = harness.flowStore().latestCheckpoint(interaction.flowId()).orElseThrow();
        assertEquals(1, checkpoint.interactionVersion(), "the first boundary must commit a checkpoint");
        assertEquals(interaction, harness.flowStore().findCommand(key).orElseThrow().response(),
                "the start command must be persisted with its original result");
    }

    @Test
    void aReplayedStartReturnsTheOriginalInteractionWithoutASecondAttempt() {
        Harness harness = harness();
        UUID key = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();
        UUID diagnosticAttemptId = started.interaction().attemptId();
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, submitKey, diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowInteraction interaction = transitioned.interaction();
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        UUID submitKey = UUID.randomUUID();
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, submitKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        LearningFlowInteraction interaction = completed.interaction();
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        UUID submitKey = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, submitKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, submitKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(first.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replayed key after completion must never accept a second Evidence");
        assertEquals(1, responseAssessmentsFor(harness, independentAttemptId).size(),
                "the isolated assessment record must be persisted exactly once");
    }

    @Test
    void anIndependentAssessmentIsCommittedAsOneVersionedEvaluationResult() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(
                LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();

        harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);

        List<CommittedEvaluationResult> results =
                harness.artifacts().committedEvaluationResultsFor(independentAttemptId);
        assertEquals(1, results.size());
        CommittedEvaluationResult result = results.get(0);
        assertEquals(CommittedEvaluationResult.RESPONSE_ASSESSMENT, result.responsibility());
        assertEquals(CommittedEvaluationResult.EVALUATION_VERSION, result.evaluationVersion());
        assertEquals(ResponseAssessment.SCHEMA, result.resultSchema());
        assertEquals(ApplyScriptData.responseAssessment(
                FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED),
                ResponseAssessment.parse(result.resultPayload()));
    }

    @Test
    void aCommittedOutcomeResubmittedWithANewKeyIsIgnoredWithoutASecondEvaluationOrReview() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        harness.useCase().submitAnswer(started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        LearningFlowResult.SubmissionIgnored duplicate = (LearningFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 3, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION, duplicate.reason(),
                "an Attempt replaced by a later Interaction cannot be routed again");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "an already-produced outcome with a new key must never accept a second Evidence");
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "an already-produced outcome must never stack a second Review");
        assertEquals(1, responseAssessmentsFor(harness, independentAttemptId).size(),
                "an already-produced outcome must never run a second evaluation");
    }

    @Test
    void anAttemptThatIsNotAddressedByTheCurrentInteractionCannotBeRouted() {
        Harness harness = harness();
        ApplyDeliveryResult reviewDelivery = reviewDelivery(harness);
        assertInstanceOf(ApplyDeliveryResult.Delivered.class, reviewDelivery);
        UUID reviewAttemptId = ((ApplyDeliveryResult.Delivered) reviewDelivery).attempt().attemptId();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.SubmissionIgnored ignored = (LearningFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), reviewAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION, ignored.reason(),
                "an Attempt must belong to the current Flow and be addressed by the current Interaction");
        assertEquals(InteractionKind.TASK,
                harness.flowStore().latestInteraction(started.interaction().flowId()).orElseThrow().kind());
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "an unrouted Attempt must never create Evidence");
        ConceptProgress progress =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.UNASSESSED, progress.currentMilestone(),
                "an unrouted Attempt must not change any Milestone");
    }

    @Test
    void aFreshInstanceResumesFromTheCommittedCheckpointAndReplaysExactlyOnce() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowCommandUseCase fresh = harness.newUseCase();
        assertEquals(transitioned.interaction(), fresh.query(started.interaction().flowId()),
                "a fresh instance must recover the latest interaction and checkpoint exactly");
        UUID submitKey = UUID.randomUUID();
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) fresh.submitAnswer(
                started.interaction().flowId(), 2, submitKey, transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(3, completed.interaction().interactionVersion());
        assertEquals(1, harness.flowStore().allEvidence().size());
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) fresh.submitAnswer(
                started.interaction().flowId(), 2, submitKey, transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(completed.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replayed key after recovery must never accept a second Evidence");
    }

    @Test
    void aCrashBetweenClosingAndCommittingResumesFromTheSavedAttempt() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, retryKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(3, recovered.interaction().interactionVersion());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must resume the evaluation of the saved submission exactly once");
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "the resumed transition must still schedule Review 1");
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, retryKey, independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(recovered.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size());
    }

    @Test
    void aCrashAfterAssessmentCommitResumesWithTheSavedAssessmentAndOnlyRunsMissingVerification() {
        FailsOnceThenPassesVerification verification = new FailsOnceThenPassesVerification();
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED))),
                verification);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(
                LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();

        assertThrows(IllegalStateException.class, () -> harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null));
        assertEquals(1, responseAssessmentsFor(harness, independentAttemptId).size(),
                "the first responsibility must survive a crash before verification");
        assertEquals(1, harness.assessment().contexts().size());

        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        assertEquals(3, recovered.interaction().interactionVersion());
        assertEquals(1, harness.assessment().contexts().size(),
                "a committed Assessment must not be invoked again during recovery");
        assertEquals(2, verification.calls,
                "recovery may invoke only the missing Response Verification");
        assertEquals(2, responseAssessmentsFor(harness, independentAttemptId).size());
        assertEquals(1, harness.flowStore().allEvidence().size());
    }

    @Test
    void aSavedEvaluationCheckpointUsesDeterministicRoutingWithoutReplanning() {
        ScriptedPedagogyModel pedagogy = new ScriptedPedagogyModel(List.of(
                ScriptedPedagogyModel.planJson(TeachingAction.APPLY_PRACTICE, "must not run")));
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of()),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                pedagogy);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(
                LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        harness.artifacts().closeAttempt(diagnosticAttemptId, new TaskSubmission(
                new MathematicalAnswer(ApplyScriptData.WRONG_DERIVATIVE,
                        ApplyScriptData.WRONG_DERIVATIVE, AnswerInputFamily.PLAIN_TEXT),
                "我猜的", CLOCK.instant()));
        ResponseAssessment savedFailure = diagnosticFailJudgment();
        harness.artifacts().saveOrReturnCommittedEvaluationResult(
                diagnosticAttemptId, CommittedEvaluationResult.RESPONSE_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION, savedFailure.schema(),
                ApplyJson.writeContract(savedFailure));
        harness.artifacts().saveOrReturnCommittedEvaluationResult(
                diagnosticAttemptId, CommittedEvaluationResult.RESPONSE_VERIFICATION,
                CommittedEvaluationResult.EVALUATION_VERSION, savedFailure.schema(),
                ApplyJson.writeContract(savedFailure));

        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertEquals(LearningStage.LEARNING_AND_PRACTICE, recovered.interaction().stage());
        assertTrue(recovered.interaction().teachingProjection() != null,
                "the deterministic recovery route must still deliver its committed teaching boundary");
        assertTrue(harness.pedagogy().calls().isEmpty(),
                "a saved evaluation without a committed route must not invoke Pedagogy again");
        assertTrue(harness.assessment().contexts().isEmpty(),
                "a saved Assessment must not be re-evaluated during route recovery");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a failed Diagnostic recovery must not fabricate Evidence");
    }

    @Test
    void aCrashBetweenClosingAndCommittingADiagnosticSubmissionResumesFromTheSavedAttempt() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        harness.artifacts().closeAttempt(diagnosticAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                                ApplyScriptData.UNICODE_CORRECT_CANONICAL, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        assertEquals(AttemptStatus.SUBMITTED,
                harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow().status());
        assertEquals(1, started.interaction().interactionVersion(),
                "the crash must leave the Diagnostic interaction unadvanced");
        UUID retryKey = UUID.randomUUID();
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, retryKey, diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(2, recovered.interaction().interactionVersion());
        assertEquals(LearningStage.INDEPENDENT_TEST, recovered.interaction().stage());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, recovered.interaction().attemptPurpose());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a recovered Diagnostic must still create no Evidence");
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, retryKey, diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(recovered.interaction(), replay.interaction());
        assertEquals(1, harness.artifacts().allPackages().stream()
                .filter(taskPackage -> taskPackage.attemptPurpose() == AttemptPurpose.INDEPENDENT_TEST)
                .count(),
                "a replayed recovery must never deliver a second Independent task");
    }

    @Test
    void aCrashBetweenClosingAndCommittingAFailedDiagnosticKeepsTheAttemptClosedAndResumesFromTheSavedSubmission() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        harness.artifacts().closeAttempt(diagnosticAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.WRONG_DERIVATIVE,
                                ApplyScriptData.WRONG_DERIVATIVE, AnswerInputFamily.PLAIN_TEXT),
                        "我猜的", CLOCK.instant()));
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(AttemptStatus.SUBMITTED,
                harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow().status(),
                "a failed submitted Diagnostic remains closed");
        assertEquals(ApplyScriptData.WRONG_DERIVATIVE,
                harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow()
                        .submission().finalDerivative().confirmedCanonical(),
                "the retry body must not replace the saved failed Diagnostic submission");
        assertNotNull(recovered.interaction().teachingProjection(),
                "recovery must resume remediation from the saved failed submission");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, recovered.interaction().stage());
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aRetryBodyCannotReplaceTheSavedDiagnosticSubmission() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        harness.artifacts().closeAttempt(diagnosticAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                                ApplyScriptData.UNICODE_CORRECT_CANONICAL, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), diagnosticAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "替换答案");
        assertEquals(LearningStage.INDEPENDENT_TEST, recovered.interaction().stage(),
                "recovery must assess the saved Diagnostic submission, not the retry body");
        assertEquals(ApplyScriptData.UNICODE_CORRECT_CANONICAL,
                harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow()
                        .submission().finalDerivative().confirmedCanonical(),
                "the closed Attempt must retain its committed submission");
        assertNotNull(recovered.interaction().learnerProjection());
    }

    @Test
    void aRetryBodyCannotReplaceTheSavedIndependentSubmissionEvenWhenUnparseable() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        harness.artifacts().closeAttempt(independentAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                "not-a-derivative", "also-wrong", null);
        assertEquals(3, recovered.interaction().interactionVersion());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "an unparseable retry body must not block recovery of the saved submission");
        assertEquals(ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                harness.artifacts().findAttempt(independentAttemptId).orElseThrow()
                        .submission().finalDerivative().confirmedCanonical());
    }

    @Test
    void aStaleInteractionVersionConflictsAndDoesNotAdvance() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.SubmissionIgnored unknownAttempt = (LearningFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), UUID.randomUUID(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND, unknownAttempt.reason());
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void aRejectedSubmissionLeavesTheAttemptOpenAndDoesNotAdvanceTheInteraction() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID attemptId = started.interaction().attemptId();
        LearningFlowResult.SubmissionRejected rejected = (LearningFlowResult.SubmissionRejected) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), attemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL + " + 1", null);
        assertEquals(SubmissionRejectionReason.CONFIRMATION_MISMATCH, rejected.reason());
        assertEquals(AttemptStatus.OPEN, harness.artifacts().findAttempt(attemptId).orElseThrow().status(),
                "a rejected submission must leave the attempt open for correction");
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
        LearningFlowResult.Boundary corrected = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), attemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(2, corrected.interaction().interactionVersion(),
                "a corrected confirmed submission must still advance the flow");
    }

    @Test
    void aClosedDiagnosticAttemptWithANewKeyIsIgnoredWithoutASecondEvaluation() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        harness.useCase().submitAnswer(started.interaction().flowId(), 1, UUID.randomUUID(), diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.SubmissionIgnored duplicate = (LearningFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION, duplicate.reason(),
                "an Attempt replaced by a later Interaction cannot be routed again");
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        LearningFlowInteraction interaction = explained.interaction();
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

        LearningFlowResult.Boundary practice = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
                interaction.flowId(), interaction.interactionVersion(), UUID.randomUUID());
        LearningFlowInteraction practiceInteraction = practice.interaction();
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
    void aProvenIncorrectDiagnosticWithoutRationaleEntersLearningAndPracticeWithoutAssessment() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())), assessment);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(
                LEARNER_ID, UUID.randomUUID());

        LearningFlowResult.Boundary failed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);

        LearningFlowInteraction interaction = failed.interaction();
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertNull(interaction.attemptId(), "the first guarded move is Explain, not a Practice Attempt");
        assertNotNull(interaction.teachingProjection());
        assertTrue(assessment.contexts().isEmpty(), "missing rationale must not invoke rationale assessment");
        assertEquals(1, harness.generation().calls().size(),
                "a Diagnostic Not Passed result must not generate an Independent task");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a Diagnostic Not Passed result must not create Diagnostic Evidence");
    }

    @Test
    void aReplayedDiagnosticFailureReturnsTheOriginalExplainBoundaryWithoutRegeneration() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID failKey = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, failKey, started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        UUID continueKey = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
                explained.interaction().flowId(), 2, continueKey);
        assertEquals(3, first.interaction().interactionVersion());
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.SubmissionIgnored ignored = (LearningFlowResult.SubmissionIgnored) harness.useCase()
                .continueRequested(started.interaction().flowId(), 1, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.CONTINUE_NOT_LEGAL, ignored.reason());
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "an illegal Continue must never create a Task Package");
    }

    @Test
    void anExplainSourceGapCommitsAnUnavailableBoundaryAndContinueIsIgnored() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainSourceGapJson())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(2, unavailable.interaction().interactionVersion());
        assertEquals(InteractionKind.UNAVAILABLE, unavailable.interaction().kind());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, unavailable.interaction().status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, unavailable.interaction().stage());
        assertEquals(ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.interaction().learnerMessage());
        assertNull(unavailable.interaction().attemptId());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a Source Gap must create no Practice Package");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
        assertEquals(0, harness.flowStore().exposedExampleFingerprints(started.interaction().flowId()).size());
        LearningFlowResult.SubmissionIgnored continueIgnored = (LearningFlowResult.SubmissionIgnored) harness.useCase()
                .continueRequested(unavailable.interaction().flowId(), 2, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.CONTINUE_NOT_LEGAL, continueIgnored.reason());
        assertEquals(2, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion());
    }

    @Test
    void anExistingFlowProviderFailureCommitsUnavailableAwaitingInputWithPendingOperationAndNoPartialArtifacts() {
        ScriptedExplainGenerationModel explain = new ScriptedExplainGenerationModel(
                1, List.of(ExplainScriptData.explainReadyJson()));
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                explain);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(InteractionKind.UNAVAILABLE, unavailable.interaction().kind());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, unavailable.interaction().status(),
                "an existing-Flow provider failure is a recoverable Unavailable Interaction, not a terminal Flow");
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, harness.flowStore().findFlow(started.interaction().flowId())
                .orElseThrow().status());
        assertEquals(ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.interaction().learnerMessage());
        assertNull(unavailable.interaction().attemptId());
        assertNull(unavailable.interaction().teachingProjection());
        PendingOperation pending = harness.flowStore().pendingOperation(started.interaction().flowId()).orElseThrow();
        assertEquals(0, pending.failedRetryCount(),
                "the initial unavailable boundary has retry count zero");
        assertTrue(pending.retryAdvertised());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a provider failure must not persist a half-finished teaching artifact or a new Attempt");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
        assertEquals(0, harness.flowStore().exposedExampleFingerprints(started.interaction().flowId()).size());
        assertEquals(1, explain.calls().size());
    }

    @Test
    void retryRequestedIsLegalOnlyOnUnavailableAndResumesTheSavedPendingOperationWithoutAClientAnswer() {
        ScriptedExplainGenerationModel explain = new ScriptedExplainGenerationModel(
                1, List.of(ExplainScriptData.explainReadyJson()));
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                explain);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.SubmissionIgnored illegal = (LearningFlowResult.SubmissionIgnored) harness.useCase()
                .retryRequested(started.interaction().flowId(), 1, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.RETRY_NOT_LEGAL, illegal.reason());
        assertEquals(1, started.interaction().interactionVersion());

        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        UUID retryKey = UUID.randomUUID();
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase()
                .retryRequested(unavailable.interaction().flowId(), 2, retryKey);
        assertEquals(3, recovered.interaction().interactionVersion());
        assertEquals(InteractionKind.TEACHING, recovered.interaction().kind());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, recovered.interaction().status());
        assertNotNull(recovered.interaction().teachingProjection());
        assertTrue(harness.flowStore().pendingOperation(started.interaction().flowId()).isEmpty(),
                "a successful retry commits the next interaction and clears the Pending Operation");
        assertEquals(2, explain.calls().size());
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase()
                .retryRequested(unavailable.interaction().flowId(), 2, retryKey);
        assertEquals(recovered.interaction(), replay.interaction(),
                "a replayed retry Idempotency-Key returns the original committed interaction");
    }

    @Test
    void threeFailedRetriesStopAdvertisingRetryAndFlowControlCanStillLeave() {
        ScriptedExplainGenerationModel explain = new ScriptedExplainGenerationModel(4, List.of());
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                explain);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        UUID flowId = unavailable.interaction().flowId();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase()
                .retryRequested(flowId, 2, UUID.randomUUID());
        assertEquals(3, first.interaction().interactionVersion());
        assertEquals(InteractionKind.UNAVAILABLE, first.interaction().kind());
        assertEquals(1, harness.flowStore().pendingOperation(flowId).orElseThrow().failedRetryCount());
        LearningFlowResult.Boundary second = (LearningFlowResult.Boundary) harness.useCase()
                .retryRequested(flowId, 3, UUID.randomUUID());
        assertEquals(2, harness.flowStore().pendingOperation(flowId).orElseThrow().failedRetryCount());
        LearningFlowResult.Boundary third = (LearningFlowResult.Boundary) harness.useCase()
                .retryRequested(flowId, 4, UUID.randomUUID());
        assertEquals(5, third.interaction().interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, third.interaction().status());
        PendingOperation exhausted = harness.flowStore().pendingOperation(flowId).orElseThrow();
        assertEquals(3, exhausted.failedRetryCount());
        assertFalse(exhausted.retryAdvertised(),
                "after three failed retries the chain no longer advertises retry");
        LearningFlowResult.SubmissionIgnored refused = (LearningFlowResult.SubmissionIgnored) harness.useCase()
                .retryRequested(flowId, 5, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.RETRY_NOT_LEGAL, refused.reason());
        assertEquals(5, harness.flowStore().latestInteraction(flowId).orElseThrow().interactionVersion());
        LearningFlowResult.Boundary left = (LearningFlowResult.Boundary) harness.useCase()
                .flowControlRequested(flowId, 5, UUID.randomUUID());
        assertEquals(InteractionKind.TRANSITION, left.interaction().kind());
        assertEquals(FlowStatus.TERMINAL, left.interaction().status());
        assertEquals(LearningStateGraph.FLOW_LEAVE_MESSAGE, left.interaction().learnerMessage());
        assertTrue(harness.flowStore().pendingOperation(flowId).isEmpty(),
                "leaving an unavailable Flow clears the Pending Operation");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
        assertEquals(1, harness.artifacts().allPackages().size());
    }

    @Test
    void aSecondInvalidExplainOutputCommitsAnUnavailableBoundary() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(
                        "{not valid json", "{not valid json")));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(InteractionKind.UNAVAILABLE, unavailable.interaction().kind());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, unavailable.interaction().status());
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary independent = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        LearningFlowInteraction interaction = independent.interaction();
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
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        LearningFlowInteraction interaction = replacement.interaction();
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        LearningFlowInteraction interaction = replacement.interaction();
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
    void aMalformedPracticeAssessmentIsRepairedOnceThenAcceptsFailEvidenceAndAFreshReplacement() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                ScriptedAssessmentModel.replies(
                        Optional.of(diagnosticFailJudgment()),
                        Optional.empty(),
                        Optional.of(conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                ScriptedPedagogyModel.scripted(
                        TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE, TeachingAction.APPLY_PRACTICE));
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        LearningFlowInteraction interaction = replacement.interaction();
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, interaction.learnerProjection().taskText());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a repaired valid fail must accept exactly one Evidence record");
        assertEquals(LearningResult.FAIL, harness.flowStore().allEvidence().get(0).result());
        assertEquals(List.of(conclusivePracticeJudgment()),
                responseAssessmentsFor(harness, practiceAttemptId),
                "only the repaired valid judgment is persisted");
        List<ModelContractAudit> audits = harness.artifacts().allContractAudits();
        assertEquals(1, audits.size());
        assertEquals(ModelContractAudit.ASSESSMENT, audits.get(0).responsibility());
        assertEquals(List.of("unknown_field"), audits.get(0).violationCodes());
        assertEquals(0, audits.get(0).repairCount());
        assertEquals(practiceAttemptId, audits.get(0).attemptId());
        assertEquals(ModelContractAudit.PROVIDER_CATEGORY, audits.get(0).providerCategory());
        assertFalse(audits.get(0).correlationId().isBlank());
    }

    @Test
    void twoInvalidPracticeAssessmentsBecomeInconclusiveWithNoEvidenceAndNoPersistedJudgment() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                ScriptedAssessmentModel.replies(
                        Optional.of(diagnosticFailJudgment()), Optional.empty(), Optional.empty()));
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        LearningFlowInteraction interaction = replacement.interaction();
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, interaction.learnerProjection().taskText());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a still-invalid Assessment must never create Evidence");
        assertTrue(responseAssessmentsFor(harness, practiceAttemptId).isEmpty(),
                "an invalid Assessment must never be persisted");
        List<ModelContractAudit> audits = harness.artifacts().allContractAudits();
        assertEquals(2, audits.size());
        assertEquals(0, audits.get(0).repairCount());
        assertEquals(1, audits.get(1).repairCount());
        assertEquals(audits.get(0).correlationId(), audits.get(1).correlationId());
        assertEquals(List.of("unknown_field"), audits.get(0).violationCodes());
        assertEquals(List.of("unknown_field"), audits.get(1).violationCodes());
        assertFalse(audits.get(0).violationCodes().toString().contains("{"),
                "audit metadata must never retain raw invalid JSON");
    }

    @Test
    void twoInvalidResponseVerificationsBecomeInconclusiveWithNoEvidence() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                ScriptedAssessmentModel.replies(
                        Optional.of(diagnosticFailJudgment()), Optional.of(inconclusiveJudgment())),
                ScriptedResponseVerificationModel.replies(Optional.empty(), Optional.empty()));
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        LearningFlowInteraction interaction = replacement.interaction();
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(ApplyScriptData.SECOND_PRACTICE_TASK_TEXT, interaction.learnerProjection().taskText());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a still-invalid Response Verification must never create Evidence");
        assertEquals(List.of(inconclusiveJudgment()),
                responseAssessmentsFor(harness, practiceAttemptId),
                "the valid Assessment is persisted; the invalid Verification is not");
        List<ModelContractAudit> audits = harness.artifacts().allContractAudits();
        assertEquals(2, audits.size());
        assertEquals(ModelContractAudit.RESPONSE_VERIFICATION, audits.get(0).responsibility());
        assertEquals(0, audits.get(0).repairCount());
        assertEquals(1, audits.get(1).repairCount());
        assertEquals(audits.get(0).correlationId(), audits.get(1).correlationId());
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        LearningFlowResult.Boundary afterFail = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practice.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, afterFail.interaction().stage(),
                "a conclusive fail must not make the Independent Test legal");
        assertEquals(AttemptPurpose.PRACTICE, afterFail.interaction().attemptPurpose());
        LearningFlowResult.Boundary afterPass = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID submitKey = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, submitKey, practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, submitKey, practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(first.interaction(), replay.interaction(), "a replayed key must return the original result");
        assertEquals(3, harness.generation().calls().size(),
                "a replay must never regenerate the Independent task");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replayed key must never accept a second Evidence");
        assertEquals(1, responseAssessmentsFor(harness, practiceAttemptId).size(),
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary independent = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        LearningFlowResult.SubmissionIgnored duplicate = (LearningFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                independent.interaction().flowId(), 4, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION, duplicate.reason(),
                "an Attempt replaced by a later Interaction cannot be routed again");
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        harness.artifacts().closeAttempt(practiceAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE,
                                ApplyScriptData.PRACTICE_CORRECT_CANONICAL, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the crash must leave the closed Attempt without committed Evidence");
        UUID retryKey = UUID.randomUUID();
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, retryKey, practiceAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "替换答案");
        assertEquals(4, recovered.interaction().interactionVersion());
        assertEquals(LearningStage.INDEPENDENT_TEST, recovered.interaction().stage());
        assertEquals(ApplyScriptData.PRACTICE_CORRECT_CANONICAL,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow()
                        .submission().finalDerivative().confirmedCanonical(),
                "the retry body must not replace the saved Practice submission");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must resume the evaluation of the saved submission exactly once");
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, retryKey, practiceAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "替换答案");
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(4, unavailable.interaction().interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, unavailable.interaction().status());
        assertEquals(InteractionKind.UNAVAILABLE, unavailable.interaction().kind());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a failed follow-up generation must not accept Practice Evidence");
        assertEquals(3, harness.generation().calls().size());
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase()
                .retryRequested(unavailable.interaction().flowId(), 4, UUID.randomUUID());
        assertEquals(5, recovered.interaction().interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, recovered.interaction().status());
        assertEquals(LearningStage.INDEPENDENT_TEST, recovered.interaction().stage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must recover the original outcome exactly once");
        assertEquals(4, harness.generation().calls().size());
        assertTrue(harness.flowStore().pendingOperation(unavailable.interaction().flowId()).isEmpty());
    }

    @Test
    void aFailedStartPreparationPersistsNothingAndReturns503() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.sourceGapJson()));
        Harness harness = harness(generation, new ScriptedTaskVerifier(List.of()),
                new ScriptedAssessmentModel(List.of()));
        UUID startKey = UUID.randomUUID();
        ApplicationException unavailable = assertThrows(ApplicationException.class,
                () -> harness.useCase().start(LEARNER_ID, startKey));
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, unavailable.errorCode(),
                "an initial Start preparation failure must return the generic 503");
        assertTrue(harness.flowStore().activeWorkFlowId(LEARNER_ID, CONCEPT_ID).isEmpty(),
                "a failed start must leave no Flow claim (and therefore no Flow, checkpoint, interaction, or exposure, all flow-bound)");
        assertTrue(harness.flowStore().findCommand(startKey).isEmpty(),
                "a failed start must not process the command");
        assertTrue(harness.artifacts().allPackages().isEmpty(),
                "a failed start must not persist a Task Package (and therefore no Attempt)");
        assertTrue(harness.artifacts().allVerifications().isEmpty(),
                "a failed start must leave no Task Verification audit either");
        assertTrue(harness.artifacts().findSource("openstax-calculus-v1-3.3").isEmpty(),
                "a failed start must not persist the Source Pack");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a failed start must never create Evidence");
        assertTrue(harness.flowStore().unfinishedReviewsFor(LEARNER_ID).isEmpty(),
                "a failed start must not create Active Review Work");
        assertTrue(harness.generation().calls().size() == 1,
                "the failing preparation must still attempt exactly one generation cycle");
    }

    @Test
    void aMalformedTaskVerificationVoidsOnlyThatCandidateAndDeliversAFreshStart() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), ApplyScriptData.taskReadyJson())),
                ScriptedTaskVerifier.replies(
                        Optional.empty(), Optional.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of()));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase()
                .start(LEARNER_ID, UUID.randomUUID());
        assertEquals(LearningStage.DIAGNOSTIC, started.interaction().stage());
        assertEquals(AttemptPurpose.DIAGNOSTIC, started.interaction().attemptPurpose());
        assertEquals(ApplyScriptData.TASK_TEXT, started.interaction().learnerProjection().taskText());
        assertEquals(2, harness.generation().calls().size(),
                "an invalid Task Verification must consume one fresh generation cycle");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "only the verified fresh candidate is persisted");
        assertEquals(1, harness.artifacts().allVerifications().size(),
                "the voided candidate must never persist a Task Verification verdict");
        assertEquals(TaskVerificationVerdict.Verdict.PASS,
                harness.artifacts().allVerifications().get(0).verdict());
        assertEquals(1, harness.flowStore().exposedTaskFingerprints(started.interaction().flowId()).size());
    }

    @Test
    void aFailedStartPreparationRetriesWithTheOriginalKeyAndBindsExactlyOnce() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.sourceGapJson(), ApplyScriptData.taskReadyJson()));
        Harness harness = harness(generation,
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of()));
        UUID startKey = UUID.randomUUID();
        assertThrows(ApplicationException.class, () -> harness.useCase().start(LEARNER_ID, startKey));
        LearningFlowResult.Boundary retried = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, startKey);
        assertEquals(1, retried.interaction().interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, retried.interaction().status());
        assertEquals(LearningStage.DIAGNOSTIC, retried.interaction().stage());
        assertEquals(AttemptPurpose.DIAGNOSTIC, retried.interaction().attemptPurpose());
        assertNotNull(retried.interaction().attemptId());
        assertEquals(LEARNER_ID, harness.flowStore().findFlow(retried.interaction().flowId())
                .orElseThrow().learnerId());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "the retried start must bind exactly one Diagnostic Package");
        assertEquals(1, harness.flowStore().exposedTaskFingerprints(retried.interaction().flowId()).size(),
                "the retried start must record exactly one Exposure");
        assertTrue(harness.artifacts().findSource("openstax-calculus-v1-3.3").isPresent(),
                "the retried start must persist the Source Pack");
        assertEquals(retried.interaction(),
                harness.flowStore().findCommand(startKey).orElseThrow().response(),
                "the retried start must process the original Idempotency-Key exactly once");
        LearningFlowResult.Boundary replayed = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, startKey);
        assertEquals(retried.interaction(), replayed.interaction(),
                "a replayed original key after recovery must return the original committed boundary");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a replay must never bind a second Package");
    }

    @Test
    void aStartHoldsTheUniqueActiveWorkClaimAndADifferentKeyConflictsWithTheExistingFlowId() {
        Harness harness = harness();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ActiveWorkConflictException conflict = assertThrows(ActiveWorkConflictException.class,
                () -> harness.useCase().start(LEARNER_ID, UUID.randomUUID()));
        assertEquals(first.interaction().flowId(), conflict.existingFlowId(),
                "the learner-safe 409 must carry only the existing Flow id needed for recovery");
        assertEquals(1, harness.generation().calls().size(),
                "a conflicting start must be rejected before any generation cycle");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a conflicting start must never bind a second Diagnostic");
        assertEquals(first.interaction().flowId(),
                harness.flowStore().activeWorkFlowId(LEARNER_ID, CONCEPT_ID).orElseThrow(),
                "the first Flow keeps the unique Active Work claim");
    }

    @Test
    void aTerminalFlowReleasesTheClaimAndAllowsANewDiagnostic() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson(), ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of()));
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary left = (LearningFlowResult.Boundary) harness.useCase().flowControlRequested(
                first.interaction().flowId(), 1, UUID.randomUUID());
        assertEquals(FlowStatus.TERMINAL, left.interaction().status());
        assertEquals(FlowStatus.TERMINAL, harness.flowStore().findFlow(first.interaction().flowId())
                        .orElseThrow().status(),
                "a committed terminal boundary must mark the Flow terminal");
        assertTrue(harness.flowStore().activeWorkFlowId(LEARNER_ID, CONCEPT_ID).isEmpty(),
                "a terminal Flow with no unfinished Review releases the Active Work claim");
        LearningFlowResult.Boundary second = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        assertNotEquals(first.interaction().flowId(), second.interaction().flowId(),
                "the released claim permits a fresh Diagnostic");
    }

    @Test
    void anUnfinishedReviewBlocksANewDiagnosticWithTheExistingFlowId() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "the Independent pass must leave exactly one unfinished Review");
        ActiveWorkConflictException blocked = assertThrows(ActiveWorkConflictException.class,
                () -> harness.useCase().start(LEARNER_ID, UUID.randomUUID()));
        assertEquals(started.interaction().flowId(), blocked.existingFlowId(),
                "the unfinished Review blocks a new Diagnostic through its terminal Flow id");
        assertEquals(2, harness.artifacts().allPackages().size(),
                "the blocked start must not bind a second Diagnostic on top of the two existing packages");
    }

    @Test
    void aNoHintIndependentFailAcceptsExactlyOneFailEvidenceAndStartsRemediation() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        LearningFlowResult.Boundary remediated = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        LearningFlowInteraction interaction = remediated.interaction();
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status(),
                "a no-hint Independent FAIL must not end the flow: remediation begins");
        assertNotNull(interaction.teachingProjection(),
                "the deterministic fallback of an Independent failure is the Explain teaching boundary");
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a conclusive no-hint Independent fail must accept exactly one fail Evidence");
        AcceptedLearningEvidence evidence = harness.flowStore().allEvidence().get(0);
        assertEquals(independentAttemptId, evidence.taskAttemptId());
        assertEquals(LearningResult.FAIL, evidence.result());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, evidence.attemptPurpose());
        assertEquals(0, evidence.highestHintLevel(), "a no-hint Independent fail must never use a hint");
        assertTrue(evidence.assistanceTrace().isEmpty());
        assertTrue(responseAssessmentsFor(harness, independentAttemptId).isEmpty(),
                "a proven non-equivalence must never invoke a model judgment");
        assertEquals(1, harness.pedagogy().calls().size(),
                "the Independent failure must be one guarded decision");
        assertFalse(harness.pedagogy().lastContextJson().contains("\"independent_test\""),
                "the legal set of an Independent failure must never offer a fresh Independent Test");
        assertEquals(2, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size());
    }

    @Test
    void aReplayedIndependentFailReturnsTheOriginalRemediationBoundaryWithoutASecondFailEvidence() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        UUID failKey = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, failKey, independentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, failKey, independentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(first.interaction(), replay.interaction(), "a replayed key must return the original result");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replayed Independent fail must never accept a second fail Evidence");
        assertEquals(1, harness.pedagogy().calls().size(),
                "a replay must never re-run the guarded decision");
        assertEquals(1, harness.explainGeneration().calls().size(),
                "a replay must never regenerate the remediation Explain");
    }

    @Test
    void aCommittedIndependentFailResubmittedWithANewKeyIsIgnoredAndNeverRunsRemediationTwice() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        harness.useCase().submitAnswer(started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        LearningFlowResult.SubmissionIgnored duplicate = (LearningFlowResult.SubmissionIgnored) harness.useCase().submitAnswer(
                started.interaction().flowId(), 3, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION, duplicate.reason(),
                "an Attempt replaced by a later Interaction cannot be routed again");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "an already-produced Independent fail outcome must never accept a second Evidence");
        assertEquals(1, harness.pedagogy().calls().size(),
                "a resubmitted committed fail must never re-run the guarded decision");
    }

    @Test
    void aCrashBetweenClosingAndCommittingAnIndependentFailResumesExactlyOnce() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        harness.artifacts().closeAttempt(independentAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.WRONG_DERIVATIVE,
                                ApplyScriptData.WRONG_DERIVATIVE, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the crash must leave the closed Attempt without committed Evidence");
        UUID retryKey = UUID.randomUUID();
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, retryKey, independentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(3, recovered.interaction().interactionVersion());
        assertNotNull(recovered.interaction().teachingProjection(),
                "the retry must resume the remediation Explain of the saved submission");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must accept the fail Evidence exactly once");
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, retryKey, independentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(recovered.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size());
    }

    @Test
    void aBlockedIndependentCreatesNoEvidenceAndDeliversAFreshVerifiedReplacement() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        independentTaskJson(),
                        ApplyScriptData.taskReadyJson(
                                "设 s(x) = 9x³ − 4x + 7，求 s'(x)。", "27*x^2 - 4"))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.CLEARLY_CONTRADICTORY))),
                new ScriptedResponseVerificationModel(List.of()));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.CONTRADICTORY_RATIONALE);
        LearningFlowInteraction interaction = replacement.interaction();
        assertEquals(3, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.INDEPENDENT_TEST, interaction.stage(),
                "a Blocked Independent judgment must deliver a fresh verified Independent replacement");
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, interaction.attemptPurpose());
        assertNotEquals(independentAttemptId, interaction.attemptId(),
                "the replacement must be a fresh Attempt, never a reuse of the closed one");
        assertEquals(IndependentSubmissionFlow.REPLACEMENT_MESSAGE, interaction.learnerMessage());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "ADR-0061: a Blocked Independent judgment must never create Evidence");
        assertEquals(3, harness.generation().calls().size(),
                "the replacement is a fresh Independent generation, never the same task");
        assertEquals(3, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size(),
                "the displayed replacement task must join the exposure ledger");
        assertEquals(0, harness.pedagogy().calls().size(),
                "a Blocked replacement is a single mandated move and must bypass the Pedagogy Agent");
    }

    @Test
    void anInconclusiveIndependentCreatesNoEvidenceAndDeliversAFreshVerifiedReplacement() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        independentTaskJson(),
                        ApplyScriptData.taskReadyJson(
                                "设 t(x) = 2x⁴ − 3x + 8，求 t'(x)。", "8*x^3 - 3"))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED))),
                new ScriptedResponseVerificationModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_EQUIVALENT, RationaleJudgment.NOT_PROVIDED))));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), independentAttemptId,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);
        LearningFlowInteraction interaction = replacement.interaction();
        assertEquals(3, interaction.interactionVersion());
        assertEquals(LearningStage.INDEPENDENT_TEST, interaction.stage(),
                "an Inconclusive Independent judgment must deliver a fresh verified Independent replacement");
        assertNotEquals(independentAttemptId, interaction.attemptId());
        assertEquals(IndependentSubmissionFlow.REPLACEMENT_MESSAGE, interaction.learnerMessage());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "an Inconclusive Independent judgment must never create Evidence");
        assertEquals(3, harness.generation().calls().size());
        assertEquals(2, responseAssessmentsFor(harness, independentAttemptId).size(),
                "the isolated assessment and its response verification of the Inconclusive submission must be recorded once each");
        assertEquals(0, harness.pedagogy().calls().size());
    }

    @Test
    void afterAnIndependentFailOnlyANewQualifyingPracticePassReopensFreshIndependentTesting() {
        Harness harness = harness(
                advancingClock(),
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), independentTaskJson(),
                        secondPracticeTaskJson(),
                        ApplyScriptData.taskReadyJson(
                                "设 u(x) = 6x⁴ − 2x² + 5，求 u'(x)。", "24*x^3 - 4*x"))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment(),
                        conclusivePracticeJudgment(), conclusivePracticeJudgment(),
                        conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(
                        ExplainScriptData.explainReadyJson(), secondExplainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel());
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary independent = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practice.interaction().flowId(), 3, UUID.randomUUID(), practiceAttemptId,
                ApplyScriptData.PRACTICE_CORRECT_DERIVATIVE, ApplyScriptData.PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(LearningStage.INDEPENDENT_TEST, independent.interaction().stage(),
                "the first qualifying practice pass makes the fresh Independent Test legal");
        UUID firstIndependentAttemptId = independent.interaction().attemptId();
        LearningFlowResult.Boundary remediated = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                independent.interaction().flowId(), 4, UUID.randomUUID(), firstIndependentAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertNotNull(remediated.interaction().teachingProjection(),
                "the Independent fail begins remediation through the Explain boundary");
        assertEquals(2, harness.flowStore().allEvidence().size(),
                "the cycle holds the qualifying practice pass and exactly one Independent fail Evidence");
        assertFalse(harness.pedagogy().lastContextJson().contains("\"independent_test\""),
                "right after the Independent fail, fresh Independent testing must not be legal");
        LearningFlowResult.Boundary practiceAgain = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
                remediated.interaction().flowId(), remediated.interaction().interactionVersion(), UUID.randomUUID());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, practiceAgain.interaction().stage());
        assertEquals(AttemptPurpose.PRACTICE, practiceAgain.interaction().attemptPurpose());
        UUID secondPracticeAttemptId = practiceAgain.interaction().attemptId();
        LearningFlowResult.Boundary freshIndependent = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                practiceAgain.interaction().flowId(), 6, UUID.randomUUID(), secondPracticeAttemptId,
                ApplyScriptData.SECOND_PRACTICE_CORRECT_DERIVATIVE,
                ApplyScriptData.SECOND_PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(LearningStage.INDEPENDENT_TEST, freshIndependent.interaction().stage(),
                "only a NEW qualifying practice pass after the failure reopens fresh Independent testing");
        assertNotEquals(firstIndependentAttemptId, freshIndependent.interaction().attemptId(),
                "the re-opened Independent Test must be a fresh Attempt");
        UUID secondIndependentAttemptId = freshIndependent.interaction().attemptId();
        UUID passKey = UUID.randomUUID();
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                freshIndependent.interaction().flowId(), 7, passKey, secondIndependentAttemptId,
                "24*x^3 - 4*x", "24*x^3 - 4*x", null);
        assertEquals(4, harness.flowStore().allEvidence().size(),
                "the re-entry cycle holds: qualifying pass #1, the Independent fail, the new qualifying pass, and the Independent pass");
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "the post-remediation Independent pass must schedule the unique Review 1");
        ConceptProgress progress =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "a successful re-entry restores the Independent milestone");
    }

    @Test
    void anExposedHintLadderAndRevealedSolutionFingerprintsEnterTheLedgerAndLaterGeneration() {
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
                        TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE,
                        TeachingAction.APPLY_PRACTICE));
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary h1 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practiceAttemptId, false, UUID.randomUUID());
        assertEquals(1, harness.flowStore().exposedHintLadderFingerprints(h1.interaction().flowId()).size(),
                "the generated ladder fingerprint must enter the novelty ledger on the first reveal");
        String ladderFingerprint = harness.flowStore()
                .exposedHintLadderFingerprints(h1.interaction().flowId()).get(0);
        assertFalse(harness.generation().calls().get(1).contextJson().contains(ladderFingerprint),
                "the practice task delivered before the ladder must not yet be excluded by it");
        LearningFlowResult.Boundary h5 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                h1.interaction().flowId(), h1.interaction().interactionVersion(),
                practiceAttemptId, true, UUID.randomUUID());
        assertEquals(1, harness.flowStore().exposedHintLadderFingerprints(h5.interaction().flowId()).size(),
                "repeated reveals must not duplicate the ladder fingerprint");
        assertEquals(1, harness.flowStore().exposedRevealedSolutionFingerprints(h5.interaction().flowId()).size(),
                "the H5 reveal fingerprint must enter the novelty ledger exactly once");
        String revealFingerprint = harness.flowStore()
                .exposedRevealedSolutionFingerprints(h5.interaction().flowId()).get(0);
        assertEquals(AttemptPurpose.PRACTICE, h5.interaction().attemptPurpose(),
                "after the H5 reveal the guarded decision may choose a fresh Apply Practice task");
        String freshContext = harness.generation().lastContextJson();
        assertTrue(freshContext.contains(ladderFingerprint),
                "the later generation context must contain the exposed ladder fingerprint");
        assertTrue(freshContext.contains(revealFingerprint),
                "the later generation context must contain the exposed revealed-solution fingerprint");
        assertTrue(harness.flowStore().exposedTaskFingerprints(h5.interaction().flowId()).size() >= 3,
                "the fresh practice task must join the task exposure ledger");
    }

    /**
     * A clock that advances on every read, so evidence accepted after an
     * Independent fail carries a strictly later acceptance time and the
     * cycle-aware readiness fact can distinguish the new qualifying pass
     * from the old one.
     */
    private static Clock advancingClock() {
        return new Clock() {
            private long nanos = 0;

            @Override
            public Instant instant() {
                return Instant.parse("2026-08-15T00:00:00Z").plusNanos(nanos++);
            }

            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }
        };
    }

    @Test
    void anIndependentFailAfterIndependentEvidenceDropsCurrentMilestoneToLearningButKeepsHighest() {
        Harness harness = harness(
                advancingClock(),
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), independentTaskJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(
                                "设 v(x) = 3x³ − 5x + 2，求 v'(x)。", "9*x^2 - 5"),
                        ApplyScriptData.taskReadyJson(
                                "设 w(x) = 4x³ − x + 3，求 w'(x)。", "12*x^2 - 1"))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel());
        // Flow 1: a passing Diagnostic and a passing Independent Test establish
        // the Independent milestone. The scheduled Review is started and then
        // explicitly left, so its Attempt is abandoned without evidence or
        // milestone change. The Started Review is then cancelled through its
        // independent resource before a fresh Diagnostic becomes legal again.
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary firstTransitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                first.interaction().flowId(), 1, UUID.randomUUID(), first.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary firstCompleted = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                firstTransitioned.interaction().flowId(), 2, UUID.randomUUID(), firstTransitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(FlowStatus.TERMINAL, firstCompleted.interaction().status());
        assertEquals(1, harness.flowStore().allEvidence().size());
        ReviewTask review = harness.flowStore().unfinishedReviewsFor(LEARNER_ID).get(0);
        harness.flowStore().markDueReviewsDue(review.dueAt().plusSeconds(1));
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary) harness.reviewStartFlow().start(
                review.reviewId(), UUID.randomUUID());
        LearningFlowResult.Boundary leftReview = (LearningFlowResult.Boundary) harness.useCase().flowControlRequested(
                first.interaction().flowId(), reviewBoundary.interaction().interactionVersion(), UUID.randomUUID());
        assertEquals(FlowStatus.TERMINAL, leftReview.interaction().status());
        assertEquals(ReviewTaskStatus.STARTED,
                harness.flowStore().findReview(review.reviewId()).orElseThrow().status(),
                "Flow Control abandons the Attempt but does not cancel the Review");
        new ReviewCancellationUseCase(harness.flowStore(), harness.flowStore(), CLOCK)
                .cancel(review.reviewId(), UUID.randomUUID());
        assertEquals(ReviewTaskStatus.CANCELLED,
                harness.flowStore().findReview(review.reviewId()).orElseThrow().status(),
                "the independent cancellation resource must release the Review claim");
        assertTrue(harness.flowStore().unfinishedReviewsFor(LEARNER_ID).isEmpty());
        ConceptProgress afterPass =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.INDEPENDENT, afterPass.currentMilestone());
        assertEquals(MasteryMilestone.INDEPENDENT, afterPass.highestMilestoneReached());
        // Flow 2: a fresh Independent Test fails conclusively without hints.
        LearningFlowResult.Boundary second = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary secondTransitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                second.interaction().flowId(), 1, UUID.randomUUID(), second.interaction().attemptId(),
                "9x²−5", "9*x^2-5", null);
        LearningFlowResult.Boundary remediated = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                secondTransitioned.interaction().flowId(), 2, UUID.randomUUID(),
                secondTransitioned.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertNotNull(remediated.interaction().teachingProjection(),
                "the conclusive no-hint Independent fail begins remediation");
        assertEquals(2, harness.flowStore().allEvidence().size(),
                "exactly one Independent-fail Evidence is accepted in the second flow");
        AcceptedLearningEvidence independentFail = harness.flowStore().allEvidence().stream()
                .filter(item -> item.result() == LearningResult.FAIL)
                .findFirst().orElseThrow();
        assertEquals(LearningResult.FAIL, independentFail.result());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, independentFail.attemptPurpose());
        assertEquals(0, independentFail.highestHintLevel());
        ConceptProgress afterFail =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.LEARNING, afterFail.currentMilestone(),
                "a verified no-hint Independent failure drops Current Milestone to Learning");
        assertEquals(MasteryMilestone.INDEPENDENT, afterFail.highestMilestoneReached(),
                "Highest Milestone Reached is historical and never decreases");
    }

    @Test
    void aFirstHintRequestGeneratesTheStableLadderAndRevealsH1KeepingTheAttemptOpen() {
        Harness harness = practiceHarness();
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary h1 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        LearningFlowInteraction interaction = h1.interaction();
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary h1 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        LearningFlowResult.Boundary h2 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary h1 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practiceAttemptId, false, UUID.randomUUID());
        LearningFlowResult.Boundary h2 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                h1.interaction().flowId(), h1.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        LearningFlowResult.Boundary submitted = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary revealed = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, true, UUID.randomUUID());
        LearningFlowInteraction interaction = revealed.interaction();
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
        assertTrue(responseAssessmentsFor(harness, practiceAttemptId).isEmpty(),
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
        LearningFlowResult.HintIgnored later = (LearningFlowResult.HintIgnored) harness.useCase().requestHint(
                interaction.flowId(), interaction.interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION, later.reason(),
                "an Attempt replaced by a later Interaction cannot be routed again");
        LearningFlowResult.HintIgnored teachBackHint = (LearningFlowResult.HintIgnored) harness.useCase().requestHint(
                interaction.flowId(), interaction.interactionVersion(), interaction.attemptId(), false, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE, teachBackHint.reason(),
                "hints are never legal on an open Teach-back Attempt (ADR-0065)");
        assertEquals(1, harness.hintGeneration().calls().size(),
                "only the practice ladder generation may ever happen, never one for the Teach-back Attempt");
    }

    @Test
    void hintsAreIgnoredForDiagnosticIndependentAndUnknownAttempts() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.HintIgnored diagnostic = (LearningFlowResult.HintIgnored) harness.useCase().requestHint(
                started.interaction().flowId(), 1, started.interaction().attemptId(), false, UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE, diagnostic.reason());
        assertEquals(0, harness.hintGeneration().calls().size(),
                "no model call may ever happen for a wrong-purpose hint request");
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion(), "an ignored hint must not advance the interaction");
        LearningFlowResult.HintIgnored unknown = (LearningFlowResult.HintIgnored) harness.useCase().requestHint(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, UUID.randomUUID());
        LearningFlowInteraction interaction = unavailable.interaction();
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().requestHint(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID key = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, key);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().requestHint(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID h1Key = UUID.randomUUID();
        LearningFlowResult.Boundary h1 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, false, h1Key);
        UUID crashKey = UUID.randomUUID();
        HintLadder ladder = harness.artifacts().findLadder(practiceAttemptId).orElseThrow();
        HintExposureOutcome exposed = harness.artifacts().exposeHint(practiceAttemptId, ladder, 2, crashKey);
        assertInstanceOf(HintExposureOutcome.Exposed.class, exposed);
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                h1.interaction().flowId(), h1.interaction().interactionVersion(), practiceAttemptId, false, crashKey);
        assertEquals(5, recovered.interaction().interactionVersion());
        assertEquals(2, recovered.interaction().hint().level(),
                "the retried command must resume the same exposed level, never the next one");
        assertEquals(List.of(1, 2), harness.artifacts().findAttempt(practiceAttemptId).orElseThrow()
                .assistanceTrace().stream().map(entry -> entry.level().level()).toList(),
                "the resumed exposure must not duplicate a trace entry");
        LearningFlowResult.Boundary next = (LearningFlowResult.Boundary) harness.useCase().requestHint(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID crashKey = UUID.randomUUID();
        HintLadder ladder = HintLadder.from(practiceAttemptId,
                (HintGenerationDraft.LadderReady) HintGenerationDraft.parse(HintScriptData.ladderReadyJson()));
        assertInstanceOf(HintExposureOutcome.Exposed.class,
                harness.artifacts().exposeHint(practiceAttemptId, ladder, 5, crashKey));
        assertEquals(AttemptStatus.SOLUTION_REVEALED,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status(),
                "the crashed run already closed the attempt as Solution Revealed");
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                practice.interaction().flowId(), practice.interaction().interactionVersion(), practiceAttemptId, true, crashKey);
        assertEquals(4, recovered.interaction().interactionVersion());
        assertEquals(5, recovered.interaction().hint().level(),
                "the retried answer request must resume the same H5 reveal, not a fresh generation");
        assertEquals("18*x^2-4", recovered.interaction().hint().proposedFinalAnswer());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the resumed H5 reveal must never create Evidence");
        assertTrue(responseAssessmentsFor(harness, practiceAttemptId).isEmpty());
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
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        LearningFlowResult.Boundary followUp = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBackAttemptId,
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        LearningFlowInteraction interaction = followUp.interaction();
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
        assertEquals(1, committedTeachBackAssessmentsFor(harness, teachBackAttemptId).size(),
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
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        LearningFlowResult.Boundary followUp = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBackAttemptId,
                "说不清为什么幂法则适用。", "说不清为什么幂法则适用。", null);
        LearningFlowInteraction interaction = replacement.interaction();
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
    void twoInvalidTeachBackAssessmentsBecomeInconclusiveWithNoEvidenceAndNoPersistedJudgment() {
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
                ScriptedTeachBackAssessmentModel.replies(Optional.empty(), Optional.empty()));
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBackAttemptId,
                "说不清为什么幂法则适用。", "说不清为什么幂法则适用。", null);
        LearningFlowInteraction interaction = replacement.interaction();
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertEquals(TeachBackScriptData.LEARNER_PROMPT, interaction.learnerProjection().taskText(),
                "a still-invalid Teach-back Assessment must deliver a fresh Teach-back task");
        assertEquals(TeachBackFlow.TEACH_BACK_REPLACEMENT_MESSAGE, interaction.learnerMessage());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a still-invalid Teach-back Assessment must never create Evidence");
        assertTrue(committedTeachBackAssessmentsFor(harness, teachBackAttemptId).isEmpty(),
                "an invalid Teach-back Assessment must never be persisted");
        List<ModelContractAudit> audits = harness.artifacts().allContractAudits();
        assertEquals(2, audits.size());
        assertEquals(ModelContractAudit.TEACH_BACK_ASSESSMENT, audits.get(0).responsibility());
        assertEquals(0, audits.get(0).repairCount());
        assertEquals(1, audits.get(1).repairCount());
        assertEquals(audits.get(0).correlationId(), audits.get(1).correlationId());
    }

    @Test
    void aReplayedTeachBackSubmissionReturnsTheOriginalInteractionWithoutASecondAssessment() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID key = UUID.randomUUID();
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                key, teachBackAttemptId,
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                key, teachBackAttemptId,
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        assertEquals(first.interaction(), replay.interaction(), "a replayed key must return the original result");
        assertEquals(1, harness.teachBackAssessment().contexts().size(),
                "a replay must never run a second Teach-back Assessment");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replay must never accept a second Evidence");
        assertEquals(1, committedTeachBackAssessmentsFor(harness, teachBackAttemptId).size());
    }

    @Test
    void aCrashBetweenClosingAndCommittingATeachBackSubmissionResumesExactlyOnce() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson(), secondPracticeTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        harness.artifacts().closeAttempt(teachBackAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(TeachBackScriptData.PASS_EXPLANATION,
                                TeachBackScriptData.PASS_EXPLANATION, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the crash must leave the closed Attempt without committed Evidence");
        UUID retryKey = UUID.randomUUID();
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                retryKey, teachBackAttemptId,
                "这不是保存的作答。", "这不是保存的作答。", null);
        assertEquals(5, recovered.interaction().interactionVersion());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, recovered.interaction().stage());
        assertEquals(TeachBackScriptData.PASS_EXPLANATION,
                harness.artifacts().findAttempt(teachBackAttemptId).orElseThrow()
                        .submission().finalDerivative().confirmedCanonical(),
                "the retry body must not replace the saved Teach-back submission");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the retry must resume the assessment of the saved submission exactly once");
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                retryKey, teachBackAttemptId,
                "这不是保存的作答。", "这不是保存的作答。", null);
        assertEquals(recovered.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size());
    }

    @Test
    void theGuardOffersTeachBackOnlyWithAnEligibleAnchorAndNeverCallsTheModelWithoutOne() {
        Harness harness = practiceHarness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        assertEquals(0, harness.flowStore().latestAnchor(started.interaction().flowId()).map(anchor -> 1).orElse(0),
                "a freshly started Flow carries no eligible anchor yet");
        TeachBackDeliveryResult.Unavailable guarded =
                (TeachBackDeliveryResult.Unavailable) harness.teachBackFlow().deliverTeachBack(started.interaction().flowId(), ScriptedModelProfile.PROFILE);
        assertEquals(TeachBackUnavailableReason.NO_ELIGIBLE_ANCHOR, guarded.reason(),
                "without an eligible anchor the Guard must not offer Teach-back");
        assertEquals(0, harness.teachBackGeneration().calls().size(),
                "no model call may ever happen without an eligible anchor");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "no Teach-back package may open without an eligible anchor");

        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        LearningFlowResult.Boundary afterTeachBackPass = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                UUID.randomUUID(), teachBack.interaction().attemptId(),
                TeachBackScriptData.PASS_EXPLANATION, TeachBackScriptData.PASS_EXPLANATION, null);
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, afterTeachBackPass.interaction().stage(),
                "a Teach-back pass must not reopen Independent testing by itself");
        assertEquals(1, harness.flowStore().allEvidence().size());
        LearningFlowResult.Boundary practicePass = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                afterTeachBackPass.interaction().flowId(), afterTeachBackPass.interaction().interactionVersion(),
                UUID.randomUUID(), afterTeachBackPass.interaction().attemptId(),
                ApplyScriptData.SECOND_PRACTICE_CORRECT_DERIVATIVE,
                ApplyScriptData.SECOND_PRACTICE_CORRECT_CANONICAL, null);
        assertEquals(LearningStage.INDEPENDENT_TEST, practicePass.interaction().stage(),
                "only the current cycle's conclusive Apply Practice PASS can reopen Independent testing");
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, practicePass.interaction().attemptPurpose());
        assertEquals(2, harness.flowStore().allEvidence().size(),
                "the understanding Evidence and the assisted Practice Evidence are both accepted");
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary practice = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        LearningFlowResult.Boundary practice = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
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
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        LearningFlowResult.Boundary independent = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        LearningFlowResult.Boundary followUp = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, unavailable.interaction().status(),
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        assertEquals(2, harness.pedagogy().calls().size(),
                "the Diagnostic failure and Explain completion decisions both had several legal moves");
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        assertEquals(3, harness.pedagogy().calls().size(),
                "the three earlier decisions all had several legal moves");
        LearningFlowResult.Boundary replacement = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        LearningFlowResult.Boundary moreLearning = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID revealedAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary freshPractice = (LearningFlowResult.Boundary) harness.useCase().requestHint(
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
        assertTrue(responseAssessmentsFor(harness, revealedAttemptId).isEmpty());
    }

    @Test
    void aTemporaryExplainInsideAnOpenPracticeAttemptReturnsToTheSamePracticeInteraction() {
        Harness harness = practiceHarness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
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
        ApplyDeliveryResult injected = executor.deliver(ScriptedModelProfile.PROFILE, PracticeApplyFixture.practiceContext());
        assertInstanceOf(ApplyDeliveryResult.Delivered.class, injected);
        UUID openAttemptId = ((ApplyDeliveryResult.Delivered) injected).attempt().attemptId();
        TaskPackage openPackage = harness.artifacts()
                .findPackage(((ApplyDeliveryResult.Delivered) injected).attempt().taskPackageId()).orElseThrow();
        harness.flowStore().recordTaskExposure(explained.interaction().flowId(), openPackage);
        assertEquals(1, harness.pedagogy().calls().size(),
                "the Explain decision was the only model-driven decision so far");
        LearningFlowResult.Boundary resumed = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
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

    @Test
    void aSubstantiveClarificationOnAnOpenPracticeAttemptRecordsAssistanceAndReturnsToTheSameAttemptAfterContinue() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(
                        ExplainScriptData.explainReadyJson(), secondExplainReadyJson())));
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practiceAttemptId, "为什么可以把系数直接提出来？", UUID.randomUUID());
        LearningFlowInteraction interaction = explained.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertNotNull(interaction.teachingProjection(),
                "the substantive clarification must deliver the temporary Explain teaching boundary");
        assertEquals(LearningStateGraph.CLARIFICATION_EXPLAIN_MESSAGE, interaction.learnerMessage());
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status(),
                "the temporary Explain must keep the Practice Attempt open");
        assertEquals(AttemptPurpose.PRACTICE,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().purpose());
        assertEquals(List.of("substantive_clarification", "temporary_explain"),
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().assistanceTraceStrings(),
                "the substantive clarification and the temporary Explain are recorded assistance");
        assertEquals(2, harness.explainGeneration().calls().size(),
                "the remediation Explain plus the clarification Explain are the only two artifacts");
        assertEquals(0, harness.hintGeneration().calls().size(),
                "a clarification must never generate a hint ladder");
        assertEquals(2, harness.artifacts().allPackages().size(),
                "a temporary Explain must never open a Task Package");
        assertEquals(2, harness.flowStore().exposedExampleFingerprints(interaction.flowId()).size(),
                "the displayed clarification Explain is exposed for novelty");

        LearningFlowResult.Boundary resumed = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
                interaction.flowId(), interaction.interactionVersion(), UUID.randomUUID());
        assertEquals(5, resumed.interaction().interactionVersion());
        assertEquals(practiceAttemptId, resumed.interaction().attemptId(),
                "after the temporary Explain the single legal move returns to the SAME open Practice attempt");
        assertEquals(LearningStateGraph.RESUME_PRACTICE_MESSAGE, resumed.interaction().learnerMessage());
        assertEquals(ApplyScriptData.PRACTICE_TASK_TEXT, resumed.interaction().learnerProjection().taskText());
        assertEquals(2, harness.pedagogy().calls().size(),
                "the clarification path runs no new guarded pedagogy decision");
    }

    @Test
    void aClarificationOnTheTemporaryExplainInsideAnOpenPracticeAttemptRestatesTeachingConditionsAndKeepsTheAttemptOpen() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(
                        ExplainScriptData.explainReadyJson(), secondExplainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(),
                new ScriptedClarificationClassifier(List.of(
                        ClarificationClassification.SUBSTANTIVE, ClarificationClassification.PROCEDURAL)));
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practiceAttemptId, "为什么可以把系数直接提出来？", UUID.randomUUID());
        assertEquals(InteractionKind.TEACHING, explained.interaction().kind(),
                "the substantive clarification delivers the temporary Explain teaching boundary");
        TeachingProjection teaching = explained.interaction().teachingProjection();
        LearningFlowResult.Boundary answered = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                explained.interaction().flowId(), explained.interaction().interactionVersion(),
                null, "这个示例的记号怎么读？", UUID.randomUUID());
        assertEquals(InteractionKind.TEACHING, answered.interaction().kind(),
                "the procedural answer keeps the same teaching boundary");
        assertNull(answered.interaction().attemptId(),
                "the temporary-Explain clarification also addresses the interaction, not an Attempt");
        assertEquals(teaching, answered.interaction().teachingProjection(),
                "the same temporary Explain is re-shown, no new Explain is generated");
        assertTrue(answered.interaction().learnerMessage().startsWith("讲解说明"),
                "the procedural answer restates the displayed teaching conditions");
        TaskAttempt attempt = harness.artifacts().findAttempt(practiceAttemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, attempt.status(),
                "the open Practice Attempt stays open through the teaching-boundary clarification");
        assertEquals(List.of("substantive_clarification", "temporary_explain"),
                attempt.assistanceTraceStrings(),
                "the procedural clarification on the teaching boundary records no additional assistance");

        LearningFlowResult.Boundary resumed = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
                answered.interaction().flowId(), answered.interaction().interactionVersion(), UUID.randomUUID());
        assertEquals(practiceAttemptId, resumed.interaction().attemptId(),
                "Continue after the teaching-boundary clarification still returns to the open Practice attempt");
    }

    @Test
    void aProceduralClarificationOnAnOpenPracticeAttemptAnswersDirectlyWithoutLoadingATeachingProfile() {
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
                new ScriptedPedagogyModel(),
                new ScriptedClarificationClassifier(List.of(ClarificationClassification.PROCEDURAL)));
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        LearningFlowResult.Boundary answered = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practiceAttemptId, "这道题是只填最终导数吗？", UUID.randomUUID());
        LearningFlowInteraction interaction = answered.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(practiceAttemptId, interaction.attemptId(),
                "a procedural answer returns to the SAME open task boundary");
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose());
        assertNotNull(interaction.learnerProjection(), "the task projection is re-shown");
        assertTrue(interaction.learnerMessage().startsWith("答题说明"),
                "the procedural answer restates the task package's own format contract");
        assertTrue(interaction.learnerMessage().contains("f'(x)"),
                "the procedural answer restates the exposed answer field");
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().status(),
                "a procedural answer must never touch the Attempt");
        assertEquals(List.of("procedural_clarification"),
                harness.artifacts().findAttempt(practiceAttemptId).orElseThrow().assistanceTraceStrings(),
                "the procedural clarification is recorded without disqualifying anything");
        assertEquals(1, harness.explainGeneration().calls().size(),
                "a procedural clarification must never load a Teaching Node Profile");
        assertEquals(0, harness.hintGeneration().calls().size());
        assertEquals(1, harness.classifier().calls().size());
        assertEquals("这道题是只填最终导数吗？", harness.classifier().calls().get(0).message());
        assertEquals(ApplyScriptData.PRACTICE_TASK_TEXT, harness.classifier().calls().get(0).taskText(),
                "the classifier sees the learner-visible task text, never private facts");
    }

    @Test
    void aReplayedClarificationReturnsTheOriginalBoundaryWithoutAClassifierOrExplainRepetition() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(
                        ExplainScriptData.explainReadyJson(), secondExplainReadyJson())));
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        UUID practiceAttemptId = practice.interaction().attemptId();
        UUID key = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practiceAttemptId, "为什么可以把系数直接提出来？", key);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                practice.interaction().flowId(), practice.interaction().interactionVersion(),
                practiceAttemptId, "为什么可以把系数直接提出来？", key);
        assertEquals(first.interaction(), replay.interaction());
        assertEquals(1, harness.classifier().calls().size(),
                "a replayed clarification must never classify again");
        assertEquals(2, harness.explainGeneration().calls().size(),
                "a replayed clarification must never regenerate the teaching artifact");
    }

    @Test
    void aSubstantiveClarificationOnAnIndependentAttemptProjectsAnAssistanceConsentRequest() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                transitioned.interaction().flowId(), transitioned.interaction().interactionVersion(),
                independentAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        LearningFlowInteraction interaction = consented.interaction();
        assertEquals(3, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.INDEPENDENT_TEST, interaction.stage());
        assertEquals(independentAttemptId, interaction.attemptId());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, interaction.attemptPurpose(),
                "the consent request must not convert the attempt yet");
        assertNull(interaction.learnerProjection(), "no teaching or task content accompanies the consent request");
        assertNotNull(interaction.assistanceConsent());
        assertEquals(LearningStateGraph.CONSENT_WARNING_MESSAGE, interaction.assistanceConsent().warning());
        assertEquals(independentAttemptId, interaction.assistanceConsent().attemptId());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, interaction.assistanceConsent().attemptPurpose());
        TaskAttempt attempt = harness.artifacts().findAttempt(independentAttemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, attempt.status());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, attempt.purpose(),
                "the consent request must never convert, record, or close the independent attempt");
        assertTrue(attempt.assistanceTrace().isEmpty());
        assertEquals(0, harness.explainGeneration().calls().size(),
                "the consent request must expose no teaching content before consent");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void anInvalidClarificationClassificationFallsBackToUncertainConsentWithoutA503() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED))),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(),
                ScriptedClarificationClassifier.replies(Optional.empty()));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase()
                .start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                transitioned.interaction().flowId(), transitioned.interaction().interactionVersion(),
                independentAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        LearningFlowInteraction interaction = consented.interaction();
        assertEquals(InteractionKind.ASSISTANCE_CONSENT, interaction.kind());
        assertEquals(independentAttemptId, interaction.attemptId());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, interaction.attemptPurpose());
        assertNotNull(interaction.assistanceConsent());
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(independentAttemptId).orElseThrow().status());
        List<ModelContractAudit> audits = harness.artifacts().allContractAudits();
        assertEquals(1, audits.size());
        assertEquals(ModelContractAudit.CLARIFICATION, audits.get(0).responsibility());
        assertEquals(0, audits.get(0).repairCount());
        assertEquals(List.of("invalid_enum"), audits.get(0).violationCodes());
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void anAssistanceRefusalLeavesTheIndependentAttemptUnchanged() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                transitioned.interaction().flowId(), transitioned.interaction().interactionVersion(),
                independentAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        LearningFlowResult.Boundary refused = (LearningFlowResult.Boundary) harness.useCase().assistanceDecided(
                consented.interaction().flowId(), consented.interaction().interactionVersion(),
                independentAttemptId, false, UUID.randomUUID());
        LearningFlowInteraction interaction = refused.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(independentAttemptId, interaction.attemptId());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, interaction.attemptPurpose(),
                "refusal must preserve the independent purpose");
        assertNotNull(interaction.learnerProjection(), "refusal returns to the unchanged task boundary");
        assertEquals(LearningStateGraph.ASSISTANCE_REFUSED_MESSAGE, interaction.learnerMessage());
        TaskAttempt attempt = harness.artifacts().findAttempt(independentAttemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, attempt.status());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, attempt.purpose());
        assertTrue(attempt.assistanceTrace().isEmpty(),
                "refusal must not record assistance, convert, or close the attempt");
        assertEquals(0, harness.explainGeneration().calls().size());
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aRetriedAssistanceAcceptanceAfterACommittedConversionHalfResumesTheHelpExposure() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), independentTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                transitioned.interaction().flowId(), transitioned.interaction().interactionVersion(),
                independentAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        // The process crashed after the conversion committed but before its
        // boundary: the attempt is durably Practice with its recorded
        // assistance and the command is unprocessed.
        harness.artifacts().convertToPractice(independentAttemptId, List.of(
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.SUBSTANTIVE_CLARIFICATION,
                        CLOCK.instant()),
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.TEMPORARY_EXPLAIN,
                        CLOCK.instant())));
        assertEquals(AttemptPurpose.PRACTICE,
                harness.artifacts().findAttempt(independentAttemptId).orElseThrow().purpose());
        LearningFlowResult.Boundary resumed = (LearningFlowResult.Boundary) harness.useCase().assistanceDecided(
                consented.interaction().flowId(), consented.interaction().interactionVersion(),
                independentAttemptId, true, UUID.randomUUID());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, resumed.interaction().status());
        assertNotNull(resumed.interaction().teachingProjection(),
                "the retried acceptance must resume the committed conversion half instead of 409ing");
        TaskAttempt attempt = harness.artifacts().findAttempt(independentAttemptId).orElseThrow();
        assertEquals(AttemptPurpose.PRACTICE, attempt.purpose());
        assertEquals(List.of("substantive_clarification", "temporary_explain"),
                attempt.assistanceTraceStrings(),
                "the resumed conversion must never append its trace entries twice");
        assertEquals(1, harness.explainGeneration().calls().size(),
                "the resumed acceptance exposes the freshly generated teaching boundary");
    }

    @Test
    void anAssistanceAcceptanceConvertsTheIndependentAttemptToPracticeBeforeExposingHelp() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), independentTaskJson(),
                        ApplyScriptData.taskReadyJson(
                                "设 w(x) = 3x⁴ − 2x + 1，求 w'(x)。", "12*x^3 - 2"))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment())),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(independentLadderJson())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                transitioned.interaction().flowId(), transitioned.interaction().interactionVersion(),
                independentAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        LearningFlowResult.Boundary converted = (LearningFlowResult.Boundary) harness.useCase().assistanceDecided(
                consented.interaction().flowId(), consented.interaction().interactionVersion(),
                independentAttemptId, true, UUID.randomUUID());
        LearningFlowInteraction interaction = converted.interaction();
        assertEquals(4, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, interaction.stage());
        assertNotNull(interaction.teachingProjection(),
                "the accepted conversion exposes the temporary Explain teaching boundary");
        assertEquals(LearningStateGraph.ASSISTANCE_CONVERTED_MESSAGE, interaction.learnerMessage());
        TaskAttempt attempt = harness.artifacts().findAttempt(independentAttemptId).orElseThrow();
        assertEquals(AttemptPurpose.PRACTICE, attempt.purpose(),
                "acceptance converts the attempt one-way to Practice before any help is exposed");
        assertEquals(AttemptStatus.OPEN, attempt.status());
        assertEquals(List.of("substantive_clarification", "temporary_explain"),
                attempt.assistanceTraceStrings());
        assertEquals(1, harness.explainGeneration().calls().size());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the conversion itself must never create Evidence");
        assertEquals(0, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size(),
                "an Independent conversion never touches the Review cadence");

        LearningFlowResult.Boundary resumed = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
                interaction.flowId(), interaction.interactionVersion(), UUID.randomUUID());
        assertEquals(independentAttemptId, resumed.interaction().attemptId(),
                "Continue returns to the SAME now-Practice attempt");
        LearningFlowResult.Boundary h1 = (LearningFlowResult.Boundary) harness.useCase().requestHint(
                resumed.interaction().flowId(), resumed.interaction().interactionVersion(),
                independentAttemptId, false, UUID.randomUUID());
        assertEquals(1, h1.interaction().hint().level(),
                "the converted attempt accepts hints like any open Apply Practice attempt");
        LearningFlowResult.Boundary readiness = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                h1.interaction().flowId(), h1.interaction().interactionVersion(), UUID.randomUUID(),
                independentAttemptId, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(LearningStage.INDEPENDENT_TEST, readiness.interaction().stage(),
                "a conclusive Practice pass on the converted attempt satisfies readiness");
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, readiness.interaction().attemptPurpose());
        AcceptedLearningEvidence evidence = harness.flowStore().allEvidence().get(0);
        assertEquals(AttemptPurpose.PRACTICE, evidence.attemptPurpose());
        assertEquals(LearningResult.PASS, evidence.result());
        assertEquals(1, evidence.highestHintLevel(),
                "the converted attempt's evidence records the exposed hint plus the recorded assistance");
        assertEquals(List.of("substantive_clarification", "temporary_explain", "H1:orient"),
                evidence.assistanceTrace());
        assertEquals(1, harness.pedagogy().calls().size(),
                "only the Practice-pass decision is a guarded pedagogy decision");
    }

    @Test
    void anAssistedReviewConversionCancelsTheReviewTaskWithoutEvidenceOrMilestoneChangeAndRestartsReviewOne() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(
                                "设 z(x) = 2x³ − x + 5，求 z'(x)。", "6*x^2 - 1"))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment(),
                        conclusivePracticeJudgment(), conclusivePracticeJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        ReviewTask review = harness.flowStore().unfinishedReviewsFor(LEARNER_ID).get(0);
        harness.flowStore().markDueReviewsDue(
                CLOCK.instant().plus(ReviewTaskScheduler.FIRST_REVIEW_DELAY));
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary) harness.reviewStartFlow().start(
                review.reviewId(), UUID.randomUUID());
        assertEquals(AttemptPurpose.REVIEW, reviewBoundary.interaction().attemptPurpose());
        assertEquals(LearningStage.DELAYED_REVIEW, reviewBoundary.interaction().stage());
        UUID reviewAttemptId = reviewBoundary.interaction().attemptId();

        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                reviewBoundary.interaction().flowId(), reviewBoundary.interaction().interactionVersion(),
                reviewAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        assertNotNull(consented.interaction().assistanceConsent());
        LearningFlowResult.Boundary converted = (LearningFlowResult.Boundary) harness.useCase().assistanceDecided(
                consented.interaction().flowId(), consented.interaction().interactionVersion(),
                reviewAttemptId, true, UUID.randomUUID());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, converted.interaction().stage());
        assertNotNull(converted.interaction().teachingProjection());
        TaskAttempt attempt = harness.artifacts().findAttempt(reviewAttemptId).orElseThrow();
        assertEquals(AttemptPurpose.PRACTICE, attempt.purpose(),
                "the accepted review conversion converts the attempt one-way to Practice");
        assertEquals(List.of("substantive_clarification", "temporary_explain"),
                attempt.assistanceTraceStrings());
        assertEquals(ReviewTaskStatus.CANCELLED,
                harness.flowStore().findReview(review.reviewId()).orElseThrow().status(),
                "the started Review Task is cancelled by the conversion");
        assertTrue(harness.flowStore().unfinishedReviewsFor(LEARNER_ID).isEmpty(),
                "the cancelled Review leaves no unfinished Review work");
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the conversion creates no Review Evidence");
        assertEquals(AttemptPurpose.INDEPENDENT_TEST,
                harness.flowStore().allEvidence().get(0).attemptPurpose(),
                "only the earlier Independent pass evidence exists");
        ConceptProgress progress =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "the conversion leaves the milestones unchanged");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());

        LearningFlowResult.Boundary resumed = (LearningFlowResult.Boundary) harness.useCase().continueRequested(
                converted.interaction().flowId(), converted.interaction().interactionVersion(), UUID.randomUUID());
        assertEquals(reviewAttemptId, resumed.interaction().attemptId(),
                "Continue returns to the same converted Practice attempt");
        LearningFlowResult.Boundary freshIndependent = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                resumed.interaction().flowId(), resumed.interaction().interactionVersion(), UUID.randomUUID(),
                reviewAttemptId, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION,
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(LearningStage.INDEPENDENT_TEST, freshIndependent.interaction().stage(),
                "the converted review attempt's Practice pass satisfies readiness");
        LearningFlowResult.Boundary rejoined = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                freshIndependent.interaction().flowId(), freshIndependent.interaction().interactionVersion(),
                UUID.randomUUID(), freshIndependent.interaction().attemptId(),
                "6*x^2 - 1", "6*x^2 - 1", null);
        assertEquals(3, harness.flowStore().allEvidence().size(),
                "the converted review creates no evidence, and the new Independent pass is the third item");
        List<ReviewTask> restarted = harness.flowStore().unfinishedReviewsFor(LEARNER_ID);
        assertEquals(1, restarted.size(),
                "the later Independent pass restarts the cadence with a single Review");
        assertEquals(1, restarted.get(0).reviewNumber(),
                "the restarted cadence begins again at Review 1");
        assertEquals(ReviewTaskStatus.SCHEDULED, restarted.get(0).status());
        ConceptProgress rejoinedProgress =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.INDEPENDENT, rejoinedProgress.currentMilestone());
    }

    @Test
    void anAssistanceRefusalLeavesTheStartedReviewTaskUnchanged() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        ReviewTask review = harness.flowStore().unfinishedReviewsFor(LEARNER_ID).get(0);
        harness.flowStore().markDueReviewsDue(
                CLOCK.instant().plus(ReviewTaskScheduler.FIRST_REVIEW_DELAY));
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary) harness.reviewStartFlow().start(
                review.reviewId(), UUID.randomUUID());
        UUID reviewAttemptId = reviewBoundary.interaction().attemptId();
        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                reviewBoundary.interaction().flowId(), reviewBoundary.interaction().interactionVersion(),
                reviewAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        LearningFlowResult.Boundary refused = (LearningFlowResult.Boundary) harness.useCase().assistanceDecided(
                consented.interaction().flowId(), consented.interaction().interactionVersion(),
                reviewAttemptId, false, UUID.randomUUID());
        assertEquals(AttemptPurpose.REVIEW, refused.interaction().attemptPurpose(),
                "refusal must preserve the Review purpose");
        assertEquals(LearningStage.DELAYED_REVIEW, refused.interaction().stage());
        assertNotNull(refused.interaction().learnerProjection());
        TaskAttempt attempt = harness.artifacts().findAttempt(reviewAttemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, attempt.status());
        assertEquals(AttemptPurpose.REVIEW, attempt.purpose());
        assertTrue(attempt.assistanceTrace().isEmpty());
        assertEquals(ReviewTaskStatus.STARTED,
                harness.flowStore().findReview(review.reviewId()).orElseThrow().status(),
                "refusal must leave the started Review Task untouched");
        assertEquals(1, harness.flowStore().unfinishedReviewsFor(LEARNER_ID).size());
        assertEquals(1, harness.flowStore().allEvidence().size());
        assertEquals(0, harness.explainGeneration().calls().size(),
                "refusal must expose no teaching content");
    }

    @Test
    void aFlowControlOnAnOpenAttemptAbandonsItWithoutAssessmentOrEvidenceAndReplaysTheLeaveTransition() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        assertEquals(InteractionKind.TASK, started.interaction().kind(),
                "an open task boundary is the task union member");

        UUID leaveKey = UUID.randomUUID();
        LearningFlowResult.Boundary left = (LearningFlowResult.Boundary) harness.useCase().flowControlRequested(
                started.interaction().flowId(), 1, leaveKey);
        LearningFlowInteraction interaction = left.interaction();
        assertEquals(2, interaction.interactionVersion());
        assertEquals(InteractionKind.TRANSITION, interaction.kind(),
                "the explicit leave must project the transition union member");
        assertEquals(FlowStatus.TERMINAL, interaction.status());
        assertEquals(LearningStateGraph.FLOW_LEAVE_MESSAGE, interaction.learnerMessage());
        assertNull(interaction.learnerProjection());
        assertNull(interaction.teachingProjection());
        assertNull(interaction.assistanceConsent());
        TaskAttempt abandoned = harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow();
        assertEquals(AttemptStatus.ABANDONED, abandoned.status(),
                "ADR-0015: an explicit leave closes the open attempt as Abandoned");
        assertNull(abandoned.submission(), "an abandoned attempt carries no submission");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "an abandoned attempt creates no Learning Evidence");
        assertEquals(0, responseAssessmentsFor(harness, diagnosticAttemptId).size(),
                "an abandoned attempt is never assessed");

        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().flowControlRequested(
                started.interaction().flowId(), 1, leaveKey);
        assertEquals(left.interaction(), replay.interaction(),
                "a replayed leave key must return the original committed transition");
        assertEquals(2, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion(),
                "a replayed leave must not advance the flow again");

        LearningFlowResult later = harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), diagnosticAttemptId,
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertTrue(later instanceof LearningFlowResult.SubmissionIgnored,
                "a submission against the abandoned attempt must never be evaluated");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aFlowControlOnATeachingBoundaryLeavesTheFlowWithoutTouchingState() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(), practiceTaskJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(diagnosticFailJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(InteractionKind.TEACHING, explained.interaction().kind(),
                "the Explain boundary is the teaching union member");
        assertNull(explained.interaction().attemptId(), "a teaching boundary holds no Attempt");

        LearningFlowResult.Boundary left = (LearningFlowResult.Boundary) harness.useCase().flowControlRequested(
                started.interaction().flowId(), 2, UUID.randomUUID());
        assertEquals(InteractionKind.TRANSITION, left.interaction().kind());
        assertEquals(LearningStateGraph.FLOW_LEAVE_MESSAGE, left.interaction().learnerMessage());
        assertEquals(3, left.interaction().interactionVersion());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "leaving a teaching boundary must never create Evidence");
        assertEquals(3, harness.flowStore().latestCheckpoint(started.interaction().flowId())
                        .orElseThrow().interactionVersion(),
                "the leave boundary must be a committed checkpoint");
    }

    @Test
    void aFlowControlOnAnOpenIndependentAttemptCreatesNoEvidenceAndPreservesTheMilestones() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = transitioned.interaction().attemptId();
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, transitioned.interaction().attemptPurpose());

        LearningFlowResult.Boundary left = (LearningFlowResult.Boundary) harness.useCase().flowControlRequested(
                started.interaction().flowId(), 2, UUID.randomUUID());
        assertEquals(InteractionKind.TRANSITION, left.interaction().kind());
        TaskAttempt abandoned = harness.artifacts().findAttempt(independentAttemptId).orElseThrow();
        assertEquals(AttemptStatus.ABANDONED, abandoned.status());
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "an abandoned Independent attempt must not create Independent Evidence");
        ConceptProgress progress =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.UNASSESSED, progress.currentMilestone(),
                "abandoning an open attempt must not change any Milestone");
        assertEquals(MasteryMilestone.UNASSESSED, progress.highestMilestoneReached());
    }

    @Test
    void aFlowControlDuringAStartedReviewAbandonsOnlyTheAttemptUntilExplicitCancellation() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment(), conclusivePracticeJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the Independent pass accepts exactly one Evidence before the leave");
        ReviewTask review = harness.flowStore().unfinishedReviewsFor(LEARNER_ID).get(0);
        harness.flowStore().markDueReviewsDue(
                CLOCK.instant().plus(ReviewTaskScheduler.FIRST_REVIEW_DELAY));
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary) harness.reviewStartFlow().start(
                review.reviewId(), UUID.randomUUID());
        UUID reviewAttemptId = reviewBoundary.interaction().attemptId();
        assertEquals(InteractionKind.TASK, reviewBoundary.interaction().kind());

        LearningFlowResult.Boundary left = (LearningFlowResult.Boundary) harness.useCase().flowControlRequested(
                started.interaction().flowId(), reviewBoundary.interaction().interactionVersion(),
                UUID.randomUUID());
        assertEquals(InteractionKind.TRANSITION, left.interaction().kind());
        assertEquals(LearningStateGraph.FLOW_LEAVE_MESSAGE, left.interaction().learnerMessage());
        TaskAttempt abandoned = harness.artifacts().findAttempt(reviewAttemptId).orElseThrow();
        assertEquals(AttemptStatus.ABANDONED, abandoned.status(),
                "leaving a started Review abandons its open Attempt");
        assertEquals(ReviewTaskStatus.STARTED,
                harness.flowStore().findReview(review.reviewId()).orElseThrow().status(),
                "Flow Control must not cancel a Started Review");
        ReviewCancellationResult cancelled = new ReviewCancellationUseCase(
                harness.flowStore(), harness.flowStore(), CLOCK).cancel(review.reviewId(), UUID.randomUUID());
        assertEquals(ReviewTaskStatus.CANCELLED, cancelled.reviewTask().status());
        assertTrue(harness.flowStore().unfinishedReviewsFor(LEARNER_ID).isEmpty());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "leaving a Review must create no Review Evidence");
        ConceptProgress progress =
                new ConceptProgressProjector().projectFor(harness.flowStore(), LEARNER_ID, CONCEPT_ID);
        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "leaving must preserve the accepted Independent milestone");
        assertEquals(MasteryMilestone.INDEPENDENT, progress.highestMilestoneReached());
    }

    @Test
    void theAssistanceConsentBoundaryCarriesTheConsentUnionMember() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                started.interaction().flowId(), 2, transitioned.interaction().attemptId(),
                "为什么幂法则适用？", UUID.randomUUID());
        assertEquals(InteractionKind.ASSISTANCE_CONSENT, consented.interaction().kind(),
                "the assistance-consent warning is the consent union member");
        assertNotNull(consented.interaction().assistanceConsent());
        assertNull(consented.interaction().learnerProjection());
        assertNull(consented.interaction().teachingProjection());
    }

    @Test
    void aStartedReviewSubmissionAdvancesTheCadenceThroughTheClosedCommandSurface() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment(), conclusivePracticeJudgment(),
                        conclusivePracticeJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        ReviewTask review = harness.flowStore().unfinishedReviewsFor(LEARNER_ID).get(0);
        harness.flowStore().markDueReviewsDue(
                CLOCK.instant().plus(ReviewTaskScheduler.FIRST_REVIEW_DELAY));
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary) harness.reviewStartFlow().start(
                review.reviewId(), UUID.randomUUID());
        assertEquals(AttemptPurpose.REVIEW, reviewBoundary.interaction().attemptPurpose());

        UUID reviewKey = UUID.randomUUID();
        LearningFlowResult.Boundary reviewDone = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), reviewBoundary.interaction().interactionVersion(), reviewKey,
                reviewBoundary.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(FlowStatus.TERMINAL, reviewDone.interaction().status());
        assertEquals(InteractionKind.TRANSITION, reviewDone.interaction().kind());
        assertTrue(reviewDone.interaction().learnerMessage().contains("复习已完成"));
        assertEquals(2, harness.flowStore().allEvidence().size(),
                "the Review pass accepts exactly one Review Evidence record");
        List<ReviewTask> remaining = harness.flowStore().unfinishedReviewsFor(LEARNER_ID);
        assertEquals(1, remaining.size(), "the cadence advances to exactly one successor");
        assertEquals(2, remaining.get(0).reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, remaining.get(0).status());

        LearningFlowResult.Boundary replayed = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), reviewBoundary.interaction().interactionVersion(), reviewKey,
                reviewBoundary.interaction().attemptId(),
                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, null);
        assertEquals(reviewDone.interaction(), replayed.interaction(),
                "a replayed Review submission key must return the original committed interaction");
        assertEquals(2, harness.flowStore().allEvidence().size(),
                "a replayed Review submission must never stack Evidence");
    }

    @Test
    void aCrashBetweenClosingAndCommittingAReviewSubmissionResumesFromTheSavedAttempt() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(conclusivePracticeJudgment(), conclusivePracticeJudgment(),
                        conclusivePracticeJudgment())));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        harness.useCase().submitAnswer(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        ReviewTask review = harness.flowStore().unfinishedReviewsFor(LEARNER_ID).get(0);
        harness.flowStore().markDueReviewsDue(
                CLOCK.instant().plus(ReviewTaskScheduler.FIRST_REVIEW_DELAY));
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary) harness.reviewStartFlow().start(
                review.reviewId(), UUID.randomUUID());
        UUID reviewAttemptId = reviewBoundary.interaction().attemptId();
        harness.artifacts().closeAttempt(reviewAttemptId,
                new TaskSubmission(
                        new MathematicalAnswer(ApplyScriptData.REVIEW_EXPECTED_EXPRESSION,
                                ApplyScriptData.REVIEW_EXPECTED_EXPRESSION, AnswerInputFamily.PLAIN_TEXT),
                        null, CLOCK.instant()));
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "the crash must leave the closed Review Attempt without Review Evidence");
        UUID retryKey = UUID.randomUUID();
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), reviewBoundary.interaction().interactionVersion(),
                retryKey, reviewAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(FlowStatus.TERMINAL, recovered.interaction().status());
        assertEquals(2, harness.flowStore().allEvidence().size(),
                "the retry must resume the evaluation of the saved Review submission exactly once");
        assertEquals(ApplyScriptData.REVIEW_EXPECTED_EXPRESSION,
                harness.artifacts().findAttempt(reviewAttemptId).orElseThrow()
                        .submission().finalDerivative().confirmedCanonical(),
                "the retry body must not replace the saved Review submission");
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), reviewBoundary.interaction().interactionVersion(),
                retryKey, reviewAttemptId,
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);
        assertEquals(recovered.interaction(), replay.interaction());
        assertEquals(2, harness.flowStore().allEvidence().size());
    }

    @Test
    void aProceduralClarificationOnTheDiagnosticAttemptRestatesTheDisplayedContractAndRecordsIt() {
        Harness harness = harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED))),
                new ScriptedResponseVerificationModel(List.of()),
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())),
                new ScriptedTeachBackGenerationModel(List.of(TeachBackScriptData.taskReadyJson())),
                new ScriptedTeachBackAssessmentModel(List.of(TeachBackScriptData.passAssessment())),
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict(), passVerdict())),
                new ScriptedPedagogyModel(),
                new ScriptedClarificationClassifier(List.of(ClarificationClassification.PROCEDURAL)));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        LearningFlowResult.Boundary answered = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                started.interaction().flowId(), 1, diagnosticAttemptId,
                "这道题是只填最终导数吗？", UUID.randomUUID());
        LearningFlowInteraction interaction = answered.interaction();
        assertEquals(2, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(diagnosticAttemptId, interaction.attemptId(),
                "the procedural answer returns to the SAME open Diagnostic task boundary");
        assertEquals(AttemptPurpose.DIAGNOSTIC, interaction.attemptPurpose(),
                "the Diagnostic purpose is never changed");
        assertNotNull(interaction.learnerProjection(), "the task projection is re-shown");
        assertTrue(interaction.learnerMessage().startsWith("答题说明"),
                "the procedural answer restates the package's own format contract");
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow().status(),
                "the procedural answer never closes the Attempt");
        assertEquals(List.of("procedural_clarification"),
                harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow().assistanceTraceStrings(),
                "the procedural clarification leaves an auditable assistance record");
        assertEquals(1, harness.classifier().calls().size());
        assertEquals(0, harness.explainGeneration().calls().size(),
                "a procedural Diagnostic clarification must never load a Teaching Node Profile");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aSubstantiveClarificationOnTheDiagnosticAttemptAddsNoTeachingAndChangesNothing() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        LearningFlowResult.Boundary refused = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                started.interaction().flowId(), 1, diagnosticAttemptId,
                "为什么幂法则适用？", UUID.randomUUID());
        LearningFlowInteraction interaction = refused.interaction();
        assertEquals(2, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(diagnosticAttemptId, interaction.attemptId(),
                "the refusal returns to the SAME open Diagnostic task boundary");
        assertEquals(AttemptPurpose.DIAGNOSTIC, interaction.attemptPurpose(),
                "the refusal never converts the Attempt purpose");
        assertEquals(LearningStateGraph.TASK_CLARIFICATION_NOT_OFFERED_MESSAGE, interaction.learnerMessage());
        TaskAttempt attempt = harness.artifacts().findAttempt(diagnosticAttemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, attempt.status());
        assertTrue(attempt.assistanceTrace().isEmpty(),
                "a refused substantive clarification records no assistance");
        assertEquals(1, harness.classifier().calls().size());
        assertEquals(0, harness.explainGeneration().calls().size(),
                "a substantive Diagnostic clarification adds no teaching content");
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "the refusal creates no Evidence and does not change evidence eligibility");
    }

    @Test
    void aProceduralClarificationOnTheTeachBackAttemptRestatesTheDisplayedContractAndRecordsIt() {
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
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict())),
                new ScriptedPedagogyModel(),
                new ScriptedClarificationClassifier(List.of(ClarificationClassification.PROCEDURAL)));
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        LearningFlowResult.Boundary answered = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                teachBackAttemptId, "这里只需要简短文字解释吗？", UUID.randomUUID());
        LearningFlowInteraction interaction = answered.interaction();
        assertEquals(teachBack.interaction().interactionVersion() + 1, interaction.interactionVersion());
        assertEquals(teachBackAttemptId, interaction.attemptId(),
                "the procedural answer returns to the SAME open Teach-back task boundary");
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose(),
                "the Teach-back purpose is never changed");
        assertNotNull(interaction.learnerProjection(), "the Teach-back task projection is re-shown");
        assertTrue(interaction.learnerMessage().startsWith("答题说明"),
                "the procedural answer restates the displayed answer contract");
        assertEquals(List.of("procedural_clarification"),
                harness.artifacts().findAttempt(teachBackAttemptId).orElseThrow().assistanceTraceStrings(),
                "the procedural clarification leaves an auditable assistance record");
        assertEquals(1, harness.classifier().calls().size());
        assertEquals(1, harness.explainGeneration().calls().size(),
                "a procedural Teach-back clarification must never load a Teaching Node Profile");
        assertEquals(1, harness.teachBackGeneration().calls().size(),
                "the procedural clarification never regenerates the Teach-back task");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aSubstantiveClarificationOnTheTeachBackAttemptAddsNoTeachingAndChangesNothing() {
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
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict())),
                new ScriptedPedagogyModel(),
                new ScriptedClarificationClassifier());
        LearningFlowResult.Boundary teachBack = reachTeachBackBoundary(harness);
        UUID teachBackAttemptId = teachBack.interaction().attemptId();
        LearningFlowResult.Boundary refused = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                teachBack.interaction().flowId(), teachBack.interaction().interactionVersion(),
                teachBackAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        LearningFlowInteraction interaction = refused.interaction();
        assertEquals(teachBack.interaction().interactionVersion() + 1, interaction.interactionVersion());
        assertEquals(teachBackAttemptId, interaction.attemptId(),
                "the refusal returns to the SAME open Teach-back task boundary");
        assertEquals(AttemptPurpose.PRACTICE, interaction.attemptPurpose(),
                "the refusal never changes the Teach-back purpose");
        assertEquals(LearningStateGraph.TASK_CLARIFICATION_NOT_OFFERED_MESSAGE, interaction.learnerMessage());
        TaskAttempt attempt = harness.artifacts().findAttempt(teachBackAttemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, attempt.status());
        assertEquals(List.of(), attempt.assistanceTraceStrings(),
                "a refused substantive clarification records no assistance");
        assertEquals(1, harness.classifier().calls().size());
        assertEquals(1, harness.explainGeneration().calls().size(),
                "a substantive Teach-back clarification adds no teaching content");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aProceduralClarificationOnTheStandaloneExplainAnswersWithoutAnAttemptId() {
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
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict())),
                new ScriptedPedagogyModel(),
                new ScriptedClarificationClassifier(List.of(ClarificationClassification.PROCEDURAL)));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        assertEquals(InteractionKind.TEACHING, explained.interaction().kind(),
                "the Diagnostic failure opens the standalone Explain teaching boundary");
        TeachingProjection teaching = explained.interaction().teachingProjection();
        assertNull(explained.interaction().attemptId(), "the standalone Explain has no Attempt");

        LearningFlowResult.Boundary answered = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                explained.interaction().flowId(), explained.interaction().interactionVersion(),
                null, "这个示例的记号怎么读？", UUID.randomUUID());
        LearningFlowInteraction interaction = answered.interaction();
        assertEquals(3, interaction.interactionVersion());
        assertEquals(InteractionKind.TEACHING, interaction.kind(),
                "the procedural answer keeps the teaching interaction");
        assertNull(interaction.attemptId(), "Explain clarification carries no Attempt ID");
        assertEquals(teaching, interaction.teachingProjection(),
                "the SAME teaching content is re-shown, no new Explain is generated");
        assertTrue(interaction.learnerMessage().startsWith("讲解说明"),
                "the procedural answer restates the displayed teaching conditions");
        assertEquals(1, harness.classifier().calls().size());
        assertEquals(1, harness.explainGeneration().calls().size(),
                "a procedural Explain clarification must never load a Teaching Node Profile");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aSubstantiveClarificationOnTheStandaloneExplainAddsNoTeachingAndLeavesTheTeachingBoundaryUnchanged() {
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
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict())),
                new ScriptedPedagogyModel(),
                new ScriptedClarificationClassifier());
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        TeachingProjection teaching = explained.interaction().teachingProjection();
        LearningFlowResult.Boundary refused = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                explained.interaction().flowId(), explained.interaction().interactionVersion(),
                null, "为什么幂法则适用？", UUID.randomUUID());
        LearningFlowInteraction interaction = refused.interaction();
        assertEquals(3, interaction.interactionVersion());
        assertEquals(InteractionKind.TEACHING, interaction.kind(),
                "the substantive refusal keeps the teaching interaction");
        assertNull(interaction.attemptId());
        assertEquals(LearningStateGraph.TEACHING_CLARIFICATION_NOT_OFFERED_MESSAGE, interaction.learnerMessage());
        assertEquals(teaching, interaction.teachingProjection(),
                "the teaching boundary is unchanged");
        assertEquals(1, harness.classifier().calls().size());
        assertEquals(1, harness.explainGeneration().calls().size(),
                "a substantive Explain clarification adds no teaching content");
        assertEquals(1, harness.artifacts().allPackages().size(),
                "the Explain clarification never opens a Task Package or Attempt");
        assertTrue(harness.flowStore().allEvidence().isEmpty());
    }

    @Test
    void aReplayedExplainClarificationReturnsTheOriginalBoundaryWithoutReclassifying() {
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
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict())),
                new ScriptedPedagogyModel(),
                new ScriptedClarificationClassifier(List.of(ClarificationClassification.PROCEDURAL)));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        UUID key = UUID.randomUUID();
        LearningFlowResult.Boundary first = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                explained.interaction().flowId(), explained.interaction().interactionVersion(),
                null, "这个示例的记号怎么读？", key);
        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                explained.interaction().flowId(), explained.interaction().interactionVersion(),
                null, "这个示例的记号怎么读？", key);
        assertEquals(first.interaction(), replay.interaction());
        assertEquals(1, harness.classifier().calls().size(),
                "a replayed Explain clarification must never classify again");
        assertEquals(1, harness.explainGeneration().calls().size(),
                "a replayed Explain clarification must never regenerate teaching content");
    }

    @Test
    void anInvalidClarificationClassificationOnTheStandaloneExplainFallsBackToUncertainRefusal() {
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
                new ScriptedTeachBackTaskVerifier(List.of(passVerdict())),
                new ScriptedPedagogyModel(),
                ScriptedClarificationClassifier.replies(Optional.empty()));
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        TeachingProjection teaching = explained.interaction().teachingProjection();
        LearningFlowResult.Boundary refused = (LearningFlowResult.Boundary) harness.useCase().clarificationAsked(
                explained.interaction().flowId(), explained.interaction().interactionVersion(),
                null, "为什么幂法则适用？", UUID.randomUUID());
        assertEquals(InteractionKind.TEACHING, refused.interaction().kind());
        assertEquals(LearningStateGraph.TEACHING_CLARIFICATION_NOT_OFFERED_MESSAGE,
                refused.interaction().learnerMessage());
        assertEquals(teaching, refused.interaction().teachingProjection());
        List<ModelContractAudit> audits = harness.artifacts().allContractAudits();
        assertEquals(1, audits.size());
        assertEquals(ModelContractAudit.CLARIFICATION, audits.get(0).responsibility());
        assertEquals(0, audits.get(0).repairCount());
        assertNull(audits.get(0).attemptId(),
                "an Explain clarification audit carries no Attempt identity");
        assertNull(audits.get(0).taskPackageId());
        assertEquals(List.of("invalid_enum"), audits.get(0).violationCodes());
        assertEquals(1, harness.explainGeneration().calls().size(),
                "the uncertain fallback adds no teaching content");
    }

    @Test
    void clarificationAndAssistanceCommandsAreIgnoredForWrongOrClosedAttempts() {
        Harness harness = harness();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID diagnosticAttemptId = started.interaction().attemptId();
        // Diagnostic now accepts procedural clarification only; a substantive
        // request is refused on the same boundary without teaching content,
        // purpose change, or evidence.
        LearningFlowResult.Boundary refused = (LearningFlowResult.Boundary) harness.useCase()
                .clarificationAsked(started.interaction().flowId(), 1, diagnosticAttemptId,
                        "这是什么题？", UUID.randomUUID());
        assertEquals(LearningStateGraph.TASK_CLARIFICATION_NOT_OFFERED_MESSAGE,
                refused.interaction().learnerMessage(),
                "a substantive Diagnostic clarification is refused, never answered");
        assertEquals(AttemptPurpose.DIAGNOSTIC, refused.interaction().attemptPurpose(),
                "the refusal never converts the Diagnostic purpose");
        assertEquals(0, harness.explainGeneration().calls().size(),
                "a Diagnostic refusal adds no teaching content");
        LearningFlowResult.AssistanceIgnored diagnosticAssist = (LearningFlowResult.AssistanceIgnored) harness.useCase()
                .assistanceDecided(started.interaction().flowId(), 2, diagnosticAttemptId, true,
                        UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE, diagnosticAssist.reason(),
                "assistance_decided remains legal only over an Independent or Review consent");
        assertEquals(1, harness.classifier().calls().size(),
                "the Diagnostic clarification is classified exactly once");
        LearningFlowResult.ClarificationIgnored unknown = (LearningFlowResult.ClarificationIgnored) harness.useCase()
                .clarificationAsked(started.interaction().flowId(), 2, UUID.randomUUID(),
                        "这是什么题？", UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND, unknown.reason());
        LearningFlowResult.AssistanceIgnored unknownAssist = (LearningFlowResult.AssistanceIgnored) harness.useCase()
                .assistanceDecided(started.interaction().flowId(), 2, UUID.randomUUID(), false,
                        UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND, unknownAssist.reason());
        harness.useCase().submitAnswer(started.interaction().flowId(), 2, UUID.randomUUID(),
                diagnosticAttemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        LearningFlowResult.ClarificationIgnored closed = (LearningFlowResult.ClarificationIgnored) harness.useCase()
                .clarificationAsked(started.interaction().flowId(), 3, diagnosticAttemptId,
                        "这是什么题？", UUID.randomUUID());
        assertEquals(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION, closed.reason(),
                "an Attempt replaced by a later Interaction cannot be routed again");
        assertEquals(3, harness.flowStore().latestInteraction(started.interaction().flowId())
                .orElseThrow().interactionVersion(),
                "the ignored commands never advance the interaction");
    }

    private static String invalidLadderJson() {
        return HintScriptData.ladderReadyJson(
                HintScriptData.H4_SCAFFOLD, HintScriptData.H5_LEARNER_CONTENT,
                HintScriptData.H5_STEPS, "6*x^2-4");
    }

    /**
     * A ladder whose H5 proposed answer matches the converted Independent
     * task's expected answer {@code 15*x^2 - 2}, so the deterministic H5
     * equivalence check passes and the converted attempt accepts hints.
     */
    private static String independentLadderJson() {
        return HintScriptData.ladderReadyJson(
                "对 5x³ 求导得 15x²，对 −2x 求导得 −2，常数 1 的导数为 0。",
                "完整解答：g'(x) = 3·5x² − 2 = 15x² − 2。",
                new String[]{
                        "使用幂法则：d/dx (5x³) = 15x²",
                        "使用常数倍法则：d/dx (−2x) = −2",
                        "常数法则：d/dx (1) = 0",
                        "合并各项：g'(x) = 15x² − 2"
                },
                "15*x^2-2");
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
        return executor.deliver(ScriptedModelProfile.PROFILE, ReviewApplyFixture.reviewContext());
    }
    /**
     * Runs the Diagnostic fail through its fresh verified Practice boundary,
     * the deterministic remediation entry of the Learning StateGraph.
     */

    private static LearningFlowResult.Boundary reachPracticeBoundary(Harness harness) {
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) harness.useCase().submitAnswer(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, "我猜的");
        return (LearningFlowResult.Boundary) harness.useCase().continueRequested(
                explained.interaction().flowId(), explained.interaction().interactionVersion(), UUID.randomUUID());
    }

    /**
     * Runs the Diagnostic fail through the Explain teaching boundary, the
     * fresh Practice boundary, and an H5 answer reveal, so the learner stands
     * at the anchored Teach-back task boundary.
     */
    private static LearningFlowResult.Boundary reachTeachBackBoundary(Harness harness) {
        LearningFlowResult.Boundary practice = reachPracticeBoundary(harness);
        return (LearningFlowResult.Boundary) harness.useCase().requestHint(
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

    private static List<ResponseAssessment> responseAssessmentsFor(Harness harness, UUID attemptId) {
        return harness.artifacts().committedEvaluationResultsFor(attemptId).stream()
                .filter(result -> result.responsibility().equals(CommittedEvaluationResult.RESPONSE_ASSESSMENT)
                        || result.responsibility().equals(CommittedEvaluationResult.RESPONSE_VERIFICATION))
                .map(result -> ResponseAssessment.parse(result.resultPayload()))
                .toList();
    }

    private static List<TeachBackAssessment> committedTeachBackAssessmentsFor(Harness harness, UUID attemptId) {
        return harness.artifacts().committedEvaluationResultsFor(attemptId).stream()
                .filter(result -> result.responsibility().equals(CommittedEvaluationResult.TEACH_BACK_ASSESSMENT))
                .map(result -> TeachBackAssessment.parse(result.resultPayload()))
                .toList();
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
            ResponseVerificationPort verification
    ) {
        return harness(generation, verifier, assessment, verification,
                new ScriptedExplainGenerationModel(List.of(ExplainScriptData.explainReadyJson())),
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ResponseVerificationPort verification,
            ScriptedExplainGenerationModel explainGeneration
    ) {
        return harness(generation, verifier, assessment, verification, explainGeneration,
                new ScriptedHintGenerationModel(List.of(HintScriptData.ladderReadyJson())));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ResponseVerificationPort verification,
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
            ResponseVerificationPort verification,
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
            ResponseVerificationPort verification,
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
            ResponseVerificationPort verification,
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
            ResponseVerificationPort verification,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedHintGenerationModel hintGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment,
            ScriptedTeachBackTaskVerifier teachBackVerifier,
            ScriptedPedagogyModel pedagogy
    ) {
        return harness(CLOCK, generation, verifier, assessment, verification, explainGeneration,
                hintGeneration, teachBackGeneration, teachBackAssessment, teachBackVerifier, pedagogy);
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ResponseVerificationPort verification,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedHintGenerationModel hintGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment,
            ScriptedTeachBackTaskVerifier teachBackVerifier,
            ScriptedPedagogyModel pedagogy,
            ScriptedClarificationClassifier classifier
    ) {
        return harness(CLOCK, generation, verifier, assessment, verification, explainGeneration,
                hintGeneration, teachBackGeneration, teachBackAssessment, teachBackVerifier, pedagogy,
                classifier);
    }

    private Harness harness(
            Clock clock,
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ResponseVerificationPort verification,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedHintGenerationModel hintGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment,
            ScriptedTeachBackTaskVerifier teachBackVerifier,
            ScriptedPedagogyModel pedagogy
    ) {
        return harness(clock, generation, verifier, assessment, verification, explainGeneration,
                hintGeneration, teachBackGeneration, teachBackAssessment, teachBackVerifier, pedagogy,
                new ScriptedClarificationClassifier());
    }

    private Harness harness(
            Clock clock,
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ResponseVerificationPort verification,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedHintGenerationModel hintGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment,
            ScriptedTeachBackTaskVerifier teachBackVerifier,
            ScriptedPedagogyModel pedagogy,
            ScriptedClarificationClassifier classifier
    ) {
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(clock);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(clock, artifacts);
        ReviewTaskScheduler reviewScheduler = new ReviewTaskScheduler(flowStore);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(ReferenceBundles.stack(), generation, verifier, artifacts);
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, artifacts, flowStore, assessment, verification,
                DiagnosticApplyFixture.diagnosticContext(), IndependentApplyFixture.independentContext(), clock);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                artifacts, flowStore, assessment, verification, reviewScheduler, clock);
        PracticeSubmissionFlow practiceFlow = new PracticeSubmissionFlow(
                executor, artifacts, flowStore, assessment, verification,
                PracticeApplyFixture.practiceContext(), IndependentApplyFixture.independentContext(), clock);
        ExplainFlow explainFlow = new ExplainFlow(
                new ExplainProfileExecutor(ReferenceBundles.explainStack(), explainGeneration),
                artifacts, flowStore, ExplainApplyFixture.explainContext());
        HintFlow hintFlow = new HintFlow(
                hintGeneration, artifacts, PracticeApplyFixture.practiceContext().conceptSourcePack());
        TeachBackFlow teachBackFlow = new TeachBackFlow(
                new TeachBackProfileExecutor(
                        ReferenceBundles.teachBackStack(), teachBackGeneration, teachBackVerifier, artifacts),
                artifacts, flowStore, teachBackAssessment,
                TeachBackApplyFixture.teachBackContext(), clock);
        ReviewStartFlow reviewStartFlow = new ReviewStartFlow(
                executor, flowStore, flowStore, ReviewApplyFixture.reviewContext(), clock);
        ReviewSubmissionFlow reviewSubmissionFlow = new ReviewSubmissionFlow(
                artifacts, flowStore, assessment, verification, reviewScheduler, executor, flowStore,
                ReviewApplyFixture.reviewContext(), clock);
        LearningStateGraph graph = new LearningStateGraph(
                artifacts, flowStore, flowStore, diagnosticFlow, independentFlow, practiceFlow,
                reviewSubmissionFlow, explainFlow, hintFlow, teachBackFlow, pedagogy, classifier, clock);
        OperatorModelProfilePort profilePort = () -> ScriptedModelProfile.PROFILE;
        LearningFlowCommandUseCase useCase = new LearningFlowCommandUseCase(
                flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), profilePort);
        return new Harness(artifacts, flowStore, generation, hintGeneration, useCase, graph,
                assessment, explainGeneration, teachBackGeneration, teachBackAssessment, teachBackFlow, pedagogy,
                reviewStartFlow, classifier);
    }

    private record Harness(
            InMemoryArtifactStore artifacts,
            InMemoryLearningFlowStore flowStore,
            ScriptedApplyGenerationModel generation,
            ScriptedHintGenerationModel hintGeneration,
            LearningFlowCommandUseCase useCase,
            LearningStateGraph graph,
            ScriptedAssessmentModel assessment,
            ScriptedExplainGenerationModel explainGeneration,
            ScriptedTeachBackGenerationModel teachBackGeneration,
            ScriptedTeachBackAssessmentModel teachBackAssessment,
            TeachBackFlow teachBackFlow,
            ScriptedPedagogyModel pedagogy,
            ReviewStartFlow reviewStartFlow,
            ScriptedClarificationClassifier classifier
    ) {
        LearningFlowCommandUseCase newUseCase() {
            return new LearningFlowCommandUseCase(
                    flowStore, graph, DiagnosticApplyFixture.diagnosticContext(),
                    () -> ScriptedModelProfile.PROFILE);
        }
    }

    private static final class FailsOnceThenPassesVerification implements ResponseVerificationPort {
        private int calls;

        @Override
        public ResponseAssessment verify(ModelProfile profile, cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext context) {
            calls++;
            if (calls == 1) {
                throw new IllegalStateException("simulated crash after the Assessment checkpoint");
            }
            return ApplyScriptData.responseAssessment(
                    FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED);
        }
    }
}
