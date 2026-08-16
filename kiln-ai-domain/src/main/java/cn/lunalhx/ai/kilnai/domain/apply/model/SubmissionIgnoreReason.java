package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * Why a formal submission was ignored without any evaluation or state change.
 */
public enum SubmissionIgnoreReason {
    ATTEMPT_NOT_FOUND,
    ALREADY_SUBMITTED,
    WRONG_ATTEMPT_PURPOSE,
    CONTINUE_NOT_LEGAL
}
