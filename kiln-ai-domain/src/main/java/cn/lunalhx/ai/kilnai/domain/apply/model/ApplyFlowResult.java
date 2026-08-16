package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed result of one Apply flow command. A successful state transition
 * returns the new durable {@link ApplyFlowInteraction}; deterministic
 * submission rejections and ignore outcomes carry only the closed reason and
 * never advance the flow.
 */
public sealed interface ApplyFlowResult
        permits ApplyFlowResult.Boundary,
        ApplyFlowResult.SubmissionRejected,
        ApplyFlowResult.SubmissionIgnored,
        ApplyFlowResult.HintIgnored,
        ApplyFlowResult.ClarificationIgnored,
        ApplyFlowResult.AssistanceIgnored {

    record Boundary(ApplyFlowInteraction interaction) implements ApplyFlowResult {
    }

    record SubmissionRejected(SubmissionRejectionReason reason) implements ApplyFlowResult {
    }

    record SubmissionIgnored(SubmissionIgnoreReason reason) implements ApplyFlowResult {
    }

    /**
     * A hint request that was never legal for the addressed attempt — an
     * unknown attempt, a wrong purpose (Diagnostic, Independent, Review, or
     * Teach-back), or an already-closed Practice attempt. Nothing is exposed
     * and the interaction never advances.
     */
    record HintIgnored(SubmissionIgnoreReason reason) implements ApplyFlowResult {
    }

    /**
     * A clarification request that was never legal for the addressed attempt
     * — an unknown attempt, a wrong purpose (Diagnostic or Teach-back), or an
     * already-closed attempt. Nothing is classified, answered, or recorded,
     * and the interaction never advances.
     */
    record ClarificationIgnored(SubmissionIgnoreReason reason) implements ApplyFlowResult {
    }

    /**
     * An assistance decision that was never legal for the addressed attempt —
     * an unknown attempt, an attempt that is no longer open, or an attempt
     * that is already Practice. Nothing is converted, recorded, or cancelled,
     * and the interaction never advances.
     */
    record AssistanceIgnored(SubmissionIgnoreReason reason) implements ApplyFlowResult {
    }
}
