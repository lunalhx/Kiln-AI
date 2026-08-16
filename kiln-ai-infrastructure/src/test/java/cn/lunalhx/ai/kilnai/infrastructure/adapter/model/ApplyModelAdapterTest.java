package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplyModelAdapterTest {

    private static final String SECRET_ENV = "KILN_APPLY_ADAPTER_TEST_KEY";
    private static final String SECRET = "sk-apply-test-secret";

    @Test
    void generationReturnsRawModelTextAndNeverRegistersTools() {
        ScriptedChatModel model = new ScriptedChatModel("{\"outcome\":\"source_gap\"}");
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.generate("compiled prompt", "{\"schema\":\"apply_execution_context/v1\"}");

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
    void taskVerificationParsesAPassVerdict() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"schema":"task_verification/v1","verdict":"pass",
                 "checks":{"answer_correctness":"pass","rubric_alignment":"pass","source_grounding":"pass",
                 "blueprint_compliance":"pass","learner_boundary":"pass"},"reason_codes":[]}
                """);
        ApplyModelAdapter adapter = adapter(model);

        TaskVerificationVerdict verdict = adapter.verify((TaskPackage) null, (ApplyExecutionContext) null);

        assertEquals(TaskVerificationVerdict.Verdict.PASS, verdict.verdict());
        assertTrue(verdict.passed());
        assertEquals(5, verdict.checks().size());
        assertTrue(verdict.checks().values().stream()
                .allMatch(result -> result == TaskVerificationVerdict.CheckResult.PASS));
    }

    @Test
    void taskVerificationParsesARejectWithReasonCodes() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"schema":"task_verification/v1","verdict":"reject",
                 "checks":{"answer_correctness":"reject","rubric_alignment":"pass","source_grounding":"pass",
                 "blueprint_compliance":"pass","learner_boundary":"pass"},
                 "reason_codes":["task_answer_inconsistent"]}
                """);
        ApplyModelAdapter adapter = adapter(model);

        TaskVerificationVerdict verdict = adapter.verify((TaskPackage) null, (ApplyExecutionContext) null);

        assertEquals(TaskVerificationVerdict.Verdict.REJECT, verdict.verdict());
        assertTrue(verdict.reasonCodes().contains("task_answer_inconsistent"));
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

        ResponseAssessment assessment = adapter.assess(context);
        ResponseAssessment verified = adapter.verify(context);

        assertEquals(FinalExpressionJudgment.EQUIVALENT, assessment.finalExpressionJudgment());
        assertEquals(RationaleJudgment.NOT_PROVIDED, assessment.rationaleJudgment());
        assertEquals(assessment, verified);
        assertEquals(2, model.prompts.size());
        assertTrue(model.prompts.stream().allMatch(prompt -> prompt.getContents().contains("# Response Assessment")));
    }

    @Test
    void aWrongContractSchemaIsRejected() {
        ScriptedChatModel model = new ScriptedChatModel("{\"schema\":\"task_verification/v2\",\"verdict\":\"pass\"}");
        ApplyModelAdapter adapter = adapter(model);

        ApplicationException error = assertThrows(ApplicationException.class, () -> adapter.verify((TaskPackage) null, (ApplyExecutionContext) null));
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, error.errorCode());
    }

    @Test
    void teachBackGenerationReturnsRawModelTextWithTheProfileSystemPrompt() {
        ScriptedChatModel model = new ScriptedChatModel("{\"outcome\":\"source_gap\"}");
        ApplyModelAdapter adapter = adapter(model);

        String raw = adapter.generate("teach-back compiled prompt", "{\"schema\":\"teach_back_execution_context/v1\"}");

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

        TaskVerificationVerdict verdict = adapter.verify(
                (cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage) null,
                (cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext) null);

        assertEquals(TaskVerificationVerdict.Verdict.REJECT, verdict.verdict());
        assertTrue(verdict.reasonCodes().contains("ambiguous_prompt"));
        assertTrue(model.prompts.getFirst().getInstructions().get(0).getText()
                .contains("# Teach-back Task Verifier"));
    }

    @Test
    void teachBackAssessmentParsesTheThreeDimensionJudgments() {
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

        cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment assessment =
                adapter.assess(context);

        assertEquals(cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.PASS,
                assessment.ruleIdentification());
        assertEquals(cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.FAIL,
                assessment.applicabilityExplanation());
        assertEquals(cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                assessment.stepsResultCoherence());
        assertEquals(cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.TeachBackOutcome.INCONCLUSIVE,
                assessment.outcome(),
                "any inconclusive dimension makes the whole judgment inconclusive");
        assertTrue(model.prompts.getFirst().getInstructions().get(0).getText()
                .contains("# Teach-back Assessment"));
    }

    @Test
    void missingCatalogFailsClosed() {
        OperatorCatalog catalog = new OperatorCatalog(List.of(), "acme/gpt-strong", "acme/gpt-small");
        ApplyModelAdapter adapter = new ApplyModelAdapter(
                catalog, (binding, apiKey) -> ChatClient.create(new ScriptedChatModel("{}")), secrets());

        ApplicationException error = assertThrows(ApplicationException.class,
                () -> adapter.generate("prompt", "{}"));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("catalog"));
    }

    private static ApplyModelAdapter adapter(ScriptedChatModel model) {
        return new ApplyModelAdapter(catalog(), (binding, apiKey) -> ChatClient.create(model), secrets());
    }

    private static OperatorCatalog catalog() {
        return new OperatorCatalog(List.of(provider()), "acme/gpt-strong", "acme/gpt-small");
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
