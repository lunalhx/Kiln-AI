package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.List;
import java.util.function.Function;

/**
 * The operator-owned Provider Catalog (ADR-0037): a registry of protocol,
 * endpoint, and listed models with model identity written as
 * {@code providerId/modelId}. Operators set the two Model Profile slots —
 * Strong Model and Small Model — plus the output-token ceiling. A missing
 * catalog entry, incomplete profile, missing secret, or absent output ceiling
 * fails closed and never falls back to scripted fakes.
 */
public final class OperatorCatalog {

    public static final String OPENAI_COMPATIBLE = "openai-compatible";

    private volatile Snapshot snapshot;

    public OperatorCatalog(List<CatalogProvider> providers, String strong, String small, int outputTokenCeiling) {
        replace(providers, strong, small, outputTokenCeiling);
    }

    public void replace(List<CatalogProvider> providers, String strong, String small, int outputTokenCeiling) {
        if (outputTokenCeiling <= 0) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "output token ceiling is not configured");
        }
        this.snapshot = new Snapshot(
                providers == null ? List.of() : List.copyOf(providers),
                strong,
                small,
                outputTokenCeiling
        );
    }

    public List<CatalogProvider> providers() {
        return snapshot.providers();
    }

    public int outputTokenCeiling() {
        return snapshot.outputTokenCeiling();
    }

    /**
     * Resolves the Flow-frozen Model Profile from the current operator
     * configuration: both slots are resolved against the catalog and the
     * secrets function, and the operator-owned output-token ceiling is
     * included. Any missing or invalid entry fails closed with an application
     * error; the profile carries the resolved snapshot, never secret values.
     */
    public ModelProfile resolve(Function<String, String> secrets) {
        Snapshot current = snapshot;
        if (current.providers().isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "provider catalog is missing");
        }
        ModelBindingSnapshot strong = bind(current, current.strong(), secrets);
        ModelBindingSnapshot small = bind(current, current.small(), secrets);
        return new ModelProfile(
                toBinding(strong),
                toBinding(small),
                current.outputTokenCeiling());
    }

    private static ModelProfile.ModelBinding toBinding(ModelBindingSnapshot snapshot) {
        return new ModelProfile.ModelBinding(
                snapshot.protocol(),
                snapshot.endpoint(),
                snapshot.providerId(),
                snapshot.modelId(),
                snapshot.secretEnvVar());
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
            String small,
            int outputTokenCeiling
    ) {
    }
}
