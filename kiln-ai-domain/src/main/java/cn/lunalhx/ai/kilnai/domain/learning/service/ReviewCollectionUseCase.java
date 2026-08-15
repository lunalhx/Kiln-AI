package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The learner-safe Review collection: unfinished Review Tasks of one learner,
 * ordered by due time, each carrying only safe fields and a safe Concept
 * Progress projection. Learner UUIDs, private assessor facts, evidence
 * records, and audit identifiers never appear.
 */
public final class ReviewCollectionUseCase {

    private final ReviewTaskStore reviewStore;
    private final LearningFlowStore flowStore;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public ReviewCollectionUseCase(ReviewTaskStore reviewStore, LearningFlowStore flowStore) {
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
    }

    public List<ReviewTaskView> unfinishedFor(UUID learnerId) {
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        return reviewStore.unfinishedReviewsFor(learnerId).stream()
                .map(review -> new ReviewTaskView(
                        review.reviewId(),
                        review.conceptId(),
                        review.status(),
                        review.reviewNumber(),
                        review.dueAt(),
                        review.status() == ReviewTaskStatus.DUE,
                        projectProgress(learnerId, review.conceptId())))
                .toList();
    }

    private ConceptProgress projectProgress(UUID learnerId, UUID conceptId) {
        List<AcceptedLearningEvidence> evidence = flowStore.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId) && item.conceptId().equals(conceptId))
                .toList();
        return progressProjector.project(learnerId, conceptId, evidence);
    }

    public record ReviewTaskView(
            UUID reviewId,
            UUID conceptId,
            ReviewTaskStatus status,
            int reviewNumber,
            Instant dueAt,
            boolean startable,
            ConceptProgress progress
    ) {
    }
}
