package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.port.EvidenceStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryEvidenceStore implements EvidenceStorePort {

    private final Map<UUID, AcceptedLearningEvidence> evidence = new ConcurrentHashMap<>();

    @Override
    public void accept(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        this.evidence.putIfAbsent(evidence.taskAttemptId(), evidence);
    }

    @Override
    public List<AcceptedLearningEvidence> allEvidence() {
        return List.copyOf(evidence.values());
    }
}
