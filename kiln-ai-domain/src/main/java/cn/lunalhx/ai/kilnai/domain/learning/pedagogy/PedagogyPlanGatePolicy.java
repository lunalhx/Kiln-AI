package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The Pedagogy Plan Gate Policy: the candidate plan must be contract-closed
 * (already enforced by parsing), carry a concise non-blank feedback summary,
 * teaching intent, capability and strategy tag lists, and a private reason,
 * and — critically — select exactly one action from the Workflow Guard's
 * closed legal set for the current decision. A plan naming any other action
 * is invalid, so invalid model text can never influence routing.
 */
public final class PedagogyPlanGatePolicy implements GatePolicy<PedagogyPlan> {

    private final Set<TeachingAction> legalActions;

    public PedagogyPlanGatePolicy(Set<TeachingAction> legalActions) {
        Objects.requireNonNull(legalActions, "legalActions must not be null");
        this.legalActions = Set.copyOf(legalActions);
    }

    @Override
    public GateResult<PedagogyPlan> evaluate(PedagogyPlan candidate, GateContext gateContext) {
        List<GateViolation> violations = new ArrayList<>();
        if (candidate.feedbackSummary().isBlank()) {
            violations.add(new GateViolation("pedagogy.feedback-summary", "a concise feedback summary is required"));
        }
        if (!legalActions.contains(candidate.action())) {
            violations.add(new GateViolation("pedagogy.action",
                    "the plan must select exactly one legal action but selected " + candidate.action().jsonName()));
        }
        if (candidate.intent().isBlank()) {
            violations.add(new GateViolation("pedagogy.intent", "a teaching intent is required"));
        }
        if (candidate.reason().isBlank()) {
            violations.add(new GateViolation("pedagogy.reason", "a private reason code is required"));
        }
        if (violations.isEmpty()) {
            return GateResult.passed(candidate);
        }
        return GateResult.rejected(violations);
    }
}
