package cn.lunalhx.ai.kilnai.domain.learning.model;

import java.util.Objects;

public record FrozenModelProfile(
        ModelBindingSnapshot strong,
        ModelBindingSnapshot small
) {
    public FrozenModelProfile {
        Objects.requireNonNull(strong, "strong model must not be null");
        Objects.requireNonNull(small, "small model must not be null");
    }
}
