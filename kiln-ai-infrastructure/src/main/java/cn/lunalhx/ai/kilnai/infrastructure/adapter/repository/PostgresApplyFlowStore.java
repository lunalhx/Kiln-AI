package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceTraceEntry;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptConversionOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExposureOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLevel;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintRequestRecord;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Postgres-backed typed LearningFlowStore and ArtifactStore for the Apply
 * reference. Every boundary commit (package plus open Attempt, closed Attempt
 * plus submission, learner interaction plus checkpoint plus processed command)
 * is one database transaction. Instantiated by
 * {@code KilnAiPersistenceAutoConfiguration} only when a DataSource exists.
 */
public class PostgresApplyFlowStore implements LearningFlowStore, ArtifactStore, ReviewTaskStore {

    private final ApplyFlowMapper mapper;
    private final ObjectMapper json;
    private final Clock clock;

    public PostgresApplyFlowStore(ApplyFlowMapper mapper, ObjectMapper json, Clock clock) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.json = Objects.requireNonNull(json, "json must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void insertFlow(FlowRecord flow) {
        mapper.insertFlow(flow.flowId(), flow.learnerId(), flow.conceptId(),
                flow.status().name(), flow.stage().name(), writeJson(flow.modelProfile()), flow.createdAt());
    }

    @Override
    public Optional<FlowRecord> findFlow(UUID flowId) {
        return mapper.findFlow(flowId).map(row -> new FlowRecord(
                row.id(), row.learnerId(), row.conceptId(),
                FlowStatus.valueOf(row.status()), LearningStage.valueOf(row.stage()),
                readJson(row.modelProfileJson(), ModelProfile.class), row.createdAt()));
    }

    @Override
    @Transactional
    public void commitBoundary(ApplyFlowInteraction interaction, ApplyCheckpoint checkpoint, ProcessedCommand command) {
        mapper.insertInteraction(new ApplyFlowMapper.InteractionRow(
                UUID.randomUUID(),
                interaction.flowId(),
                interaction.interactionVersion(),
                interaction.status().name(),
                interaction.stage().name(),
                interaction.attemptId(),
                interaction.attemptPurpose() == null ? null : interaction.attemptPurpose().name(),
                interaction.learnerProjection() == null ? null : writeJson(interaction.learnerProjection()),
                interaction.learnerMessage(),
                interaction.teachingProjection() == null ? null : writeJson(interaction.teachingProjection()),
                interaction.hint() == null ? null : writeJson(interaction.hint()),
                interaction.assistanceConsent() == null ? null : writeJson(interaction.assistanceConsent()),
                checkpoint.createdAt()));
        mapper.insertCheckpoint(new ApplyFlowMapper.CheckpointRow(
                checkpoint.checkpointId(), checkpoint.flowId(), checkpoint.interactionVersion(),
                checkpoint.createdAt()));
        mapper.insertCommand(new ApplyFlowMapper.CommandRow(
                command.idempotencyKey(), command.requestHash(), command.flowId(),
                writeJson(command.response()), command.createdAt()));
    }

    @Override
    public void saveExplainArtifact(UUID flowId, ExplainTeachingArtifact artifact) {
        mapper.insertExplainArtifact(
                artifact.artifactId(), flowId, writeJson(artifact), clock.instant());
    }

    @Override
    public Optional<ExplainTeachingArtifact> findExplainArtifact(UUID artifactId) {
        return mapper.findExplainArtifact(artifactId)
                .map(row -> readJson(row.artifactJson(), ExplainTeachingArtifact.class));
    }

    @Override
    public Optional<ApplyFlowInteraction> latestInteraction(UUID flowId) {
        return mapper.latestInteraction(flowId).map(row -> new ApplyFlowInteraction(
                row.flowId(),
                row.interactionVersion(),
                FlowStatus.valueOf(row.status()),
                LearningStage.valueOf(row.stage()),
                row.attemptId(),
                row.attemptPurpose() == null ? null : AttemptPurpose.valueOf(row.attemptPurpose()),
                row.learnerProjectionJson() == null ? null : readJson(row.learnerProjectionJson(),
                        cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection.class),
                row.learnerMessage(),
                row.teachingProjectionJson() == null ? null : readJson(row.teachingProjectionJson(),
                        cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection.class),
                row.hintJson() == null ? null : readJson(row.hintJson(),
                        cn.lunalhx.ai.kilnai.domain.apply.model.HintView.class),
                row.assistanceConsentJson() == null ? null : readJson(row.assistanceConsentJson(),
                        cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceConsentView.class)));
    }

    @Override
    public Optional<ApplyCheckpoint> latestCheckpoint(UUID flowId) {
        return mapper.latestCheckpoint(flowId).map(row -> new ApplyCheckpoint(
                row.id(), row.flowId(), row.interactionVersion(), row.createdAt()));
    }

    @Override
    public void recordTaskExposure(UUID flowId, TaskPackage taskPackage) {
        mapper.recordExposure(
                flowId,
                taskPackage.taskPackageId(),
                taskPackage.privateAssessorProjection().taskFingerprint().value(),
                taskPackage.privateAssessorProjection().solutionFingerprint().value(),
                clock.instant());
    }

    @Override
    public List<String> exposedTaskFingerprints(UUID flowId) {
        return mapper.exposedTaskFingerprints(flowId);
    }

    @Override
    public List<String> exposedSolutionFingerprints(UUID flowId) {
        return mapper.exposedSolutionFingerprints(flowId);
    }

    @Override
    public List<UUID> exposedTaskPackageIds(UUID flowId) {
        return mapper.exposedTaskPackageIds(flowId);
    }

    @Override
    public void recordExampleExposure(UUID flowId, String exampleFingerprint) {
        mapper.recordExampleExposure(flowId, exampleFingerprint, clock.instant());
    }

    @Override
    public List<String> exposedExampleFingerprints(UUID flowId) {
        return mapper.exposedExampleFingerprints(flowId);
    }

    @Override
    public void recordHintLadderExposure(UUID flowId, String ladderFingerprint) {
        mapper.recordHintLadderExposure(flowId, ladderFingerprint, clock.instant());
    }

    @Override
    public List<String> exposedHintLadderFingerprints(UUID flowId) {
        return mapper.exposedHintLadderFingerprints(flowId);
    }

    @Override
    public void recordRevealedSolutionExposure(UUID flowId, String revealFingerprint) {
        mapper.recordRevealedSolutionExposure(flowId, revealFingerprint, clock.instant());
    }

    @Override
    public List<String> exposedRevealedSolutionFingerprints(UUID flowId) {
        return mapper.exposedRevealedSolutionFingerprints(flowId);
    }

    @Override
    public void recordAnchor(UUID flowId, TeachBackAnchor anchor) {
        mapper.insertTeachBackAnchor(
                flowId, anchor.anchorId(), anchor.kind().name(), anchor.exposedAt());
    }

    @Override
    public Optional<TeachBackAnchor> latestAnchor(UUID flowId) {
        List<ApplyFlowMapper.TeachBackAnchorRow> anchors = mapper.listTeachBackAnchors(flowId);
        if (anchors.isEmpty()) {
            return Optional.empty();
        }
        ApplyFlowMapper.TeachBackAnchorRow latest = anchors.get(anchors.size() - 1);
        return Optional.of(new TeachBackAnchor(
                TeachBackAnchor.TeachBackAnchorKind.valueOf(latest.anchorKind()),
                latest.anchorId(),
                latest.exposedAt()));
    }

    @Override
    @Transactional
    public Optional<ReviewTask> acceptEvidenceAndScheduleFirstReview(AcceptedLearningEvidence evidence, Instant dueAt) {
        if (mapper.evidenceExists(evidence.taskAttemptId()).isPresent()) {
            return Optional.empty();
        }
        mapper.cancelUnfinishedReviews(evidence.learnerId(), evidence.conceptId(), clock.instant());
        mapper.insertEvidence(new ApplyFlowMapper.EvidenceRow(
                evidence.id(),
                evidence.taskAttemptId(),
                evidence.flowId(),
                evidence.conceptId(),
                evidence.learnerId(),
                evidence.result().name(),
                evidence.attemptPurpose().name(),
                evidence.highestHintLevel(),
                writeJson(evidence.assistanceTrace()),
                evidence.acceptedAt()));
        ReviewTask review = new ReviewTask(
                UUID.randomUUID(), evidence.learnerId(), evidence.conceptId(), evidence.flowId(),
                1, ReviewTaskStatus.SCHEDULED, dueAt, clock.instant(), null, null, null, null);
        mapper.insertReviewTask(new ApplyFlowMapper.ReviewTaskRow(
                review.reviewId(),
                review.learnerId(),
                review.conceptId(),
                review.flowId(),
                review.reviewNumber(),
                review.status().name(),
                review.dueAt(),
                review.createdAt(),
                review.startedAt(),
                review.openAttemptId(),
                review.completedAt(),
                review.cancelledAt()));
        return Optional.of(review);
    }

    @Override
    public List<ReviewTask> unfinishedReviewsFor(UUID learnerId) {
        return mapper.listUnfinishedReviews(learnerId).stream().map(this::toReviewTask).toList();
    }

    @Override
    @Transactional
    public int markDueReviewsDue(Instant now) {
        return mapper.markDueReviewsDue(now);
    }

    @Override
    public Optional<ReviewTask> findReview(UUID reviewId) {
        return mapper.findReviewTask(reviewId).map(this::toReviewTask);
    }

    @Override
    public Optional<ReviewTask> findStartedReview(UUID learnerId, UUID conceptId) {
        return mapper.findStartedReview(learnerId, conceptId).map(this::toReviewTask);
    }

    @Override
    @Transactional
    public Optional<ReviewTask> cancelStartedReview(UUID learnerId, UUID conceptId, Instant cancelledAt) {
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        ReviewTask started = toReviewTask(mapper.findStartedReview(learnerId, conceptId).orElse(null));
        if (started == null) {
            return Optional.empty();
        }
        int cancelled = mapper.cancelStartedReview(learnerId, conceptId, cancelledAt);
        if (cancelled == 0) {
            return Optional.empty();
        }
        return Optional.of(new ReviewTask(
                started.reviewId(), started.learnerId(), started.conceptId(), started.flowId(),
                started.reviewNumber(), ReviewTaskStatus.CANCELLED, started.dueAt(), started.createdAt(),
                started.startedAt(), null, started.completedAt(), cancelledAt));
    }

    @Override
    @Transactional
    public Optional<ReviewTaskStore.ReviewAdvance> acceptEvidenceAndAdvanceReview(
            AcceptedLearningEvidence evidence,
            UUID completedReviewId,
            Instant nextDueAt
    ) {
        if (mapper.evidenceExists(evidence.taskAttemptId()).isPresent()) {
            return Optional.empty();
        }
        int completed = mapper.completeStartedReview(completedReviewId, evidence.acceptedAt());
        if (completed == 0) {
            return Optional.empty();
        }
        mapper.insertEvidence(new ApplyFlowMapper.EvidenceRow(
                evidence.id(),
                evidence.taskAttemptId(),
                evidence.flowId(),
                evidence.conceptId(),
                evidence.learnerId(),
                evidence.result().name(),
                evidence.attemptPurpose().name(),
                evidence.highestHintLevel(),
                writeJson(evidence.assistanceTrace()),
                evidence.acceptedAt()));
        ReviewTask completedReview = toReviewTask(mapper.findReviewTask(completedReviewId).orElseThrow());
        ReviewTask successor = null;
        if (nextDueAt != null) {
            successor = new ReviewTask(
                    UUID.randomUUID(), evidence.learnerId(), evidence.conceptId(), evidence.flowId(),
                    completedReview.reviewNumber() + 1, ReviewTaskStatus.SCHEDULED, nextDueAt,
                    clock.instant(), null, null, null, null);
            mapper.insertReviewTask(new ApplyFlowMapper.ReviewTaskRow(
                    successor.reviewId(),
                    successor.learnerId(),
                    successor.conceptId(),
                    successor.flowId(),
                    successor.reviewNumber(),
                    successor.status().name(),
                    successor.dueAt(),
                    successor.createdAt(),
                    successor.startedAt(),
                    successor.openAttemptId(),
                    successor.completedAt(),
                    successor.cancelledAt()));
        }
        return Optional.of(new ReviewTaskStore.ReviewAdvance(evidence, completedReview, successor));
    }

    @Override
    @Transactional
    public Optional<ReviewTaskStore.ReviewStop> acceptEvidenceAndFailReview(
            AcceptedLearningEvidence evidence,
            UUID completedReviewId
    ) {
        if (mapper.evidenceExists(evidence.taskAttemptId()).isPresent()) {
            return Optional.empty();
        }
        int completed = mapper.completeStartedReview(completedReviewId, evidence.acceptedAt());
        if (completed == 0) {
            return Optional.empty();
        }
        mapper.cancelUnfinishedReviews(evidence.learnerId(), evidence.conceptId(), evidence.acceptedAt());
        mapper.insertEvidence(new ApplyFlowMapper.EvidenceRow(
                evidence.id(),
                evidence.taskAttemptId(),
                evidence.flowId(),
                evidence.conceptId(),
                evidence.learnerId(),
                evidence.result().name(),
                evidence.attemptPurpose().name(),
                evidence.highestHintLevel(),
                writeJson(evidence.assistanceTrace()),
                evidence.acceptedAt()));
        ReviewTask completedReview = toReviewTask(mapper.findReviewTask(completedReviewId).orElseThrow());
        return Optional.of(new ReviewTaskStore.ReviewStop(evidence, completedReview));
    }

    @Override
    @Transactional
    public Optional<ApplyFlowInteraction> bindReviewAttempt(ReviewTaskStore.ReviewStartBind bind) {
        TaskAttempt attempt = TaskAttempt.open(bind.taskPackage(), bind.startedAt());
        int claimed = mapper.claimReviewAttempt(bind.reviewId(), bind.startedAt(), attempt.attemptId());
        if (claimed == 0) {
            return Optional.empty();
        }
        mapper.insertPackage(new ApplyFlowMapper.PackageRow(
                bind.taskPackage().taskPackageId(),
                bind.taskPackage().attemptPurpose().name(),
                writeJson(bind.taskPackage().learnerProjection()),
                writeJson(bind.taskPackage().privateAssessorProjection()),
                bind.startedAt()));
        mapper.insertAttempt(new ApplyFlowMapper.AttemptRow(
                attempt.attemptId(),
                attempt.taskPackageId(),
                attempt.purpose().name(),
                attempt.status().name(),
                attempt.openedAt(),
                null,
                null,
                writeJson(attempt.assistanceTrace())));
        mapper.recordExposure(
                bind.flowId(),
                bind.taskPackage().taskPackageId(),
                bind.taskPackage().privateAssessorProjection().taskFingerprint().value(),
                bind.taskPackage().privateAssessorProjection().solutionFingerprint().value(),
                bind.startedAt());
        ApplyFlowInteraction interaction = new ApplyFlowInteraction(
                bind.flowId(), bind.interactionVersion(), FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.DELAYED_REVIEW, attempt.attemptId(), AttemptPurpose.REVIEW,
                bind.taskPackage().learnerProjection(), null, null, null, null);
        insertBoundary(bind.flowId(), bind.interactionVersion(), interaction,
                bind.idempotencyKey(), bind.requestHash(), bind.startedAt());
        return Optional.of(interaction);
    }

    @Override
    @Transactional
    public Optional<ApplyFlowInteraction> resolveInconclusiveSubmission(
            ReviewTaskStore.ResolveInconclusiveBind bind
    ) {
        ReviewTask review = toReviewTask(mapper.findReviewTask(bind.reviewId()).orElse(null));
        if (review == null) {
            return Optional.empty();
        }
        TaskAttempt replacement = null;
        UUID openAttemptId = null;
        LearnerProjection replacementProjection = null;
        if (bind.replacementPackage() != null) {
            replacement = TaskAttempt.open(bind.replacementPackage(), clock.instant());
            openAttemptId = replacement.attemptId();
            replacementProjection = bind.replacementPackage().learnerProjection();
        }
        int resolved = mapper.resolveInconclusiveClaim(
                bind.reviewId(), bind.closedAttemptId(), openAttemptId);
        if (resolved == 0) {
            return Optional.empty();
        }
        if (replacement != null) {
            mapper.insertPackage(new ApplyFlowMapper.PackageRow(
                    bind.replacementPackage().taskPackageId(),
                    bind.replacementPackage().attemptPurpose().name(),
                    writeJson(bind.replacementPackage().learnerProjection()),
                    writeJson(bind.replacementPackage().privateAssessorProjection()),
                    clock.instant()));
            mapper.insertAttempt(new ApplyFlowMapper.AttemptRow(
                    replacement.attemptId(),
                    replacement.taskPackageId(),
                    replacement.purpose().name(),
                    replacement.status().name(),
                    replacement.openedAt(),
                    null,
                    null,
                    writeJson(replacement.assistanceTrace())));
            mapper.recordExposure(
                    review.flowId(),
                    bind.replacementPackage().taskPackageId(),
                    bind.replacementPackage().privateAssessorProjection().taskFingerprint().value(),
                    bind.replacementPackage().privateAssessorProjection().solutionFingerprint().value(),
                    clock.instant());
        }
        ApplyFlowInteraction interaction = replacement == null
                ? new ApplyFlowInteraction(
                        review.flowId(), bind.interactionVersion(), FlowStatus.TERMINAL,
                        LearningStage.DELAYED_REVIEW, null, null, null, bind.learnerMessage(), null, null, null)
                : new ApplyFlowInteraction(
                        review.flowId(), bind.interactionVersion(), FlowStatus.AWAITING_LEARNER_INPUT,
                        LearningStage.DELAYED_REVIEW, replacement.attemptId(), AttemptPurpose.REVIEW,
                        replacementProjection, bind.learnerMessage(), null, null, null);
        insertBoundary(review.flowId(), bind.interactionVersion(), interaction,
                bind.idempotencyKey(), bind.requestHash(), clock.instant());
        return Optional.of(interaction);
    }

    private void insertBoundary(
            UUID flowId,
            int interactionVersion,
            ApplyFlowInteraction interaction,
            UUID idempotencyKey,
            String requestHash,
            Instant createdAt
    ) {
        mapper.insertInteraction(new ApplyFlowMapper.InteractionRow(
                UUID.randomUUID(),
                interaction.flowId(),
                interaction.interactionVersion(),
                interaction.status().name(),
                interaction.stage().name(),
                interaction.attemptId(),
                interaction.attemptPurpose() == null ? null : interaction.attemptPurpose().name(),
                interaction.learnerProjection() == null ? null : writeJson(interaction.learnerProjection()),
                interaction.learnerMessage(),
                interaction.teachingProjection() == null ? null : writeJson(interaction.teachingProjection()),
                interaction.hint() == null ? null : writeJson(interaction.hint()),
                interaction.assistanceConsent() == null ? null : writeJson(interaction.assistanceConsent()),
                createdAt));
        mapper.insertCheckpoint(new ApplyFlowMapper.CheckpointRow(
                UUID.randomUUID(), flowId, interactionVersion, createdAt));
        mapper.insertCommand(new ApplyFlowMapper.CommandRow(
                idempotencyKey, requestHash, flowId,
                writeJson(interaction), createdAt));
    }

    private ReviewTask toReviewTask(ApplyFlowMapper.ReviewTaskRow row) {
        if (row == null) {
            return null;
        }
        return new ReviewTask(
                row.id(), row.learnerId(), row.conceptId(), row.flowId(), row.reviewNumber(),
                ReviewTaskStatus.valueOf(row.status()), row.dueAt(), row.createdAt(),
                row.startedAt(), row.openAttemptId(), row.completedAt(), row.cancelledAt());
    }

    @Override
    public boolean evidenceExists(UUID attemptId) {
        return mapper.evidenceExists(attemptId).isPresent();
    }

    @Override
    @Transactional
    public boolean acceptEvidence(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        if (mapper.evidenceExists(evidence.taskAttemptId()).isPresent()) {
            return false;
        }
        mapper.insertEvidence(new ApplyFlowMapper.EvidenceRow(
                evidence.id(),
                evidence.taskAttemptId(),
                evidence.flowId(),
                evidence.conceptId(),
                evidence.learnerId(),
                evidence.result().name(),
                evidence.attemptPurpose().name(),
                evidence.highestHintLevel(),
                writeJson(evidence.assistanceTrace()),
                evidence.acceptedAt()));
        return true;
    }

    @Override
    public List<AcceptedLearningEvidence> allEvidence() {
        return mapper.listEvidence().stream().map(row -> new AcceptedLearningEvidence(
                row.id(),
                row.taskAttemptId(),
                row.flowId(),
                row.conceptId(),
                row.learnerId(),
                LearningResult.valueOf(row.result()),
                AttemptPurpose.valueOf(row.attemptPurpose()),
                row.highestHintLevel(),
                readJson(row.assistanceTraceJson(), new TypeReference<List<String>>() {
                }),
                row.acceptedAt())).toList();
    }

    @Override
    public Optional<ProcessedCommand> findCommand(UUID idempotencyKey) {
        return mapper.findCommand(idempotencyKey).map(row -> new ProcessedCommand(
                row.idempotencyKey(), row.requestHash(), row.flowId(),
                readJson(row.responseJson(), ApplyFlowInteraction.class), row.createdAt()));
    }

    @Override
    @Transactional
    public TaskAttempt openAttempt(TaskPackage taskPackage) {
        TaskAttempt attempt = TaskAttempt.open(taskPackage, clock.instant());
        mapper.insertPackage(new ApplyFlowMapper.PackageRow(
                taskPackage.taskPackageId(),
                taskPackage.attemptPurpose().name(),
                writeJson(taskPackage.learnerProjection()),
                writeJson(taskPackage.privateAssessorProjection()),
                clock.instant()));
        mapper.insertAttempt(new ApplyFlowMapper.AttemptRow(
                attempt.attemptId(),
                attempt.taskPackageId(),
                attempt.purpose().name(),
                attempt.status().name(),
                attempt.openedAt(),
                null,
                null,
                writeJson(attempt.assistanceTrace())));
        return attempt;
    }

    @Override
    @Transactional
    public TaskAttempt openAttempt(TeachBackTaskPackage taskPackage) {
        TaskAttempt attempt = TaskAttempt.open(taskPackage, clock.instant());
        mapper.insertTeachBackPackage(new ApplyFlowMapper.TeachBackPackageRow(
                taskPackage.taskPackageId(),
                taskPackage.attemptPurpose().name(),
                writeJson(taskPackage.learnerProjection()),
                writeJson(taskPackage.privateProjection()),
                clock.instant()));
        mapper.insertAttempt(new ApplyFlowMapper.AttemptRow(
                attempt.attemptId(),
                attempt.taskPackageId(),
                attempt.purpose().name(),
                attempt.status().name(),
                attempt.openedAt(),
                null,
                null,
                writeJson(attempt.assistanceTrace())));
        return attempt;
    }

    @Override
    public Optional<TaskPackage> findPackage(UUID taskPackageId) {
        return mapper.findPackage(taskPackageId).map(row -> new TaskPackage(
                TaskPackage.SCHEMA,
                row.id(),
                AttemptPurpose.valueOf(row.attemptPurpose()),
                readJson(row.learnerProjectionJson(), cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection.class),
                readJson(row.privateAssessorProjectionJson(),
                        cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection.class)));
    }

    @Override
    public Optional<TeachBackTaskPackage> findTeachBackPackage(UUID taskPackageId) {
        return mapper.findTeachBackPackage(taskPackageId).map(row -> new TeachBackTaskPackage(
                TeachBackTaskPackage.SCHEMA,
                row.id(),
                AttemptPurpose.valueOf(row.attemptPurpose()),
                readJson(row.learnerProjectionJson(), cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection.class),
                readJson(row.privateProjectionJson(),
                        cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage.TeachBackPrivateProjection.class)));
    }

    @Override
    public List<TaskPackage> allPackages() {
        return mapper.listPackages().stream().map(row -> new TaskPackage(
                TaskPackage.SCHEMA,
                row.id(),
                AttemptPurpose.valueOf(row.attemptPurpose()),
                readJson(row.learnerProjectionJson(), cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection.class),
                readJson(row.privateAssessorProjectionJson(),
                        cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection.class))).toList();
    }

    @Override
    public Optional<TaskAttempt> findAttempt(UUID attemptId) {
        return mapper.findAttempt(attemptId).map(row -> new TaskAttempt(
                row.id(),
                row.taskPackageId(),
                AttemptPurpose.valueOf(row.purpose()),
                AttemptStatus.valueOf(row.status()),
                row.openedAt(),
                row.closedAt(),
                row.submissionJson() == null ? null : readJson(row.submissionJson(), TaskSubmission.class),
                readJson(row.assistanceTraceJson(), new TypeReference<List<AssistanceTraceEntry>>() {
                })));
    }

    @Override
    public Optional<TaskAttempt> findOpenPracticeAttempt(List<UUID> taskPackageIds) {
        if (taskPackageIds.isEmpty()) {
            return Optional.empty();
        }
        return mapper.findOpenPracticeAttempt(taskPackageIds).map(row -> new TaskAttempt(
                row.id(),
                row.taskPackageId(),
                AttemptPurpose.valueOf(row.purpose()),
                AttemptStatus.valueOf(row.status()),
                row.openedAt(),
                row.closedAt(),
                row.submissionJson() == null ? null : readJson(row.submissionJson(), TaskSubmission.class),
                readJson(row.assistanceTraceJson(), new TypeReference<List<AssistanceTraceEntry>>() {
                })));
    }

    @Override
    public Optional<HintLadder> findLadder(UUID attemptId) {
        return mapper.findHintLadder(attemptId).map(row -> readJson(row.ladderJson(), HintLadder.class));
    }

    @Override
    @Transactional
    public HintExposureOutcome exposeHint(
            UUID attemptId,
            HintLadder ladder,
            int requestedLevel,
            UUID commandKey
    ) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(ladder, "ladder must not be null");
        Objects.requireNonNull(commandKey, "commandKey must not be null");
        TaskAttempt current = findAttempt(attemptId).orElse(null);
        if (current == null) {
            return new HintExposureOutcome.NotFound();
        }
        // A saved request record resumes a crashed command even when its
        // exposure already closed the attempt (H5 reveal).
        HintRequestRecord recorded = findHintRequest(attemptId, commandKey).orElse(null);
        if (recorded != null) {
            return new HintExposureOutcome.AlreadyExposed(current, recorded);
        }
        if (current.purpose() != AttemptPurpose.PRACTICE || !current.isOpen()) {
            return new HintExposureOutcome.NotOpen(current);
        }
        TaskAttempt extended = current.appendAssistance(
                AssistanceTraceEntry.hint(HintLevel.of(requestedLevel), clock.instant()));
        AttemptCloseOutcome closed = requestedLevel == 5
                ? extended.closeAsSolutionRevealed(clock.instant())
                : new AttemptCloseOutcome(AttemptCloseOutcome.Result.CLOSED, extended);
        if (closed.result() != AttemptCloseOutcome.Result.CLOSED) {
            return new HintExposureOutcome.NotOpen(current);
        }
        mapper.insertHintLadder(attemptId, writeJson(ladder), clock.instant());
        int updated = mapper.appendAssistanceAndReveal(
                attemptId,
                writeJson(closed.attempt().assistanceTrace()),
                closed.attempt().status().name(),
                closed.attempt().closedAt());
        if (updated == 0) {
            return findAttempt(attemptId)
                    .<HintExposureOutcome>map(HintExposureOutcome.NotOpen::new)
                    .orElseGet(HintExposureOutcome.NotFound::new);
        }
        HintRequestRecord request = new HintRequestRecord(
                attemptId, commandKey, requestedLevel, requestedLevel, clock.instant());
        mapper.insertHintRequest(new ApplyFlowMapper.HintRequestRow(
                request.attemptId(), request.commandKey(), request.requestedLevel(),
                request.exposedLevel(), request.exposedAt()));
        return new HintExposureOutcome.Exposed(closed.attempt(), request);
    }

    @Override
    public Optional<HintRequestRecord> findHintRequest(UUID attemptId, UUID commandKey) {
        return mapper.findHintRequest(attemptId, commandKey).map(row -> new HintRequestRecord(
                row.attemptId(), row.commandKey(), row.requestedLevel(), row.exposedLevel(), row.exposedAt()));
    }

    @Override
    @Transactional
    public AttemptCloseOutcome closeAttempt(UUID attemptId, TaskSubmission submission) {
        TaskAttempt current = findAttempt(attemptId).orElse(null);
        if (current == null) {
            return new AttemptCloseOutcome(AttemptCloseOutcome.Result.NOT_FOUND, null);
        }
        AttemptCloseOutcome outcome = current.close(submission, clock.instant());
        if (outcome.result() != AttemptCloseOutcome.Result.CLOSED) {
            return outcome;
        }
        TaskAttempt closed = outcome.attempt();
        int updated = mapper.closeOpenAttempt(attemptId, closed.closedAt(), writeJson(submission));
        if (updated == 0) {
            return findAttempt(attemptId)
                    .map(latest -> new AttemptCloseOutcome(AttemptCloseOutcome.Result.ALREADY_CLOSED, latest))
                    .orElseGet(() -> new AttemptCloseOutcome(AttemptCloseOutcome.Result.NOT_FOUND, null));
        }
        return new AttemptCloseOutcome(AttemptCloseOutcome.Result.CLOSED, closed);
    }

    @Override
    @Transactional
    public Optional<TaskAttempt> appendAssistance(UUID attemptId, List<AssistanceTraceEntry> entries) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        TaskAttempt current = findAttempt(attemptId).orElse(null);
        if (current == null || !current.isOpen()) {
            return Optional.empty();
        }
        TaskAttempt extended = current;
        for (AssistanceTraceEntry entry : entries) {
            extended = extended.appendAssistance(entry);
        }
        int updated = mapper.appendAttemptAssistance(attemptId, writeJson(extended.assistanceTrace()));
        if (updated == 0) {
            return Optional.empty();
        }
        return Optional.of(extended);
    }

    @Override
    @Transactional
    public AttemptConversionOutcome convertToPractice(UUID attemptId, List<AssistanceTraceEntry> entries) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        TaskAttempt current = findAttempt(attemptId).orElse(null);
        if (current == null) {
            return new AttemptConversionOutcome.Ignored(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        }
        if (!current.isOpen()) {
            return new AttemptConversionOutcome.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        if (current.purpose() == AttemptPurpose.PRACTICE) {
            return new AttemptConversionOutcome.AlreadyPractice(current);
        }
        if (current.purpose() != AttemptPurpose.INDEPENDENT_TEST
                && current.purpose() != AttemptPurpose.REVIEW) {
            return new AttemptConversionOutcome.Ignored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
        }
        TaskAttempt converted = new TaskAttempt(
                current.attemptId(), current.taskPackageId(), AttemptPurpose.PRACTICE,
                current.status(), current.openedAt(), current.closedAt(),
                current.submission(), current.assistanceTrace());
        for (AssistanceTraceEntry entry : entries) {
            converted = converted.appendAssistance(entry);
        }
        int updated = mapper.convertAttemptToPractice(attemptId, writeJson(converted.assistanceTrace()));
        if (updated == 0) {
            return findAttempt(attemptId)
                    .<AttemptConversionOutcome>map(AttemptConversionOutcome.AlreadyPractice::new)
                    .orElseGet(() -> new AttemptConversionOutcome.Ignored(
                            SubmissionIgnoreReason.ALREADY_SUBMITTED));
        }
        return new AttemptConversionOutcome.Converted(converted);
    }

    @Override
    public void recordTaskVerification(UUID taskPackageId, TaskVerificationVerdict verdict) {
        mapper.insertVerification(UUID.randomUUID(), taskPackageId, writeJson(verdict), clock.instant());
    }

    @Override
    public List<TaskVerificationVerdict> verificationsFor(UUID taskPackageId) {
        return mapper.listVerificationJson(taskPackageId).stream()
                .map(payload -> readJson(payload, TaskVerificationVerdict.class)).toList();
    }

    @Override
    public void recordResponseAssessment(UUID attemptId, ResponseAssessment assessment) {
        mapper.insertAssessment(UUID.randomUUID(), attemptId, writeJson(assessment), clock.instant());
    }

    @Override
    public List<ResponseAssessment> assessmentsFor(UUID attemptId) {
        return mapper.listAssessmentJson(attemptId).stream()
                .map(payload -> readJson(payload, ResponseAssessment.class)).toList();
    }

    @Override
    public void recordTeachBackAssessment(UUID attemptId, TeachBackAssessment assessment) {
        mapper.insertTeachBackAssessment(
                UUID.randomUUID(), attemptId, writeJson(assessment), clock.instant());
    }

    @Override
    public List<TeachBackAssessment> teachBackAssessmentsFor(UUID attemptId) {
        return mapper.listTeachBackAssessmentJson(attemptId).stream()
                .map(payload -> readJson(payload, TeachBackAssessment.class)).toList();
    }

    @Override
    public void saveSource(SourceArtifact source) {
        mapper.insertSource(source.sourcePackId(), source.version(), writeJson(source.passages()), clock.instant());
    }

    @Override
    public Optional<SourceArtifact> findSource(String sourcePackId) {
        return mapper.findSource(sourcePackId).map(row -> new SourceArtifact(
                row.sourcePackId(), row.version(), readJson(row.passagesJson(),
                        new TypeReference<List<cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext.SourcePassage>>() {
                        })));
    }

    private String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize apply flow payload", exception);
        }
    }

    private <T> T readJson(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to deserialize apply flow payload", exception);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to deserialize apply flow payload", exception);
        }
    }
}
