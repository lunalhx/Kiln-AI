package cn.lunalhx.ai.kilnai.domain.apply.port;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;


public interface ApplyGenerationPort {

    String generate(ModelProfile profile, String compiledSystemPrompt, String executionContextJson);
}
