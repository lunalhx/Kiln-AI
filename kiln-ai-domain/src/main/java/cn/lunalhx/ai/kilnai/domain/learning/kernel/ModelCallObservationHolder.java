package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import cn.lunalhx.ai.kilnai.domain.learning.model.ModelCallObservation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ModelCallObservationHolder {

    private final ConcurrentHashMap<UUID, List<ModelCallObservation>> records = new ConcurrentHashMap<>();

    public void add(ModelCallObservation observation) {
        records.computeIfAbsent(observation.flowId(), key -> new CopyOnWriteArrayList<>()).add(observation);
    }

    public List<ModelCallObservation> list(UUID flowId) {
        return List.copyOf(records.getOrDefault(flowId, List.of()));
    }

    public List<ModelCallObservation> drain(UUID flowId) {
        List<ModelCallObservation> current = records.remove(flowId);
        return current == null ? List.of() : List.copyOf(current);
    }

    public void clear(UUID flowId) {
        records.remove(flowId);
    }
}
