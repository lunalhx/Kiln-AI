package cn.lunalhx.ai.kilnai.graph.saa;

import cn.lunalhx.ai.kilnai.application.kernel.PendingCommitBuffer;
import cn.lunalhx.ai.kilnai.application.port.LearningCheckpointRecord;
import cn.lunalhx.ai.kilnai.application.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ApplicationCheckpointSaver implements BaseCheckpointSaver {

    private final SpikeStorePort store;
    private final PendingCommitBuffer buffer;
    private final LearningBlackboardMapper mapper;
    private final Clock clock;

    public ApplicationCheckpointSaver(
            SpikeStorePort store,
            PendingCommitBuffer buffer,
            LearningBlackboardMapper mapper,
            Clock clock
    ) {
        this.store = store;
        this.buffer = buffer;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        return load(config).stream().map(this::toFramework).toList();
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        List<LearningCheckpointRecord> records = load(config);
        if (config.checkPointId().isPresent()) {
            String id = config.checkPointId().get().toString();
            return records.stream()
                    .filter(record -> record.id().toString().equals(id))
                    .findFirst()
                    .map(this::toFramework);
        }
        return records.isEmpty() ? Optional.empty() : Optional.of(toFramework(records.get(0)));
    }

    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) {
        UUID flowId = flowId(checkpoint);
        LearningCheckpointRecord record = new LearningCheckpointRecord(
                UUID.fromString(checkpoint.getId()),
                flowId,
                config.threadId().orElse(flowId.toString()),
                checkpoint.getNodeId(),
                checkpoint.getNextNodeId(),
                LearningBlackboard.SCHEMA_VERSION,
                mapper.fromMap(blackboardMap(checkpoint.getState())),
                clock.instant()
        );
        store.commit(record, buffer.poll(flowId));
        return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
    }

    @Override
    public Tag release(RunnableConfig config) {
        String threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
        return new Tag(threadId, list(config));
    }

    private List<LearningCheckpointRecord> load(RunnableConfig config) {
        UUID flowId = config.threadId().map(UUID::fromString).orElse(null);
        if (flowId == null) {
            return List.of();
        }
        return new ArrayList<>(store.list(flowId));
    }

    private Checkpoint toFramework(LearningCheckpointRecord record) {
        return Checkpoint.builder()
                .id(record.id().toString())
                .state(mapper.toFramework(record.blackboard(), record.nextNodeId()))
                .nodeId(record.nodeId())
                .nextNodeId(record.nextNodeId())
                .build();
    }

    private UUID flowId(Checkpoint checkpoint) {
        Map<String, Object> blackboard = blackboardMap(checkpoint.getState());
        return UUID.fromString(String.valueOf(blackboard.get("flowId")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> blackboardMap(Map<String, Object> state) {
        Object raw = state.get(LearningBlackboardMapper.BLACKBOARD_KEY);
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalStateException("checkpoint is missing application blackboard");
    }
}
