package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Objects;

/**
 * The learner-visible content of one Explain teaching interaction: a targeted
 * principle explanation and exactly one complete worked example. It carries no
 * source identities, Fingerprints, execution trace, or pedagogy facts; those
 * stay in the private {@link ExplainTeachingArtifact}.
 */
public record TeachingProjection(
        String principleSummary,
        WorkedExample workedExample,
        List<ApplyLearnerEvent> allowedEvents
) {

    public TeachingProjection {
        Objects.requireNonNull(principleSummary, "principleSummary must not be null");
        Objects.requireNonNull(workedExample, "workedExample must not be null");
        Objects.requireNonNull(allowedEvents, "allowedEvents must not be null");
        allowedEvents = List.copyOf(allowedEvents);
    }

    public record WorkedExample(String problem, List<Step> steps, String finalResult) {

        public WorkedExample {
            Objects.requireNonNull(problem, "problem must not be null");
            Objects.requireNonNull(steps, "steps must not be null");
            Objects.requireNonNull(finalResult, "finalResult must not be null");
            steps = List.copyOf(steps);
        }
    }

    public record Step(String expression, String ruleReference, String explanation) {

        public Step {
            Objects.requireNonNull(expression, "expression must not be null");
            Objects.requireNonNull(ruleReference, "ruleReference must not be null");
            Objects.requireNonNull(explanation, "explanation must not be null");
        }
    }
}
