package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.ProcessedCommand;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor.PreparedDelivery;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The deterministic start flow of a Due Review Task. It claims the Due Review
 * with a concurrency-safe conditional transition, then generates, gates, and
 * verifies a Fresh Equivalent Task just in time with the frozen Review
 * Blueprint and the complete Exposure Ledger of the original Apply Flow as
 * novelty exclusions. A successful start durably binds the new Review Attempt,
 * the exposure, the Started state, the Flow's next Delayed Review interaction,
 * and the processed idempotent command; a Source Gap or exhausted generation
 * releases the Review back to Due without creating an Attempt or Exposure.
 * Replays and racing starts never create a second Package or Attempt.
 */
public final class ReviewStartFlow {

    private final ApplyProfileExecutor executor;
    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final ReviewTaskStore reviewStore;
    private final ApplyExecutionContext reviewContextTemplate;
    private final Clock clock;

    public ReviewStartFlow(
            ApplyProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            ReviewTaskStore reviewStore,
            ApplyExecutionContext reviewContextTemplate,
            Clock clock
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
        this.reviewContextTemplate = Objects.requireNonNull(reviewContextTemplate, "reviewContextTemplate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ReviewStartResult start(UUID reviewId, UUID idempotencyKey) {
        requireUuidKey(idempotencyKey);
        Objects.requireNonNull(reviewId, "reviewId must not be null");
        String hash = ApplyHash.sha256HexDelimited("review-start", reviewId);
        return replayOrRun(idempotencyKey, hash, () -> run(reviewId, idempotencyKey, hash));
    }

    private ReviewStartResult run(UUID reviewId, UUID idempotencyKey, String hash) {
        ReviewTask review = reviewStore.findReview(reviewId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND, "review task not found"));
        if (review.status() != ReviewTaskStatus.DUE) {
            throw new ApplicationException(ErrorCode.CONFLICT, "review task is not startable");
        }
        Instant startedAt = clock.instant();
        Optional<ReviewTask> claimed = reviewStore.claimReviewStarted(reviewId, startedAt);
        if (claimed.isEmpty()) {
            throw new ApplicationException(ErrorCode.CONFLICT, "review task was already started");
        }
        UUID flowId = review.flowId();
        ApplyExecutionContext reviewContext = reviewContextTemplate.withNoveltyExclusions(
                flowStore.exposedTaskFingerprints(flowId),
                flowStore.exposedSolutionFingerprints(flowId));
        PreparedDelivery prepared = executor.prepareTask(reviewContext);
        if (prepared instanceof PreparedDelivery.Unavailable unavailable) {
            reviewStore.releaseReviewToDue(reviewId, startedAt);
            return new ReviewStartResult.Unavailable(
                    unavailable.reason(), flowId, unavailable.learnerMessage());
        }
        TaskPackage taskPackage = ((PreparedDelivery.TaskReady) prepared).taskPackage();
        TaskAttempt attempt = artifactStore.openAttempt(taskPackage);
        flowStore.recordTaskExposure(flowId, taskPackage);
        int version = flowStore.latestInteraction(flowId).orElseThrow().interactionVersion() + 1;
        ApplyFlowInteraction interaction = new ApplyFlowInteraction(
                flowId, version, FlowStatus.AWAITING_LEARNER_INPUT, LearningStage.DELAYED_REVIEW,
                attempt.attemptId(), AttemptPurpose.REVIEW, taskPackage.learnerProjection(), null);
        commitBoundary(interaction, idempotencyKey, hash);
        return new ReviewStartResult.Boundary(interaction);
    }

    private void commitBoundary(ApplyFlowInteraction interaction, UUID idempotencyKey, String hash) {
        flowStore.commitBoundary(
                interaction,
                new ApplyCheckpoint(UUID.randomUUID(), interaction.flowId(),
                        interaction.interactionVersion(), clock.instant()),
                new ProcessedCommand(idempotencyKey, hash, interaction.flowId(), interaction, clock.instant()));
    }

    private ReviewStartResult replayOrRun(UUID key, String hash, java.util.function.Supplier<ReviewStartResult> action) {
        return flowStore.findCommand(key).map(existing -> {
            if (!existing.requestHash().equals(hash)) {
                throw new ApplicationException(ErrorCode.CONFLICT, "idempotency key reused with a different payload");
            }
            return (ReviewStartResult) new ReviewStartResult.Boundary(existing.response());
        }).orElseGet(action);
    }

    private void requireUuidKey(UUID key) {
        if (key == null) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key is required");
        }
    }
}
