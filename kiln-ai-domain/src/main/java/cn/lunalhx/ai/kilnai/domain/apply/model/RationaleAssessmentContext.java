package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.Objects;

/**
 * The bounded input for judging whether a Diagnostic rationale is applicable.
 * The assessor receives the private expected answer but never mutates state.
 */
public record RationaleAssessmentContext(
        String taskText,
        String expectedCanonicalExpression,
        String confirmedCanonicalExpression,
        String rationale
) {

    public RationaleAssessmentContext {
        Objects.requireNonNull(taskText, "taskText must not be null");
        Objects.requireNonNull(expectedCanonicalExpression, "expectedCanonicalExpression must not be null");
        Objects.requireNonNull(confirmedCanonicalExpression, "confirmedCanonicalExpression must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
    }
}
