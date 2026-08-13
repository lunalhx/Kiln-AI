package cn.lunalhx.ai.kilnai.domain.learning.adapter.port;

import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.model.FrozenModelProfile;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.CommitEffects;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SpikeStorePort extends CheckpointCommitPort {

    void insertFlow(FlowRecord flow);

    Optional<FlowRecord> findFlow(UUID flowId);

    Optional<LearnerVisibleInteraction> latestInteraction(UUID flowId);

    Optional<PublicTraceView> publicTrace(UUID flowId);

    Optional<Map<String, Object>> privateTrace(UUID flowId);

    Optional<Map<String, Object>> artifact(UUID artifactId);

    Optional<ProcessedCommand> findCommand(UUID idempotencyKey);

    void saveCommand(ProcessedCommand command);

    boolean evidenceExists(UUID attemptId);

    record FlowRecord(
            UUID id,
            UUID learnerId,
            UUID conceptId,
            UUID contractId,
            UUID rubricId,
            UUID sourcePackId,
            FlowStatus status,
            LearningStage stage,
            Instant createdAt,
            FrozenModelProfile frozenProfile
    ) {
    }

    record ProcessedCommand(
            UUID idempotencyKey,
            String requestHash,
            UUID flowId,
            int statusCode,
            LearnerVisibleInteraction response,
            Instant createdAt
    ) {
    }
}
