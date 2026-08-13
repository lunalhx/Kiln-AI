package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GraphRunBudgetHolder {

    private final ConcurrentHashMap<UUID, GraphRunBudget> open = new ConcurrentHashMap<>();

    public GraphRunBudget open(UUID flowId, GraphRunBudget budget) {
        open.put(flowId, budget);
        return budget;
    }

    public GraphRunBudget required(UUID flowId) {
        GraphRunBudget budget = open.get(flowId);
        if (budget == null) {
            throw new IllegalStateException("graph run budget is not open for " + flowId);
        }
        return budget;
    }

    public void close(UUID flowId) {
        open.remove(flowId);
    }
}
