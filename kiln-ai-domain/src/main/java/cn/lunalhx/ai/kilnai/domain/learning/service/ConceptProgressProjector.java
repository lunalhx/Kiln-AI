package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.MasteryMilestone;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConceptProgressProjector {

    /**
     * The number of consecutive qualifying Review successes after the latest
     * Independent success that project Current Milestone and Highest Milestone
     * Reached as Durable.
     */
    public static final int QUALIFYING_REVIEW_COUNT = 4;

    /**
     * The deterministic Evidence fold order: acceptance time first, then the
     * Evidence id, so ties never depend on storage or stream order.
     */
    public static final Comparator<AcceptedLearningEvidence> EVIDENCE_ORDER =
            Comparator.comparing(AcceptedLearningEvidence::acceptedAt)
                    .thenComparing(AcceptedLearningEvidence::id);

    /**
     * The shared projection helper used by the flows, collection, and HTTP
     * mapper: folds the stored Evidence of one learner and Concept.
     */
    public ConceptProgress projectFor(LearningFlowStore flowStore, UUID learnerId, UUID conceptId) {
        Objects.requireNonNull(flowStore, "flowStore must not be null");
        List<AcceptedLearningEvidence> conceptEvidence = flowStore.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId) && item.conceptId().equals(conceptId))
                .toList();
        return project(learnerId, conceptId, conceptEvidence);
    }

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
        int reviewSuccessCount = 0;
        for (AcceptedLearningEvidence item : ordered) {
            if (item.isIndependentSuccess()) {
                current = MasteryMilestone.INDEPENDENT;
                reviewSuccessCount = 0;
            } else if (item.isReviewSuccess() && hasIndependentFoundation(current)) {
                reviewSuccessCount++;
                if (reviewSuccessCount >= QUALIFYING_REVIEW_COUNT) {
                    current = MasteryMilestone.DURABLE;
                }
            } else {
                current = nextCurrent(current, item);
            }
            highest = max(highest, current);
            updatedAt = item.acceptedAt();
        }
        LearningStage stage = current == MasteryMilestone.INDEPENDENT || current == MasteryMilestone.DURABLE
                ? LearningStage.DELAYED_REVIEW
                : LearningStage.LEARNING_AND_PRACTICE;
        return new ConceptProgress(learnerId, conceptId, current, highest, stage, updatedAt);
    }

    /**
     * A Review success only advances the consecutive count while the latest
     * accepted evidence still supports at least an Independent foundation;
     * any qualifying failure or the absence of an Independent pass ends the
     * consecutive run, so the count can never reach Durable across a fall.
     */
    private static boolean hasIndependentFoundation(MasteryMilestone current) {
        return current == MasteryMilestone.INDEPENDENT || current == MasteryMilestone.DURABLE;
    }

    private MasteryMilestone nextCurrent(MasteryMilestone current, AcceptedLearningEvidence item) {
        if (item.isIndependentSuccess()) {
            return MasteryMilestone.INDEPENDENT;
        }
        if ((item.isIndependentFailure() || item.isReviewFailure())
                && (current == MasteryMilestone.INDEPENDENT || current == MasteryMilestone.DURABLE)) {
            return MasteryMilestone.LEARNING;
        }
        if (item.isPracticeSuccess() && current.ordinal() < MasteryMilestone.LEARNING.ordinal()) {
            return MasteryMilestone.LEARNING;
        }
        return current;
    }

    private MasteryMilestone max(MasteryMilestone left, MasteryMilestone right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
