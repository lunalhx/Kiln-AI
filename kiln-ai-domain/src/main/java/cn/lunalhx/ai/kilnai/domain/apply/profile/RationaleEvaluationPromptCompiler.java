package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleSlot;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.EvaluationBundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationContext;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.util.List;
import java.util.Objects;

/**
 * Compiles the fixed Rationale Evaluation Profile and its two-bundle stack in
 * a dedicated order and budget.
 */
public final class RationaleEvaluationPromptCompiler {

    public static final int INSTRUCTION_BUDGET = 16_000;

    private static final List<BundleSlot> FIXED_SLOT_ORDER = List.of(
            BundleSlot.EVALUATION,
            BundleSlot.VERIFICATION
    );

    public String compile(EvaluationBundleStack stack) {
        return compile(stack, List.of());
    }

    public String compile(EvaluationBundleStack stack, List<String> normalizedViolations) {
        Objects.requireNonNull(stack, "stack must not be null");
        Objects.requireNonNull(normalizedViolations, "normalizedViolations must not be null");
        validateStack(stack);

        StringBuilder compiled = new StringBuilder();
        compiled.append(RationaleEvaluationProfile.BASE_SYSTEM_PROMPT.trim()).append('\n');
        for (BundleSlot slot : FIXED_SLOT_ORDER) {
            SkillBundle bundle = stack.bundle(slot);
            compiled.append("\n[bundle:").append(slot.name().toLowerCase()).append(':')
                    .append(bundle.pinnedId()).append("]\n")
                    .append(bundle.coreMarkdown().trim()).append('\n');
        }
        compiled.append('\n').append(RationaleEvaluationProfile.RESPONSE_CONTRACT.trim()).append('\n');
        if (!normalizedViolations.isEmpty()) {
            compiled.append("\n[contract-repair]\n")
                    .append("The previous response violated these normalized contract checks: ")
                    .append(String.join(", ", normalizedViolations))
                    .append(". Return a corrected object that satisfies the same contract.\n");
        }
        if (compiled.length() > INSTRUCTION_BUDGET) {
            throw new CapabilityGap("rationale evaluation prompt budget exceeded: " + compiled.length());
        }
        return compiled.toString();
    }

    public String serializeContext(RationaleEvaluationContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return ApplyJson.write(context);
    }

    private void validateStack(EvaluationBundleStack stack) {
        for (SkillBundle bundle : stack.bundles()) {
            if (!bundle.manifest().outputContribution().isEmpty()) {
                throw new CapabilityGap(
                        "evaluation bundles may not contribute result fields: " + bundle.pinnedId());
            }
            if (!bundle.manifest().permissions().tools().isEmpty()) {
                throw new CapabilityGap("evaluation bundles must be tool-free: " + bundle.pinnedId());
            }
            if (!bundle.manifest().compatibility().profiles().contains("rationale_evaluation")
                    || !"rationale_evaluation/v1".equals(bundle.manifest().compatibility().responseDraft())) {
                throw new CapabilityGap(
                        "bundle is not compatible with the Rationale Evaluation Profile: " + bundle.pinnedId());
            }
        }
    }
}
