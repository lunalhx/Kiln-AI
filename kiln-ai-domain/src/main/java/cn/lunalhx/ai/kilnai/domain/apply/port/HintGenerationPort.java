package cn.lunalhx.ai.kilnai.domain.apply.port;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;


/**
 * The model port of the reference Hint Profile: one call generates the raw
 * model text of a {@code hint_generation/v1} draft for the current open Apply
 * Practice Attempt. The Hint Flow runs the strict parser, the Hint Ladder
 * Gate, and the one allowed repair before anything is persisted or exposed.
 */
public interface HintGenerationPort {

    String generate(ModelProfile profile, String compiledSystemPrompt, String executionContextJson);
}
