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
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionRejectionReason;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyFlowUseCaseTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
    private static final UUID LEARNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Test
    void startPersistsFlowSourcePackageAttemptExposureInteractionCheckpointAndCommand() {
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
        assertEquals(3, interaction.learnerProjection().allowedEvents().size());
        assertFalse(interaction.learnerProjection().taskText().contains("12*x^2 - 6*x + 7"),
                "expected answers must never reach the learner");
        assertFalse(interaction.learnerProjection().taskText().contains("openstax"));

        LearningFlowStore.FlowRecord flow = harness.flowStore().findFlow(interaction.flowId()).orElseThrow();
        assertEquals(LEARNER_ID, flow.learnerId());
        assertEquals(DiagnosticApplyFixture.CONCEPT_ID, flow.conceptId());

        assertEquals(1, harness.artifacts().allPackages().size(),
                "the delivered package must be persisted");
        assertEquals(1, harness.artifacts().findAttempt(interaction.attemptId()).map(a -> a.status()).stream()
                .filter(status -> status == AttemptStatus.OPEN).count());
        assertEquals(1, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size(),
                "the displayed task fingerprint must be recorded");
        assertEquals(1, harness.flowStore().exposedSolutionFingerprints(interaction.flowId()).size());
        SourceArtifact source = harness.artifacts().findSource("openstax-calculus-v1-3.3").orElseThrow();
        assertEquals("1.0.0", source.version());
        assertFalse(source.passages().isEmpty());

        assertEquals(interaction, harness.flowStore().latestInteraction(interaction.flowId()).orElseThrow());
        assertTrue(harness.flowStore().latestCheckpoint(interaction.flowId()).isPresent(),
                "every interaction must commit a checkpoint");
        assertEquals(interaction,
                harness.flowStore().findCommand(key).orElseThrow().response(),
                "the start command must be persisted with its original result");
    }

    @Test
    void aReplayedStartKeyReturnsTheOriginalFlowWithoutASecondAttemptOrPackage() {
        Harness harness = harness();
        UUID key = UUID.randomUUID();

        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, key);

        assertEquals(first.interaction(), replay.interaction());
        assertEquals(first.interaction().flowId(), replay.interaction().flowId());
        assertEquals(1, harness.artifacts().allPackages().size(),
                "a replayed start must never create a second Task Package");
    }

    @Test
    void aReusedKeyWithADifferentPayloadConflicts() {
        Harness harness = harness();
        UUID key = UUID.randomUUID();
        harness.useCase().start(LEARNER_ID, key);

        ApplicationException conflict = assertThrows(ApplicationException.class,
                () -> harness.useCase().start(UUID.randomUUID(), key));
        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
    }

    @Test
    void aPassingDiagnosticSubmissionCommitsTheIndependentBoundaryAndOriginalResult() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();

        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submit(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        ApplyFlowInteraction interaction = transitioned.interaction();
        assertEquals(2, interaction.interactionVersion());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.INDEPENDENT_TEST, interaction.stage());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, interaction.attemptPurpose());
        assertNotNull(interaction.attemptId());
        assertEquals(ApplyScriptData.INDEPENDENT_TASK_TEXT, interaction.learnerProjection().taskText());
        assertEquals(interaction, harness.flowStore().findCommand(submitKey).orElseThrow().response(),
                "the submission result must be persisted with its idempotency key");

        assertEquals(2, harness.artifacts().allPackages().size());
        assertEquals(AttemptStatus.SUBMITTED,
                harness.artifacts().findAttempt(started.interaction().attemptId()).orElseThrow().status());
        assertNotNull(harness.artifacts().findAttempt(started.interaction().attemptId()).orElseThrow().submission());
        assertEquals(2, harness.flowStore().exposedTaskFingerprints(interaction.flowId()).size(),
                "the displayed Independent package must also be exposed");
    }

    @Test
    void aReplayedSubmissionKeyReturnsTheOriginalResultWithoutASecondEvaluation() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();

        ApplyFlowResult.Boundary first = (ApplyFlowResult.Boundary) harness.useCase().submit(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) harness.useCase().submit(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertEquals(first.interaction(), replay.interaction(),
                "a replayed key must return the original result");
        assertEquals(2, harness.artifacts().allPackages().size(),
                "a replay must never create a second package or attempt");
        assertTrue(harness.generation().calls().size() <= 2,
                "a replay must never trigger a second generation");
    }

    @Test
    void aStaleInteractionVersionConflicts() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());

        ApplicationException conflict = assertThrows(ApplicationException.class, () -> harness.useCase().submit(
                started.interaction().flowId(), 0, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null));
        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
    }

    @Test
    void anUnknownFlowOrAttemptIsHandledWithoutStateChange() {
        Harness harness = harness();
        ApplicationException missing = assertThrows(ApplicationException.class,
                () -> harness.useCase().submit(UUID.randomUUID(), 1, UUID.randomUUID(),
                        UUID.randomUUID(), "x", "x", null));
        assertEquals(ErrorCode.FLOW_NOT_FOUND, missing.errorCode());

        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.SubmissionIgnored unknownAttempt = (ApplyFlowResult.SubmissionIgnored) harness.useCase().submit(
                started.interaction().flowId(), 1, UUID.randomUUID(), UUID.randomUUID(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND, unknownAttempt.reason());
    }

    @Test
    void aFreshInstanceRecoversTheOpenIndependentAttemptAndCommitsEvidenceOnce() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submit(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        ApplyFlowUseCase recovered = harness.newUseCase();
        ApplyFlowInteraction queried = recovered.query(started.interaction().flowId());
        assertEquals(transitioned.interaction(), queried,
                "a fresh instance must recover the latest interaction exactly");

        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) recovered.submit(
                started.interaction().flowId(), 2, submitKey, transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(3, completed.interaction().interactionVersion());
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "exactly one Independent Evidence may be accepted");

        ApplyFlowResult.Boundary replay = (ApplyFlowResult.Boundary) recovered.submit(
                started.interaction().flowId(), 2, submitKey, transitioned.interaction().attemptId(),
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertEquals(completed.interaction(), replay.interaction());
        assertEquals(1, harness.flowStore().allEvidence().size(),
                "a replayed key after recovery must never accept a second Evidence");
        assertEquals(1, harness.artifacts().assessmentsFor(transitioned.interaction().attemptId()).size(),
                "the isolated assessment record must be persisted exactly once for the Independent attempt");
        assertEquals(1, harness.artifacts().verificationsFor(
                        harness.artifacts().findAttempt(started.interaction().attemptId()).orElseThrow()
                                .taskPackageId()).size(),
                "the pre-delivery Task Verification record must be persisted");
    }

    @Test
    void aClosedAttemptWithANewKeyIsIgnoredWithoutASecondEvaluationOrEvidence() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submit(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        ApplyFlowResult.SubmissionIgnored duplicate = (ApplyFlowResult.SubmissionIgnored) harness.useCase().submit(
                started.interaction().flowId(), 2, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(SubmissionIgnoreReason.ALREADY_SUBMITTED, duplicate.reason());
        assertEquals(2, harness.artifacts().allPackages().size(),
                "a duplicate submission with a new key must never create a second attempt");
        assertEquals(0, harness.flowStore().allEvidence().size());
        assertEquals(2, harness.flowStore().latestInteraction(started.interaction().flowId()).orElseThrow()
                        .interactionVersion(),
                "an ignored submission must not advance the interaction");
    }

    @Test
    void aRejectedSubmissionDoesNotAdvanceTheFlowAndReplaysDeterministically() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());

        ApplyFlowResult.SubmissionRejected rejected = (ApplyFlowResult.SubmissionRejected) harness.useCase().submit(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL + " + 1", null);
        assertEquals(SubmissionRejectionReason.CONFIRMATION_MISMATCH, rejected.reason());
        assertEquals(1, harness.flowStore().latestInteraction(started.interaction().flowId()).orElseThrow()
                .interactionVersion());
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(started.interaction().attemptId()).orElseThrow().status(),
                "a rejected submission must leave the attempt open for correction");

        ApplyFlowResult.Boundary corrected = (ApplyFlowResult.Boundary) harness.useCase().submit(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertEquals(2, corrected.interaction().interactionVersion(),
                "a corrected confirmed submission must still advance the flow");
    }

    @Test
    void anUnavailableDeliveryCommitsOnlyATerminalBoundaryWithoutAnAttempt() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.sourceGapJson()));
        Harness harness = harness(generation, new ScriptedTaskVerifier(List.of()),
                new ScriptedAssessmentModel(List.of()));
        UUID startKey = UUID.randomUUID();

        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, startKey);

        assertEquals(FlowStatus.TERMINAL, started.interaction().status());
        assertEquals("暂时无法准备一道可验证的题目。请稍后重试。", started.interaction().learnerMessage());
        assertTrue(harness.artifacts().allPackages().isEmpty());
        assertTrue(harness.flowStore().latestCheckpoint(started.interaction().flowId()).isPresent());
        assertEquals(started.interaction(),
                harness.flowStore().findCommand(startKey).map(c -> c.response()).orElseThrow());
    }

    @Test
    void anIndependentSubmissionWithAProvenWrongAnswerNeverAcceptsEvidence() {
        Harness harness = harness();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase().start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) harness.useCase().submit(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE, ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        ApplyFlowResult.Boundary failed = (ApplyFlowResult.Boundary) harness.useCase().submit(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE, ApplyScriptData.WRONG_DERIVATIVE, null);

        assertEquals(FlowStatus.TERMINAL, failed.interaction().status());
        assertEquals(IndependentSubmissionFlow.SAFE_END_MESSAGE, failed.interaction().learnerMessage());
        assertTrue(harness.flowStore().allEvidence().isEmpty());
        assertEquals(AttemptStatus.SUBMITTED,
                harness.artifacts().findAttempt(transitioned.interaction().attemptId()).orElseThrow().status());
    }

    private Harness harness() {
        return harness(
                new ScriptedApplyGenerationModel(List.of(
                        ApplyScriptData.taskReadyJson(),
                        ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION))),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED))));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment
    ) {
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        ReviewTaskScheduler reviewScheduler = new ReviewTaskScheduler(flowStore);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(ReferenceBundles.stack(), generation, verifier, artifacts);
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                DiagnosticApplyFixture.diagnosticContext(), IndependentApplyFixture.independentContext(), CLOCK);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                reviewScheduler, CLOCK);
        return new Harness(
                artifacts, flowStore, generation, diagnosticFlow, independentFlow,
                new ApplyFlowUseCase(artifacts, flowStore, diagnosticFlow, independentFlow,
                        DiagnosticApplyFixture.diagnosticContext(), CLOCK));
    }

    private record Harness(
            ArtifactStore artifacts,
            LearningFlowStore flowStore,
            ScriptedApplyGenerationModel generation,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            ApplyFlowUseCase useCase
    ) {

        ApplyFlowUseCase newUseCase() {
            return new ApplyFlowUseCase(
                    artifacts, flowStore, diagnosticFlow, independentFlow,
                    DiagnosticApplyFixture.diagnosticContext(), CLOCK);
        }
    }

    private BundleStack referenceStack() {
        return ReferenceBundles.stack();
    }
}
