package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.Objects;
import java.util.UUID;

/**
 * The learner-visible assistance-consent projection of an open Independent
 * Test or Review attempt after a substantive or uncertain clarification. It
 * states the consequence — the attempt will be converted one-way to Practice
 * before any help is exposed — and carries the unchanged attempt so the
 * learner can refuse or accept. No teaching content and no conversion ever
 * precede this projection (ADR-0014).
 */
public record AssistanceConsentView(
        String warning,
        UUID attemptId,
        AttemptPurpose attemptPurpose
) {

    public AssistanceConsentView {
        Objects.requireNonNull(warning, "warning must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(attemptPurpose, "attemptPurpose must not be null");
    }
}
