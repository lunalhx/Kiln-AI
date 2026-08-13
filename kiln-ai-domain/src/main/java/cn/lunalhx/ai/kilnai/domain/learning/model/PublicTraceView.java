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
        List<String> retries
) {
}
