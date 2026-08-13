package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.learning.model.FrozenModelProfile;

import java.util.function.Function;

public final class CatalogModelProfilePort implements ModelProfilePort {

    private final OperatorCatalog catalog;
    private final Function<String, String> secrets;

    public CatalogModelProfilePort(OperatorCatalog catalog, Function<String, String> secrets) {
        this.catalog = catalog;
        this.secrets = secrets;
    }

    @Override
    public FrozenModelProfile resolveCurrentDefaults() {
        return catalog.resolve(secrets);
    }
}
