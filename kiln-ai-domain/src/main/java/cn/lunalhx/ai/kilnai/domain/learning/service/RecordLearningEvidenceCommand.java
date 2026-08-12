package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningEventType;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;

import java.time.Instant;
import java.util.UUID;

public record RecordLearningEvidenceCommand(
        UUID userId,
        UUID conceptId,
        LearningEventType eventType,
        LearningResult result,
        int hintLevel,
        boolean delayedReview,
        boolean transfer,
        Instant occurredAt,
        Integer confidence,
        String errorTag
) {
}
