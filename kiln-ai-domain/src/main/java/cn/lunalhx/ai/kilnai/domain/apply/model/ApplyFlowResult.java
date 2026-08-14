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
        ApplyFlowResult.SubmissionIgnored {

    record Boundary(ApplyFlowInteraction interaction) implements ApplyFlowResult {
    }

    record SubmissionRejected(SubmissionRejectionReason reason) implements ApplyFlowResult {
    }

    record SubmissionIgnored(SubmissionIgnoreReason reason) implements ApplyFlowResult {
    }
}
