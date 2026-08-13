package cn.lunalhx.ai.kilnai.domain.learning.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;

import java.util.List;
import java.util.UUID;

public record LearnerVisibleInteraction(
        UUID flowId,
        FlowStatus status,
        LearningStage stage,
        int interactionVersion,
        String visibleContent,
        List<LearnerInputKind> allowedEventKinds
) {
}
