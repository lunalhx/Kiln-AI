package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse.ApplyTaskView;
import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse.ProgressView;
import cn.lunalhx.ai.kilnai.api.dto.ReviewTaskView;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCollectionUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The learner-safe Review Task API: a read-only collection of unfinished work
 * and the idempotent start of one Due Review Task. Starting a Due Review
 * returns the same learner-facing Apply flow representation used by
 * start/query/submit; an unavailable generation outcome returns the shared
 * neutral message and leaves the Review Due. Only safe fields, the learner
 * projection, and the safe Concept Progress projection are ever exposed.
 */
@RestController
@RequestMapping("/api/apply/reviews")
public class ReviewFlowController {

    private final ReviewCollectionUseCase collectionUseCase;
    private final ReviewStartFlow startFlow;
    private final LearningFlowStore flowStore;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public ReviewFlowController(
            ReviewCollectionUseCase collectionUseCase,
            ReviewStartFlow startFlow,
            LearningFlowStore flowStore
    ) {
        this.collectionUseCase = Objects.requireNonNull(collectionUseCase, "collectionUseCase must not be null");
        this.startFlow = Objects.requireNonNull(startFlow, "startFlow must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
    }

    @GetMapping
    public List<ReviewTaskView> list(@RequestParam UUID learnerId) {
        return collectionUseCase.unfinishedFor(learnerId).stream()
                .map(view -> new ReviewTaskView(
                        view.reviewId(),
                        view.conceptId(),
                        view.status().name(),
                        view.reviewNumber(),
                        view.dueAt(),
                        view.startable(),
                        progress(view.progress())))
                .toList();
    }

    @PostMapping("/{reviewId}/start")
    public ApplyFlowResponse start(
            @PathVariable UUID reviewId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        ReviewStartResult result = startFlow.start(reviewId, idempotencyKey);
        return switch (result) {
            case ReviewStartResult.Boundary boundary -> toResponse(boundary.interaction());
            case ReviewStartResult.Unavailable unavailable -> unavailableResponse(unavailable);
        };
    }

    private ApplyFlowResponse toResponse(ApplyFlowInteraction interaction) {
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

    private ApplyFlowResponse unavailableResponse(ReviewStartResult.Unavailable unavailable) {
        int latestVersion = flowStore.latestInteraction(unavailable.flowId())
                .map(ApplyFlowInteraction::interactionVersion)
                .orElse(0);
        return new ApplyFlowResponse(
                unavailable.flowId(),
                latestVersion,
                FlowStatus.TERMINAL.name(),
                LearningStage.DELAYED_REVIEW.name(),
                null,
                null,
                null,
                unavailable.learnerMessage(),
                List.of(),
                progressOf(unavailable.flowId()));
    }

    private ProgressView progressOf(UUID flowId) {
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

    private ProgressView progress(ConceptProgress progress) {
        return new ProgressView(
                progress.currentMilestone().name(),
                progress.highestMilestoneReached().name(),
                progress.currentStage().name());
    }
}
