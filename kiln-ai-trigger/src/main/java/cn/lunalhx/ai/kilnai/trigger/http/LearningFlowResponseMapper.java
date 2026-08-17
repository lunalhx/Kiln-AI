package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse.ConsentView;
import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse.HintView;
import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse.ProgressView;
import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse.TaskView;
import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse.TeachingView;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceConsentView;
import cn.lunalhx.ai.kilnai.domain.apply.model.InteractionKind;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The shared learner-safe mapping from a durable Learning Flow interaction to
 * the public {@link LearningFlowResponse}: the closed interaction union kind,
 * the safe Concept Progress projection, and the closed command names the
 * learner may issue against this committed interaction. Expected answers,
 * source traces, Fingerprints, unexposed hint levels, and assessment facts
 * never appear.
 */
@Component
public class LearningFlowResponseMapper {

    private final LearningFlowStore flowStore;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public LearningFlowResponseMapper(LearningFlowStore flowStore) {
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
    }

    public LearningFlowResponse toResponse(LearningFlowInteraction interaction) {
        LearnerProjection projection = interaction.learnerProjection();
        TaskView task = projection == null ? null : new TaskView(
                projection.locale(),
                projection.taskText(),
                projection.answerFields().stream()
                        .map(field -> new TaskView.AnswerFieldView(
                                field.id(), field.label(), field.kind(),
                                field.variables(), field.acceptedInputFamilies(), field.required()))
                        .toList(),
                projection.submissionRule().maxFormalSubmissions());
        TeachingProjection teachingProjection = interaction.teachingProjection();
        TeachingView teaching = teachingProjection == null ? null : new TeachingView(
                teachingProjection.principleSummary(),
                new TeachingView.WorkedExampleView(
                        teachingProjection.workedExample().problem(),
                        teachingProjection.workedExample().steps().stream()
                                .map(step -> new TeachingView.WorkedExampleView.StepView(
                                        step.expression(), step.ruleReference(), step.explanation()))
                                .toList(),
                        teachingProjection.workedExample().finalResult()));
        AssistanceConsentView consentView = interaction.assistanceConsent();
        ConsentView consent = consentView == null ? null : new ConsentView(consentView.warning());
        cn.lunalhx.ai.kilnai.domain.apply.model.HintView hintView = interaction.hint();
        HintView hint = hintView == null ? null : new HintView(
                hintView.level(), hintView.disclosureKind(), hintView.learnerContent(),
                hintView.reasoningSteps(), hintView.proposedFinalAnswer());
        return new LearningFlowResponse(
                interaction.flowId(),
                interaction.interactionVersion(),
                kindOf(interaction.kind()),
                interaction.status().name(),
                interaction.stage().name(),
                interaction.attemptId(),
                interaction.attemptPurpose() == null ? null : interaction.attemptPurpose().name(),
                task,
                teaching,
                consent,
                hint,
                interaction.learnerMessage(),
                allowedEvents(interaction),
                progressOf(interaction.flowId()));
    }

    /**
     * The closed command names the learner may issue against the committed
     * interaction, derived from the active Profile's interaction contract and
     * the Guard-legal actions of each union member. A task boundary carries
     * its Profile-owned learner events; a teaching boundary carries its
     * Explain events; an assistance-consent request offers the accept/refuse
     * decision or leaving; an unavailable node offers retry and Flow Control
     * until its Retry Chain is exhausted; a terminal transition offers
     * nothing further.
     */
    private List<String> allowedEvents(LearningFlowInteraction interaction) {
        return switch (interaction.kind()) {
            case TASK -> interaction.learnerProjection().allowedEvents().stream()
                    .map(LearningFlowResponseMapper::commandOf)
                    .distinct()
                    .toList();
            case TEACHING -> interaction.teachingProjection().allowedEvents().stream()
                    .map(LearningFlowResponseMapper::commandOf)
                    .distinct()
                    .toList();
            case ASSISTANCE_CONSENT -> List.of("assistance_decided", "flow_control_requested");
            case UNAVAILABLE -> retryEvents(interaction);
            case TRANSITION -> List.of();
        };
    }

    /**
     * An unavailable boundary advertises retry and Flow Control until its
     * Retry Chain is exhausted, after which only leaving remains (ADR-0069).
     */
    private List<String> retryEvents(LearningFlowInteraction interaction) {
        boolean retry = flowStore.pendingOperation(interaction.flowId())
                .map(pending -> pending.retryAdvertised())
                .orElse(false);
        return retry
                ? List.of("retry_requested", "flow_control_requested")
                : List.of("flow_control_requested");
    }

    private static String commandOf(ApplyLearnerEvent event) {
        return switch (event) {
            case ANSWER_SUBMITTED -> "answer_submitted";
            case PROCEDURAL_CLARIFICATION, CLARIFICATION_ASKED -> "clarification_asked";
            case CONTINUE_REQUESTED -> "continue_requested";
            case HINT_REQUESTED -> "hint_requested";
            case FLOW_CONTROL -> "flow_control_requested";
        };
    }

    private static String kindOf(InteractionKind kind) {
        return switch (kind) {
            case TASK -> "task";
            case TEACHING -> "teaching";
            case ASSISTANCE_CONSENT -> "assistance_consent";
            case TRANSITION -> "transition";
            case UNAVAILABLE -> "unavailable";
        };
    }

    public ProgressView progressOf(UUID flowId) {
        return flowStore.findFlow(flowId)
                .map(flow -> progressOf(flow.learnerId(), flow.conceptId()))
                .orElse(null);
    }

    public ProgressView progressOf(UUID learnerId, UUID conceptId) {
        ConceptProgress progress = progressProjector.projectFor(flowStore, learnerId, conceptId);
        return new ProgressView(
                progress.currentMilestone().name(),
                progress.highestMilestoneReached().name(),
                progress.currentStage().name());
    }

    /**
     * The learner-safe response of an unavailable Review start: the Flow's
     * actual durable state plus the shared neutral message, never a fabricated
     * interaction. The Review Task itself stays Due and startable.
     */
    public LearningFlowResponse unavailable(UUID flowId, String learnerMessage) {
        LearningFlowInteraction latest = flowStore.latestInteraction(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"));
        return new LearningFlowResponse(
                latest.flowId(),
                latest.interactionVersion(),
                "unavailable",
                latest.status().name(),
                latest.stage().name(),
                null,
                null,
                null,
                null,
                null,
                null,
                learnerMessage,
                List.of("flow_control_requested"),
                progressOf(latest.flowId()));
    }
}
