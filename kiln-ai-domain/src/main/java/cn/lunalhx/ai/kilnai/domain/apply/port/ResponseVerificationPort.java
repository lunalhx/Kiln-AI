package cn.lunalhx.ai.kilnai.domain.apply.port;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;


import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessmentContext;

/**
 * The isolated second evaluation of the same performance, invoked only when
 * the deterministic Mathematical Equivalence Check returns Cannot Decide. It
 * receives the identical context as Assessment without ever seeing the
 * Assessment result; conflicting evaluations produce an inconclusive outcome
 * rather than an averaged score.
 */
public interface ResponseVerificationPort {

    ResponseAssessment verify(ModelProfile profile, ResponseAssessmentContext context);
}
