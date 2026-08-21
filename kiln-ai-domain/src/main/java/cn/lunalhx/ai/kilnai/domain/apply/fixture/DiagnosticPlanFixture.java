package cn.lunalhx.ai.kilnai.domain.apply.fixture;

import cn.lunalhx.ai.kilnai.domain.apply.ModelProviderFailure;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.AcceptedDiagnosticPlanPort;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlan;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlanGateContext;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlanGatePolicy;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlanGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlanPreparationAgent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The approved source-backed Plan fixture used by the current reference
 * runtime. The provider still runs the Concept Preparation Agent and its
 * type-specific Gate before exposing the accepted Plan.
 */
public final class DiagnosticPlanFixture {

    private static final String PLAN_ID = "calculus.polynomial-differentiation.diagnostic-plan";
    private static final String PLAN_VERSION = "1.0.0";

    private DiagnosticPlanFixture() {
    }

    public static AcceptedDiagnosticPlanPort acceptedPlanPort() {
        return conceptId -> {
            if (!DiagnosticApplyFixture.CONCEPT_ID.equals(conceptId)) {
                return Optional.empty();
            }
            try {
                DiagnosticPlanPreparationAgent.PreparationResult result =
                        new DiagnosticPlanPreparationAgent(
                                ignored -> generationJson(candidate()), gateContext()).prepare();
                return result instanceof DiagnosticPlanPreparationAgent.Accepted accepted
                        ? Optional.of(accepted.plan())
                        : Optional.empty();
            } catch (RuntimeException preparationFailure) {
                if (ModelProviderFailure.isProviderOrConfiguration(preparationFailure)) {
                    return Optional.empty();
                }
                throw preparationFailure;
            }
        };
    }

    public static DiagnosticPlan acceptedPlan() {
        DiagnosticPlan candidate = candidate();
        GateResult<DiagnosticPlan> result = new TypedArtifactGatePipeline().validate(
                candidate, new DiagnosticPlanGatePolicy(gateContext()),
                GateContext.empty());
        if (result.outcome() != GateOutcome.PASSED) {
            throw new IllegalStateException("Diagnostic Plan fixture is not Gate-accepted: " + result.violations());
        }
        return result.artifact();
    }

    public static DiagnosticPlan acceptedPlanWithTargetCriteria(List<String> criterionIds) {
        List<String> targetCriteria = List.copyOf(criterionIds);
        DiagnosticPlan source = acceptedPlan();
        DiagnosticPlan candidate = new DiagnosticPlan(
                source.schema(),
                source.id(),
                source.version(),
                source.targetConceptId(),
                source.conceptContractId(),
                source.conceptContractVersion(),
                source.masteryRubricId(),
                source.masteryRubricVersion(),
                targetCriteria,
                source.supportingConcepts(),
                source.dependencyOrder(),
                source.sourceBasis(),
                source.coverageRule(),
                source.terminationRule(),
                new DiagnosticPlan.RationalePolicy(source.rationalePolicy().mode(), targetCriteria),
                source.maximumAttempts());
        DiagnosticPlanGateContext context = new DiagnosticPlanGateContext(
                source.targetConceptId(),
                source.conceptContractId(),
                source.conceptContractVersion(),
                source.masteryRubricId(),
                source.masteryRubricVersion(),
                Set.copyOf(targetCriteria),
                Set.copyOf(targetCriteria),
                Set.copyOf(targetCriteria),
                Set.of(sourceBasis()),
                Map.of());
        GateResult<DiagnosticPlan> result = new TypedArtifactGatePipeline().validate(
                candidate, new DiagnosticPlanGatePolicy(context), GateContext.empty());
        if (result.outcome() != GateOutcome.PASSED) {
            throw new IllegalStateException("Diagnostic Plan fixture is not Gate-accepted: " + result.violations());
        }
        return result.artifact();
    }

    private static DiagnosticPlan candidate() {
        return new DiagnosticPlan(
                DiagnosticPlan.SCHEMA,
                PLAN_ID,
                PLAN_VERSION,
                DiagnosticApplyFixture.CONCEPT_ID,
                "calculus.polynomial-differentiation",
                "1.0.0",
                "differentiate-polynomial",
                "1.0.0",
                List.of("differentiate-polynomial"),
                List.of(),
                List.of(),
                sourceBasis(),
                new DiagnosticPlan.CoverageRule("all_target_readiness_criteria"),
                new DiagnosticPlan.TerminationRule("early_stop_when_readiness_complete"),
                new DiagnosticPlan.RationalePolicy(
                        "primary_or_corroborated", List.of("differentiate-polynomial")),
                3);
    }

    private static String generationJson(DiagnosticPlan candidate) {
        return ApplyJson.writeContract(Map.of(
                "schema", DiagnosticPlanGenerationDraft.SCHEMA,
                "outcome", "plan_ready",
                "plan", candidate));
    }

    public static DiagnosticPlanGateContext gateContext() {
        return new DiagnosticPlanGateContext(
                DiagnosticApplyFixture.CONCEPT_ID,
                "calculus.polynomial-differentiation",
                "1.0.0",
                "differentiate-polynomial",
                "1.0.0",
                Set.of("differentiate-polynomial"),
                Set.of("differentiate-polynomial"),
                Set.of("differentiate-polynomial"),
                Set.of(sourceBasis()),
                Map.of());
    }

    public static DiagnosticPlan.SourceBasis sourceBasis() {
        return new DiagnosticPlan.SourceBasis(
                "openstax-calculus-v1-3.3", "1.0.0", List.of(DiagnosticApplyFixture.PASSAGE_ID));
    }
}
