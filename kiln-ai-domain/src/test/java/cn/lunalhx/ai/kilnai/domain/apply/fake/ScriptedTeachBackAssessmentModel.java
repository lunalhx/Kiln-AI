package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackAssessmentPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScriptedTeachBackAssessmentModel implements TeachBackAssessmentPort {

    private final List<TeachBackAssessment> judgments;
    private final List<TeachBackAssessmentContext> contexts = new ArrayList<>();

    public ScriptedTeachBackAssessmentModel(List<TeachBackAssessment> judgments) {
        this.judgments = List.copyOf(judgments);
    }

    @Override
    public TeachBackAssessment assess(TeachBackAssessmentContext context) {
        Objects.requireNonNull(context, "context must not be null");
        contexts.add(context);
        if (contexts.size() > judgments.size()) {
            throw new IllegalStateException("scripted teach-back assessment model exhausted: no more scripted judgments");
        }
        return judgments.get(contexts.size() - 1);
    }

    public List<TeachBackAssessmentContext> contexts() {
        return List.copyOf(contexts);
    }
}
