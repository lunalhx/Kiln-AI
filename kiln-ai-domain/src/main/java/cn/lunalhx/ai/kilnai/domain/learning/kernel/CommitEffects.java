package cn.lunalhx.ai.kilnai.application.kernel;

import cn.lunalhx.ai.kilnai.domain.blackboard.BlackboardDelta;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;

import java.util.Map;
import java.util.UUID;

public record CommitEffects(
        UUID flowId,
        String visibleContent,
        java.util.List<cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind> allowedEventKinds,
        cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus status,
        cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage stage,
        int interactionVersion,
        Map<String, Object> taskPackagePayload,
        UUID taskPackageId,
        UUID attemptId,
        AcceptedLearningEvidence evidence,
        ConceptProgress progress,
        Map<String, Object> privateTrace,
        Map<String, Object> publicTrace,
        BlackboardDelta delta
) {
}
