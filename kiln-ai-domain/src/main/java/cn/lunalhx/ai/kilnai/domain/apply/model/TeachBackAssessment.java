package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The closed {@code teach_back_assessment/v1} contract returned by the
 * isolated semantic Assessment of one Teach-back submission. It carries one
 * judgment per mandatory Rubric dimension — correct rule identification,
 * correct explanation of applicability, and a coherent, noncontradictory
 * connection between the steps and the result — plus closed reason codes.
 * The deterministic {@link #outcome()} resolves the dimensions: an
 * unreliable or disputed dimension makes the whole judgment Inconclusive; all
 * three must pass for a Teach-back pass; a clearly missing or wrong dimension
 * fails. The record can never award Evidence or mutate Flow State.
 */
public record TeachBackAssessment(
        String schema,
        DimensionJudgment ruleIdentification,
        DimensionJudgment applicabilityExplanation,
        DimensionJudgment stepsResultCoherence,
        List<String> reasonCodes
) {

    public static final String SCHEMA = "teach_back_assessment/v1";

    public TeachBackAssessment {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(ruleIdentification, "ruleIdentification must not be null");
        Objects.requireNonNull(applicabilityExplanation, "applicabilityExplanation must not be null");
        Objects.requireNonNull(stepsResultCoherence, "stepsResultCoherence must not be null");
        Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
        reasonCodes = List.copyOf(reasonCodes);
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("unsupported assessment schema: " + schema);
        }
    }

    public static TeachBackAssessment parse(String json) {
        JsonNode root = ModelContract.object(json);
        ModelContract.requireExactFields(root, Set.of(
                "schema", "rule_identification", "applicability_explanation",
                "steps_result_coherence", "reason_codes"));
        return new TeachBackAssessment(
                ModelContract.requiredSchema(root, SCHEMA),
                ModelContract.requiredEnum(root, "rule_identification", DimensionJudgment.class),
                ModelContract.requiredEnum(root, "applicability_explanation", DimensionJudgment.class),
                ModelContract.requiredEnum(root, "steps_result_coherence", DimensionJudgment.class),
                ModelContract.requiredStringList(root, "reason_codes"));
    }

    /**
     * The deterministic three-dimension resolution: any Inconclusive
     * dimension dominates (uncertainty must never become failure or success),
     * otherwise all three dimensions must pass for a Teach-back pass, and a
     * clearly missing or wrong dimension fails.
     */
    public TeachBackOutcome outcome() {
        if (ruleIdentification == DimensionJudgment.INCONCLUSIVE
                || applicabilityExplanation == DimensionJudgment.INCONCLUSIVE
                || stepsResultCoherence == DimensionJudgment.INCONCLUSIVE) {
            return TeachBackOutcome.INCONCLUSIVE;
        }
        if (ruleIdentification == DimensionJudgment.PASS
                && applicabilityExplanation == DimensionJudgment.PASS
                && stepsResultCoherence == DimensionJudgment.PASS) {
            return TeachBackOutcome.PASS;
        }
        return TeachBackOutcome.FAIL;
    }

    public enum DimensionJudgment {
        PASS,
        FAIL,
        INCONCLUSIVE
    }

    public enum TeachBackOutcome {
        PASS,
        FAIL,
        INCONCLUSIVE
    }
}
