package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackTaskVerifierPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ScriptedTeachBackTaskVerifier implements TeachBackTaskVerifierPort {

    private final List<Optional<TaskVerificationVerdict>> replies;
    private final List<TeachBackTaskPackage> verified = new ArrayList<>();

    public ScriptedTeachBackTaskVerifier(List<TaskVerificationVerdict> verdicts) {
        this.replies = verdicts.stream().map(Optional::of).toList();
    }

    @SafeVarargs
    public static ScriptedTeachBackTaskVerifier replies(Optional<TaskVerificationVerdict>... replies) {
        return new ScriptedTeachBackTaskVerifier(List.of(replies), true);
    }

    private ScriptedTeachBackTaskVerifier(List<Optional<TaskVerificationVerdict>> replies, boolean ignored) {
        this.replies = List.copyOf(replies);
    }

    @Override
    public TaskVerificationVerdict verify(ModelProfile profile, TeachBackTaskPackage taskPackage, TeachBackExecutionContext context) {
        Objects.requireNonNull(profile, "profile must not be null");
        verified.add(taskPackage);
        if (verified.size() > replies.size()) {
            throw new IllegalStateException("scripted teach-back verifier exhausted: no more scripted verdicts");
        }
        return replies.get(verified.size() - 1)
                .orElseThrow(() -> new ModelContractInvalidException(List.of("unknown_field")));
    }

    public List<TeachBackTaskPackage> verified() {
        return List.copyOf(verified);
    }
}
