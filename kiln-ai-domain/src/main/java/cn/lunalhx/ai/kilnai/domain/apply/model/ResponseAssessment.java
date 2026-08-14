package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Objects;

/**
 * The closed {@code response_assessment/v1} contract returned by isolated
 * Assessment and Response Verification. It separates the final-expression
 * judgment from the rationale judgment, carries closed reason codes, and can
 * never award evidence or modify Flow State.
 */
public record ResponseAssessment(
        String schema,
        FinalExpressionJudgment finalExpressionJudgment,
        RationaleJudgment rationaleJudgment,
        List<String> reasonCodes
) {

    public static final String SCHEMA = "response_assessment/v1";

    public ResponseAssessment {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(finalExpressionJudgment, "finalExpressionJudgment must not be null");
        Objects.requireNonNull(rationaleJudgment, "rationaleJudgment must not be null");
        Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
        reasonCodes = List.copyOf(reasonCodes);
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("unsupported assessment schema: " + schema);
        }
    }
}
