package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;

import java.util.Objects;
import java.util.UUID;

/**
 * The durable learner-visible interaction of one Apply Learning Flow at one
 * Learner Interaction Boundary. It carries the open Task Attempt awaiting a
 * submission, a teaching interaction with its learner-visible teaching
 * projection, or a terminal message; it never carries private assessor
 * projections, expected answers, source traces, or Fingerprints.
 */
public record ApplyFlowInteraction(
        UUID flowId,
        int interactionVersion,
        FlowStatus status,
        LearningStage stage,
        UUID attemptId,
        AttemptPurpose attemptPurpose,
        LearnerProjection learnerProjection,
        String learnerMessage,
        TeachingProjection teachingProjection
) {

    public ApplyFlowInteraction {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(stage, "stage must not be null");
    }
}
