package cn.lunalhx.ai.kilnai.domain.learning.model.valobj;

import java.time.Instant;
import java.util.Objects;

/** Assessed evidence used by the aggregate to update a learner's concept state. */
public record LearningEvidence(
        LearningEventType eventType,
        LearningResult result,
        int hintLevel,
        boolean delayedReview,
        boolean transfer,
        Instant occurredAt
) {
    public LearningEvidence {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (hintLevel < 0 || hintLevel > 4) {
            throw new IllegalArgumentException("hintLevel must be between 0 and 4");
        }
        if ((delayedReview || transfer) && hintLevel != 0) {
            throw new IllegalArgumentException("delayed review and transfer evidence must be independent");
        }
    }

    public boolean isIndependentSuccess() {
        return result.isSuccessful() && hintLevel == 0 && eventType != LearningEventType.EXPLAIN
                && eventType != LearningEventType.HINT;
    }

    public boolean isAssistedSuccess() {
        return result.isSuccessful() && hintLevel > 0 && eventType != LearningEventType.HINT;
    }
}
