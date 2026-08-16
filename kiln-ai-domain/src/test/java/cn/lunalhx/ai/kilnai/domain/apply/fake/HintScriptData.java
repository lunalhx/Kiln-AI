package cn.lunalhx.ai.kilnai.domain.apply.fake;

/**
 * Scripted fixtures for the closed {@code hint_generation/v1} contract and
 * the Hint Ladder Gate, mirroring {@link ApplyScriptData} for the reference
 * polynomial-differentiation Practice task whose expected answer is
 * {@code 18*x^2 - 4}.
 */
public final class HintScriptData {

    public static final String PRACTICE_EXPECTED = "18*x^2 - 4";

    public static final String H1_ORIENT = "先明确目标：求多项式函数 p(x) 的导函数 p'(x)。";
    public static final String H2_CUE = "可用的求导法则：幂法则、常数倍法则、和差法则与常数法则。";
    public static final String H3_STRATEGY = "对每一项分别求导，再合并结果：常数项导数为零，幂法则给出每项的导数。";
    public static final String H4_SCAFFOLD = "对 6x³ 求导得 18x²，对 −4x 求导得 −4，常数 3 的导数为 0。";
    public static final String H5_LEARNER_CONTENT = "完整解答：p'(x) = 3·6x² − 4 = 18x² − 4。";

    public static final String SOURCE_DOCUMENT = "openstax-calculus-v1";
    public static final String SOURCE_PASSAGE = "sec-3.3-differentiation-rules";

    public static final String[] H5_STEPS = {
            "使用幂法则：d/dx (6x³) = 18x²",
            "使用常数倍法则：d/dx (−4x) = −4",
            "常数法则：d/dx (3) = 0",
            "合并各项：p'(x) = 18x² − 4"
    };

    private HintScriptData() {
    }

    public static String ladderReadyJson() {
        return ladderReadyJson(H4_SCAFFOLD, H5_LEARNER_CONTENT, H5_STEPS, "18*x^2-4");
    }

    public static String ladderReadyJson(
            String h4Content,
            String h5Content,
            String[] h5Steps,
            String h5Answer
    ) {
        return """
                {
                  "schema": "hint_generation/v1",
                  "outcome": "ladder_ready",
                  "entries": [
                    {
                      "level": 1,
                      "disclosure_kind": "orient",
                      "learner_content": "%s",
                      "source_trace": [
                        { "source_document_id": "%s", "passage_id": "%s" }
                      ]
                    },
                    {
                      "level": 2,
                      "disclosure_kind": "cue",
                      "learner_content": "%s",
                      "source_trace": [
                        { "source_document_id": "%s", "passage_id": "%s" }
                      ]
                    },
                    {
                      "level": 3,
                      "disclosure_kind": "strategy",
                      "learner_content": "%s",
                      "source_trace": [
                        { "source_document_id": "%s", "passage_id": "%s" }
                      ]
                    },
                    {
                      "level": 4,
                      "disclosure_kind": "scaffold",
                      "learner_content": "%s",
                      "source_trace": [
                        { "source_document_id": "%s", "passage_id": "%s" }
                      ]
                    },
                    {
                      "level": 5,
                      "disclosure_kind": "reveal",
                      "learner_content": "%s",
                      "source_trace": [
                        { "source_document_id": "%s", "passage_id": "%s" }
                      ],
                      "reasoning_steps": ["%s", "%s", "%s", "%s"],
                      "proposed_final_answer": "%s"
                    }
                  ]
                }
                """.formatted(
                H1_ORIENT, SOURCE_DOCUMENT, SOURCE_PASSAGE,
                H2_CUE, SOURCE_DOCUMENT, SOURCE_PASSAGE,
                H3_STRATEGY, SOURCE_DOCUMENT, SOURCE_PASSAGE,
                h4Content, SOURCE_DOCUMENT, SOURCE_PASSAGE,
                h5Content, SOURCE_DOCUMENT, SOURCE_PASSAGE,
                h5Steps[0], h5Steps[1], h5Steps[2], h5Steps[3],
                h5Answer);
    }

    public static String sourceGapJson() {
        return """
                {
                  "schema": "hint_generation/v1",
                  "outcome": "source_gap",
                  "source_gap": {
                    "reason_code": "ladder_not_grounded",
                    "missing_requirement_ids": ["h5-reveal"]
                  }
                }
                """;
    }
}
