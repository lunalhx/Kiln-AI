package cn.lunalhx.ai.kilnai.domain.apply;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleLoader;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleRegistry;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryTaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyProfileContractTest {

    private final ApplyExecutionContext context = DiagnosticApplyFixture.diagnosticContext();
    private final BundleRegistry registry = referenceRegistry();

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
