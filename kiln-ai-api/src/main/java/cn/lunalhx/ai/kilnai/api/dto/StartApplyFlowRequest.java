package cn.lunalhx.ai.kilnai.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartApplyFlowRequest(
        @NotNull UUID learnerId
) {
}
