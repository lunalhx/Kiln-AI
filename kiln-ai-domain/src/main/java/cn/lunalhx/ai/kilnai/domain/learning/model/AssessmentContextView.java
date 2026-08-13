package cn.lunalhx.ai.kilnai.domain.learning.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AssessmentContextView(
        UUID flowId,
        Map<String, Object> taskPackage,
        String answer,
        List<String> assistanceTrace
) {
    public AssessmentContextView {
        taskPackage = taskPackage == null ? Map.of() : Map.copyOf(taskPackage);
        assistanceTrace = assistanceTrace == null ? List.of() : List.copyOf(assistanceTrace);
    }
}
