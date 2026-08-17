package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractAudit;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The one allowed same-context repair for a model responsibility. The first
 * invalid result is audited at repair count 0 and retried; a second invalid
 * result is audited at repair count 1 and returns null so the caller can
 * apply its type-specific Inconclusive or fallback outcome.
 */
public final class ModelContractRepair {

    private ModelContractRepair() {
    }

    static <T> T once(
            Supplier<T> call,
            ArtifactStore artifactStore,
            UUID flowId,
            UUID attemptId,
            UUID taskPackageId,
            String responsibility
    ) {
        Objects.requireNonNull(call, "call must not be null");
        Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        Objects.requireNonNull(responsibility, "responsibility must not be null");
        String correlationId = UUID.randomUUID().toString();
        try {
            return call.get();
        } catch (ModelContractInvalidException first) {
            artifactStore.recordModelContractAudit(audit(
                    flowId, attemptId, taskPackageId, responsibility, first, 0, correlationId));
            try {
                return call.get();
            } catch (ModelContractInvalidException second) {
                artifactStore.recordModelContractAudit(audit(
                        flowId, attemptId, taskPackageId, responsibility, second, 1, correlationId));
                return null;
            }
        }
    }

    public static void recordVoidedCandidate(
            ArtifactStore artifactStore,
            UUID taskPackageId,
            String responsibility,
            ModelContractInvalidException exception
    ) {
        artifactStore.recordModelContractAudit(new ModelContractAudit(
                null, null, taskPackageId, responsibility, exception.violationCodes(), 0,
                UUID.randomUUID().toString(), ModelContractAudit.PROVIDER_CATEGORY));
    }

    private static ModelContractAudit audit(
            UUID flowId,
            UUID attemptId,
            UUID taskPackageId,
            String responsibility,
            ModelContractInvalidException exception,
            int repairCount,
            String correlationId
    ) {
        return new ModelContractAudit(
                flowId, attemptId, taskPackageId, responsibility, exception.violationCodes(),
                repairCount, correlationId, ModelContractAudit.PROVIDER_CATEGORY);
    }
}
