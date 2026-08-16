package cn.lunalhx.ai.kilnai.domain.apply.fake;

import java.util.List;

/**
 * Scripted fixtures for the closed {@code explain_generation/v1} contract. The
 * worked example is the reference method's single complete example over the
 * polynomial-differentiation scope: every step maps to one approved rule from
 * the Concept Contract's included scope, and the source trace references the
 * approved OpenStax passage.
 */
public final class ExplainScriptData {

    public static final String PRINCIPLE_SUMMARY =
            "多项式求导逐项进行：常数项的导数为零（常数法则）；系数保持不变、只对变量项求导"
                    + "（常数倍数法则）；各项分别求导后相加（和差法则）；对变量的幂次项，指数乘入系数并降次（幂法则）。";

    public static final String EXPLAIN_PROBLEM = "设 f(x) = 5x³ − 2x² + 7，求 f'(x)。";

    public static final String EXPLAIN_FINAL_RESULT = "15x² − 4x";

    private ExplainScriptData() {
    }

    public static String explainReadyJson() {
        return """
                {
                  "schema": "explain_generation/v1",
                  "outcome": "teaching_ready",
                  "principle_summary": "%s",
                  "worked_example": {
                    "problem": "%s",
                    "steps": [
                      { "expression": "d/dx[5x³] = 5 · d/dx[x³]", "rule_reference": "constant-multiple rule", "explanation": "常数倍数法则：提出系数 5，只对 x³ 求导。" },
                      { "expression": "d/dx[x³] = 3x²", "rule_reference": "power rule for polynomial terms", "explanation": "幂法则：指数 3 乘入系数，指数降为 2。" },
                      { "expression": "d/dx[7] = 0", "rule_reference": "constant rule", "explanation": "常数法则：常数项的导数为零。" },
                      { "expression": "f'(x) = 5·3x² − 2·2x + 0", "rule_reference": "sum and difference rules", "explanation": "和差法则：逐项求导后合并结果。" }
                    ],
                    "final_result": "%s"
                  },
                  "source_trace": [
                    { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" }
                  ]
                }
                """.formatted(PRINCIPLE_SUMMARY, EXPLAIN_PROBLEM, EXPLAIN_FINAL_RESULT);
    }

    public static String explainReadyJson(String principleSummary, String problem, String finalResult) {
        return explainReadyJsonWithSteps(principleSummary, problem, finalResult, standardSteps());
    }

    public static String explainReadyJsonWithSteps(
            String principleSummary,
            String problem,
            String finalResult,
            List<StepJson> steps
    ) {
        StringBuilder stepsJson = new StringBuilder();
        for (int index = 0; index < steps.size(); index++) {
            StepJson step = steps.get(index);
            if (index > 0) {
                stepsJson.append(',');
            }
            stepsJson.append("""
                    { "expression": "%s", "rule_reference": "%s", "explanation": "%s" }
                    """.formatted(step.expression(), step.ruleReference(), step.explanation()));
        }
        return """
                {
                  "schema": "explain_generation/v1",
                  "outcome": "teaching_ready",
                  "principle_summary": "%s",
                  "worked_example": {
                    "problem": "%s",
                    "steps": [ %s ],
                    "final_result": "%s"
                  },
                  "source_trace": [
                    { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" }
                  ]
                }
                """.formatted(principleSummary, problem, stepsJson, finalResult);
    }

    public static List<StepJson> standardSteps() {
        return List.of(
                new StepJson("d/dx[5x³] = 5 · d/dx[x³]", "constant-multiple rule", "常数倍数法则：提出系数 5，只对 x³ 求导。"),
                new StepJson("d/dx[x³] = 3x²", "power rule for polynomial terms", "幂法则：指数 3 乘入系数，指数降为 2。"),
                new StepJson("d/dx[7] = 0", "constant rule", "常数法则：常数项的导数为零。"),
                new StepJson("f'(x) = 5·3x² − 2·2x + 0", "sum and difference rules", "和差法则：逐项求导后合并结果。"));
    }

    public static String explainSourceGapJson() {
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

    public record StepJson(String expression, String ruleReference, String explanation) {
    }
}
