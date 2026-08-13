package cn.lunalhx.ai.kilnai.application.port;

import cn.lunalhx.ai.kilnai.domain.artifact.PedagogyPlan;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;

public interface PedagogyModelPort {

    PedagogyPlan propose(LearningBlackboard blackboard);
}
