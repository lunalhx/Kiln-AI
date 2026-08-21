package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

/**
 * Learner-safe projection of one Flow-scoped Diagnostic Plan. Plan identity,
 * source basis, readiness criteria, rationale policy, and assessment facts
 * deliberately stay outside this value object.
 */
public record DiagnosticProgress(int completedAttempts, int maximumAttempts) {

    public DiagnosticProgress {
        if (completedAttempts < 0) {
            throw new IllegalArgumentException("completedAttempts must not be negative");
        }
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException("maximumAttempts must be positive");
        }
        if (completedAttempts > maximumAttempts) {
            throw new IllegalArgumentException("completedAttempts must not exceed maximumAttempts");
        }
    }
}
