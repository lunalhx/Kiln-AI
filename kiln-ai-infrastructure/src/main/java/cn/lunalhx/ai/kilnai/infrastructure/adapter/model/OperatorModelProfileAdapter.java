package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;

import java.util.Objects;
import java.util.function.Function;

/**
 * The infrastructure adapter that resolves the operator-owned Model Profile
 * from the Provider Catalog and the environment secrets function at Learning
 * Flow start. The resolved snapshot — Strong and Small bindings plus the
 * output-token ceiling — is frozen onto the Flow and never re-resolved for
 * later model calls. A missing or invalid operator configuration fails
 * closed inside the catalog and never falls back to scripted fakes.
 */
public final class OperatorModelProfileAdapter implements OperatorModelProfilePort {

    private final OperatorCatalog catalog;
    private final Function<String, String> secrets;

    public OperatorModelProfileAdapter(OperatorCatalog catalog, Function<String, String> secrets) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.secrets = Objects.requireNonNull(secrets, "secrets must not be null");
    }

    @Override
    public ModelProfile resolve() {
        return catalog.resolve(secrets);
    }
}
