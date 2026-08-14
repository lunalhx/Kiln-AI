package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.Objects;

/**
 * The bounded input shared verbatim by isolated Assessment and Response
 * Verification. It carries the same raw and learner-confirmed canonical input
 * plus the deterministic mathematical-check result; neither evaluator ever
 * sees the other's judgment.
 */
public record ResponseAssessmentContext(
        String taskText,
        String expectedCanonicalExpression,
        String confirmedCanonicalExpression,
        String rawAnswer,
        String rationale,
        AttemptPurpose purpose,
        EquivalenceOutcome deterministicOutcome
) {

    public ResponseAssessmentContext {
        Objects.requireNonNull(taskText, "taskText must not be null");
        Objects.requireNonNull(expectedCanonicalExpression, "expectedCanonicalExpression must not be null");
        Objects.requireNonNull(confirmedCanonicalExpression, "confirmedCanonicalExpression must not be null");
        Objects.requireNonNull(rawAnswer, "rawAnswer must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(deterministicOutcome, "deterministicOutcome must not be null");
    }
}
