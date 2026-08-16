package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

/**
 * The bounded model port of the Pedagogy Agent: one stateless Small-model
 * call per generation that receives the Flow-frozen Model Profile, the
 * compiled system prompt, and the serialized closed execution context
 * (sanitized Feedback Facts plus the Workflow Guard's legal-action set) and
 * returns the raw {@code pedagogy_plan/v1} draft JSON. The agent cannot
 * reassess the answer, name Skills, execute a Teaching Action, call other
 * Agents, loop, or mutate Learning State.
 */
public interface PedagogyPort {

    /**
     * One Small-model plan generation. The method name is distinct from the
     * Strong-model generation ports so one adapter class can implement both
     * responsibilities with different slots of the frozen Model Profile.
     */
    String generatePlan(ModelProfile profile, String compiledSystemPrompt, String executionContextJson);
}
