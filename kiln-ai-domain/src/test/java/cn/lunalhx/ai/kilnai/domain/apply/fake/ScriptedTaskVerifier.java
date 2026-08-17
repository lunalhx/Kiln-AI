package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ScriptedTaskVerifier implements TaskVerifierPort {

    private final List<Optional<TaskVerificationVerdict>> replies;
    private final List<TaskPackage> verified = new ArrayList<>();

    public ScriptedTaskVerifier(List<TaskVerificationVerdict> verdicts) {
        this.replies = verdicts.stream().map(Optional::of).toList();
    }

    @SafeVarargs
    public static ScriptedTaskVerifier replies(Optional<TaskVerificationVerdict>... replies) {
        return new ScriptedTaskVerifier(List.of(replies), true);
    }

    private ScriptedTaskVerifier(List<Optional<TaskVerificationVerdict>> replies, boolean ignored) {
        this.replies = List.copyOf(replies);
    }

    @Override
    public TaskVerificationVerdict verify(ModelProfile profile, TaskPackage taskPackage, ApplyExecutionContext context) {
        Objects.requireNonNull(profile, "profile must not be null");
        verified.add(taskPackage);
        if (verified.size() > replies.size()) {
            throw new IllegalStateException("scripted task verifier exhausted: no more scripted verdicts");
        }
        return replies.get(verified.size() - 1)
                .orElseThrow(() -> new ModelContractInvalidException(List.of("unknown_field")));
    }

    public List<TaskPackage> verified() {
        return List.copyOf(verified);
    }
}
