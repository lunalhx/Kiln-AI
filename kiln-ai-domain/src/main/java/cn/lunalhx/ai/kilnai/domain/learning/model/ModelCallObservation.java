package cn.lunalhx.ai.kilnai.domain.learning.model;

import java.util.UUID;

public record ModelCallObservation(
        UUID flowId,
        ModelSlot slot,
        String protocol,
        String endpoint,
        String providerId,
        String modelId,
        Integer promptTokens,
        Integer completionTokens,
        long latencyMs
) {
    public String identity() {
        return providerId + "/" + modelId;
    }

    public String usage() {
        return "promptTokens=" + promptTokens + " completionTokens=" + completionTokens + " latencyMs=" + latencyMs;
    }
}
