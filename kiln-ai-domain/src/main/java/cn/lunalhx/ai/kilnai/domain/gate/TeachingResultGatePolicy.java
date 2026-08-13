package cn.lunalhx.ai.kilnai.domain.gate;

import cn.lunalhx.ai.kilnai.domain.artifact.TeachingResultEnvelope;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TeachingResultGatePolicy implements GatePolicy<TeachingResultEnvelope> {

    private static final Set<LearnerInputKind> EXPLAIN_EVENTS = Set.of(
            LearnerInputKind.CONTINUE_REQUESTED,
            LearnerInputKind.CLARIFICATION_ASKED,
            LearnerInputKind.FLOW_CONTROL_REQUESTED
    );
    private static final Set<LearnerInputKind> APPLY_EVENTS = Set.of(
            LearnerInputKind.ANSWER_SUBMITTED,
            LearnerInputKind.HINT_REQUESTED,
            LearnerInputKind.CLARIFICATION_ASKED,
            LearnerInputKind.FLOW_CONTROL_REQUESTED
    );

    @Override
    public GateResult<TeachingResultEnvelope> evaluate(TeachingResultEnvelope candidate, GateContext context) {
        List<GateViolation> violations = new ArrayList<>();
        if (candidate.learnerVisibleContent() == null || candidate.learnerVisibleContent().isBlank()) {
            violations.add(new GateViolation("visible.required", "learner-visible content is required"));
        }
        if (candidate.privateArtifacts() == null) {
            violations.add(new GateViolation("private.required", "private artifact map is required"));
        }
        if (candidate.action() == TeachingAction.APPLY && !candidate.privateArtifacts().containsKey("answerKey")) {
            violations.add(new GateViolation("apply.answer-key", "Apply must include a private answer key"));
        }
        if (candidate.action() == TeachingAction.APPLY && !candidate.privateArtifacts().containsKey("taskRubric")) {
            violations.add(new GateViolation("apply.task-rubric", "Apply must include a Task Rubric"));
        }
        Set<LearnerInputKind> allowed = candidate.action() == TeachingAction.APPLY ? APPLY_EVENTS : EXPLAIN_EVENTS;
        if (candidate.allowedEventKinds() == null || !allowed.containsAll(candidate.allowedEventKinds())) {
            violations.add(new GateViolation("interaction.illegal", "event kinds are not allowed by the profile"));
        }
        if (candidate.learnerVisibleContent() != null && candidate.privateArtifacts() != null) {
            String visible = candidate.learnerVisibleContent();
            Object answerKey = candidate.privateArtifacts().get("answerKey");
            if (answerKey != null && visible.contains(String.valueOf(answerKey))) {
                violations.add(new GateViolation("visibility.leak", "private answer leaked into visible content"));
            }
        }
        if (violations.isEmpty()) {
            return GateResult.passed(candidate);
        }
        boolean repairable = violations.stream().anyMatch(v -> v.code().startsWith("apply.") || v.code().equals("visible.required"));
        return repairable ? GateResult.repairable(violations) : GateResult.rejected(violations);
    }
}
