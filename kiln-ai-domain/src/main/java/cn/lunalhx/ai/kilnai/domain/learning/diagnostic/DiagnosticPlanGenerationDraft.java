package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Closed output contract of Concept Preparation. A provider must explicitly
 * return either a complete Plan candidate or a Source Gap; an arbitrary JSON
 * object is not treated as an accepted Plan.
 */
public sealed interface DiagnosticPlanGenerationDraft
        permits DiagnosticPlanGenerationDraft.PlanReady, DiagnosticPlanGenerationDraft.SourceGap {

    String SCHEMA = "diagnostic_plan_generation/v1";

    String schema();

    String outcome();

    record PlanReady(String schema, String outcome, DiagnosticPlan plan)
            implements DiagnosticPlanGenerationDraft {
    }

    record SourceGap(String schema, String outcome, SourceGapFacts facts)
            implements DiagnosticPlanGenerationDraft {
    }

    record SourceGapFacts(String reasonCode, List<String> missingRequirementIds) {
        public SourceGapFacts {
            if (reasonCode == null || reasonCode.isBlank()) {
                throw new IllegalArgumentException("reasonCode must not be blank");
            }
            missingRequirementIds = List.copyOf(missingRequirementIds);
        }
    }

    static DiagnosticPlanGenerationDraft parse(String json) {
        JsonNode root = ApplyJson.readTree(json);
        if (root == null || !root.isObject()) {
            throw new ApplyDraftException("Diagnostic Plan generation result must be a JSON object");
        }
        String schema = requiredText(root, "schema");
        if (!SCHEMA.equals(schema)) {
            throw new ApplyDraftException("unsupported Diagnostic Plan generation schema: " + schema);
        }
        String outcome = requiredText(root, "outcome");
        return switch (outcome) {
            case "plan_ready" -> parsePlanReady(root, schema, outcome);
            case "source_gap" -> parseSourceGap(root, schema, outcome);
            default -> throw new ApplyDraftException("unknown Diagnostic Plan generation outcome: " + outcome);
        };
    }

    private static PlanReady parsePlanReady(JsonNode root, String schema, String outcome) {
        requireFields(root, Set.of("schema", "outcome", "plan"));
        return new PlanReady(schema, outcome, parsePlan(requiredObject(root, "plan")));
    }

    private static SourceGap parseSourceGap(JsonNode root, String schema, String outcome) {
        requireFields(root, Set.of("schema", "outcome", "source_gap"));
        JsonNode gap = requiredObject(root, "source_gap");
        requireFields(gap, Set.of("reason_code", "missing_requirement_ids"));
        return new SourceGap(schema, outcome, new SourceGapFacts(
                requiredText(gap, "reason_code"), requiredStringList(gap, "missing_requirement_ids")));
    }

    private static DiagnosticPlan parsePlan(JsonNode root) {
        requireFields(root, Set.of(
                "schema", "id", "version", "target_concept_id",
                "concept_contract_id", "concept_contract_version",
                "mastery_rubric_id", "mastery_rubric_version",
                "target_readiness_criterion_ids", "supporting_concepts", "dependency_order",
                "source_basis", "coverage_rule", "termination_rule", "rationale_policy",
                "maximum_attempts"));
        return new DiagnosticPlan(
                requiredText(root, "schema"),
                requiredText(root, "id"),
                requiredText(root, "version"),
                requiredUuid(root, "target_concept_id"),
                requiredText(root, "concept_contract_id"),
                requiredText(root, "concept_contract_version"),
                requiredText(root, "mastery_rubric_id"),
                requiredText(root, "mastery_rubric_version"),
                requiredStringList(root, "target_readiness_criterion_ids"),
                parseSupportingConcepts(requiredArray(root, "supporting_concepts")),
                requiredStringList(root, "dependency_order"),
                parseSourceBasis(requiredObject(root, "source_basis")),
                new DiagnosticPlan.CoverageRule(parseRuleKind(root, "coverage_rule")),
                new DiagnosticPlan.TerminationRule(parseRuleKind(root, "termination_rule")),
                parseRationalePolicy(requiredObject(root, "rationale_policy")),
                requiredInt(root, "maximum_attempts"));
    }

    private static List<DiagnosticPlan.SupportingConcept> parseSupportingConcepts(JsonNode array) {
        List<DiagnosticPlan.SupportingConcept> concepts = new ArrayList<>();
        for (JsonNode item : array) {
            requireFields(item, Set.of(
                    "concept_id", "required", "mastery_rubric_id", "mastery_rubric_version",
                    "mastery_criterion_id", "source_basis", "dependencies"));
            concepts.add(new DiagnosticPlan.SupportingConcept(
                    requiredText(item, "concept_id"),
                    requiredBoolean(item, "required"),
                    requiredText(item, "mastery_rubric_id"),
                    requiredText(item, "mastery_rubric_version"),
                    requiredText(item, "mastery_criterion_id"),
                    parseSourceBasis(requiredObject(item, "source_basis")),
                    requiredStringList(item, "dependencies")));
        }
        return List.copyOf(concepts);
    }

    private static DiagnosticPlan.SourceBasis parseSourceBasis(JsonNode object) {
        requireFields(object, Set.of("source_pack_id", "source_pack_version", "passage_ids"));
        return new DiagnosticPlan.SourceBasis(
                requiredText(object, "source_pack_id"),
                requiredText(object, "source_pack_version"),
                requiredStringList(object, "passage_ids"));
    }

    private static String parseRuleKind(JsonNode parent, String field) {
        JsonNode object = requiredObject(parent, field);
        requireFields(object, Set.of("kind"));
        return requiredText(object, "kind");
    }

    private static DiagnosticPlan.RationalePolicy parseRationalePolicy(JsonNode object) {
        requireFields(object, Set.of("mode", "criterion_ids"));
        return new DiagnosticPlan.RationalePolicy(
                requiredText(object, "mode"), requiredStringList(object, "criterion_ids"));
    }

    private static void requireFields(JsonNode object, Set<String> allowed) {
        if (object == null || !object.isObject()) {
            throw new ApplyDraftException("Diagnostic Plan generation field must be an object");
        }
        Set<String> actual = new HashSet<>();
        object.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(allowed)) {
            throw new ApplyDraftException("Diagnostic Plan generation fields must be exactly " + allowed);
        }
    }

    private static JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode node = requiredNode(parent, field);
        if (!node.isObject()) {
            throw new ApplyDraftException("field must be an object: " + field);
        }
        return node;
    }

    private static JsonNode requiredArray(JsonNode parent, String field) {
        JsonNode node = requiredNode(parent, field);
        if (!node.isArray()) {
            throw new ApplyDraftException("field must be an array: " + field);
        }
        return node;
    }

    private static JsonNode requiredNode(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            throw new ApplyDraftException("missing required field: " + field);
        }
        return node;
    }

    private static String requiredText(JsonNode parent, String field) {
        JsonNode node = requiredNode(parent, field);
        if (!node.isTextual() || node.textValue().isBlank()) {
            throw new ApplyDraftException("field must be a non-blank string: " + field);
        }
        return node.textValue();
    }

    private static List<String> requiredStringList(JsonNode parent, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : requiredArray(parent, field)) {
            if (!item.isTextual() || item.textValue().isBlank()) {
                throw new ApplyDraftException("field must contain only non-blank strings: " + field);
            }
            values.add(item.textValue());
        }
        return List.copyOf(values);
    }

    private static UUID requiredUuid(JsonNode parent, String field) {
        try {
            return UUID.fromString(requiredText(parent, field));
        } catch (IllegalArgumentException exception) {
            throw new ApplyDraftException("field must be a UUID: " + field);
        }
    }

    private static int requiredInt(JsonNode parent, String field) {
        JsonNode node = requiredNode(parent, field);
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new ApplyDraftException("field must be an integer: " + field);
        }
        return node.intValue();
    }

    private static boolean requiredBoolean(JsonNode parent, String field) {
        JsonNode node = requiredNode(parent, field);
        if (!node.isBoolean()) {
            throw new ApplyDraftException("field must be a boolean: " + field);
        }
        return node.booleanValue();
    }
}
