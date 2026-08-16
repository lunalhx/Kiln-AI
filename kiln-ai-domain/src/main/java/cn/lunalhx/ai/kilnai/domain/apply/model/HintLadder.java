package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The stable private Hint Ladder persisted for one open Apply Practice
 * Attempt after its first hint request passed the Hint Gate. Later requests
 * expose persisted levels deterministically without another model call; the
 * learner-visible projection is the {@link HintView} of the exposed level
 * only, never the whole ladder.
 */
public record HintLadder(UUID attemptId, List<HintGenerationDraft.Entry> entries) {

    public HintLadder {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        entries = List.copyOf(entries);
        if (entries.size() != 5) {
            throw new IllegalArgumentException("a Hint Ladder must contain exactly five entries");
        }
    }

    public static HintLadder from(UUID attemptId, HintGenerationDraft.LadderReady draft) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        Objects.requireNonNull(draft, "draft must not be null");
        return new HintLadder(attemptId, draft.entries());
    }

    public HintGenerationDraft.Entry entry(int level) {
        HintLevel.of(level);
        return entries.get(level - 1);
    }

    /**
     * The deterministic content fingerprint of the whole generated ladder:
     * every level, disclosure kind, learner-visible content, source trace,
     * H5 reasoning step, and proposed final answer. Once the ladder is
     * generated, the fingerprint is recorded in the Flow's novelty ledger so
     * later task and example generation never reuses the exposed hint
     * content.
     */
    public String fingerprint() {
        StringBuilder raw = new StringBuilder();
        for (HintGenerationDraft.Entry entry : entries) {
            raw.append(entry.level()).append('|').append(entry.disclosureKind()).append('|')
                    .append(entry.learnerContent()).append('|');
            entry.sourceTrace().forEach(ref ->
                    raw.append(ref.sourceDocumentId()).append('/').append(ref.passageId()).append(';'));
            raw.append('|');
            if (entry.reasoningSteps() != null) {
                raw.append(String.join(",", entry.reasoningSteps()));
            }
            raw.append('|');
            if (entry.proposedFinalAnswer() != null) {
                raw.append(entry.proposedFinalAnswer());
            }
            raw.append('\n');
        }
        return ApplyHash.sha256Hex(raw.toString());
    }

    /**
     * The deterministic content fingerprint of the H5 reveal entry alone
     * (learner content, reasoning steps, and proposed final answer): the
     * revealed solution that must never be reused by later generation.
     */
    public String revealFingerprint() {
        HintGenerationDraft.Entry reveal = entry(5);
        String raw = String.join("|",
                reveal.learnerContent(),
                String.join(",", reveal.reasoningSteps()),
                reveal.proposedFinalAnswer());
        return ApplyHash.sha256Hex(raw.toString());
    }

    /**
     * The learner-visible projection of one exposed level. The H5 reveal
     * carries the reasoning steps and the proposed final answer; H1-H4 carry
     * neither, so a lower level can never smuggle the solution.
     */
    public HintView view(int level) {
        HintGenerationDraft.Entry entry = entry(level);
        return new HintView(
                entry.level(),
                entry.disclosureKind(),
                entry.learnerContent(),
                level == 5 ? entry.reasoningSteps() : null,
                level == 5 ? entry.proposedFinalAnswer() : null);
    }
}
