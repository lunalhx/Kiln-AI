package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ConceptState;

import java.time.Instant;
import java.util.UUID;

public record ProgressRow(
        UUID userId,
        UUID conceptId,
        ConceptState state,
        boolean hasIndependentSuccess,
        boolean hasDelayedIndependentSuccess,
        boolean hasTransferSuccess,
        Instant lastIndependentSuccessAt,
        Instant lastFailureAt,
        Instant updatedAt
) {
}
