package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

/**
 * The bounded model port of the Pedagogy Agent: one stateless call per
 * generation that receives the compiled system prompt and the serialized
 * closed execution context (sanitized Feedback Facts plus the Workflow
 * Guard's legal-action set) and returns the raw {@code pedagogy_plan/v1}
 * draft JSON. The agent cannot reassess the answer, name Skills, execute a
 * Teaching Action, call other Agents, loop, or mutate Learning State.
 */
public interface PedagogyPort {

    String generate(String compiledSystemPrompt, String executionContextJson);
}
