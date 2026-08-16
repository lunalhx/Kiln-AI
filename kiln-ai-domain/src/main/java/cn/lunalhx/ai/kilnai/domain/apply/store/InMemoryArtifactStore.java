package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceTraceEntry;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExposureOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLevel;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintRequestRecord;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryArtifactStore implements ArtifactStore {

    private final Map<UUID, TaskPackage> packages = new HashMap<>();
    private final Map<UUID, TaskAttempt> attempts = new HashMap<>();
    private final Map<UUID, HintLadder> ladders = new HashMap<>();
    private final Map<UUID, List<HintRequestRecord>> hintRequests = new HashMap<>();
    private final Map<UUID, List<TaskVerificationVerdict>> verifications = new HashMap<>();
    private final Map<UUID, List<ResponseAssessment>> assessments = new HashMap<>();
    private final Map<String, SourceArtifact> sources = new HashMap<>();
    private final Map<UUID, ExplainTeachingArtifact> explainArtifacts = new HashMap<>();
    private final Clock clock;

    public InMemoryArtifactStore(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public synchronized TaskAttempt openAttempt(TaskPackage taskPackage) {
        Objects.requireNonNull(taskPackage, "taskPackage must not be null");
        if (packages.containsKey(taskPackage.taskPackageId())) {
            throw new IllegalStateException("task package already persisted: " + taskPackage.taskPackageId());
        }
        TaskAttempt attempt = TaskAttempt.open(taskPackage, clock.instant());
        packages.put(taskPackage.taskPackageId(), taskPackage);
        attempts.put(attempt.attemptId(), attempt);
        return attempt;
    }

    @Override
    public synchronized Optional<TaskPackage> findPackage(UUID taskPackageId) {
        return Optional.ofNullable(packages.get(taskPackageId));
    }

    @Override
    public synchronized List<TaskPackage> allPackages() {
        return List.copyOf(new ArrayList<>(packages.values()));
    }

    @Override
    public synchronized Optional<TaskAttempt> findAttempt(UUID attemptId) {
        return Optional.ofNullable(attempts.get(attemptId));
    }

    @Override
    public synchronized AttemptCloseOutcome closeAttempt(UUID attemptId, TaskSubmission submission) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(submission, "submission must not be null");
        TaskAttempt current = attempts.get(attemptId);
        if (current == null) {
            return new AttemptCloseOutcome(AttemptCloseOutcome.Result.NOT_FOUND, null);
        }
        AttemptCloseOutcome outcome = current.close(submission, clock.instant());
        if (outcome.result() == AttemptCloseOutcome.Result.CLOSED) {
            attempts.put(attemptId, outcome.attempt());
        }
        return outcome;
    }

    @Override
    public synchronized Optional<HintLadder> findLadder(UUID attemptId) {
        return Optional.ofNullable(ladders.get(attemptId));
    }

    @Override
    public synchronized HintExposureOutcome exposeHint(
            UUID attemptId,
            HintLadder ladder,
            int requestedLevel,
            UUID commandKey
    ) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(ladder, "ladder must not be null");
        Objects.requireNonNull(commandKey, "commandKey must not be null");
        TaskAttempt current = attempts.get(attemptId);
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
        HintRequestRecord request = new HintRequestRecord(
                attemptId, commandKey, requestedLevel, requestedLevel, clock.instant());
        TaskAttempt extended = current.appendAssistance(
                new AssistanceTraceEntry(HintLevel.of(requestedLevel), clock.instant()));
        AttemptCloseOutcome closed = requestedLevel == 5
                ? extended.closeAsSolutionRevealed(clock.instant())
                : new AttemptCloseOutcome(AttemptCloseOutcome.Result.CLOSED, extended);
        if (closed.result() != AttemptCloseOutcome.Result.CLOSED) {
            return new HintExposureOutcome.NotOpen(current);
        }
        ladders.putIfAbsent(attemptId, ladder);
        attempts.put(attemptId, closed.attempt());
        hintRequests.computeIfAbsent(attemptId, key -> new ArrayList<>()).add(request);
        return new HintExposureOutcome.Exposed(closed.attempt(), request);
    }

    @Override
    public synchronized Optional<HintRequestRecord> findHintRequest(UUID attemptId, UUID commandKey) {
        return hintRequests.getOrDefault(attemptId, List.of()).stream()
                .filter(request -> request.commandKey().equals(commandKey))
                .findFirst();
    }

    @Override
    public synchronized void recordTaskVerification(UUID taskPackageId, TaskVerificationVerdict verdict) {
        verifications.computeIfAbsent(taskPackageId, key -> new ArrayList<>()).add(verdict);
    }

    @Override
    public synchronized List<TaskVerificationVerdict> verificationsFor(UUID taskPackageId) {
        return List.copyOf(verifications.getOrDefault(taskPackageId, List.of()));
    }

    @Override
    public synchronized void recordResponseAssessment(UUID attemptId, ResponseAssessment assessment) {
        assessments.computeIfAbsent(attemptId, key -> new ArrayList<>()).add(assessment);
    }

    @Override
    public synchronized List<ResponseAssessment> assessmentsFor(UUID attemptId) {
        return List.copyOf(assessments.getOrDefault(attemptId, List.of()));
    }

    @Override
    public synchronized void saveSource(SourceArtifact source) {
        sources.put(source.sourcePackId(), source);
    }

    @Override
    public synchronized Optional<SourceArtifact> findSource(String sourcePackId) {
        return Optional.ofNullable(sources.get(sourcePackId));
    }

    @Override
    public synchronized void saveExplainArtifact(UUID flowId, ExplainTeachingArtifact artifact) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(artifact, "artifact must not be null");
        if (explainArtifacts.containsKey(artifact.artifactId())) {
            throw new IllegalStateException("explain artifact already persisted: " + artifact.artifactId());
        }
        explainArtifacts.put(artifact.artifactId(), artifact);
    }

    @Override
    public synchronized Optional<ExplainTeachingArtifact> findExplainArtifact(UUID artifactId) {
        return Optional.ofNullable(explainArtifacts.get(artifactId));
    }
}
