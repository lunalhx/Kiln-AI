package cn.lunalhx.ai.kilnai.application.kernel;

import cn.lunalhx.ai.kilnai.domain.blackboard.BlackboardDelta;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;

public record AuthorizedNodeResult(LearningBlackboard blackboard, BlackboardDelta delta, CommitEffects effects, String nextRoute) {
}
