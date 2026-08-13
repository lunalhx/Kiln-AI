package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import cn.lunalhx.ai.kilnai.domain.blackboard.BlackboardDelta;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingCommitBuffer {

    private final ConcurrentHashMap<UUID, CommitEffects> pending = new ConcurrentHashMap<>();

    public void put(UUID flowId, CommitEffects effects) {
        pending.put(flowId, effects);
    }

    public CommitEffects poll(UUID flowId) {
        return pending.remove(flowId);
    }

    public void discard(UUID flowId) {
        pending.remove(flowId);
    }
}
