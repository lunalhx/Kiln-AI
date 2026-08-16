package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed rationale judgment of a {@link ResponseAssessment}.
 * {@link #APPLICABLE} and {@link #NOT_APPLICABLE} are valid only for a
 * Diagnostic; the two contradiction judgments are valid only for the
 * final-derivative policies (Independent Test, Review, and Practice).
 */
public enum RationaleJudgment {
    NOT_PROVIDED,
    NON_SUBSTANTIVE,
    APPLICABLE,
    NOT_APPLICABLE,
    NOT_CLEARLY_CONTRADICTORY,
    CLEARLY_CONTRADICTORY,
    INCONCLUSIVE
}
