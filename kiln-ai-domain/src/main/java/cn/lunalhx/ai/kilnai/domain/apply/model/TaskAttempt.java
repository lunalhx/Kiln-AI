package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskAttempt(
        UUID attemptId,
        UUID taskPackageId,
        AttemptPurpose purpose,
        AttemptStatus status,
        Instant openedAt,
        Instant closedAt,
        TaskSubmission submission
) {

    public TaskAttempt {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(taskPackageId, "taskPackageId must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(openedAt, "openedAt must not be null");
    }

    public boolean isOpen() {
        return status == AttemptStatus.OPEN;
    }

    public boolean isClosed() {
        return status != AttemptStatus.OPEN;
    }
}
