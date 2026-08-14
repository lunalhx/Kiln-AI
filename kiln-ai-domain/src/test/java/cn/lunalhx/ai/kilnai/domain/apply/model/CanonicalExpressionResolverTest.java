package cn.lunalhx.ai.kilnai.domain.apply.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalExpressionResolverTest {

    private static final List<String> VARIABLES = List.of("x");

    @Test
    void plainTextWithExplicitMultiplicationIsCanonicalized() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("12*x^2 - 6*x + 7", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12*x^2 - 6*x + 7", resolution.get().canonical());
        assertEquals(AnswerInputFamily.PLAIN_TEXT, resolution.get().family());
    }

    @Test
    void plainTextWithImplicitMultiplicationIsMadeExplicit() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("12x^2-6x+7", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12*x^2-6*x+7", resolution.get().canonical());
        assertEquals(AnswerInputFamily.PLAIN_TEXT, resolution.get().family());
    }

    @Test
    void aLeadingDerivativeNotationPrefixIsStripped() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("f'(x) = 12*x^2 - 6*x + 7", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12*x^2 - 6*x + 7", resolution.get().canonical());
    }

    @Test
    void unicodeSuperscriptsAndMinusSignAreNormalized() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("12x\u00b2\u22126x+7", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12*x^2-6*x+7", resolution.get().canonical());
        assertEquals(AnswerInputFamily.UNICODE_MATH, resolution.get().family());
    }

    @Test
    void unicodeTimesSymbolsAreNormalized() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("3\u00d7x + 2\u00b7x", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("3*x + 2*x", resolution.get().canonical());
        assertEquals(AnswerInputFamily.UNICODE_MATH, resolution.get().family());
    }

    @Test
    void aMultiDigitUnicodeExponentIsNormalized() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("x\u00b9\u00b2", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("x^12", resolution.get().canonical());
    }

    @Test
    void unicodeDerivativeNotationIsStripped() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("f\u2032(x)=12x\u00b2\u22126x+7", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12*x^2-6*x+7", resolution.get().canonical());
        assertEquals(AnswerInputFamily.UNICODE_MATH, resolution.get().family());
    }

    @Test
    void latexLikeCommandsAreTranslated() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("12 \\cdot x^{2} - 6x + 7", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12 * x^2 - 6*x + 7", resolution.get().canonical());
        assertEquals(AnswerInputFamily.LATEX_LIKE, resolution.get().family());
    }

    @Test
    void latexDerivativeNotationIsStripped() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("f^{\\prime}(x) = 12x^{2} - 6x + 7", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12*x^2 - 6*x + 7", resolution.get().canonical());
        assertEquals(AnswerInputFamily.LATEX_LIKE, resolution.get().family());
    }

    @Test
    void latexTimesCommandIsTranslated() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve("12 \\times x^{2}", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12 * x^2", resolution.get().canonical());
    }

    @Test
    void whitespaceIsCollapsed() {
        Optional<CanonicalExpressionResolver.Resolution> resolution =
                CanonicalExpressionResolver.resolve(" 12  *  x ^ 2  - 6*x + 7 ", VARIABLES);

        assertTrue(resolution.isPresent());
        assertEquals("12 * x ^ 2 - 6*x + 7", resolution.get().canonical());
    }

    @Test
    void anUndeclaredVariableIsRejected() {
        assertFalse(CanonicalExpressionResolver.resolve("12*y^2 + 1", VARIABLES).isPresent());
    }

    @Test
    void blankInputIsRejected() {
        assertFalse(CanonicalExpressionResolver.resolve("", VARIABLES).isPresent());
        assertFalse(CanonicalExpressionResolver.resolve("   ", VARIABLES).isPresent());
    }

    @Test
    void anUnsupportedLaTeXCommandIsRejected() {
        assertFalse(CanonicalExpressionResolver.resolve("\\frac{1}{2}x", VARIABLES).isPresent());
    }

    @Test
    void aResidualUnicodeCharacterOutsideTheAcceptedFamiliesIsRejected() {
        assertFalse(CanonicalExpressionResolver.resolve("x \u221a 2", VARIABLES).isPresent());
    }
}
