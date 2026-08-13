package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.List;
import java.util.Objects;

/** Deterministic legal moves. A model cannot invent or bypass these candidates. */
public final class WorkflowGuard {

    public List<TeachingAction> legalCandidates(GuardSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (snapshot.status() == FlowStatus.TERMINAL || snapshot.status() == FlowStatus.FAILED) {
            return List.of();
        }
        if (shouldAssess(snapshot) || !isLegalInput(snapshot)) {
            return List.of();
        }
        if (!snapshot.explanationDelivered()) {
            return List.of(TeachingAction.EXPLAIN);
        }
        if (snapshot.pendingInput() == LearnerInputKind.CONTINUE_REQUESTED) {
            return List.of(TeachingAction.EXPLAIN, TeachingAction.APPLY);
        }
        return List.of();
    }

    public boolean isLegalInput(GuardSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (snapshot.status() == FlowStatus.TERMINAL || snapshot.status() == FlowStatus.FAILED) {
            return false;
        }
        if (snapshot.pendingInput() == null) {
            return snapshot.status() == FlowStatus.READY && !snapshot.explanationDelivered();
        }
        return switch (snapshot.pendingInput()) {
            case CONTINUE_REQUESTED -> snapshot.explanationDelivered() && !snapshot.hasOpenAttempt();
            case ANSWER_SUBMITTED -> snapshot.hasOpenAttempt();
            case HINT_REQUESTED, CLARIFICATION_ASKED, FLOW_CONTROL_REQUESTED, UNKNOWN_INPUT ->
                    snapshot.status() == FlowStatus.AWAITING_LEARNER_INPUT;
        };
    }

    public boolean shouldAssess(GuardSnapshot snapshot) {
        return snapshot.status() != FlowStatus.TERMINAL
                && snapshot.status() != FlowStatus.FAILED
                && snapshot.pendingInput() == LearnerInputKind.ANSWER_SUBMITTED
                && snapshot.hasOpenAttempt();
    }

    public TeachingAction fallbackAction() {
        return TeachingAction.EXPLAIN;
    }
}
