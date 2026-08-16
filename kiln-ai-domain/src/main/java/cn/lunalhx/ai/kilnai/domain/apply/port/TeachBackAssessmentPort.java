package cn.lunalhx.ai.kilnai.domain.apply.port;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;


import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessmentContext;

/**
 * The isolated semantic Assessment of one Teach-back submission. It returns
 * only the closed {@code teach_back_assessment/v1} three-dimension contract
 * and cannot accept Evidence or mutate Flow State.
 */
public interface TeachBackAssessmentPort {

    TeachBackAssessment assess(ModelProfile profile, TeachBackAssessmentContext context);
}
