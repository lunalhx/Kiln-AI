package cn.lunalhx.ai.kilnai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitSpikeEventRequest(
        @NotNull Integer interactionVersion,
        @NotBlank String kind,
        String text
) {
}
