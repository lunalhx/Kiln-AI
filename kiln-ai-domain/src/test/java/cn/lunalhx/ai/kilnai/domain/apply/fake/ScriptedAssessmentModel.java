package cn.lunalhx.ai.kilnai.domain.apply.fake;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScriptedAssessmentModel implements AssessmentPort {

    private final List<ResponseAssessment> judgments;
    private final List<ResponseAssessmentContext> contexts = new ArrayList<>();

    public ScriptedAssessmentModel(List<ResponseAssessment> judgments) {
        this.judgments = List.copyOf(judgments);
    }

    @Override
    public ResponseAssessment assess(ModelProfile profile, ResponseAssessmentContext context) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(context, "context must not be null");
        contexts.add(context);
        if (contexts.size() > judgments.size()) {
            throw new IllegalStateException("scripted assessment model exhausted: no more scripted judgments");
        }
        return judgments.get(contexts.size() - 1);
    }

    public List<ResponseAssessmentContext> contexts() {
        return List.copyOf(contexts);
    }
}
