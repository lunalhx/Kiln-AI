package cn.lunalhx.ai.kilnai.domain.apply.port;

public interface ApplyGenerationPort {

    String generate(String compiledSystemPrompt, String executionContextJson);
}
