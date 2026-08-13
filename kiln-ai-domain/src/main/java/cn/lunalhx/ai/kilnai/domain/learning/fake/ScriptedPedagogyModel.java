package cn.lunalhx.ai.kilnai.application.fake;

import cn.lunalhx.ai.kilnai.application.port.PedagogyModelPort;
import cn.lunalhx.ai.kilnai.domain.artifact.PedagogyPlan;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class ScriptedPedagogyModel implements PedagogyModelPort {

    private final ScriptedScenario scenario;
    private final AtomicInteger calls = new AtomicInteger();

    public ScriptedPedagogyModel(ScriptedScenario scenario) {
        this.scenario = scenario;
    }

    public int calls() {
        return calls.get();
    }

    @Override
    public PedagogyPlan propose(LearningBlackboard blackboard) {
        calls.incrementAndGet();
        TeachingAction action = scenario == ScriptedScenario.ILLEGAL_PEDAGOGY
                ? TeachingAction.RETRIEVE
                : TeachingAction.APPLY;
        return new PedagogyPlan(
                "Continue with a quantitative practice task.",
                action,
                "practice-percent-change",
                Set.of("quantitative"),
                Set.of("worked-example"),
                "continue-after-explain"
        );
    }
}
