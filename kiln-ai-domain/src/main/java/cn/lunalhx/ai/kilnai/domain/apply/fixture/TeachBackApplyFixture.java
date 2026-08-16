package cn.lunalhx.ai.kilnai.domain.apply.fixture;

import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;

import java.util.List;
import java.util.UUID;

/**
 * The frozen {@code teach_back_execution_context/v1} template for the
 * existing polynomial-differentiation reference. It reuses the Concept
 * Contract, Mastery Rubric, and pedagogy intent of the Apply fixtures; the
 * eligible anchor is supplied per invocation by the Teach-back flow and is
 * never part of the frozen template.
 */
public final class TeachBackApplyFixture {

    public static final UUID PLACEHOLDER_ANCHOR_ID =
            UUID.fromString("00000000-0000-0000-0000-00000000a5a5");

    private TeachBackApplyFixture() {
    }

    public static TeachBackExecutionContext teachBackContext() {
        return new TeachBackExecutionContext(
                "teach_back_execution_context/v1",
                new TeachBackExecutionContext.ConceptContract(
                        "calculus.polynomial-differentiation",
                        "1.0.0",
                        List.of(
                                "constant rule",
                                "constant-multiple rule",
                                "sum and difference rules",
                                "power rule for polynomial terms"),
                        List.of(
                                "product rule",
                                "quotient rule",
                                "chain rule",
                                "trigonometric functions",
                                "exponential functions",
                                "logarithmic functions")),
                new TeachBackExecutionContext.MasteryRubric(
                        "differentiate-polynomial",
                        "1.0.0",
                        List.of(new TeachBackExecutionContext.RubricCriterion(
                                "differentiate-polynomial",
                                "Differentiate an in-scope polynomial function correctly."))),
                new TeachBackExecutionContext.PedagogyIntent(
                        "remediate_diagnostic_failure",
                        List.of(),
                        List.of(),
                        List.of()),
                new TeachBackExecutionContext.AnchorView(
                        PLACEHOLDER_ANCHOR_ID,
                        "EXPLAIN_WORKED_EXAMPLE",
                        "",
                        List.of()),
                "zh-CN");
    }
}
