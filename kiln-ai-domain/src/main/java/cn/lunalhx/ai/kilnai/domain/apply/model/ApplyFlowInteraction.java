package cn.lunalhx.ai.kilnai.domain.apply.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;

import java.util.Objects;
import java.util.UUID;

/**
 * The durable learner-visible interaction of one Apply Learning Flow at one
 * Learner Interaction Boundary. Its closed {@link InteractionKind} declares
 * which one of the union shapes the interaction carries — an open Task
 * Attempt awaiting a submission, a teaching interaction with its
 * learner-visible teaching projection, an assistance-consent request over an
 * open Independent or Review attempt, a message-only transition, or the
 * neutral unavailable boundary of a failed node. The learner-visible view of
 * the last exposed hint level rides on the task shape of an open Apply
 * Practice Attempt. It never carries private assessor projections, expected
 * answers, unexposed hint levels, source traces, or Fingerprints.
 */
public record ApplyFlowInteraction(
        InteractionKind kind,
        UUID flowId,
        int interactionVersion,
        FlowStatus status,
        LearningStage stage,
        UUID attemptId,
        AttemptPurpose attemptPurpose,
        LearnerProjection learnerProjection,
        String learnerMessage,
        TeachingProjection teachingProjection,
        HintView hint,
        AssistanceConsentView assistanceConsent
) {

    public ApplyFlowInteraction {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(stage, "stage must not be null");
    }
}
