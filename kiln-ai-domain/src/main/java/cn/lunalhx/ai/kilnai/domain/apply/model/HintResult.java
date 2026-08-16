package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed result of one hint request on an open Apply Practice Attempt. A
 * {@link Revealed} outcome carries the current attempt (open for H1-H4,
 * closed as Solution Revealed for H5) and the learner-visible view of the
 * exposed level only. An {@link Unavailable} outcome exposes nothing and
 * leaves the attempt open. An {@link Ignored} outcome means the request was
 * never legal for this attempt.
 */
public sealed interface HintResult permits HintResult.Revealed, HintResult.Unavailable, HintResult.Ignored {

    record Revealed(TaskAttempt attempt, HintView hint) implements HintResult {
        public Revealed {
            java.util.Objects.requireNonNull(attempt, "attempt must not be null");
            java.util.Objects.requireNonNull(hint, "hint must not be null");
        }
    }

    record Unavailable(HintUnavailableReason reason, String learnerMessage) implements HintResult {
        public Unavailable {
            java.util.Objects.requireNonNull(reason, "reason must not be null");
            java.util.Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }

    record Ignored(SubmissionIgnoreReason reason) implements HintResult {
        public Ignored {
            java.util.Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
