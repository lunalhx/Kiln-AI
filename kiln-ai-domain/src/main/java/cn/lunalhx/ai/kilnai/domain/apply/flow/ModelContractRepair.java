package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.ModelProviderFailure;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractAudit;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.PostSubmissionEvaluationUnavailableException;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * The one allowed same-context repair for a model responsibility. The first
 * invalid result is audited at repair count 0 and retried; a second invalid
 * result is audited at repair count 1 and signals the graph to commit a
 * durable Unavailable Interaction. Provider and configuration failures signal
 * the same outcome immediately, without a hidden provider retry.
 */
public final class ModelContractRepair {

    private ModelContractRepair() {
    }

    static <T> T once(
            Function<List<String>, T> call,
            ArtifactStore artifactStore,
            UUID flowId,
            UUID attemptId,
            UUID taskPackageId,
            String responsibility,
            String evaluationVersion
    ) {
        Objects.requireNonNull(call, "call must not be null");
        Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        Objects.requireNonNull(responsibility, "responsibility must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(evaluationVersion, "evaluationVersion must not be null");
        String correlationId = UUID.randomUUID().toString();
        try {
            return call.apply(List.of());
        } catch (RuntimeException first) {
            String firstProviderCategory = ModelProviderFailure.providerCategory(first);
            if (firstProviderCategory != null) {
                recordProviderFailure(artifactStore, flowId, attemptId, taskPackageId,
                        responsibility, firstProviderCategory, correlationId);
                throw unavailable(attemptId, responsibility, evaluationVersion);
            }
            if (!(first instanceof ModelContractInvalidException invalid)) {
                throw first;
            }
            artifactStore.recordModelContractAudit(audit(
                    flowId, attemptId, taskPackageId, responsibility, invalid, 0, correlationId));
            try {
                return call.apply(invalid.violationCodes());
            } catch (RuntimeException second) {
                String secondProviderCategory = ModelProviderFailure.providerCategory(second);
                if (secondProviderCategory != null) {
                    recordProviderFailure(artifactStore, flowId, attemptId, taskPackageId,
                            responsibility, secondProviderCategory, correlationId);
                    throw unavailable(attemptId, responsibility, evaluationVersion);
                }
                if (!(second instanceof ModelContractInvalidException invalidSecond)) {
                    throw second;
                }
                artifactStore.recordModelContractAudit(audit(
                        flowId, attemptId, taskPackageId, responsibility, invalidSecond, 1, correlationId));
                throw unavailable(attemptId, responsibility, evaluationVersion);
            }
        }
    }

    private static PostSubmissionEvaluationUnavailableException unavailable(
            UUID attemptId,
            String responsibility,
            String evaluationVersion
    ) {
        return new PostSubmissionEvaluationUnavailableException(attemptId, responsibility, evaluationVersion);
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

    private static void recordProviderFailure(
            ArtifactStore artifactStore,
            UUID flowId,
            UUID attemptId,
            UUID taskPackageId,
            String responsibility,
            String providerCategory,
            String correlationId
    ) {
        artifactStore.recordModelContractAudit(new ModelContractAudit(
                flowId, attemptId, taskPackageId, responsibility,
                List.of(ModelContractAudit.TECHNICAL_FAILURE), 0,
                correlationId, providerCategory));
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
