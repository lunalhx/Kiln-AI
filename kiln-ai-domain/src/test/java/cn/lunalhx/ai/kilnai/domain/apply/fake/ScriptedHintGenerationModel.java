package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.port.HintGenerationPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scripted model double for the Hint Generation port: returns the next
 * scripted raw {@code hint_generation/v1} response per call and records the
 * compiled prompt and context so tests can assert the ladder is generated
 * exactly once per attempt.
 */
public final class ScriptedHintGenerationModel implements HintGenerationPort {

    private final List<String> responses;
    private final List<Call> calls = new ArrayList<>();

    public ScriptedHintGenerationModel(List<String> responses) {
        this.responses = List.copyOf(responses);
    }

    @Override
    public String generate(ModelProfile profile, String compiledSystemPrompt, String executionContextJson) {
        Objects.requireNonNull(profile, "profile must not be null");
        calls.add(new Call(compiledSystemPrompt, executionContextJson));
        if (calls.size() > responses.size()) {
            throw new IllegalStateException("scripted hint generation model exhausted: no more scripted responses");
        }
        return responses.get(calls.size() - 1);
    }

    public List<Call> calls() {
        return List.copyOf(calls);
    }

    public record Call(String systemPrompt, String contextJson) {
        public Call {
            Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
            Objects.requireNonNull(contextJson, "contextJson must not be null");
        }
    }
}
