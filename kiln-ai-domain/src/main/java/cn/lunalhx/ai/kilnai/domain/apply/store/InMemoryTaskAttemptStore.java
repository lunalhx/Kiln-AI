package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryTaskAttemptStore implements TaskAttemptStore {

    private final Map<UUID, TaskPackage> packages = new HashMap<>();
    private final Map<UUID, TaskAttempt> attempts = new HashMap<>();
    private final Clock clock;

    public InMemoryTaskAttemptStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public synchronized TaskAttempt openAttempt(TaskPackage taskPackage) {
        Objects.requireNonNull(taskPackage, "taskPackage must not be null");
        if (packages.containsKey(taskPackage.taskPackageId())) {
            throw new IllegalStateException("task package already persisted: " + taskPackage.taskPackageId());
        }
        TaskAttempt attempt = new TaskAttempt(
                UUID.randomUUID(),
                taskPackage.taskPackageId(),
                taskPackage.attemptPurpose(),
                AttemptStatus.OPEN,
                clock.instant(),
                null,
                null
        );
        packages.put(taskPackage.taskPackageId(), taskPackage);
        attempts.put(attempt.attemptId(), attempt);
        return attempt;
    }

    @Override
    public synchronized Optional<TaskPackage> findPackage(UUID taskPackageId) {
        return Optional.ofNullable(packages.get(taskPackageId));
    }

    @Override
    public synchronized Optional<TaskAttempt> findAttempt(UUID attemptId) {
        return Optional.ofNullable(attempts.get(attemptId));
    }

    @Override
    public synchronized List<TaskPackage> allPackages() {
        return List.copyOf(new ArrayList<>(packages.values()));
    }

    @Override
    public synchronized AttemptCloseOutcome closeAttempt(UUID attemptId, TaskSubmission submission) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(submission, "submission must not be null");
        TaskAttempt current = attempts.get(attemptId);
        if (current == null) {
            return new AttemptCloseOutcome(AttemptCloseOutcome.Result.NOT_FOUND, null);
        }
        if (!current.isOpen()) {
            return new AttemptCloseOutcome(AttemptCloseOutcome.Result.ALREADY_CLOSED, current);
        }
        TaskAttempt closed = new TaskAttempt(
                current.attemptId(),
                current.taskPackageId(),
                current.purpose(),
                AttemptStatus.SUBMITTED,
                current.openedAt(),
                clock.instant(),
                submission
        );
        attempts.put(attemptId, closed);
        return new AttemptCloseOutcome(AttemptCloseOutcome.Result.CLOSED, closed);
    }
}
