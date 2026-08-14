package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.util.Objects;

/**
 * A learner's final-derivative answer retaining both the original raw text and
 * the learner-confirmed canonical expression used for Assessment, together with
 * the detected input family.
 */
public record MathematicalAnswer(
        String raw,
        String confirmedCanonical,
        AnswerInputFamily inputFamily
) {

    public MathematicalAnswer {
        Objects.requireNonNull(raw, "raw must not be null");
        Objects.requireNonNull(confirmedCanonical, "confirmedCanonical must not be null");
        Objects.requireNonNull(inputFamily, "inputFamily must not be null");
    }
}
