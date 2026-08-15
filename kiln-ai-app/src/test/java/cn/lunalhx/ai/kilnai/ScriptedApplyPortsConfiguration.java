package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;

@TestConfiguration
public class ScriptedApplyPortsConfiguration {

    public static final String DIAGNOSTIC_TASK = "设 f(x) = 4x³ − 3x² + 7x − 5，求 f'(x)。";
    public static final String DIAGNOSTIC_EXPECTED = "12*x^2 - 6*x + 7";
    public static final String INDEPENDENT_TASK = "设 g(x) = 5x³ − 2x + 1，求 g'(x)。";
    public static final String INDEPENDENT_EXPECTED = "15*x^2 - 2";
    public static final String REVIEW_TASK = "设 h(x) = 2x⁴ − 3x² + 5，求 h'(x)。";
    public static final String REVIEW_EXPECTED = "8*x^3 - 6*x";

    @Bean
    @Primary
    ApplyGenerationPort scriptedApplyGeneration() {
        return (compiledSystemPrompt, executionContextJson) -> {
            if (executionContextJson.contains("\"attempt_purpose\":\"review\"")) {
                return taskReadyJson(REVIEW_TASK, REVIEW_EXPECTED);
            }
            return executionContextJson.contains("\"attempt_purpose\":\"independent_test\"")
                    ? taskReadyJson(INDEPENDENT_TASK, INDEPENDENT_EXPECTED)
                    : taskReadyJson(DIAGNOSTIC_TASK, DIAGNOSTIC_EXPECTED);
        };
    }

    @Bean
    @Primary
    TaskVerifierPort scriptedApplyTaskVerifier() {
        return new TaskVerifierPort() {
            @Override
            public TaskVerificationVerdict verify(TaskPackage taskPackage, ApplyExecutionContext context) {
                return new TaskVerificationVerdict(
                        TaskVerificationVerdict.SCHEMA,
                        TaskVerificationVerdict.Verdict.PASS,
                        Map.of(
                                "answer_correctness", TaskVerificationVerdict.CheckResult.PASS,
                                "rubric_alignment", TaskVerificationVerdict.CheckResult.PASS,
                                "source_grounding", TaskVerificationVerdict.CheckResult.PASS,
                                "blueprint_compliance", TaskVerificationVerdict.CheckResult.PASS,
                                "learner_boundary", TaskVerificationVerdict.CheckResult.PASS),
                        List.of());
            }
        };
    }

    @Bean
    @Primary
    AssessmentPort scriptedApplyAssessment() {
        return context -> new ResponseAssessment(
                ResponseAssessment.SCHEMA,
                FinalExpressionJudgment.NOT_REQUESTED,
                RationaleJudgment.NOT_PROVIDED,
                List.of());
    }

    @Bean
    @Primary
    ResponseVerificationPort scriptedApplyResponseVerification() {
        return context -> {
            throw new IllegalStateException("scripted response verification must never be invoked");
        };
    }

    public static String taskReadyJson(String taskText, String expression) {
        return """
                {
                  "schema": "apply_generation/v1",
                  "outcome": "task_ready",
                  "learner_task_text": "%s",
                  "private_assessor_facts": {
                    "proposed_expected_answer": { "expression": "%s" },
                    "rubric_mapping": [
                      { "mastery_criterion_id": "differentiate-polynomial", "evidence_channels": ["final_derivative", "optional_rule_rationale"] }
                    ],
                    "source_trace": [
                      { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" }
                    ],
                    "equivalence_declaration": { "kind": "symbolic_expression", "variables": ["x"], "domain": "real" }
                  }
                }
                """.formatted(taskText, expression);
    }
}
