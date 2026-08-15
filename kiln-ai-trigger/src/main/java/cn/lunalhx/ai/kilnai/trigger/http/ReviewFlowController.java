package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse.ProgressView;
import cn.lunalhx.ai.kilnai.api.dto.ReviewTaskView;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewCollectionUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The learner-safe Review Task collection. It returns only unfinished Review
 * Tasks of the supplied learner, ordered by due time, and exposes only safe
 * fields plus the safe Concept Progress projection; Scheduled work is shown
 * as upcoming but is never startable.
 */
@RestController
@RequestMapping("/api/apply/reviews")
public class ReviewFlowController {

    private final ReviewCollectionUseCase useCase;

    public ReviewFlowController(ReviewCollectionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
    }

    @GetMapping
    public List<ReviewTaskView> list(@RequestParam UUID learnerId) {
        return useCase.unfinishedFor(learnerId).stream()
                .map(view -> new ReviewTaskView(
                        view.reviewId(),
                        view.conceptId(),
                        view.status().name(),
                        view.reviewNumber(),
                        view.dueAt(),
                        false,
                        progress(view.progress())))
                .toList();
    }

    private ProgressView progress(ConceptProgress progress) {
        return new ProgressView(
                progress.currentMilestone().name(),
                progress.highestMilestoneReached().name(),
                progress.currentStage().name());
    }
}
