package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptConversionOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceTraceEntry;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExposureOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintRequestRecord;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The typed store for large and private Apply execution artifacts: curated
 * sources, Task Packages, Task Attempts with their submissions and Assistance
 * Traces, stable Hint Ladders, isolated Task Verification verdicts, and
 * isolated Response Assessment records.
 *
 * <p>{@link #openAttempt(TaskPackage)} persists the Task Package and opens its
 * Task Attempt atomically, and {@link #closeAttempt(UUID, TaskSubmission)}
 * closes one open attempt and persists its single formal submission
 * atomically. {@link #exposeHint} persists the validated ladder, appends the
 * exposed level to the attempt's Assistance Trace, records the hint request,
 * and closes the attempt as Solution Revealed for H5 — all atomically, so a
 * replay or duplicate never reveals a second level for one command. A
 * replay, duplicate, or stale close never produces a second evaluation.
 */
public interface ArtifactStore {

    TaskAttempt openAttempt(TaskPackage taskPackage);

    /**
     * Persists one validated Teach-back task package and opens its
     * Practice-purpose Attempt atomically, exactly like an Apply package.
     */
    TaskAttempt openAttempt(TeachBackTaskPackage taskPackage);

    Optional<TaskPackage> findPackage(UUID taskPackageId);

    Optional<TeachBackTaskPackage> findTeachBackPackage(UUID taskPackageId);

    List<TaskPackage> allPackages();

    Optional<TaskAttempt> findAttempt(UUID attemptId);

    /**
     * Finds the one open Apply Practice Attempt among the given exposed Task
     * Package ids, if any. The Graph passes the current Flow's exposed
     * package ids from the exposure ledger, so a Continue can only resume an
     * Attempt that belongs to the current Flow. The Guard uses it as the
     * committed-state fact that a temporary Explain was shown inside an open
     * Attempt: the only legal next move is returning to the same Practice
     * interaction, never opening a new task. Teach-back Attempts are
     * Practice-purpose but are excluded by their package type.
     */
    Optional<TaskAttempt> findOpenPracticeAttempt(List<UUID> taskPackageIds);

    /**
     * Atomically closes one open attempt with its single formal submission.
     * A replay, duplicate, or stale close never produces a second evaluation.
     */
    AttemptCloseOutcome closeAttempt(UUID attemptId, TaskSubmission submission);

    Optional<HintLadder> findLadder(UUID attemptId);

    /**
     * Atomically persists the stable validated ladder (when absent), appends
     * the exposed level to the attempt's Assistance Trace, records the
     * request, and — for the H5 reveal — closes the attempt as Solution
     * Revealed without Assessment or Evidence. An already-recorded request
     * for the same command key returns {@link HintExposureOutcome.AlreadyExposed}
     * so a crashed command resumes its original exposed level.
     */
    HintExposureOutcome exposeHint(UUID attemptId, HintLadder ladder, int requestedLevel, UUID commandKey);

    Optional<HintRequestRecord> findHintRequest(UUID attemptId, UUID commandKey);

    /**
     * Appends recorded assistance — a procedural or substantive clarification
     * or a temporary Explain shown inside the open Attempt — to the Attempt's
     * Assistance Trace. Only an OPEN attempt may be extended; a closed or
     * unknown attempt returns empty and nothing is written, so a stale
     * clarification can never touch a committed Attempt.
     */
    Optional<TaskAttempt> appendAssistance(UUID attemptId, List<AssistanceTraceEntry> entries);

    /**
     * Atomically converts one open Independent Test or Review Attempt to
     * Practice (one-way, before any assistance content is exposed) and
     * appends the recorded assistance to its Trace. An already-Practice open
     * attempt returns {@link AttemptConversionOutcome.AlreadyPractice}
     * without appending again, so a replayed conversion never duplicates its
     * trace; a closed or unknown attempt returns
     * {@link AttemptConversionOutcome.Ignored} and nothing is written.
     */
    AttemptConversionOutcome convertToPractice(UUID attemptId, List<AssistanceTraceEntry> entries);

    void recordTaskVerification(UUID taskPackageId, TaskVerificationVerdict verdict);

    List<TaskVerificationVerdict> verificationsFor(UUID taskPackageId);

    void recordResponseAssessment(UUID attemptId, ResponseAssessment assessment);

    List<ResponseAssessment> assessmentsFor(UUID attemptId);

    /**
     * Records one isolated Teach-back semantic Assessment as an audit record
     * of the closed attempt. Duplicate recordings are audit records, never
     * state.
     */
    void recordTeachBackAssessment(UUID attemptId, TeachBackAssessment assessment);

    List<TeachBackAssessment> teachBackAssessmentsFor(UUID attemptId);

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
