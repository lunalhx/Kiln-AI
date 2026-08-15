package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class InMemoryLearningFlowStore implements LearningFlowStore, ReviewTaskStore {

    private final Map<UUID, FlowRecord> flows = new HashMap<>();
    private final Map<UUID, List<ApplyFlowInteraction>> interactions = new HashMap<>();
    private final Map<UUID, List<ApplyCheckpoint>> checkpoints = new HashMap<>();
    private final Map<UUID, Set<String>> taskFingerprints = new HashMap<>();
    private final Map<UUID, Set<String>> solutionFingerprints = new HashMap<>();
    private final Map<UUID, AcceptedLearningEvidence> evidence = new HashMap<>();
    private final Map<UUID, ProcessedCommand> commands = new LinkedHashMap<>();
    private final Map<UUID, ReviewTask> reviews = new LinkedHashMap<>();
    private final ArtifactStore artifactStore;
    private final java.time.Clock clock;

    public InMemoryLearningFlowStore() {
        this(java.time.Clock.systemUTC());
    }

    public InMemoryLearningFlowStore(java.time.Clock clock) {
        this(clock, null);
    }

    /**
     * The composite form used by Review start tests: the flow store also owns
     * the atomic {@code ReviewTaskStore.bindStartedReview} transition, which
     * persists the generated Package and its open Attempt through the given
     * artifact store in the same commit as the review, flow, and command
     * state.
     */
    public InMemoryLearningFlowStore(java.time.Clock clock, ArtifactStore artifactStore) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.artifactStore = artifactStore;
    }

    @Override
    public synchronized void insertFlow(FlowRecord flow) {
        Objects.requireNonNull(flow, "flow must not be null");
        flows.put(flow.flowId(), flow);
    }

    @Override
    public synchronized Optional<FlowRecord> findFlow(UUID flowId) {
        return Optional.ofNullable(flows.get(flowId));
    }

    @Override
    public synchronized void commitBoundary(
            ApplyFlowInteraction interaction,
            ApplyCheckpoint checkpoint,
            ProcessedCommand command
    ) {
        Objects.requireNonNull(interaction, "interaction must not be null");
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        Objects.requireNonNull(command, "command must not be null");
        List<ApplyFlowInteraction> history = interactions.computeIfAbsent(
                interaction.flowId(), key -> new ArrayList<>());
        if (history.stream().anyMatch(item -> item.interactionVersion() == interaction.interactionVersion())) {
            return;
        }
        history.add(interaction);
        checkpoints.computeIfAbsent(checkpoint.flowId(), key -> new ArrayList<>()).add(checkpoint);
        commands.putIfAbsent(command.idempotencyKey(), command);
    }

    @Override
    public synchronized Optional<ApplyFlowInteraction> latestInteraction(UUID flowId) {
        List<ApplyFlowInteraction> history = interactions.get(flowId);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(history.get(history.size() - 1));
    }

    @Override
    public synchronized Optional<ApplyCheckpoint> latestCheckpoint(UUID flowId) {
        List<ApplyCheckpoint> history = checkpoints.get(flowId);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(history.get(history.size() - 1));
    }

    @Override
    public synchronized void recordTaskExposure(UUID flowId, TaskPackage taskPackage) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(taskPackage, "taskPackage must not be null");
        taskFingerprints.computeIfAbsent(flowId, key -> new LinkedHashSet<>())
                .add(taskPackage.privateAssessorProjection().taskFingerprint().value());
        solutionFingerprints.computeIfAbsent(flowId, key -> new LinkedHashSet<>())
                .add(taskPackage.privateAssessorProjection().solutionFingerprint().value());
    }

    @Override
    public synchronized List<String> exposedTaskFingerprints(UUID flowId) {
        return List.copyOf(taskFingerprints.getOrDefault(flowId, Set.of()));
    }

    @Override
    public synchronized List<String> exposedSolutionFingerprints(UUID flowId) {
        return List.copyOf(solutionFingerprints.getOrDefault(flowId, Set.of()));
    }

    @Override
    public synchronized void acceptEvidence(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        this.evidence.putIfAbsent(evidence.taskAttemptId(), evidence);
    }

    @Override
    public synchronized ReviewTask acceptEvidenceAndScheduleFirstReview(AcceptedLearningEvidence evidence, Instant dueAt) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(dueAt, "dueAt must not be null");
        for (Map.Entry<UUID, ReviewTask> entry : reviews.entrySet()) {
            ReviewTask review = entry.getValue();
            if (review.learnerId().equals(evidence.learnerId())
                    && review.conceptId().equals(evidence.conceptId())
                    && review.isUnfinished()) {
                entry.setValue(cancelled(review));
            }
        }
        this.evidence.putIfAbsent(evidence.taskAttemptId(), evidence);
        ReviewTask review = new ReviewTask(
                UUID.randomUUID(), evidence.learnerId(), evidence.conceptId(), evidence.flowId(),
                1, ReviewTaskStatus.SCHEDULED, dueAt, clock.instant(), null, null, null);
        reviews.put(review.reviewId(), review);
        return review;
    }

    @Override
    public synchronized Optional<ReviewTask> findReview(UUID reviewId) {
        return Optional.ofNullable(reviews.get(reviewId));
    }

    @Override
    public synchronized Optional<ReviewTask> findStartedReview(UUID learnerId, UUID conceptId) {
        return reviews.values().stream()
                .filter(review -> review.learnerId().equals(learnerId))
                .filter(review -> review.conceptId().equals(conceptId))
                .filter(review -> review.status() == ReviewTaskStatus.STARTED)
                .findFirst();
    }

    @Override
    public synchronized Optional<ReviewTaskStore.ReviewAdvance> acceptEvidenceAndAdvanceReview(
            AcceptedLearningEvidence evidence,
            UUID completedReviewId,
            Instant nextDueAt
    ) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(completedReviewId, "completedReviewId must not be null");
        if (this.evidence.containsKey(evidence.taskAttemptId())) {
            return Optional.empty();
        }
        ReviewTask current = reviews.get(completedReviewId);
        if (current == null || current.status() != ReviewTaskStatus.STARTED) {
            return Optional.empty();
        }
        this.evidence.putIfAbsent(evidence.taskAttemptId(), evidence);
        ReviewTask completed = new ReviewTask(
                current.reviewId(), current.learnerId(), current.conceptId(), current.flowId(),
                current.reviewNumber(), ReviewTaskStatus.COMPLETED, current.dueAt(), current.createdAt(),
                current.startedAt(), evidence.acceptedAt(), null);
        reviews.put(completed.reviewId(), completed);
        ReviewTask successor = null;
        if (nextDueAt != null) {
            successor = new ReviewTask(
                    UUID.randomUUID(), current.learnerId(), current.conceptId(), current.flowId(),
                    current.reviewNumber() + 1, ReviewTaskStatus.SCHEDULED, nextDueAt,
                    clock.instant(), null, null, null);
            reviews.put(successor.reviewId(), successor);
        }
        return Optional.of(new ReviewTaskStore.ReviewAdvance(evidence, completed, successor));
    }

    @Override
    public synchronized Optional<ApplyFlowInteraction> bindStartedReview(ReviewTaskStore.ReviewStartBind bind) {
        Objects.requireNonNull(bind, "bind must not be null");
        if (artifactStore == null) {
            throw new IllegalStateException(
                    "bindStartedReview requires the composite InMemoryLearningFlowStore(clock, artifactStore) form");
        }
        ReviewTask review = reviews.get(bind.reviewId());
        if (review == null || review.status() != ReviewTaskStatus.DUE) {
            return Optional.empty();
        }
        ReviewTask claimed = new ReviewTask(
                review.reviewId(), review.learnerId(), review.conceptId(), review.flowId(),
                review.reviewNumber(), ReviewTaskStatus.STARTED, review.dueAt(), review.createdAt(),
                bind.startedAt(), null, null);
        reviews.put(bind.reviewId(), claimed);
        TaskAttempt attempt = artifactStore.openAttempt(bind.taskPackage());
        recordTaskExposure(bind.flowId(), bind.taskPackage());
        ApplyFlowInteraction interaction = new ApplyFlowInteraction(
                bind.flowId(), bind.interactionVersion(), FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.DELAYED_REVIEW, attempt.attemptId(), AttemptPurpose.REVIEW,
                bind.taskPackage().learnerProjection(), null);
        commitBoundary(interaction,
                new ApplyCheckpoint(UUID.randomUUID(), bind.flowId(), bind.interactionVersion(), clock.instant()),
                new ProcessedCommand(bind.idempotencyKey(), bind.requestHash(), bind.flowId(),
                        interaction, clock.instant()));
        return Optional.of(interaction);
    }

    @Override
    public synchronized int markDueReviewsDue(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        int transitions = 0;
        for (Map.Entry<UUID, ReviewTask> entry : reviews.entrySet()) {
            ReviewTask review = entry.getValue();
            if (review.status() == ReviewTaskStatus.SCHEDULED && !review.dueAt().isAfter(now)) {
                entry.setValue(new ReviewTask(
                        review.reviewId(), review.learnerId(), review.conceptId(), review.flowId(),
                        review.reviewNumber(), ReviewTaskStatus.DUE, review.dueAt(), review.createdAt(),
                        null, null, null));
                transitions++;
            }
        }
        return transitions;
    }

    @Override
    public synchronized List<ReviewTask> unfinishedReviewsFor(UUID learnerId) {
        return unfinishedReviewsFor(learnerId, null);
    }

    private List<ReviewTask> unfinishedReviewsFor(UUID learnerId, UUID conceptId) {
        return reviews.values().stream()
                .filter(review -> review.learnerId().equals(learnerId))
                .filter(review -> conceptId == null || review.conceptId().equals(conceptId))
                .filter(ReviewTask::isUnfinished)
                .sorted(Comparator.comparing(ReviewTask::dueAt))
                .toList();
    }

    private ReviewTask cancelled(ReviewTask review) {
        return new ReviewTask(
                review.reviewId(), review.learnerId(), review.conceptId(), review.flowId(),
                review.reviewNumber(), ReviewTaskStatus.CANCELLED, review.dueAt(), review.createdAt(),
                review.startedAt(), review.completedAt(), clock.instant());
    }

    @Override
    public synchronized boolean evidenceExists(UUID attemptId) {
        return evidence.containsKey(attemptId);
    }

    @Override
    public synchronized List<AcceptedLearningEvidence> allEvidence() {
        return List.copyOf(evidence.values());
    }

    @Override
    public synchronized Optional<ProcessedCommand> findCommand(UUID idempotencyKey) {
        return Optional.ofNullable(commands.get(idempotencyKey));
    }
}
