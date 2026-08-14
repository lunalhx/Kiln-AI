package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class CanonicalExpressionResolver {

    private static final String ASCII_OPERATORS = "0123456789+-*/^.() \t";

    private static final Pattern DERIVATIVE_PREFIX =
            Pattern.compile("^f\\s*'\\s*\\(\\s*x\\s*\\)\\s*=\\s*");

    private CanonicalExpressionResolver() {
    }

    /**
     * Determines the canonical ASCII expression and input family for one raw
     * derivative answer within the supported plain-text, Unicode-math, and
     * LaTeX-like families. Returns empty when the raw input cannot be resolved
     * within the declared variables.
     */
    public static Optional<Resolution> resolve(String raw, List<String> declaredVariables) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Set<String> variables = Set.copyOf(declaredVariables);
        if (variables.isEmpty()) {
            return Optional.empty();
        }
        AnswerInputFamily family = detectFamily(raw);
        String translated = translate(raw, family);
        if (translated == null) {
            return Optional.empty();
        }
        String withoutPrefix = DERIVATIVE_PREFIX.matcher(translated).replaceFirst("");
        if (withoutPrefix.isBlank()) {
            return Optional.empty();
        }
        String withExplicitMultiplication = insertImplicitMultiplication(withoutPrefix);
        String canonical = withExplicitMultiplication.trim().replaceAll("\\s+", " ");
        if (!isCanonical(canonical, variables)) {
            return Optional.empty();
        }
        return Optional.of(new Resolution(canonical, family));
    }

    public record Resolution(String canonical, AnswerInputFamily family) {
    }

    private static AnswerInputFamily detectFamily(String raw) {
        if (raw.indexOf('\\') >= 0) {
            return AnswerInputFamily.LATEX_LIKE;
        }
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) > 127) {
                return AnswerInputFamily.UNICODE_MATH;
            }
        }
        return AnswerInputFamily.PLAIN_TEXT;
    }

    private static String translate(String raw, AnswerInputFamily family) {
        String translated = raw;
        switch (family) {
            case LATEX_LIKE -> {
                translated = translated
                        .replace("\\cdot", "*")
                        .replace("\\times", "*")
                        .replace("^{\\prime}", "'");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < translated.length(); i++) {
                    if (translated.charAt(i) == '^' && i + 1 < translated.length()
                            && translated.charAt(i + 1) == '{') {
                        int close = translated.indexOf('}', i + 2);
                        if (close < 0) {
                            return null;
                        }
                        String inner = translated.substring(i + 2, close);
                        if (inner.isEmpty() || inner.chars().anyMatch(c -> !Character.isDigit(c))) {
                            return null;
                        }
                        builder.append('^').append(inner);
                        i = close;
                    } else {
                        builder.append(translated.charAt(i));
                    }
                }
                translated = builder.toString();
                if (translated.indexOf('\\') >= 0 || translated.indexOf('{') >= 0
                        || translated.indexOf('}') >= 0) {
                    return null;
                }
            }
            case UNICODE_MATH -> {
                StringBuilder builder = new StringBuilder();
                int superscriptRun = 0;
                for (int i = 0; i < translated.length(); i++) {
                    char c = translated.charAt(i);
                    int superscript = superscriptDigit(c);
                    if (superscript >= 0) {
                        if (superscriptRun == 0) {
                            builder.append('^');
                        }
                        builder.append(superscript);
                        superscriptRun++;
                    } else {
                        if (superscriptRun > 0) {
                            superscriptRun = 0;
                        }
                        builder.append(switch (c) {
                            case '\u2212' -> '-';
                            case '\u00d7', '\u00b7' -> '*';
                            case '\u2032' -> '\'';
                            default -> c;
                        });
                    }
                }
                translated = builder.toString();
            }
            case PLAIN_TEXT -> {
            }
        }
        return translated;
    }

    private static int superscriptDigit(char c) {
        return switch (c) {
            case '\u2070' -> 0;
            case '\u00b9' -> 1;
            case '\u00b2' -> 2;
            case '\u00b3' -> 3;
            case '\u2074' -> 4;
            case '\u2075' -> 5;
            case '\u2076' -> 6;
            case '\u2077' -> 7;
            case '\u2078' -> 8;
            case '\u2079' -> 9;
            default -> -1;
        };
    }

    private static String insertImplicitMultiplication(String expression) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char current = expression.charAt(i);
            if (i > 0) {
                char previous = builder.charAt(builder.length() - 1);
                if (needsMultiplication(previous, current)) {
                    builder.append('*');
                }
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static boolean needsMultiplication(char previous, char current) {
        if (Character.isDigit(previous) && Character.isLetter(current)) {
            return true;
        }
        if (previous == ')' && (Character.isDigit(current) || Character.isLetter(current) || current == '(')) {
            return true;
        }
        return Character.isLetter(previous) && current == '(';
    }

    private static boolean isCanonical(String expression, Set<String> variables) {
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isLetter(c)) {
                if (!variables.contains(String.valueOf(c))) {
                    return false;
                }
            } else if (ASCII_OPERATORS.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }
}
