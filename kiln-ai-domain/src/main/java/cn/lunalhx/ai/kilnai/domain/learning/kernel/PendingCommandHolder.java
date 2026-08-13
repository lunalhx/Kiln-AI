package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingCommandHolder {

    public record CommandSeed(UUID idempotencyKey, String requestHash) {
    }

    private final ConcurrentHashMap<UUID, CommandSeed> pending = new ConcurrentHashMap<>();

    public void hold(UUID flowId, UUID idempotencyKey, String requestHash) {
        pending.put(flowId, new CommandSeed(idempotencyKey, requestHash));
    }

    public Optional<CommandSeed> peek(UUID flowId) {
        return Optional.ofNullable(pending.get(flowId));
    }

    public Optional<CommandSeed> poll(UUID flowId) {
        return Optional.ofNullable(pending.remove(flowId));
    }

    public void discard(UUID flowId) {
        pending.remove(flowId);
    }
}
