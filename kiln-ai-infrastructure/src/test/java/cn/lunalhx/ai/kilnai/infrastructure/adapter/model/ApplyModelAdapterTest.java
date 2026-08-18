package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyModelAdapterTest {

    private static final String SECRET_ENV = "KILN_APPLY_ADAPTER_TEST_KEY";
    private static final String SECRET = "sk-apply-test-secret";
    private static final int OUTPUT_CEILING = 2048;

    private static final ModelProfile PROFILE = new ModelProfile(
            new ModelProfile.ModelBinding(
                    OperatorCatalog.OPENAI_COMPATIBLE, "https://api.acme.test/v1",
                    "acme", "gpt-strong", SECRET_ENV),
            new ModelProfile.ModelBinding(
                    OperatorCatalog.OPENAI_COMPATIBLE, "https://api.acme.test/v1",
                    "acme", "gpt-small", SECRET_ENV),
            OUTPUT_CEILING);

    @Test
    void generationReturnsRawModelTextAndNeverRegistersTools() {
        ScriptedChatModel model = new ScriptedChatModel("{\"outcome\":\"source_gap\"}");
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.generate(PROFILE, "compiled prompt", "{\"schema\":\"apply_execution_context/v1\"}");

        assertEquals("{\"outcome\":\"source_gap\"}", raw);
        assertEquals(List.of("system", "user"), model.prompts.getFirst().getInstructions().stream()
                .map(message -> message.getMessageType().getValue()).toList());
        assertEquals("compiled prompt", model.prompts.getFirst().getInstructions().get(0).getText());
        assertEquals("{\"schema\":\"apply_execution_context/v1\"}",
                model.prompts.getFirst().getInstructions().get(1).getText());
        assertEquals(1, model.prompts.size());
        assertTrue(model.prompts.stream().allMatch(ApplyModelAdapterTest::hasNoTools),
                "the Apply adapter must never register tools");
    }

    @Test
    void generationReturnsProviderTextWithoutRepairingTheModelContract() {
        String providerText = """
                ```json
                {"outcome":"source_gap"}
                ```
                """;
        ScriptedChatModel model = new ScriptedChatModel(providerText);
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.generate(PROFILE, "compiled prompt", "{}");

        assertEquals(providerText, raw);
    }

    @Test
    void taskVerificationReturnsRawClosedJson() {
        String rawJson = """
                {"schema":"task_verification/v1","verdict":"pass",
                 "checks":{"answer_correctness":"pass","rubric_alignment":"pass","source_grounding":"pass",
                 "blueprint_compliance":"pass","learner_boundary":"pass"},"reason_codes":[]}
                """;
        ScriptedChatModel model = new ScriptedChatModel(rawJson);
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.verify(PROFILE, (TaskPackage) null, (ApplyExecutionContext) null);

        assertTrue(raw.contains("\"verdict\":\"pass\""));
        assertTrue(raw.contains("task_verification/v1"));
    }

    @Test
    void taskVerificationReturnsRawRejectJson() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"schema":"task_verification/v1","verdict":"reject",
                 "checks":{"answer_correctness":"reject","rubric_alignment":"pass","source_grounding":"pass",
                 "blueprint_compliance":"pass","learner_boundary":"pass"},
                 "reason_codes":["task_answer_inconsistent"]}
                """);
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.verify(PROFILE, (TaskPackage) null, (ApplyExecutionContext) null);

        assertTrue(raw.contains("task_answer_inconsistent"));
        assertTrue(raw.contains("\"verdict\":\"reject\""));
    }

    @Test
    void responseAssessmentParsesTheClosedJudgments() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"schema":"response_assessment/v1",
                 "final_expression_judgment":"equivalent","rationale_judgment":"not_provided","reason_codes":[]}
                """);
        ApplyModelAdapter adapter = adapter(model);
        ResponseAssessmentContext context = new ResponseAssessmentContext(
                "task", "12*x^2 - 6*x + 7", "12x²−6x+7", "12x²−6x+7", "",
                AttemptPurpose.INDEPENDENT_TEST, EquivalenceOutcome.CANNOT_DECIDE);

        String assessed = adapter.assess(PROFILE, context);
        String verified = adapter.verifyResponse(PROFILE, context);

        assertTrue(assessed.contains("\"final_expression_judgment\":\"equivalent\""));
        assertEquals(assessed, verified);
        assertEquals(2, model.prompts.size());
        assertTrue(model.prompts.stream().allMatch(prompt -> prompt.getContents().contains("# Response Assessment")));
    }

    @Test
    void aWrongContractSchemaIsReturnedAsRawContentNotAProviderFailure() {
        ScriptedChatModel model = new ScriptedChatModel("{\"schema\":\"task_verification/v2\",\"verdict\":\"pass\"}");
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.verify(PROFILE, (TaskPackage) null, (ApplyExecutionContext) null);
        assertEquals("{\"schema\":\"task_verification/v2\",\"verdict\":\"pass\"}", raw);
    }

    @Test
    void teachBackGenerationReturnsRawModelTextWithTheProfileSystemPrompt() {
        ScriptedChatModel model = new ScriptedChatModel("{\"outcome\":\"source_gap\"}");
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.generate(PROFILE, "teach-back compiled prompt", "{\"schema\":\"teach_back_execution_context/v1\"}");

        assertEquals("{\"outcome\":\"source_gap\"}", raw);
        assertEquals("teach-back compiled prompt",
                model.prompts.getFirst().getInstructions().get(0).getText());
    }

    @Test
    void teachBackVerificationParsesTheClosedVerdict() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"schema":"task_verification/v1","verdict":"reject",
                 "checks":{"answer_clarity":"reject","rubric_alignment":"pass","source_grounding":"pass",
                 "anchor_grounding":"pass","learner_boundary":"pass"},
                 "reason_codes":["ambiguous_prompt"]}
                """);
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.verify(
                PROFILE,
                (cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage) null,
                (cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext) null);

        assertTrue(raw.contains("ambiguous_prompt"));
        assertTrue(raw.contains("\"verdict\":\"reject\""));
        assertTrue(model.prompts.getFirst().getInstructions().get(0).getText()
                .contains("# Teach-back Task Verifier"));
    }

    @Test
    void teachBackAssessmentReturnsRawThreeDimensionJson() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"schema":"teach_back_assessment/v1",
                 "rule_identification":"pass","applicability_explanation":"fail",
                 "steps_result_coherence":"inconclusive","reason_codes":["rule_not_identified"]}
                """);
        ApplyModelAdapter adapter = adapter(model);
        cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessmentContext context =
                new cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessmentContext(
                        "请解释刚才的解题思路。", "完整解答：p'(x) = 18x² − 4。", "用了幂法则与和差法则。",
                        AttemptPurpose.PRACTICE);

        String raw = adapter.assess(PROFILE, context);

        assertTrue(raw.contains("rule_not_identified"));
        assertTrue(raw.contains("\"steps_result_coherence\":\"inconclusive\""));
        assertTrue(model.prompts.getFirst().getInstructions().get(0).getText()
                .contains("# Teach-back Assessment"));
    }

    @Test
    void aMissingCatalogFailsClosedAtFlowStart() {
        OperatorCatalog catalog = new OperatorCatalog(List.of(), "acme/gpt-strong", "acme/gpt-small", OUTPUT_CEILING);

        ApplicationException error = assertThrows(ApplicationException.class,
                () -> catalog.resolve(secrets()));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("catalog"));
    }

    @Test
    void aMissingSecretFailsClosedAtCallTime() {
        OperatorCatalog catalog = new OperatorCatalog(List.of(provider()), "acme/gpt-strong", "acme/gpt-small", OUTPUT_CEILING);
        ApplyModelAdapter adapter = new ApplyModelAdapter(
                catalog, (binding, apiKey, maxTokens) -> ChatClient.create(new ScriptedChatModel("{}")),
                name -> null);

        ApplicationException error = assertThrows(ApplicationException.class,
                () -> adapter.generate(PROFILE, "prompt", "{}"));
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, error.errorCode());
        assertTrue(error.getMessage().contains("secret"));
    }

    @Test
    void pedagogyAndClarificationUseTheFrozenSmallSlotWithTheOutputCeiling() {
        ScriptedChatModel model = new ScriptedChatModel(
                "{\"schema\":\"clarification_classification/v1\",\"classification\":\"procedural\"}");
        CapturingFactory factory = new CapturingFactory(model);
        ApplyModelAdapter adapter = new ApplyModelAdapter(catalog(), factory, secrets());

        adapter.generatePlan(PROFILE, "pedagogy compiled prompt", "{\"schema\":\"pedagogy_execution_context/v1\"}");
        assertEquals(1, factory.calls().size());
        assertEquals("gpt-small", factory.calls().getFirst().binding().modelId(),
                "the Pedagogy Agent must always use the Small slot of the frozen profile");
        assertEquals(OUTPUT_CEILING, factory.calls().getFirst().maxTokens(),
                "the operator-owned output ceiling must be enforced on every model call");

        adapter.classify(PROFILE, "这道题怎么求导？", "求 p(x) 的导数。");
        assertEquals(2, factory.calls().size());
        assertEquals("gpt-small", factory.calls().get(1).binding().modelId(),
                "the Clarification classifier must always use the Small slot of the frozen profile");
    }

    @Test
    void pedagogyAndClassifierNeverSeeToolsOrTheSecretValue() {
        ScriptedChatModel model = new ScriptedChatModel("{\"schema\":\"pedagogy_plan/v1\"}");
        CapturingFactory factory = new CapturingFactory(model);
        ApplyModelAdapter adapter = new ApplyModelAdapter(catalog(), factory, secrets());

        adapter.generatePlan(PROFILE, "pedagogy compiled prompt", "{\"schema\":\"pedagogy_execution_context/v1\"}");

        assertEquals(1, model.prompts.size());
        assertTrue(model.prompts.stream().allMatch(ApplyModelAdapterTest::hasNoTools),
                "the adapter must never register tools");
        assertFalse(factory.calls().getFirst().binding().secretEnvVar().contains("sk-"),
                "the frozen profile carries the environment variable name, never the secret value");
    }

    @Test
    void clarificationClassificationReturnsRawClosedJson() {
        ScriptedChatModel model = new ScriptedChatModel(
                "{\"schema\":\"clarification_classification/v1\",\"classification\":\"procedural\"}");
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.classify(PROFILE, "符号输入方式是什么？", "请填写最终答案。");
        assertTrue(raw.contains("\"classification\":\"procedural\""));
        assertTrue(model.prompts.getFirst().getInstructions().get(0).getText()
                .contains("# Clarification Classifier"));
    }

    @Test
    void anUnknownClarificationClassificationIsReturnedAsRawContent() {
        ScriptedChatModel model = new ScriptedChatModel(
                "{\"schema\":\"clarification_classification/v1\",\"classification\":\"guess\"}");
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.classify(PROFILE, "帮我解题", "求导数。");
        assertTrue(raw.contains("\"classification\":\"guess\""));
    }

    @Test
    void missingOutputTokenCeilingFailsClosed() {
        ApplicationException error = assertThrows(ApplicationException.class,
                () -> new OperatorCatalog(List.of(provider()), "acme/gpt-strong", "acme/gpt-small", 0));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("output token ceiling"));
    }

    /** Captures the binding, secret, and output ceiling of every client factory call. */
    private static final class CapturingFactory implements ChatClientFactory {

        private final ScriptedChatModel model;
        private final List<Call> calls = new java.util.concurrent.CopyOnWriteArrayList<>();

        private CapturingFactory(ScriptedChatModel model) {
            this.model = model;
        }

        @Override
        public ChatClient create(ModelBindingSnapshot binding, String apiKey, int maxTokens) {
            calls.add(new Call(binding, apiKey, maxTokens));
            return ChatClient.create(model);
        }

        private List<Call> calls() {
            return List.copyOf(calls);
        }

        private record Call(ModelBindingSnapshot binding, String apiKey, int maxTokens) {
        }
    }

    private static ApplyModelAdapter adapter(ScriptedChatModel model) {
        return new ApplyModelAdapter(catalog(), (binding, apiKey, maxTokens) -> ChatClient.create(model), secrets());
    }

    private static OperatorCatalog catalog() {
        return new OperatorCatalog(List.of(provider()), "acme/gpt-strong", "acme/gpt-small", OUTPUT_CEILING);
    }

    private static CatalogProvider provider() {
        return new CatalogProvider(
                "acme",
                OperatorCatalog.OPENAI_COMPATIBLE,
                "https://api.acme.test/v1",
                SECRET_ENV,
                List.of("gpt-strong", "gpt-small")
        );
    }

    private static Function<String, String> secrets() {
        return name -> SECRET_ENV.equals(name) ? SECRET : null;
    }

    private static boolean hasNoTools(Prompt prompt) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions options) {
            return options.getToolCallbacks() == null || options.getToolCallbacks().isEmpty();
        }
        return true;
    }

    private static final class ScriptedChatModel implements ChatModel {

        private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
        private final String response;

        private ScriptedChatModel(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return ChatResponse.builder()
                    .generations(List.of(new Generation(new AssistantMessage(response))))
                    .metadata(ChatResponseMetadata.builder()
                            .model("scripted")
                            .usage(new DefaultUsage(10, 5))
                            .build())
                    .build();
        }
    }
}
