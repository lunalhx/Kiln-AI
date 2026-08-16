package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The closed model-produced {@code hint_generation/v1} contract for the
 * reference Hint Profile: a discriminated outcome that is either a complete
 * five-level ladder for the current Practice task, or a closed source-gap
 * outcome. Unknown fields, a wrong entry cardinality, H5-only fields on
 * H1-H4, and malformed payloads are rejected at parse time so the Hint Gate
 * never sees an open draft.
 */
public sealed interface HintGenerationDraft permits HintGenerationDraft.LadderReady, HintGenerationDraft.SourceGap {

    String SCHEMA = "hint_generation/v1";

    String schema();

    String outcome();

    record SourceTraceRef(String sourceDocumentId, String passageId) {
        public SourceTraceRef {
            requireNonBlank(sourceDocumentId, "sourceDocumentId must not be blank");
            requireNonBlank(passageId, "passageId must not be blank");
        }
    }

    /**
     * One ladder entry. {@code reasoningSteps} and {@code proposedFinalAnswer}
     * are non-null only for the H5 reveal; the strict parser rejects them on
     * any lower level so the reveal cannot be smuggled into H1-H4.
     */
    record Entry(
            int level,
            String disclosureKind,
            String learnerContent,
            List<SourceTraceRef> sourceTrace,
            List<String> reasoningSteps,
            String proposedFinalAnswer
    ) {
        public Entry {
            requireNonBlank(disclosureKind, "disclosureKind must not be blank");
            requireNonBlank(learnerContent, "learnerContent must not be blank");
            sourceTrace = List.copyOf(sourceTrace);
            reasoningSteps = reasoningSteps == null ? null : List.copyOf(reasoningSteps);
        }
    }

    record LadderReady(String schema, String outcome, List<Entry> entries) implements HintGenerationDraft {
        public LadderReady {
            entries = List.copyOf(entries);
        }
    }

    record SourceGapFacts(String reasonCode, List<String> missingRequirementIds) {
        public SourceGapFacts {
            missingRequirementIds = List.copyOf(missingRequirementIds);
        }
    }

    record SourceGap(String schema, String outcome, SourceGapFacts sourceGap) implements HintGenerationDraft {
    }

    static HintGenerationDraft parse(String json) {
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
            case "ladder_ready" -> parseLadderReady(root, schema, outcome);
            case "source_gap" -> parseSourceGap(root, schema, outcome);
            default -> throw new ApplyDraftException("unknown outcome: " + outcome);
        };
    }

    private static LadderReady parseLadderReady(JsonNode root, String schema, String outcome) {
        requireFields(root, Set.of("schema", "outcome", "entries"));
        JsonNode entriesNode = requiredArray(root, "entries");
        if (entriesNode.size() != 5) {
            throw new ApplyDraftException("hint_generation/v1 requires exactly five entries but got " + entriesNode.size());
        }
        List<Entry> entries = new ArrayList<>();
        for (JsonNode node : entriesNode) {
            entries.add(parseEntry(node));
        }
        return new LadderReady(schema, outcome, entries);
    }

    private static Entry parseEntry(JsonNode node) {
        if (!node.isObject()) {
            throw new ApplyDraftException("each ladder entry must be a JSON object");
        }
        int level = requiredInt(node, "level");
        if (level < 1 || level > 5) {
            throw new ApplyDraftException("ladder entry level must be between 1 and 5 but was " + level);
        }
        Set<String> fields = new HashSet<>();
        node.fieldNames().forEachRemaining(fields::add);
        Set<String> expected = level == 5
                ? Set.of("level", "disclosure_kind", "learner_content", "source_trace",
                        "reasoning_steps", "proposed_final_answer")
                : Set.of("level", "disclosure_kind", "learner_content", "source_trace");
        if (!fields.equals(expected)) {
            throw new ApplyDraftException("entry fields must be exactly " + expected + " but were " + fields);
        }
        List<SourceTraceRef> sourceTrace = new ArrayList<>();
        for (JsonNode ref : requiredArray(node, "source_trace")) {
            if (!ref.isObject()) {
                throw new ApplyDraftException("source_trace entries must be JSON objects");
            }
            requireFields(ref, Set.of("source_document_id", "passage_id"));
            sourceTrace.add(new SourceTraceRef(
                    requiredText(ref, "source_document_id"), requiredText(ref, "passage_id")));
        }
        if (sourceTrace.isEmpty()) {
            throw new ApplyDraftException("every ladder entry must carry at least one source trace ref");
        }
        if (level == 5) {
            List<String> steps = new ArrayList<>();
            for (JsonNode step : requiredArray(node, "reasoning_steps")) {
                if (!step.isTextual() || step.textValue().isBlank()) {
                    throw new ApplyDraftException("reasoning_steps must contain only non-blank strings");
                }
                steps.add(step.textValue());
            }
            if (steps.isEmpty()) {
                throw new ApplyDraftException("the H5 reveal must carry at least one reasoning step");
            }
            return new Entry(level, requiredText(node, "disclosure_kind"),
                    requiredText(node, "learner_content"), sourceTrace, steps,
                    requiredText(node, "proposed_final_answer"));
        }
        return new Entry(level, requiredText(node, "disclosure_kind"),
                requiredText(node, "learner_content"), sourceTrace, null, null);
    }

    private static SourceGap parseSourceGap(JsonNode root, String schema, String outcome) {
        requireFields(root, Set.of("schema", "outcome", "source_gap"));
        JsonNode gap = requiredObject(root, "source_gap");
        requireFields(gap, Set.of("reason_code", "missing_requirement_ids"));
        return new SourceGap(
                schema,
                outcome,
                new SourceGapFacts(requiredText(gap, "reason_code"), requiredStringList(gap, "missing_requirement_ids"))
        );
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

    private static int requiredInt(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isInt()) {
            throw new ApplyDraftException("missing or invalid integer field: " + field);
        }
        return node.intValue();
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
