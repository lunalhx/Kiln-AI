package cn.lunalhx.ai.kilnai.domain.learning.model;

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
