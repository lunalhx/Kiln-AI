package cn.lunalhx.ai.kilnai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * One formal submission of the flow's current Task Attempt. The confirmed
 * canonical expression must match the canonical form the learner approved.
 */
public record SubmitApplyFlowRequest(
        @NotNull Integer interactionVersion,
        @NotNull UUID attemptId,
        @NotBlank String rawDerivative,
        @NotBlank String confirmedCanonical,
        String rationale
) {
}
