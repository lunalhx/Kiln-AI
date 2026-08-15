package cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;

import java.util.Objects;

public record SkillBundleSource(BundleManifest manifest, String coreMarkdown, byte[] fullFileContent) {

    public SkillBundleSource {
        Objects.requireNonNull(manifest, "manifest must not be null");
        Objects.requireNonNull(coreMarkdown, "coreMarkdown must not be null");
        Objects.requireNonNull(fullFileContent, "fullFileContent must not be null");
    }

    public SkillBundle toBundle() {
        return new SkillBundle(manifest, coreMarkdown, ApplyHash.sha256Hex(fullFileContent));
    }
}
