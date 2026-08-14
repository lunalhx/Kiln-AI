package cn.lunalhx.ai.kilnai.domain.apply.port;

import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;

/**
 * The isolated model-backed evaluation of one submitted response. It returns
 * only the closed {@code response_assessment/v1} contract and cannot accept
 * Evidence or mutate Flow State.
 */
public interface AssessmentPort {

    ResponseAssessment assess(ResponseAssessmentContext context);
}
