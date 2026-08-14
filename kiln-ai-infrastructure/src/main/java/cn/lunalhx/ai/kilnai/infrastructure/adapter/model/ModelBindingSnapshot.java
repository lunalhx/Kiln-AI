package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

public record ModelBindingSnapshot(
        String protocol,
        String endpoint,
        String providerId,
        String modelId,
        String secretEnvVar
) {
    public String identity() {
        return providerId + "/" + modelId;
    }
}
