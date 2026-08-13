package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;

public record GuardSnapshot(
        FlowStatus status,
        LearningStage stage,
        boolean hasOpenAttempt,
        AttemptPurpose openAttemptPurpose,
        boolean explanationDelivered,
        LearnerInputKind pendingInput
) {
}
