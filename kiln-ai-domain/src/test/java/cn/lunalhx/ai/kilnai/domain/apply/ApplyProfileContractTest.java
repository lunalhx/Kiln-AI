package cn.lunalhx.ai.kilnai.domain.apply;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedResponseVerificationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.IndependentSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionRejectionReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.MasteryMilestone;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyProfileContractTest {

    private final ApplyExecutionContext context = DiagnosticApplyFixture.diagnosticContext();
    private final BundleStack stack = referenceStack();

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void deliversAValidDiagnosticTaskWithAPrivateFreeLearnerProjection() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore store = new InMemoryArtifactStore(Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC));
        ApplyProfileExecutor executor = new ApplyProfileExecutor(stack, generation, verifier, store);

        ApplyDeliveryResult result = executor.deliver(context);

        assertInstanceOf(ApplyDeliveryResult.Delivered.class, result);
        ApplyDeliveryResult.Delivered delivered = (ApplyDeliveryResult.Delivered) result;

        TaskAttempt attempt = delivered.attempt();
        assertNotNull(attempt.attemptId());
        assertEquals(AttemptPurpose.DIAGNOSTIC, attempt.purpose());
        assertEquals(AttemptStatus.OPEN, attempt.status());

        LearnerProjection learner = delivered.learnerProjection();
        assertEquals("zh-CN", learner.locale());
        assertEquals(ApplyScriptData.TASK_TEXT, learner.taskText());
        assertEquals(List.of(ApplyLearnerEvent.ANSWER_SUBMITTED, ApplyLearnerEvent.PROCEDURAL_CLARIFICATION,
                ApplyLearnerEvent.FLOW_CONTROL), learner.allowedEvents());
        assertEquals(1, learner.submissionRule().maxFormalSubmissions());
        LearnerProjection.AnswerField derivative = learner.answerFields().stream()
                .filter(field -> "final_derivative".equals(field.id())).findFirst().orElseThrow();
        assertEquals("f'(x)", derivative.label());
        assertEquals("mathematical_expression", derivative.kind());
        assertTrue(derivative.required());
        assertEquals(List.of("x"), derivative.variables());
        assertEquals(List.of("plain_text", "unicode_math", "latex_like"), derivative.acceptedInputFamilies());
        LearnerProjection.AnswerField rationale = learner.answerFields().stream()
                .filter(field -> "rule_rationale".equals(field.id())).findFirst().orElseThrow();
        assertEquals("理由（可选）", rationale.label());
        assertEquals("short_text", rationale.kind());
        assertFalse(rationale.required());

        String learnerText = learner.taskText();
        assertFalse(learnerText.contains("12*x^2 - 6*x + 7"), "expected answer must not reach the learner");
        assertFalse(learnerText.contains("openstax"), "source identities must not reach the learner");
        assertFalse(learnerText.contains("sec-3.3"), "source anchors must not reach the learner");
        assertFalse(learnerText.contains("fingerprint"), "fingerprints must not reach the learner");

        TaskPackage persisted = store.findPackage(attempt.taskPackageId()).orElseThrow();
        PrivateAssessorProjection privateProjection = persisted.privateAssessorProjection();
        assertEquals("12*x^2 - 6*x + 7", privateProjection.canonicalExpectedAnswer().expression());
        assertEquals("profile", privateProjection.taskFingerprint().derivedBy());
        assertFalse(privateProjection.taskFingerprint().value().isBlank());
        assertEquals(ApplyProfile.PROFILE_ID, privateProjection.executionTrace().profile());
        assertEquals("apply.polynomial-differentiation.diagnostic@1.0.0",
                privateProjection.executionTrace().taskBlueprint());
        assertEquals(ApplyProfile.FIXED_STACK, privateProjection.executionTrace().skillStack());
        assertEquals("1.0.0", privateProjection.sourceTrace().get(0).sourceVersion());
    }

    @Test
    void separatesCompiledSystemInstructionsFromClosedExecutionContextJson() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore store = new InMemoryArtifactStore(Clock.systemUTC());
        new ApplyProfileExecutor(stack, generation, verifier, store).deliver(context);

        String systemPrompt = generation.lastSystemPrompt();
        String contextJson = generation.lastContextJson();

        assertTrue(systemPrompt.contains("# Apply Profile"));
        assertTrue(systemPrompt.contains("[bundle:action:apply.task-first@0.1.0]"));
        assertTrue(systemPrompt.contains("[bundle:subject:subject.calculus-notation@0.1.0]"));
        assertTrue(systemPrompt.contains("# Response Contract"));
        assertFalse(systemPrompt.contains(context.conceptSourcePack().passages().get(0).content()));
        assertFalse(systemPrompt.contains("\"learner_locale\""));

        assertEquals("apply_execution_context/v1",
                cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson.readTree(contextJson)
                        .get("schema").asText());
        assertTrue(contextJson.contains("\"learner_locale\":\"zh-CN\""));
        assertTrue(contextJson.contains("\"attempt_purpose\":\"diagnostic\""));
        assertFalse(contextJson.contains("# Apply Profile"));
    }

    @Test
    void sourceGapEndsGenerationImmediatelyWithoutAttemptOrEvidence() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.sourceGapJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of());
        ArtifactStore store = new InMemoryArtifactStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(stack, generation, verifier, store);

        ApplyDeliveryResult result = executor.deliver(context);

        assertInstanceOf(ApplyDeliveryResult.Unavailable.class, result);
        ApplyDeliveryResult.Unavailable unavailable = (ApplyDeliveryResult.Unavailable) result;
        assertEquals(TaskUnavailableReason.SOURCE_GAP, unavailable.reason());
        assertEquals("暂时无法准备一道可验证的题目。请稍后重试。", unavailable.learnerMessage());
        assertEquals(1, generation.calls().size(), "Source Gap must never retry");
        assertTrue(store.allPackages().isEmpty(), "no Task Package may be created");
        assertTrue(verifier.verified().isEmpty(), "no Task Verification may run");
    }

    @Test
    void aRejectedFirstCandidatePermitsOneFreshCycleAndOnlyExposesTheSecond() {
        String firstTaskText = "设 g(x) = 5x² − 2x + 1，求 g'(x)。";
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(firstTaskText, "10*x - 2"),
                ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.rejectVerdict(), ApplyScriptData.passVerdict()));
        ArtifactStore store = new InMemoryArtifactStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(stack, generation, verifier, store);

        ApplyDeliveryResult result = executor.deliver(context);

        assertInstanceOf(ApplyDeliveryResult.Delivered.class, result);
        ApplyDeliveryResult.Delivered delivered = (ApplyDeliveryResult.Delivered) result;
        assertEquals(2, generation.calls().size());
        assertEquals(2, verifier.verified().size());
        assertEquals(1, store.allPackages().size(), "only the accepted candidate may be persisted");
        assertEquals(ApplyScriptData.TASK_TEXT, delivered.learnerProjection().taskText());
        assertEquals(ApplyScriptData.TASK_TEXT,
                store.allPackages().get(0).learnerProjection().taskText());
        assertFalse(store.allPackages().get(0).learnerProjection().taskText().contains(firstTaskText));
    }

    @Test
    void twoFailedCandidatesReturnTaskGenerationExhaustedWithoutAttemptOrEvidence() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(), ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.inconclusiveVerdict(), ApplyScriptData.inconclusiveVerdict()));
        ArtifactStore store = new InMemoryArtifactStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(stack, generation, verifier, store);

        ApplyDeliveryResult result = executor.deliver(context);

        assertInstanceOf(ApplyDeliveryResult.Unavailable.class, result);
        ApplyDeliveryResult.Unavailable unavailable = (ApplyDeliveryResult.Unavailable) result;
        assertEquals(TaskUnavailableReason.TASK_GENERATION_EXHAUSTED, unavailable.reason());
        assertEquals("暂时无法准备一道可验证的题目。请稍后重试。", unavailable.learnerMessage());
        assertEquals(2, generation.calls().size());
        assertTrue(store.allPackages().isEmpty(), "no Task Package may be created");
    }

    @Test
    void anUnparseableCandidateCountsAsAFailedCycleAndAllowsOneRetry() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                "{not valid json", ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore store = new InMemoryArtifactStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(stack, generation, verifier, store);

        ApplyDeliveryResult result = executor.deliver(context);

        assertInstanceOf(ApplyDeliveryResult.Delivered.class, result);
        assertEquals(2, generation.calls().size());
        assertEquals(1, store.allPackages().size());
    }

    @Test
    void anInvalidDraftFieldClaimCountsAsAFailedCycle() {
        String polluted = ApplyScriptData.taskReadyJson().replace(
                "\"learner_task_text\"", "\"task_fingerprint\": \"fp-1\", \"learner_task_text\"");
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                polluted, ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ArtifactStore store = new InMemoryArtifactStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(stack, generation, verifier, store);

        ApplyDeliveryResult result = executor.deliver(context);

        assertInstanceOf(ApplyDeliveryResult.Delivered.class, result);
        assertEquals(2, generation.calls().size());
        assertEquals(1, store.allPackages().size());
    }

    @Test
    void bothUnavailableReasonsShareTheSameNeutralLearnerMessage() {
        assertEquals(ApplyDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE,
                "暂时无法准备一道可验证的题目。请稍后重试。");
    }

    @Test
    void theFingerprintsAreDeterministicallyDerivedFromValidatedTaskFacts() {
        ArtifactStore firstStore = new InMemoryArtifactStore(Clock.systemUTC());
        new ApplyProfileExecutor(stack,
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())), firstStore)
                .deliver(context);
        ArtifactStore secondStore = new InMemoryArtifactStore(Clock.systemUTC());
        new ApplyProfileExecutor(stack,
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())), secondStore)
                .deliver(context);

        assertEquals(
                firstStore.allPackages().get(0).privateAssessorProjection().taskFingerprint().value(),
                secondStore.allPackages().get(0).privateAssessorProjection().taskFingerprint().value(),
                "the Profile, not the model, must own the derived Task Fingerprint");
        assertEquals(
                firstStore.allPackages().get(0).privateAssessorProjection().solutionFingerprint().value(),
                secondStore.allPackages().get(0).privateAssessorProjection().solutionFingerprint().value(),
                "the Profile must own the derived Solution Fingerprint");
        assertNotEquals(
                firstStore.allPackages().get(0).privateAssessorProjection().taskFingerprint().value(),
                firstStore.allPackages().get(0).privateAssessorProjection().solutionFingerprint().value(),
                "the task and solution fingerprints must be distinct");
    }

    @Test
    void oneFormalSubmissionClosesTheAttemptRetainingRawAndConfirmedCanonical() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL,
                null);

        assertInstanceOf(DiagnosticSubmissionResult.Passed.class, result);
        DiagnosticSubmissionResult.Passed passed = (DiagnosticSubmissionResult.Passed) result;

        TaskAttempt closedAttempt = passed.closedDiagnosticAttempt();
        assertEquals(AttemptStatus.SUBMITTED, closedAttempt.status());
        assertNotNull(closedAttempt.closedAt(), "a formal submission must close the attempt atomically");
        assertNotNull(closedAttempt.submission(), "the closed attempt must retain its submission");
        assertEquals(ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                closedAttempt.submission().finalDerivative().raw(),
                "the raw derivative must be retained with the submission");
        assertEquals(ApplyScriptData.UNICODE_CORRECT_CANONICAL,
                closedAttempt.submission().finalDerivative().confirmedCanonical(),
                "the learner-confirmed canonical expression must be retained with the submission");
        assertEquals(AnswerInputFamily.UNICODE_MATH,
                closedAttempt.submission().finalDerivative().inputFamily());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, passed.independentAttempt().purpose());
        assertEquals(AttemptStatus.OPEN, passed.independentAttempt().status());
    }

    @Test
    void aPassingDiagnosticMovesThroughANeutralTransitionWithoutFeedback() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL,
                null);

        assertInstanceOf(DiagnosticSubmissionResult.Passed.class, result);
        DiagnosticSubmissionResult.Passed passed = (DiagnosticSubmissionResult.Passed) result;
        assertEquals(DiagnosticFlow.NEUTRAL_TRANSITION_MESSAGE, passed.neutralTransitionMessage());
        assertFalse(passed.neutralTransitionMessage().contains("正确"), "no correctness feedback");
        assertFalse(passed.neutralTransitionMessage().contains("错误"), "no failure feedback");
        assertFalse(passed.neutralTransitionMessage().contains("答案"), "no answer feedback");
        assertTrue(assessment.contexts().isEmpty(), "a proven derivative must not need a rationale judgment");
        assertEquals(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                passed.independentLearnerProjection().taskText());
        assertFalse(passed.independentLearnerProjection().taskText()
                .contains(ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION), "the expected answer must stay private");
    }

    @Test
    void aPassingDiagnosticViaAnApplicableRationaleCountsWhenTheDerivativeIsIncorrect() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.APPLICABLE)));
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE,
                ApplyScriptData.WRONG_DERIVATIVE,
                ApplyScriptData.APPLICABLE_RATIONALE);

        assertInstanceOf(DiagnosticSubmissionResult.Passed.class, result);
        assertEquals(1, assessment.contexts().size());
        assertEquals(ApplyScriptData.APPLICABLE_RATIONALE, assessment.contexts().get(0).rationale());
        assertEquals(ApplyScriptData.EXPECTED_EXPRESSION,
                assessment.contexts().get(0).expectedCanonicalExpression());
        assertEquals(ApplyScriptData.TASK_TEXT, assessment.contexts().get(0).taskText());
    }

    @Test
    void aFailingDiagnosticEndsSafelyWithoutFeedbackAndWithoutAnIndependentTask() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_APPLICABLE)));
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE,
                ApplyScriptData.WRONG_DERIVATIVE,
                "我猜的");

        assertInstanceOf(DiagnosticSubmissionResult.Failed.class, result);
        DiagnosticSubmissionResult.Failed failed = (DiagnosticSubmissionResult.Failed) result;
        assertEquals(List.of("differentiate-polynomial"), failed.facts().missingCriteria(),
                "the sanitized failure facts must carry the missing rubric criterion");
        assertTrue(failed.facts().satisfiedCriteria().isEmpty(),
                "a conclusive failure satisfies no rubric criterion");
        assertEquals(1, generation.calls().size(), "no Independent task may be generated after a failure");
        assertEquals(1, assessment.contexts().size(), "the failing rationale must be judged once");
    }

    @Test
    void aReplayedOrDuplicateSubmissionCannotProduceASecondEvaluationOrResult() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        UUID attemptId = diagnostic.attempt().attemptId();
        harness.flow().submitDiagnostic(FLOW_ID, attemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        DiagnosticSubmissionResult replayed = harness.flow().submitDiagnostic(FLOW_ID, 
                attemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.Ignored.class, replayed);
        assertEquals(SubmissionIgnoreReason.ALREADY_SUBMITTED,
                ((DiagnosticSubmissionResult.Ignored) replayed).reason());
        assertEquals(0, assessment.contexts().size(), "a replay must never trigger a second evaluation");
        assertEquals(2, generation.calls().size(), "a replay must never deliver a second Independent task");
        assertEquals(1, harness.artifacts().allPackages().stream()
                .filter(package_ -> package_.attemptPurpose() == AttemptPurpose.DIAGNOSTIC).count());
    }

    @Test
    void aStaleSubmissionForAnUnknownAttemptIsIgnoredWithoutEvaluation() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                UUID.randomUUID(), ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.Ignored.class, result);
        assertEquals(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND,
                ((DiagnosticSubmissionResult.Ignored) result).reason());
        assertTrue(assessment.contexts().isEmpty());
        assertEquals(0, generation.calls().size(), "an unknown attempt must not trigger generation");
    }

    @Test
    void aConfirmationMismatchOrUnparseableRawAnswerIsRejectedWithoutClosingOrAssessing() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        UUID attemptId = diagnostic.attempt().attemptId();

        DiagnosticSubmissionResult mismatched = harness.flow().submitDiagnostic(FLOW_ID, 
                attemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL + " + 1", null);
        assertInstanceOf(DiagnosticSubmissionResult.NotSubmittable.class, mismatched);
        assertEquals(SubmissionRejectionReason.CONFIRMATION_MISMATCH,
                ((DiagnosticSubmissionResult.NotSubmittable) mismatched).reason());

        DiagnosticSubmissionResult unparseable = harness.flow().submitDiagnostic(FLOW_ID, 
                attemptId, "12*y^2 + 1", "12*y^2 + 1", null);
        assertInstanceOf(DiagnosticSubmissionResult.NotSubmittable.class, unparseable);
        assertEquals(SubmissionRejectionReason.UNPARSEABLE_RAW,
                ((DiagnosticSubmissionResult.NotSubmittable) unparseable).reason());

        assertTrue(assessment.contexts().isEmpty(), "no evaluation before a valid confirmed submission");
        assertEquals(AttemptStatus.OPEN,
                harness.artifacts().findAttempt(attemptId).orElseThrow().status(),
                "the attempt must remain open for correction");

        DiagnosticSubmissionResult corrected = harness.flow().submitDiagnostic(FLOW_ID, 
                attemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        assertInstanceOf(DiagnosticSubmissionResult.Passed.class, corrected,
                "a corrected confirmed submission must still close and pass the attempt");
    }

    @Test
    void theIndependentDeliveryReceivesOnlyThePassRoutingAndExposedFingerprints() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        harness.flow().submitDiagnostic(FLOW_ID, diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        String independentContextJson = generation.calls().get(1).contextJson();
        String diagnosticFingerprint = harness.artifacts().findPackage(diagnostic.attempt().taskPackageId())
                .orElseThrow().privateAssessorProjection().taskFingerprint().value();
        String diagnosticSolutionFingerprint = harness.artifacts().findPackage(diagnostic.attempt().taskPackageId())
                .orElseThrow().privateAssessorProjection().solutionFingerprint().value();

        assertTrue(independentContextJson.contains("\"attempt_purpose\":\"independent_test\""),
                "the Independent invocation must carry the Independent blueprint");
        assertTrue(independentContextJson.contains("\"exposed_task_fingerprints\":[\"" + diagnosticFingerprint + "\"]"),
                "the Independent invocation must carry every exposed task fingerprint");
        assertTrue(independentContextJson.contains("\"exposed_solution_fingerprints\":[\"" + diagnosticSolutionFingerprint + "\"]"),
                "the Independent invocation must carry every exposed solution fingerprint");
        assertFalse(independentContextJson.contains(ApplyScriptData.TASK_TEXT),
                "the Diagnostic task text must not reach the Independent generation");
        assertFalse(independentContextJson.contains(ApplyScriptData.EXPECTED_EXPRESSION),
                "the Diagnostic expected answer must not reach the Independent generation");
        assertFalse(independentContextJson.contains(ApplyScriptData.UNICODE_CORRECT_DERIVATIVE),
                "the Diagnostic raw answer must not reach the Independent generation");
        assertFalse(independentContextJson.contains(ApplyScriptData.APPLICABLE_RATIONALE),
                "the Diagnostic rationale must not reach the Independent generation");
    }

    @Test
    void everyDisplayedTaskRecordsTaskAndSolutionFingerprintsForFreshnessExclusion() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult passed = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentTaskPackageId = harness.artifacts().findAttempt(
                        ((DiagnosticSubmissionResult.Passed) passed).independentAttempt().attemptId())
                .orElseThrow().taskPackageId();

        TaskPackage diagnosticPackage = harness.artifacts().findPackage(diagnostic.attempt().taskPackageId()).orElseThrow();
        TaskPackage independentPackage = harness.artifacts().findPackage(independentTaskPackageId).orElseThrow();

        List<String> exposedTasks = harness.flowStore().exposedTaskFingerprints(FLOW_ID);
        List<String> exposedSolutions = harness.flowStore().exposedSolutionFingerprints(FLOW_ID);
        assertEquals(2, exposedTasks.size(), "every displayed task must record its task fingerprint");
        assertEquals(2, exposedSolutions.size(), "every displayed task must record its solution fingerprint");
        assertTrue(exposedTasks.contains(diagnosticPackage.privateAssessorProjection().taskFingerprint().value()));
        assertTrue(exposedTasks.contains(independentPackage.privateAssessorProjection().taskFingerprint().value()));
        assertTrue(exposedSolutions.contains(diagnosticPackage.privateAssessorProjection().solutionFingerprint().value()));
        assertTrue(exposedSolutions.contains(independentPackage.privateAssessorProjection().solutionFingerprint().value()));

        String independentContextJson = generation.calls().get(1).contextJson();
        assertTrue(independentContextJson.contains("\"exposed_solution_fingerprints\":[\""
                        + diagnosticPackage.privateAssessorProjection().solutionFingerprint().value() + "\"]"),
                "the Independent generation must continue to apply solution-fingerprint freshness exclusion");
    }

    @Test
    void anIndependentTaskExcludesPreviouslyExposedTaskFingerprints() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        String diagnosticFingerprint = harness.artifacts().findPackage(diagnostic.attempt().taskPackageId())
                .orElseThrow().privateAssessorProjection().taskFingerprint().value();
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.Passed.class, result);
        DiagnosticSubmissionResult.Passed passed = (DiagnosticSubmissionResult.Passed) result;
        String independentFingerprint = harness.artifacts().findPackage(passed.independentAttempt().taskPackageId())
                .orElseThrow().privateAssessorProjection().taskFingerprint().value();
        assertEquals(3, generation.calls().size(),
                "a fingerprint collision must consume one fresh generation cycle");
        assertEquals(2, verifier.verified().size(),
                "a colliding candidate must be discarded before Task Verification");
        assertFalse(independentFingerprint.equals(diagnosticFingerprint),
                "the Independent task must be a fresh equivalent task");
    }

    @Test
    void aSubmissionAgainstANonDiagnosticAttemptIsIgnored() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult passed = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = ((DiagnosticSubmissionResult.Passed) passed).independentAttempt().attemptId();

        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                independentAttemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.Ignored.class, result);
        assertEquals(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE,
                ((DiagnosticSubmissionResult.Ignored) result).reason());
        assertEquals(AttemptStatus.OPEN, harness.artifacts().findAttempt(independentAttemptId).orElseThrow().status(),
                "the Independent attempt must not be closed through the Diagnostic submission path");
        assertEquals(0, assessment.contexts().size(), "no evaluation may run for the wrong-purpose submission");
    }

    @Test
    void aPassingDiagnosticWhoseIndependentTaskCannotBePreparedSurfacesTheNeutralUnavailableMessage() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.sourceGapJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.IndependentUnavailable.class, result);
        DiagnosticSubmissionResult.IndependentUnavailable unavailable =
                (DiagnosticSubmissionResult.IndependentUnavailable) result;
        assertEquals(TaskUnavailableReason.SOURCE_GAP, unavailable.reason());
        assertEquals(ApplyDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.learnerMessage());
    }

    @Test
    void cannotDecidePassesOnlyWhenIsolatedAssessmentAndResponseVerificationBothJudgeEquivalent() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_APPLICABLE)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_APPLICABLE)));
        Harness harness = flow(generation, verifier, assessment, verification);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                null);

        assertInstanceOf(DiagnosticSubmissionResult.Passed.class, result);
        assertEquals(1, assessment.contexts().size(), "the isolated Assessment must judge the Cannot Decide input");
        assertEquals(1, verification.contexts().size(),
                "independent Response Verification must judge the same Cannot Decide input");
        assertEquals(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                ((DiagnosticSubmissionResult.Passed) result).independentLearnerProjection().taskText());
    }

    @Test
    void cannotDecideAssessmentAndVerificationAreIsolatedAndReceiveTheSameInputs() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_APPLICABLE)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_EQUIVALENT, RationaleJudgment.NOT_APPLICABLE)));
        Harness harness = flow(generation, verifier, assessment, verification);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                ApplyScriptData.APPLICABLE_RATIONALE);

        assertEquals(1, assessment.contexts().size());
        assertEquals(1, verification.contexts().size());
        ResponseAssessmentContext assessed = assessment.contexts().get(0);
        ResponseAssessmentContext verified = verification.contexts().get(0);
        assertEquals(assessed, verified,
                "Assessment and Response Verification must receive the identical raw and confirmed inputs");
        assertEquals(ApplyScriptData.UNDECIDABLE_DERIVATIVE, assessed.rawAnswer());
        assertEquals(ApplyScriptData.UNDECIDABLE_DERIVATIVE, assessed.confirmedCanonicalExpression());
        assertEquals(ApplyScriptData.EXPECTED_EXPRESSION, assessed.expectedCanonicalExpression());
        assertEquals(ApplyScriptData.APPLICABLE_RATIONALE, assessed.rationale());
        assertEquals(AttemptPurpose.DIAGNOSTIC, assessed.purpose());
        assertEquals(EquivalenceOutcome.CANNOT_DECIDE, assessed.deterministicOutcome());
    }

    @Test
    void cannotDecideWithDisagreementIsInconclusiveWithoutFailureFeedbackOrEvidence() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_APPLICABLE)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_EQUIVALENT, RationaleJudgment.NOT_APPLICABLE)));
        Harness harness = flow(generation, verifier, assessment, verification);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                null);

        assertInstanceOf(DiagnosticSubmissionResult.Inconclusive.class, result);
        DiagnosticSubmissionResult.Inconclusive inconclusive =
                (DiagnosticSubmissionResult.Inconclusive) result;
        assertEquals(DiagnosticFlow.NEUTRAL_TRANSITION_MESSAGE, inconclusive.neutralTransitionMessage());
        assertFalse(inconclusive.neutralTransitionMessage().contains("正确"), "no correctness feedback");
        assertFalse(inconclusive.neutralTransitionMessage().contains("错误"), "no failure feedback");
        assertFalse(inconclusive.neutralTransitionMessage().contains("答案"), "no answer feedback");
        assertNotNull(inconclusive.independentAttempt(), "a fresh Independent task must be prepared");
        String diagnosticFingerprint = harness.artifacts().findPackage(diagnostic.attempt().taskPackageId())
                .orElseThrow().privateAssessorProjection().taskFingerprint().value();
        String independentFingerprint = harness.artifacts().findPackage(inconclusive.independentAttempt().taskPackageId())
                .orElseThrow().privateAssessorProjection().taskFingerprint().value();
        assertFalse(independentFingerprint.equals(diagnosticFingerprint),
                "the prepared Independent task must be fresh");
    }

    @Test
    void cannotDecideWithBothNonEquivalentIsInconclusiveNeverGuessedWrong() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_EQUIVALENT, RationaleJudgment.NOT_APPLICABLE)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_EQUIVALENT, RationaleJudgment.NOT_APPLICABLE)));
        Harness harness = flow(generation, verifier, assessment, verification);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                null);

        assertInstanceOf(DiagnosticSubmissionResult.Inconclusive.class, result,
                "an unresolved expression must never be judged a failure");
        assertEquals(2, generation.calls().size(),
                "the Inconclusive Diagnostic must prepare a fresh Independent task");
        assertNotNull(((DiagnosticSubmissionResult.Inconclusive) result).independentAttempt());
    }

    @Test
    void anIndependentSubmissionWithAnOmittedRationaleAcceptsExactlyOneIndependentEvidence() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of());
        PassedIndependent passed = passDiagnosticAndDeliverIndependent(assessment, verification);
        IndependentSubmissionFlow flow = independentFlow(passed.harness(), assessment, verification);

        IndependentSubmissionResult result = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);

        assertInstanceOf(IndependentSubmissionResult.EvidenceAccepted.class, result);
        IndependentSubmissionResult.EvidenceAccepted accepted =
                (IndependentSubmissionResult.EvidenceAccepted) result;
        assertEquals(AttemptStatus.SUBMITTED, accepted.closedAttempt().status());
        AcceptedLearningEvidence evidence = accepted.evidence();
        assertEquals(passed.independentAttemptId(), evidence.taskAttemptId());
        assertEquals(FLOW_ID, evidence.flowId());
        assertEquals(CONCEPT_ID, evidence.conceptId());
        assertEquals(LEARNER_ID, evidence.learnerId());
        assertEquals(LearningResult.PASS, evidence.result());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, evidence.attemptPurpose());
        assertEquals(0, evidence.highestHintLevel(), "a no-hint Independent success must never use a hint");
        assertTrue(evidence.assistanceTrace().isEmpty());
        assertEquals(MasteryMilestone.INDEPENDENT, accepted.progress().currentMilestone(),
                "the accepted evidence must project the Concept to Independent");
        assertEquals(MasteryMilestone.INDEPENDENT, accepted.progress().highestMilestoneReached());
        assertEquals(LearningStage.DELAYED_REVIEW, accepted.progress().currentStage());
        assertEquals(1, passed.harness().flowStore().allEvidence().size(),
                "exactly one evidence record may be accepted");
        assertTrue(verification.contexts().isEmpty(), "a proven result must never invoke Response Verification");
    }

    @Test
    void anIndependentSubmissionWithANonSubstantiveRationaleDoesNotBlockEvidence() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NON_SUBSTANTIVE)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of());
        PassedIndependent passed = passDiagnosticAndDeliverIndependent(assessment, verification);
        IndependentSubmissionFlow flow = independentFlow(passed.harness(), assessment, verification);

        IndependentSubmissionResult result = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.NON_SUBSTANTIVE_RATIONALE);

        assertInstanceOf(IndependentSubmissionResult.EvidenceAccepted.class, result,
                "a non-substantive rationale must not block Independent evidence");
        assertEquals(1, passed.harness().flowStore().allEvidence().size());
    }

    @Test
    void anIndependentSubmissionWithAClearlyContradictoryRationaleCreatesNoEvidenceAndRequiresAReplacement() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.CLEARLY_CONTRADICTORY)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of());
        PassedIndependent passed = passDiagnosticAndDeliverIndependent(assessment, verification);
        IndependentSubmissionFlow flow = independentFlow(passed.harness(), assessment, verification);

        IndependentSubmissionResult result = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, ApplyScriptData.CONTRADICTORY_RATIONALE);

        assertInstanceOf(IndependentSubmissionResult.ReplacementRequired.class, result,
                "ADR-0061: a clearly contradictory rationale must block Independent evidence and require a fresh replacement");
        assertEquals(IndependentSubmissionFlow.REPLACEMENT_MESSAGE,
                ((IndependentSubmissionResult.ReplacementRequired) result).learnerMessage());
        assertTrue(passed.harness().flowStore().allEvidence().isEmpty(),
                "a contradictory rationale must never create evidence");
    }

    @Test
    void aProvenDeterministicNonEquivalenceBuildsExactlyOneNoHintFailEvidenceCandidate() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of());
        PassedIndependent passed = passDiagnosticAndDeliverIndependent(assessment, verification);
        IndependentSubmissionFlow flow = independentFlow(passed.harness(), assessment, verification);

        IndependentSubmissionResult result = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.WRONG_DERIVATIVE,
                ApplyScriptData.WRONG_DERIVATIVE, null);

        assertInstanceOf(IndependentSubmissionResult.FailureEvidenceAccepted.class, result,
                "a proven deterministic non-equivalence is a conclusive no-hint failure");
        IndependentSubmissionResult.FailureEvidenceAccepted failed =
                (IndependentSubmissionResult.FailureEvidenceAccepted) result;
        assertEquals(LearningResult.FAIL, failed.evidence().result());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, failed.evidence().attemptPurpose());
        assertEquals(0, failed.evidence().highestHintLevel(), "a no-hint fail carries the zero hint level");
        assertTrue(failed.evidence().assistanceTrace().isEmpty());
        assertTrue(passed.harness().flowStore().allEvidence().isEmpty(),
                "the flow proposes the fail Evidence; only the Graph accepts it after remediation generation");
        assertTrue(assessment.contexts().isEmpty(), "no model judgment may override a proven deterministic result");
        assertTrue(verification.contexts().isEmpty());
    }

    @Test
    void aDuplicateIndependentSubmissionIsIgnoredWithoutASecondEvidenceOrAssessment() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of());
        PassedIndependent passed = passDiagnosticAndDeliverIndependent(assessment, verification);
        IndependentSubmissionFlow flow = independentFlow(passed.harness(), assessment, verification);

        IndependentSubmissionResult first = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertInstanceOf(IndependentSubmissionResult.EvidenceAccepted.class, first);
        IndependentSubmissionResult replayed = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);

        assertInstanceOf(IndependentSubmissionResult.Ignored.class, replayed);
        assertEquals(SubmissionIgnoreReason.ALREADY_SUBMITTED,
                ((IndependentSubmissionResult.Ignored) replayed).reason());
        assertEquals(1, assessment.contexts().size(), "a replay must never trigger a second evaluation");
        assertEquals(1, passed.harness().flowStore().allEvidence().size(),
                "a replay must never accept a second evidence record");
    }

    @Test
    void anUnclosedIndependentAttemptIsRejectedWithoutAssessmentOrEvidence() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of());
        PassedIndependent passed = passDiagnosticAndDeliverIndependent(assessment, verification);
        IndependentSubmissionFlow flow = independentFlow(passed.harness(), assessment, verification);

        IndependentSubmissionResult mismatched = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION + " + 1", null);
        assertInstanceOf(IndependentSubmissionResult.NotSubmittable.class, mismatched);
        assertEquals(SubmissionRejectionReason.CONFIRMATION_MISMATCH,
                ((IndependentSubmissionResult.NotSubmittable) mismatched).reason());
        assertEquals(AttemptStatus.OPEN,
                passed.harness().artifacts().findAttempt(passed.independentAttemptId()).orElseThrow().status(),
                "the attempt must remain open for correction");
        assertTrue(assessment.contexts().isEmpty(), "an open attempt must never be assessed");
        assertTrue(passed.harness().flowStore().allEvidence().isEmpty(),
                "an unclosed attempt must never create evidence");

        IndependentSubmissionResult corrected = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);
        assertInstanceOf(IndependentSubmissionResult.EvidenceAccepted.class, corrected,
                "a corrected confirmed submission must close and accept evidence");
    }

    @Test
    void anIndependentSubmissionWithDisagreeingCannotDecideJudgmentsIsInconclusiveWithoutEvidence() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.EQUIVALENT, RationaleJudgment.NOT_PROVIDED)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_EQUIVALENT, RationaleJudgment.NOT_PROVIDED)));
        PassedIndependent passed = passDiagnosticAndDeliverIndependent(assessment, verification);
        IndependentSubmissionFlow flow = independentFlow(passed.harness(), assessment, verification);

        IndependentSubmissionResult result = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.UNDECIDABLE_DERIVATIVE,
                ApplyScriptData.UNDECIDABLE_DERIVATIVE, null);

        assertInstanceOf(IndependentSubmissionResult.ReplacementRequired.class, result,
                "evaluator disagreement on an uncertain expression must be Inconclusive, not a failure");
        assertEquals(IndependentSubmissionFlow.REPLACEMENT_MESSAGE,
                ((IndependentSubmissionResult.ReplacementRequired) result).learnerMessage());
        assertTrue(passed.harness().flowStore().allEvidence().isEmpty(),
                "an Inconclusive Independent submission must never create evidence");
        assertEquals(1, assessment.contexts().size());
        assertEquals(1, verification.contexts().size());
        assertEquals(assessment.contexts().get(0), verification.contexts().get(0),
                "both isolated evaluators must receive the identical raw and confirmed inputs");
    }

    @Test
    void aDiagnosticNeverCreatesIndependentEvidence() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        harness.flow().submitDiagnostic(FLOW_ID, diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a passing Diagnostic must never create Learning Evidence");
    }

    @Test
    void aDiagnosticPassingViaAnApplicableRationaleNeverCreatesIndependentEvidence() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.APPLICABLE)));
        Harness harness = flow(generation, verifier, assessment, new ScriptedResponseVerificationModel(List.of()));

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE,
                ApplyScriptData.WRONG_DERIVATIVE,
                ApplyScriptData.APPLICABLE_RATIONALE);

        assertInstanceOf(DiagnosticSubmissionResult.Passed.class, result);
        assertTrue(harness.flowStore().allEvidence().isEmpty(),
                "a rationale-passing Diagnostic must never create Learning Evidence");
    }

    @Test
    void theIndependentResultExposesOnlyASafeContinueOrEndState() {
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(
                ApplyScriptData.responseAssessment(FinalExpressionJudgment.NOT_REQUESTED, RationaleJudgment.NOT_PROVIDED)));
        ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of());
        PassedIndependent passed = passDiagnosticAndDeliverIndependent(assessment, verification);
        IndependentSubmissionFlow flow = independentFlow(passed.harness(), assessment, verification);

        IndependentSubmissionResult result = flow.submitIndependent(new cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.FlowRecord(
                FLOW_ID, LEARNER_ID, CONCEPT_ID, cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.AWAITING_LEARNER_INPUT,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.INDEPENDENT_TEST, CLOCK.instant()),
                
                passed.independentAttemptId(), ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION,
                ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION, null);

        assertInstanceOf(IndependentSubmissionResult.EvidenceAccepted.class, result);
        IndependentSubmissionResult.EvidenceAccepted accepted =
                (IndependentSubmissionResult.EvidenceAccepted) result;
        String message = accepted.learnerMessage();
        assertFalse(message.contains(ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION), "no answer facts");
        assertFalse(message.contains("正确"), "no correctness conclusion");
        assertFalse(message.contains("错误"), "no failure feedback");
        assertFalse(message.contains("答案"), "no answer key");
        assertFalse(message.contains("openstax"), "no source identities");
        assertFalse(message.contains("fingerprint"), "no fingerprints");
        assertFalse(message.contains("assessment"), "no assessment conclusion");
        assertFalse(message.contains(accepted.evidence().id().toString()), "no audit identifiers");
        assertFalse(message.contains(passed.independentAttemptId().toString()), "no attempt identifiers");
    }

    private PassedIndependent passDiagnosticAndDeliverIndependent(
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification
    ) {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        Harness harness = flow(generation, verifier, assessment, verification);
        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic(FLOW_ID);
        DiagnosticSubmissionResult passed = harness.flow().submitDiagnostic(FLOW_ID, 
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL,
                null);
        return new PassedIndependent(
                harness,
                ((DiagnosticSubmissionResult.Passed) passed).independentAttempt().attemptId());
    }

    private record PassedIndependent(Harness harness, UUID independentAttemptId) {
    }

    private static final UUID LEARNER_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID FLOW_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID CONCEPT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    private IndependentSubmissionFlow independentFlow(
            Harness harness,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification
    ) {
        return new IndependentSubmissionFlow(
                harness.artifacts(), harness.flowStore(), assessment, verification,
                new ReviewTaskScheduler((ReviewTaskStore) harness.flowStore()), CLOCK);
    }

    private Harness flow(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment,
            ScriptedResponseVerificationModel verification
    ) {
        ArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(CLOCK);
        ApplyProfileExecutor executor = new ApplyProfileExecutor(stack, generation, verifier, artifacts);
        DiagnosticFlow flow = new DiagnosticFlow(executor, artifacts, flowStore, assessment, verification,
                DiagnosticApplyFixture.diagnosticContext(),
                IndependentApplyFixture.independentContext(),
                CLOCK);
        return new Harness(flow, artifacts, flowStore);
    }

    private record Harness(DiagnosticFlow flow, ArtifactStore artifacts, LearningFlowStore flowStore) {
    }

    private BundleStack referenceStack() {
        return ReferenceBundles.stack();
    }
}
