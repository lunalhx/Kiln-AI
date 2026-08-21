package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.LearningCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.PendingOperation;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticFinding;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlan;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticProgress;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The typed store of one Learning Flow's durable state: the Flow
 * record, every learner-visible interaction and its checkpoint, the Exposure
 * Ledger of displayed task and solution Fingerprints, accepted Learning
 * Evidence, and the idempotency ledger of processed commands.
 *
 * <p>{@link #commitBoundary(LearningFlowInteraction, LearningCheckpoint, ProcessedCommand)}
 * persists the learner interaction, its checkpoint, and the processed command
 * atomically, so a replayed idempotency key always returns the original
 * result and a restart can resume the exact boundary.
 */
public interface LearningFlowStore {

    void insertFlow(FlowRecord flow);

    Optional<FlowRecord> findFlow(UUID flowId);

    /**
     * The immutable Diagnostic Plan snapshot frozen onto one started Flow.
     * Empty is expected for legacy or non-Diagnostic Flow records.
     */
    Optional<DiagnosticPlan> diagnosticPlan(UUID flowId);

    /**
     * Learner-safe completed/max projection for one Flow's Diagnostic Plan.
     * The store never returns the Plan's source, readiness, or assessment
     * details through this projection.
     */
    Optional<DiagnosticProgress> diagnosticProgress(UUID flowId);

    /**
     * Flow-scoped Diagnostic Findings in commit order. Empty when none have
     * been recorded. Findings are never Learning Evidence.
     */
    List<DiagnosticFinding> diagnosticFindings(UUID flowId);

    /**
     * Records one Finding for a closed Diagnostic Attempt. The same Attempt
     * is idempotent: a replay or recovered submission never duplicates it.
     */
    void recordDiagnosticFinding(DiagnosticFinding finding);

    /**
     * The existing Active Learning Work claim of one learner and Target
     * Concept (ADR-0070): the Flow id of a non-terminal Flow, or the Flow id
     * of an unfinished Review Task (SCHEDULED, DUE, or STARTED) belonging to
     * its terminal Flow. Empty when no claim exists and a new Diagnostic may
     * start.
     */
    Optional<UUID> activeWorkFlowId(UUID learnerId, UUID conceptId);

    /**
     * Atomically binds one fully-prepared Start: the Flow record, its Source
     * Pack, the Diagnostic Task Package with its open Attempt, the exposure,
     * the first learner interaction, its checkpoint, and the processed
     * command commit in one transaction — and only when no Active Learning
     * Work already exists for the learner and Concept. A losing race throws
     * {@link cn.lunalhx.ai.kilnai.types.error.ActiveWorkConflictException}
     * carrying the existing Flow id, and nothing at all is written.
     */
    LearningFlowInteraction bindStart(StartBind bind);

    /**
     * Atomically persists one Learner Interaction Boundary: the learner-visible
     * interaction, its checkpoint, and the processed command that produced it.
     * Repeating a boundary for the same interaction version is a no-op, so a
     * concurrent duplicate commit cannot corrupt state; the committed
     * interaction is returned so a racing caller projects durable state rather
     * than its local candidate. A null pending operation clears any saved
     * Pending Operation for the Flow; a non-null value replaces it in the same
     * commit.
     */
    LearningFlowInteraction commitBoundary(
            LearningFlowInteraction interaction,
            LearningCheckpoint checkpoint,
            ProcessedCommand command,
            PendingOperation pending,
            DiagnosticFinding diagnosticFinding);

    /**
     * Commits a boundary and clears any Pending Operation. Every successful
     * next interaction and explicit leave uses this form so a recovered
     * retry cannot resume a completed operation.
     */
    default LearningFlowInteraction commitBoundary(
            LearningFlowInteraction interaction,
            LearningCheckpoint checkpoint,
            ProcessedCommand command
    ) {
        return commitBoundary(interaction, checkpoint, command, null, null);
    }

    default LearningFlowInteraction commitBoundary(
            LearningFlowInteraction interaction,
            LearningCheckpoint checkpoint,
            ProcessedCommand command,
            PendingOperation pending
    ) {
        return commitBoundary(interaction, checkpoint, command, pending, null);
    }

    /**
     * The saved Pending Operation of one Flow's current Unavailable
     * Interaction, if any. Empty after a successful next interaction or an
     * explicit leave.
     */
    Optional<PendingOperation> pendingOperation(UUID flowId);

    Optional<LearningFlowInteraction> latestInteraction(UUID flowId);

    Optional<LearningCheckpoint> latestCheckpoint(UUID flowId);

    void recordTaskExposure(UUID flowId, TaskPackage taskPackage);

    List<String> exposedTaskFingerprints(UUID flowId);

    List<String> exposedSolutionFingerprints(UUID flowId);

    /**
     * The Task Package ids the Flow has exposed to its learner, from the
     * exposure ledger. The Graph uses them to scope the open Apply Practice
     * Attempt lookup to the current Flow, so a Continue can never resume an
     * Attempt from another Flow.
     */
    List<UUID> exposedTaskPackageIds(UUID flowId);

    /**
     * Records one exposed Explain worked-example Fingerprint in the Flow's
     * exposure ledger so later teaching content can be checked for novelty.
     */
    void recordExampleExposure(UUID flowId, String exampleFingerprint);

    List<String> exposedExampleFingerprints(UUID flowId);

    /**
     * Records the deterministic content fingerprint of one generated Hint
     * Ladder in the Flow's exposure ledger (idempotent per fingerprint) so
     * later task and example generation never reuses exposed hint content.
     */
    void recordHintLadderExposure(UUID flowId, String ladderFingerprint);

    List<String> exposedHintLadderFingerprints(UUID flowId);

    /**
     * Records the deterministic content fingerprint of one H5 revealed
     * solution in the Flow's exposure ledger (idempotent per fingerprint) so
     * later task and example generation never reuses the revealed answer.
     */
    void recordRevealedSolutionExposure(UUID flowId, String revealFingerprint);

    List<String> exposedRevealedSolutionFingerprints(UUID flowId);

    /**
     * The complete novelty-exclusion snapshot of one Flow's exposure ledger —
     * task, solution, example, hint-ladder, and revealed-solution fingerprints
     * — assembled in one place so every generation flow supplies the same
     * closed exclusion set to its execution context.
     */
    default ApplyExecutionContext.NoveltyExclusions noveltyExclusions(UUID flowId) {
        return new ApplyExecutionContext.NoveltyExclusions(
                exposedTaskFingerprints(flowId),
                exposedSolutionFingerprints(flowId),
                exposedExampleFingerprints(flowId),
                exposedHintLadderFingerprints(flowId),
                exposedRevealedSolutionFingerprints(flowId));
    }

    /**
     * Records one eligible Teach-back anchor (an exposed Explain worked
     * example or an H5 solution reveal) in the Flow's anchor ledger. The same
     * anchor id is idempotent, so a crashed command that re-records its own
     * anchor never duplicates it. Teach-back is legal only while the Flow
     * carries such an anchor.
     */
    void recordAnchor(UUID flowId, TeachBackAnchor anchor);

    Optional<TeachBackAnchor> latestAnchor(UUID flowId);

    boolean evidenceExists(UUID attemptId);

    List<AcceptedLearningEvidence> allEvidence();

    /**
     * Atomically accepts exactly one item of Learning Evidence per Task
     * Attempt: an attempt that already has Evidence makes this a no-op
     * returning false, so a replay or recovered submission can never stack a
     * second record. Practice uses this plain acceptance because it must never
     * touch the Review cadence; the Independent and Review acceptances own
     * their cadence transitions separately.
     */
    boolean acceptEvidence(AcceptedLearningEvidence evidence);

    /**
     * The cycle-aware readiness fact of one Flow's current remediation cycle:
     * at least one conclusive Apply Practice pass accepted after the latest
     * triggering failure (an accepted no-hint Independent failure starts a
     * new cycle). A pass from an earlier cycle never re-qualifies the
     * learner, so a fresh Independent Test cannot be reopened on stale
     * evidence. The Workflow Guard and the Practice flow's Feedback Facts
     * derive readiness from this single rule.
     */
    default boolean qualifyingPracticePassExists(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        List<AcceptedLearningEvidence> flowEvidence = allEvidence().stream()
                .filter(evidence -> evidence.flowId().equals(flowId))
                .toList();
        Optional<Instant> latestFail = flowEvidence.stream()
                .filter(AcceptedLearningEvidence::isIndependentFailure)
                .map(AcceptedLearningEvidence::acceptedAt)
                .max(Instant::compareTo);
        return flowEvidence.stream().anyMatch(evidence ->
                evidence.attemptPurpose() == AttemptPurpose.PRACTICE
                        && evidence.result() == LearningResult.PASS
                        && (latestFail.isEmpty() || evidence.acceptedAt().isAfter(latestFail.get())));
    }

    Optional<ProcessedCommand> findCommand(UUID idempotencyKey);

    record FlowRecord(
            UUID flowId,
            UUID learnerId,
            UUID conceptId,
            FlowStatus status,
            LearningStage stage,
            ModelProfile modelProfile,
            Instant createdAt
    ) {

        public FlowRecord {
            Objects.requireNonNull(flowId, "flowId must not be null");
            Objects.requireNonNull(learnerId, "learnerId must not be null");
            Objects.requireNonNull(conceptId, "conceptId must not be null");
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(stage, "stage must not be null");
            Objects.requireNonNull(modelProfile, "modelProfile must not be null");
            Objects.requireNonNull(createdAt, "createdAt must not be null");
        }
    }

    record ProcessedCommand(
            UUID idempotencyKey,
            String requestHash,
            UUID flowId,
            LearningFlowInteraction response,
            Instant createdAt
    ) {

        public ProcessedCommand {
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(requestHash, "requestHash must not be null");
            Objects.requireNonNull(flowId, "flowId must not be null");
            Objects.requireNonNull(response, "response must not be null");
            Objects.requireNonNull(createdAt, "createdAt must not be null");
        }
    }

    /**
     * The complete domain-owned specification of one atomic Start. The store
     * builds the Flow record, the open Attempt, the first learner
     * interaction, its checkpoint, and the processed command from these
     * fields, so the whole binding is one atomic write (ADR-0063): the claim,
     * the Source Pack, the Package, the Attempt, the exposure, the
     * checkpoint, the interaction, and the processed command either all
     * commit or none do.
     */
    record StartBind(
            UUID flowId,
            UUID learnerId,
            UUID conceptId,
            ModelProfile modelProfile,
            DiagnosticPlan diagnosticPlan,
            SourceArtifact source,
            TaskPackage taskPackage,
            TaskVerificationVerdict verificationVerdict,
            UUID idempotencyKey,
            String requestHash
    ) {

        public StartBind {
            Objects.requireNonNull(flowId, "flowId must not be null");
            Objects.requireNonNull(learnerId, "learnerId must not be null");
            Objects.requireNonNull(conceptId, "conceptId must not be null");
            Objects.requireNonNull(modelProfile, "modelProfile must not be null");
            Objects.requireNonNull(diagnosticPlan, "diagnosticPlan must not be null");
            Objects.requireNonNull(source, "source must not be null");
            Objects.requireNonNull(taskPackage, "taskPackage must not be null");
            Objects.requireNonNull(verificationVerdict, "verificationVerdict must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(requestHash, "requestHash must not be null");
        }
    }
}
