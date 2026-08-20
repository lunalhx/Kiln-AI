package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleEvaluationResult;

/**
 * The model boundary for the first isolated Rationale Assessment. Prompt
 * compilation and closed-contract parsing remain outside the provider port.
 */
public interface RationaleAssessmentPort {

    RationaleEvaluationResult assess(
            ModelProfile profile,
            String compiledSystemPrompt,
            String evaluationContextJson
    );
}
