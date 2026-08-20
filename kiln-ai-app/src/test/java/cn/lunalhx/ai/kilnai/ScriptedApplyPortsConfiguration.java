package cn.lunalhx.ai.kilnai;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.RationaleAssessmentPort;
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
    public static final String REVIEW_TASK_2 = "设 p(x) = 3x⁵ − 4x + 2，求 p'(x)。";
    public static final String REVIEW_EXPECTED_2 = "15*x^4 - 4";
    public static final String REVIEW_TASK_3 = "设 q(x) = 6x⁴ + 5x² − 3，求 q'(x)。";
    public static final String REVIEW_EXPECTED_3 = "24*x^3 + 10*x";
    public static final String REVIEW_TASK_4 = "设 r(x) = 7x³ − 2x + 9，求 r'(x)。";
    public static final String REVIEW_EXPECTED_4 = "21*x^2 - 2";

    /** The rationale value that scripts the assessment to judge a clear contradiction. */
    public static final String CONTRADICTORY_RATIONALE_MARKER = "我用了乘积法则，但答案是错误的规则推导";

    /** The scripted frozen Model Profile of the app contract tests. */
    public static ModelProfile scriptedModelProfile() {
        return new ModelProfile(
                new ModelProfile.ModelBinding(
                        "openai-compatible",
                        "https://api.test/v1",
                        "acme",
                        "scripted-strong",
                        "TEST_STRONG_SECRET"),
                new ModelProfile.ModelBinding(
                        "openai-compatible",
                        "https://api.test/v1",
                        "acme",
                        "scripted-small",
                        "TEST_SMALL_SECRET"),
                2048);
    }

    @Bean
    @Primary
    OperatorModelProfilePort scriptedOperatorModelProfile() {
        return ScriptedApplyPortsConfiguration::scriptedModelProfile;
    }

    @Bean
    @Primary
    ApplyGenerationPort scriptedApplyGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            if (executionContextJson.contains("\"attempt_purpose\":\"review\"")) {
                // The novelty exclusions already carry every previously
                // exposed task fingerprint; their count selects the Review
                // of the cadence, so the same call is always deterministic.
                int reviewNumber = exposedTaskCount(executionContextJson) - 2 + 1;
                return switch (reviewNumber) {
                    case 1 -> taskReadyJson(REVIEW_TASK, REVIEW_EXPECTED);
                    case 2 -> taskReadyJson(REVIEW_TASK_2, REVIEW_EXPECTED_2);
                    case 3 -> taskReadyJson(REVIEW_TASK_3, REVIEW_EXPECTED_3);
                    default -> taskReadyJson(REVIEW_TASK_4, REVIEW_EXPECTED_4);
                };
            }
            return executionContextJson.contains("\"attempt_purpose\":\"independent_test\"")
                    ? taskReadyJson(INDEPENDENT_TASK, INDEPENDENT_EXPECTED)
                    : taskReadyJson(DIAGNOSTIC_TASK, DIAGNOSTIC_EXPECTED);
        };
    }

    static int exposedTaskCount(String executionContextJson) {
        String marker = "\"exposed_task_fingerprints\":[";
        int start = executionContextJson.indexOf(marker);
        int end = executionContextJson.indexOf(']', start);
        String contents = executionContextJson.substring(start + marker.length(), end).trim();
        return contents.isEmpty() ? 0 : contents.split(",").length;
    }

    /** The shared scripted Task Verification pass verdict of the app contract tests. */
    public static TaskVerificationVerdict passVerdict() {
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

    @Bean
    @Primary
    TaskVerifierPort scriptedApplyTaskVerifier() {
        return new TaskVerifierPort() {
            @Override
            public TaskVerificationVerdict verify(
                    ModelProfile profile,
                    TaskPackage taskPackage,
                    ApplyExecutionContext context
            ) {
                return passVerdict();
            }
        };
    }

    @Bean
    @Primary
    AssessmentPort scriptedApplyAssessment() {
        return (profile, context) -> {
            if (context.purpose() == cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose.REVIEW
                    && CONTRADICTORY_RATIONALE_MARKER.equals(context.rationale())) {
                return new ResponseAssessment(
                        ResponseAssessment.SCHEMA,
                        FinalExpressionJudgment.NOT_REQUESTED,
                        RationaleJudgment.CLEARLY_CONTRADICTORY,
                        List.of());
            }
            return new ResponseAssessment(
                    ResponseAssessment.SCHEMA,
                    FinalExpressionJudgment.NOT_REQUESTED,
                    RationaleJudgment.NOT_PROVIDED,
                    List.of());
        };
    }

    @Bean
    @Primary
    ResponseVerificationPort scriptedApplyResponseVerification() {
        return (profile, context) -> {
            throw new IllegalStateException("scripted response verification must never be invoked");
        };
    }

    @Bean
    @Primary
    RationaleAssessmentPort scriptedRationaleAssessment() {
        return (profile, compiledSystemPrompt, evaluationContextJson) ->
                RationaleEvaluationResult.notApplicable(
                        List.of(RationaleEvaluationResult.ReasonCode.MATERIAL_GAP));
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
