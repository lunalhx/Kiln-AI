package cn.lunalhx.ai.kilnai.domain.apply;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleLoader;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleRegistry;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExposureLedger;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryExposureLedger;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryTaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyProfileContractTest {

    private final ApplyExecutionContext context = DiagnosticApplyFixture.diagnosticContext();
    private final BundleRegistry registry = referenceRegistry();

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void deliversAValidDiagnosticTaskWithAPrivateFreeLearnerProjection() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        TaskAttemptStore store = new InMemoryTaskAttemptStore(Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneOffset.UTC));
        ApplyProfileExecutor executor = new ApplyProfileExecutor(registry, generation, verifier, store);

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
        TaskAttemptStore store = new InMemoryTaskAttemptStore(Clock.systemUTC());
        new ApplyProfileExecutor(registry, generation, verifier, store).deliver(context);

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
        TaskAttemptStore store = new InMemoryTaskAttemptStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(registry, generation, verifier, store);

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
        TaskAttemptStore store = new InMemoryTaskAttemptStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(registry, generation, verifier, store);

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
        TaskAttemptStore store = new InMemoryTaskAttemptStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(registry, generation, verifier, store);

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
        TaskAttemptStore store = new InMemoryTaskAttemptStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(registry, generation, verifier, store);

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
        TaskAttemptStore store = new InMemoryTaskAttemptStore(Clock.systemUTC());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(registry, generation, verifier, store);

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
    void theFingerprintIsDeterministicallyDerivedFromValidatedTaskFacts() {
        TaskAttemptStore firstStore = new InMemoryTaskAttemptStore(Clock.systemUTC());
        new ApplyProfileExecutor(registry,
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())), firstStore)
                .deliver(context);
        TaskAttemptStore secondStore = new InMemoryTaskAttemptStore(Clock.systemUTC());
        new ApplyProfileExecutor(registry,
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())), secondStore)
                .deliver(context);

        assertEquals(
                firstStore.allPackages().get(0).privateAssessorProjection().taskFingerprint().value(),
                secondStore.allPackages().get(0).privateAssessorProjection().taskFingerprint().value(),
                "the Profile, not the model, must own the derived Task Fingerprint");
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
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(
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
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(
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
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(RationaleJudgment.APPLICABLE));
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(
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
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of(RationaleJudgment.NOT_APPLICABLE));
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(
                diagnostic.attempt().attemptId(),
                ApplyScriptData.WRONG_DERIVATIVE,
                ApplyScriptData.WRONG_DERIVATIVE,
                "我猜的");

        assertInstanceOf(DiagnosticSubmissionResult.Failed.class, result);
        DiagnosticSubmissionResult.Failed failed = (DiagnosticSubmissionResult.Failed) result;
        assertEquals(DiagnosticFlow.SAFE_END_MESSAGE, failed.safeEndMessage());
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
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        UUID attemptId = diagnostic.attempt().attemptId();
        harness.flow().submitDiagnostic(attemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        DiagnosticSubmissionResult replayed = harness.flow().submitDiagnostic(
                attemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.Ignored.class, replayed);
        assertEquals(DiagnosticSubmissionResult.IgnoreReason.ALREADY_SUBMITTED,
                ((DiagnosticSubmissionResult.Ignored) replayed).reason());
        assertEquals(0, assessment.contexts().size(), "a replay must never trigger a second evaluation");
        assertEquals(2, generation.calls().size(), "a replay must never deliver a second Independent task");
        assertEquals(1, harness.store().allPackages().stream()
                .filter(package_ -> package_.attemptPurpose() == AttemptPurpose.DIAGNOSTIC).count());
    }

    @Test
    void aStaleSubmissionForAnUnknownAttemptIsIgnoredWithoutEvaluation() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson()));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment);

        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(
                UUID.randomUUID(), ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.Ignored.class, result);
        assertEquals(DiagnosticSubmissionResult.IgnoreReason.ATTEMPT_NOT_FOUND,
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
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        UUID attemptId = diagnostic.attempt().attemptId();

        DiagnosticSubmissionResult mismatched = harness.flow().submitDiagnostic(
                attemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL + " + 1", null);
        assertInstanceOf(DiagnosticSubmissionResult.NotSubmittable.class, mismatched);
        assertEquals(DiagnosticSubmissionResult.RejectionReason.CONFIRMATION_MISMATCH,
                ((DiagnosticSubmissionResult.NotSubmittable) mismatched).reason());

        DiagnosticSubmissionResult unparseable = harness.flow().submitDiagnostic(
                attemptId, "12*y^2 + 1", "12*y^2 + 1", null);
        assertInstanceOf(DiagnosticSubmissionResult.NotSubmittable.class, unparseable);
        assertEquals(DiagnosticSubmissionResult.RejectionReason.UNPARSEABLE_RAW,
                ((DiagnosticSubmissionResult.NotSubmittable) unparseable).reason());

        assertTrue(assessment.contexts().isEmpty(), "no evaluation before a valid confirmed submission");
        assertEquals(AttemptStatus.OPEN,
                harness.store().findAttempt(attemptId).orElseThrow().status(),
                "the attempt must remain open for correction");

        DiagnosticSubmissionResult corrected = harness.flow().submitDiagnostic(
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
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        harness.flow().submitDiagnostic(diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        String independentContextJson = generation.calls().get(1).contextJson();
        String diagnosticFingerprint = harness.store().findPackage(diagnostic.attempt().taskPackageId())
                .orElseThrow().privateAssessorProjection().taskFingerprint().value();

        assertTrue(independentContextJson.contains("\"attempt_purpose\":\"independent_test\""),
                "the Independent invocation must carry the Independent blueprint");
        assertTrue(independentContextJson.contains("\"exposed_task_fingerprints\":[\"" + diagnosticFingerprint + "\"]"),
                "the Independent invocation must carry every exposed fingerprint");
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
    void anIndependentTaskExcludesPreviouslyExposedTaskFingerprints() {
        ScriptedApplyGenerationModel generation = new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(),
                ApplyScriptData.taskReadyJson(ApplyScriptData.INDEPENDENT_TASK_TEXT,
                        ApplyScriptData.INDEPENDENT_EXPECTED_EXPRESSION)));
        ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(
                ApplyScriptData.passVerdict(), ApplyScriptData.passVerdict()));
        ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        String diagnosticFingerprint = harness.store().findPackage(diagnostic.attempt().taskPackageId())
                .orElseThrow().privateAssessorProjection().taskFingerprint().value();
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.Passed.class, result);
        DiagnosticSubmissionResult.Passed passed = (DiagnosticSubmissionResult.Passed) result;
        String independentFingerprint = harness.store().findPackage(passed.independentAttempt().taskPackageId())
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
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        DiagnosticSubmissionResult passed = harness.flow().submitDiagnostic(
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);
        UUID independentAttemptId = ((DiagnosticSubmissionResult.Passed) passed).independentAttempt().attemptId();

        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(
                independentAttemptId, ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.Ignored.class, result);
        assertEquals(DiagnosticSubmissionResult.IgnoreReason.NOT_A_DIAGNOSTIC_ATTEMPT,
                ((DiagnosticSubmissionResult.Ignored) result).reason());
        assertEquals(AttemptStatus.OPEN, harness.store().findAttempt(independentAttemptId).orElseThrow().status(),
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
        Harness harness = flow(generation, verifier, assessment);

        ApplyDeliveryResult.Delivered diagnostic = (ApplyDeliveryResult.Delivered) harness.flow().startDiagnostic();
        DiagnosticSubmissionResult result = harness.flow().submitDiagnostic(
                diagnostic.attempt().attemptId(),
                ApplyScriptData.UNICODE_CORRECT_DERIVATIVE,
                ApplyScriptData.UNICODE_CORRECT_CANONICAL, null);

        assertInstanceOf(DiagnosticSubmissionResult.IndependentUnavailable.class, result);
        DiagnosticSubmissionResult.IndependentUnavailable unavailable =
                (DiagnosticSubmissionResult.IndependentUnavailable) result;
        assertEquals(TaskUnavailableReason.SOURCE_GAP, unavailable.reason());
        assertEquals(ApplyDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE, unavailable.learnerMessage());
    }

    private Harness flow(
            ScriptedApplyGenerationModel generation,
            ScriptedTaskVerifier verifier,
            ScriptedAssessmentModel assessment
    ) {
        TaskAttemptStore store = new InMemoryTaskAttemptStore(CLOCK);
        ExposureLedger ledger = new InMemoryExposureLedger();
        ApplyProfileExecutor executor = new ApplyProfileExecutor(registry, generation, verifier, store);
        DiagnosticFlow flow = new DiagnosticFlow(executor, store, ledger, assessment,
                DiagnosticApplyFixture.diagnosticContext(),
                IndependentApplyFixture.independentContext(),
                CLOCK);
        return new Harness(flow, store, ledger);
    }

    private record Harness(DiagnosticFlow flow, TaskAttemptStore store, ExposureLedger ledger) {
    }

    private BundleRegistry referenceRegistry() {
        BundleRegistry registry = new BundleRegistry();
        BundleLoader loader = new BundleLoader();
        ApplyProfile.FIXED_STACK.forEach(pinned -> {
            int at = pinned.lastIndexOf('@');
            registry.register(loader.load(pinned.substring(0, at)));
        });
        return registry;
    }
}
