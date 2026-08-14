package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record BundleStack(List<SkillBundle> bundles) {

    public BundleStack {
        Objects.requireNonNull(bundles, "bundles must not be null");
        bundles = List.copyOf(bundles);
        Map<BundleSlot, Long> counts = bundles.stream()
                .collect(Collectors.groupingBy(bundle -> bundle.manifest().slot(), Collectors.counting()));
        if (counts.getOrDefault(BundleSlot.ACTION, 0L) != 1L) {
            throw new IllegalArgumentException("a stack requires exactly one action bundle");
        }
        counts.forEach((slot, count) -> {
            if (count > 1) {
                throw new IllegalArgumentException("stack has multiple bundles in slot " + slot);
            }
        });
    }

    public SkillBundle bundle(BundleSlot slot) {
        return bundles.stream()
                .filter(bundle -> bundle.manifest().slot() == slot)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("stack has no bundle in slot " + slot));
    }

    public List<String> pinnedIds() {
        return bundles.stream().map(SkillBundle::pinnedId).toList();
    }
}
