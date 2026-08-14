package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The result of one formal Independent-Test submission. The assessment verdict
 * is a closed typed contract; it never accepts Evidence or updates Concept
 * Progress by itself.
 */
public sealed interface IndependentSubmissionResult
        permits IndependentSubmissionResult.Assessed,
        IndependentSubmissionResult.NotSubmittable,
        IndependentSubmissionResult.Ignored {

    record Assessed(TaskAttempt closedAttempt, AssessmentOutcome outcome)
            implements IndependentSubmissionResult {
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements IndependentSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements IndependentSubmissionResult {
    }
}
