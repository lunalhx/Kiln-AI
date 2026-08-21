package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

/**
 * Content-side generation boundary for the Concept Preparation Agent. The
 * provider receives approved preparation facts and returns one closed
 * {@code diagnostic_plan_generation/v1} result; it cannot mutate Learning
 * State or select runtime tasks.
 */
public interface DiagnosticPlanGenerationPort {

    String generatePlan(DiagnosticPlanGateContext context);
}
