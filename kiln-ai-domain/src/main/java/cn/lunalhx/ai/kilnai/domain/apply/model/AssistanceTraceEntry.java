package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.time.Instant;
import java.util.Objects;

/**
 * One append-only entry of a Task Attempt's Assistance Trace: an actually
 * exposed hint level, an answered procedural or substantive clarification, or
 * a temporary Explain shown inside an open Attempt, each with its exposure
 * moment. Only actually exposed assistance is ever appended, so the audit
 * trail reflects what the learner really saw. A procedural clarification is
 * recorded but never disqualifies an Independent attempt by itself; a
 * substantive clarification or a temporary Explain is assistance and keeps
 * the converted Practice attempt's evidence honestly classified.
 */
public record AssistanceTraceEntry(AssistanceKind kind, HintLevel level, Instant exposedAt) {

    public AssistanceTraceEntry {
        Objects.requireNonNull(kind, "kind must not be null");
        if (kind == AssistanceKind.HINT) {
            Objects.requireNonNull(level, "level must not be null");
        } else {
            level = null;
        }
        Objects.requireNonNull(exposedAt, "exposedAt must not be null");
    }

    /**
     * The closed kinds of one recorded assistance event. HINT entries carry
     * the exposed Hint Level and keep the existing ladder semantics;
     * clarification and temporary-Explain entries carry no level.
     */
    public enum AssistanceKind {
        HINT,
        PROCEDURAL_CLARIFICATION,
        SUBSTANTIVE_CLARIFICATION,
        TEMPORARY_EXPLAIN
    }

    public static AssistanceTraceEntry hint(HintLevel level, Instant exposedAt) {
        return new AssistanceTraceEntry(AssistanceKind.HINT, level, exposedAt);
    }

    public static AssistanceTraceEntry clarification(AssistanceKind kind, Instant exposedAt) {
        if (kind == AssistanceKind.HINT) {
            throw new IllegalArgumentException("a clarification entry must never carry a hint kind");
        }
        return new AssistanceTraceEntry(kind, null, exposedAt);
    }

    /**
     * The stable audit string recorded with accepted Learning Evidence, e.g.
     * {@code H1:orient} for a hint and {@code substantive_clarification} for
     * a substantive clarification.
     */
    public String asEvidenceString() {
        return switch (kind) {
            case HINT -> level.name() + ":" + level.disclosureKind();
            case PROCEDURAL_CLARIFICATION -> "procedural_clarification";
            case SUBSTANTIVE_CLARIFICATION -> "substantive_clarification";
            case TEMPORARY_EXPLAIN -> "temporary_explain";
        };
    }
}
