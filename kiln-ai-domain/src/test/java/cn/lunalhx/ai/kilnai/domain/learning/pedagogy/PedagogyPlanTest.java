package cn.lunalhx.ai.kilnai.domain.learning.pedagogy;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDraftException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The closed {@code pedagogy_plan/v1} contract of the Pedagogy Agent: exactly
 * the declared fields, a non-blank feedback summary, intent, and reason, one
 * known Teaching Action, and non-blank tag lists. Unknown fields, unknown
 * actions, and blank content are rejected so invalid model text can never
 * influence routing.
 */
class PedagogyPlanTest {

    @Test
    void aValidPlanParsesWithItsClosedFields() {
        PedagogyPlan plan = PedagogyPlan.parse("""
                {
                  "schema": "pedagogy_plan/v1",
                  "feedback_summary": "请先学习下面的讲解。",
                  "action": "explain",
                  "intent": "remediate_diagnostic_failure",
                  "capability_tags": ["conceptual_reasoning"],
                  "strategy_tags": ["contrastive_explanation"],
                  "reason": "failed_diagnostic"
                }
                """);
        assertEquals("请先学习下面的讲解。", plan.feedbackSummary());
        assertEquals(TeachingAction.EXPLAIN, plan.action());
        assertEquals("remediate_diagnostic_failure", plan.intent());
        assertEquals(List.of("conceptual_reasoning"), plan.capabilityTags());
        assertEquals(List.of("contrastive_explanation"), plan.strategyTags());
        assertEquals("failed_diagnostic", plan.reason());
    }

    @Test
    void emptyTagListsAreValidButUnknownFieldsAreRejected() {
        PedagogyPlan plan = PedagogyPlan.parse("""
                {
                  "schema": "pedagogy_plan/v1",
                  "feedback_summary": "请完成一道新的练习题。",
                  "action": "apply_practice",
                  "intent": "practice_more",
                  "capability_tags": [],
                  "strategy_tags": [],
                  "reason": "no_pass_yet"
                }
                """);
        assertEquals(TeachingAction.APPLY_PRACTICE, plan.action());
        assertTrue(plan.capabilityTags().isEmpty());

        assertThrows(ApplyDraftException.class, () -> PedagogyPlan.parse("""
                {
                  "schema": "pedagogy_plan/v1",
                  "feedback_summary": "请完成一道新的练习题。",
                  "action": "apply_practice",
                  "intent": "practice_more",
                  "capability_tags": [],
                  "strategy_tags": [],
                  "reason": "no_pass_yet",
                  "extra": true
                }
                """), "unknown fields must be rejected");
    }

    @Test
    void malformedAndUnknownContentIsRejected() {
        assertThrows(ApplyDraftException.class, () -> PedagogyPlan.parse("not json"));
        assertThrows(ApplyDraftException.class, () -> PedagogyPlan.parse("""
                {
                  "schema": "pedagogy_plan/v1",
                  "feedback_summary": " ",
                  "action": "explain",
                  "intent": "remediate",
                  "capability_tags": [],
                  "strategy_tags": [],
                  "reason": "r"
                }
                """), "blank feedback must be rejected");
        assertThrows(ApplyDraftException.class, () -> PedagogyPlan.parse("""
                {
                  "schema": "pedagogy_plan/v1",
                  "feedback_summary": "f",
                  "action": "self_reward",
                  "intent": "remediate",
                  "capability_tags": [],
                  "strategy_tags": [],
                  "reason": "r"
                }
                """), "an unknown action must be rejected");
        assertThrows(ApplyDraftException.class, () -> PedagogyPlan.parse("""
                {
                  "schema": "pedagogy_plan/v1",
                  "feedback_summary": "f",
                  "action": "explain",
                  "intent": "remediate",
                  "capability_tags": [" "],
                  "strategy_tags": [],
                  "reason": "r"
                }
                """), "blank tag entries must be rejected");
    }
}
