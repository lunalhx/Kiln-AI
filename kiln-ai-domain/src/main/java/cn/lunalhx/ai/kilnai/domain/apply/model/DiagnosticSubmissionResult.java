package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.util.Objects;

/**
 * The result of one formal Diagnostic submission. Only a two-judgment passing
 * Diagnostic moves through the Neutral Transition to a fresh verified
 * Independent task; an Unconfirmed or conclusive failure closes the attempt without Evidence
 * and returns the sanitized Feedback Facts so the Learning StateGraph can
 * derive the legal remediation actions through the Workflow Guard and
 * Pedagogy Agent. The next learner-visible move after a failure is never
 * chosen here.
 */
public sealed interface DiagnosticSubmissionResult
        permits DiagnosticSubmissionResult.Passed,
        DiagnosticSubmissionResult.Failed,
        DiagnosticSubmissionResult.Unconfirmed,
        DiagnosticSubmissionResult.NotSubmittable,
        DiagnosticSubmissionResult.Ignored,
        DiagnosticSubmissionResult.IndependentUnavailable {

    record Passed(
            TaskAttempt closedDiagnosticAttempt,
            String neutralTransitionMessage,
            TaskAttempt independentAttempt,
            LearnerProjection independentLearnerProjection
    ) implements DiagnosticSubmissionResult {
    }

    record Failed(TaskAttempt closedDiagnosticAttempt, FeedbackFacts facts) implements DiagnosticSubmissionResult {

        public Failed {
            Objects.requireNonNull(closedDiagnosticAttempt, "closedDiagnosticAttempt must not be null");
            Objects.requireNonNull(facts, "facts must not be null");
        }
    }

    /**
     * The submitted Diagnostic is not confirmed as learner performance. The
     * StateGraph may use only neutral Feedback Facts to choose Explain or
     * Apply Practice remediation; no Independent task is prepared here.
     */
    record Unconfirmed(TaskAttempt closedDiagnosticAttempt, FeedbackFacts facts)
            implements DiagnosticSubmissionResult {

        public Unconfirmed {
            Objects.requireNonNull(closedDiagnosticAttempt, "closedDiagnosticAttempt must not be null");
            Objects.requireNonNull(facts, "facts must not be null");
        }
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements DiagnosticSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements DiagnosticSubmissionResult {
    }

    record IndependentUnavailable(TaskUnavailableReason reason, String learnerMessage)
            implements DiagnosticSubmissionResult {
    }
}
