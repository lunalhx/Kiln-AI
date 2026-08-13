package cn.lunalhx.ai.kilnai.application.kernel;

import cn.lunalhx.ai.kilnai.application.graph.ResumeGraphRun;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingLearnerEventHolder {

    private final ConcurrentHashMap<UUID, ResumeGraphRun> pending = new ConcurrentHashMap<>();

    public void offer(ResumeGraphRun command) {
        pending.put(command.flowId(), command);
    }

    public ResumeGraphRun peek(UUID flowId) {
        return pending.get(flowId);
    }

    public ResumeGraphRun poll(UUID flowId) {
        return pending.remove(flowId);
    }
}
