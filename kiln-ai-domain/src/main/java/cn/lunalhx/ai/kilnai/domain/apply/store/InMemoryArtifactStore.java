package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;

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
    private final Map<UUID, List<TaskVerificationVerdict>> verifications = new HashMap<>();
    private final Map<UUID, List<ResponseAssessment>> assessments = new HashMap<>();
    private final Map<String, SourceArtifact> sources = new HashMap<>();
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
}
