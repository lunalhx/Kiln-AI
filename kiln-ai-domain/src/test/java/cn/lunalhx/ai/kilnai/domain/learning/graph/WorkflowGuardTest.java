package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.TeachingAction;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static cn.lunalhx.ai.kilnai.domain.learning.graph.WorkflowGuard.DecisionContext;
import static cn.lunalhx.ai.kilnai.domain.learning.graph.WorkflowGuard.GuardFacts;
import static cn.lunalhx.ai.kilnai.domain.learning.graph.WorkflowGuard.LegalMoves;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The deterministic Workflow Guard: it derives the closed legal next-move set
 * from committed state at every guarded decision node and orders the
 * deterministic fallback first. The Inconclusive replacements and the
 * temporary-Explain resume are single-move contexts that must bypass the
 * model, and Independent testing is legal only once readiness is satisfied.
 */
class WorkflowGuardTest {

    private final WorkflowGuard guard = new WorkflowGuard();

    @Test
    void aDiagnosticNotPassedOffersExplainAndFreshPracticeWithExplainAsFallback() {
        LegalMoves moves = guard.derive(DecisionContext.DIAGNOSTIC_NOT_PASSED, GuardFacts.none());
        assertEquals(Set.of(TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE),
                Set.copyOf(moves.legalActions()));
        assertEquals(TeachingAction.EXPLAIN, moves.fallback());
        assertEquals(TeachingAction.EXPLAIN, moves.legalActions().get(0),
                "the deterministic fallback is ordered first for the serialized context");
        assertFalse(moves.single());
    }

    @Test
    void explainCompletionOffersFreshPracticeAndTeachBackAndResumesWhenAnAttemptIsOpen() {
        LegalMoves moves = guard.derive(DecisionContext.EXPLAIN_COMPLETED,
                new GuardFacts(true, false, false));
        assertEquals(Set.of(TeachingAction.APPLY_PRACTICE, TeachingAction.TEACH_BACK),
                Set.copyOf(moves.legalActions()));
        assertEquals(TeachingAction.APPLY_PRACTICE, moves.fallback());

        LegalMoves resume = guard.derive(DecisionContext.EXPLAIN_COMPLETED,
                new GuardFacts(true, true, false));
        assertEquals(Set.of(TeachingAction.RESUME_PRACTICE), Set.copyOf(resume.legalActions()));
        assertEquals(TeachingAction.RESUME_PRACTICE, resume.fallback());
        assertTrue(resume.single(), "a new task can never open while one is open");
    }

    @Test
    void teachBackIsNeverOfferedWithoutAnEligibleAnchor() {
        LegalMoves noAnchor = guard.derive(DecisionContext.PRACTICE_FAILED,
                new GuardFacts(false, false, false));
        assertFalse(noAnchor.legalActions().contains(TeachingAction.TEACH_BACK),
                "the Guard does not offer Teach-back without an eligible anchor");
        LegalMoves withAnchor = guard.derive(DecisionContext.PRACTICE_FAILED,
                new GuardFacts(true, false, false));
        assertTrue(withAnchor.legalActions().contains(TeachingAction.TEACH_BACK));
    }

    @Test
    void independentTestingIsLegalOnlyOnceReadinessIsSatisfied() {
        LegalMoves notReady = guard.derive(DecisionContext.PRACTICE_FAILED,
                new GuardFacts(true, false, false));
        assertFalse(notReady.legalActions().contains(TeachingAction.INDEPENDENT_TEST),
                "a conclusive fail must not make Independent testing legal without a qualifying pass");
        LegalMoves ready = guard.derive(DecisionContext.PRACTICE_FAILED,
                new GuardFacts(true, false, true));
        assertTrue(ready.legalActions().contains(TeachingAction.INDEPENDENT_TEST),
                "once the prerequisite is satisfied, Independent stays legal among the guarded moves");
        assertEquals(TeachingAction.EXPLAIN, ready.fallback());
    }

    @Test
    void theInconclusiveReplacementsAreSingleMovesThatBypassTheModel() {
        LegalMoves practiceInconclusive = guard.derive(DecisionContext.PRACTICE_INCONCLUSIVE,
                new GuardFacts(true, false, true));
        assertEquals(Set.of(TeachingAction.APPLY_PRACTICE), Set.copyOf(practiceInconclusive.legalActions()));
        assertTrue(practiceInconclusive.single());
        assertEquals(TeachingAction.APPLY_PRACTICE, practiceInconclusive.fallback());

        LegalMoves teachBackInconclusive = guard.derive(DecisionContext.TEACH_BACK_INCONCLUSIVE,
                new GuardFacts(true, false, true));
        assertEquals(Set.of(TeachingAction.TEACH_BACK), Set.copyOf(teachBackInconclusive.legalActions()));
        assertTrue(teachBackInconclusive.single());
    }

    @Test
    void everyFallbackIsAlwaysAMemberOfItsLegalSet() {
        for (DecisionContext context : DecisionContext.values()) {
            for (boolean anchor : new boolean[]{false, true}) {
                for (boolean readiness : new boolean[]{false, true}) {
                    LegalMoves moves = guard.derive(context, new GuardFacts(anchor, false, readiness));
                    assertTrue(moves.legalActions().contains(moves.fallback()),
                            "the fallback of " + context + " must be a member of its legal set");
                }
            }
        }
    }

    @Test
    void anIndependentFailureStartsRemediationWithExplainAndFreshPracticeOnly() {
        LegalMoves moves = guard.derive(DecisionContext.INDEPENDENT_FAILED, GuardFacts.none());
        assertEquals(Set.of(TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE),
                Set.copyOf(moves.legalActions()),
                "a conclusive no-hint Independent fail must begin remediation, never a fresh Independent");
        assertEquals(TeachingAction.EXPLAIN, moves.fallback());
        assertFalse(moves.legalActions().contains(TeachingAction.INDEPENDENT_TEST));
        assertFalse(moves.single());
    }

    @Test
    void h5RevealAndTheTeachBackResultsMatchTheSpecifiedFallbacks() {
        assertEquals(TeachingAction.TEACH_BACK,
                guard.derive(DecisionContext.H5_REVEALED, GuardFacts.none()).fallback());
        assertEquals(TeachingAction.APPLY_PRACTICE,
                guard.derive(DecisionContext.TEACH_BACK_PASSED, GuardFacts.none()).fallback());
        assertEquals(TeachingAction.EXPLAIN,
                guard.derive(DecisionContext.TEACH_BACK_FAILED, GuardFacts.none()).fallback());
        assertEquals(TeachingAction.INDEPENDENT_TEST,
                guard.derive(DecisionContext.PRACTICE_PASSED, GuardFacts.none()).fallback());
    }
}
