package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;

import java.util.Objects;

/**
 * The result of one formal Review submission. Only a conclusive passing
 * final-expression channel with a non-contradictory rationale accepts exactly
 * one Review PASS evidence record, completes the started Review Task at the
 * actual acceptance time, and schedules the successor after the fixed cadence
 * interval (or ends the cadence at Review 4). A conclusive no-hint failure —
 * a failed final-expression channel, or a clearly contradictory rationale
 * over a correct final answer (ADR-0061) — accepts exactly one Review FAIL
 * evidence record, completes the started Review Task, cancels any other
 * unfinished Review work, and stops the cadence with no successor. An
 * Inconclusive assessment closes the submitted Attempt but accepts no
 * Evidence and changes no milestone or cadence position: it binds a new
 * verified Fresh Equivalent Attempt to the same Started Review Task
 * ({@link ReplacementBound}) or, when the replacement cannot be prepared,
 * leaves the Review Started and resumable
 * ({@link ReplacementUnavailable}). The learner-visible surface is only the
 * safe message and the learner projection of the replacement; the closed
 * attempt, evidence, and Review Tasks are private audit data for the Graph
 * layer, and answers, assessment conclusions, sources, Fingerprints, and audit
 * identifiers never appear in learner-visible content.
 */
public sealed interface ReviewSubmissionResult
        permits ReviewSubmissionResult.EvidenceAccepted,
        ReviewSubmissionResult.FailureEvidenceAccepted,
        ReviewSubmissionResult.NoEvidence,
        ReviewSubmissionResult.ReplacementBound,
        ReviewSubmissionResult.ReplacementUnavailable,
        ReviewSubmissionResult.NotSubmittable,
        ReviewSubmissionResult.Ignored {

    record EvidenceAccepted(
            TaskAttempt closedAttempt,
            AcceptedLearningEvidence evidence,
            ReviewTask completedReview,
            ReviewTask successor,
            ConceptProgress progress,
            String learnerMessage
    ) implements ReviewSubmissionResult {
    }

    record FailureEvidenceAccepted(
            TaskAttempt closedAttempt,
            AcceptedLearningEvidence evidence,
            ReviewTask completedReview,
            ConceptProgress progress,
            String learnerMessage
    ) implements ReviewSubmissionResult {
    }

    record NoEvidence(TaskAttempt closedAttempt, String learnerMessage) implements ReviewSubmissionResult {
    }

    /**
     * An Inconclusive submission whose replacement was prepared and durably
     * bound to the same Started Review Task: the interaction is already
     * committed and carries the replacement Attempt and its learner
     * projection.
     */
    record ReplacementBound(
            TaskAttempt closedAttempt,
            LearningFlowInteraction interaction
    ) implements ReviewSubmissionResult {

        public ReplacementBound {
            Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
            Objects.requireNonNull(interaction, "interaction must not be null");
        }
    }

    /**
     * An Inconclusive submission whose replacement could not be prepared: the
     * interaction is already committed with the neutral continuation message,
     * the Review stays Started with no open Attempt, and the same start
     * endpoint can safely resume it.
     */
    record ReplacementUnavailable(
            TaskAttempt closedAttempt,
            LearningFlowInteraction interaction
    ) implements ReviewSubmissionResult {

        public ReplacementUnavailable {
            Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
            Objects.requireNonNull(interaction, "interaction must not be null");
        }
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements ReviewSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements ReviewSubmissionResult {
    }
}
