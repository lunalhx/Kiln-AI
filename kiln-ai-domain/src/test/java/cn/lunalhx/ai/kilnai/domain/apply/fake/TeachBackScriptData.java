package cn.lunalhx.ai.kilnai.domain.apply.fake;

import java.util.List;
import java.util.UUID;

/**
 * Scripted fixtures for the closed {@code teach_back_generation/v1} contract
 * and the three-dimension Teach-back assessment, mirroring
 * {@link ExplainScriptData} for the reference polynomial-differentiation
 * concept. The task-ready draft is anchored to a fixed Explain worked-example
 * anchor whose learner-visible content asks the learner to explain the rules,
 * their applicability, and the connection between the steps and the result.
 */
public final class TeachBackScriptData {

    public static final UUID ANCHOR_ID = UUID.fromString("00000000-0000-0000-0000-00000000a5a5");

    public static final String ANCHOR_KIND = "EXPLAIN_WORKED_EXAMPLE";

    public static final String ANCHOR_CONTENT =
            "多项式求导逐项进行：常数项的导数为零（常数法则）；系数保持不变、只对变量项求导"
                    + "（常数倍数法则）；各项分别求导后相加（和差法则）；对变量的幂次项，指数乘入系数并降次（幂法则）。"
                    + " 设 f(x) = 5x³ − 2x² + 7，求 f'(x)。"
                    + " d/dx[5x³] = 5 · d/dx[x³] 常数倍数法则：提出系数 5，只对 x³ 求导。"
                    + " d/dx[x³] = 3x² 幂法则：指数 3 乘入系数，指数降为 2。"
                    + " d/dx[7] = 0 常数法则：常数项的导数为零。"
                    + " f'(x) = 5·3x² − 2·2x + 0 和差法则：逐项求导后合并结果。"
                    + " 15x² − 4x";

    public static final String LEARNER_PROMPT =
            "请用简短文字解释刚才例题中使用了哪些求导法则、为什么这些法则适用，以及这些步骤如何"
                    + "最终得出 15x² − 4x 这个结果。";

    public static final String SOURCE_DOCUMENT = "openstax-calculus-v1";
    public static final String SOURCE_PASSAGE = "sec-3.3-differentiation-rules";

    public static final String PASS_EXPLANATION =
            "例题用到了幂法则、常数倍数法则、常数法则与和差法则：5x³ 是常数倍与幂次的组合，"
                    + "所以先用常数倍数法则提出 5 再用幂法则求导；−2x² 同理；常数 7 的导数为零；"
                    + "最后用和差法则把各项导数合并，得到 f'(x) = 15x² − 4x，与例题结果一致。";

    private TeachBackScriptData() {
    }

    public static String taskReadyJson() {
        return """
                {
                  "schema": "teach_back_generation/v1",
                  "outcome": "task_ready",
                  "learner_prompt": "%s",
                  "rubric_mapping": [
                    { "dimension": "rule_identification", "mastery_criterion": "differentiate-polynomial" },
                    { "dimension": "applicability_explanation", "mastery_criterion": "differentiate-polynomial" },
                    { "dimension": "steps_result_coherence", "mastery_criterion": "differentiate-polynomial" }
                  ],
                  "source_trace": [
                    { "source_document_id": "%s", "passage_id": "%s" }
                  ],
                  "anchor_reference": {
                    "anchor_id": "%s",
                    "anchor_kind": "%s"
                  }
                }
                """.formatted(LEARNER_PROMPT, SOURCE_DOCUMENT, SOURCE_PASSAGE, ANCHOR_ID, ANCHOR_KIND);
    }

    public static String taskReadyJson(String learnerPrompt) {
        return taskReadyJson(learnerPrompt, SOURCE_DOCUMENT, SOURCE_PASSAGE, ANCHOR_ID.toString(), ANCHOR_KIND);
    }

    public static String taskReadyJson(String learnerPrompt, String anchorId, String anchorKind) {
        return taskReadyJson(learnerPrompt, SOURCE_DOCUMENT, SOURCE_PASSAGE, anchorId, anchorKind);
    }

    public static String taskReadyJson(
            String learnerPrompt,
            String sourceDocument,
            String passage,
            String anchorId,
            String anchorKind
    ) {
        return """
                {
                  "schema": "teach_back_generation/v1",
                  "outcome": "task_ready",
                  "learner_prompt": "%s",
                  "rubric_mapping": [
                    { "dimension": "rule_identification", "mastery_criterion": "differentiate-polynomial" },
                    { "dimension": "applicability_explanation", "mastery_criterion": "differentiate-polynomial" },
                    { "dimension": "steps_result_coherence", "mastery_criterion": "differentiate-polynomial" }
                  ],
                  "source_trace": [
                    { "source_document_id": "%s", "passage_id": "%s" }
                  ],
                  "anchor_reference": {
                    "anchor_id": "%s",
                    "anchor_kind": "%s"
                  }
                }
                """.formatted(learnerPrompt, sourceDocument, passage, anchorId, anchorKind);
    }

    public static String sourceGapJson() {
        return """
                {
                  "schema": "teach_back_generation/v1",
                  "outcome": "source_gap",
                  "source_gap": {
                    "reason_code": "anchor_not_grounded",
                    "missing_requirement_ids": ["rule_identification"]
                  }
                }
                """;
    }

    public static cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment passAssessment() {
        return assessment(
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.PASS,
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.PASS,
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.PASS,
                List.of());
    }

    public static cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment failAssessment() {
        return assessment(
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.FAIL,
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.FAIL,
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.FAIL,
                List.of("rule_not_identified"));
    }

    public static cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment inconclusiveAssessment() {
        return assessment(
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                List.of("unreliable_judgment"));
    }

    public static cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment assessment(
            cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment ruleIdentification,
            cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment applicability,
            cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.DimensionJudgment coherence,
            List<String> reasonCodes
    ) {
        return new cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment(
                cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment.SCHEMA,
                ruleIdentification, applicability, coherence, reasonCodes);
    }
}
