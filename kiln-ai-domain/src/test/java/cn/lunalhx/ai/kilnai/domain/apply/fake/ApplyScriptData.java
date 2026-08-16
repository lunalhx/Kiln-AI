package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;

import java.util.List;
import java.util.Map;

public final class ApplyScriptData {

    public static final String TASK_TEXT = "设 f(x) = 4x³ − 3x² + 7x − 5，求 f'(x)。";
    public static final String EXPECTED_EXPRESSION = "12*x^2 - 6*x + 7";

    public static final String INDEPENDENT_TASK_TEXT = "设 g(x) = 5x³ − 2x + 1，求 g'(x)。";
    public static final String INDEPENDENT_EXPECTED_EXPRESSION = "15*x^2 - 2";

    public static final String REVIEW_TASK_TEXT = "设 h(x) = 2x⁴ − 3x² + 5，求 h'(x)。";
    public static final String REVIEW_EXPECTED_EXPRESSION = "8*x^3 - 6*x";

    public static final String PRACTICE_TASK_TEXT = "设 p(x) = 6x³ − 4x + 3，求 p'(x)。";
    public static final String PRACTICE_EXPECTED_EXPRESSION = "18*x^2 - 4";

    public static final String SECOND_PRACTICE_TASK_TEXT = "设 q(x) = 7x³ − 5x + 2，求 q'(x)。";
    public static final String SECOND_PRACTICE_EXPECTED_EXPRESSION = "21*x^2 - 5";

    public static final String PRACTICE_CORRECT_DERIVATIVE = "18x²−4";
    public static final String PRACTICE_CORRECT_CANONICAL = "18*x^2-4";

    public static final String SECOND_PRACTICE_CORRECT_DERIVATIVE = "21x²−5";
    public static final String SECOND_PRACTICE_CORRECT_CANONICAL = "21*x^2-5";

    public static final String WRONG_DERIVATIVE = "6*x^2 - 3*x + 7";

    public static final String UNICODE_CORRECT_DERIVATIVE = "12x²−6x+7";
    public static final String UNICODE_CORRECT_CANONICAL = "12*x^2-6*x+7";

    public static final String APPLICABLE_RATIONALE = "利用幂法则和和差法则逐项求导";

    /** A confirmed expression the checker cannot decide, e.g. a chained power. */
    public static final String UNDECIDABLE_DERIVATIVE = "x^2^3";

    public static final String NON_SUBSTANTIVE_RATIONALE = "凭感觉";
    public static final String CONTRADICTORY_RATIONALE = "我用了乘积法则，答案是 6x";

    private ApplyScriptData() {
    }

    public static ResponseAssessment responseAssessment(
            FinalExpressionJudgment finalExpressionJudgment,
            RationaleJudgment rationaleJudgment
    ) {
        return new ResponseAssessment(ResponseAssessment.SCHEMA, finalExpressionJudgment, rationaleJudgment, List.of());
    }

    public static String taskReadyJson() {
        return taskReadyJson(TASK_TEXT, EXPECTED_EXPRESSION);
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

    public static String sourceGapJson() {
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

    public static ApplyGenerationDraft.TaskReady taskReadyDraft() {
        return (ApplyGenerationDraft.TaskReady) ApplyGenerationDraft.parse(taskReadyJson());
    }

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

    public static TaskVerificationVerdict rejectVerdict() {
        return new TaskVerificationVerdict(
                TaskVerificationVerdict.SCHEMA,
                TaskVerificationVerdict.Verdict.REJECT,
                Map.of(
                        "answer_correctness", TaskVerificationVerdict.CheckResult.REJECT,
                        "rubric_alignment", TaskVerificationVerdict.CheckResult.PASS,
                        "source_grounding", TaskVerificationVerdict.CheckResult.PASS,
                        "blueprint_compliance", TaskVerificationVerdict.CheckResult.PASS,
                        "learner_boundary", TaskVerificationVerdict.CheckResult.PASS),
                List.of("task_answer_inconsistent"));
    }

    public static TaskVerificationVerdict inconclusiveVerdict() {
        return new TaskVerificationVerdict(
                TaskVerificationVerdict.SCHEMA,
                TaskVerificationVerdict.Verdict.INCONCLUSIVE,
                Map.of(
                        "answer_correctness", TaskVerificationVerdict.CheckResult.INCONCLUSIVE,
                        "rubric_alignment", TaskVerificationVerdict.CheckResult.PASS,
                        "source_grounding", TaskVerificationVerdict.CheckResult.PASS,
                        "blueprint_compliance", TaskVerificationVerdict.CheckResult.PASS,
                        "learner_boundary", TaskVerificationVerdict.CheckResult.PASS),
                List.of("insufficient_verification_basis"));
    }
}
