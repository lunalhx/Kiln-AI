package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BundleRegistry {

    private final Map<String, SkillBundle> bundles = new LinkedHashMap<>();

    public SkillBundle register(SkillBundleSource source) {
        Objects.requireNonNull(source, "source must not be null");
        String pinnedId = source.manifest().pinnedId();
        if (bundles.containsKey(pinnedId)) {
            throw new IllegalArgumentException("bundle already registered: " + pinnedId);
        }
        SkillBundle bundle = new SkillBundle(
                source.manifest(),
                source.coreMarkdown(),
                ApplyHash.sha256Hex(source.fullFileContent())
        );
        bundles.put(pinnedId, bundle);
        return bundle;
    }

    public SkillBundle resolve(String id, String version) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(version, "version must not be null");
        SkillBundle bundle = bundles.get(id + "@" + version);
        if (bundle == null) {
            throw new CapabilityGap("bundle not registered: " + id + "@" + version);
        }
        return bundle;
    }

    public Collection<SkillBundle> all() {
        return List.copyOf(bundles.values());
    }
}
