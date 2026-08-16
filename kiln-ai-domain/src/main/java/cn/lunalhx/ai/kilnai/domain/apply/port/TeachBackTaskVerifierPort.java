package cn.lunalhx.ai.kilnai.domain.apply.port;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;


import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;

/**
 * The isolated pre-delivery Task Verification of one generated Teach-back
 * task package, separate from the Teach-back generation context. It returns
 * only the closed {@code task_verification/v1} verdict and never rewrites the
 * task or modifies the Rubric.
 */
public interface TeachBackTaskVerifierPort {

    TaskVerificationVerdict verify(ModelProfile profile, TeachBackTaskPackage taskPackage, TeachBackExecutionContext context);
}
