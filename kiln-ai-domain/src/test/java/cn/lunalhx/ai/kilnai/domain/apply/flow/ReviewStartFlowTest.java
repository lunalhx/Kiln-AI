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
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningFlowCommandUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningStateGraph;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCollectionUseCase;
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

/**
 * The deterministic Delayed Review start contract: a Due Review Task starts
 * through the durable Apply flow with a just-in-time, verified, never-exposed
 * fresh equivalent task, and only the winning idempotent command may bind the
 * Package, Attempt, Exposure, Started state, interaction, and command. Failed
 * generation leaves the Review Due with no Attempt or Exposure.
 */
class ReviewStartFlowTest {

    private static OperatorModelProfilePort profilePort() {
        return () -> ScriptedModelProfile.PROFILE;
    }


    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneOffset.UTC);
    private static final Instant DUE_CLOCK = Instant.parse("2026-08-16T01:00:00Z");
    private static final UUID LEARNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Test
    void aDueReviewStartBindsTheFreshVerifiedReviewPackageAttemptExposureStartedStateAndBoundary() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();

        ReviewTask review = harness.onlyReview();
        harness.makeDue();

        UUID startKey = UUID.randomUUID();
        ReviewStartResult.Boundary boundary = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review.reviewId(), startKey);

        LearningFlowInteraction interaction = boundary.interaction();
        assertEquals(flowId, interaction.flowId());
        assertEquals(4, interaction.interactionVersion(), "the Review appends the next interaction of the same Flow");
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, interaction.status());
        assertEquals(LearningStage.DELAYED_REVIEW, interaction.stage());
        assertEquals(AttemptPurpose.REVIEW, interaction.attemptPurpose());
        assertNotNull(interaction.attemptId());
        assertNotNull(interaction.learnerProjection());
        assertEquals(ApplyScriptData.REVIEW_TASK_TEXT, interaction.learnerProjection().taskText());
        assertFalse(interaction.learnerProjection().taskText().contains(ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                "the Review expected answer must never reach the learner");
        assertFalse(interaction.learnerProjection().taskText().contains("openstax"));
        assertFalse(interaction.learnerProjection().taskText().contains("fingerprint"));

        ReviewTask started = harness.reviewStore().findReview(review.reviewId()).orElseThrow();
        assertEquals(ReviewTaskStatus.STARTED, started.status());
        assertEquals(CLOCK.instant(), started.startedAt());

        assertEquals(3, harness.artifacts().allPackages().size(),
                "the Review start must create exactly one new Package");
        TaskPackage reviewPackage = harness.artifacts().findPackage(interaction.attemptId() == null
                ? null : attempt(harness, interaction.attemptId()).taskPackageId()).orElseThrow();
        assertEquals(AttemptPurpose.REVIEW, reviewPackage.attemptPurpose());
        assertEquals(ApplyProfile.PROFILE_ID, reviewPackage.privateAssessorProjection().executionTrace().profile());
        assertEquals("apply.polynomial-differentiation.review@1.0.0",
                reviewPackage.privateAssessorProjection().executionTrace().taskBlueprint(),
                "the Review must use the frozen versioned Review Blueprint");
        assertEquals(ApplyProfile.FIXED_STACK, reviewPackage.privateAssessorProjection().executionTrace().skillStack(),
                "the Review must reuse the same frozen Skill Stack as Independent");

        TaskAttempt reviewAttempt = attempt(harness, interaction.attemptId());
        assertEquals(AttemptStatus.OPEN, reviewAttempt.status());
        assertEquals(AttemptPurpose.REVIEW, reviewAttempt.purpose());
        assertEquals(1, harness.artifacts().allPackages().stream()
                .filter(package_ -> package_.attemptPurpose() == AttemptPurpose.REVIEW).count(),
                "at most one Review Package may exist");

        assertEquals(3, harness.flowStore().exposedTaskFingerprints(flowId).size(),
                "the Review package must be recorded in the Exposure Ledger");
        assertEquals(3, harness.flowStore().exposedSolutionFingerprints(flowId).size());
        assertEquals(1, harness.artifacts().verificationsFor(reviewPackage.taskPackageId()).size(),
                "the Review task must pass isolated Task Verification before exposure");
        assertTrue(harness.artifacts().verificationsFor(reviewPackage.taskPackageId()).get(0).passed());

        assertEquals(interaction, harness.flowStore().latestInteraction(flowId).orElseThrow());
        assertEquals(interaction, harness.flowStore().findCommand(startKey).orElseThrow().response(),
                "the successful start must be persisted with its idempotency key");
        assertTrue(harness.flowStore().latestCheckpoint(flowId).isPresent());
        assertEquals(interaction, harness.useCase().query(flowId),
                "recovery must return the exact Review interaction without regenerating");

        ReviewCollectionUseCase.ReviewTaskView view = harness.collection().unfinishedFor(LEARNER_ID).get(0);
        assertEquals(ReviewTaskStatus.STARTED, view.status());
        assertFalse(view.startable(), "a Started Review is bound to its open Attempt and not startable again");
    }

    @Test
    void theReviewGenerationCarriesTheReviewBlueprintAndExcludesEveryPriorFingerprintAndLearnerFact() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review = harness.onlyReview();
        harness.makeDue();

        String diagnosticFingerprint = harness.flowStore().exposedTaskFingerprints(flowId).get(0);
        String independentFingerprint = harness.flowStore().exposedTaskFingerprints(flowId).get(1);
        String diagnosticSolution = harness.flowStore().exposedSolutionFingerprints(flowId).get(0);
        String independentSolution = harness.flowStore().exposedSolutionFingerprints(flowId).get(1);

        harness.reviewStart().start(review.reviewId(), UUID.randomUUID());

        String reviewContextJson = harness.generation().lastContextJson();
        assertTrue(reviewContextJson.contains("\"attempt_purpose\":\"review\""),
                "the Review invocation must carry the Review Blueprint");
        assertTrue(reviewContextJson.contains("\"exposed_task_fingerprints\":[\"" + diagnosticFingerprint
                        + "\",\"" + independentFingerprint + "\"]"),
                "the Review invocation must exclude every previously exposed task fingerprint");
        assertTrue(reviewContextJson.contains("\"exposed_solution_fingerprints\":[\"" + diagnosticSolution
                        + "\",\"" + independentSolution + "\"]"),
                "the Review invocation must exclude every previously exposed solution fingerprint");
        assertFalse(reviewContextJson.contains(ApplyScriptData.TASK_TEXT),
                "the Diagnostic task text must not reach the Review generation");
        assertFalse(reviewContextJson.contains(ApplyScriptData.INDEPENDENT_TASK_TEXT),
                "the Independent task text must not reach the Review generation");
        assertFalse(reviewContextJson.contains(ApplyScriptData.EXPECTED_EXPRESSION),
                "the Diagnostic expected answer must not reach the Review generation");
        assertFalse(reviewContextJson.contains(ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                "the Independent expected answer must not reach the Review generation");
        assertFalse(reviewContextJson.contains(ApplyScriptData.UNICODE_CORRECT_DERIVATIVE),
                "learner answers must not reach the Review generation");
        assertFalse(reviewContextJson.contains(ApplyScriptData.APPLICABLE_RATIONALE),
                "learner rationales must not reach the Review generation");
        assertFalse(reviewContextJson.contains("rationaleJudgment"),
                "assessment judgments must not reach the Review generation");
        assertFalse(reviewContextJson.contains("reasonCodes"),
                "assessment reason codes must not reach the Review generation");
        assertFalse(reviewContextJson.contains("response_assessment"),
                "no assessment record may reach the Review generation");
    }

    @Test
    void aReplayedStartKeyReturnsTheOriginalInteractionWithoutASecondPackageOrAttempt() {
        Harness harness = harness();
        harness.completeIndependentPass();
        ReviewTask review = harness.onlyReview();
        harness.makeDue();
        UUID startKey = UUID.randomUUID();

        ReviewStartResult.Boundary first = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review.reviewId(), startKey);
        ReviewStartResult.Boundary replay = (ReviewStartResult.Boundary) harness.reviewStart().start(
                review.reviewId(), startKey);

        assertEquals(first.interaction(), replay.interaction(),
                "a replayed start key must return the original result");
        assertEquals(3, harness.artifacts().allPackages().size(),
                "a replay must never create a second Review Package");
        assertEquals(1, harness.artifacts().allPackages().stream()
                .filter(package_ -> package_.attemptPurpose() == AttemptPurpose.REVIEW).count());
        assertEquals(3, harness.generation().calls().size(),
                "a replay must never trigger a second Review generation");
        assertEquals(ReviewTaskStatus.STARTED, harness.reviewStore().findReview(review.reviewId())
                .orElseThrow().status());
    }

    @Test
    void aSecondStartWithADifferentKeyConflictsWithoutCreatingAnything() {
        Harness harness = harness();
        harness.completeIndependentPass();
        ReviewTask review = harness.onlyReview();
        harness.makeDue();

        harness.reviewStart().start(review.reviewId(), UUID.randomUUID());
        ApplicationException conflict = assertThrows(ApplicationException.class,
                () -> harness.reviewStart().start(review.reviewId(), UUID.randomUUID()));

        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
        assertEquals(3, harness.artifacts().allPackages().size(),
                "a racing start must never create a second Package or Attempt");
        assertEquals(1, harness.artifacts().allPackages().stream()
                .filter(package_ -> package_.attemptPurpose() == AttemptPurpose.REVIEW).count(),
                "exactly one Review Attempt may ever be opened");
        assertEquals(ReviewTaskStatus.STARTED, harness.reviewStore().findReview(review.reviewId())
                .orElseThrow().status());
    }

    @Test
    void aScheduledReviewIsNotStartable() {
        Harness harness = harness();
        harness.completeIndependentPass();
        ReviewTask review = harness.onlyReview();

        ApplicationException conflict = assertThrows(ApplicationException.class,
                () -> harness.reviewStart().start(review.reviewId(), UUID.randomUUID()));

        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
        assertEquals(ReviewTaskStatus.SCHEDULED, harness.reviewStore().findReview(review.reviewId())
                .orElseThrow().status());
        assertEquals(2, harness.artifacts().allPackages().size(),
                "a pre-due start must never create a Review Package or Attempt");
        assertTrue(harness.artifacts().allPackages().stream()
                .noneMatch(package_ -> package_.attemptPurpose() == AttemptPurpose.REVIEW));
    }

    @Test
    void aCancelledReviewIsNotStartable() {
        Harness harness = harness();
        UUID flowId = harness.completeIndependentPass();
        ReviewTask stale = harness.onlyReview();
        harness.makeDue();

        ReviewTask fresh = harness.reviewStore().acceptEvidenceAndScheduleFirstReview(
                new cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence(
                        UUID.randomUUID(), UUID.randomUUID(), flowId,
                        DiagnosticApplyFixture.CONCEPT_ID, LEARNER_ID,
                        cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult.PASS,
                        AttemptPurpose.INDEPENDENT_TEST, 0, List.of(), CLOCK.instant()),
                DUE_CLOCK).orElseThrow();

        assertEquals(ReviewTaskStatus.CANCELLED, harness.reviewStore().findReview(stale.reviewId())
                .orElseThrow().status());
        ApplicationException conflict = assertThrows(ApplicationException.class,
                () -> harness.reviewStart().start(stale.reviewId(), UUID.randomUUID()));
        assertEquals(ErrorCode.CONFLICT, conflict.errorCode());
        assertEquals(ReviewTaskStatus.SCHEDULED, harness.reviewStore().findReview(fresh.reviewId())
                .orElseThrow().status(),
                "cancelling stale work must not touch the fresh Review");
    }

    @Test
    void aSourceGapOrExhaustedReviewStartLeavesTheReviewDueWithoutAttemptOrExposure() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                ApplyScriptData.sourceGapJson()));
        Harness harness = harness(generation,
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED))));
        UUID flowId = harness.completeIndependentPass();
        ReviewTask review = harness.onlyReview();
        harness.makeDue();

        ReviewStartResult result = harness.reviewStart().start(review.reviewId(), UUID.randomUUID());

        assertInstanceOf(ReviewStartResult.Unavailable.class, result);
        ReviewStartResult.Unavailable unavailable = (ReviewStartResult.Unavailable) result;
        assertEquals(TaskUnavailableReason.SOURCE_GAP, unavailable.reason());
        assertEquals(flowId, unavailable.flowId());
        assertEquals("暂时无法准备一道可验证的题目。请稍后重试。", unavailable.learnerMessage());
        assertEquals(ReviewTaskStatus.DUE, harness.reviewStore().findReview(review.reviewId())
                .orElseThrow().status(),
                "an unavailable start must leave the Review Due for a later retry");
        assertEquals(2, harness.artifacts().allPackages().size(),
                "an unavailable start must never create a Review Package or Attempt");
        assertEquals(2, harness.flowStore().exposedTaskFingerprints(flowId).size(),
                "an unavailable start must never record Exposure");
        assertEquals(2, harness.flowStore().exposedSolutionFingerprints(flowId).size());
        assertEquals(3, harness.flowStore().latestInteraction(flowId).orElseThrow().interactionVersion(),
                "an unavailable start must never advance the Flow interaction");
        assertTrue(harness.collection().unfinishedFor(LEARNER_ID).get(0).startable(),
                "the Review must remain startable for a retry");
    }

    @Test
    void exhaustedReviewGenerationAlsoLeavesTheReviewDueAndNothingBound() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION)));
        Harness harness = harness(generation,
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.inconclusiveVerdict(), ApplyScriptData.inconclusiveVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED))));
        harness.completeIndependentPass();
        ReviewTask review = harness.onlyReview();
        harness.makeDue();

        ReviewStartResult result = harness.reviewStart().start(review.reviewId(), UUID.randomUUID());

        assertInstanceOf(ReviewStartResult.Unavailable.class, result);
        assertEquals(TaskUnavailableReason.TASK_GENERATION_EXHAUSTED,
                ((ReviewStartResult.Unavailable) result).reason());
        assertEquals(ReviewTaskStatus.DUE, harness.reviewStore().findReview(review.reviewId())
                .orElseThrow().status());
        assertEquals(2, harness.artifacts().allPackages().size(),
                "no Review Package may be persisted after exhausted generation");
        assertEquals(4, harness.generation().calls().size(),
                "both generation cycles must be attempted before exhaustion");
    }

    @Test
    void anUnknownReviewTaskIsNotFoundAndAMissingKeyIsRejected() {
        Harness harness = harness();

        ApplicationException missing = assertThrows(ApplicationException.class,
                () -> harness.reviewStart().start(UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(ErrorCode.REVIEW_NOT_FOUND, missing.errorCode());

        harness.completeIndependentPass();
        ReviewTask review = harness.onlyReview();
        ApplicationException noKey = assertThrows(ApplicationException.class,
                () -> harness.reviewStart().start(review.reviewId(), null));
        assertEquals(ErrorCode.INVALID_ARGUMENT, noKey.errorCode());
    }

    private TaskAttempt attempt(Harness harness, UUID attemptId) {
        return harness.artifacts().findAttempt(attemptId).orElseThrow();
    }

    private Harness harness() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION),
                ApplyScriptData.taskReadyJson(ApplyScriptData.REVIEW_TASK_TEXT, ApplyScriptData.REVIEW_EXPECTED_EXPRESSION)));
        return harness(generation,
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict(),
                        ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict())),
                new ScriptedAssessmentModel(List.of(ApplyScriptData.responseAssessment(
                        FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED))));
    }

    private Harness harness(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment
    ) {
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK, artifacts);
        ReviewTaskScheduler reviewScheduler = new ReviewTaskScheduler(flowStore);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(ReferenceBundles.stack(), generation, verifier, artifacts);
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                DiagnosticApplyFixture.diagnosticContext(), IndependentApplyFixture.independentContext(), CLOCK);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                reviewScheduler, CLOCK);
        ReviewSubmissionFlow reviewFlow = new ReviewSubmissionFlow(
                artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                reviewScheduler, executor, flowStore, ReviewApplyFixture.reviewContext(), CLOCK);
        PracticeSubmissionFlow practiceFlow = new PracticeSubmissionFlow(
                executor, artifacts, flowStore, assessment, new ScriptedResponseVerificationModel(List.of()),
                PracticeApplyFixture.practiceContext(), IndependentApplyFixture.independentContext(), CLOCK);
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
                TeachBackApplyFixture.teachBackContext(), CLOCK);
        LearningStateGraph graph = new LearningStateGraph(
                artifacts, flowStore, flowStore, diagnosticFlow, independentFlow, practiceFlow,
                reviewFlow, explainFlow, hintFlow, teachBackFlow,
                new ScriptedPedagogyModel(), new ScriptedClarificationClassifier(), CLOCK);
        LearningFlowCommandUseCase useCase = new LearningFlowCommandUseCase(
                flowStore, graph, DiagnosticApplyFixture.diagnosticContext(), profilePort());
        ReviewStartFlow reviewStart = new ReviewStartFlow(
                executor, flowStore, flowStore, ReviewApplyFixture.reviewContext(), CLOCK);
        return new Harness(artifacts, flowStore, generation, useCase, reviewStart);
    }

    private record Harness(
            ArtifactStore artifacts,
            InMemoryLearningFlowStore flowStore,
            ScriptedApplyGenerationModel generation,
            LearningFlowCommandUseCase useCase,
            ReviewStartFlow reviewStart
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

        ReviewTask onlyReview() {
            List<ReviewTask> reviews = flowStore.unfinishedReviewsFor(LEARNER_ID);
            assertEquals(1, reviews.size());
            return reviews.get(0);
        }

        void makeDue() {
            flowStore.markDueReviewsDue(DUE_CLOCK);
        }

        ReviewTaskStore reviewStore() {
            return flowStore;
        }

        ReviewCollectionUseCase collection() {
            return new ReviewCollectionUseCase(flowStore, flowStore);
        }
    }
}
