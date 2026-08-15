package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse.ProgressView;
import cn.lunalhx.ai.kilnai.api.dto.ReviewTaskView;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
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
 * neutral message with the Flow's actual durable state and leaves the Review
 * Due. Only safe fields, the learner projection, and the safe Concept
 * Progress projection are ever exposed.
 */
@RestController
@RequestMapping("/api/apply/reviews")
public class ReviewFlowController {

    private final ReviewCollectionUseCase collectionUseCase;
    private final ReviewStartFlow startFlow;
    private final LearningFlowStore flowStore;
    private final ApplyFlowResponseMapper responseMapper;

    public ReviewFlowController(
            ReviewCollectionUseCase collectionUseCase,
            ReviewStartFlow startFlow,
            LearningFlowStore flowStore,
            ApplyFlowResponseMapper responseMapper
    ) {
        this.collectionUseCase = Objects.requireNonNull(collectionUseCase, "collectionUseCase must not be null");
        this.startFlow = Objects.requireNonNull(startFlow, "startFlow must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.responseMapper = Objects.requireNonNull(responseMapper, "responseMapper must not be null");
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
            case ReviewStartResult.Boundary boundary -> responseMapper.toResponse(boundary.interaction());
            case ReviewStartResult.Unavailable unavailable -> unavailableResponse(unavailable);
        };
    }

    /**
     * The unavailable start never commits an interaction, so the response
     * carries the shared neutral message over the Flow's actual durable state
     * — the Review Task itself stays Due and startable.
     */
    private ApplyFlowResponse unavailableResponse(ReviewStartResult.Unavailable unavailable) {
        return flowStore.latestInteraction(unavailable.flowId())
                .map(latest -> new ApplyFlowResponse(
                        latest.flowId(),
                        latest.interactionVersion(),
                        latest.status().name(),
                        latest.stage().name(),
                        null,
                        null,
                        null,
                        unavailable.learnerMessage(),
                        List.of(),
                        responseMapper.progressOf(latest.flowId())))
                .orElseGet(() -> new ApplyFlowResponse(
                        unavailable.flowId(),
                        0,
                        FlowStatus.TERMINAL.name(),
                        LearningStage.DELAYED_REVIEW.name(),
                        null,
                        null,
                        null,
                        unavailable.learnerMessage(),
                        List.of(),
                        responseMapper.progressOf(unavailable.flowId())));
    }

    private ProgressView progress(ConceptProgress progress) {
        return new ProgressView(
                progress.currentMilestone().name(),
                progress.highestMilestoneReached().name(),
                progress.currentStage().name());
    }
}
