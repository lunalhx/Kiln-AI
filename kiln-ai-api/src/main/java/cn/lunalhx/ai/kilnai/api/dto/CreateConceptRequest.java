package cn.lunalhx.ai.kilnai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConceptRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String summary,
        @Size(max = 2_000) String sourceReference
) {
}
