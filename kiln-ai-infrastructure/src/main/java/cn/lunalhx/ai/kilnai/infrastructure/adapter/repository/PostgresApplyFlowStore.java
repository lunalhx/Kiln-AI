package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
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
                flow.status().name(), flow.stage().name(), flow.createdAt());
    }

    @Override
    public Optional<FlowRecord> findFlow(UUID flowId) {
        return mapper.findFlow(flowId).map(row -> new FlowRecord(
                row.id(), row.learnerId(), row.conceptId(),
                FlowStatus.valueOf(row.status()), LearningStage.valueOf(row.stage()), row.createdAt()));
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
                checkpoint.createdAt()));
        mapper.insertCheckpoint(new ApplyFlowMapper.CheckpointRow(
                checkpoint.checkpointId(), checkpoint.flowId(), checkpoint.interactionVersion(),
                checkpoint.createdAt()));
        mapper.insertCommand(new ApplyFlowMapper.CommandRow(
                command.idempotencyKey(), command.requestHash(), command.flowId(),
                writeJson(command.response()), command.createdAt()));
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
                row.learnerMessage()));
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
    @Transactional
    public ReviewTask acceptEvidenceAndScheduleFirstReview(AcceptedLearningEvidence evidence, Instant dueAt) {
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
                1, ReviewTaskStatus.SCHEDULED, dueAt, clock.instant(), null, null, null);
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
                review.completedAt(),
                review.cancelledAt()));
        return review;
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
    @Transactional
    public Optional<ApplyFlowInteraction> bindStartedReview(ReviewTaskStore.ReviewStartBind bind) {
        int claimed = mapper.claimReviewStarted(bind.reviewId(), bind.startedAt());
        if (claimed == 0) {
            return Optional.empty();
        }
        TaskAttempt attempt = TaskAttempt.open(bind.taskPackage(), bind.startedAt());
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
                null));
        mapper.recordExposure(
                bind.flowId(),
                bind.taskPackage().taskPackageId(),
                bind.taskPackage().privateAssessorProjection().taskFingerprint().value(),
                bind.taskPackage().privateAssessorProjection().solutionFingerprint().value(),
                bind.startedAt());
        ApplyFlowInteraction interaction = new ApplyFlowInteraction(
                bind.flowId(), bind.interactionVersion(), FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.DELAYED_REVIEW, attempt.attemptId(), AttemptPurpose.REVIEW,
                bind.taskPackage().learnerProjection(), null);
        mapper.insertInteraction(new ApplyFlowMapper.InteractionRow(
                UUID.randomUUID(),
                interaction.flowId(),
                interaction.interactionVersion(),
                interaction.status().name(),
                interaction.stage().name(),
                interaction.attemptId(),
                interaction.attemptPurpose().name(),
                writeJson(interaction.learnerProjection()),
                interaction.learnerMessage(),
                bind.startedAt()));
        mapper.insertCheckpoint(new ApplyFlowMapper.CheckpointRow(
                UUID.randomUUID(), bind.flowId(), bind.interactionVersion(), bind.startedAt()));
        mapper.insertCommand(new ApplyFlowMapper.CommandRow(
                bind.idempotencyKey(), bind.requestHash(), bind.flowId(),
                writeJson(interaction), bind.startedAt()));
        return Optional.of(interaction);
    }

    private ReviewTask toReviewTask(ApplyFlowMapper.ReviewTaskRow row) {
        return new ReviewTask(
                row.id(), row.learnerId(), row.conceptId(), row.flowId(), row.reviewNumber(),
                ReviewTaskStatus.valueOf(row.status()), row.dueAt(), row.createdAt(),
                row.startedAt(), row.completedAt(), row.cancelledAt());
    }

    @Override
    public void acceptEvidence(AcceptedLearningEvidence evidence) {
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
    }

    @Override
    public boolean evidenceExists(UUID attemptId) {
        return mapper.evidenceExists(attemptId).isPresent();
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
                null));
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
                row.submissionJson() == null ? null : readJson(row.submissionJson(), TaskSubmission.class)));
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
