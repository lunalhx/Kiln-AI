package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticFinding;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.util.Objects;

/**
 * The result of one formal Diagnostic submission. Assessment produces a
 * per-Attempt fact; the Learning StateGraph records the Finding and applies
 * the Diagnostic Routing Decision. Independent delivery happens only after
 * that Decision authorizes it.
 */
public sealed interface DiagnosticSubmissionResult
        permits DiagnosticSubmissionResult.Passed,
        DiagnosticSubmissionResult.PassedAttempt,
        DiagnosticSubmissionResult.Failed,
        DiagnosticSubmissionResult.Unconfirmed,
        DiagnosticSubmissionResult.NotSubmittable,
        DiagnosticSubmissionResult.Ignored,
        DiagnosticSubmissionResult.IndependentUnavailable {

    record Passed(
            TaskAttempt closedDiagnosticAttempt,
            String neutralTransitionMessage,
            TaskAttempt independentAttempt,
            LearnerProjection independentLearnerProjection,
            DiagnosticFinding finding
    ) implements DiagnosticSubmissionResult {
    }

    record PassedAttempt(TaskAttempt closedDiagnosticAttempt, FeedbackFacts facts, DiagnosticFinding finding)
            implements DiagnosticSubmissionResult {

        public PassedAttempt {
            Objects.requireNonNull(closedDiagnosticAttempt, "closedDiagnosticAttempt must not be null");
            Objects.requireNonNull(facts, "facts must not be null");
        }
    }

    record Failed(TaskAttempt closedDiagnosticAttempt, FeedbackFacts facts, DiagnosticFinding finding)
            implements DiagnosticSubmissionResult {

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
    record Unconfirmed(TaskAttempt closedDiagnosticAttempt, FeedbackFacts facts, DiagnosticFinding finding)
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
