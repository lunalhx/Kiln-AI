package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassification;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassifierPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The scripted Clarification Gate classifier port. In the default auto-policy
 * mode the model returns SUBSTANTIVE for every message — the conservative
 * classification that requires explicit consent on an independent attempt —
 * so tests that never address a clarification are unaffected. A scripted
 * response list instead returns the classifications in call order, so tests
 * can drive procedural answers and both consent paths deterministically.
 */
public final class ScriptedClarificationClassifier implements ClarificationClassifierPort {

    private final List<ClarificationClassification> responses;
    private final List<Call> calls = new ArrayList<>();

    public ScriptedClarificationClassifier() {
        this.responses = List.of();
    }

    public ScriptedClarificationClassifier(List<ClarificationClassification> responses) {
        this.responses = List.copyOf(responses);
    }

    @Override
    public ClarificationClassification classify(String message, String taskText) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(taskText, "taskText must not be null");
        calls.add(new Call(message, taskText));
        if (responses.isEmpty()) {
            return ClarificationClassification.SUBSTANTIVE;
        }
        if (calls.size() > responses.size()) {
            throw new IllegalStateException(
                    "scripted clarification classifier exhausted: no more scripted classifications");
        }
        return responses.get(calls.size() - 1);
    }

    public List<Call> calls() {
        return List.copyOf(calls);
    }

    public record Call(String message, String taskText) {

        public Call {
            Objects.requireNonNull(message, "message must not be null");
            Objects.requireNonNull(taskText, "taskText must not be null");
        }
    }
}
