package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The closed result shared by Rationale Assessment and the later corroborating
 * responsibility. The Domain derives the overall verdict from the three
 * dimension checks and never trusts a contradictory model declaration.
 */
public record RationaleEvaluationResult(
        String schema,
        Verdict verdict,
        DimensionJudgment rubricBasis,
        DimensionJudgment taskConnection,
        DimensionJudgment coherence,
        List<ReasonCode> reasonCodes
) {

    public static final String SCHEMA = "rationale_evaluation/v1";

    public RationaleEvaluationResult {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(rubricBasis, "rubricBasis must not be null");
        Objects.requireNonNull(taskConnection, "taskConnection must not be null");
        Objects.requireNonNull(coherence, "coherence must not be null");
        Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("unsupported rationale evaluation schema: " + schema);
        }
        reasonCodes = canonicalReasonCodes(reasonCodes);
        Verdict derived = derive(rubricBasis, taskConnection, coherence);
        if (verdict != derived) {
            throw new IllegalArgumentException("declared rationale verdict does not match dimensions");
        }
        if (verdict == Verdict.APPLICABLE && !reasonCodes.isEmpty()) {
            throw new IllegalArgumentException("applicable rationale evaluation must have no reason codes");
        }
        if (verdict == Verdict.NOT_APPLICABLE && reasonCodes.isEmpty()) {
            throw new IllegalArgumentException("not applicable rationale evaluation needs a reason code");
        }
        if (verdict == Verdict.INCONCLUSIVE && !reasonCodes.isEmpty()) {
            throw new IllegalArgumentException("inconclusive rationale evaluation must have no reason codes");
        }
    }

    public static RationaleEvaluationResult parse(String json) {
        JsonNode root = ModelContract.object(json);
        ModelContract.requireExactFields(root, Set.of(
                "schema", "verdict", "rubric_basis", "task_connection", "coherence", "reason_codes"));
        try {
            return new RationaleEvaluationResult(
                    ModelContract.requiredSchema(root, SCHEMA),
                    ModelContract.requiredEnum(root, "verdict", Verdict.class),
                    ModelContract.requiredEnum(root, "rubric_basis", DimensionJudgment.class),
                    ModelContract.requiredEnum(root, "task_connection", DimensionJudgment.class),
                    ModelContract.requiredEnum(root, "coherence", DimensionJudgment.class),
                    parseReasonCodes(ModelContract.requiredStringList(root, "reason_codes")));
        } catch (IllegalArgumentException exception) {
            throw ModelContract.invalid("inconsistent_verdict");
        }
    }

    public static RationaleEvaluationResult applicable() {
        return new RationaleEvaluationResult(
                SCHEMA, Verdict.APPLICABLE,
                DimensionJudgment.PASS, DimensionJudgment.PASS, DimensionJudgment.PASS, List.of());
    }

    public static RationaleEvaluationResult notApplicable(List<ReasonCode> reasonCodes) {
        return new RationaleEvaluationResult(
                SCHEMA, Verdict.NOT_APPLICABLE,
                DimensionJudgment.FAIL, DimensionJudgment.PASS, DimensionJudgment.PASS, reasonCodes);
    }

    public static RationaleEvaluationResult inconclusive() {
        return new RationaleEvaluationResult(
                SCHEMA, Verdict.INCONCLUSIVE,
                DimensionJudgment.INCONCLUSIVE, DimensionJudgment.PASS, DimensionJudgment.PASS, List.of());
    }

    private static Verdict derive(
            DimensionJudgment rubricBasis,
            DimensionJudgment taskConnection,
            DimensionJudgment coherence
    ) {
        List<DimensionJudgment> dimensions = List.of(rubricBasis, taskConnection, coherence);
        if (dimensions.contains(DimensionJudgment.FAIL)) {
            return Verdict.NOT_APPLICABLE;
        }
        if (dimensions.contains(DimensionJudgment.INCONCLUSIVE)) {
            return Verdict.INCONCLUSIVE;
        }
        return Verdict.APPLICABLE;
    }

    private static List<ReasonCode> canonicalReasonCodes(List<ReasonCode> reasonCodes) {
        for (ReasonCode reasonCode : reasonCodes) {
            Objects.requireNonNull(reasonCode, "reasonCodes must not contain null");
        }
        return Arrays.stream(ReasonCode.values())
                .filter(reasonCodes::contains)
                .toList();
    }

    private static List<ReasonCode> parseReasonCodes(List<String> values) {
        try {
            return values.stream().map(ReasonCode::fromWireValue).toList();
        } catch (IllegalArgumentException exception) {
            throw ModelContract.invalid("invalid_reason_code");
        }
    }

    public enum Verdict {
        APPLICABLE("applicable"),
        NOT_APPLICABLE("not_applicable"),
        INCONCLUSIVE("inconclusive");

        private final String wireValue;

        Verdict(String wireValue) {
            this.wireValue = wireValue;
        }

        @JsonValue
        public String wireValue() {
            return wireValue;
        }
    }

    public enum DimensionJudgment {
        PASS("pass"),
        FAIL("fail"),
        INCONCLUSIVE("inconclusive");

        private final String wireValue;

        DimensionJudgment(String wireValue) {
            this.wireValue = wireValue;
        }

        @JsonValue
        public String wireValue() {
            return wireValue;
        }
    }

    public enum ReasonCode {
        MISSING_SUPPORT("missing_support"),
        MISAPPLICATION("misapplication"),
        FACTUAL_ERROR("factual_error"),
        MATERIAL_GAP("material_gap"),
        CONTRADICTION("contradiction");

        private final String wireValue;

        ReasonCode(String wireValue) {
            this.wireValue = wireValue;
        }

        @JsonValue
        public String wireValue() {
            return wireValue;
        }

        static ReasonCode fromWireValue(String value) {
            return Arrays.stream(values())
                    .filter(code -> code.wireValue.equals(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown reason code"));
        }
    }
}
