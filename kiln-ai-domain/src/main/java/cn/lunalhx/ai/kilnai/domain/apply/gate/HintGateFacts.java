package cn.lunalhx.ai.kilnai.domain.apply.gate;

import java.util.List;
import java.util.Objects;

/**
 * The deterministic facts the Hint Ladder Gate validates against: the current
 * Practice task's private canonical expected answer and the approved source
 * trace of its verified Task Package. The Gate never receives assessment
 * reasoning, other raw answers, the full Blackboard, or the learner's draft.
 */
public record HintGateFacts(
        String expectedExpression,
        List<String> variables,
        List<SourceRef> approvedSourceTrace
) {
    public HintGateFacts {
        Objects.requireNonNull(expectedExpression, "expectedExpression must not be null");
        Objects.requireNonNull(variables, "variables must not be null");
        Objects.requireNonNull(approvedSourceTrace, "approvedSourceTrace must not be null");
        variables = List.copyOf(variables);
        approvedSourceTrace = List.copyOf(approvedSourceTrace);
    }

    public record SourceRef(String sourceDocumentId, String passageId) {
        public SourceRef {
            Objects.requireNonNull(sourceDocumentId, "sourceDocumentId must not be null");
            Objects.requireNonNull(passageId, "passageId must not be null");
        }
    }
}
