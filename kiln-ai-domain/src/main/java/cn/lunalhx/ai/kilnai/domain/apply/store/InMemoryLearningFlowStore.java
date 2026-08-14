package cn.lunalhx.ai.kilnai.domain.apply.store;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class InMemoryLearningFlowStore implements LearningFlowStore {

    private final Map<UUID, FlowRecord> flows = new HashMap<>();
    private final Map<UUID, List<ApplyFlowInteraction>> interactions = new HashMap<>();
    private final Map<UUID, List<ApplyCheckpoint>> checkpoints = new HashMap<>();
    private final Map<UUID, Set<String>> taskFingerprints = new HashMap<>();
    private final Map<UUID, Set<String>> solutionFingerprints = new HashMap<>();
    private final Map<UUID, AcceptedLearningEvidence> evidence = new HashMap<>();
    private final Map<UUID, ProcessedCommand> commands = new LinkedHashMap<>();

    @Override
    public synchronized void insertFlow(FlowRecord flow) {
        Objects.requireNonNull(flow, "flow must not be null");
        flows.put(flow.flowId(), flow);
    }

    @Override
    public synchronized Optional<FlowRecord> findFlow(UUID flowId) {
        return Optional.ofNullable(flows.get(flowId));
    }

    @Override
    public synchronized void commitBoundary(
            ApplyFlowInteraction interaction,
            ApplyCheckpoint checkpoint,
            ProcessedCommand command
    ) {
        Objects.requireNonNull(interaction, "interaction must not be null");
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        Objects.requireNonNull(command, "command must not be null");
        List<ApplyFlowInteraction> history = interactions.computeIfAbsent(
                interaction.flowId(), key -> new ArrayList<>());
        if (history.stream().anyMatch(item -> item.interactionVersion() == interaction.interactionVersion())) {
            return;
        }
        history.add(interaction);
        checkpoints.computeIfAbsent(checkpoint.flowId(), key -> new ArrayList<>()).add(checkpoint);
        commands.putIfAbsent(command.idempotencyKey(), command);
    }

    @Override
    public synchronized Optional<ApplyFlowInteraction> latestInteraction(UUID flowId) {
        List<ApplyFlowInteraction> history = interactions.get(flowId);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(history.get(history.size() - 1));
    }

    @Override
    public synchronized Optional<ApplyCheckpoint> latestCheckpoint(UUID flowId) {
        List<ApplyCheckpoint> history = checkpoints.get(flowId);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(history.get(history.size() - 1));
    }

    @Override
    public synchronized void recordTaskExposure(UUID flowId, TaskPackage taskPackage) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(taskPackage, "taskPackage must not be null");
        taskFingerprints.computeIfAbsent(flowId, key -> new LinkedHashSet<>())
                .add(taskPackage.privateAssessorProjection().taskFingerprint().value());
        solutionFingerprints.computeIfAbsent(flowId, key -> new LinkedHashSet<>())
                .add(taskPackage.privateAssessorProjection().solutionFingerprint().value());
    }

    @Override
    public synchronized List<String> exposedTaskFingerprints(UUID flowId) {
        return List.copyOf(taskFingerprints.getOrDefault(flowId, Set.of()));
    }

    @Override
    public synchronized List<String> exposedSolutionFingerprints(UUID flowId) {
        return List.copyOf(solutionFingerprints.getOrDefault(flowId, Set.of()));
    }

    @Override
    public synchronized void acceptEvidence(AcceptedLearningEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        this.evidence.putIfAbsent(evidence.taskAttemptId(), evidence);
    }

    @Override
    public synchronized boolean evidenceExists(UUID attemptId) {
        return evidence.containsKey(attemptId);
    }

    @Override
    public synchronized List<AcceptedLearningEvidence> allEvidence() {
        return List.copyOf(evidence.values());
    }

    @Override
    public synchronized Optional<ProcessedCommand> findCommand(UUID idempotencyKey) {
        return Optional.ofNullable(commands.get(idempotencyKey));
    }
}
