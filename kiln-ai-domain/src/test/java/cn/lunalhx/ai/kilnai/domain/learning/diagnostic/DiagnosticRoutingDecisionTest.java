package cn.lunalhx.ai.kilnai.domain.learning.diagnostic;

import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticPlanFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticRoutingDecisionTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");

    @Test
    void aPassingObservationOfTheSingleCriterionAuthorizesFreshIndependent() {
        DiagnosticPlan plan = DiagnosticPlanFixture.acceptedPlan();
        DiagnosticFinding finding = finding(DiagnosticFinding.Kind.PASSING_OBSERVATION, List.of(), List.of());
        assertEquals(DiagnosticRoutingDecision.Route.FRESH_INDEPENDENT_TEST,
                DiagnosticRoutingDecision.decide(plan, List.of(finding)));
    }

    @Test
    void partialTargetReadinessCoverageContinuesDiagnosticInsteadOfRoutingEarly() {
        DiagnosticPlan plan = planWithCriteria(List.of("criterion-a", "criterion-b"), 3);
        DiagnosticFinding finding = finding(
                DiagnosticFinding.Kind.PASSING_OBSERVATION,
                List.of("criterion-a"), List.of(), List.of());

        assertEquals(DiagnosticRoutingDecision.Route.CONTINUE_DIAGNOSTIC,
                DiagnosticRoutingDecision.decide(plan, List.of(finding)));
    }

    @Test
    void oneDiagnosticTaskMayCoverEveryReadinessCriterion() {
        DiagnosticPlan plan = planWithCriteria(List.of("criterion-a", "criterion-b"), 3);
        DiagnosticFinding finding = finding(
                DiagnosticFinding.Kind.PASSING_OBSERVATION,
                List.of("criterion-a", "criterion-b"), List.of(), List.of());

        assertEquals(DiagnosticRoutingDecision.Route.FRESH_INDEPENDENT_TEST,
                DiagnosticRoutingDecision.decide(plan, List.of(finding)));
    }

    @Test
    void unconfirmedPerformanceGetsAnotherProbeOnlyWhileBudgetRemains() {
        DiagnosticPlan plan = planWithCriteria(List.of("criterion-a", "criterion-b"), 2);
        DiagnosticFinding first = finding(
                DiagnosticFinding.Kind.UNCONFIRMED_PERFORMANCE, List.of(), List.of(), List.of());
        DiagnosticFinding second = finding(
                DiagnosticFinding.Kind.UNCONFIRMED_PERFORMANCE, List.of(), List.of(), List.of());

        assertEquals(DiagnosticRoutingDecision.Route.CONTINUE_DIAGNOSTIC,
                DiagnosticRoutingDecision.decide(plan, List.of(first)));
        assertEquals(DiagnosticRoutingDecision.Route.TARGET_LEARNING_NEUTRAL,
                DiagnosticRoutingDecision.decide(plan, List.of(first, second)));
    }

    @Test
    void conclusiveTargetGapStopsDiagnosticBeforeTheRemainingBudgetIsUsed() {
        DiagnosticPlan plan = planWithCriteria(List.of("criterion-a", "criterion-b"), 8);
        DiagnosticFinding finding = finding(
                DiagnosticFinding.Kind.CONCLUSIVE_GAP, List.of(), List.of("criterion-b"), List.of("wrong_rule"));

        assertEquals(DiagnosticRoutingDecision.Route.TARGET_LEARNING_WITH_SUMMARY,
                DiagnosticRoutingDecision.decide(plan, List.of(finding)));
    }

    @Test
    void aConclusiveGapRoutesToLearningWithALearnerSafeSummary() {
        DiagnosticPlan plan = DiagnosticPlanFixture.acceptedPlan();
        DiagnosticFinding finding = finding(DiagnosticFinding.Kind.CONCLUSIVE_GAP,
                List.of("differentiate-polynomial"), List.of("wrong_rule"));
        assertEquals(DiagnosticRoutingDecision.Route.TARGET_LEARNING_WITH_SUMMARY,
                DiagnosticRoutingDecision.decide(plan, List.of(finding)));
        String summary = DiagnosticRoutingDecision.learnerSafeSummary(List.of(finding));
        assertTrue(summary.contains("教学重点"));
        assertFalse(summary.contains("wrong_rule"));
    }

    @Test
    void unconfirmedPerformanceRoutesNeutrallyWithoutADeficitClaim() {
        DiagnosticPlan plan = DiagnosticPlanFixture.acceptedPlan();
        DiagnosticFinding finding = finding(DiagnosticFinding.Kind.UNCONFIRMED_PERFORMANCE, List.of(), List.of());
        assertEquals(DiagnosticRoutingDecision.Route.CONTINUE_DIAGNOSTIC,
                DiagnosticRoutingDecision.decide(plan, List.of(finding)));
        String summary = DiagnosticRoutingDecision.learnerSafeSummary(List.of(finding));
        assertFalse(summary.contains("教学重点"));
    }

    private static DiagnosticFinding finding(
            DiagnosticFinding.Kind kind,
            List<String> missing,
            List<String> errors
    ) {
        return finding(kind, List.of("differentiate-polynomial"), missing, errors);
    }

    private static DiagnosticFinding finding(
            DiagnosticFinding.Kind kind,
            List<String> covered,
            List<String> missing,
            List<String> errors
    ) {
        return new DiagnosticFinding(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), kind,
                covered, missing, errors, NOW);
    }

    private static DiagnosticPlan planWithCriteria(List<String> criteria, int maximumAttempts) {
        DiagnosticPlan source = DiagnosticPlanFixture.acceptedPlan();
        return new DiagnosticPlan(
                source.schema(), source.id(), source.version(), source.targetConceptId(),
                source.conceptContractId(), source.conceptContractVersion(), source.masteryRubricId(),
                source.masteryRubricVersion(), criteria, source.supportingConcepts(), source.dependencyOrder(),
                source.sourceBasis(), source.coverageRule(), source.terminationRule(), source.rationalePolicy(),
                maximumAttempts);
    }
}
