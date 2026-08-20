package cn.lunalhx.ai.kilnai.domain.apply;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractAudit;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

/**
 * Provider network/timeout/upstream 5xx and Model Profile configuration
 * failures are represented by the model boundary as SERVICE_UNAVAILABLE or
 * INVALID_ARGUMENT. On an existing Flow they become a durable Unavailable
 * Interaction; before the first Start binding they remain the generic 503.
 */
public final class ModelProviderFailure {

    private ModelProviderFailure() {
    }

    public static boolean isProviderOrConfiguration(RuntimeException exception) {
        return providerCategory(exception) != null;
    }

    /**
     * Returns the durable internal category for a model-boundary failure, or
     * {@code null} when the exception is not a provider/configuration failure.
     */
    public static String providerCategory(RuntimeException exception) {
        if (!(exception instanceof ApplicationException application)) {
            return null;
        }
        return switch (application.errorCode()) {
            case INVALID_ARGUMENT -> ModelContractAudit.MODEL_CONFIGURATION_INVALID;
            case SERVICE_UNAVAILABLE -> ModelContractAudit.MODEL_PROVIDER_UNAVAILABLE;
            default -> null;
        };
    }
}
