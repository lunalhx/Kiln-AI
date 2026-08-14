package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;

import java.util.Objects;
import java.util.UUID;

public record TaskPackage(
        String schema,
        UUID taskPackageId,
        AttemptPurpose attemptPurpose,
        LearnerProjection learnerProjection,
        PrivateAssessorProjection privateAssessorProjection
) {

    public static final String SCHEMA = "task_package/v1";

    public TaskPackage {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(taskPackageId, "taskPackageId must not be null");
        Objects.requireNonNull(attemptPurpose, "attemptPurpose must not be null");
        Objects.requireNonNull(learnerProjection, "learnerProjection must not be null");
        Objects.requireNonNull(privateAssessorProjection, "privateAssessorProjection must not be null");
    }
}
