package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExposureLedger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class InMemoryExposureLedger implements ExposureLedger {

    private final Set<String> taskFingerprints = new LinkedHashSet<>();
    private final Set<String> solutionFingerprints = new LinkedHashSet<>();

    @Override
    public synchronized void recordTaskExposure(TaskPackage taskPackage) {
        Objects.requireNonNull(taskPackage, "taskPackage must not be null");
        taskFingerprints.add(taskPackage.privateAssessorProjection().taskFingerprint().value());
        solutionFingerprints.add(taskPackage.privateAssessorProjection().solutionFingerprint().value());
    }

    @Override
    public synchronized List<String> exposedTaskFingerprints() {
        return List.copyOf(taskFingerprints);
    }

    @Override
    public synchronized List<String> exposedSolutionFingerprints() {
        return List.copyOf(solutionFingerprints);
    }
}
