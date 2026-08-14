package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;

/**
 * The isolated model-backed judgment of a Diagnostic rationale. It returns a
 * closed typed judgment and cannot accept Evidence or mutate Flow State.
 */
public interface AssessmentPort {

    RationaleJudgment judgeDiagnosticRationale(RationaleAssessmentContext context);
}
