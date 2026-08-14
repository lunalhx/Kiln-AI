package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.CanonicalExpressionResolver;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionRejectionReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates one formal submission against the attempt's own Task Package and
 * atomically closes the attempt. A replay, duplicate, or stale submission
 * never produces a second evaluation or result.
 */
final class SubmissionCloser {

    private final TaskAttemptStore attemptStore;
    private final Clock clock;

    SubmissionCloser(TaskAttemptStore attemptStore, Clock clock) {
        this.attemptStore = Objects.requireNonNull(attemptStore, "attemptStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    CloseResult close(
            UUID attemptId,
            AttemptPurpose expectedPurpose,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Optional<TaskAttempt> maybeAttempt = attemptStore.findAttempt(attemptId);
        if (maybeAttempt.isEmpty()) {
            return new CloseResult.Ignored(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        }
        TaskAttempt attempt = maybeAttempt.get();
        if (attempt.purpose() != expectedPurpose) {
            return new CloseResult.Ignored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
        }
        List<String> variables = packageVariables(attempt);
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve(rawDerivative, variables);
        if (resolution.isEmpty()) {
            return new CloseResult.NotSubmittable(SubmissionRejectionReason.UNPARSEABLE_RAW);
        }
        if (!sameCanonical(resolution.get().canonical(), confirmedCanonical)) {
            return new CloseResult.NotSubmittable(SubmissionRejectionReason.CONFIRMATION_MISMATCH);
        }
        TaskSubmission submission = new TaskSubmission(
                new MathematicalAnswer(rawDerivative, confirmedCanonical, resolution.get().family()),
                rationale,
                clock.instant());
        AttemptCloseOutcome closeOutcome = attemptStore.closeAttempt(attemptId, submission);
        if (closeOutcome.result() != AttemptCloseOutcome.Result.CLOSED) {
            return new CloseResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return new CloseResult.Closed(closeOutcome.attempt());
    }

    private List<String> packageVariables(TaskAttempt attempt) {
        TaskPackage taskPackage = attemptStore.findPackage(attempt.taskPackageId()).orElseThrow();
        return taskPackage.privateAssessorProjection().canonicalExpectedAnswer().variables();
    }

    private static boolean sameCanonical(String derivedCanonical, String confirmedCanonical) {
        if (confirmedCanonical == null) {
            return false;
        }
        return derivedCanonical.trim().replaceAll("\\s+", " ")
                .equals(confirmedCanonical.trim().replaceAll("\\s+", " "));
    }

    sealed interface CloseResult
            permits CloseResult.Closed, CloseResult.NotSubmittable, CloseResult.Ignored {

        record Closed(TaskAttempt attempt) implements CloseResult {
        }

        record NotSubmittable(SubmissionRejectionReason reason) implements CloseResult {
        }

        record Ignored(SubmissionIgnoreReason reason) implements CloseResult {
        }
    }
}
