package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * The deterministic prompt compiler of the Pedagogy Agent. The system prompt
 * is one fixed English instruction (the agent is a non-teaching model role,
 * not a Teaching Node Profile, so no Skill Bundle stack is compiled), and the
 * execution data is serialized separately as the closed
 * {@code pedagogy_execution_context/v1} JSON object: only the sanitized
 * Feedback Facts and the Workflow Guard's closed legal-action set. Raw
 * answers, expected answers, assessment reasoning, Skill ids, and unrestricted
 * state history are never part of the context.
 */
public final class PedagogyPromptCompiler {

    public static final int INSTRUCTION_BUDGET = 16_000;

    private static final String SYSTEM_PROMPT = """
            You are the pedagogy planner of a mathematics learning system. You
            choose the single next teaching action from the closed legal set
            and write a concise learner-facing feedback summary.

            Rules:
            - Choose exactly one action from the legal_actions list. Never
              invent an action outside it, and never name a Skill or Bundle.
            - The feedback_summary is a short, neutral, learner-facing message
              in the learner's language. It never contains expected answers,
              assessment reasoning, source ids, or private facts.
            - The intent describes the pedagogical purpose in one short phrase.
            - capability_tags and strategy_tags are optional lists of short
              registry-controlled tag names.
            - The reason is a private, concise reason code phrase; it never
              reaches the learner.
            - Respond with ONLY the JSON object, no commentary.
            """;

    private static final String RESPONSE_CONTRACT = """
            {
              "schema": "pedagogy_plan/v1",
              "feedback_summary": "concise learner-facing feedback",
              "action": "one of: explain, apply_practice, teach_back, independent_test",
              "intent": "short pedagogical purpose",
              "capability_tags": [],
              "strategy_tags": [],
              "reason": "private reason"
            }
            """;

    public String compile() {
        String compiled = SYSTEM_PROMPT.trim() + "\n\n" + RESPONSE_CONTRACT.trim() + "\n";
        if (compiled.length() > INSTRUCTION_BUDGET) {
            throw new IllegalStateException("pedagogy prompt budget exceeded: " + compiled.length());
        }
        return compiled;
    }

    public String serializeContext(FeedbackFacts facts, List<TeachingAction> legalActions) {
        Objects.requireNonNull(facts, "facts must not be null");
        Objects.requireNonNull(legalActions, "legalActions must not be null");
        return ApplyJson.write(new PedagogyExecutionContext(facts, legalActions));
    }

    /**
     * The closed {@code pedagogy_execution_context/v1} execution data: the
     * sanitized Feedback Facts and the closed legal-action set in the Guard's
     * canonical order (deterministic fallback first), serialized strictly so
     * unknown fields fail.
     */
    record PedagogyExecutionContext(
            @JsonProperty("feedback_facts") FeedbackFacts feedbackFacts,
            @JsonProperty("legal_actions") List<TeachingAction> legalActions
    ) {

        PedagogyExecutionContext {
            Objects.requireNonNull(feedbackFacts, "feedbackFacts must not be null");
            Objects.requireNonNull(legalActions, "legalActions must not be null");
            legalActions = List.copyOf(legalActions);
        }
    }
}
