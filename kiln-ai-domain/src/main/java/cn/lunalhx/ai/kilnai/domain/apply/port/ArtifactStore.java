package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The typed store for large and private Apply execution artifacts: curated
 * sources, Task Packages, Task Attempts with their submissions, isolated
 * Task Verification verdicts, and isolated Response Assessment records.
 *
 * <p>{@link #openAttempt(TaskPackage)} persists the Task Package and opens its
 * Task Attempt atomically, and {@link #closeAttempt(UUID, TaskSubmission)}
 * closes one open attempt and persists its single formal submission
 * atomically. A replay, duplicate, or stale close never produces a second
 * evaluation.
 */
public interface ArtifactStore {

    TaskAttempt openAttempt(TaskPackage taskPackage);

    Optional<TaskPackage> findPackage(UUID taskPackageId);

    List<TaskPackage> allPackages();

    Optional<TaskAttempt> findAttempt(UUID attemptId);

    /**
     * Atomically closes one open attempt with its single formal submission.
     * A replay, duplicate, or stale close never produces a second evaluation.
     */
    AttemptCloseOutcome closeAttempt(UUID attemptId, TaskSubmission submission);

    void recordTaskVerification(UUID taskPackageId, TaskVerificationVerdict verdict);

    List<TaskVerificationVerdict> verificationsFor(UUID taskPackageId);

    void recordResponseAssessment(UUID attemptId, ResponseAssessment assessment);

    List<ResponseAssessment> assessmentsFor(UUID attemptId);

    void saveSource(SourceArtifact source);

    Optional<SourceArtifact> findSource(String sourcePackId);

    /**
     * Persists one durable Explain teaching artifact bound to its Flow. The
     * artifact's learner projection is the only learner-visible content; its
     * source trace, example Fingerprint, and execution trace stay private and
     * are supplied to later nodes only under an explicit Node Context View
     * policy.
     */
    void saveExplainArtifact(UUID flowId, ExplainTeachingArtifact artifact);

    Optional<ExplainTeachingArtifact> findExplainArtifact(UUID artifactId);
}
