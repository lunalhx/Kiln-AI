package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    public static ResponseAssessment parse(String json) {
        JsonNode root = ModelContract.object(json);
        ModelContract.requireExactFields(root, Set.of(
                "schema", "final_expression_judgment", "rationale_judgment", "reason_codes"));
        return new ResponseAssessment(
                ModelContract.requiredSchema(root, SCHEMA),
                ModelContract.requiredEnum(root, "final_expression_judgment", FinalExpressionJudgment.class),
                ModelContract.requiredEnum(root, "rationale_judgment", RationaleJudgment.class),
                ModelContract.requiredStringList(root, "reason_codes"));
    }
}
