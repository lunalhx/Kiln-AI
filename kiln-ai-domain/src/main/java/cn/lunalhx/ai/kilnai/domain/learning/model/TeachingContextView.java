package cn.lunalhx.ai.kilnai.domain.learning.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.UUID;

public record TeachingContextView(
        UUID flowId,
        TeachingAction action,
        LearningStage stage
) {
}
