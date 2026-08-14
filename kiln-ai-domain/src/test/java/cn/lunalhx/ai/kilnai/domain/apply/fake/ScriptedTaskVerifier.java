package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;

import java.util.ArrayList;
import java.util.List;

public final class ScriptedTaskVerifier implements TaskVerifierPort {

    private final List<TaskVerificationVerdict> verdicts;
    private final List<TaskPackage> verified = new ArrayList<>();

    public ScriptedTaskVerifier(List<TaskVerificationVerdict> verdicts) {
        this.verdicts = List.copyOf(verdicts);
    }

    @Override
    public TaskVerificationVerdict verify(TaskPackage taskPackage, ApplyExecutionContext context) {
        verified.add(taskPackage);
        if (verified.size() > verdicts.size()) {
            throw new IllegalStateException("scripted task verifier exhausted: no more scripted verdicts");
        }
        return verdicts.get(verified.size() - 1);
    }

    public List<TaskPackage> verified() {
        return List.copyOf(verified);
    }
}
