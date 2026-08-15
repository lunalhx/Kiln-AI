package cn.lunalhx.ai.kilnai.trigger.schedule;

import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewDueTransitionUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * The conventional per-minute trigger that runs the deterministic Review
 * due-transition use case. It never calls a model, generates a Task Package,
 * creates an Attempt, records Exposure or Evidence, or resumes a Learning
 * Flow; a missed task simply stays Due until the learner starts it.
 */
@Component
public class ReviewDueScheduler {

    private final ReviewDueTransitionUseCase useCase;

    public ReviewDueScheduler(ReviewDueTransitionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
    }

    @Scheduled(fixedDelay = 60_000)
    public void tick() {
        useCase.markDueReviewsDue();
    }
}
