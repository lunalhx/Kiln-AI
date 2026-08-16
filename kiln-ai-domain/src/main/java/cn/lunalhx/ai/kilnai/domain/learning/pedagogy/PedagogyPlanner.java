package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;

import java.util.List;
import java.util.Objects;

/**
 * The bounded Pedagogy Agent executor: one initial plan generation and at most
 * one same-plan repair under the same closed context, with no autonomous
 * loop. Each candidate is parsed against the closed {@code pedagogy_plan/v1}
 * contract and validated by the Pedagogy Plan Gate against the Workflow
 * Guard's legal-action set. When the second result is still invalid, the
 * entire invalid output — its feedback, action, reason, and tags — is
 * discarded and the deterministic fallback action is returned; the caller
 * owns the neutral learner feedback. The planner never writes Learning State.
 */
public final class PedagogyPlanner {

    private static final int MAX_GENERATION_CYCLES = 2;

    private final PedagogyPort port;
    private final PedagogyPromptCompiler compiler;
    private final TypedArtifactGatePipeline gatePipeline;

    public PedagogyPlanner(PedagogyPort port) {
        this.port = Objects.requireNonNull(port, "port must not be null");
        this.compiler = new PedagogyPromptCompiler();
        this.gatePipeline = new TypedArtifactGatePipeline();
    }

    /**
     * Runs the bounded plan cycle for one guarded decision. Returns the first
     * validated plan, or the deterministic fallback when both candidates are
     * invalid. The context JSON and system prompt are identical for the
     * initial generation and the same-plan repair.
     */
    public PedagogyDecision plan(
            ModelProfile profile,
            FeedbackFacts facts,
            List<TeachingAction> legalActions,
            TeachingAction fallback
    ) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(facts, "facts must not be null");
        Objects.requireNonNull(legalActions, "legalActions must not be null");
        Objects.requireNonNull(fallback, "fallback must not be null");
        String systemPrompt = compiler.compile();
        String contextJson = compiler.serializeContext(facts, legalActions);
        for (int cycle = 1; cycle <= MAX_GENERATION_CYCLES; cycle++) {
            String raw = port.generatePlan(profile, systemPrompt, contextJson);
            PedagogyPlan plan;
            try {
                plan = PedagogyPlan.parse(raw);
            } catch (ApplyDraftException exception) {
                continue;
            }
            GateResult<PedagogyPlan> gate = gatePipeline.validate(
                    plan, new PedagogyPlanGatePolicy(java.util.Set.copyOf(legalActions)), GateContext.empty());
            if (gate.outcome() == GateOutcome.PASSED) {
                return new PedagogyDecision.PlanAccepted(plan);
            }
        }
        return new PedagogyDecision.Fallback(fallback);
    }

    /**
     * The closed outcome of one guarded plan cycle: a validated Pedagogy Plan
     * whose learner feedback and action may drive the next Teaching Node
     * invocation, or the deterministic fallback action with the caller-owned
     * neutral feedback. Nothing is persisted by the planner.
     */
    public sealed interface PedagogyDecision
            permits PedagogyDecision.PlanAccepted, PedagogyDecision.Fallback {

        record PlanAccepted(PedagogyPlan plan) implements PedagogyDecision {

            public PlanAccepted {
                Objects.requireNonNull(plan, "plan must not be null");
            }
        }

        record Fallback(TeachingAction action) implements PedagogyDecision {

            public Fallback {
                Objects.requireNonNull(action, "action must not be null");
            }
        }
    }
}
