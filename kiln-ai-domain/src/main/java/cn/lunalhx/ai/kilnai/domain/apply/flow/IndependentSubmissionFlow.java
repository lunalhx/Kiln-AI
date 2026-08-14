package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.IndependentSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * The Independent-Test submission flow: one formal submission atomically
 * closes the Independent Attempt and returns the closed typed assessment
 * outcome. It never accepts Evidence and never updates Concept Progress;
 * evidence acceptance belongs to a later node.
 */
public final class IndependentSubmissionFlow {

    private final TaskAttemptStore attemptStore;
    private final AssessmentRunner assessmentRunner;
    private final SubmissionCloser submissionCloser;

    public IndependentSubmissionFlow(
            TaskAttemptStore attemptStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            Clock clock
    ) {
        this.attemptStore = Objects.requireNonNull(attemptStore, "attemptStore must not be null");
        this.assessmentRunner = new AssessmentRunner(
                Objects.requireNonNull(assessmentPort, "assessmentPort must not be null"),
                Objects.requireNonNull(verificationPort, "verificationPort must not be null"));
        this.submissionCloser = new SubmissionCloser(
                attemptStore, Objects.requireNonNull(clock, "clock must not be null"));
    }

    public IndependentSubmissionResult submitIndependent(
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        SubmissionCloser.CloseResult closed = submissionCloser.close(
                attemptId, AttemptPurpose.INDEPENDENT_TEST, rawDerivative, confirmedCanonical, rationale);
        return switch (closed) {
            case SubmissionCloser.CloseResult.Ignored ignored ->
                    new IndependentSubmissionResult.Ignored(ignored.reason());
            case SubmissionCloser.CloseResult.NotSubmittable notSubmittable ->
                    new IndependentSubmissionResult.NotSubmittable(notSubmittable.reason());
            case SubmissionCloser.CloseResult.Closed closedAttempt -> assess(closedAttempt.attempt());
        };
    }

    private IndependentSubmissionResult assess(TaskAttempt closedAttempt) {
        return new IndependentSubmissionResult.Assessed(
                closedAttempt,
                assessmentRunner.run(closedAttempt, attemptStore.findPackage(closedAttempt.taskPackageId()).orElseThrow()));
    }
}
