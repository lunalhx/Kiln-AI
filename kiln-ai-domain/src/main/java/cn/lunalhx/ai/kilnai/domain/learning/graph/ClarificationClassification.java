package cn.lunalhx.ai.kilnai.domain.learning.graph;

/**
 * The closed classification of one Clarification Asked event (CONTEXT.md
 * Clarification Gate). A procedural request only restates interface behavior,
 * response format, symbol notation, or conditions already present in the Task
 * Package; a substantive request explains knowledge, suggests a method,
 * exposes a reasoning step, supplies an example, or narrows the answer.
 * Uncertainty is never silently resolved: it is handled like substantive
 * assistance and requires the learner's explicit consent on an independent
 * attempt.
 */
public enum ClarificationClassification {
    PROCEDURAL,
    SUBSTANTIVE,
    UNCERTAIN
}
