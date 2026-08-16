package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackGenerationPort;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The scripted Teach-back generation port. Each scripted draft is re-anchored
 * to the anchor id supplied in the execution context, mirroring the real
 * contract where the model must echo the supplied eligible anchor; this keeps
 * the scripted fixtures valid for the runtime anchor ids of the graph
 * harness.
 */
public final class ScriptedTeachBackGenerationModel implements TeachBackGenerationPort {

    private final List<String> responses;
    private final List<Call> calls = new ArrayList<>();

    public ScriptedTeachBackGenerationModel(List<String> responses) {
        this.responses = List.copyOf(responses);
    }

    @Override
    public String generate(String compiledSystemPrompt, String executionContextJson) {
        calls.add(new Call(compiledSystemPrompt, executionContextJson));
        if (calls.size() > responses.size()) {
            throw new IllegalStateException("scripted teach-back generation model exhausted: no more scripted responses");
        }
        JsonNode anchor = ApplyJson.readTree(executionContextJson).path("anchor");
        String contextAnchorId = anchor.path("anchor_id").asText("");
        String contextAnchorKind = anchor.path("anchor_kind").asText("");
        String response = responses.get(calls.size() - 1);
        if (!contextAnchorId.isBlank()) {
            response = response.replace(TeachBackScriptData.ANCHOR_ID.toString(), contextAnchorId);
        }
        if (!contextAnchorKind.isBlank()) {
            response = response.replace(TeachBackScriptData.ANCHOR_KIND, contextAnchorKind);
        }
        return response;
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
