package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import java.util.Objects;

public record SkillBundleSource(BundleManifest manifest, String coreMarkdown, byte[] fullFileContent) {

    public SkillBundleSource {
        Objects.requireNonNull(manifest, "manifest must not be null");
        Objects.requireNonNull(coreMarkdown, "coreMarkdown must not be null");
        Objects.requireNonNull(fullFileContent, "fullFileContent must not be null");
    }
}
