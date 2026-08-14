package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScriptedResponseVerificationModel implements ResponseVerificationPort {

    private final List<ResponseAssessment> judgments;
    private final List<ResponseAssessmentContext> contexts = new ArrayList<>();

    public ScriptedResponseVerificationModel(List<ResponseAssessment> judgments) {
        this.judgments = List.copyOf(judgments);
    }

    @Override
    public ResponseAssessment verify(ResponseAssessmentContext context) {
        Objects.requireNonNull(context, "context must not be null");
        contexts.add(context);
        if (contexts.size() > judgments.size()) {
            throw new IllegalStateException("scripted response verification model exhausted: no more scripted judgments");
        }
        return judgments.get(contexts.size() - 1);
    }

    public List<ResponseAssessmentContext> contexts() {
        return List.copyOf(contexts);
    }
}
