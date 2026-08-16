package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

/**
 * The result of one formal Independent-Test submission. Only a passing
 * final-expression channel with a non-contradictory rationale accepts exactly
 * one Independent Evidence record, atomically schedules the unique Review 1,
 * and projects the updated Concept Progress. A conclusive no-hint failure
 * carries exactly one Independent-fail Evidence candidate with its sanitized
 * Feedback Facts so the Graph can begin remediation; a Blocked or Inconclusive
 * judgment creates no Evidence and no milestone change and requires a fresh
 * verified Independent replacement. The learner-visible surface is only the
 * safe continue-or-end message; the closed attempt and evidence are private
 * audit data for the Graph layer, and answers, assessment conclusions,
 * sources, Fingerprints, and audit identifiers never appear in
 * learner-visible content.
 */
public sealed interface IndependentSubmissionResult
        permits IndependentSubmissionResult.EvidenceAccepted,
        IndependentSubmissionResult.FailureEvidenceAccepted,
        IndependentSubmissionResult.ReplacementRequired,
        IndependentSubmissionResult.NoEvidence,
        IndependentSubmissionResult.NotSubmittable,
        IndependentSubmissionResult.Ignored {

    record EvidenceAccepted(
            TaskAttempt closedAttempt,
            AcceptedLearningEvidence evidence,
            ReviewTask scheduledReview,
            ConceptProgress progress,
            String learnerMessage
    ) implements IndependentSubmissionResult {
    }

    /**
     * A conclusive no-hint Independent failure: exactly one fail Evidence
     * candidate and the sanitized Feedback Facts of the demonstrated gap. The
     * Graph accepts the Evidence only after the chosen remediation node's
     * generation, gating, and verification succeed, so a failed generation
     * leaves no Evidence and the command can be retried.
     */
    record FailureEvidenceAccepted(
            TaskAttempt closedAttempt,
            AcceptedLearningEvidence evidence,
            FeedbackFacts facts
    ) implements IndependentSubmissionResult {
    }

    /**
     * A Blocked or Inconclusive Independent judgment: no Evidence, no
     * milestone change, and a fresh verified Independent replacement must be
     * delivered using all applicable novelty exclusions.
     */
    record ReplacementRequired(TaskAttempt closedAttempt, String learnerMessage) implements IndependentSubmissionResult {
    }

    record NoEvidence(TaskAttempt closedAttempt, String learnerMessage) implements IndependentSubmissionResult {
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements IndependentSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements IndependentSubmissionResult {
    }
}
