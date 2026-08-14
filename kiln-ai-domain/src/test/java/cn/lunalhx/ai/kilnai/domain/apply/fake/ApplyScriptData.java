package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;

import java.util.List;
import java.util.Map;

public final class ApplyScriptData {

    public static final String TASK_TEXT = "设 f(x) = 4x³ − 3x² + 7x − 5，求 f'(x)。";
    public static final String EXPECTED_EXPRESSION = "12*x^2 - 6*x + 7";

    private ApplyScriptData() {
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
