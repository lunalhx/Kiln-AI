package cn.lunalhx.ai.kilnai.domain.pedagogy.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ConceptState;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEvidence;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

/** Deterministic MVP pedagogy policy. AI providers do not make this decision. */
public class LearningWorkflow {

    public TeachingAction nextAction(ConceptState state, LearningEvidence latestEvidence) {
        if (!latestEvidence.result().isSuccessful()) {
            return TeachingAction.HINT;
        }
        return switch (state) {
            case UNKNOWN -> TeachingAction.EXPLAIN;
            case UNDERSTOOD -> TeachingAction.APPLY;
            case ASSISTED -> TeachingAction.INDEPENDENT_TEST;
            case INDEPENDENT, DURABLE -> TeachingAction.SCHEDULE_REVIEW;
        };
    }
}
