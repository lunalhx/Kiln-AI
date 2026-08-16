package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackTaskVerifierPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScriptedTeachBackTaskVerifier implements TeachBackTaskVerifierPort {

    private final List<TaskVerificationVerdict> verdicts;
    private final List<TeachBackTaskPackage> verified = new ArrayList<>();

    public ScriptedTeachBackTaskVerifier(List<TaskVerificationVerdict> verdicts) {
        this.verdicts = List.copyOf(verdicts);
    }

    @Override
    public TaskVerificationVerdict verify(ModelProfile profile, TeachBackTaskPackage taskPackage, TeachBackExecutionContext context) {
        Objects.requireNonNull(profile, "profile must not be null");
        verified.add(taskPackage);
        if (verified.size() > verdicts.size()) {
            throw new IllegalStateException("scripted teach-back verifier exhausted: no more scripted verdicts");
        }
        return verdicts.get(verified.size() - 1);
    }

    public List<TeachBackTaskPackage> verified() {
        return List.copyOf(verified);
    }
}
