package cn.lunalhx.ai.kilnai.domain.learning.model.entity;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.MasteryMilestone;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Rebuildable projection of accepted evidence; not a mutable mastery claim. */
public record ConceptProgress(
        UUID learnerId,
        UUID conceptId,
        MasteryMilestone currentMilestone,
        MasteryMilestone highestMilestoneReached,
        LearningStage currentStage,
        Instant updatedAt
) {
    public ConceptProgress {
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(currentMilestone, "currentMilestone must not be null");
        Objects.requireNonNull(highestMilestoneReached, "highestMilestoneReached must not be null");
        Objects.requireNonNull(currentStage, "currentStage must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
