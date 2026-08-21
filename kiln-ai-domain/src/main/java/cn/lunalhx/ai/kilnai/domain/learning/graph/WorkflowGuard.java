package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.TeachingAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The deterministic Workflow Guard (CONTEXT.md): it derives the closed legal
 * next-move set from committed state at every guarded decision node —
 * Target Learning and Practice after Diagnostic, Explain completion, Practice or Teach-back results, H5
 * reveal, and readiness — so the Pedagogy Agent can choose only among valid
 * transitions. The guard, never the agent, owns legality and
 * state-transition authorization, and it bypasses model-based pedagogy
 * selection whenever exactly one move is legal.
 *
 * <p>Legality is derived from committed state by these rules:
 * <ul>
 *   <li>{@link TeachingAction#EXPLAIN} is legal only directly after a
 *       conclusive Diagnostic, Practice, or Teach-back failure.</li>
 *   <li>{@link TeachingAction#APPLY_PRACTICE} is legal whenever a fresh
 *       verified Practice task may be delivered — every decision context
 *       except the two Inconclusive replacements, which mandate their own
 *       fresh task, and the temporary-Explain resume inside an open
 *       Attempt.</li>
 *   <li>{@link TeachingAction#TEACH_BACK} is legal only while the Flow
 *       carries an eligible anchor (the Guard does not offer Teach-back
 *       without one), and never at the Inconclusive replacements.</li>
 *   <li>{@link TeachingAction#INDEPENDENT_TEST} is legal only once the
 *       qualifying Apply Practice pass prerequisite of the current
 *       remediation cycle is satisfied.</li>
 *   <li>{@link TeachingAction#RESUME_PRACTICE} is the single legal move
 *       after a temporary Explain shown inside an open Apply Practice
 *       Attempt: a new task can never open while one is open.</li>
 *   <li>A Practice or Teach-back Inconclusive judgment has exactly one legal
 *       move — the mandated fresh replacement task of the same kind — so the
 *       model is bypassed.</li>
 * </ul>
 *
 * <p>The deterministic fallback per decision context follows the
 * Learning/Practice reference spec: failures fall back to Explain; Explain
 * completion and Teach-back passes without a qualifying Practice pass fall
 * back to fresh Apply Practice; an H5 reveal falls back to Teach-back; a
 * qualifying Practice pass falls back to the fresh Independent Test; and the
 * temporary-Explain resume falls back to the same open Practice interaction.
 * The legal set is ordered fallback-first so the serialized context's first
 * entry is the deterministic default.
 */
public final class WorkflowGuard {

    public enum DecisionContext {
        TARGET_LEARNING_AND_PRACTICE,
        EXPLAIN_COMPLETED,
        H5_REVEALED,
        PRACTICE_PASSED,
        PRACTICE_FAILED,
        PRACTICE_INCONCLUSIVE,
        TEACH_BACK_PASSED,
        TEACH_BACK_FAILED,
        TEACH_BACK_INCONCLUSIVE,
        INDEPENDENT_FAILED
    }

    /**
     * The committed-state facts that shape legality: whether the Flow carries
     * an eligible Teach-back anchor, whether an Apply Practice Attempt is
     * currently open, and whether the qualifying Practice pass prerequisite
     * of the current remediation cycle is satisfied.
     */
    public record GuardFacts(
            boolean teachBackEligible,
            boolean openPracticeAttempt,
            boolean readinessSatisfied
    ) {

        public static GuardFacts none() {
            return new GuardFacts(false, false, false);
        }
    }

    /**
     * The closed legal-move set (ordered fallback-first for the serialized
     * context) and the deterministic fallback for the decision context.
     */
    public record LegalMoves(List<TeachingAction> legalActions, TeachingAction fallback) {

        public LegalMoves {
            Objects.requireNonNull(legalActions, "legalActions must not be null");
            Objects.requireNonNull(fallback, "fallback must not be null");
            legalActions = List.copyOf(legalActions);
            if (legalActions.isEmpty() || !legalActions.contains(fallback)) {
                throw new IllegalArgumentException("the fallback must be a member of the legal set");
            }
        }

        public boolean single() {
            return legalActions.size() == 1;
        }
    }

    public LegalMoves derive(DecisionContext context, GuardFacts facts) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(facts, "facts must not be null");
        return switch (context) {
            case TARGET_LEARNING_AND_PRACTICE -> moves(List.of(
                    TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE), TeachingAction.EXPLAIN);
            case EXPLAIN_COMPLETED -> facts.openPracticeAttempt()
                    ? moves(List.of(TeachingAction.RESUME_PRACTICE), TeachingAction.RESUME_PRACTICE)
                    : moves(withReadiness(
                            List.of(TeachingAction.APPLY_PRACTICE, TeachingAction.TEACH_BACK), facts),
                            TeachingAction.APPLY_PRACTICE);
            case H5_REVEALED -> moves(withReadiness(
                    List.of(TeachingAction.TEACH_BACK, TeachingAction.APPLY_PRACTICE), facts),
                    TeachingAction.TEACH_BACK);
            case PRACTICE_PASSED -> moves(withAnchor(
                    List.of(TeachingAction.INDEPENDENT_TEST, TeachingAction.APPLY_PRACTICE), facts),
                    TeachingAction.INDEPENDENT_TEST);
            case PRACTICE_FAILED -> moves(withReadiness(withAnchor(
                    List.of(TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE), facts), facts),
                    TeachingAction.EXPLAIN);
            case PRACTICE_INCONCLUSIVE -> moves(List.of(TeachingAction.APPLY_PRACTICE),
                    TeachingAction.APPLY_PRACTICE);
            case TEACH_BACK_PASSED -> moves(withReadiness(withAnchor(
                    List.of(TeachingAction.APPLY_PRACTICE), facts), facts),
                    TeachingAction.APPLY_PRACTICE);
            case TEACH_BACK_FAILED -> moves(withReadiness(withAnchor(
                    List.of(TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE), facts), facts),
                    TeachingAction.EXPLAIN);
            case TEACH_BACK_INCONCLUSIVE -> moves(List.of(TeachingAction.TEACH_BACK),
                    TeachingAction.TEACH_BACK);
            case INDEPENDENT_FAILED -> moves(List.of(
                    TeachingAction.EXPLAIN, TeachingAction.APPLY_PRACTICE), TeachingAction.EXPLAIN);
        };
    }

    private static LegalMoves moves(List<TeachingAction> legalActions, TeachingAction fallback) {
        return new LegalMoves(legalActions, fallback);
    }

    private static List<TeachingAction> withAnchor(
            List<TeachingAction> base,
            GuardFacts facts
    ) {
        if (!facts.teachBackEligible() || base.contains(TeachingAction.TEACH_BACK)) {
            return base;
        }
        List<TeachingAction> extended = new ArrayList<>(base);
        extended.add(TeachingAction.TEACH_BACK);
        return List.copyOf(extended);
    }

    private static List<TeachingAction> withReadiness(
            List<TeachingAction> base,
            GuardFacts facts
    ) {
        if (!facts.readinessSatisfied() || base.contains(TeachingAction.INDEPENDENT_TEST)) {
            return base;
        }
        List<TeachingAction> extended = new ArrayList<>(base);
        extended.add(TeachingAction.INDEPENDENT_TEST);
        return List.copyOf(extended);
    }
}
