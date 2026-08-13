package cn.lunalhx.ai.kilnai.application.port;

import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;

import java.time.Instant;
import java.util.UUID;

public record LearningCheckpointRecord(
        UUID id,
        UUID flowId,
        String threadId,
        String nodeId,
        String nextNodeId,
        int schemaVersion,
        LearningBlackboard blackboard,
        Instant createdAt
) {
}
