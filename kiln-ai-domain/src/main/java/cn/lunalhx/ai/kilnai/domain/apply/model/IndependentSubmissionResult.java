package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;

/**
 * The result of one formal Independent-Test submission. Only a passing
 * final-expression channel with a non-contradictory rationale accepts exactly
 * one Independent Evidence record, atomically schedules the unique Review 1,
 * and projects the updated Concept Progress. The learner-visible surface is
 * only the safe continue-or-end message; the closed attempt and evidence are
 * private audit data for the Graph layer, and answers, assessment
 * conclusions, sources, Fingerprints, and audit identifiers never appear in
 * learner-visible content.
 */
public sealed interface IndependentSubmissionResult
        permits IndependentSubmissionResult.EvidenceAccepted,
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

    record NoEvidence(TaskAttempt closedAttempt, String learnerMessage) implements IndependentSubmissionResult {
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements IndependentSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements IndependentSubmissionResult {
    }
}
