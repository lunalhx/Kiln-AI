package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.flow.ApplyFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL recovery coverage for the Inconclusive Review path: an
 * inconclusive submission durably binds the replacement as the single open
 * Attempt of the same Started Review, survives a query against a fresh
 * instance, and a replay never duplicates the replacement; when the
 * replacement cannot be prepared, the Review stays Started with no open
 * Attempt and the same start endpoint resumes it over Postgres.
 */
@SpringBootTest
@Import(InconclusiveReviewPortsConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplyPostgresReviewReplacementTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kiln_ai")
            .withUsername("kiln_ai")
            .withPassword("kiln_ai");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplyFlowUseCase useCase;

    @Autowired
    ArtifactStore artifacts;

    @Autowired
    LearningFlowStore flowStore;

    @Autowired
    ReviewTaskStore reviewStore;

    @Autowired
    ReviewStartFlow reviewStart;

    @Autowired
    InconclusiveReviewPortsConfiguration ports;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE review_tasks, exposures, commands, checkpoints,
                         interactions, evidence, assessments, verifications,
                         attempts, packages, sources, flows RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void anInconclusiveSubmissionOverPostgresBindsTheReplacementAndSurvivesRecoveryWithoutDuplication() {
        UUID learnerId = UUID.randomUUID();
        UUID flowId = completeIndependentPass(learnerId);
        ReviewTask due = reviewStore.unfinishedReviewsFor(learnerId).get(0);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        UUID submitKey = UUID.randomUUID();
        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) reviewStart.start(
                due.reviewId(), UUID.randomUUID());
        ApplyFlowResult.Boundary replaced = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                "x^2^3", "x^2^3", null);

        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, replaced.interaction().status());
        assertEquals(LearningStage.DELAYED_REVIEW, replaced.interaction().stage());
        assertEquals(AttemptPurpose.REVIEW, replaced.interaction().attemptPurpose());
        assertNotNull(replaced.interaction().attemptId());
        assertTrue(replaced.interaction().learnerMessage().contains("未能确定"));
        assertFalse(replaced.interaction().learnerMessage().contains("失败"));
        assertFalse(replaced.interaction().learnerProjection().taskText()
                .contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2));

        ReviewTask review = reviewStore.findReview(due.reviewId()).orElseThrow();
        assertEquals(ReviewTaskStatus.STARTED, review.status());
        assertEquals(replaced.interaction().attemptId(), review.openAttemptId(),
                "the replacement must be the Review's single open Attempt");
        assertEquals(AttemptStatus.SUBMITTED,
                artifacts.findAttempt(started.interaction().attemptId()).orElseThrow().status(),
                "the inconclusive submission must be durably closed");
        assertEquals(AttemptStatus.OPEN,
                artifacts.findAttempt(replaced.interaction().attemptId()).orElseThrow().status());
        assertTrue(flowStore.allEvidence().stream()
                .noneMatch(item -> item.attemptPurpose() == AttemptPurpose.REVIEW),
                "an inconclusive submission must accept no Review Evidence");

        assertEquals(replaced.interaction(), useCase.query(flowId),
                "query must recover the exact replacement interaction");
        ApplyFlowResult.Boundary replayed = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, started.interaction().interactionVersion(), submitKey,
                started.interaction().attemptId(),
                "x^2^3", "x^2^3", null);
        assertEquals(replaced.interaction(), replayed.interaction(),
                "a replayed key must return the original replacement result");
        assertEquals(4, artifacts.allPackages().size(),
                "a replay must never create a duplicate replacement");
        assertThrows(ApplicationException.class, () -> reviewStart.start(due.reviewId(), UUID.randomUUID()),
                "a Started Review with an open Attempt is never startable again");

        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, replaced.interaction().interactionVersion(), UUID.randomUUID(),
                replaced.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        assertEquals(1, flowStore.allEvidence().stream()
                        .filter(item -> item.attemptPurpose() == AttemptPurpose.REVIEW).count(),
                "the replacement attempt can complete the Review with exactly one PASS evidence");
        assertEquals(ReviewTaskStatus.COMPLETED, reviewStore.findReview(due.reviewId()).orElseThrow().status());
        List<ReviewTask> unfinished = reviewStore.unfinishedReviewsFor(learnerId);
        assertEquals(1, unfinished.size(), "exactly one successor Review must survive");
        assertEquals(2, unfinished.get(0).reviewNumber());
    }

    @Test
    void anUnpreparedReplacementOverPostgresStaysStartedAndResumesThroughTheStartEndpoint() {
        UUID learnerId = UUID.randomUUID();
        UUID flowId = completeIndependentPass(learnerId);
        ReviewTask due = reviewStore.unfinishedReviewsFor(learnerId).get(0);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ReviewStartResult.Boundary started = (ReviewStartResult.Boundary) reviewStart.start(
                due.reviewId(), UUID.randomUUID());
        ports.failNextReviewGeneration();
        ApplyFlowResult.Boundary unavailable = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, started.interaction().interactionVersion(), UUID.randomUUID(),
                started.interaction().attemptId(),
                "x^2^3", "x^2^3", null);

        assertEquals(FlowStatus.TERMINAL, unavailable.interaction().status());
        assertTrue(unavailable.interaction().learnerMessage().contains("未能确定"));
        assertTrue(unavailable.interaction().learnerMessage().contains("继续"));

        ReviewTask review = reviewStore.findReview(due.reviewId()).orElseThrow();
        assertEquals(ReviewTaskStatus.STARTED, review.status());
        assertNull(review.openAttemptId(),
                "with no prepared replacement the Review must hold no open Attempt");
        assertEquals(3, artifacts.allPackages().size(),
                "an unprepared replacement must create no Package or Attempt");
        assertTrue(flowStore.allEvidence().stream()
                .noneMatch(item -> item.attemptPurpose() == AttemptPurpose.REVIEW));

        ReviewStartResult.Boundary resumed = (ReviewStartResult.Boundary) reviewStart.start(
                due.reviewId(), UUID.randomUUID());
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, resumed.interaction().status());
        assertEquals(LearningStage.DELAYED_REVIEW, resumed.interaction().stage());
        assertNotNull(resumed.interaction().attemptId());
        assertEquals(resumed.interaction().attemptId(), reviewStore.findReview(due.reviewId())
                .orElseThrow().openAttemptId(), "the resumed Review must be bound to its single open Attempt");
        assertEquals(4, artifacts.allPackages().size(),
                "the resume must create exactly one new Package");

        assertThrows(ApplicationException.class, () -> reviewStart.start(due.reviewId(), UUID.randomUUID()),
                "a second resume with a new key must conflict without a duplicate replacement");
        assertEquals(4, artifacts.allPackages().size());

        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, resumed.interaction().interactionVersion(), UUID.randomUUID(),
                resumed.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        assertEquals(ReviewTaskStatus.COMPLETED, reviewStore.findReview(due.reviewId()).orElseThrow().status());
        assertEquals(1, flowStore.allEvidence().stream()
                        .filter(item -> item.attemptPurpose() == AttemptPurpose.REVIEW).count(),
                "the resumed attempt must complete the Review with exactly one PASS evidence");
    }

    private UUID completeIndependentPass(UUID learnerId) {
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, UUID.randomUUID());
        UUID flowId = started.interaction().flowId();
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        useCase.submit(flowId, 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        return flowId;
    }
}
