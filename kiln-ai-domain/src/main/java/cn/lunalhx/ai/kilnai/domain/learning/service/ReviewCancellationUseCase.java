package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * The independent, learner-confirmed Review cancellation resource. It owns no
 * Flow command discriminator; the Review store owns the atomic state change
 * and its separate idempotency ledger.
 */
public final class ReviewCancellationUseCase {

    private final ReviewTaskStore reviewStore;
    private final LearningFlowStore flowStore;
    private final Clock clock;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public ReviewCancellationUseCase(
            ReviewTaskStore reviewStore,
            LearningFlowStore flowStore,
            Clock clock
    ) {
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ReviewCancellationResult cancel(UUID reviewId, UUID idempotencyKey) {
        requireKey(idempotencyKey);
        Objects.requireNonNull(reviewId, "reviewId must not be null");
        String requestHash = ApplyHash.sha256HexDelimited("review-cancel", reviewId);
        ReviewTaskStore.ReviewCancellation cancellation = reviewStore.cancelReview(
                new ReviewTaskStore.ReviewCancellationBind(
                        reviewId, idempotencyKey, requestHash, clock.instant()));
        return new ReviewCancellationResult(
                cancellation.reviewTask(),
                progressProjector.projectFor(flowStore,
                        cancellation.reviewTask().learnerId(), cancellation.reviewTask().conceptId()),
                cancellation.flowInteraction());
    }

    private void requireKey(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key is required");
        }
    }
}
