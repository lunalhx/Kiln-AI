package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;

import java.util.Objects;

/**
 * The learner-safe projection of one explicit Review cancellation. The Flow
 * interaction is present only when Started work had to commit a new terminal
 * boundary; Scheduled, Due, Completed, and already Cancelled work keep their
 * existing committed Flow interaction unchanged.
 */
public record ReviewCancellationResult(
        ReviewTask reviewTask,
        ConceptProgress progress,
        LearningFlowInteraction flowInteraction
) {

    public ReviewCancellationResult {
        Objects.requireNonNull(reviewTask, "reviewTask must not be null");
        Objects.requireNonNull(progress, "progress must not be null");
    }
}
