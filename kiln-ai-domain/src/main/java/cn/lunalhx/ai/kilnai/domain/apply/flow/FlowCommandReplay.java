package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The shared idempotency-replay boundary of the durable Apply commands: a
 * replayed key returns the original committed interaction; a key reused with
 * a different payload conflicts. An unprocessed key runs the fresh action.
 */
final class FlowCommandReplay {

    private FlowCommandReplay() {
    }

    static <T> T replayOrRun(
            LearningFlowStore flowStore,
            UUID key,
            String hash,
            Function<ApplyFlowInteraction, T> toBoundary,
            Supplier<T> action
    ) {
        Objects.requireNonNull(flowStore, "flowStore must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(hash, "hash must not be null");
        return flowStore.findCommand(key).map(existing -> {
            if (!existing.requestHash().equals(hash)) {
                throw new ApplicationException(ErrorCode.CONFLICT, "idempotency key reused with a different payload");
            }
            return toBoundary.apply(existing.response());
        }).orElseGet(action);
    }
}
