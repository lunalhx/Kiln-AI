package cn.lunalhx.ai.kilnai.domain.apply.model;

public sealed interface DiagnosticSubmissionResult
        permits DiagnosticSubmissionResult.Passed,
        DiagnosticSubmissionResult.Failed,
        DiagnosticSubmissionResult.Inconclusive,
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

    record Failed(TaskAttempt closedDiagnosticAttempt, String safeEndMessage) implements DiagnosticSubmissionResult {
    }

    /**
     * The Diagnostic cannot be judged reliably: the isolated Assessment and
     * Response Verification disagree or any result is non-equivalent. The
     * learner receives no failure feedback and no Evidence; a fresh
     * Independent task is prepared instead.
     */
    record Inconclusive(
            TaskAttempt closedDiagnosticAttempt,
            String neutralTransitionMessage,
            TaskAttempt independentAttempt,
            LearnerProjection independentLearnerProjection
    ) implements DiagnosticSubmissionResult {
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements DiagnosticSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements DiagnosticSubmissionResult {
    }

    record IndependentUnavailable(TaskUnavailableReason reason, String learnerMessage)
            implements DiagnosticSubmissionResult {
    }
}
