package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The typed store of one Apply Learning Flow's durable state: the Flow
 * record, every learner-visible interaction and its checkpoint, the Exposure
 * Ledger of displayed task and solution Fingerprints, accepted Learning
 * Evidence, and the idempotency ledger of processed commands.
 *
 * <p>{@link #commitBoundary(ApplyFlowInteraction, ApplyCheckpoint, ProcessedCommand)}
 * persists the learner interaction, its checkpoint, and the processed command
 * atomically, so a replayed idempotency key always returns the original
 * result and a restart can resume the exact boundary.
 */
public interface LearningFlowStore {

    void insertFlow(FlowRecord flow);

    Optional<FlowRecord> findFlow(UUID flowId);

    /**
     * Atomically persists one Learner Interaction Boundary: the learner-visible
     * interaction, its checkpoint, and the processed command that produced it.
     * Repeating a boundary for the same interaction version is a no-op, so a
     * concurrent duplicate commit cannot corrupt state.
     */
    void commitBoundary(ApplyFlowInteraction interaction, ApplyCheckpoint checkpoint, ProcessedCommand command);

    Optional<ApplyFlowInteraction> latestInteraction(UUID flowId);

    Optional<ApplyCheckpoint> latestCheckpoint(UUID flowId);

    void recordTaskExposure(UUID flowId, TaskPackage taskPackage);

    List<String> exposedTaskFingerprints(UUID flowId);

    List<String> exposedSolutionFingerprints(UUID flowId);

    /**
     * Records one exposed Explain worked-example Fingerprint in the Flow's
     * exposure ledger so later teaching content can be checked for novelty.
     */
    void recordExampleExposure(UUID flowId, String exampleFingerprint);

    List<String> exposedExampleFingerprints(UUID flowId);

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

    Optional<ProcessedCommand> findCommand(UUID idempotencyKey);

    record FlowRecord(
            UUID flowId,
            UUID learnerId,
            UUID conceptId,
            FlowStatus status,
            LearningStage stage,
            Instant createdAt
    ) {

        public FlowRecord {
            Objects.requireNonNull(flowId, "flowId must not be null");
            Objects.requireNonNull(learnerId, "learnerId must not be null");
            Objects.requireNonNull(conceptId, "conceptId must not be null");
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(stage, "stage must not be null");
            Objects.requireNonNull(createdAt, "createdAt must not be null");
        }
    }

    record ProcessedCommand(
            UUID idempotencyKey,
            String requestHash,
            UUID flowId,
            ApplyFlowInteraction response,
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
}
