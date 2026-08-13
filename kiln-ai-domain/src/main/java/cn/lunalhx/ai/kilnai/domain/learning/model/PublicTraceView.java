package cn.lunalhx.ai.kilnai.domain.learning.model;

import java.util.List;
import java.util.UUID;

public record PublicTraceView(
        UUID flowId,
        List<String> routes,
        List<String> selectedSkills,
        List<String> checkpoints,
        String budget,
        List<String> validations,
        List<String> retries,
        List<String> models,
        List<String> usage
) {
    public PublicTraceView {
        models = models == null ? List.of() : List.copyOf(models);
        usage = usage == null ? List.of() : List.copyOf(usage);
    }
}
