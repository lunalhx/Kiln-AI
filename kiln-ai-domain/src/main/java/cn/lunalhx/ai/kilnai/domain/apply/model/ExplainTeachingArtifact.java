package cn.lunalhx.ai.kilnai.domain.apply.model;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The durable teaching artifact of one Explain node execution. Its learner
 * projection is the only learner-visible content; the source trace, the
 * Profile-derived example Fingerprint, and the pinned execution trace remain
 * private and are never projected to the learner.
 */
public record ExplainTeachingArtifact(
        UUID artifactId,
        TeachingProjection learnerProjection,
        List<SourceTraceEntry> sourceTrace,
        ExampleFingerprint exampleFingerprint,
        ExecutionTrace executionTrace
) {

    public ExplainTeachingArtifact {
        Objects.requireNonNull(artifactId, "artifactId must not be null");
        Objects.requireNonNull(learnerProjection, "learnerProjection must not be null");
        Objects.requireNonNull(sourceTrace, "sourceTrace must not be null");
        Objects.requireNonNull(exampleFingerprint, "exampleFingerprint must not be null");
        Objects.requireNonNull(executionTrace, "executionTrace must not be null");
        sourceTrace = List.copyOf(sourceTrace);
    }

    public record SourceTraceEntry(String sourceDocumentId, String sourceVersion, String passageId) {

        public SourceTraceEntry {
            Objects.requireNonNull(sourceDocumentId, "sourceDocumentId must not be null");
            Objects.requireNonNull(sourceVersion, "sourceVersion must not be null");
            Objects.requireNonNull(passageId, "passageId must not be null");
        }
    }

    public record ExampleFingerprint(String derivedBy, String value) {

        public ExampleFingerprint {
            Objects.requireNonNull(derivedBy, "derivedBy must not be null");
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    public record ExecutionTrace(String profile, List<String> skillStack, ModelExecution model) {

        public ExecutionTrace {
            Objects.requireNonNull(profile, "profile must not be null");
            skillStack = List.copyOf(skillStack);
            Objects.requireNonNull(model, "model must not be null");
        }
    }
}
