package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Proof-bounded mathematical equivalence over the reference scope: integer
 * polynomial expressions in one variable. Returns only PROVEN_EQUIVALENT,
 * PROVEN_NOT_EQUIVALENT, or CANNOT_DECIDE; anything outside the supported
 * syntax is never guessed to be incorrect.
 */
public final class MathematicalEquivalenceCheck {

    private static final int MAX_ITERATED_POWER = 128;
    private static final int MAX_EXPONENT = 1_000_000;

    private MathematicalEquivalenceCheck() {
    }

    public static EquivalenceOutcome check(String expressionA, String expressionB, List<String> declaredVariables) {
        Optional<Polynomial> polynomialA = Polynomial.parse(expressionA, declaredVariables);
        Optional<Polynomial> polynomialB = Polynomial.parse(expressionB, declaredVariables);
        if (polynomialA.isEmpty() || polynomialB.isEmpty()) {
            return EquivalenceOutcome.CANNOT_DECIDE;
        }
        if (polynomialA.get().equals(polynomialB.get())) {
            return EquivalenceOutcome.PROVEN_EQUIVALENT;
        }
        return EquivalenceOutcome.PROVEN_NOT_EQUIVALENT;
    }

    private static final class Polynomial {

        private final TreeMap<Integer, Long> terms = new TreeMap<>();

        static Polynomial constant(long value) {
            Polynomial polynomial = new Polynomial();
            if (value != 0) {
                polynomial.terms.put(0, value);
            }
            return polynomial;
        }

        static Polynomial variable() {
            Polynomial polynomial = new Polynomial();
            polynomial.terms.put(1, 1L);
            return polynomial;
        }

        static Optional<Polynomial> parse(String expression, List<String> declaredVariables) {
            if (expression == null || expression.isBlank()) {
                return Optional.empty();
            }
            try {
                String source = expression.replaceAll("\\s+", "");
                Parser parser = new Parser(source, declaredVariables);
                Polynomial parsed = parser.parseExpression();
                if (!parser.atEnd()) {
                    throw new ParseException();
                }
                return Optional.of(parsed);
            } catch (ParseException | ArithmeticException exception) {
                return Optional.empty();
            }
        }

        Polynomial plus(Polynomial other) {
            Polynomial result = new Polynomial();
            result.terms.putAll(terms);
            other.terms.forEach((degree, coefficient) ->
                    result.terms.merge(degree, coefficient, Math::addExact));
            return result.normalized();
        }

        Polynomial minus(Polynomial other) {
            Polynomial result = new Polynomial();
            result.terms.putAll(terms);
            other.terms.forEach((degree, coefficient) ->
                    result.terms.merge(degree, Math.negateExact(coefficient), Math::addExact));
            return result.normalized();
        }

        Polynomial negated() {
            Polynomial result = new Polynomial();
            terms.forEach((degree, coefficient) ->
                    result.terms.put(degree, Math.negateExact(coefficient)));
            return result;
        }

        Polynomial times(Polynomial other) {
            Polynomial result = new Polynomial();
            terms.forEach((degreeA, coefficientA) ->
                    other.terms.forEach((degreeB, coefficientB) ->
                            result.terms.merge(degreeA + degreeB,
                                    Math.multiplyExact(coefficientA, coefficientB),
                                    Math::addExact)));
            return result.normalized();
        }

        Polynomial power(int exponent) {
            if (exponent == 0) {
                return constant(1);
            }
            if (terms.size() > 1 && exponent > MAX_ITERATED_POWER) {
                throw new ParseException();
            }
            Polynomial result = constant(1);
            for (int i = 0; i < exponent; i++) {
                result = result.times(this);
            }
            return result;
        }

        Polynomial divideBy(long divisor) {
            if (divisor == 0) {
                throw new ParseException();
            }
            Polynomial result = new Polynomial();
            for (Map.Entry<Integer, Long> entry : terms.entrySet()) {
                if (entry.getValue() % divisor != 0) {
                    throw new ParseException();
                }
                result.terms.put(entry.getKey(), entry.getValue() / divisor);
            }
            return result;
        }

        private Polynomial normalized() {
            terms.entrySet().removeIf(entry -> entry.getValue() == 0);
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Polynomial polynomial && terms.equals(polynomial.terms);
        }

        @Override
        public int hashCode() {
            return terms.hashCode();
        }
    }

    private static final class Parser {

        private final String source;
        private final Set<Character> declaredVariables;
        private int pos;

        Parser(String source, List<String> declaredVariables) {
            this.source = source;
            this.declaredVariables = new HashSet<>();
            for (String variable : declaredVariables) {
                if (variable.length() == 1) {
                    this.declaredVariables.add(variable.charAt(0));
                }
            }
            if (this.declaredVariables.isEmpty()) {
                throw new ParseException();
            }
        }

        Polynomial parseExpression() {
            Polynomial value = parseTerm();
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (c == '+') {
                    pos++;
                    value = value.plus(parseTerm());
                } else if (c == '-') {
                    pos++;
                    value = value.minus(parseTerm());
                } else {
                    break;
                }
            }
            return value;
        }

        boolean atEnd() {
            return pos == source.length();
        }

        private Polynomial parseTerm() {
            Polynomial value = parseFactor();
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (c == '*') {
                    pos++;
                    value = value.times(parseFactor());
                } else if (c == '/') {
                    pos++;
                    value = value.divideBy(parseInteger());
                } else if (startsFactor(c)) {
                    value = value.times(parseFactor());
                } else {
                    break;
                }
            }
            return value;
        }

        private Polynomial parseFactor() {
            if (pos >= source.length()) {
                throw new ParseException();
            }
            char c = source.charAt(pos);
            if (c == '-') {
                pos++;
                return parseFactor().negated();
            }
            if (c == '(') {
                pos++;
                Polynomial inner = parseExpression();
                if (pos >= source.length() || source.charAt(pos) != ')') {
                    throw new ParseException();
                }
                pos++;
                return parseOptionalPower(inner);
            }
            if (Character.isDigit(c)) {
                return parseOptionalPower(Polynomial.constant(parseInteger()));
            }
            if (Character.isLetter(c) && declaredVariables.contains(c)) {
                pos++;
                return parseOptionalPower(Polynomial.variable());
            }
            throw new ParseException();
        }

        private boolean startsFactor(char c) {
            return Character.isDigit(c) || Character.isLetter(c) || c == '(';
        }

        private Polynomial parseOptionalPower(Polynomial base) {
            if (pos < source.length() && source.charAt(pos) == '^') {
                pos++;
                return base.power(parseExponent());
            }
            return base;
        }

        private long parseInteger() {
            int start = pos;
            while (pos < source.length() && Character.isDigit(source.charAt(pos))) {
                pos++;
            }
            if (start == pos) {
                throw new ParseException();
            }
            try {
                return Long.parseLong(source.substring(start, pos));
            } catch (NumberFormatException exception) {
                throw new ParseException();
            }
        }

        private int parseExponent() {
            long exponent = parseInteger();
            if (exponent > MAX_EXPONENT) {
                throw new ParseException();
            }
            return (int) exponent;
        }
    }

    private static final class ParseException extends RuntimeException {
    }
}
