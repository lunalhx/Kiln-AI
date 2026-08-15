package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed typed result of deterministic response assessment. It is a pure
 * value object produced by {@link ResponseAssessmentDecider}; it cannot
 * modify Flow State or accept Evidence. The carried {@code assessment} and
 * {@code verification} contracts are null exactly when the deterministic
 * Mathematical Equivalence Check alone decided the outcome and no model
 * judgment was invoked. {@link #Blocked} occurs when the final answer is
 * correct but the rationale is clearly contradictory; Independent Test
 * accepts no Evidence from it, while the Review submission flow treats it as
 * a conclusive no-hint failure (ADR-0061).
 */
public sealed interface AssessmentOutcome
        permits AssessmentOutcome.Passed,
        AssessmentOutcome.Failed,
        AssessmentOutcome.Inconclusive,
        AssessmentOutcome.Blocked {

    record Passed(
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) implements AssessmentOutcome {
    }

    record Failed(
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) implements AssessmentOutcome {
    }

    record Inconclusive(
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) implements AssessmentOutcome {
    }

    record Blocked(
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) implements AssessmentOutcome {
    }
}
