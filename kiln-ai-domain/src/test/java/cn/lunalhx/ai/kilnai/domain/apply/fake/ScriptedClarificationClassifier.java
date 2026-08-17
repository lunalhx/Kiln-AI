package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassification;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassifierPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The scripted Clarification Gate classifier port. In the default auto-policy
 * mode the model returns SUBSTANTIVE for every message — the conservative
 * classification that requires explicit consent on an independent attempt —
 * so tests that never address a clarification are unaffected. A scripted
 * response list instead returns the classifications in call order, so tests
 * can drive procedural answers and both consent paths deterministically.
 */
public final class ScriptedClarificationClassifier implements ClarificationClassifierPort {

    private final List<Optional<ClarificationClassification>> replies;
    private final List<Call> calls = new ArrayList<>();

    public ScriptedClarificationClassifier() {
        this.replies = List.of();
    }

    public ScriptedClarificationClassifier(List<ClarificationClassification> responses) {
        this.replies = responses.stream().map(Optional::of).toList();
    }

    @SafeVarargs
    public static ScriptedClarificationClassifier replies(Optional<ClarificationClassification>... replies) {
        return new ScriptedClarificationClassifier(List.of(replies), true);
    }

    private ScriptedClarificationClassifier(List<Optional<ClarificationClassification>> replies, boolean ignored) {
        this.replies = List.copyOf(replies);
    }

    @Override
    public ClarificationClassification classify(ModelProfile profile, String message, String taskText) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(taskText, "taskText must not be null");
        calls.add(new Call(message, taskText));
        if (replies.isEmpty()) {
            return ClarificationClassification.SUBSTANTIVE;
        }
        if (calls.size() > replies.size()) {
            throw new IllegalStateException(
                    "scripted clarification classifier exhausted: no more scripted classifications");
        }
        return replies.get(calls.size() - 1)
                .orElseThrow(() -> new ModelContractInvalidException(List.of("invalid_enum")));
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
