package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ExpectedExpressionNormalizer {

    private static final String ALLOWED = "0123456789+-*/^.() \t";

    private ExpectedExpressionNormalizer() {
    }

    public static Optional<String> normalize(String expression, List<String> declaredVariables) {
        if (expression == null || expression.isBlank()) {
            return Optional.empty();
        }
        String normalized = expression
                .replace('\u2212', '-')
                .replace('\u00d7', '*')
                .replace('\u00b7', '*')
                .trim()
                .replaceAll("\\s+", " ");
        if (!isWellFormed(normalized, declaredVariables)) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }

    private static boolean isWellFormed(String expression, List<String> declaredVariables) {
        Set<String> variables = Set.copyOf(declaredVariables);
        if (variables.isEmpty()) {
            return false;
        }
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isLetter(c)) {
                if (!variables.contains(String.valueOf(c))) {
                    return false;
                }
            } else if (ALLOWED.indexOf(c) < 0) {
                return false;
            }
        }
        char last = expression.charAt(expression.length() - 1);
        return "+-*/^".indexOf(last) < 0 && last != '.';
    }
}
