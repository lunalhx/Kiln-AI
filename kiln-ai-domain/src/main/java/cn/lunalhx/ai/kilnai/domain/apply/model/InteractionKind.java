package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed learner interaction union of one committed Learning Flow
 * interaction (spec: one closed committed-interaction union so clients render
 * only committed state). {@link #TASK} carries an open Task Attempt with its
 * learner projection; {@link #TEACHING} carries the learner-visible teaching
 * projection of an Explain boundary; {@link #ASSISTANCE_CONSENT} carries the
 * one-way conversion warning over an open Independent or Review Attempt;
 * {@link #TRANSITION} is a message-only boundary of a normal flow ending or
 * leave; and {@link #UNAVAILABLE} is the neutral unavailable boundary of a
 * failed generation or unavailable node offering a safe retry or Flow Control
 * action. Private answers, unexposed hints, Rubric internals, source
 * passages, assessment facts, Blackboard content, and execution traces never
 * appear in any of them.
 */
public enum InteractionKind {
    TASK,
    TEACHING,
    ASSISTANCE_CONSENT,
    TRANSITION,
    UNAVAILABLE
}
