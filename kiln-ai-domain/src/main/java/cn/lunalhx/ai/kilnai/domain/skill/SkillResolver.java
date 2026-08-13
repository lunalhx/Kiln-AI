package cn.lunalhx.ai.kilnai.domain.skill;

import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class SkillResolver {

    public SkillStack resolve(
            TeachingAction action,
            Set<String> requiredCapabilities,
            Set<String> preferredStrategies,
            List<SkillManifest> registry
    ) {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(requiredCapabilities, "requiredCapabilities must not be null");
        Objects.requireNonNull(preferredStrategies, "preferredStrategies must not be null");
        Objects.requireNonNull(registry, "registry must not be null");

        List<SkillManifest> actionCandidates = registry.stream()
                .filter(manifest -> manifest.slot() == SkillSlot.ACTION)
                .filter(manifest -> manifest.teachingAction() == action)
                .toList();
        SkillManifest actionSkill = selectAction(action, preferredStrategies, actionCandidates);

        List<SkillManifest> capabilitySkills = new ArrayList<>();
        for (String capability : requiredCapabilities) {
            List<SkillManifest> matches = registry.stream()
                    .filter(manifest -> manifest.slot() == SkillSlot.REASONING)
                    .filter(manifest -> manifest.capabilityTags().contains(capability))
                    .toList();
            if (matches.isEmpty()) {
                throw new CapabilityGap("missing required capability: " + capability);
            }
            capabilitySkills.add(selectUnique(matches, "capability " + capability));
        }
        return new SkillStack(actionSkill, capabilitySkills);
    }

    private SkillManifest selectAction(
            TeachingAction action,
            Set<String> preferredStrategies,
            List<SkillManifest> candidates
    ) {
        if (candidates.isEmpty()) {
            throw new CapabilityGap("no action skill registered for " + action);
        }
        List<SkillManifest> strategyMatches = candidates.stream()
                .filter(manifest -> manifest.strategyTags().stream().anyMatch(preferredStrategies::contains))
                .toList();
        if (!strategyMatches.isEmpty()) {
            return selectUnique(strategyMatches, action.name() + " strategy");
        }
        List<SkillManifest> defaults = candidates.stream().filter(SkillManifest::defaultAction).toList();
        if (defaults.isEmpty()) {
            throw new CapabilityGap("no default action skill for " + action);
        }
        return selectUnique(defaults, action.name() + " default");
    }

    private SkillManifest selectUnique(List<SkillManifest> candidates, String label) {
        int winning = candidates.stream().mapToInt(SkillManifest::priority).max().orElseThrow();
        List<SkillManifest> winners = candidates.stream()
                .filter(manifest -> manifest.priority() == winning)
                .collect(Collectors.toList());
        if (winners.size() > 1) {
            throw new CapabilityGap("skill collision for " + label + ": "
                    + winners.stream().map(SkillManifest::id).sorted(Comparator.naturalOrder()).toList());
        }
        return winners.get(0);
    }
}
