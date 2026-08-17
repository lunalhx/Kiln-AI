package cn.lunalhx.ai.kilnai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * One closed learning command of the unified Learning Flow API. The
 * {@code command} discriminator is one of {@code answer_submitted},
 * {@code hint_requested}, {@code clarification_asked},
 * {@code assistance_decided}, {@code continue_requested},
 * {@code retry_requested}, or {@code flow_control_requested}. Every command
 * carries the expected {@code interactionVersion}; commands targeting an
 * open Attempt also carry {@code attemptId}. {@code retry_requested} and an
 * Explain {@code clarification_asked} target the current Interaction without
 * an Attempt ID. {@code answer_submitted} reuses the existing raw answer,
 * learner-confirmed canonical representation, and optional rationale
 * contract; {@code hint_requested} may request the answer directly;
 * {@code clarification_asked} carries the free-form message;
 * {@code assistance_decided} carries the accept/refuse choice.
 */
public record LearningFlowCommandRequest(
        @NotBlank String command,
        @NotNull Integer interactionVersion,
        UUID attemptId,
        String rawAnswer,
        String confirmedCanonical,
        String rationale,
        Boolean answerRequested,
        String message,
        Boolean accept
) {
}
