package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAttemptStore {

    TaskAttempt openAttempt(TaskPackage taskPackage);

    Optional<TaskPackage> findPackage(UUID taskPackageId);

    Optional<TaskAttempt> findAttempt(UUID attemptId);

    List<TaskPackage> allPackages();

    /**
     * Atomically closes one open attempt with its single formal submission.
     * A replay, duplicate, or stale close never produces a second evaluation.
     */
    AttemptCloseOutcome closeAttempt(UUID attemptId, TaskSubmission submission);
}
