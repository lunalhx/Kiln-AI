package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticPlanGatePolicyTest {

    private static final UUID TARGET_CONCEPT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    void aGateAcceptedPlanSupportsTheSmallestAndLargestAllowedMaximum() {
        for (int maximumAttempts : List.of(1, 8)) {
            GateResult<DiagnosticPlan> result = gate(plan(maximumAttempts), context());

            assertEquals(GateOutcome.PASSED, result.outcome(),
                    "maximum " + maximumAttempts + " should be accepted");
        }
    }

    @Test
    void aPlanWhoseWorstCaseExceedsEightAttemptsIsRejected() {
        GateResult<DiagnosticPlan> result = gate(plan(9), context());

        assertRejectedWith(result, "attempts.maximum");
    }

    @Test
    void aPlanWithAnUnsupportedSourceBasisIsRejected() {
        DiagnosticPlan unsupported = copy(plan(3),
                new DiagnosticPlan.SourceBasis("unapproved-source", "1.0.0", List.of("passage")));

        assertRejectedWith(gate(unsupported, context()), "source.ungrounded");
    }

    @Test
    void anUnsafeTargetReadinessSetIsRejected() {
        DiagnosticPlan unsafe = copyReadiness(plan(3), List.of());

        assertRejectedWith(gate(unsafe, context()), "readiness.empty");
    }

    @Test
    void aReadinessCriterionThatExpandsTheMasteryRubricIsRejected() {
        DiagnosticPlan expanded = copyReadiness(plan(3), List.of("unprepared-criterion"));

        assertRejectedWith(gate(expanded, context()), "rubric.expansion");
    }

    @Test
    void anUnjustifiedRationalePolicyIsRejected() {
        DiagnosticPlan rationale = copy(plan(3),
                new DiagnosticPlan.RationalePolicy("primary_or_corroborated", List.of("differentiate-polynomial")));

        assertRejectedWith(gate(rationale, context()), "rationale.unjustified");
    }

    @Test
    void anUnknownSupportingConceptReferenceIsRejected() {
        DiagnosticPlan plan = copySupporting(plan(3), List.of(new DiagnosticPlan.SupportingConcept(
                "unknown-supporting-concept", true, "rubric", "1.0.0", "criterion", sourceBasis(), List.of())));

        assertRejectedWith(gate(plan, context()), "supporting.reference");
    }

    @Test
    void cyclicSupportingConceptDependenciesAreRejected() {
        DiagnosticPlan.SourceBasis supportSource = new DiagnosticPlan.SourceBasis(
                "support-source", "1.0.0", List.of("support-passage"));
        DiagnosticPlan.SupportingConcept first = new DiagnosticPlan.SupportingConcept(
                "support-a", true, "support-rubric-a", "1.0.0", "support-a-criterion", supportSource,
                List.of("support-b"));
        DiagnosticPlan.SupportingConcept second = new DiagnosticPlan.SupportingConcept(
                "support-b", true, "support-rubric-b", "1.0.0", "support-b-criterion", supportSource,
                List.of("support-a"));
        DiagnosticPlan cyclic = copy(plan(3), List.of(first, second), List.of("support-a", "support-b"));

        assertRejectedWith(gate(cyclic, supportingContext(supportSource)), "dependency.cycle");
    }

    private GateResult<DiagnosticPlan> gate(DiagnosticPlan plan, DiagnosticPlanGateContext context) {
        return new TypedArtifactGatePipeline().validate(
                plan, new DiagnosticPlanGatePolicy(context), GateContext.empty());
    }

    private DiagnosticPlan plan(int maximumAttempts) {
        return new DiagnosticPlan(
                DiagnosticPlan.SCHEMA,
                "calculus.polynomial-differentiation.diagnostic-plan",
                "1.0.0",
                TARGET_CONCEPT_ID,
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
                new DiagnosticPlan.RationalePolicy("disabled", List.of()),
                maximumAttempts);
    }

    private DiagnosticPlan copy(DiagnosticPlan source, DiagnosticPlan.SourceBasis sourceBasis) {
        return new DiagnosticPlan(source.schema(), source.id(), source.version(), source.targetConceptId(),
                source.conceptContractId(), source.conceptContractVersion(), source.masteryRubricId(),
                source.masteryRubricVersion(), source.targetReadinessCriterionIds(), source.supportingConcepts(),
                source.dependencyOrder(), sourceBasis, source.coverageRule(), source.terminationRule(),
                source.rationalePolicy(), source.maximumAttempts());
    }

    private DiagnosticPlan copyReadiness(DiagnosticPlan source, List<String> readiness) {
        return new DiagnosticPlan(source.schema(), source.id(), source.version(), source.targetConceptId(),
                source.conceptContractId(), source.conceptContractVersion(), source.masteryRubricId(),
                source.masteryRubricVersion(), readiness, source.supportingConcepts(), source.dependencyOrder(),
                source.sourceBasis(), source.coverageRule(), source.terminationRule(), source.rationalePolicy(),
                source.maximumAttempts());
    }

    private DiagnosticPlan copySupporting(DiagnosticPlan source, List<DiagnosticPlan.SupportingConcept> supportingConcepts) {
        return new DiagnosticPlan(source.schema(), source.id(), source.version(), source.targetConceptId(),
                source.conceptContractId(), source.conceptContractVersion(), source.masteryRubricId(),
                source.masteryRubricVersion(), source.targetReadinessCriterionIds(), supportingConcepts,
                source.dependencyOrder(), source.sourceBasis(), source.coverageRule(), source.terminationRule(),
                source.rationalePolicy(), source.maximumAttempts());
    }

    private DiagnosticPlan copy(DiagnosticPlan source, DiagnosticPlan.RationalePolicy rationalePolicy) {
        return new DiagnosticPlan(source.schema(), source.id(), source.version(), source.targetConceptId(),
                source.conceptContractId(), source.conceptContractVersion(), source.masteryRubricId(),
                source.masteryRubricVersion(), source.targetReadinessCriterionIds(), source.supportingConcepts(),
                source.dependencyOrder(), source.sourceBasis(), source.coverageRule(), source.terminationRule(),
                rationalePolicy, source.maximumAttempts());
    }

    private DiagnosticPlan copy(
            DiagnosticPlan source,
            List<DiagnosticPlan.SupportingConcept> supportingConcepts,
            List<String> dependencyOrder
    ) {
        return new DiagnosticPlan(source.schema(), source.id(), source.version(), source.targetConceptId(),
                source.conceptContractId(), source.conceptContractVersion(), source.masteryRubricId(),
                source.masteryRubricVersion(), source.targetReadinessCriterionIds(), supportingConcepts,
                dependencyOrder, source.sourceBasis(), source.coverageRule(), source.terminationRule(),
                source.rationalePolicy(), source.maximumAttempts());
    }

    private DiagnosticPlanGateContext context() {
        return new DiagnosticPlanGateContext(
                TARGET_CONCEPT_ID,
                "calculus.polynomial-differentiation",
                "1.0.0",
                "differentiate-polynomial",
                "1.0.0",
                Set.of("differentiate-polynomial"),
                Set.of("differentiate-polynomial"),
                Set.of(),
                Set.of(sourceBasis()),
                Map.of());
    }

    private DiagnosticPlanGateContext supportingContext(DiagnosticPlan.SourceBasis source) {
        return new DiagnosticPlanGateContext(
                TARGET_CONCEPT_ID,
                "calculus.polynomial-differentiation",
                "1.0.0",
                "differentiate-polynomial",
                "1.0.0",
                Set.of("differentiate-polynomial"),
                Set.of("differentiate-polynomial"),
                Set.of(),
                Set.of(sourceBasis(), source),
                Map.of(
                        "support-a", new DiagnosticPlanGateContext.SupportingConceptFacts(
                                "support-a", "support-rubric-a", "1.0.0", "support-a-criterion", source),
                        "support-b", new DiagnosticPlanGateContext.SupportingConceptFacts(
                                "support-b", "support-rubric-b", "1.0.0", "support-b-criterion", source)));
    }

    private void assertRejectedWith(GateResult<DiagnosticPlan> result, String code) {
        assertEquals(GateOutcome.REJECTED, result.outcome());
        assertTrue(result.violations().stream().anyMatch(violation -> code.equals(violation.code())),
                () -> "missing violation " + code + ": " + result.violations());
    }

    private DiagnosticPlan.SourceBasis sourceBasis() {
        return new DiagnosticPlan.SourceBasis("openstax-calculus-v1-3.3", "1.0.0",
                List.of("sec-3.3-differentiation-rules"));
    }
}
