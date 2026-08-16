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
        ApplyFlowResult.HintIgnored {

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
}
