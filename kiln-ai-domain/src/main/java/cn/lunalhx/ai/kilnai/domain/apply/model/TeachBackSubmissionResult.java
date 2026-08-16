package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;

/**
 * The closed result of one Teach-back submission. A conclusive pass or fail
 * carries the accepted understanding-dimension Evidence plus the fresh
 * verified Apply Practice follow-up task and its learner projection; an
 * Inconclusive judgment carries no Evidence and a fresh Teach-back
 * replacement task. An {@link Unavailable} outcome means the follow-up or
 * replacement could not be prepared: the closed Attempt is retained without
 * Evidence so the same command can be retried. Ignored and NotSubmittable
 * outcomes never advance the flow.
 */
public sealed interface TeachBackSubmissionResult
        permits TeachBackSubmissionResult.Passed,
        TeachBackSubmissionResult.Failed,
        TeachBackSubmissionResult.Inconclusive,
        TeachBackSubmissionResult.Unavailable,
        TeachBackSubmissionResult.Ignored,
        TeachBackSubmissionResult.NotSubmittable {

    record Passed(
            TaskAttempt closedAttempt,
            AcceptedLearningEvidence evidence,
            TaskAttempt followUpAttempt,
            LearnerProjection followUpLearnerProjection,
            String learnerMessage
    ) implements TeachBackSubmissionResult {

        public Passed {
            java.util.Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
            java.util.Objects.requireNonNull(evidence, "evidence must not be null");
            java.util.Objects.requireNonNull(followUpAttempt, "followUpAttempt must not be null");
            java.util.Objects.requireNonNull(followUpLearnerProjection, "followUpLearnerProjection must not be null");
            java.util.Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }

    record Failed(
            TaskAttempt closedAttempt,
            AcceptedLearningEvidence evidence,
            TaskAttempt followUpAttempt,
            LearnerProjection followUpLearnerProjection,
            String learnerMessage
    ) implements TeachBackSubmissionResult {

        public Failed {
            java.util.Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
            java.util.Objects.requireNonNull(evidence, "evidence must not be null");
            java.util.Objects.requireNonNull(followUpAttempt, "followUpAttempt must not be null");
            java.util.Objects.requireNonNull(followUpLearnerProjection, "followUpLearnerProjection must not be null");
            java.util.Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }

    record Inconclusive(
            TaskAttempt closedAttempt,
            TaskAttempt replacementAttempt,
            LearnerProjection replacementLearnerProjection,
            String learnerMessage
    ) implements TeachBackSubmissionResult {

        public Inconclusive {
            java.util.Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
            java.util.Objects.requireNonNull(replacementAttempt, "replacementAttempt must not be null");
            java.util.Objects.requireNonNull(replacementLearnerProjection, "replacementLearnerProjection must not be null");
            java.util.Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }

    record Unavailable(TeachBackUnavailableReason reason, String learnerMessage) implements TeachBackSubmissionResult {
        public Unavailable {
            java.util.Objects.requireNonNull(reason, "reason must not be null");
            java.util.Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }

    record Ignored(SubmissionIgnoreReason reason) implements TeachBackSubmissionResult {
        public Ignored {
            java.util.Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements TeachBackSubmissionResult {
        public NotSubmittable {
            java.util.Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
