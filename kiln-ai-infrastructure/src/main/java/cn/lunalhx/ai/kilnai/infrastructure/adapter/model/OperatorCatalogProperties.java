package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "kiln.catalog")
public class OperatorCatalogProperties {

    private String strong;
    private String small;
    private Integer toolBudget;
    private List<ProviderProperties> providers = new ArrayList<>();

    public String getStrong() {
        return strong;
    }

    public void setStrong(String strong) {
        this.strong = strong;
    }

    public String getSmall() {
        return small;
    }

    public void setSmall(String small) {
        this.small = small;
    }

    public Integer getToolBudget() {
        return toolBudget;
    }

    public void setToolBudget(Integer toolBudget) {
        this.toolBudget = toolBudget;
    }

    public List<ProviderProperties> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderProperties> providers) {
        this.providers = providers == null ? new ArrayList<>() : providers;
    }

    public OperatorCatalog toCatalog() {
        List<CatalogProvider> mapped = providers.stream()
                .map(provider -> new CatalogProvider(
                        provider.getProviderId(),
                        provider.getProtocol(),
                        provider.getEndpoint(),
                        provider.getSecretEnvVar(),
                        provider.getModels()
                ))
                .toList();
        return new OperatorCatalog(mapped, strong, small, toolBudget);
    }

    public static class ProviderProperties {

        private String providerId;
        private String protocol;
        private String endpoint;
        private String secretEnvVar;
        private List<String> models = new ArrayList<>();

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getSecretEnvVar() {
            return secretEnvVar;
        }

        public void setSecretEnvVar(String secretEnvVar) {
            this.secretEnvVar = secretEnvVar;
        }

        public List<String> getModels() {
            return models;
        }

        public void setModels(List<String> models) {
            this.models = models == null ? new ArrayList<>() : models;
        }
    }
}
