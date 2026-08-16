package cn.lunalhx.ai.kilnai.domain.apply.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One durable Teach-back anchor of a Learning Flow: the most recently
 * exposed Explain worked-example artifact or H5 solution reveal that makes
 * Teach-back legal. The Workflow Guard offers Teach-back only while such an
 * eligible anchor exists; the anchor content stays in the Artifact Store and
 * is resolved by the Teach-back node through the anchor id and kind.
 */
public record TeachBackAnchor(
        TeachBackAnchorKind kind,
        UUID anchorId,
        Instant exposedAt
) {

    public TeachBackAnchor {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(anchorId, "anchorId must not be null");
        Objects.requireNonNull(exposedAt, "exposedAt must not be null");
    }

    public enum TeachBackAnchorKind {
        EXPLAIN_WORKED_EXAMPLE,
        H5_SOLUTION_REVEAL
    }
}
