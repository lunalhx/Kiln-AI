package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore.ReviewStartBind;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor.PreparedDelivery;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The deterministic start flow of a Review Task. It generates, gates, and
 * verifies a Fresh Equivalent Task just in time with the frozen Review
 * Blueprint and the complete Exposure Ledger of the original Apply Flow as
 * novelty exclusions — before any durable state changes. A ready task is then
 * bound atomically by the store: the claim, the Package, the open Review
 * Attempt, the exposure, the Started state, the Flow's next Delayed Review
 * interaction, and the processed idempotent command commit together or not at
 * all, so a crash or a losing race can never leave a stranded Review or a
 * second Package or Attempt. A Source Gap or exhausted generation leaves the
 * Review Due (or, for a resume, Started) with nothing written.
 *
 * <p>The same endpoint also resumes a Started Review whose submitted attempt
 * was inconclusive and whose replacement could not be prepared: the Review
 * holds no open Attempt, so the same atomic bind claims it and delivers the
 * replacement. A Started Review that holds an open Attempt is never
 * startable, so a replacement can never stack on an unanswered attempt.
 */
public final class ReviewStartFlow {

    private final ApplyProfileExecutor executor;
    private final LearningFlowStore flowStore;
    private final ReviewTaskStore reviewStore;
    private final ApplyExecutionContext reviewContextTemplate;
    private final Clock clock;

    public ReviewStartFlow(
            ApplyProfileExecutor executor,
            LearningFlowStore flowStore,
            ReviewTaskStore reviewStore,
            ApplyExecutionContext reviewContextTemplate,
            Clock clock
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
        this.reviewContextTemplate = Objects.requireNonNull(reviewContextTemplate, "reviewContextTemplate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ReviewStartResult start(UUID reviewId, UUID idempotencyKey) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(reviewId, "reviewId must not be null");
        String hash = ApplyHash.sha256HexDelimited("review-start", reviewId);
        return FlowCommandReplay.replayOrRun(flowStore, idempotencyKey, hash,
                interaction -> new ReviewStartResult.Boundary(interaction),
                () -> run(reviewId, idempotencyKey, hash));
    }

    private ReviewStartResult run(UUID reviewId, UUID idempotencyKey, String hash) {
        ReviewTask review = reviewStore.findReview(reviewId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND, "review task not found"));
        if (!review.isStartable()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "review task is not startable");
        }
        UUID flowId = review.flowId();
        ApplyExecutionContext reviewContext = reviewContextTemplate.withNoveltyExclusions(
                flowStore.exposedTaskFingerprints(flowId),
                flowStore.exposedSolutionFingerprints(flowId));
        PreparedDelivery prepared = executor.prepareTask(reviewContext);
        if (prepared instanceof PreparedDelivery.Unavailable unavailable) {
            return new ReviewStartResult.Unavailable(unavailable.reason(), flowId, unavailable.learnerMessage());
        }
        TaskPackage taskPackage = ((PreparedDelivery.TaskReady) prepared).taskPackage();
        int interactionVersion = latestInteractionVersion(flowId) + 1;
        Optional<ApplyFlowInteraction> bound = reviewStore.bindReviewAttempt(new ReviewStartBind(
                reviewId, clock.instant(), flowId, taskPackage, interactionVersion, idempotencyKey, hash));
        if (bound.isPresent()) {
            return new ReviewStartResult.Boundary(bound.get());
        }
        return flowStore.findCommand(idempotencyKey)
                .map(existing -> (ReviewStartResult) new ReviewStartResult.Boundary(existing.response()))
                .orElseThrow(() -> new ApplicationException(ErrorCode.CONFLICT, "review task was already started"));
    }

    private int latestInteractionVersion(UUID flowId) {
        return flowStore.latestInteraction(flowId)
                .map(ApplyFlowInteraction::interactionVersion)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
    }

    private void requireUuidKey(UUID key) {
        if (key == null) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key is required");
        }
    }
}
