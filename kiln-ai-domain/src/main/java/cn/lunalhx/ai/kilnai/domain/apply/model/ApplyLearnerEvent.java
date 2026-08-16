package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The learner events a Task Package's learner projection permits. Hints are
 * legal only on an open Apply Practice Attempt (ADR-0065); Diagnostic,
 * Independent, Review, and Teach-back packages never carry
 * {@link #HINT_REQUESTED}.
 */
public enum ApplyLearnerEvent {
    ANSWER_SUBMITTED,
    PROCEDURAL_CLARIFICATION,
    FLOW_CONTROL,
    HINT_REQUESTED
}
