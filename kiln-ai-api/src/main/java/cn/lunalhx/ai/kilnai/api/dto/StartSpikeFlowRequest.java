package cn.lunalhx.ai.kilnai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartSpikeFlowRequest(@NotNull UUID learnerId, @NotBlank String fixtureId) {
}
