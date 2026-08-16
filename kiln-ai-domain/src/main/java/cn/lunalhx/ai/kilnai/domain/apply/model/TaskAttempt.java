package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record TaskAttempt(
        UUID attemptId,
        UUID taskPackageId,
        AttemptPurpose purpose,
        AttemptStatus status,
        Instant openedAt,
        Instant closedAt,
        TaskSubmission submission,
        List<AssistanceTraceEntry> assistanceTrace
) {

    public TaskAttempt {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(taskPackageId, "taskPackageId must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(openedAt, "openedAt must not be null");
        assistanceTrace = assistanceTrace == null ? List.of() : List.copyOf(assistanceTrace);
    }

    @JsonIgnore
    public boolean isOpen() {
        return status == AttemptStatus.OPEN;
    }

    @JsonIgnore
    public boolean isClosed() {
        return status != AttemptStatus.OPEN;
    }

    public static TaskAttempt open(TaskPackage taskPackage, Instant now) {
        Objects.requireNonNull(taskPackage, "taskPackage must not be null");
        Objects.requireNonNull(now, "now must not be null");
        return new TaskAttempt(
                UUID.randomUUID(),
                taskPackage.taskPackageId(),
                taskPackage.attemptPurpose(),
                AttemptStatus.OPEN,
                now,
                null,
                null,
                List.of()
        );
    }

    public AttemptCloseOutcome close(TaskSubmission submission, Instant now) {
        Objects.requireNonNull(submission, "submission must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (!isOpen()) {
            return new AttemptCloseOutcome(AttemptCloseOutcome.Result.ALREADY_CLOSED, this);
        }
        return new AttemptCloseOutcome(
                AttemptCloseOutcome.Result.CLOSED,
                new TaskAttempt(attemptId, taskPackageId, purpose, AttemptStatus.SUBMITTED, openedAt, now, submission,
                        assistanceTrace));
    }

    /**
     * Closes an open attempt as {@link AttemptStatus#SOLUTION_REVEALED} after
     * an H5 hint exposure, keeping its assistance trace. No submission,
     * Assessment, or Evidence ever results from a revealed attempt.
     */
    public AttemptCloseOutcome closeAsSolutionRevealed(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (!isOpen()) {
            return new AttemptCloseOutcome(AttemptCloseOutcome.Result.ALREADY_CLOSED, this);
        }
        return new AttemptCloseOutcome(
                AttemptCloseOutcome.Result.CLOSED,
                new TaskAttempt(attemptId, taskPackageId, purpose, AttemptStatus.SOLUTION_REVEALED, openedAt, now,
                        null, assistanceTrace));
    }

    /**
     * The append-only copy of this attempt with one more exposed assistance
     * entry. H1-H4 exposures keep the attempt open; the caller closes it
     * explicitly for H5.
     */
    public TaskAttempt appendAssistance(AssistanceTraceEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        List<AssistanceTraceEntry> extended = new java.util.ArrayList<>(assistanceTrace);
        extended.add(entry);
        return new TaskAttempt(attemptId, taskPackageId, purpose, status, openedAt, closedAt, submission, extended);
    }

    @JsonIgnore
    public int highestHintLevel() {
        return assistanceTrace.stream()
                .mapToInt(entry -> entry.level().level())
                .max()
                .orElse(0);
    }

    @JsonIgnore
    public List<String> assistanceTraceStrings() {
        return assistanceTrace.stream()
                .map(AssistanceTraceEntry::asEvidenceString)
                .toList();
    }
}
