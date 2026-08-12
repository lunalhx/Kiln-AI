package cn.lunalhx.ai.kilnai.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ConceptResponse(
        UUID id,
        String title,
        String summary,
        String sourceReference,
        Instant createdAt
) {
}
