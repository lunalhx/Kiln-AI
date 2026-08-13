package cn.lunalhx.ai.kilnai.api.dto;

import java.util.List;
import java.util.UUID;

public record SpikeFlowResponse(
        UUID flowId,
        String status,
        String stage,
        int interactionVersion,
        String visibleContent,
        List<String> allowedEventKinds
) {
}
