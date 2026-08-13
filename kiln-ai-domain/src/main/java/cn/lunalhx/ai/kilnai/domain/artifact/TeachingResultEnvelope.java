package cn.lunalhx.ai.kilnai.domain.artifact;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.List;
import java.util.Map;

public record TeachingResultEnvelope(
        TeachingAction action,
        String learnerVisibleContent,
        Map<String, Object> privateArtifacts,
        List<String> sourceTrace,
        List<LearnerInputKind> allowedEventKinds,
        String hiddenReasoning
) {
}
