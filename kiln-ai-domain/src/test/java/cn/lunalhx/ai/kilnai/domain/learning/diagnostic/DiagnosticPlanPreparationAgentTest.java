package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DiagnosticPlanPreparationAgentTest {

    private static final UUID TARGET_CONCEPT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    void acceptsOnlyAPlanThatPassesTheTypeSpecificGate() {
        DiagnosticPlanPreparationAgent agent = new DiagnosticPlanPreparationAgent(
                ignored -> validPlanJson(3), context());

        DiagnosticPlanPreparationAgent.PreparationResult result = agent.prepare();

        DiagnosticPlanPreparationAgent.Accepted accepted =
                assertInstanceOf(DiagnosticPlanPreparationAgent.Accepted.class, result);
        assertEquals("calculus.polynomial-differentiation.diagnostic-plan@1.0.0",
                accepted.plan().pinnedId());
    }

    @Test
    void sourceGapStopsBeforeAnAcceptedPlanExists() {
        DiagnosticPlanPreparationAgent agent = new DiagnosticPlanPreparationAgent(
                ignored -> """
                        {
                          "schema": "diagnostic_plan_generation/v1",
                          "outcome": "source_gap",
                          "source_gap": {
                            "reason_code": "missing_operator_curated_passage",
                            "missing_requirement_ids": ["target-readiness-source"]
                          }
                        }
                        """, context());

        DiagnosticPlanPreparationAgent.PreparationResult result = agent.prepare();

        DiagnosticPlanPreparationAgent.SourceGap gap =
                assertInstanceOf(DiagnosticPlanPreparationAgent.SourceGap.class, result);
        assertEquals("missing_operator_curated_passage", gap.facts().reasonCode());
    }

    @Test
    void aGateRejectedPlanCannotBecomeAccepted() {
        DiagnosticPlanPreparationAgent agent = new DiagnosticPlanPreparationAgent(
                ignored -> validPlanJson(9), context());

        DiagnosticPlanPreparationAgent.PreparationResult result = agent.prepare();

        DiagnosticPlanPreparationAgent.Rejected rejected =
                assertInstanceOf(DiagnosticPlanPreparationAgent.Rejected.class, result);
        assertEquals(GateOutcome.REJECTED, rejected.gate().outcome());
    }

    private String validPlanJson(int maximumAttempts) {
        return """
                {
                  "schema": "diagnostic_plan_generation/v1",
                  "outcome": "plan_ready",
                  "plan": {
                    "schema": "diagnostic_plan/v1",
                    "id": "calculus.polynomial-differentiation.diagnostic-plan",
                    "version": "1.0.0",
                    "target_concept_id": "%s",
                    "concept_contract_id": "calculus.polynomial-differentiation",
                    "concept_contract_version": "1.0.0",
                    "mastery_rubric_id": "differentiate-polynomial",
                    "mastery_rubric_version": "1.0.0",
                    "target_readiness_criterion_ids": ["differentiate-polynomial"],
                    "supporting_concepts": [],
                    "dependency_order": [],
                    "source_basis": {
                      "source_pack_id": "openstax-calculus-v1-3.3",
                      "source_pack_version": "1.0.0",
                      "passage_ids": ["sec-3.3-differentiation-rules"]
                    },
                    "coverage_rule": {"kind": "all_target_readiness_criteria"},
                    "termination_rule": {"kind": "early_stop_when_readiness_complete"},
                    "rationale_policy": {"mode": "disabled", "criterion_ids": []},
                    "maximum_attempts": %d
                  }
                }
                """.formatted(TARGET_CONCEPT_ID, maximumAttempts);
    }

    private DiagnosticPlanGateContext context() {
        DiagnosticPlan.SourceBasis source = new DiagnosticPlan.SourceBasis(
                "openstax-calculus-v1-3.3", "1.0.0", List.of("sec-3.3-differentiation-rules"));
        return new DiagnosticPlanGateContext(
                TARGET_CONCEPT_ID,
                "calculus.polynomial-differentiation",
                "1.0.0",
                "differentiate-polynomial",
                "1.0.0",
                Set.of("differentiate-polynomial"),
                Set.of("differentiate-polynomial"),
                Set.of(),
                Set.of(source),
                Map.of());
    }
}
