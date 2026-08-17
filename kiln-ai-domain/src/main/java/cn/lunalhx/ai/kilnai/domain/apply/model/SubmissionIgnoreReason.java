package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * Why a learner command (a formal submission or a hint request) was ignored
 * without any evaluation or state change.
 */
public enum SubmissionIgnoreReason {
    ATTEMPT_NOT_FOUND,
    ALREADY_SUBMITTED,
    WRONG_ATTEMPT_PURPOSE,
    CONTINUE_NOT_LEGAL,
    RETRY_NOT_LEGAL,
    NOT_LEGAL_FOR_INTERACTION
}
