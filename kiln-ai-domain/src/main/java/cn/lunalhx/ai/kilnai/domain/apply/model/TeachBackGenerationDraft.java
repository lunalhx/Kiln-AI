package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The closed model-produced {@code teach_back_generation/v1} contract of the
 * reference Teach-back Profile: a discriminated outcome that is either one
 * task-ready short-text learner prompt with its three-dimensional Rubric
 * mapping, source trace, and anchor reference — or a closed source-gap
 * outcome. Unknown fields, a wrong Rubric mapping shape, a mismatched anchor
 * reference, and generic private artifact maps are rejected at parse time so
 * the Teach-back gate never sees an open draft. The draft must never carry a
 * verbatim expected explanation: the assessor judges the learner's own
 * explanation, and no expected explanation exists to leak.
 */
public sealed interface TeachBackGenerationDraft
        permits TeachBackGenerationDraft.TaskReady, TeachBackGenerationDraft.SourceGap {

    String SCHEMA = "teach_back_generation/v1";

    String schema();

    String outcome();

    record RubricEntry(String dimension, String masteryCriterion) {
        public RubricEntry {
            requireNonBlank(dimension, "dimension must not be blank");
            requireNonBlank(masteryCriterion, "masteryCriterion must not be blank");
        }
    }

    record SourceTraceEntry(String sourceDocumentId, String passageId) {
        public SourceTraceEntry {
            requireNonBlank(sourceDocumentId, "sourceDocumentId must not be blank");
            requireNonBlank(passageId, "passageId must not be blank");
        }
    }

    record AnchorReference(String anchorId, String anchorKind) {
        public AnchorReference {
            requireNonBlank(anchorId, "anchorId must not be blank");
            requireNonBlank(anchorKind, "anchorKind must not be blank");
        }
    }

    record TaskReady(
            String schema,
            String outcome,
            String learnerPrompt,
            List<RubricEntry> rubricMapping,
            List<SourceTraceEntry> sourceTrace,
            AnchorReference anchorReference
    ) implements TeachBackGenerationDraft {

        public TaskReady {
            sourceTrace = List.copyOf(sourceTrace);
            rubricMapping = List.copyOf(rubricMapping);
        }
    }

    record SourceGap(
            String schema,
            String outcome,
            SourceGapFacts sourceGap
    ) implements TeachBackGenerationDraft {
    }

    record SourceGapFacts(String reasonCode, List<String> missingRequirementIds) {
        public SourceGapFacts {
            missingRequirementIds = List.copyOf(missingRequirementIds);
        }
    }

    static TeachBackGenerationDraft parse(String json) {
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
            case "task_ready" -> parseTaskReady(root, schema, outcome);
            case "source_gap" -> parseSourceGap(root, schema, outcome);
            default -> throw new ApplyDraftException("unknown outcome: " + outcome);
        };
    }

    private static TaskReady parseTaskReady(JsonNode root, String schema, String outcome) {
        requireFields(root, Set.of("schema", "outcome", "learner_prompt", "rubric_mapping",
                "source_trace", "anchor_reference"));
        List<RubricEntry> rubricMapping = new ArrayList<>();
        for (JsonNode entry : requiredArray(root, "rubric_mapping")) {
            requireFields(entry, Set.of("dimension", "mastery_criterion"));
            rubricMapping.add(new RubricEntry(
                    requiredText(entry, "dimension"), requiredText(entry, "mastery_criterion")));
        }
        if (rubricMapping.isEmpty()) {
            throw new ApplyDraftException("rubric_mapping must not be empty");
        }
        List<SourceTraceEntry> sourceTrace = new ArrayList<>();
        for (JsonNode entry : requiredArray(root, "source_trace")) {
            requireFields(entry, Set.of("source_document_id", "passage_id"));
            sourceTrace.add(new SourceTraceEntry(
                    requiredText(entry, "source_document_id"), requiredText(entry, "passage_id")));
        }
        if (sourceTrace.isEmpty()) {
            throw new ApplyDraftException("source_trace must not be empty");
        }
        JsonNode anchor = requiredObject(root, "anchor_reference");
        requireFields(anchor, Set.of("anchor_id", "anchor_kind"));
        String anchorId = requiredText(anchor, "anchor_id");
        try {
            UUID.fromString(anchorId);
        } catch (IllegalArgumentException exception) {
            throw new ApplyDraftException("anchor_reference.anchor_id must be a valid UUID");
        }
        return new TaskReady(
                schema,
                outcome,
                requiredText(root, "learner_prompt"),
                rubricMapping,
                sourceTrace,
                new AnchorReference(anchorId, requiredText(anchor, "anchor_kind")));
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

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
