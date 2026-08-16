package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.List;
import java.util.Objects;

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
