package cn.lunalhx.ai.kilnai.domain.apply.fixture;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.List;
import java.util.UUID;

public final class DiagnosticApplyFixture {

    public static final String PASSAGE_ID = "sec-3.3-differentiation-rules";
    public static final UUID CONCEPT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    private DiagnosticApplyFixture() {
    }

    public static ApplyExecutionContext diagnosticContext() {
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
                        "apply.polynomial-differentiation.diagnostic",
                        "1.0.0",
                        AttemptPurpose.DIAGNOSTIC,
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
                        ApplyExecutionContext.TaskBlueprint.DIAGNOSTIC_PRIMARY_OR_CORROBORATED_RATIONALE_POLICY,
                        ApplyExecutionContext.TaskBlueprint.MATHEMATICAL_EQUIVALENCE_CHECK),
                new ApplyExecutionContext.ConceptSourcePack(
                        "openstax-calculus-v1-3.3",
                        "1.0.0",
                        List.of(new ApplyExecutionContext.SourcePassage(
                                "openstax-calculus-v1",
                                "1.0.0",
                                PASSAGE_ID,
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
