package cn.lunalhx.ai.kilnai.domain.learning.service;

import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReviewCancellationUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-18T00:00:00Z"), ZoneOffset.UTC);
    private static final UUID LEARNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CONCEPT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void scheduledCancellationIsTerminalIdempotentAndReleasesActiveWork() {
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK);
        ReviewTask review = new ReviewTaskScheduler(store)
                .acceptEvidenceAndScheduleFirstReview(independentPass())
                .orElseThrow();
        ReviewCancellationUseCase useCase = new ReviewCancellationUseCase(store, store, CLOCK);

        UUID cancellationKey = UUID.randomUUID();
        ReviewCancellationResult cancelled = useCase.cancel(review.reviewId(), cancellationKey);

        assertEquals(ReviewTaskStatus.CANCELLED, cancelled.reviewTask().status());
        assertEquals(ReviewTaskStatus.CANCELLED,
                store.findReview(review.reviewId()).orElseThrow().status());
        assertEquals(List.of(), store.unfinishedReviewsFor(LEARNER_ID));
        assertFalse(store.activeWorkFlowId(LEARNER_ID, CONCEPT_ID).isPresent());
        assertNull(cancelled.flowInteraction());
        assertEquals(cancelled, useCase.cancel(review.reviewId(), cancellationKey));
    }

    @Test
    void startedCancellationAbandonsAttemptCommitsTerminalFlowAndDoesNotDuplicateOnReplay() {
        InMemoryArtifactStore artifacts = new InMemoryArtifactStore(CLOCK);
        InMemoryLearningFlowStore store = new InMemoryLearningFlowStore(CLOCK, artifacts);
        ReviewTask review = new ReviewTaskScheduler(store)
                .acceptEvidenceAndScheduleFirstReview(independentPass())
                .orElseThrow();
        store.markDueReviewsDue(review.dueAt());
        ApplyProfileExecutor executor = new ApplyProfileExecutor(
                ReferenceBundles.stack(),
                new ScriptedApplyGenerationModel(List.of(ApplyScriptData.taskReadyJson())),
                new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict())),
                artifacts);
        TaskPackage base = ((ApplyProfileExecutor.PreparedDelivery.TaskReady) executor.prepareTask(
                ScriptedModelProfile.PROFILE, ReviewApplyFixture.reviewContext())).taskPackage();
        TaskPackage reviewPackage = new TaskPackage(
                TaskPackage.SCHEMA, UUID.randomUUID(), AttemptPurpose.REVIEW,
                base.learnerProjection(), base.privateAssessorProjection());
        LearningFlowInteraction startedInteraction = store.bindReviewAttempt(new ReviewTaskStore.ReviewStartBind(
                review.reviewId(), CLOCK.instant(), review.flowId(), reviewPackage,
                1, UUID.randomUUID(), "review-start-hash")).orElseThrow();
        UUID cancellationKey = UUID.randomUUID();

        ReviewCancellationResult cancelled = new ReviewCancellationUseCase(store, store, CLOCK)
                .cancel(review.reviewId(), cancellationKey);

        assertEquals(ReviewTaskStatus.CANCELLED, cancelled.reviewTask().status());
        assertNotNull(cancelled.flowInteraction());
        assertEquals("TERMINAL", cancelled.flowInteraction().status().name());
        assertEquals(cancelled.flowInteraction(), store.latestInteraction(review.flowId()).orElseThrow());
        assertEquals(cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus.ABANDONED,
                artifacts.findAttempt(startedInteraction.attemptId()).orElseThrow().status());
        assertEquals(1, store.allEvidence().size(), "cancellation must never create Learning Evidence");
        assertFalse(store.activeWorkFlowId(LEARNER_ID, CONCEPT_ID).isPresent());

        ReviewCancellationResult replayed = new ReviewCancellationUseCase(store, store, CLOCK)
                .cancel(review.reviewId(), cancellationKey);
        assertEquals(cancelled, replayed, "the independent ledger must replay the committed outcome");
        assertEquals(cancelled.flowInteraction(), store.latestInteraction(review.flowId()).orElseThrow());
    }

    private AcceptedLearningEvidence independentPass() {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CONCEPT_ID, LEARNER_ID,
                LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0, List.of(), CLOCK.instant());
    }
}
