package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import java.util.Objects;

public record SkillBundle(BundleManifest manifest, String coreMarkdown, String contentHash) {

    public SkillBundle {
        Objects.requireNonNull(manifest, "manifest must not be null");
        Objects.requireNonNull(coreMarkdown, "coreMarkdown must not be null");
        Objects.requireNonNull(contentHash, "contentHash must not be null");
        if (contentHash.isBlank()) {
            throw new IllegalArgumentException("contentHash must not be blank");
        }
    }

    public String pinnedId() {
        return manifest.pinnedId();
    }
}
