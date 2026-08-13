package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import cn.lunalhx.ai.kilnai.domain.artifact.TeachingResultEnvelope;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import cn.lunalhx.ai.kilnai.domain.skill.SkillStack;
import cn.lunalhx.ai.kilnai.domain.tool.ToolHandle;

import java.util.List;

public interface TeachingModelPort {

    TeachingResultEnvelope teach(
            TeachingAction action,
            LearningBlackboard blackboard,
            SkillStack stack,
            String compiledPrompt,
            List<ToolHandle> tools,
            ToolSession toolSession
    );
}
