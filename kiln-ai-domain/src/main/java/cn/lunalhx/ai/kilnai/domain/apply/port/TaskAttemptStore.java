package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAttemptStore {

    TaskAttempt openAttempt(TaskPackage taskPackage);

    Optional<TaskPackage> findPackage(UUID taskPackageId);

    Optional<TaskAttempt> findAttempt(UUID attemptId);

    List<TaskPackage> allPackages();
}
