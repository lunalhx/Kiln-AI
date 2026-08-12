package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ConceptState;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;

import java.time.Instant;
import java.util.UUID;

public record LearningProgressResult(
        UUID userId,
        UUID conceptId,
        ConceptState state,
        TeachingAction nextAction,
        Instant nextReviewDueAt
) {
}
