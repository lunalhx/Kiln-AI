package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import java.util.List;

public record CatalogProvider(
        String providerId,
        String protocol,
        String endpoint,
        String secretEnvVar,
        List<String> models
) {
    public CatalogProvider {
        models = models == null ? List.of() : List.copyOf(models);
    }
}
