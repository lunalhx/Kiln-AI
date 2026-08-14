package cn.lunalhx.ai.kilnai.domain.apply.model;

public sealed interface DiagnosticSubmissionResult
        permits DiagnosticSubmissionResult.Passed,
        DiagnosticSubmissionResult.Failed,
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

    record NotSubmittable(RejectionReason reason) implements DiagnosticSubmissionResult {
    }

    record Ignored(IgnoreReason reason) implements DiagnosticSubmissionResult {
    }

    record IndependentUnavailable(TaskUnavailableReason reason, String learnerMessage)
            implements DiagnosticSubmissionResult {
    }

    enum RejectionReason {
        UNPARSEABLE_RAW,
        CONFIRMATION_MISMATCH
    }

    enum IgnoreReason {
        ATTEMPT_NOT_FOUND,
        ALREADY_SUBMITTED,
        NOT_A_DIAGNOSTIC_ATTEMPT
    }
}
