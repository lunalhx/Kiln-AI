package cn.lunalhx.ai.kilnai.domain.content.model.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A smallest independently teachable and assessable unit from a learning source. */
public record Concept(
        UUID id,
        String title,
        String summary,
        String sourceReference,
        Instant createdAt
) {
    public Concept {
        Objects.requireNonNull(id, "id must not be null");
        title = requireText(title, "title");
        summary = requireText(summary, "summary");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }
}
