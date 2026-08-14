package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;

import java.util.List;

/**
 * The ledger of every exposed task and solution fingerprint. Independent
 * generation excludes every fingerprint already present here.
 */
public interface ExposureLedger {

    void recordTaskExposure(TaskPackage taskPackage);

    List<String> exposedTaskFingerprints();

    List<String> exposedSolutionFingerprints();
}
