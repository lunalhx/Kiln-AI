package cn.lunalhx.ai.kilnai;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExplainGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.HintGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackAssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackTaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassification;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassifierPort;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPort;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The scripted model ports of the Learning/Practice graph for the PostgreSQL
 * recovery contract. Apply generation is deterministic by Attempt Purpose and
 * can be told to fail the next generation once, which exercises the
 * failed-generation-leaves-no-trace path. Explain generation can likewise be
 * failed once. Assessment always defers to the proof-bounded deterministic
 * Mathematical Equivalence Check, so a confirmed wrong answer is a conclusive
 * failure and a confirmed correct answer passes. The Pedagogy Agent port
 * always returns an invalid plan, so every guarded decision runs the spec's
 * deterministic fallback; the Clarification Gate classifies as scripted or,
 * by default, conservatively as substantive, so a Diagnostic or Teach-back
 * clarification is refused and an Independent or Review attempt first
 * projects the assistance-consent boundary.
 */
@TestConfiguration
public class ScriptedLearningGraphPortsConfiguration {

    public static final String PRACTICE_TASK = "设 p(x) = 6x³ − 4x + 3，求 p'(x)。";
    public static final String PRACTICE_EXPECTED = "18*x^2 - 4";
    public static final String PRACTICE_TASK_2 = "设 v(x) = 7x⁴ − 5x² + 2，求 v'(x)。";
    public static final String PRACTICE_EXPECTED_2 = "28*x^3 - 10*x";

    private volatile boolean failNextApplyGeneration = false;
    private volatile boolean failNextExplainGeneration = false;
    private final AtomicInteger remainingInvalidAssessments = new AtomicInteger(0);
    private final AtomicInteger remainingAssessmentConfigurationFailures = new AtomicInteger(0);
    private final AtomicInteger remainingAssessmentProviderFailures = new AtomicInteger(0);
    private final AtomicInteger remainingResponseVerificationProviderFailures = new AtomicInteger(0);
    private final AtomicInteger remainingTeachBackAssessmentProviderFailures = new AtomicInteger(0);
    private volatile boolean responseVerificationEnabled = false;
    private final Deque<ClarificationClassification> scriptedClarifications = new ArrayDeque<>();

    public void failNextApplyGeneration() {
        this.failNextApplyGeneration = true;
    }

    public void failNextExplainGeneration() {
        this.failNextExplainGeneration = true;
    }

    public void failNextAssessments(int count) {
        this.remainingInvalidAssessments.set(count);
    }

    public void failNextAssessmentProviderCalls(int count) {
        this.remainingAssessmentProviderFailures.set(count);
    }

    public void failNextAssessmentConfigurationCalls(int count) {
        this.remainingAssessmentConfigurationFailures.set(count);
    }

    public void failNextResponseVerificationProviderCalls(int count) {
        this.responseVerificationEnabled = true;
        this.remainingResponseVerificationProviderFailures.set(count);
    }

    public void failNextTeachBackAssessmentProviderCalls(int count) {
        this.remainingTeachBackAssessmentProviderFailures.set(count);
    }

    public void resetTransientFailures() {
        failNextApplyGeneration = false;
        failNextExplainGeneration = false;
        remainingInvalidAssessments.set(0);
        remainingAssessmentConfigurationFailures.set(0);
        remainingAssessmentProviderFailures.set(0);
        remainingResponseVerificationProviderFailures.set(0);
        remainingTeachBackAssessmentProviderFailures.set(0);
        responseVerificationEnabled = false;
    }

    /**
     * Scripts the next clarification classification (consumed in call order).
     * Without a scripted classification the gate conservatively classifies as
     * substantive, so a Diagnostic or Teach-back clarification is refused and
     * an Independent or Review clarification first projects the consent
     * boundary.
     */
    public void scriptClarification(ClarificationClassification classification) {
        this.scriptedClarifications.addLast(classification);
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
            if (failNextApplyGeneration) {
                failNextApplyGeneration = false;
                return sourceGapJson();
            }
            if (executionContextJson.contains("\"attempt_purpose\":\"review\"")) {
                int reviewNumber = ScriptedApplyPortsConfiguration.exposedTaskCount(executionContextJson) - 2 + 1;
                return switch (reviewNumber) {
                    case 1 -> ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.REVIEW_TASK,
                            ScriptedApplyPortsConfiguration.REVIEW_EXPECTED);
                    case 2 -> ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.REVIEW_TASK_2,
                            ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2);
                    case 3 -> ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.REVIEW_TASK_3,
                            ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_3);
                    default -> ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.REVIEW_TASK_4,
                            ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_4);
                };
            }
            if (executionContextJson.contains("\"attempt_purpose\":\"practice\"")) {
                return ScriptedApplyPortsConfiguration.taskReadyJson(
                        exposedPracticeTask(executionContextJson), exposedPracticeExpected(executionContextJson));
            }
            return executionContextJson.contains("\"attempt_purpose\":\"independent_test\"")
                    ? ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.INDEPENDENT_TASK,
                            ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED)
                    : ScriptedApplyPortsConfiguration.taskReadyJson(
                            ScriptedApplyPortsConfiguration.DIAGNOSTIC_TASK,
                            ScriptedApplyPortsConfiguration.DIAGNOSTIC_EXPECTED);
        };
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
                return ScriptedApplyPortsConfiguration.passVerdict();
            }
        };
    }

    @Bean
    @Primary
    AssessmentPort scriptedApplyAssessment() {
        // The Assessment defers to the proof-bounded deterministic
        // Mathematical Equivalence Check: NOT_REQUESTED/NOT_PROVIDED with a
        // proven-correct expression passes and a proven-wrong expression is a
        // conclusive failure for every Attempt Purpose. Tests can script a
        // bounded number of Model Contract Invalid replies before that.
        return (profile, context) -> {
            if (remainingAssessmentConfigurationFailures.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
                throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "model configuration invalid");
            }
            if (remainingAssessmentProviderFailures.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
                throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "provider unavailable");
            }
            if (remainingInvalidAssessments.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
                throw new ModelContractInvalidException(List.of("unknown_field"));
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
            if (!responseVerificationEnabled) {
                throw new IllegalStateException("scripted response verification must never be invoked");
            }
            if (remainingResponseVerificationProviderFailures.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
                throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "provider unavailable");
            }
            return new ResponseAssessment(
                    ResponseAssessment.SCHEMA,
                    FinalExpressionJudgment.EQUIVALENT,
                    RationaleJudgment.NOT_PROVIDED,
                    List.of());
        };
    }

    @Bean
    @Primary
    ExplainGenerationPort scriptedExplainGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            if (failNextExplainGeneration) {
                failNextExplainGeneration = false;
                return """
                        {
                          "schema": "explain_generation/v1",
                          "outcome": "source_gap",
                          "source_gap": {
                            "reason_code": "required_rule_not_grounded",
                            "missing_requirement_ids": ["sum and difference rules"]
                          }
                        }
                        """;
            }
            return explainReadyJson();
        };
    }

    @Bean
    @Primary
    HintGenerationPort scriptedHintGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> hintLadderReadyJson();
    }

    @Bean
    @Primary
    TeachBackGenerationPort scriptedTeachBackGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> teachBackTaskReadyJson(executionContextJson);
    }

    @Bean
    @Primary
    TeachBackTaskVerifierPort scriptedTeachBackTaskVerifier() {
        return (profile, taskPackage, context) -> ScriptedApplyPortsConfiguration.passVerdict();
    }

    @Bean
    @Primary
    TeachBackAssessmentPort scriptedTeachBackAssessment() {
        return (profile, context) -> {
            if (remainingTeachBackAssessmentProviderFailures.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
                throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "provider unavailable");
            }
            return new TeachBackAssessment(
                    TeachBackAssessment.SCHEMA,
                    TeachBackAssessment.DimensionJudgment.PASS,
                    TeachBackAssessment.DimensionJudgment.PASS,
                    TeachBackAssessment.DimensionJudgment.PASS,
                    List.of());
        };
    }

    @Bean
    @Primary
    PedagogyPort scriptedPedagogy() {
        // An always-invalid plan discards the entire output after one repair
        // and runs the spec's deterministic fallback, so the recovery contract
        // never depends on a scripted action choice.
        return (profile, compiledSystemPrompt, executionContextJson) -> "{}";
    }

    @Bean
    @Primary
    ClarificationClassifierPort scriptedClarificationClassifier() {
        return (profile, message, taskText) -> {
            ClarificationClassification next = scriptedClarifications.pollFirst();
            return next == null ? ClarificationClassification.SUBSTANTIVE : next;
        };
    }

    private static String sourceGapJson() {
        return """
                {
                  "schema": "apply_generation/v1",
                  "outcome": "source_gap",
                  "source_gap": {
                    "reason_code": "required_criterion_not_grounded",
                    "missing_requirement_ids": ["differentiate-polynomial"]
                  }
                }
                """;
    }

    private static String exposedPracticeTask(String executionContextJson) {
        return ScriptedApplyPortsConfiguration.exposedTaskCount(executionContextJson) > 1
                ? PRACTICE_TASK_2 : PRACTICE_TASK;
    }

    private static String exposedPracticeExpected(String executionContextJson) {
        return ScriptedApplyPortsConfiguration.exposedTaskCount(executionContextJson) > 1
                ? PRACTICE_EXPECTED_2 : PRACTICE_EXPECTED;
    }

    private static String explainReadyJson() {
        return """
                {
                  "schema": "explain_generation/v1",
                  "outcome": "teaching_ready",
                  "principle_summary": "多项式求导逐项进行：常数项的导数为零（常数法则）；系数保持不变、只对变量项求导（常数倍数法则）；各项分别求导后相加（和差法则）；对变量的幂次项，指数乘入系数并降次（幂法则）。",
                  "worked_example": {
                    "problem": "设 f(x) = 5x³ − 2x² + 7，求 f'(x)。",
                    "steps": [
                      { "expression": "d/dx[5x³] = 5 · d/dx[x³]", "rule_reference": "constant-multiple rule", "explanation": "常数倍数法则：提出系数 5，只对 x³ 求导。" },
                      { "expression": "d/dx[x³] = 3x²", "rule_reference": "power rule for polynomial terms", "explanation": "幂法则：指数 3 乘入系数，指数降为 2。" },
                      { "expression": "d/dx[7] = 0", "rule_reference": "constant rule", "explanation": "常数法则：常数项的导数为零。" },
                      { "expression": "f'(x) = 5·3x² − 2·2x + 0", "rule_reference": "sum and difference rules", "explanation": "和差法则：逐项求导后合并结果。" }
                    ],
                    "final_result": "15x² − 4x"
                  },
                  "source_trace": [
                    { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" }
                  ]
                }
                """;
    }

    private static String hintLadderReadyJson() {
        return """
                {
                  "schema": "hint_generation/v1",
                  "outcome": "ladder_ready",
                  "entries": [
                    { "level": 1, "disclosure_kind": "orient", "learner_content": "先明确目标：求多项式函数 p(x) 的导函数 p'(x)。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ] },
                    { "level": 2, "disclosure_kind": "cue", "learner_content": "可用的求导法则：幂法则、常数倍法则、和差法则与常数法则。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ] },
                    { "level": 3, "disclosure_kind": "strategy", "learner_content": "对每一项分别求导，再合并结果：常数项导数为零，幂法则给出每项的导数。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ] },
                    { "level": 4, "disclosure_kind": "scaffold", "learner_content": "对 6x³ 求导得 18x²，对 −4x 求导得 −4，常数 3 的导数为 0。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ] },
                    { "level": 5, "disclosure_kind": "reveal", "learner_content": "完整解答：p'(x) = 3·6x² − 4 = 18x² − 4。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ], "reasoning_steps": ["使用幂法则：d/dx (6x³) = 18x²", "使用常数倍法则：d/dx (−4x) = −4", "常数法则：d/dx (3) = 0", "合并各项：p'(x) = 18x² − 4"], "proposed_final_answer": "18*x^2-4" }
                  ]
                }
                """;
    }

    private static String teachBackTaskReadyJson(String executionContextJson) {
        Matcher anchor = Pattern.compile(
                        "\\\"anchor\\\"\\s*:\\s*\\{\\s*"
                                + "\\\"anchor_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*"
                                + "\\\"anchor_kind\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(executionContextJson);
        if (!anchor.find()) {
            throw new IllegalArgumentException("scripted Teach-back context is missing its anchor");
        }
        return """
                {
                  "schema": "teach_back_generation/v1",
                  "outcome": "task_ready",
                  "learner_prompt": "请用简短文字解释刚才例题中使用了哪些求导法则、为什么这些法则适用，以及这些步骤如何最终得出 15x² − 4x 这个结果。",
                  "rubric_mapping": [
                    { "dimension": "rule_identification", "mastery_criterion": "differentiate-polynomial" },
                    { "dimension": "applicability_explanation", "mastery_criterion": "differentiate-polynomial" },
                    { "dimension": "steps_result_coherence", "mastery_criterion": "differentiate-polynomial" }
                  ],
                  "source_trace": [
                    { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" }
                  ],
                  "anchor_reference": {
                    "anchor_id": "%s",
                    "anchor_kind": "%s"
                  }
                }
                """.formatted(anchor.group(1), anchor.group(2));
    }
}
