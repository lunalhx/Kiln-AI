package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.CommitEffects;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommandHolder;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningCheckpointRecord;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnBean(DataSource.class)
public class PostgresSpikeStore implements SpikeStorePort {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };

    private final SpikeStoreMapper mapper;
    private final ObjectMapper json;
    private final PendingCommandHolder pendingCommands;
    private final Clock clock;

    public PostgresSpikeStore(
            SpikeStoreMapper mapper,
            ObjectMapper json,
            PendingCommandHolder pendingCommands,
            Clock clock
    ) {
        this.mapper = mapper;
        this.json = json;
        this.pendingCommands = pendingCommands;
        this.clock = clock;
    }

    @Override
    public void insertFlow(FlowRecord flow) {
        mapper.insertFlow(
                flow.id(), flow.learnerId(), flow.conceptId(), flow.contractId(), flow.rubricId(),
                flow.sourcePackId(), flow.status().name(), flow.stage().name(), flow.createdAt()
        );
    }

    @Override
    public Optional<FlowRecord> findFlow(UUID flowId) {
        return mapper.findFlow(flowId).map(row -> new FlowRecord(
                row.id(), row.learnerId(), row.conceptId(), row.contractId(), row.rubricId(),
                row.sourcePackId(), FlowStatus.valueOf(row.status()), LearningStage.valueOf(row.stage()),
                row.createdAt()
        ));
    }

    @Override
    @Transactional
    public void commit(LearningCheckpointRecord checkpoint, CommitEffects effects) {
        Instant now = clock.instant();
        mapper.insertCheckpoint(new SpikeStoreMapper.CheckpointRow(
                checkpoint.id(),
                checkpoint.flowId(),
                checkpoint.threadId(),
                blankToEmpty(checkpoint.nodeId()),
                blankToEmpty(checkpoint.nextNodeId()),
                checkpoint.schemaVersion(),
                writeJson(checkpoint.blackboard()),
                checkpoint.createdAt()
        ));
        if (effects == null) {
            return;
        }
        mapper.updateFlow(checkpoint.flowId(), effects.status().name(), effects.stage().name(), now);
        LearnerVisibleInteraction interaction = new LearnerVisibleInteraction(
                checkpoint.flowId(), effects.status(), effects.stage(), effects.interactionVersion(),
                effects.visibleContent(), effects.allowedEventKinds()
        );
        mapper.insertInteraction(new SpikeStoreMapper.InteractionRow(
                UUID.randomUUID(),
                checkpoint.flowId(),
                effects.interactionVersion(),
                effects.status().name(),
                effects.stage().name(),
                effects.visibleContent(),
                writeJson(effects.allowedEventKinds().stream().map(Enum::name).toList()),
                now
        ));
        if (effects.taskPackageId() != null && effects.taskPackagePayload() != null) {
            String payload = writeJson(effects.taskPackagePayload());
            mapper.insertArtifact(
                    effects.taskPackageId(), "TASK_PACKAGE", 1, "private", sha256(payload), payload, now
            );
            if (effects.attemptId() != null) {
                mapper.insertAttempt(
                        effects.attemptId(), checkpoint.flowId(), effects.taskPackageId(),
                        "PRACTICE", "OPEN", now
                );
            }
        }
        if (effects.evidence() != null) {
            mapper.insertEvidence(new SpikeStoreMapper.EvidenceRow(
                    effects.evidence().id(),
                    effects.evidence().taskAttemptId(),
                    effects.evidence().flowId(),
                    effects.evidence().conceptId(),
                    effects.evidence().learnerId(),
                    effects.evidence().result().name(),
                    effects.evidence().attemptPurpose().name(),
                    effects.evidence().highestHintLevel(),
                    writeJson(effects.evidence().assistanceTrace()),
                    effects.evidence().acceptedAt()
            ));
        }
        if (effects.progress() != null) {
            mapper.upsertProgress(
                    effects.progress().learnerId(),
                    effects.progress().conceptId(),
                    effects.progress().currentMilestone().name(),
                    effects.progress().highestMilestoneReached().name(),
                    effects.progress().currentStage().name(),
                    effects.progress().updatedAt()
            );
        }
        mapper.insertTrace(
                UUID.randomUUID(),
                checkpoint.flowId(),
                writeJson(effects.privateTrace() == null ? Map.of() : effects.privateTrace()),
                writeJson(effects.publicTrace() == null ? Map.of() : effects.publicTrace()),
                now
        );
        persistPendingCommand(checkpoint.flowId(), interaction, now);
    }

    @Override
    public Optional<LearningCheckpointRecord> latest(UUID flowId) {
        return list(flowId).stream().findFirst();
    }

    @Override
    public List<LearningCheckpointRecord> list(UUID flowId) {
        return mapper.listCheckpoints(flowId).stream().map(row -> new LearningCheckpointRecord(
                row.id(),
                row.flowId(),
                row.threadId(),
                row.nodeId(),
                row.nextNodeId(),
                row.schemaVersion(),
                readJson(row.blackboardJson(), LearningBlackboard.class),
                row.createdAt()
        )).toList();
    }

    @Override
    public Optional<LearnerVisibleInteraction> latestInteraction(UUID flowId) {
        return mapper.latestInteraction(flowId).map(row -> new LearnerVisibleInteraction(
                row.flowId(),
                FlowStatus.valueOf(row.status()),
                LearningStage.valueOf(row.stage()),
                row.interactionVersion(),
                row.visibleContent(),
                readJson(row.allowedEventKindsJson(), STRINGS).stream().map(LearnerInputKind::valueOf).toList()
        ));
    }

    @Override
    public Optional<PublicTraceView> publicTrace(UUID flowId) {
        List<Map<String, Object>> payloads = mapper.listTraces(flowId).stream()
                .map(row -> readJson(row.publicJson(), MAP))
                .toList();
        if (payloads.isEmpty()) {
            return Optional.empty();
        }
        List<String> routes = new java.util.ArrayList<>();
        List<String> skills = new java.util.ArrayList<>();
        List<String> validations = new java.util.ArrayList<>();
        String budget = "";
        for (Map<String, Object> publicPayload : payloads) {
            routes.add(String.valueOf(publicPayload.getOrDefault("route", "")));
            skills.addAll(castList(publicPayload.get("skills")));
            validations.add(String.valueOf(publicPayload.getOrDefault("validation", "")));
            budget = String.valueOf(publicPayload.getOrDefault("budget", budget));
        }
        List<String> checkpointIds = list(flowId).stream()
                .map(checkpoint -> checkpoint.id().toString())
                .toList();
        return Optional.of(new PublicTraceView(flowId, routes, skills, checkpointIds, budget, validations, List.of()));
    }

    @Override
    public Optional<Map<String, Object>> privateTrace(UUID flowId) {
        return mapper.latestTrace(flowId).map(row -> readJson(row.privateJson(), MAP));
    }

    @Override
    public Optional<Map<String, Object>> artifact(UUID artifactId) {
        return mapper.findArtifactPayload(artifactId).map(payload -> readJson(payload, MAP));
    }

    @Override
    public Optional<ProcessedCommand> findCommand(UUID idempotencyKey) {
        return mapper.findCommand(idempotencyKey).map(row -> new ProcessedCommand(
                row.idempotencyKey(),
                row.requestHash(),
                row.flowId(),
                row.statusCode(),
                readJson(row.responseJson(), LearnerVisibleInteraction.class),
                row.createdAt()
        ));
    }

    @Override
    public void saveCommand(ProcessedCommand command) {
        mapper.insertCommand(new SpikeStoreMapper.CommandRow(
                command.idempotencyKey(),
                command.requestHash(),
                command.flowId(),
                command.statusCode(),
                writeJson(command.response()),
                command.createdAt()
        ));
    }

    @Override
    public boolean evidenceExists(UUID attemptId) {
        return mapper.evidenceExists(attemptId).isPresent();
    }

    private void persistPendingCommand(UUID flowId, LearnerVisibleInteraction interaction, Instant now) {
        pendingCommands.poll(flowId).ifPresent(seed -> mapper.insertCommand(new SpikeStoreMapper.CommandRow(
                seed.idempotencyKey(),
                seed.requestHash(),
                flowId,
                200,
                writeJson(interaction),
                now
        )));
    }

    private String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize spike payload", exception);
        }
    }

    private <T> T readJson(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to deserialize spike payload", exception);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to deserialize spike payload", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
