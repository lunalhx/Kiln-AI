package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TaskVerificationVerdict(
        String schema,
        Verdict verdict,
        Map<String, CheckResult> checks,
        List<String> reasonCodes
) {

    public static final String SCHEMA = "task_verification/v1";

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
