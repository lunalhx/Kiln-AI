package cn.lunalhx.ai.kilnai.domain.apply.fixture;

import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainExecutionContext;

import java.util.List;

/**
 * The frozen {@code explain_execution_context/v1} template for the existing
 * polynomial-differentiation reference. It reuses the Concept Contract,
 * Mastery Rubric, and approved Concept Source Pack of the Apply fixtures and
 * supplies the sanitized pedagogy intent of the deterministic remediation
 * entry (a failed Diagnostic with no Feedback Facts yet). Novelty exclusions
 * are supplied per invocation by the Explain flow and are never part of the
 * frozen template.
 */
public final class ExplainApplyFixture {

    private ExplainApplyFixture() {
    }

    public static ExplainExecutionContext explainContext() {
        return new ExplainExecutionContext(
                "explain_execution_context/v1",
                new ExplainExecutionContext.ConceptContract(
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
                new ExplainExecutionContext.MasteryRubric(
                        "differentiate-polynomial",
                        "1.0.0",
                        List.of(new ExplainExecutionContext.RubricCriterion(
                                "differentiate-polynomial",
                                "Differentiate an in-scope polynomial function correctly."))),
                new ExplainExecutionContext.PedagogyIntent(
                        "remediate_diagnostic_failure",
                        List.of(),
                        List.of(),
                        List.of()),
                new ExplainExecutionContext.ConceptSourcePack(
                        "openstax-calculus-v1-3.3",
                        "1.0.0",
                        List.of(new ExplainExecutionContext.SourcePassage(
                                "openstax-calculus-v1",
                                "1.0.0",
                                "sec-3.3-differentiation-rules",
                                "en",
                                "Differentiation rules. The derivative of a constant function is zero. "
                                        + "For a constant c and a differentiable function f, "
                                        + "the derivative of c times f(x) equals c times f'(x). "
                                        + "The derivative of a sum or difference f(x) plus or minus g(x) "
                                        + "is f'(x) plus or minus g'(x). "
                                        + "The power rule states that the derivative of x raised to a "
                                        + "constant real power n is n times x raised to n minus one."))),
                new ExplainExecutionContext.NoveltyExclusions(List.of()),
                "zh-CN");
    }
}
