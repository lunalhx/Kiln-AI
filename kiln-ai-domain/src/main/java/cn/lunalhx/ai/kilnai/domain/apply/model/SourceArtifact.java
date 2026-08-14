package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext.SourcePassage;

import java.util.List;
import java.util.Objects;

/**
 * The immutable, versioned curated source artifact registered with an Apply
 * Learning Flow. It is the authoritative source record; it is never copied
 * into a learner-visible response.
 */
public record SourceArtifact(
        String sourcePackId,
        String version,
        List<SourcePassage> passages
) {

    public SourceArtifact {
        Objects.requireNonNull(sourcePackId, "sourcePackId must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(passages, "passages must not be null");
        passages = List.copyOf(passages);
    }
}
