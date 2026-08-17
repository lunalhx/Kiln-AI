package cn.lunalhx.ai.kilnai.domain.apply.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record TaskVerificationVerdict(
        String schema,
        Verdict verdict,
        Map<String, CheckResult> checks,
        List<String> reasonCodes
) {

    public static final String SCHEMA = "task_verification/v1";

    private static final Set<String> KNOWN_CHECKS = Set.of(
            "answer_correctness",
            "answer_clarity",
            "rubric_alignment",
            "source_grounding",
            "blueprint_compliance",
            "anchor_grounding",
            "learner_boundary");

    public TaskVerificationVerdict {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(checks, "checks must not be null");
        Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
        checks = Map.copyOf(checks);
        reasonCodes = List.copyOf(reasonCodes);
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("unsupported verification schema: " + schema);
        }
        if (verdict == Verdict.PASS) {
            if (checks.isEmpty() || checks.values().stream().anyMatch(c -> c != CheckResult.PASS)) {
                throw new IllegalArgumentException("a pass verdict requires every check to pass");
            }
        } else if (checks.isEmpty() || checks.values().stream().allMatch(c -> c == CheckResult.PASS)) {
            throw new IllegalArgumentException("a non-pass verdict requires at least one failing check");
        }
    }

    public static TaskVerificationVerdict parse(String json) {
        JsonNode root = ModelContract.object(json);
        ModelContract.requireExactFields(root, Set.of("schema", "verdict", "checks", "reason_codes"));
        Map<String, CheckResult> checks = parseChecks(ModelContract.requiredObject(root, "checks"));
        try {
            return new TaskVerificationVerdict(
                    ModelContract.requiredSchema(root, SCHEMA),
                    ModelContract.requiredEnum(root, "verdict", Verdict.class),
                    checks,
                    ModelContract.requiredStringList(root, "reason_codes"));
        } catch (IllegalArgumentException exception) {
            throw ModelContract.invalid("invalid_collection");
        }
    }

    private static Map<String, CheckResult> parseChecks(JsonNode checks) {
        if (checks.isEmpty()) {
            throw ModelContract.invalid("invalid_collection");
        }
        Map<String, CheckResult> parsed = new LinkedHashMap<>();
        checks.fieldNames().forEachRemaining(name -> {
            if (!KNOWN_CHECKS.contains(name)) {
                throw ModelContract.invalid("unknown_field");
            }
            JsonNode value = checks.get(name);
            if (value == null || value.isNull()) {
                throw ModelContract.invalid("null_required");
            }
            if (!value.isTextual()) {
                throw ModelContract.invalid("invalid_type");
            }
            try {
                parsed.put(name, CheckResult.valueOf(
                        value.textValue().toUpperCase(Locale.ROOT).replace('-', '_')));
            } catch (IllegalArgumentException exception) {
                throw ModelContract.invalid("invalid_enum");
            }
        });
        return Map.copyOf(parsed);
    }

    public boolean passed() {
        return verdict == Verdict.PASS;
    }

    public enum Verdict {
        PASS,
        REJECT,
        INCONCLUSIVE
    }

    public enum CheckResult {
        PASS,
        REJECT,
        INCONCLUSIVE
    }
}
