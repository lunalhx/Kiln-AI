package cn.lunalhx.ai.kilnai.domain.gate;

import cn.lunalhx.ai.kilnai.domain.artifact.PedagogyPlan;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedagogyPlanGatePolicyTest {

    private final PedagogyPlanGatePolicy policy = new PedagogyPlanGatePolicy();

    @Test
    void legalApplyPasses() {
        PedagogyPlan plan = new PedagogyPlan(
                "try a practice task", TeachingAction.APPLY, "practice",
                Set.of("quantitative"), Set.of("worked-example"), "continue"
        );
        GateContext context = new GateContext(Set.of("EXPLAIN", "APPLY"));

        assertEquals(GateOutcome.PASSED, policy.evaluate(plan, context).outcome());
    }

    @Test
    void illegalRetrieveIsRejected() {
        PedagogyPlan plan = new PedagogyPlan(
                "skip ahead", TeachingAction.RETRIEVE, "illegal",
                Set.of(), Set.of(), "bypass"
        );
        GateContext context = new GateContext(Set.of("EXPLAIN", "APPLY"));

        assertEquals(GateOutcome.REJECTED, policy.evaluate(plan, context).outcome());
    }

    @Test
    void skillIdInPlanIsRejected() {
        PedagogyPlan plan = new PedagogyPlan(
                "ok", TeachingAction.APPLY, "practice",
                Set.of("skill:apply.worked-example@1"), Set.of(), "named-skill"
        );
        GateContext context = new GateContext(Set.of("EXPLAIN", "APPLY"));

        assertEquals(GateOutcome.REJECTED, policy.evaluate(plan, context).outcome());
    }
}
