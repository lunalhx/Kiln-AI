package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The closed V1 stack for one Evaluation Profile invocation. Evaluation
 * stacks deliberately do not use the Teaching Stack's Action invariant or
 * permit subject-specific bundles.
 */
public record EvaluationBundleStack(List<SkillBundle> bundles) {

    public EvaluationBundleStack {
        Objects.requireNonNull(bundles, "bundles must not be null");
        bundles = List.copyOf(bundles);
        Map<BundleSlot, Long> counts = bundles.stream()
                .collect(Collectors.groupingBy(bundle -> bundle.manifest().slot(), Collectors.counting()));
        if (counts.getOrDefault(BundleSlot.EVALUATION, 0L) != 1L
                || counts.getOrDefault(BundleSlot.VERIFICATION, 0L) != 1L
                || counts.size() != 2
                || counts.values().stream().anyMatch(count -> count > 1)) {
            throw new IllegalArgumentException(
                    "an evaluation stack requires exactly one evaluation and one verification bundle");
        }
    }

    public SkillBundle bundle(BundleSlot slot) {
        if (slot != BundleSlot.EVALUATION && slot != BundleSlot.VERIFICATION) {
            throw new IllegalArgumentException("evaluation stack does not support slot " + slot);
        }
        return bundles.stream()
                .filter(bundle -> bundle.manifest().slot() == slot)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("evaluation stack has no bundle in slot " + slot));
    }

    public List<String> pinnedIds() {
        return bundles.stream().map(SkillBundle::pinnedId).toList();
    }
}
