package cn.lunalhx.ai.kilnai.domain.apply.gate;

import cn.lunalhx.ai.kilnai.domain.apply.model.EquivalenceOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLevel;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalEquivalenceCheck;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The Hint Ladder Gate over the closed {@code hint_generation/v1} draft:
 * contract closure is enforced by the strict parser, and this policy adds
 * level order, disclosure-kind sequence, source grounding, H1-H4 answer
 * leakage, and deterministic polynomial H5 answer equivalence. A rejected or
 * repairable ladder exposes nothing; after the one allowed repair a repeated
 * invalid result becomes Node Execution Failed with the Practice Attempt
 * still open.
 */
public final class HintLadderGatePolicy implements GatePolicy<HintGenerationDraft.LadderReady> {

    private final HintGateFacts facts;

    public HintLadderGatePolicy(HintGateFacts facts) {
        this.facts = Objects.requireNonNull(facts, "facts must not be null");
    }

    @Override
    public GateResult<HintGenerationDraft.LadderReady> evaluate(
            HintGenerationDraft.LadderReady draft,
            GateContext context
    ) {
        List<GateViolation> violations = new ArrayList<>();
        List<HintGenerationDraft.Entry> entries = draft.entries();

        for (int index = 0; index < entries.size(); index++) {
            HintGenerationDraft.Entry entry = entries.get(index);
            HintLevel expectedLevel = HintLevel.of(index + 1);
            if (entry.level() != expectedLevel.level()) {
                violations.add(new GateViolation("hint.level.order",
                        "ladder entries must be ordered H1 to H5 but entry " + (index + 1)
                                + " has level " + entry.level()));
            }
            if (!expectedLevel.disclosureKind().equals(entry.disclosureKind())) {
                violations.add(new GateViolation("hint.kind.sequence",
                        "ladder entry " + entry.level() + " must use disclosure kind "
                                + expectedLevel.disclosureKind() + " but was " + entry.disclosureKind()));
            }
            if (!grounded(entry)) {
                violations.add(new GateViolation("hint.source.ungrounded",
                        "every hint source trace ref must reference the task package's approved source trace"));
            }
            if (entry.level() < 5 && leaksAnswer(entry.learnerContent())) {
                violations.add(new GateViolation("hint.answer.leak",
                        "H1-H4 content must not reveal the proposed final answer"));
            }
            if (entry.level() == 5 && !h5Equivalent(entry)) {
                violations.add(new GateViolation("hint.h5.equivalence",
                        "the H5 proposed final answer must be deterministically equivalent to the expected answer"));
            }
        }

        if (!violations.isEmpty()) {
            return GateResult.rejected(violations);
        }
        return GateResult.passed(draft);
    }

    private boolean grounded(HintGenerationDraft.Entry entry) {
        Set<String> approved = new java.util.HashSet<>();
        for (HintGateFacts.SourceRef ref : facts.approvedSourceTrace()) {
            approved.add(ref.sourceDocumentId() + ":" + ref.passageId());
        }
        if (approved.isEmpty()) {
            return false;
        }
        for (HintGenerationDraft.SourceTraceRef ref : entry.sourceTrace()) {
            if (!approved.contains(ref.sourceDocumentId() + ":" + ref.passageId())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deterministic leakage check over H1-H4 content: the normalized expected
     * answer must not appear as a substring, and no math expression embedded
     * in the content may be proven equivalent to the expected answer.
     * Expressions the checker cannot parse are ignored — a leaked answer in
     * an unparseable form is still caught by the substring and equivalence
     * probes over normalized notation.
     */
    private boolean leaksAnswer(String learnerContent) {
        String normalizedContent = normalizeMath(learnerContent);
        String normalizedExpected = normalizeMath(facts.expectedExpression());
        if (normalizedContent.contains(normalizedExpected)) {
            return true;
        }
        for (String candidate : expressionCandidates(normalizedContent)) {
            EquivalenceOutcome outcome = MathematicalEquivalenceCheck.check(
                    candidate, facts.expectedExpression(), facts.variables());
            if (outcome == EquivalenceOutcome.PROVEN_EQUIVALENT) {
                return true;
            }
        }
        return false;
    }

    private boolean h5Equivalent(HintGenerationDraft.Entry entry) {
        if (entry.proposedFinalAnswer() == null || entry.reasoningSteps() == null) {
            return false;
        }
        EquivalenceOutcome outcome = MathematicalEquivalenceCheck.check(
                entry.proposedFinalAnswer(), facts.expectedExpression(), facts.variables());
        return outcome == EquivalenceOutcome.PROVEN_EQUIVALENT;
    }

    private static final Set<Character> EXPRESSION_CHARS = Set.of(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '+', '-', '*', '/', '^', '.', '(', ')');

    /**
     * Extracts maximal math-looking runs from prose; only the declared
     * variable letters may appear inside a run, so Chinese prose never fuses
     * with an embedded expression.
     */
    private List<String> expressionCandidates(String normalizedContent) {
        Set<Character> variables = new java.util.HashSet<>();
        for (String variable : facts.variables()) {
            if (variable.length() == 1) {
                variables.add(variable.charAt(0));
            }
        }
        List<String> candidates = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        for (int index = 0; index < normalizedContent.length(); index++) {
            char c = normalizedContent.charAt(index);
            if (EXPRESSION_CHARS.contains(c) || variables.contains(c)) {
                run.append(c);
            } else {
                flushCandidate(candidates, run);
            }
        }
        flushCandidate(candidates, run);
        return candidates;
    }

    private static void flushCandidate(List<String> candidates, StringBuilder run) {
        if (!run.isEmpty()) {
            String candidate = run.toString();
            while (candidate.endsWith("+") || candidate.endsWith("-")
                    || candidate.endsWith("*") || candidate.endsWith("/")
                    || candidate.endsWith("^") || candidate.endsWith(".")) {
                candidate = candidate.substring(0, candidate.length() - 1);
            }
            if (!candidate.isEmpty()) {
                candidates.add(candidate);
            }
            run.setLength(0);
        }
    }

    /**
     * Normalizes common math notation to the ASCII form the deterministic
     * checker parses: Unicode minus, multiplication dot, and superscript
     * digits, with all whitespace removed.
     */
    static String normalizeMath(String value) {
        String normalized = value
                .replace('\u2212', '-')
                .replace('\u00d7', '*')
                .replace('\u00b7', '*');
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int index = 0; index < normalized.length(); index++) {
            char c = normalized.charAt(index);
            char superscript = superscriptOf(c);
            if (superscript != 0) {
                builder.append('^').append(superscript);
            } else {
                builder.append(c);
            }
        }
        return builder.toString().replaceAll("\\s+", "");
    }

    private static char superscriptOf(char c) {
        return switch (c) {
            case '\u2070' -> '0';
            case '\u00b9' -> '1';
            case '\u00b2' -> '2';
            case '\u00b3' -> '3';
            case '\u2074' -> '4';
            case '\u2075' -> '5';
            case '\u2076' -> '6';
            case '\u2077' -> '7';
            case '\u2078' -> '8';
            case '\u2079' -> '9';
            default -> 0;
        };
    }
}
