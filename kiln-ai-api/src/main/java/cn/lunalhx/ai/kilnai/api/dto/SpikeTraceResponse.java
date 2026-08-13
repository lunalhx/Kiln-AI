package cn.lunalhx.ai.kilnai.api.dto;

import java.util.List;
import java.util.UUID;

public record SpikeTraceResponse(
        UUID flowId,
        List<String> routes,
        List<String> selectedSkills,
        List<String> checkpoints,
        String budget,
        List<String> validations,
        List<String> retries
) {
}
