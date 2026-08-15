package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;

/**
 * The result of one formal Review submission. Only a conclusive passing
 * final-expression channel with a non-contradictory rationale accepts exactly
 * one Review PASS evidence record, completes the started Review Task at the
 * actual acceptance time, and schedules the successor after the fixed cadence
 * interval (or ends the cadence at Review 4). A conclusive no-hint failure —
 * a failed final-expression channel, or a clearly contradictory rationale
 * over a correct final answer (ADR-0061) — accepts exactly one Review FAIL
 * evidence record, completes the started Review Task, cancels any other
 * unfinished Review work, and stops the cadence with no successor. The
 * learner-visible surface is only the safe end message; the closed attempt,
 * evidence, and Review Tasks are private audit data for the Graph layer, and
 * answers, assessment conclusions, sources, Fingerprints, and audit
 * identifiers never appear in learner-visible content.
 */
public sealed interface ReviewSubmissionResult
        permits ReviewSubmissionResult.EvidenceAccepted,
        ReviewSubmissionResult.FailureEvidenceAccepted,
        ReviewSubmissionResult.NoEvidence,
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

    record NotSubmittable(SubmissionRejectionReason reason) implements ReviewSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements ReviewSubmissionResult {
    }
}
