package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;

/**
 * The result of one formal Apply Practice submission. A conclusive Practice
 * pass delivers a fresh verified Independent Test — the only outcome that
 * makes fresh Independent testing legal in the current remediation cycle — and
 * accepts exactly one assisted Practice PASS Evidence record. A conclusive
 * Practice fail accepts exactly one assisted Practice FAIL Evidence record and
 * delivers a fresh Practice task. An Inconclusive Practice judgment accepts no
 * Evidence and delivers a fresh verified Practice replacement. The
 * learner-visible surface is only the fresh task and a safe neutral message;
 * the closed attempt and evidence are private audit data for the Graph layer.
 */
public sealed interface PracticeSubmissionResult
        permits PracticeSubmissionResult.PracticePassed,
        PracticeSubmissionResult.PracticeFailed,
        PracticeSubmissionResult.PracticeInconclusive,
        PracticeSubmissionResult.PracticeUnavailable,
        PracticeSubmissionResult.NotSubmittable,
        PracticeSubmissionResult.Ignored {

    record PracticePassed(
            TaskAttempt closedPracticeAttempt,
            AcceptedLearningEvidence evidence,
            TaskAttempt independentAttempt,
            LearnerProjection independentLearnerProjection,
            String learnerMessage
    ) implements PracticeSubmissionResult {
    }

    record PracticeFailed(
            TaskAttempt closedPracticeAttempt,
            AcceptedLearningEvidence evidence,
            TaskAttempt practiceAttempt,
            LearnerProjection practiceLearnerProjection,
            String learnerMessage
    ) implements PracticeSubmissionResult {
    }

    /**
     * An Inconclusive Practice judgment: no Evidence is accepted and a fresh
     * verified Practice replacement is delivered, so evaluative uncertainty
     * never becomes a counted pass or fail.
     */
    record PracticeInconclusive(
            TaskAttempt closedPracticeAttempt,
            TaskAttempt practiceAttempt,
            LearnerProjection practiceLearnerProjection,
            String learnerMessage
    ) implements PracticeSubmissionResult {
    }

    record PracticeUnavailable(TaskUnavailableReason reason, String learnerMessage)
            implements PracticeSubmissionResult {
    }

    record NotSubmittable(SubmissionRejectionReason reason) implements PracticeSubmissionResult {
    }

    record Ignored(SubmissionIgnoreReason reason) implements PracticeSubmissionResult {
    }
}
