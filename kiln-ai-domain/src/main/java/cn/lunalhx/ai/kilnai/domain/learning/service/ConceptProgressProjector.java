package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.MasteryMilestone;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConceptProgressProjector {

    /**
     * The deterministic Evidence fold order: acceptance time first, then the
     * Evidence id, so ties never depend on storage or stream order.
     */
    public static final Comparator<AcceptedLearningEvidence> EVIDENCE_ORDER =
            Comparator.comparing(AcceptedLearningEvidence::acceptedAt)
                    .thenComparing(AcceptedLearningEvidence::id);

    public ConceptProgress project(UUID learnerId, UUID conceptId, List<AcceptedLearningEvidence> evidence) {
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        List<AcceptedLearningEvidence> ordered = evidence.stream().sorted(EVIDENCE_ORDER).toList();
        if (ordered.isEmpty()) {
            return new ConceptProgress(
                    learnerId, conceptId, MasteryMilestone.UNASSESSED, MasteryMilestone.UNASSESSED,
                    LearningStage.LEARNING_AND_PRACTICE, Instant.EPOCH
            );
        }
        MasteryMilestone current = MasteryMilestone.UNASSESSED;
        MasteryMilestone highest = MasteryMilestone.UNASSESSED;
        Instant updatedAt = Instant.EPOCH;
        for (AcceptedLearningEvidence item : ordered) {
            current = nextCurrent(current, item);
            highest = max(highest, current);
            updatedAt = item.acceptedAt();
        }
        LearningStage stage = current == MasteryMilestone.INDEPENDENT || current == MasteryMilestone.DURABLE
                ? LearningStage.DELAYED_REVIEW
                : LearningStage.LEARNING_AND_PRACTICE;
        return new ConceptProgress(learnerId, conceptId, current, highest, stage, updatedAt);
    }

    private MasteryMilestone nextCurrent(MasteryMilestone current, AcceptedLearningEvidence item) {
        if (item.isIndependentSuccess()) {
            return MasteryMilestone.INDEPENDENT;
        }
        if (item.attemptPurpose() == AttemptPurpose.INDEPENDENT_TEST
                && item.result() == LearningResult.FAIL
                && item.highestHintLevel() == 0
                && (current == MasteryMilestone.INDEPENDENT || current == MasteryMilestone.DURABLE)) {
            return MasteryMilestone.LEARNING;
        }
        if (item.isPracticeSuccess() && current == MasteryMilestone.UNASSESSED) {
            return MasteryMilestone.LEARNING;
        }
        if (item.isPracticeSuccess() && current.ordinal() < MasteryMilestone.LEARNING.ordinal()) {
            return MasteryMilestone.LEARNING;
        }
        if (item.isPracticeSuccess()) {
            return current == MasteryMilestone.UNASSESSED ? MasteryMilestone.LEARNING : current;
        }
        return current;
    }

    private MasteryMilestone max(MasteryMilestone left, MasteryMilestone right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
