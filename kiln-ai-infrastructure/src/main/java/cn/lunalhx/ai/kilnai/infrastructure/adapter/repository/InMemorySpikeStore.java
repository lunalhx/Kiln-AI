package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.CommitEffects;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommandHolder;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningCheckpointRecord;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySpikeStore implements SpikeStorePort {

    private final ConcurrentHashMap<UUID, FlowRecord> flows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<LearningCheckpointRecord>> checkpoints = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, LearnerVisibleInteraction> interactions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PublicTraceView> publicTraces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Map<String, Object>> privateTraces = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Map<String, Object>> artifacts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ProcessedCommand> commands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AcceptedLearningEvidence> evidence = new ConcurrentHashMap<>();
    private final PendingCommandHolder pendingCommands;

    public InMemorySpikeStore() {
        this(new PendingCommandHolder());
    }

    public InMemorySpikeStore(PendingCommandHolder pendingCommands) {
        this.pendingCommands = pendingCommands;
    }

    @Override
    public void insertFlow(FlowRecord flow) {
        flows.put(flow.id(), flow);
    }

    @Override
    public Optional<FlowRecord> findFlow(UUID flowId) {
        return Optional.ofNullable(flows.get(flowId));
    }

    @Override
    public void commit(LearningCheckpointRecord checkpoint, CommitEffects effects) {
        checkpoints.computeIfAbsent(checkpoint.flowId(), key -> new ArrayList<>()).add(0, checkpoint);
        if (effects == null) {
            return;
        }
        interactions.put(checkpoint.flowId(), new LearnerVisibleInteraction(
                checkpoint.flowId(), effects.status(), effects.stage(), effects.interactionVersion(),
                effects.visibleContent(), effects.allowedEventKinds()
        ));
        if (effects.taskPackageId() != null && effects.taskPackagePayload() != null) {
            artifacts.put(effects.taskPackageId(), Map.copyOf(effects.taskPackagePayload()));
        }
        if (effects.evidence() != null) {
            evidence.putIfAbsent(effects.evidence().taskAttemptId(), effects.evidence());
        }
        if (effects.publicTrace() != null) {
            PublicTraceView previous = publicTraces.get(checkpoint.flowId());
            List<String> routes = new ArrayList<>(previous == null ? List.of() : previous.routes());
            routes.add(String.valueOf(effects.publicTrace().getOrDefault("route", "")));
            List<String> skills = new ArrayList<>(previous == null ? List.of() : previous.selectedSkills());
            skills.addAll(castList(effects.publicTrace().get("skills")));
            List<String> checkpointIds = new ArrayList<>(previous == null ? List.of() : previous.checkpoints());
            checkpointIds.add(checkpoint.id().toString());
            List<String> validations = new ArrayList<>(previous == null ? List.of() : previous.validations());
            validations.add(String.valueOf(effects.publicTrace().getOrDefault("validation", "")));
            List<String> models = new ArrayList<>(previous == null ? List.of() : previous.models());
            models.addAll(castList(effects.publicTrace().get("models")));
            List<String> usage = new ArrayList<>(previous == null ? List.of() : previous.usage());
            usage.addAll(castList(effects.publicTrace().get("usage")));
            publicTraces.put(checkpoint.flowId(), new PublicTraceView(
                    checkpoint.flowId(),
                    List.copyOf(routes),
                    List.copyOf(skills),
                    List.copyOf(checkpointIds),
                    String.valueOf(effects.publicTrace().getOrDefault("budget", "")),
                    List.copyOf(validations),
                    previous == null ? List.of() : previous.retries(),
                    List.copyOf(models),
                    List.copyOf(usage)
            ));
        }
        if (effects.privateTrace() != null) {
            privateTraces.put(checkpoint.flowId(), Map.copyOf(effects.privateTrace()));
        }
        persistPendingCommand(checkpoint.flowId(), interactions.get(checkpoint.flowId()));
    }

    private void persistPendingCommand(UUID flowId, LearnerVisibleInteraction interaction) {
        pendingCommands.poll(flowId).ifPresent(seed -> commands.put(seed.idempotencyKey(), new ProcessedCommand(
                seed.idempotencyKey(), seed.requestHash(), flowId, 200, interaction, java.time.Instant.now()
        )));
    }

    @Override
    public Optional<LearningCheckpointRecord> latest(UUID flowId) {
        return list(flowId).stream().findFirst();
    }

    @Override
    public List<LearningCheckpointRecord> list(UUID flowId) {
        return List.copyOf(checkpoints.getOrDefault(flowId, List.of()));
    }

    @Override
    public Optional<LearnerVisibleInteraction> latestInteraction(UUID flowId) {
        return Optional.ofNullable(interactions.get(flowId));
    }

    @Override
    public Optional<PublicTraceView> publicTrace(UUID flowId) {
        return Optional.ofNullable(publicTraces.get(flowId));
    }

    @Override
    public Optional<Map<String, Object>> privateTrace(UUID flowId) {
        return Optional.ofNullable(privateTraces.get(flowId));
    }

    @Override
    public Optional<Map<String, Object>> artifact(UUID artifactId) {
        return Optional.ofNullable(artifacts.get(artifactId));
    }

    @Override
    public Optional<ProcessedCommand> findCommand(UUID idempotencyKey) {
        return Optional.ofNullable(commands.get(idempotencyKey));
    }

    @Override
    public void saveCommand(ProcessedCommand command) {
        commands.put(command.idempotencyKey(), command);
    }

    @Override
    public boolean evidenceExists(UUID attemptId) {
        return evidence.containsKey(attemptId);
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
