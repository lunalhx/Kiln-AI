package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Objects;

/**
 * The learner-visible projection of one exposed hint level, carried by the
 * committed interaction. It contains only the exposed level's content —
 * reasoning steps and the proposed final answer appear only for the H5
 * reveal — so unexposed ladder levels never reach the learner.
 */
public record HintView(
        int level,
        String disclosureKind,
        String learnerContent,
        List<String> reasoningSteps,
        String proposedFinalAnswer
) {
    public HintView {
        Objects.requireNonNull(disclosureKind, "disclosureKind must not be null");
        Objects.requireNonNull(learnerContent, "learnerContent must not be null");
        reasoningSteps = reasoningSteps == null ? null : List.copyOf(reasoningSteps);
    }
}
