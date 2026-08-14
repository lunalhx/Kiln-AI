package cn.lunalhx.ai.kilnai.domain.apply.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome.CANNOT_DECIDE;
import static cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome.PROVEN_EQUIVALENT;
import static cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome.PROVEN_NOT_EQUIVALENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MathematicalEquivalenceCheckTest {

    private static final List<String> VARIABLES = List.of("x");

    @Test
    void theSameExpressionIsProvenEquivalent() {
        assertEquals(PROVEN_EQUIVALENT,
                MathematicalEquivalenceCheck.check("12*x^2 - 6*x + 7", "12*x^2 - 6*x + 7", VARIABLES));
    }

    @Test
    void differentlyOrderedTermsAreProvenEquivalent() {
        assertEquals(PROVEN_EQUIVALENT,
                MathematicalEquivalenceCheck.check("12*x^2 - 6*x + 7", "7 + 12*x^2 - 6*x", VARIABLES));
    }

    @Test
    void anExpandedParenthesizedFormIsProvenEquivalent() {
        assertEquals(PROVEN_EQUIVALENT,
                MathematicalEquivalenceCheck.check("(2*x+1)*(6*x-7)+14", "12*x^2-8*x+7", VARIABLES));
    }

    @Test
    void aProductOfPowersIsProvenEquivalent() {
        assertEquals(PROVEN_EQUIVALENT,
                MathematicalEquivalenceCheck.check("x^2 * x^4", "x^6", VARIABLES));
    }

    @Test
    void repeatedMultiplicationIsProvenEquivalent() {
        assertEquals(PROVEN_EQUIVALENT,
                MathematicalEquivalenceCheck.check("x*x*x", "x^3", VARIABLES));
    }

    @Test
    void anExactDivisionByAConstantIsProvenEquivalent() {
        assertEquals(PROVEN_EQUIVALENT,
                MathematicalEquivalenceCheck.check("12*x^2/2", "6*x^2", VARIABLES));
    }

    @Test
    void aZeroFormIsProvenEquivalentToZero() {
        assertEquals(PROVEN_EQUIVALENT,
                MathematicalEquivalenceCheck.check("0", "0*x", VARIABLES));
        assertEquals(PROVEN_EQUIVALENT,
                MathematicalEquivalenceCheck.check("x^0 - 1", "0", VARIABLES));
    }

    @Test
    void aDifferentPolynomialIsProvenNotEquivalent() {
        assertEquals(PROVEN_NOT_EQUIVALENT,
                MathematicalEquivalenceCheck.check("12*x^2 - 6*x", "12*x^2 - 6*x + 7", VARIABLES));
    }

    @Test
    void aDifferentDegreeIsProvenNotEquivalent() {
        assertEquals(PROVEN_NOT_EQUIVALENT,
                MathematicalEquivalenceCheck.check("2*x", "2*x^2", VARIABLES));
    }

    @Test
    void aNegativeExponentReturnsCannotDecide() {
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("x^-1", "1/x", VARIABLES));
    }

    @Test
    void aNonIntegerExponentReturnsCannotDecide() {
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("x^0.5", "x^0.5", VARIABLES));
    }

    @Test
    void aComparisonExpressionReturnsCannotDecide() {
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("x > 3", "x > 3", VARIABLES));
    }

    @Test
    void anUnsupportedFunctionReturnsCannotDecide() {
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("sqrt(x)", "x^0.5", VARIABLES));
    }

    @Test
    void anAmbiguousChainedPowerReturnsCannotDecide() {
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("x^2^3", "x^8", VARIABLES));
    }

    @Test
    void anUndeclaredLetterReturnsCannotDecide() {
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("12*y^2 + 1", "12*y^2 + 1", VARIABLES));
    }

    @Test
    void anIndivisibleDivisionReturnsCannotDecide() {
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("x/2", "x", VARIABLES));
    }

    @Test
    void blankInputReturnsCannotDecide() {
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("", "12*x^2 - 6*x + 7", VARIABLES));
        assertEquals(CANNOT_DECIDE,
                MathematicalEquivalenceCheck.check("   ", "12*x^2 - 6*x + 7", VARIABLES));
    }
}
