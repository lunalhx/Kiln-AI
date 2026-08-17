package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.util.Objects;

/**
 * The closed result of one Teach-back submission: the closed Attempt, the
 * isolated semantic Teach-back Assessment, the understanding-dimension
 * Evidence candidate (built for a conclusive pass or fail, null for an
 * Inconclusive judgment), and the sanitized Feedback Facts. The follow-up
 * Teaching Node is never selected here: the Learning StateGraph derives the
 * legal next moves through the Workflow Guard and the Pedagogy Agent, then
 * accepts the Evidence only after the chosen follow-up node's generation and
 * verification succeed. An {@link Unavailable} outcome means the closed
 * Attempt's own task package or anchor could not be resolved for the isolated
 * Assessment; the graph stops at a safe boundary. Ignored and NotSubmittable
 * outcomes never advance the flow.
 */
public sealed interface TeachBackSubmissionResult
        permits TeachBackSubmissionResult.TeachBackAssessed,
        TeachBackSubmissionResult.Unavailable,
        TeachBackSubmissionResult.NotSubmittable,
        TeachBackSubmissionResult.Ignored {

    /**
     * A closed and assessed Teach-back Attempt. A conclusive pass or fail
     * carries the pre-built understanding Evidence (never Independent
     * Evidence, never lowering Current Mastery); an Inconclusive judgment
     * carries null Evidence and the mandated fresh Teach-back replacement.
     */
    record TeachBackAssessed(
            TaskAttempt closedAttempt,
            TeachBackAssessment assessment,
            AcceptedLearningEvidence evidence,
            FeedbackFacts facts
    ) implements TeachBackSubmissionResult {

        public TeachBackAssessed {
            Objects.requireNonNull(closedAttempt, "closedAttempt must not be null");
            Objects.requireNonNull(facts, "facts must not be null");
            if (assessment == null && evidence != null) {
                throw new IllegalArgumentException("a null assessment cannot carry Evidence");
            }
        }
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements TeachBackSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements TeachBackSubmissionResult {
    }

    record Unavailable(TeachBackUnavailableReason reason, String learnerMessage) implements TeachBackSubmissionResult {

        public Unavailable {
            Objects.requireNonNull(reason, "reason must not be null");
            Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
        }
    }
}