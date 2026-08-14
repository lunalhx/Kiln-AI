package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.List;
import java.util.function.Function;

public final class OperatorCatalog {

    public static final String OPENAI_COMPATIBLE = "openai-compatible";

    private volatile Snapshot snapshot;

    public OperatorCatalog(List<CatalogProvider> providers, String strong, String small) {
        replace(providers, strong, small);
    }

    public void replace(List<CatalogProvider> providers, String strong, String small) {
        this.snapshot = new Snapshot(
                providers == null ? List.of() : List.copyOf(providers),
                strong,
                small
        );
    }

    public List<CatalogProvider> providers() {
        return snapshot.providers();
    }

    public ModelBindingSnapshot strong(Function<String, String> secrets) {
        Snapshot current = snapshot;
        if (current.providers().isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "provider catalog is missing");
        }
        return bind(current, current.strong(), secrets);
    }

    public ModelBindingSnapshot small(Function<String, String> secrets) {
        Snapshot current = snapshot;
        if (current.providers().isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "provider catalog is missing");
        }
        return bind(current, current.small(), secrets);
    }

    private ModelBindingSnapshot bind(Snapshot current, String identity, Function<String, String> secrets) {
        if (isBlank(identity)) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "strong/small model profile is incomplete");
        }
        int slash = identity.indexOf('/');
        if (slash <= 0 || slash == identity.length() - 1) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "unknown model: " + identity);
        }
        String providerId = identity.substring(0, slash);
        String modelId = identity.substring(slash + 1);
        CatalogProvider provider = current.providers().stream()
                .filter(candidate -> providerId.equals(candidate.providerId()))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_ARGUMENT, "unknown model: " + identity));
        if (!OPENAI_COMPATIBLE.equals(provider.protocol())) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "unsupported protocol: " + provider.protocol());
        }
        if (!provider.models().contains(modelId)) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "unknown model: " + identity);
        }
        if (isBlank(provider.secretEnvVar())) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "secret is missing: " + provider.secretEnvVar());
        }
        String secret = secrets.apply(provider.secretEnvVar());
        if (isBlank(secret)) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "secret is missing: " + provider.secretEnvVar());
        }
        return new ModelBindingSnapshot(
                provider.protocol(),
                provider.endpoint(),
                provider.providerId(),
                modelId,
                provider.secretEnvVar()
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Snapshot(
            List<CatalogProvider> providers,
            String strong,
            String small
    ) {
    }
}
