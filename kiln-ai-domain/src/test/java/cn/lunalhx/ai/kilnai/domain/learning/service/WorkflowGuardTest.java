package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowGuardTest {

    private final WorkflowGuard guard = new WorkflowGuard();

    @Test
    void startWithNoEvidenceAllowsOnlyExplain() {
        GuardSnapshot snapshot = new GuardSnapshot(
                FlowStatus.READY,
                LearningStage.LEARNING_AND_PRACTICE,
                false,
                null,
                false,
                null
        );

        assertEquals(List.of(TeachingAction.EXPLAIN), guard.legalCandidates(snapshot));
    }

    @Test
    void continueAfterExplanationAllowsExplainAndApply() {
        GuardSnapshot snapshot = new GuardSnapshot(
                FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.LEARNING_AND_PRACTICE,
                false,
                null,
                true,
                LearnerInputKind.CONTINUE_REQUESTED
        );

        assertEquals(List.of(TeachingAction.EXPLAIN, TeachingAction.APPLY), guard.legalCandidates(snapshot));
    }

    @Test
    void answerOnOpenPracticeAttemptIsLegalAndDoesNotSelectTeaching() {
        GuardSnapshot snapshot = new GuardSnapshot(
                FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.LEARNING_AND_PRACTICE,
                true,
                AttemptPurpose.PRACTICE,
                true,
                LearnerInputKind.ANSWER_SUBMITTED
        );

        assertTrue(guard.isLegalInput(snapshot));
        assertEquals(List.of(), guard.legalCandidates(snapshot));
        assertTrue(guard.shouldAssess(snapshot));
    }

    @Test
    void answerWithoutOpenAttemptIsIllegal() {
        GuardSnapshot snapshot = new GuardSnapshot(
                FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.LEARNING_AND_PRACTICE,
                false,
                null,
                true,
                LearnerInputKind.ANSWER_SUBMITTED
        );

        assertFalse(guard.isLegalInput(snapshot));
        assertEquals(List.of(), guard.legalCandidates(snapshot));
        assertFalse(guard.shouldAssess(snapshot));
    }

    @Test
    void terminalFlowRejectsFurtherLearnerInput() {
        GuardSnapshot snapshot = new GuardSnapshot(
                FlowStatus.TERMINAL,
                LearningStage.LEARNING_AND_PRACTICE,
                true,
                AttemptPurpose.PRACTICE,
                true,
                LearnerInputKind.ANSWER_SUBMITTED
        );

        assertFalse(guard.isLegalInput(snapshot));
        assertEquals(List.of(), guard.legalCandidates(snapshot));
        assertFalse(guard.shouldAssess(snapshot));
    }
}
