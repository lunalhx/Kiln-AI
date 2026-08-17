package cn.lunalhx.ai.kilnai.domain.apply;

import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

/**
 * Provider network/timeout/upstream 5xx and runtime Model Profile
 * configuration failures share {@link ErrorCode#SERVICE_UNAVAILABLE} today.
 * On an existing Flow they become a durable Unavailable Interaction; before
 * the first Start binding they remain the generic 503.
 */
public final class ModelProviderFailure {

    private ModelProviderFailure() {
    }

    public static boolean isProviderOrConfiguration(RuntimeException exception) {
        return exception instanceof ApplicationException application
                && application.errorCode() == ErrorCode.SERVICE_UNAVAILABLE;
    }
}
