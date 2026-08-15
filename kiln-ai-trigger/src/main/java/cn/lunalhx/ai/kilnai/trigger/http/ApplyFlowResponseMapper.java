package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse.ApplyTaskView;
import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse.ProgressView;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The shared learner-safe mapping from a durable Apply interaction to the
 * public Apply flow representation, including the safe Concept Progress
 * projection. Expected answers, source traces, Fingerprints, and assessment
 * facts never appear.
 */
@Component
public class ApplyFlowResponseMapper {

    private final LearningFlowStore flowStore;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public ApplyFlowResponseMapper(LearningFlowStore flowStore) {
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
    }

    public ApplyFlowResponse toResponse(ApplyFlowInteraction interaction) {
        LearnerProjection projection = interaction.learnerProjection();
        ApplyTaskView task = projection == null ? null : new ApplyTaskView(
                projection.locale(),
                projection.taskText(),
                projection.answerFields().stream()
                        .map(field -> new ApplyTaskView.AnswerFieldView(
                                field.id(), field.label(), field.kind(),
                                field.variables(), field.acceptedInputFamilies(), field.required()))
                        .toList(),
                projection.submissionRule().maxFormalSubmissions());
        return new ApplyFlowResponse(
                interaction.flowId(),
                interaction.interactionVersion(),
                interaction.status().name(),
                interaction.stage().name(),
                interaction.attemptId(),
                interaction.attemptPurpose() == null ? null : interaction.attemptPurpose().name(),
                task,
                interaction.learnerMessage(),
                projection == null ? List.of() : projection.allowedEvents().stream().map(Enum::name).toList(),
                progressOf(interaction.flowId()));
    }

    public ProgressView progressOf(UUID flowId) {
        return flowStore.findFlow(flowId)
                .map(flow -> {
                    List<AcceptedLearningEvidence> evidence = flowStore.allEvidence().stream()
                            .filter(item -> item.learnerId().equals(flow.learnerId())
                                    && item.conceptId().equals(flow.conceptId()))
                            .toList();
                    ConceptProgress progress = progressProjector.project(
                            flow.learnerId(), flow.conceptId(), evidence);
                    return new ProgressView(
                            progress.currentMilestone().name(),
                            progress.highestMilestoneReached().name(),
                            progress.currentStage().name());
                })
                .orElse(null);
    }
}
