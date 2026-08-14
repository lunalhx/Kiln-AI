package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * Why a submitted response cannot become a formal submission. The attempt
 * stays open for correction.
 */
public enum SubmissionRejectionReason {
    UNPARSEABLE_RAW,
    CONFIRMATION_MISMATCH
}
