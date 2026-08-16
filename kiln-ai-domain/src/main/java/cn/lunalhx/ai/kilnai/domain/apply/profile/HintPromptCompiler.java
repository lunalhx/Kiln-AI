package cn.lunalhx.ai.kilnai.domain.apply.profile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExecutionContext;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;

import java.util.Objects;

/**
 * The deterministic prompt compiler of the reference Hint Profile: it builds
 * the bounded system instructions from the Profile contract and passes the
 * {@code hint_execution_context/v1} JSON as separate execution data, never
 * relying on prompt order or silent truncation.
 */
public final class HintPromptCompiler {

    public static final int INSTRUCTION_BUDGET = 16_000;

    public String compile() {
        String compiled = HintProfile.BASE_SYSTEM_PROMPT.trim()
                + "\n\n" + HintProfile.RESPONSE_CONTRACT.trim() + "\n";
        if (compiled.length() > INSTRUCTION_BUDGET) {
            throw new CapabilityGap("hint prompt budget exceeded: " + compiled.length());
        }
        return compiled;
    }

    public String serializeContext(HintExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return ApplyJson.write(context);
    }
}
