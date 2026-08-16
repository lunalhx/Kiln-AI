package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.time.Instant;
import java.util.Objects;

/**
 * One append-only entry of a Task Attempt's Assistance Trace: an actually
 * exposed hint level and its disclosure moment. Only exposed levels are ever
 * appended, so the audit trail reflects what the learner actually saw.
 */
public record AssistanceTraceEntry(HintLevel level, Instant exposedAt) {

    public AssistanceTraceEntry {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(exposedAt, "exposedAt must not be null");
    }

    /**
     * The stable audit string recorded with accepted Learning Evidence, e.g.
     * {@code H1:orient}.
     */
    public String asEvidenceString() {
        return level.name() + ":" + level.disclosureKind();
    }
}
