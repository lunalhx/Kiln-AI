package cn.lunalhx.ai.kilnai.domain.gate;

import java.util.List;
import java.util.Objects;

public record GateResult<T>(GateOutcome outcome, T accepted, List<GateViolation> violations) {

    public GateResult {
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(violations, "violations must not be null");
        violations = List.copyOf(violations);
        if (outcome == GateOutcome.PASSED) {
            Objects.requireNonNull(accepted, "accepted artifact must not be null");
        }
    }

    public static <T> GateResult<T> passed(T artifact) {
        return new GateResult<>(GateOutcome.PASSED, artifact, List.of());
    }

    public static <T> GateResult<T> repairable(List<GateViolation> violations) {
        return new GateResult<>(GateOutcome.REPAIRABLE, null, violations);
    }

    public static <T> GateResult<T> rejected(List<GateViolation> violations) {
        return new GateResult<>(GateOutcome.REJECTED, null, violations);
    }

    public T artifact() {
        if (outcome != GateOutcome.PASSED) {
            throw new IllegalStateException("no accepted artifact for " + outcome);
        }
        return accepted;
    }
}
