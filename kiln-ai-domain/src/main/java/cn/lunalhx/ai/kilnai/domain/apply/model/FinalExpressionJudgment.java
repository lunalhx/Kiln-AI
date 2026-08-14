package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed final-expression judgment of a {@link ResponseAssessment}.
 * {@link #NOT_REQUESTED} means the deterministic Mathematical Equivalence
 * Check already proved a result, so a model must not override it.
 */
public enum FinalExpressionJudgment {
    NOT_REQUESTED,
    EQUIVALENT,
    NOT_EQUIVALENT,
    INCONCLUSIVE
}
