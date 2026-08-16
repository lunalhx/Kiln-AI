package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPort;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.TeachingAction;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The scripted Pedagogy Agent model port. In the default auto-policy mode the
 * model parses the serialized {@code pedagogy_execution_context/v1} JSON and
 * returns a valid plan that selects the first legal action (the Workflow
 * Guard's deterministic fallback, which the guard orders first), reproducing
 * the historical deterministic routing for tests that do not script the
 * choice. A scripted response list instead returns the raw drafts in call
 * order, so tests can drive alternative actions, well-formed but illegal
 * plans, or invalid output that must be repaired and discarded.
 */
public final class ScriptedPedagogyModel implements PedagogyPort {

    public static final String DEFAULT_FEEDBACK = "好的，我们继续。";
    public static final String DEFAULT_INTENT = "continue";
    public static final String DEFAULT_REASON = "scripted";

    private final List<String> responses;
    private final List<Call> calls = new ArrayList<>();

    public ScriptedPedagogyModel() {
        this.responses = List.of();
    }

    public ScriptedPedagogyModel(List<String> responses) {
        this.responses = List.copyOf(responses);
    }

    /**
     * A scripted model that returns one valid canned plan per decision, in
     * call order, with the default feedback.
     */
    public static ScriptedPedagogyModel scripted(TeachingAction... actions) {
        List<String> plans = new ArrayList<>();
        for (TeachingAction action : actions) {
            plans.add(planJson(action, DEFAULT_FEEDBACK));
        }
        return new ScriptedPedagogyModel(plans);
    }

    @Override
    public String generate(String compiledSystemPrompt, String executionContextJson) {
        calls.add(new Call(compiledSystemPrompt, executionContextJson));
        if (responses.isEmpty()) {
            return autoPlan(executionContextJson);
        }
        if (calls.size() > responses.size()) {
            throw new IllegalStateException("scripted pedagogy model exhausted: no more scripted responses");
        }
        return responses.get(calls.size() - 1);
    }

    /**
     * The auto-policy plan: parses the closed execution context and selects
     * the first legal action (the Guard's deterministic fallback, ordered
     * first), with the canned feedback and intent.
     */
    private String autoPlan(String executionContextJson) {
        JsonNode root = ApplyJson.readTree(executionContextJson);
        String action = root.path("legal_actions").path(0).asText();
        return planJson(action, DEFAULT_FEEDBACK, DEFAULT_INTENT, DEFAULT_REASON);
    }

    public static String planJson(String action, String feedback) {
        return planJson(action, feedback, DEFAULT_INTENT, DEFAULT_REASON);
    }

    public static String planJson(String action, String feedback, String intent, String reason) {
        return """
                {
                  "schema": "pedagogy_plan/v1",
                  "feedback_summary": "%s",
                  "action": "%s",
                  "intent": "%s",
                  "capability_tags": [],
                  "strategy_tags": [],
                  "reason": "%s"
                }
                """.formatted(feedback, action, intent, reason);
    }

    public static String planJson(TeachingAction action, String feedback) {
        return planJson(action.jsonName(), feedback);
    }

    public List<Call> calls() {
        return List.copyOf(calls);
    }

    public String lastContextJson() {
        return calls.get(calls.size() - 1).contextJson();
    }

    public record Call(String systemPrompt, String contextJson) {

        public Call {
            Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
            Objects.requireNonNull(contextJson, "contextJson must not be null");
        }
    }
}
