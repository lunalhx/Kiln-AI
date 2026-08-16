package cn.lunalhx.ai.kilnai.domain.apply.model;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The validated {@code teach_back_task_package/v1} output of one Teach-back
 * generation: a Practice-purpose Task Package whose learner projection is
 * exactly one short-text response field and whose private projection retains
 * the three-dimensional Rubric mapping, the grounded source trace, the
 * anchor reference, and the pinned execution trace. It contains no canonical
 * expected answer and no verbatim expected explanation — the assessor judges
 * the learner's own explanation against the already exposed anchor content.
 */
public record TeachBackTaskPackage(
        String schema,
        UUID taskPackageId,
        AttemptPurpose attemptPurpose,
        LearnerProjection learnerProjection,
        TeachBackPrivateProjection privateProjection
) {

    public static final String SCHEMA = "teach_back_task_package/v1";

    public TeachBackTaskPackage {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(taskPackageId, "taskPackageId must not be null");
        Objects.requireNonNull(attemptPurpose, "attemptPurpose must not be null");
        Objects.requireNonNull(learnerProjection, "learnerProjection must not be null");
        Objects.requireNonNull(privateProjection, "privateProjection must not be null");
        if (attemptPurpose != AttemptPurpose.PRACTICE) {
            throw new IllegalArgumentException("a Teach-back task package must be Practice-purpose");
        }
    }

    public record TeachBackPrivateProjection(
            List<RubricDimension> rubricMapping,
            List<SourceTraceEntry> sourceTrace,
            AnchorReference anchorReference,
            ExecutionTrace executionTrace
    ) {

        public TeachBackPrivateProjection {
            Objects.requireNonNull(rubricMapping, "rubricMapping must not be null");
            Objects.requireNonNull(sourceTrace, "sourceTrace must not be null");
            Objects.requireNonNull(anchorReference, "anchorReference must not be null");
            Objects.requireNonNull(executionTrace, "executionTrace must not be null");
            rubricMapping = List.copyOf(rubricMapping);
            sourceTrace = List.copyOf(sourceTrace);
        }
    }

    public record RubricDimension(String dimension, String masteryCriterion) {

        public RubricDimension {
            Objects.requireNonNull(dimension, "dimension must not be null");
            Objects.requireNonNull(masteryCriterion, "masteryCriterion must not be null");
        }
    }

    public record SourceTraceEntry(String sourceDocumentId, String passageId) {

        public SourceTraceEntry {
            Objects.requireNonNull(sourceDocumentId, "sourceDocumentId must not be null");
            Objects.requireNonNull(passageId, "passageId must not be null");
        }
    }

    public record AnchorReference(UUID anchorId, String anchorKind) {

        public AnchorReference {
            Objects.requireNonNull(anchorId, "anchorId must not be null");
            Objects.requireNonNull(anchorKind, "anchorKind must not be null");
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
