package cn.lunalhx.ai.kilnai.domain.apply.port;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;


/**
 * The bounded model port that produces a closed {@code explain_generation/v1}
 * draft from the compiled Explain Profile instructions and its execution
 * context. The Profile owns validation, gating, and repair; this port is the
 * single model call boundary.
 */
@FunctionalInterface
public interface ExplainGenerationPort {

    String generate(ModelProfile profile, String compiledSystemPrompt, String executionContextJson);
}
