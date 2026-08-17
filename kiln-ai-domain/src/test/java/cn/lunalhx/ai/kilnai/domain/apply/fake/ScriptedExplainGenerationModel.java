package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.port.ExplainGenerationPort;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScriptedExplainGenerationModel implements ExplainGenerationPort {

    private final List<String> responses;
    private final List<Call> calls = new ArrayList<>();
    private int remainingProviderFailures;
    private int responseIndex;

    public ScriptedExplainGenerationModel(List<String> responses) {
        this(0, responses);
    }

    public ScriptedExplainGenerationModel(int providerFailures, List<String> responses) {
        this.responses = List.copyOf(responses);
        this.remainingProviderFailures = providerFailures;
    }

    @Override
    public String generate(ModelProfile profile, String compiledSystemPrompt, String executionContextJson) {
        Objects.requireNonNull(profile, "profile must not be null");
        calls.add(new Call(compiledSystemPrompt, executionContextJson));
        if (remainingProviderFailures > 0) {
            remainingProviderFailures--;
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "provider unavailable");
        }
        if (responseIndex >= responses.size()) {
            throw new IllegalStateException("scripted explain generation model exhausted: no more scripted responses");
        }
        return responses.get(responseIndex++);
    }

    public List<Call> calls() {
        return List.copyOf(calls);
    }

    public String lastSystemPrompt() {
        return calls.get(calls.size() - 1).systemPrompt();
    }

    public String lastContextJson() {
        return calls.get(calls.size() - 1).contextJson();
    }

    public record Call(String systemPrompt, String contextJson) {
        public Call {
            Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
            Objects.requireNonNull(contextJson, "contextJson must not be null");
        }
    }
}
