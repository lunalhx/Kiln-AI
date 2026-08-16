package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The closed {@code explain_generation/v1} model contract of the Explain
 * Profile: a teaching-ready draft with one targeted principle summary and
 * exactly one complete worked example whose ordered steps each carry an
 * expression, an approved rule reference, and an explanation — or a closed
 * source-gap outcome. Unknown fields and generic private maps are rejected.
 */
public sealed interface ExplainGenerationDraft
        permits ExplainGenerationDraft.TeachingReady, ExplainGenerationDraft.SourceGap {

    String SCHEMA = "explain_generation/v1";

    String schema();

    String outcome();

    record TeachingReady(
            String schema,
            String outcome,
            String principleSummary,
            WorkedExample workedExample,
            List<SourceTraceEntry> sourceTrace
    ) implements ExplainGenerationDraft {

        public TeachingReady {
            sourceTrace = List.copyOf(sourceTrace);
        }
    }

    record WorkedExample(String problem, List<Step> steps, String finalResult) {

        public WorkedExample {
            steps = List.copyOf(steps);
        }
    }

    record Step(String expression, String ruleReference, String explanation) {
    }

    record SourceTraceEntry(String sourceDocumentId, String passageId) {
    }

    record SourceGap(
            String schema,
            String outcome,
            SourceGapFacts sourceGap
    ) implements ExplainGenerationDraft {
    }

    record SourceGapFacts(String reasonCode, List<String> missingRequirementIds) {

        public SourceGapFacts {
            missingRequirementIds = List.copyOf(missingRequirementIds);
        }
    }

    static ExplainGenerationDraft parse(String json) {
        JsonNode root = ApplyJson.readTree(json);
        if (root == null || !root.isObject()) {
            throw new ApplyDraftException("draft must be a JSON object");
        }
        String schema = requiredText(root, "schema");
        if (!SCHEMA.equals(schema)) {
            throw new ApplyDraftException("unsupported schema: " + schema);
        }
        String outcome = requiredText(root, "outcome");
        return switch (outcome) {
            case "teaching_ready" -> parseTeachingReady(root, schema, outcome);
            case "source_gap" -> parseSourceGap(root, schema, outcome);
            default -> throw new ApplyDraftException("unknown outcome: " + outcome);
        };
    }

    private static TeachingReady parseTeachingReady(JsonNode root, String schema, String outcome) {
        requireFields(root, Set.of("schema", "outcome", "principle_summary", "worked_example", "source_trace"));
        WorkedExample workedExample = parseWorkedExample(requiredObject(root, "worked_example"));
        List<SourceTraceEntry> sourceTrace = new ArrayList<>();
        for (JsonNode entry : requiredArray(root, "source_trace")) {
            requireFields(entry, Set.of("source_document_id", "passage_id"));
            sourceTrace.add(new SourceTraceEntry(
                    requiredText(entry, "source_document_id"),
                    requiredText(entry, "passage_id")));
        }
        return new TeachingReady(
                schema,
                outcome,
                requiredText(root, "principle_summary"),
                workedExample,
                sourceTrace);
    }

    private static WorkedExample parseWorkedExample(JsonNode node) {
        requireFields(node, Set.of("problem", "steps", "final_result"));
        List<Step> steps = new ArrayList<>();
        for (JsonNode step : requiredArray(node, "steps")) {
            requireFields(step, Set.of("expression", "rule_reference", "explanation"));
            steps.add(new Step(
                    requiredText(step, "expression"),
                    requiredText(step, "rule_reference"),
                    requiredText(step, "explanation")));
        }
        return new WorkedExample(
                requiredText(node, "problem"),
                steps,
                requiredText(node, "final_result"));
    }

    private static SourceGap parseSourceGap(JsonNode root, String schema, String outcome) {
        requireFields(root, Set.of("schema", "outcome", "source_gap"));
        JsonNode gap = requiredObject(root, "source_gap");
        requireFields(gap, Set.of("reason_code", "missing_requirement_ids"));
        return new SourceGap(
                schema,
                outcome,
                new SourceGapFacts(requiredText(gap, "reason_code"),
                        requiredStringList(gap, "missing_requirement_ids")));
    }

    private static void requireFields(JsonNode object, Set<String> allowed) {
        Set<String> actual = new HashSet<>();
        object.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(allowed)) {
            throw new ApplyDraftException("draft fields must be exactly " + allowed + " but were " + actual);
        }
    }

    private static JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isObject()) {
            throw new ApplyDraftException("missing or invalid object field: " + field);
        }
        return node;
    }

    private static JsonNode requiredArray(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isArray()) {
            throw new ApplyDraftException("missing or invalid array field: " + field);
        }
        return node;
    }

    private static String requiredText(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw new ApplyDraftException("missing or blank text field: " + field);
        }
        return node.textValue();
    }

    private static List<String> requiredStringList(JsonNode parent, String field) {
        JsonNode node = requiredArray(parent, field);
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
