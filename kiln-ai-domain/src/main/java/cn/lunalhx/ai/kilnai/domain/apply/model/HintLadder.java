package cn.lunalhx.ai.kilnai.domain.apply.model;

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
