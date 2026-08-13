package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GraphRunBudgetTest {

    @Test
    void ordinaryRunAllowsThreeNodeEntries() {
        GraphRunBudget budget = new GraphRunBudget(GraphRunKind.ORDINARY, 8);
        budget.enterNode();
        budget.enterNode();
        budget.enterNode();
        assertEquals(3, budget.nodeEntries());
        assertThrows(GraphRunBudgetExhausted.class, budget::enterNode);
    }

    @Test
    void highConsequenceRunAllowsFourNodeEntries() {
        GraphRunBudget budget = new GraphRunBudget(GraphRunKind.HIGH_CONSEQUENCE, 8);
        budget.enterNode();
        budget.enterNode();
        budget.enterNode();
        budget.enterNode();
        assertEquals(4, budget.nodeEntries());
        assertThrows(GraphRunBudgetExhausted.class, budget::enterNode);
    }

    @Test
    void toolExecutionsAreSeparateFromNodeEntries() {
        GraphRunBudget budget = new GraphRunBudget(GraphRunKind.ORDINARY, 1);
        budget.enterNode();
        budget.executeTool();
        assertEquals(1, budget.nodeEntries());
        assertEquals(1, budget.toolExecutions());
        assertThrows(GraphRunBudgetExhausted.class, budget::executeTool);
        budget.enterNode();
        assertEquals(2, budget.nodeEntries());
    }
}
