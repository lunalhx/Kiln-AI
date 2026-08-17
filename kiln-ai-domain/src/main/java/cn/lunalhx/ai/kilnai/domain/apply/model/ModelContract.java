package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Strict closed-contract JSON parsing for model outputs. Violations become
 * normalized codes; raw JSON and parser details never enter the exception.
 */
public final class ModelContract {

    private ModelContract() {
    }

    public static JsonNode object(String json) {
        JsonNode root;
        try {
            root = ApplyJson.readTree(json);
        } catch (ApplyDraftException exception) {
            throw invalid("invalid_json");
        }
        if (root == null || !root.isObject()) {
            throw invalid("invalid_json");
        }
        return root;
    }

    public static void requireExactFields(JsonNode object, Set<String> allowed) {
        Set<String> actual = new HashSet<>();
        object.fieldNames().forEachRemaining(actual::add);
        if (!allowed.containsAll(actual)) {
            throw invalid("unknown_field");
        }
        if (!actual.containsAll(allowed)) {
            throw invalid("missing_field");
        }
    }

    public static String requiredText(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null) {
            throw invalid("missing_field");
        }
        if (node.isNull()) {
            throw invalid("null_required");
        }
        if (!node.isTextual()) {
            throw invalid("invalid_type");
        }
        String value = node.textValue();
        if (value.isBlank()) {
            throw invalid("invalid_type");
        }
        return value;
    }

    public static String requiredSchema(JsonNode parent, String expected) {
        String schema = requiredText(parent, "schema");
        if (!expected.equals(schema)) {
            throw invalid("wrong_schema");
        }
        return schema;
    }

    public static <E extends Enum<E>> E requiredEnum(JsonNode parent, String field, Class<E> type) {
        String value = requiredText(parent, field);
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw invalid("invalid_enum");
        }
    }

    public static JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode node = requiredNode(parent, field);
        if (!node.isObject()) {
            throw invalid("invalid_collection");
        }
        return node;
    }

    public static JsonNode requiredArray(JsonNode parent, String field) {
        JsonNode node = requiredNode(parent, field);
        if (!node.isArray()) {
            throw invalid("invalid_collection");
        }
        return node;
    }

    public static List<String> requiredStringList(JsonNode parent, String field) {
        JsonNode array = requiredArray(parent, field);
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            if (item == null || item.isNull()) {
                throw invalid("null_required");
            }
            if (!item.isTextual() || item.textValue().isBlank()) {
                throw invalid("invalid_collection");
            }
            values.add(item.textValue());
        }
        return List.copyOf(values);
    }

    public static ModelContractInvalidException invalid(String code) {
        return new ModelContractInvalidException(List.of(code));
    }

    private static JsonNode requiredNode(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null) {
            throw invalid("missing_field");
        }
        if (node.isNull()) {
            throw invalid("null_required");
        }
        return node;
    }
}
