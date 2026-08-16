package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Pedagogy Plan Gate: a plan is valid only when it selects exactly one
 * action from the Workflow Guard's closed legal set and carries non-blank
 * feedback, intent, and reason. A plan naming any other action — however
 * well-formed — is rejected, so invalid model text can never influence
 * routing.
 */
class PedagogyPlanGatePolicyTest {

    @Test
    void aPlanWithinTheLegalSetPasses() {
        PedagogyPlan plan = plan(TeachingAction.EXPLAIN);
        GateResult<PedagogyPlan> result = new PedagogyPlanGatePolicy(
                Set.of(TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE))
                .evaluate(plan, GateContext.empty());
        assertEquals(GateOutcome.PASSED, result.outcome());
    }

    @Test
    void aPlanOutsideTheLegalSetIsRejectedEvenWhenWellFormed() {
        PedagogyPlan plan = plan(TeachingAction.INDEPENDENT_TEST);
        GateResult<PedagogyPlan> result = new PedagogyPlanGatePolicy(
                Set.of(TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE))
                .evaluate(plan, GateContext.empty());
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(result.violations().stream()
                        .anyMatch(violation -> violation.code().equals("pedagogy.action")),
                "the illegal action must be reported as the violation");
    }

    @Test
    void blankFieldsAreRejected() {
        PedagogyPlan blankFeedback = new PedagogyPlan(" ", TeachingAction.EXPLAIN, "i", List.of(), List.of(), "r");
        assertEquals(GateOutcome.REJECTED,
                new PedagogyPlanGatePolicy(Set.of(TeachingAction.EXPLAIN)).evaluate(
                        blankFeedback, GateContext.empty()).outcome());
        PedagogyPlan blankReason = new PedagogyPlan("f", TeachingAction.EXPLAIN, "i", List.of(), List.of(), " ");
        assertEquals(GateOutcome.REJECTED,
                new PedagogyPlanGatePolicy(Set.of(TeachingAction.EXPLAIN)).evaluate(
                        blankReason, GateContext.empty()).outcome());
    }

    private static PedagogyPlan plan(TeachingAction action) {
        return new PedagogyPlan("反馈", action, "intent", List.of(), List.of(), "reason");
    }
}
