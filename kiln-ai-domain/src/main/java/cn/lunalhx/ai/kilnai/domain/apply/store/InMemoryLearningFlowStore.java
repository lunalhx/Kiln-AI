package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.model.LearningCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.InteractionKind;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PendingOperation;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlan;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticProgress;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ActiveWorkConflictException;
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
    private final Map<UUID, List<LearningFlowInteraction>> interactions = new HashMap<>();
    private final Map<UUID, List<LearningCheckpoint>> checkpoints = new HashMap<>();
    private final Map<UUID, Set<String>> taskFingerprints = new HashMap<>();
    private final Map<UUID, Set<String>> solutionFingerprints = new HashMap<>();
    private final Map<UUID, Set<UUID>> exposedTaskPackages = new HashMap<>();
    private final Map<UUID, Set<String>> exampleFingerprints = new HashMap<>();
    private final Map<UUID, Set<String>> hintLadderFingerprints = new HashMap<>();
    private final Map<UUID, Set<String>> revealedSolutionFingerprints = new HashMap<>();
    private final Map<UUID, List<TeachBackAnchor>> teachBackAnchors = new HashMap<>();
    private final Map<UUID, AcceptedLearningEvidence> evidence = new HashMap<>();
    private final Map<UUID, ProcessedCommand> commands = new LinkedHashMap<>();
    private final Map<UUID, ReviewTask> reviews = new LinkedHashMap<>();
    private final Map<UUID, CancellationRecord> reviewCancellations = new HashMap<>();
    private final Map<UUID, PendingOperation> pendingOperations = new HashMap<>();
    private final Map<UUID, DiagnosticPlan> diagnosticPlans = new HashMap<>();
    private final Map<UUID, Integer> diagnosticCompletedAttempts = new HashMap<>();
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
    public synchronized Optional<DiagnosticPlan> diagnosticPlan(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        return Optional.ofNullable(diagnosticPlans.get(flowId));
    }

    @Override
    public synchronized Optional<DiagnosticProgress> diagnosticProgress(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        DiagnosticPlan plan = diagnosticPlans.get(flowId);
        if (plan == null) {
            return Optional.empty();
        }
        return Optional.of(new DiagnosticProgress(
                diagnosticCompletedAttempts.getOrDefault(flowId, 0), plan.maximumAttempts()));
    }

    @Override
    public synchronized Optional<UUID> activeWorkFlowId(UUID learnerId, UUID conceptId) {
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        return flows.values().stream()
                .filter(flow -> flow.learnerId().equals(learnerId))
                .filter(flow -> flow.conceptId().equals(conceptId))
                .filter(flow -> flow.status() != FlowStatus.TERMINAL)
                .map(FlowRecord::flowId)
                .findFirst()
                .or(() -> reviews.values().stream()
                        .filter(review -> review.learnerId().equals(learnerId))
                        .filter(review -> review.conceptId().equals(conceptId))
                        .filter(ReviewTask::isUnfinished)
                        .map(ReviewTask::flowId)
                        .findFirst());
    }

    @Override
    public synchronized LearningFlowInteraction bindStart(StartBind bind) {
        Objects.requireNonNull(bind, "bind must not be null");
        if (artifactStore == null) {
            throw new IllegalStateException(
                    "bindStart requires the composite InMemoryLearningFlowStore(clock, artifactStore) form");
        }
        UUID activeWork = activeWorkFlowId(bind.learnerId(), bind.conceptId()).orElse(null);
        if (activeWork != null) {
            throw new ActiveWorkConflictException(activeWork);
        }
        TaskAttempt attempt = artifactStore.openAttempt(bind.taskPackage());
        artifactStore.saveSource(bind.source());
        artifactStore.recordTaskVerification(bind.taskPackage().taskPackageId(), bind.verificationVerdict());
        flows.put(bind.flowId(), new FlowRecord(
                bind.flowId(), bind.learnerId(), bind.conceptId(),
                FlowStatus.AWAITING_LEARNER_INPUT, LearningStage.DIAGNOSTIC,
                bind.modelProfile(), clock.instant()));
        diagnosticPlans.put(bind.flowId(), bind.diagnosticPlan());
        diagnosticCompletedAttempts.put(bind.flowId(), 0);
        recordTaskExposure(bind.flowId(), bind.taskPackage());
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                InteractionKind.TASK, bind.flowId(), 1, FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.DIAGNOSTIC, attempt.attemptId(), AttemptPurpose.DIAGNOSTIC,
                bind.taskPackage().learnerProjection(), null, null, null, null);
        return commitBoundary(interaction,
                new LearningCheckpoint(UUID.randomUUID(), bind.flowId(), 1, clock.instant()),
                new ProcessedCommand(bind.idempotencyKey(), bind.requestHash(), bind.flowId(),
                        interaction, clock.instant()));
    }

    @Override
    public synchronized LearningFlowInteraction commitBoundary(
            LearningFlowInteraction interaction,
            LearningCheckpoint checkpoint,
            ProcessedCommand command,
            PendingOperation pending
    ) {
        Objects.requireNonNull(interaction, "interaction must not be null");
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        Objects.requireNonNull(command, "command must not be null");
        ProcessedCommand existingCommand = commands.get(command.idempotencyKey());
        if (existingCommand != null) {
            if (!existingCommand.requestHash().equals(command.requestHash())) {
                throw new ApplicationException(
                        ErrorCode.CONFLICT,
                        "Idempotency-Key was already used for another command");
            }
            return existingCommand.response();
        }
        List<LearningFlowInteraction> history = interactions.computeIfAbsent(
                interaction.flowId(), key -> new ArrayList<>());
        if (history.stream().anyMatch(item -> item.interactionVersion() == interaction.interactionVersion())) {
            return history.stream()
                    .filter(item -> item.interactionVersion() == interaction.interactionVersion())
                    .findFirst()
                    .orElseThrow();
        }
        LearningFlowInteraction previous = history.isEmpty() ? null : history.get(history.size() - 1);
        history.add(interaction);
        checkpoints.computeIfAbsent(checkpoint.flowId(), key -> new ArrayList<>()).add(checkpoint);
        commands.putIfAbsent(command.idempotencyKey(), command);
        // The Flow record mirrors its latest committed interaction status and
        // stage: a terminal boundary releases the Active Learning Work claim
        // of ADR-0070, an awaiting-input boundary holds it.
        FlowRecord current = flows.get(interaction.flowId());
        if (current != null && (current.status() != interaction.status()
                || current.stage() != interaction.stage())) {
            flows.put(current.flowId(), new FlowRecord(
                    current.flowId(), current.learnerId(), current.conceptId(),
                    interaction.status(), interaction.stage(), current.modelProfile(), current.createdAt()));
        }
        if (pending == null) {
            pendingOperations.remove(interaction.flowId());
        } else {
            pendingOperations.put(interaction.flowId(), pending);
        }
        if (isCompletedDiagnosticAttempt(previous)) {
            diagnosticCompletedAttempts.computeIfPresent(
                    interaction.flowId(), (flowId, completed) -> completed + 1);
        }
        return interaction;
    }

    private boolean isCompletedDiagnosticAttempt(LearningFlowInteraction interaction) {
        return interaction != null
                && interaction.kind() == InteractionKind.TASK
                && interaction.stage() == LearningStage.DIAGNOSTIC
                && interaction.attemptPurpose() == AttemptPurpose.DIAGNOSTIC
                && artifactStore != null
                && artifactStore.findAttempt(interaction.attemptId())
                .map(attempt -> attempt.status() == AttemptStatus.SUBMITTED)
                .orElse(false);
    }

    @Override
    public synchronized Optional<PendingOperation> pendingOperation(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        return Optional.ofNullable(pendingOperations.get(flowId));
    }

    @Override
    public synchronized Optional<LearningFlowInteraction> latestInteraction(UUID flowId) {
        List<LearningFlowInteraction> history = interactions.get(flowId);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(history.get(history.size() - 1));
    }

    @Override
    public synchronized Optional<LearningCheckpoint> latestCheckpoint(UUID flowId) {
        List<LearningCheckpoint> history = checkpoints.get(flowId);
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
        exposedTaskPackages.computeIfAbsent(flowId, key -> new LinkedHashSet<>())
                .add(taskPackage.taskPackageId());
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
    public synchronized List<UUID> exposedTaskPackageIds(UUID flowId) {
        return List.copyOf(exposedTaskPackages.getOrDefault(flowId, Set.of()));
    }

    @Override
    public synchronized void recordExampleExposure(UUID flowId, String exampleFingerprint) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(exampleFingerprint, "exampleFingerprint must not be null");
        exampleFingerprints.computeIfAbsent(flowId, key -> new LinkedHashSet<>()).add(exampleFingerprint);
    }

    @Override
    public synchronized List<String> exposedExampleFingerprints(UUID flowId) {
        return List.copyOf(exampleFingerprints.getOrDefault(flowId, Set.of()));
    }

    @Override
    public synchronized void recordHintLadderExposure(UUID flowId, String ladderFingerprint) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(ladderFingerprint, "ladderFingerprint must not be null");
        hintLadderFingerprints.computeIfAbsent(flowId, key -> new LinkedHashSet<>()).add(ladderFingerprint);
    }

    @Override
    public synchronized List<String> exposedHintLadderFingerprints(UUID flowId) {
        return List.copyOf(hintLadderFingerprints.getOrDefault(flowId, Set.of()));
    }

    @Override
    public synchronized void recordRevealedSolutionExposure(UUID flowId, String revealFingerprint) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(revealFingerprint, "revealFingerprint must not be null");
        revealedSolutionFingerprints.computeIfAbsent(flowId, key -> new LinkedHashSet<>()).add(revealFingerprint);
    }

    @Override
    public synchronized List<String> exposedRevealedSolutionFingerprints(UUID flowId) {
        return List.copyOf(revealedSolutionFingerprints.getOrDefault(flowId, Set.of()));
    }

    @Override
    public synchronized void recordAnchor(UUID flowId, TeachBackAnchor anchor) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(anchor, "anchor must not be null");
        List<TeachBackAnchor> anchors = teachBackAnchors.computeIfAbsent(flowId, key -> new ArrayList<>());
        boolean alreadyRecorded = anchors.stream().anyMatch(existing -> existing.anchorId().equals(anchor.anchorId()));
        if (!alreadyRecorded) {
            anchors.add(anchor);
        }
    }

    @Override
    public synchronized Optional<TeachBackAnchor> latestAnchor(UUID flowId) {
        List<TeachBackAnchor> anchors = teachBackAnchors.get(flowId);
        if (anchors == null || anchors.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(anchors.get(anchors.size() - 1));
    }

    @Override
    public synchronized Optional<ReviewTask> acceptEvidenceAndScheduleFirstReview(AcceptedLearningEvidence evidence, Instant dueAt) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(dueAt, "dueAt must not be null");
        if (this.evidence.containsKey(evidence.taskAttemptId())) {
            return Optional.empty();
        }
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
                1, ReviewTaskStatus.SCHEDULED, dueAt, clock.instant(), null, null, null, null);
        reviews.put(review.reviewId(), review);
        return Optional.of(review);
    }

    @Override
    public synchronized Optional<ReviewTask> findReview(UUID reviewId) {
        return Optional.ofNullable(reviews.get(reviewId));
    }

    @Override
    public synchronized ReviewTaskStore.ReviewCancellation cancelReview(
            ReviewTaskStore.ReviewCancellationBind bind
    ) {
        Objects.requireNonNull(bind, "bind must not be null");
        CancellationRecord recorded = reviewCancellations.get(bind.idempotencyKey());
        if (recorded != null) {
            if (!recorded.reviewId().equals(bind.reviewId())
                    || !recorded.requestHash().equals(bind.requestHash())) {
                throw new ApplicationException(ErrorCode.CONFLICT,
                        "Idempotency-Key was already used for another Review cancellation");
            }
            return recorded.outcome();
        }

        ReviewTask current = reviews.get(bind.reviewId());
        if (current == null) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_FOUND, "review task not found");
        }
        LearningFlowInteraction terminalInteraction = null;
        ReviewTask cancelled = current;
        if (current.isUnfinished()) {
            if (current.status() == ReviewTaskStatus.STARTED) {
                if (current.openAttemptId() != null && artifactStore != null) {
                    artifactStore.abandonAttempt(current.openAttemptId());
                }
                terminalInteraction = commitReviewCancellationBoundary(current.flowId(), bind.cancelledAt());
            }
            cancelled = new ReviewTask(
                    current.reviewId(), current.learnerId(), current.conceptId(), current.flowId(),
                    current.reviewNumber(), ReviewTaskStatus.CANCELLED, current.dueAt(), current.createdAt(),
                    current.startedAt(), null, current.completedAt(), bind.cancelledAt());
            reviews.put(cancelled.reviewId(), cancelled);
        }
        ReviewTaskStore.ReviewCancellation outcome = new ReviewTaskStore.ReviewCancellation(
                cancelled, terminalInteraction);
        reviewCancellations.put(bind.idempotencyKey(),
                new CancellationRecord(bind.reviewId(), bind.requestHash(), outcome));
        return outcome;
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
    public synchronized Optional<ReviewTask> cancelStartedReview(UUID learnerId, UUID conceptId, Instant cancelledAt) {
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        Optional<ReviewTask> started = findStartedReview(learnerId, conceptId);
        if (started.isEmpty()) {
            return Optional.empty();
        }
        ReviewTask current = started.get();
        ReviewTask cancelled = new ReviewTask(
                current.reviewId(), current.learnerId(), current.conceptId(), current.flowId(),
                current.reviewNumber(), ReviewTaskStatus.CANCELLED, current.dueAt(), current.createdAt(),
                current.startedAt(), null, current.completedAt(), cancelledAt);
        reviews.put(cancelled.reviewId(), cancelled);
        return Optional.of(cancelled);
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
                current.startedAt(), null, evidence.acceptedAt(), null);
        reviews.put(completed.reviewId(), completed);
        ReviewTask successor = null;
        if (nextDueAt != null) {
            successor = new ReviewTask(
                    UUID.randomUUID(), current.learnerId(), current.conceptId(), current.flowId(),
                    current.reviewNumber() + 1, ReviewTaskStatus.SCHEDULED, nextDueAt,
                    clock.instant(), null, null, null, null);
            reviews.put(successor.reviewId(), successor);
        }
        return Optional.of(new ReviewTaskStore.ReviewAdvance(evidence, completed, successor));
    }

    @Override
    public synchronized Optional<ReviewTaskStore.ReviewStop> acceptEvidenceAndFailReview(
            AcceptedLearningEvidence evidence,
            UUID completedReviewId
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
        for (Map.Entry<UUID, ReviewTask> entry : reviews.entrySet()) {
            ReviewTask review = entry.getValue();
            if (review.learnerId().equals(evidence.learnerId())
                    && review.conceptId().equals(evidence.conceptId())
                    && review.isUnfinished()
                    && !review.reviewId().equals(completedReviewId)) {
                entry.setValue(cancelled(review));
            }
        }
        this.evidence.putIfAbsent(evidence.taskAttemptId(), evidence);
        ReviewTask completed = new ReviewTask(
                current.reviewId(), current.learnerId(), current.conceptId(), current.flowId(),
                current.reviewNumber(), ReviewTaskStatus.COMPLETED, current.dueAt(), current.createdAt(),
                current.startedAt(), null, evidence.acceptedAt(), null);
        reviews.put(completed.reviewId(), completed);
        return Optional.of(new ReviewTaskStore.ReviewStop(evidence, completed));
    }

    @Override
    public synchronized Optional<LearningFlowInteraction> bindReviewAttempt(ReviewTaskStore.ReviewStartBind bind) {
        Objects.requireNonNull(bind, "bind must not be null");
        if (artifactStore == null) {
            throw new IllegalStateException(
                    "bindReviewAttempt requires the composite InMemoryLearningFlowStore(clock, artifactStore) form");
        }
        ReviewTask review = reviews.get(bind.reviewId());
        boolean dueStart = review != null && review.status() == ReviewTaskStatus.DUE;
        boolean resume = review != null && review.status() == ReviewTaskStatus.STARTED
                && review.openAttemptId() == null;
        if (!dueStart && !resume) {
            return Optional.empty();
        }
        ReviewTask claimed = new ReviewTask(
                review.reviewId(), review.learnerId(), review.conceptId(), review.flowId(),
                review.reviewNumber(), ReviewTaskStatus.STARTED, review.dueAt(), review.createdAt(),
                resume ? review.startedAt() : bind.startedAt(), null, null, null);
        TaskAttempt attempt = artifactStore.openAttempt(bind.taskPackage());
        reviews.put(bind.reviewId(), claimed.withOpenAttempt(attempt.attemptId()));
        recordTaskExposure(bind.flowId(), bind.taskPackage());
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                InteractionKind.TASK, bind.flowId(), bind.interactionVersion(), FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.DELAYED_REVIEW, attempt.attemptId(), AttemptPurpose.REVIEW,
                bind.taskPackage().learnerProjection(), null, null, null, null);
        return Optional.of(commitBoundary(interaction,
                new LearningCheckpoint(UUID.randomUUID(), bind.flowId(), bind.interactionVersion(), clock.instant()),
                new ProcessedCommand(bind.idempotencyKey(), bind.requestHash(), bind.flowId(),
                        interaction, clock.instant())));
    }

    @Override
    public synchronized Optional<LearningFlowInteraction> resolveInconclusiveSubmission(
            ReviewTaskStore.ResolveInconclusiveBind bind
    ) {
        Objects.requireNonNull(bind, "bind must not be null");
        if (artifactStore == null) {
            throw new IllegalStateException(
                    "resolveInconclusiveSubmission requires the composite InMemoryLearningFlowStore(clock, artifactStore) form");
        }
        ReviewTask review = reviews.get(bind.reviewId());
        if (review == null || review.status() != ReviewTaskStatus.STARTED
                || !Objects.equals(review.openAttemptId(), bind.closedAttemptId())) {
            return Optional.empty();
        }
        TaskAttempt replacement = null;
        UUID openAttemptId = null;
        LearnerProjection replacementProjection = null;
        if (bind.replacementPackage() != null) {
            replacement = artifactStore.openAttempt(bind.replacementPackage());
            openAttemptId = replacement.attemptId();
            recordTaskExposure(review.flowId(), bind.replacementPackage());
            replacementProjection = bind.replacementPackage().learnerProjection();
        }
        reviews.put(bind.reviewId(), review.withOpenAttempt(openAttemptId));
        LearningFlowInteraction interaction = replacement == null
                ? new LearningFlowInteraction(
                        InteractionKind.UNAVAILABLE, review.flowId(), bind.interactionVersion(), FlowStatus.TERMINAL,
                        LearningStage.DELAYED_REVIEW, null, null, null, bind.learnerMessage(), null, null, null)
                : new LearningFlowInteraction(
                        InteractionKind.TASK, review.flowId(), bind.interactionVersion(), FlowStatus.AWAITING_LEARNER_INPUT,
                        LearningStage.DELAYED_REVIEW, replacement.attemptId(), AttemptPurpose.REVIEW,
                        replacementProjection, bind.learnerMessage(), null, null, null);
        return Optional.of(commitBoundary(interaction,
                new LearningCheckpoint(UUID.randomUUID(), review.flowId(), bind.interactionVersion(), clock.instant()),
                new ProcessedCommand(bind.idempotencyKey(), bind.requestHash(), review.flowId(),
                        interaction, clock.instant())));
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
                        null, null, null, null));
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
                review.startedAt(), null, review.completedAt(), clock.instant());
    }

    private LearningFlowInteraction commitReviewCancellationBoundary(UUID flowId, Instant cancelledAt) {
        LearningFlowInteraction previous = latestInteraction(flowId)
                .orElseThrow(() -> new IllegalStateException("started Review flow has no interaction"));
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                InteractionKind.TRANSITION,
                flowId,
                previous.interactionVersion() + 1,
                FlowStatus.TERMINAL,
                LearningStage.DELAYED_REVIEW,
                null,
                null,
                null,
                ReviewTaskStore.REVIEW_CANCELLED_MESSAGE,
                null,
                null,
                null);
        interactions.computeIfAbsent(flowId, key -> new ArrayList<>()).add(interaction);
        checkpoints.computeIfAbsent(flowId, key -> new ArrayList<>()).add(
                new LearningCheckpoint(UUID.randomUUID(), flowId, interaction.interactionVersion(), cancelledAt));
        FlowRecord current = flows.get(flowId);
        if (current != null) {
            flows.put(flowId, new FlowRecord(
                    current.flowId(), current.learnerId(), current.conceptId(),
                    FlowStatus.TERMINAL, LearningStage.DELAYED_REVIEW,
                    current.modelProfile(), current.createdAt()));
        }
        return interaction;
    }

    @Override
    public synchronized boolean evidenceExists(UUID attemptId) {
        return evidence.containsKey(attemptId);
    }

    @Override
    public synchronized boolean acceptEvidence(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        return this.evidence.putIfAbsent(evidence.taskAttemptId(), evidence) == null;
    }

    @Override
    public synchronized List<AcceptedLearningEvidence> allEvidence() {
        return List.copyOf(evidence.values());
    }

    @Override
    public synchronized Optional<ProcessedCommand> findCommand(UUID idempotencyKey) {
        return Optional.ofNullable(commands.get(idempotencyKey));
    }

    private record CancellationRecord(
            UUID reviewId,
            String requestHash,
            ReviewTaskStore.ReviewCancellation outcome
    ) {
    }
}
