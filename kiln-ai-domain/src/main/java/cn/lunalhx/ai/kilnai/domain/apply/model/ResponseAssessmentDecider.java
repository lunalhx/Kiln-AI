package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.Objects;

/**
 * Deterministically combines the proof-bounded Mathematical Equivalence Check
 * with the closed judgments of isolated Assessment and independent Response
 * Verification. A proven deterministic result is never overridden. On
 * Cannot Decide, both isolated judgments must return equivalent for the
 * final-expression channel to pass; disagreement or any non-equivalent result
 * is Inconclusive.
 */
public final class ResponseAssessmentDecider {

    private ResponseAssessmentDecider() {
    }

    public static AssessmentOutcome decide(
            ResponseAssessmentContext context,
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) {
        Objects.requireNonNull(context, "context must not be null");
        if (assessment != null) {
            validateRationaleFor(context.purpose(), assessment.rationaleJudgment());
        }
        return switch (context.purpose()) {
            case DIAGNOSTIC -> decideDiagnostic(context, assessment, verification);
            // Review and Practice use the same final-expression and
            // non-contradictory optional-rationale policy as Independent Test
            // (spec line 81): a clearly contradictory rationale over a correct
            // final answer is Blocked, which the Review and Practice flows map
            // to a conclusive failure (ADR-0061, ADR-0067), never evaluative
            // uncertainty.
            case INDEPENDENT_TEST, REVIEW, PRACTICE -> decideIndependent(context, assessment, verification);
        };
    }

    private static void validateRationaleFor(AttemptPurpose purpose, RationaleJudgment rationale) {
        boolean valid = switch (purpose) {
            case DIAGNOSTIC -> rationale != RationaleJudgment.CLEARLY_CONTRADICTORY
                    && rationale != RationaleJudgment.NOT_CLEARLY_CONTRADICTORY;
            case INDEPENDENT_TEST, REVIEW, PRACTICE -> rationale != RationaleJudgment.APPLICABLE
                    && rationale != RationaleJudgment.NOT_APPLICABLE;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "rationale judgment " + rationale + " is not valid for " + purpose);
        }
    }

    private static AssessmentOutcome decideDiagnostic(
            ResponseAssessmentContext context,
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) {
        FinalExpressionJudgment finalExpression = finalExpression(context.deterministicOutcome(), assessment, verification);
        if (finalExpression == FinalExpressionJudgment.EQUIVALENT) {
            return new AssessmentOutcome.Passed(assessment, verification);
        }
        RationaleJudgment rationale = rationaleOf(assessment);
        if (rationale == RationaleJudgment.APPLICABLE) {
            return new AssessmentOutcome.Passed(assessment, verification);
        }
        if (finalExpression == FinalExpressionJudgment.INCONCLUSIVE || rationale == RationaleJudgment.INCONCLUSIVE) {
            return new AssessmentOutcome.Inconclusive(assessment, verification);
        }
        return new AssessmentOutcome.Failed(assessment, verification);
    }

    private static AssessmentOutcome decideIndependent(
            ResponseAssessmentContext context,
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) {
        FinalExpressionJudgment finalExpression = finalExpression(context.deterministicOutcome(), assessment, verification);
        return switch (finalExpression) {
            case EQUIVALENT -> {
                RationaleJudgment rationale = rationaleOf(assessment);
                if (rationale == RationaleJudgment.CLEARLY_CONTRADICTORY) {
                    yield new AssessmentOutcome.Blocked(assessment, verification);
                }
                if (rationale == RationaleJudgment.INCONCLUSIVE) {
                    yield new AssessmentOutcome.Inconclusive(assessment, verification);
                }
                yield new AssessmentOutcome.Passed(assessment, verification);
            }
            case NOT_EQUIVALENT -> new AssessmentOutcome.Failed(assessment, verification);
            case INCONCLUSIVE -> new AssessmentOutcome.Inconclusive(assessment, verification);
            case NOT_REQUESTED -> throw new IllegalStateException(
                    "the decider requires a resolved final-expression judgment");
        };
    }

    private static RationaleJudgment rationaleOf(ResponseAssessment assessment) {
        if (assessment == null) {
            return RationaleJudgment.INCONCLUSIVE;
        }
        return assessment.rationaleJudgment();
    }

    private static FinalExpressionJudgment finalExpression(
            EquivalenceOutcome deterministic,
            ResponseAssessment assessment,
            ResponseAssessment verification
    ) {
        return switch (deterministic) {
            case PROVEN_EQUIVALENT -> FinalExpressionJudgment.EQUIVALENT;
            case PROVEN_NOT_EQUIVALENT -> FinalExpressionJudgment.NOT_EQUIVALENT;
            case CANNOT_DECIDE -> {
                boolean bothEquivalent = assessment != null && verification != null
                        && assessment.finalExpressionJudgment() == FinalExpressionJudgment.EQUIVALENT
                        && verification.finalExpressionJudgment() == FinalExpressionJudgment.EQUIVALENT;
                yield bothEquivalent
                        ? FinalExpressionJudgment.EQUIVALENT
                        : FinalExpressionJudgment.INCONCLUSIVE;
            }
        };
    }
}
