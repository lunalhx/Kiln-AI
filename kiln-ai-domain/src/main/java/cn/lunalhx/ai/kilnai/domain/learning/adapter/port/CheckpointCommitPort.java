package cn.lunalhx.ai.kilnai.application.port;

import cn.lunalhx.ai.kilnai.application.kernel.CommitEffects;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckpointCommitPort {

    void commit(LearningCheckpointRecord checkpoint, CommitEffects effects);

    Optional<LearningCheckpointRecord> latest(UUID flowId);

    List<LearningCheckpointRecord> list(UUID flowId);
}
