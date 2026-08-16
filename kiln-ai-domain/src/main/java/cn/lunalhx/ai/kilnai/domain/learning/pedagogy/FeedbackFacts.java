package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * The sanitized, structured projection of accepted Assessment and evidence
 * validation supplied to the Pedagogy Agent (CONTEXT.md). It identifies
 * satisfied and missing Rubric criteria and closed error-dimension reason
 * codes without exposing raw learner answers, hidden assessment reasoning, or
 * answer keys, and carries the relevant assistance and readiness facts. It is
 * a pure read-only value: the agent can never write Learning State through it.
 */
public record FeedbackFacts(
        @JsonProperty("satisfied_criteria") List<String> satisfiedCriteria,
        @JsonProperty("missing_criteria") List<String> missingCriteria,
        @JsonProperty("error_dimensions") List<String> errorDimensions,
        @JsonProperty("highest_exposed_hint_level") int highestExposedHintLevel,
        @JsonProperty("assistance_trace") List<String> assistanceTrace,
        @JsonProperty("practice_readiness_satisfied") boolean practiceReadinessSatisfied
) {

    public FeedbackFacts {
        satisfiedCriteria = List.copyOf(satisfiedCriteria);
        missingCriteria = List.copyOf(missingCriteria);
        errorDimensions = List.copyOf(errorDimensions);
        assistanceTrace = List.copyOf(assistanceTrace);
        Objects.requireNonNull(satisfiedCriteria, "satisfiedCriteria must not be null");
        Objects.requireNonNull(missingCriteria, "missingCriteria must not be null");
        Objects.requireNonNull(errorDimensions, "errorDimensions must not be null");
        Objects.requireNonNull(assistanceTrace, "assistanceTrace must not be null");
    }
}
