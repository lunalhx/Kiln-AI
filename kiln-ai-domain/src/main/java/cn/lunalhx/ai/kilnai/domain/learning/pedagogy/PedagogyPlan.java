package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The typed Pedagogy Plan (CONTEXT.md): concise learner feedback, exactly one
 * legal next Teaching Action, teaching intent, required Capability Tags,
 * preferred Strategy Tags, and a private reason code. It is the validated
 * model artifact of the Pedagogy Agent; the closed {@code pedagogy_plan/v1}
 * contract rejects unknown fields and blank content, and the graph validates
 * the selected action against the Workflow Guard's legal set before routing.
 */
public record PedagogyPlan(
        String feedbackSummary,
        TeachingAction action,
        String intent,
        List<String> capabilityTags,
        List<String> strategyTags,
        String reason
) {

    public static final String SCHEMA = "pedagogy_plan/v1";

    public PedagogyPlan {
        Objects.requireNonNull(feedbackSummary, "feedbackSummary must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(intent, "intent must not be null");
        capabilityTags = List.copyOf(capabilityTags);
        strategyTags = List.copyOf(strategyTags);
        Objects.requireNonNull(capabilityTags, "capabilityTags must not be null");
        Objects.requireNonNull(strategyTags, "strategyTags must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }

    static PedagogyPlan parse(String json) {
        JsonNode root = ApplyJson.readTree(json);
        if (root == null || !root.isObject()) {
            throw new ApplyDraftException("pedagogy plan must be a JSON object");
        }
        requireFields(root, Set.of("schema", "feedback_summary", "action", "intent",
                "capability_tags", "strategy_tags", "reason"));
        String schema = requiredText(root, "schema");
        if (!SCHEMA.equals(schema)) {
            throw new ApplyDraftException("unsupported pedagogy plan schema: " + schema);
        }
        return new PedagogyPlan(
                requiredText(root, "feedback_summary"),
                TeachingAction.fromJson(requiredText(root, "action")),
                requiredText(root, "intent"),
                requiredStringList(root, "capability_tags"),
                requiredStringList(root, "strategy_tags"),
                requiredText(root, "reason"));
    }

    private static void requireFields(JsonNode object, Set<String> allowed) {
        Set<String> actual = new HashSet<>();
        object.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(allowed)) {
            throw new ApplyDraftException("pedagogy plan fields must be exactly " + allowed + " but were " + actual);
        }
    }

    private static String requiredText(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw new ApplyDraftException("missing or blank text field: " + field);
        }
        return node.textValue();
    }

    private static List<String> requiredStringList(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isArray()) {
            throw new ApplyDraftException("missing or invalid array field: " + field);
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual() || item.textValue().isBlank()) {
                throw new ApplyDraftException("field " + field + " must contain only non-blank strings");
            }
            values.add(item.textValue());
        }
        return List.copyOf(values);
    }
}
