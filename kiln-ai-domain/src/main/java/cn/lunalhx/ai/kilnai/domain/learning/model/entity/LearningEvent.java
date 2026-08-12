package cn.lunalhx.ai.kilnai.domain.learning.model.entity;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEvidence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable audit record. Assessment providers cannot update state directly. */
public record LearningEvent(
        UUID id,
        UUID userId,
        UUID conceptId,
        LearningEvidence evidence,
        Integer confidence,
        String errorTag,
        Instant recordedAt
) {
    public LearningEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (confidence != null && (confidence < 1 || confidence > 5)) {
            throw new IllegalArgumentException("confidence must be between 1 and 5");
        }
    }
}
