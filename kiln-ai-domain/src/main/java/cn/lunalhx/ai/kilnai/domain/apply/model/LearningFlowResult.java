package cn.lunalhx.ai.kilnai.domain.apply.model;

/**
 * The closed result of one Learning Flow command. A successful state transition
 * returns the new durable {@link LearningFlowInteraction}; deterministic
 * submission rejections and ignore outcomes carry only the closed reason and
 * never advance the flow.
 */
public sealed interface LearningFlowResult
        permits LearningFlowResult.Boundary,
        LearningFlowResult.SubmissionRejected,
        LearningFlowResult.SubmissionIgnored,
        LearningFlowResult.HintIgnored,
        LearningFlowResult.ClarificationIgnored,
        LearningFlowResult.AssistanceIgnored {

    record Boundary(LearningFlowInteraction interaction) implements LearningFlowResult {
    }

    record SubmissionRejected(SubmissionRejectionReason reason) implements LearningFlowResult {
    }

    record SubmissionIgnored(SubmissionIgnoreReason reason) implements LearningFlowResult {
    }

    /**
     * A hint request that was never legal for the addressed attempt — an
     * unknown attempt, a wrong purpose (Diagnostic, Independent, Review, or
     * Teach-back), or an already-closed Practice attempt. Nothing is exposed
     * and the interaction never advances.
     */
    record HintIgnored(SubmissionIgnoreReason reason) implements LearningFlowResult {
    }

    /**
     * A clarification request that was never legal for the addressed attempt
     * — an unknown attempt, a wrong purpose (Diagnostic or Teach-back), or an
     * already-closed attempt. Nothing is classified, answered, or recorded,
     * and the interaction never advances.
     */
    record ClarificationIgnored(SubmissionIgnoreReason reason) implements LearningFlowResult {
    }

    /**
     * An assistance decision that was never legal for the addressed attempt —
     * an unknown attempt, an attempt that is no longer open, or an attempt
     * that is already Practice. Nothing is converted, recorded, or cancelled,
     * and the interaction never advances.
     */
    record AssistanceIgnored(SubmissionIgnoreReason reason) implements LearningFlowResult {
    }
}
