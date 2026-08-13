package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ToolSession;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.Map;
import java.util.Objects;

public final class BudgetedToolSession implements ToolSession {

    private final ToolSession delegate;
    private final GraphRunBudget budget;

    public BudgetedToolSession(ToolSession delegate, GraphRunBudget budget) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.budget = Objects.requireNonNull(budget, "budget must not be null");
    }

    @Override
    public Map<String, Object> call(String toolId, Map<String, Object> input) {
        try {
            budget.executeTool();
        } catch (GraphRunBudgetExhausted exhausted) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "graph run tool budget exhausted");
        }
        return delegate.call(toolId, input);
    }
}
