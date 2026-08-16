package cn.lunalhx.ai.kilnai.domain.learning.graph;

/**
 * The bounded model port of the Clarification Gate's classifier: one stateless
 * Small-model call per free-form clarification that returns the closed
 * classification of the learner message against the learner-visible task text.
 * The classifier never answers the question, loads a Teaching Node Profile,
 * or mutates state — the gate owns the response routing.
 */
public interface ClarificationClassifierPort {

    ClarificationClassification classify(String message, String taskText);
}
