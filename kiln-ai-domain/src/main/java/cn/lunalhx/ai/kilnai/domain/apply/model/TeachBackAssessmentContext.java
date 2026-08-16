package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.Objects;

/**
 * The bounded input of one isolated Teach-back Assessment: the learner-visible
 * task text, the already exposed anchor content the learner was asked to
 * explain, the learner's confirmed short-text response, and the attempt
 * purpose. The evaluator never sees an expected explanation — none exists.
 */
public record TeachBackAssessmentContext(
        String taskText,
        String anchorContent,
        String learnerResponse,
        AttemptPurpose purpose
) {

    public TeachBackAssessmentContext {
        Objects.requireNonNull(taskText, "taskText must not be null");
        Objects.requireNonNull(anchorContent, "anchorContent must not be null");
        Objects.requireNonNull(learnerResponse, "learnerResponse must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
    }
}
