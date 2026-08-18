package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.ReviewTaskCancellationResponse;
import cn.lunalhx.ai.kilnai.api.dto.ReviewTaskView;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCollectionUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCancellationResult;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCancellationUseCase;
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
 * The learner-safe Review Task API: a collection of unfinished work, the
 * idempotent start of one Due Review Task, and the independent explicit
 * cancellation resource. Starting a Due Review returns the same
 * learner-facing Learning Flow representation used by the unified Learning
 * Flow API; an unavailable generation outcome returns the shared neutral
 * message with the Flow's actual durable state and leaves the Review Due.
 * Cancellation returns only committed Review, Flow, and Concept Progress
 * projections.
 */
@RestController
@RequestMapping("/api/review-tasks")
public class ReviewTaskController {

    private final ReviewCollectionUseCase collectionUseCase;
    private final ReviewStartFlow startFlow;
    private final ReviewCancellationUseCase cancellationUseCase;
    private final LearningFlowStore flowStore;
    private final LearningFlowResponseMapper responseMapper;

    public ReviewTaskController(
            ReviewCollectionUseCase collectionUseCase,
            ReviewStartFlow startFlow,
            ReviewCancellationUseCase cancellationUseCase,
            LearningFlowStore flowStore,
            LearningFlowResponseMapper responseMapper
    ) {
        this.collectionUseCase = Objects.requireNonNull(collectionUseCase, "collectionUseCase must not be null");
        this.startFlow = Objects.requireNonNull(startFlow, "startFlow must not be null");
        this.cancellationUseCase = Objects.requireNonNull(cancellationUseCase, "cancellationUseCase must not be null");
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
    public LearningFlowResponse start(
            @PathVariable UUID reviewId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        ReviewStartResult result = startFlow.start(reviewId, idempotencyKey);
        return switch (result) {
            case ReviewStartResult.Boundary boundary -> responseMapper.toResponse(boundary.interaction());
            case ReviewStartResult.Unavailable unavailable ->
                    responseMapper.unavailable(unavailable.flowId(), unavailable.learnerMessage());
        };
    }

    @PostMapping("/{reviewId}/cancel")
    public ReviewTaskCancellationResponse cancel(
            @PathVariable UUID reviewId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        ReviewCancellationResult result = cancellationUseCase.cancel(reviewId, idempotencyKey);
        LearningFlowResponse flow = result.flowInteraction() == null
                ? flowStore.latestInteraction(result.reviewTask().flowId())
                        .map(responseMapper::toResponse)
                        .orElse(null)
                : responseMapper.toResponse(result.flowInteraction());
        return new ReviewTaskCancellationResponse(
                result.reviewTask().reviewId(),
                result.reviewTask().conceptId(),
                result.reviewTask().status().name(),
                result.reviewTask().reviewNumber(),
                result.reviewTask().dueAt(),
                result.reviewTask().isStartable(),
                progress(result.progress()),
                flow);
    }

    private LearningFlowResponse.ProgressView progress(ConceptProgress progress) {
        return new LearningFlowResponse.ProgressView(
                progress.currentMilestone().name(),
                progress.highestMilestoneReached().name(),
                progress.currentStage().name());
    }
}
