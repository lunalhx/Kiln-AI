package cn.lunalhx.ai.kilnai.domain.apply.fixture;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.List;

/**
 * The frozen {@code kiln.task-blueprint/v1} Practice Blueprint for the
 * existing polynomial-differentiation reference. It reuses the Independent
 * Blueprint's Concept, Mastery Rubric criteria, source grounding, task shape,
 * mathematical scope, notation, answer representation, required derivative,
 * and optional rationale, and only changes the Attempt Purpose to Practice,
 * the assessment policy reference to the Practice final-derivative policy,
 * and the Blueprint identity. Novelty exclusions are supplied per invocation
 * by the Practice flow and are never part of the frozen template.
 */
public final class PracticeApplyFixture {

    public static final String BLUEPRINT_PINNED_ID = "apply.polynomial-differentiation.practice@1.0.0";

    private PracticeApplyFixture() {
    }

    public static ApplyExecutionContext practiceContext() {
        return new ApplyExecutionContext(
                "apply_execution_context/v1",
                new ApplyExecutionContext.ConceptContract(
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
                new ApplyExecutionContext.MasteryRubric(
                        "differentiate-polynomial",
                        "1.0.0",
                        List.of(new ApplyExecutionContext.RubricCriterion(
                                "differentiate-polynomial",
                                "Differentiate an in-scope polynomial function correctly."))),
                new ApplyExecutionContext.TaskBlueprint(
                        "apply.polynomial-differentiation.practice",
                        "1.0.0",
                        AttemptPurpose.PRACTICE,
                        new ApplyExecutionContext.TaskShape(
                                1,
                                "direct_symbolic_expression",
                                "forbidden",
                                "forbidden",
                                "forbidden",
                                "forbidden",
                                "forbidden"),
                        new ApplyExecutionContext.MathematicalScope(
                                "x",
                                "polynomial",
                                new ApplyExecutionContext.Range(3, 4),
                                new ApplyExecutionContext.Range(2, 4),
                                new ApplyExecutionContext.CoefficientConstraints("nonzero_integer", -9, 9),
                                true),
                        new ApplyExecutionContext.ResponseFields("required", "optional"),
                        "practice.final-derivative@1",
                        null),
                new ApplyExecutionContext.ConceptSourcePack(
                        "openstax-calculus-v1-3.3",
                        "1.0.0",
                        List.of(new ApplyExecutionContext.SourcePassage(
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
                new ApplyExecutionContext.NoveltyExclusions(List.of(), List.of(), List.of(), List.of(), List.of()),
                new ApplyExecutionContext.AnswerRepresentationContract(
                        "mathematical-expression.x",
                        "1.0.0",
                        "mathematical_expression",
                        List.of("x"),
                        List.of("plain_text", "unicode_math", "latex_like")),
                "zh-CN");
    }
}
