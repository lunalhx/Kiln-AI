package cn.lunalhx.ai.kilnai.domain.apply.port;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;


/**
 * The model port of the reference Teach-back Profile generation: it returns
 * raw model text for the domain's strict closed
 * {@code teach_back_generation/v1} parser. It cannot assess, teach, or mutate
 * Learning State.
 */
public interface TeachBackGenerationPort {

    String generate(ModelProfile profile, String compiledSystemPrompt, String executionContextJson);
}
