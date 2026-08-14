package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
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
                clock.instant()
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
}
