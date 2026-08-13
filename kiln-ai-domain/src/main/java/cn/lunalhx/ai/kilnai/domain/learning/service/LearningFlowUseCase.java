package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningGraphRuntimePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort.FlowRecord;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort.ProcessedCommand;
import cn.lunalhx.ai.kilnai.domain.learning.fixture.SpikeFixture;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommandHolder;
import cn.lunalhx.ai.kilnai.domain.learning.model.FrozenModelProfile;
import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;

public final class LearningFlowUseCase {

    private final LearningGraphRuntimePort runtime;
    private final SpikeStorePort store;
    private final ModelProfilePort modelProfiles;
    private final PendingCommandHolder pendingCommands;
    private final Clock clock;

    public LearningFlowUseCase(
            LearningGraphRuntimePort runtime,
            SpikeStorePort store,
            ModelProfilePort modelProfiles,
            PendingCommandHolder pendingCommands,
            Clock clock
    ) {
        this.runtime = runtime;
        this.store = store;
        this.modelProfiles = modelProfiles;
        this.pendingCommands = pendingCommands;
        this.clock = clock;
    }

    public LearnerVisibleInteraction start(StartGraphRun command) {
        requireUuidKey(command.idempotencyKey());
        String hash = hash("start", command.learnerId(), command.fixtureId());
        return replayOrRun(command.idempotencyKey(), hash, () -> {
            if (!SpikeFixture.PERCENT_CHANGE_V1.equals(command.fixtureId())) {
                throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "unknown fixture: " + command.fixtureId());
            }
            UUID flowId = UUID.randomUUID();
            FrozenModelProfile frozen = modelProfiles.resolveCurrentDefaults();
            store.insertFlow(new FlowRecord(
                    flowId, command.learnerId(), SpikeFixture.CONCEPT_ID, SpikeFixture.CONTRACT_ID,
                    SpikeFixture.RUBRIC_ID, SpikeFixture.SOURCE_PACK_ID, FlowStatus.READY,
                    LearningStage.LEARNING_AND_PRACTICE, clock.instant(), frozen
            ));
            pendingCommands.hold(flowId, command.idempotencyKey(), hash);
            try {
                LearnerVisibleInteraction interaction = runtime.start(new StartGraphRun(
                        command.learnerId(), command.fixtureId(), command.idempotencyKey(), flowId
                ));
                return persist(command.idempotencyKey(), hash, interaction);
            } catch (RuntimeException exception) {
                pendingCommands.discard(flowId);
                throw exception;
            }
        });
    }

    public LearnerVisibleInteraction resume(ResumeGraphRun command) {
        requireUuidKey(command.idempotencyKey());
        String hash = hash("resume", command.flowId(), command.interactionVersion(), command.kind(), command.text());
        return replayOrRun(command.idempotencyKey(), hash, () -> {
            LearnerVisibleInteraction current = store.latestInteraction(command.flowId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
            if (command.interactionVersion() != current.interactionVersion()) {
                throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
            }
            pendingCommands.hold(command.flowId(), command.idempotencyKey(), hash);
            try {
                LearnerVisibleInteraction interaction = runtime.resume(command);
                return persist(command.idempotencyKey(), hash, interaction);
            } catch (RuntimeException exception) {
                pendingCommands.discard(command.flowId());
                throw exception;
            }
        });
    }

    public LearnerVisibleInteraction query(UUID flowId) {
        return store.latestInteraction(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
    }

    public PublicTraceView trace(UUID flowId) {
        return store.publicTrace(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "trace not found"));
    }

    private LearnerVisibleInteraction replayOrRun(UUID key, String hash, java.util.function.Supplier<LearnerVisibleInteraction> action) {
        return store.findCommand(key).map(existing -> {
            if (!existing.requestHash().equals(hash)) {
                throw new ApplicationException(ErrorCode.CONFLICT, "idempotency key reused with a different payload");
            }
            return existing.response();
        }).orElseGet(action);
    }

    private LearnerVisibleInteraction persist(UUID key, String hash, LearnerVisibleInteraction interaction) {
        store.saveCommand(new ProcessedCommand(key, hash, interaction.flowId(), 200, interaction, clock.instant()));
        return interaction;
    }

    private void requireUuidKey(UUID key) {
        if (key == null) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key is required");
        }
    }

    private String hash(Object... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object part : parts) {
                digest.update(String.valueOf(part).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
