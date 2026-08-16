package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.util.Objects;

/**
 * The result of one formal Apply Practice submission: the closed Attempt, the
 * isolated AssessmentOutcome, the accepted-Evidence candidate (built for a
 * conclusive pass or fail, null for an Inconclusive judgment), and the
 * sanitized Feedback Facts. The follow-up Teaching Node is never selected
 * here: the Learning StateGraph derives the legal next moves through the
 * Workflow Guard and the Pedagogy Agent, then accepts the Evidence only after
 * the chosen follow-up node's generation and verification succeed, so a
 * failed generation leaves no Evidence and the same command can be retried.
 */
public sealed interface PracticeSubmissionResult
        permits PracticeSubmissionResult.PracticeAssessed,
        PracticeSubmissionResult.NotSubmittable,
        PracticeSubmissionResult.Ignored {

    /**
     * A closed and assessed Practice Attempt. A conclusive pass or fail
     * carries the pre-built assisted Evidence record (never lowering Current
     * Mastery); an Inconclusive judgment carries null Evidence and the
     * mandated fresh replacement task of the same kind.
     */
    record PracticeAssessed(
            TaskAttempt closedAttempt,
            AssessmentOutcome outcome,
            AcceptedLearningEvidence evidence,
            FeedbackFacts facts
    ) implements PracticeSubmissionResult {

        public PracticeAssessed {
            Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
            Objects.requireNonNull(outcome, "outcome must not be null");
            Objects.requireNonNull(facts, "facts must not be null");
        }
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements PracticeSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements PracticeSubmissionResult {
    }
}
