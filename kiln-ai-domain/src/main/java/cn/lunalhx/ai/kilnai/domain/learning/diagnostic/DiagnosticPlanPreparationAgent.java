package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;

import java.util.List;
import java.util.Objects;

/**
 * Bounded Concept Preparation Agent for the initial Diagnostic Plan. It
 * parses the provider's closed result, returns Source Gap explicitly, and
 * exposes a Plan only after the type-specific Gate has passed. This class has
 * no persistence dependency and therefore cannot leave partial Flow state.
 */
public final class DiagnosticPlanPreparationAgent {

    private final DiagnosticPlanGenerationPort generationPort;
    private final DiagnosticPlanGateContext gateContext;
    private final TypedArtifactGatePipeline gatePipeline = new TypedArtifactGatePipeline();

    public DiagnosticPlanPreparationAgent(
            DiagnosticPlanGenerationPort generationPort,
            DiagnosticPlanGateContext gateContext
    ) {
        this.generationPort = Objects.requireNonNull(generationPort, "generationPort must not be null");
        this.gateContext = Objects.requireNonNull(gateContext, "gateContext must not be null");
    }

    public PreparationResult prepare() {
        DiagnosticPlanGenerationDraft draft;
        try {
            draft = DiagnosticPlanGenerationDraft.parse(generationPort.generatePlan(gateContext));
        } catch (ApplyDraftException exception) {
            return new Rejected(GateResult.rejected(List.of(
                    new GateViolation("plan.contract", "Diagnostic Plan generation contract is invalid"))));
        }
        if (draft instanceof DiagnosticPlanGenerationDraft.SourceGap sourceGap) {
            return new SourceGap(sourceGap.facts());
        }
        DiagnosticPlan candidate = ((DiagnosticPlanGenerationDraft.PlanReady) draft).plan();
        GateResult<DiagnosticPlan> gate = gatePipeline.validate(
                candidate, new DiagnosticPlanGatePolicy(gateContext), GateContext.empty());
        return gate.outcome() == GateOutcome.PASSED ? new Accepted(gate.artifact()) : new Rejected(gate);
    }

    public sealed interface PreparationResult permits Accepted, Rejected, SourceGap {
    }

    public record Accepted(DiagnosticPlan plan) implements PreparationResult {
        public Accepted {
            Objects.requireNonNull(plan, "plan must not be null");
        }
    }

    public record Rejected(GateResult<DiagnosticPlan> gate) implements PreparationResult {
        public Rejected {
            Objects.requireNonNull(gate, "gate must not be null");
        }
    }

    public record SourceGap(DiagnosticPlanGenerationDraft.SourceGapFacts facts) implements PreparationResult {
        public SourceGap {
            Objects.requireNonNull(facts, "facts must not be null");
        }
    }
}
