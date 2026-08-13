package cn.lunalhx.ai.kilnai.domain.gate;

import cn.lunalhx.ai.kilnai.domain.artifact.PedagogyPlan;

import java.util.List;

public final class PedagogyPlanGatePolicy implements GatePolicy<PedagogyPlan> {

    @Override
    public GateResult<PedagogyPlan> evaluate(PedagogyPlan candidate, GateContext context) {
        if (candidate.nextAction() == null || !context.legalActions().contains(candidate.nextAction().name())) {
            return GateResult.rejected(List.of(new GateViolation("illegal.action", "plan action is not legal")));
        }
        if (candidate.requiredCapabilityTags().stream().anyMatch(tag -> tag.startsWith("skill:"))) {
            return GateResult.rejected(List.of(new GateViolation("illegal.skill-id", "plan must not name Skill IDs")));
        }
        if (candidate.preferredStrategyTags().stream().anyMatch(tag -> tag.contains("@"))) {
            return GateResult.rejected(List.of(new GateViolation("illegal.skill-id", "plan must not name Skill versions")));
        }
        return GateResult.passed(candidate);
    }
}
