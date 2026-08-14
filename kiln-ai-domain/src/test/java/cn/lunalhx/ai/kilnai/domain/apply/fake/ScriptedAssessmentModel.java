package cn.lunalhx.ai.kilnai.domain.apply.fake;

import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScriptedAssessmentModel implements AssessmentPort {

    private final List<RationaleJudgment> judgments;
    private final List<RationaleAssessmentContext> contexts = new ArrayList<>();

    public ScriptedAssessmentModel(List<RationaleJudgment> judgments) {
        this.judgments = List.copyOf(judgments);
    }

    @Override
    public RationaleJudgment judgeDiagnosticRationale(RationaleAssessmentContext context) {
        Objects.requireNonNull(context, "context must not be null");
        contexts.add(context);
        if (contexts.size() > judgments.size()) {
            throw new IllegalStateException("scripted assessment model exhausted: no more scripted judgments");
        }
        return judgments.get(contexts.size() - 1);
    }

    public List<RationaleAssessmentContext> contexts() {
        return List.copyOf(contexts);
    }
}
