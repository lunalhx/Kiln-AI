package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed result of one one-way Independent/Review-to-Practice attempt
 * conversion after an accepted assistance decision. A {@link Converted}
 * outcome carries the converted open attempt with its recorded assistance; an
 * {@link AlreadyPractice} outcome is the idempotent retry of a conversion
 * that already committed (nothing is appended again); an {@link Ignored}
 * outcome means the attempt is missing or no longer open.
 */
public sealed interface AttemptConversionOutcome
        permits AttemptConversionOutcome.Converted,
        AttemptConversionOutcome.AlreadyPractice,
        AttemptConversionOutcome.Ignored {

    record Converted(TaskAttempt attempt) implements AttemptConversionOutcome {

        public Converted {
            java.util.Objects.requireNonNull(attempt, "attempt must not be null");
        }
    }

    record AlreadyPractice(TaskAttempt attempt) implements AttemptConversionOutcome {

        public AlreadyPractice {
            java.util.Objects.requireNonNull(attempt, "attempt must not be null");
        }
    }

    record Ignored(SubmissionIgnoreReason reason) implements AttemptConversionOutcome {

        public Ignored {
            java.util.Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
