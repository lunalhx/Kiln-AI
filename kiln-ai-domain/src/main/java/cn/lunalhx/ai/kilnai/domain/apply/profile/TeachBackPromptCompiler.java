package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleSlot;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.util.List;
import java.util.Objects;

public final class TeachBackPromptCompiler {

    public static final int INSTRUCTION_BUDGET = 16_000;

    private static final List<BundleSlot> FIXED_SLOT_ORDER = List.of(
            BundleSlot.ACTION,
            BundleSlot.SUBJECT
    );

    public String compile(BundleStack stack) {
        Objects.requireNonNull(stack, "stack must not be null");
        validateStack(stack);

        StringBuilder compiled = new StringBuilder();
        compiled.append(TeachBackProfile.BASE_SYSTEM_PROMPT.trim()).append('\n');
        for (BundleSlot slot : FIXED_SLOT_ORDER) {
            SkillBundle bundle = stack.bundle(slot);
            compiled.append("\n[bundle:").append(slot.name().toLowerCase()).append(':')
                    .append(bundle.pinnedId()).append("]\n");
            compiled.append(bundle.coreMarkdown().trim()).append('\n');
        }
        compiled.append('\n').append(TeachBackProfile.RESPONSE_CONTRACT.trim()).append('\n');
        if (compiled.length() > INSTRUCTION_BUDGET) {
            throw new CapabilityGap("teach-back prompt budget exceeded: " + compiled.length());
        }
        return compiled.toString();
    }

    public String serializeContext(TeachBackExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return ApplyJson.write(context);
    }

    private void validateStack(BundleStack stack) {
        for (SkillBundle bundle : stack.bundles()) {
            BundleSlot slot = bundle.manifest().slot();
            if (slot != BundleSlot.ACTION && !bundle.manifest().outputContribution().isEmpty()) {
                throw new CapabilityGap(
                        "only the action bundle may contribute draft fields: " + bundle.pinnedId());
            }
            if (!bundle.manifest().permissions().tools().isEmpty()) {
                throw new CapabilityGap(
                        "bundle declares tools but the Teach-back stack is tool-free: " + bundle.pinnedId());
            }
            if (!bundle.manifest().compatibility().profiles().contains("teach_back")) {
                throw new CapabilityGap(
                        "bundle is not compatible with the Teach-back profile: " + bundle.pinnedId());
            }
            if (slot == BundleSlot.ACTION
                    && !"teach_back_generation/v1".equals(bundle.manifest().compatibility().responseDraft())) {
                throw new CapabilityGap(
                        "the action bundle must declare the teach_back_generation/v1 draft: " + bundle.pinnedId());
            }
        }
    }
}
