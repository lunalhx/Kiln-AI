package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public sealed interface ApplyGenerationDraft permits ApplyGenerationDraft.TaskReady, ApplyGenerationDraft.SourceGap {

    String SCHEMA = "apply_generation/v1";

    String schema();

    String outcome();

    record TaskReady(
            String schema,
            String outcome,
            String learnerTaskText,
            PrivateAssessorFacts privateAssessorFacts
    ) implements ApplyGenerationDraft {
    }

    record SourceGap(
            String schema,
            String outcome,
            SourceGapFacts sourceGap
    ) implements ApplyGenerationDraft {
    }

    record SourceGapFacts(String reasonCode, List<String> missingRequirementIds) {
        public SourceGapFacts {
            missingRequirementIds = List.copyOf(missingRequirementIds);
        }
    }

    static ApplyGenerationDraft parse(String json) {
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
        requireFields(root, Set.of("schema", "outcome", "learner_task_text", "private_assessor_facts"));
        return new TaskReady(
                schema,
                outcome,
                requiredText(root, "learner_task_text"),
                parseFacts(requiredObject(root, "private_assessor_facts"))
        );
    }

    private static PrivateAssessorFacts parseFacts(JsonNode facts) {
        requireFields(facts, Set.of(
                "proposed_expected_answer", "rubric_mapping", "source_trace", "equivalence_declaration"
        ));
        JsonNode expected = requiredObject(facts, "proposed_expected_answer");
        requireFields(expected, Set.of("expression"));

        List<PrivateAssessorFacts.RubricMapping> rubricMapping = new ArrayList<>();
        for (JsonNode entry : requiredArray(facts, "rubric_mapping")) {
            requireFields(entry, Set.of("mastery_criterion_id", "evidence_channels"));
            rubricMapping.add(new PrivateAssessorFacts.RubricMapping(
                    requiredText(entry, "mastery_criterion_id"),
                    requiredStringList(entry, "evidence_channels")
            ));
        }

        List<PrivateAssessorFacts.DraftSourceTraceEntry> sourceTrace = new ArrayList<>();
        for (JsonNode entry : requiredArray(facts, "source_trace")) {
            requireFields(entry, Set.of("source_document_id", "passage_id"));
            sourceTrace.add(new PrivateAssessorFacts.DraftSourceTraceEntry(
                    requiredText(entry, "source_document_id"),
                    requiredText(entry, "passage_id")
            ));
        }

        JsonNode equivalence = requiredObject(facts, "equivalence_declaration");
        requireFields(equivalence, Set.of("kind", "variables", "domain"));
        return new PrivateAssessorFacts(
                new PrivateAssessorFacts.ProposedExpectedAnswer(requiredText(expected, "expression")),
                rubricMapping,
                sourceTrace,
                new PrivateAssessorFacts.EquivalenceDeclaration(
                        requiredText(equivalence, "kind"),
                        requiredStringList(equivalence, "variables"),
                        requiredText(equivalence, "domain")
                )
        );
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
