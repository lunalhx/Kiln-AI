package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import java.util.concurrent.atomic.AtomicInteger;

public final class GraphRunBudget {

    public static final int ORDINARY_NODE_LIMIT = 3;
    public static final int HIGH_CONSEQUENCE_NODE_LIMIT = 4;

    private final int nodeLimit;
    private final int toolLimit;
    private final AtomicInteger nodeEntries = new AtomicInteger();
    private final AtomicInteger toolExecutions = new AtomicInteger();

    public GraphRunBudget(GraphRunKind kind, int toolLimit) {
        this(nodeLimit(kind), toolLimit);
    }

    public GraphRunBudget(int nodeLimit, int toolLimit) {
        if (nodeLimit < 0) {
            throw new IllegalArgumentException("nodeLimit must not be negative");
        }
        if (toolLimit < 0) {
            throw new IllegalArgumentException("toolLimit must not be negative");
        }
        this.nodeLimit = nodeLimit;
        this.toolLimit = toolLimit;
    }

    public static int nodeLimit(GraphRunKind kind) {
        return kind == GraphRunKind.HIGH_CONSEQUENCE ? HIGH_CONSEQUENCE_NODE_LIMIT : ORDINARY_NODE_LIMIT;
    }

    public void enterNode() {
        if (nodeEntries.get() >= nodeLimit) {
            throw new GraphRunBudgetExhausted("graph run node budget exhausted");
        }
        nodeEntries.incrementAndGet();
    }

    public void executeTool() {
        if (toolExecutions.get() >= toolLimit) {
            throw new GraphRunBudgetExhausted("graph run tool budget exhausted");
        }
        toolExecutions.incrementAndGet();
    }

    public int nodeEntries() {
        return nodeEntries.get();
    }

    public int toolExecutions() {
        return toolExecutions.get();
    }

    public String trace() {
        return "nodes=" + nodeEntries() + " tools=" + toolExecutions();
    }
}
