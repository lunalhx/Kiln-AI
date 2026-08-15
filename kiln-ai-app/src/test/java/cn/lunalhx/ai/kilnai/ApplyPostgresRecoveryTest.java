package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.flow.ApplyFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(ScriptedApplyPortsConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplyPostgresRecoveryTest {

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

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE review_tasks, apply_exposures, apply_commands, apply_checkpoints,
                         apply_interactions, apply_evidence, apply_assessments, apply_verifications,
                         apply_attempts, apply_packages, apply_sources, apply_flows RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void anIndependentPassAtomicallySchedulesOneReviewOneDue24HoursLater() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();

        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, startKey);
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);

        AcceptedLearningEvidence evidence = flowStore.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId))
                .findFirst().orElseThrow();
        List<ReviewTask> reviews = reviewStore.unfinishedReviewsFor(learnerId);
        assertEquals(1, reviews.size(), "the independent pass must schedule exactly one Review");
        ReviewTask review = reviews.get(0);
        assertEquals(ReviewTaskStatus.SCHEDULED, review.status());
        assertEquals(1, review.reviewNumber());
        assertEquals(evidence.acceptedAt().plus(Duration.ofHours(24)), review.dueAt(),
                "Review 1 must be due exactly 24 hours after the actual acceptance time");
        assertEquals(completed.interaction().flowId(), review.flowId());
        assertEquals(learnerId, review.learnerId());
    }

    @Test
    void theDatabaseEnforcesAtMostOneUnfinishedReviewPerLearnerAndConcept() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, UUID.randomUUID());
        LearningFlowStore.FlowRecord flow = flowStore.findFlow(started.interaction().flowId()).orElseThrow();
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        reviewStore.acceptEvidenceAndScheduleFirstReview(
                evidence(flow.learnerId(), flow.conceptId(), flow.flowId(),
                        started.interaction().attemptId(), now.minusSeconds(3600)),
                now.plus(Duration.ofHours(24)));
        reviewStore.acceptEvidenceAndScheduleFirstReview(
                evidence(flow.learnerId(), flow.conceptId(), flow.flowId(),
                        started.interaction().attemptId(), now),
                now.plus(Duration.ofHours(25)));

        assertEquals(1, reviewStore.unfinishedReviewsFor(learnerId).size(),
                "the scheduler cancels stale work so at most one unfinished Review survives");
        assertThrows(DuplicateKeyException.class, () -> jdbc.update("""
                        INSERT INTO review_tasks (id, learner_id, concept_id, flow_id, review_number,
                                                  status, due_at, created_at)
                        VALUES (?, ?, ?, ?, 2, 'SCHEDULED', ?, ?)
                        """,
                        UUID.randomUUID(), learnerId, flow.conceptId(), flow.flowId(),
                        java.sql.Timestamp.from(now.plus(Duration.ofHours(26))), java.sql.Timestamp.from(now)),
                "the partial unique index must reject a second unfinished Review for the same learner and Concept");
    }

    @Test
    void theDueTransitionMarksOnlyEligibleScheduledReviewsDueAndIsIdempotent() {
        UUID learnerId = UUID.randomUUID();
        UUID conceptDue = UUID.randomUUID();
        UUID conceptFuture = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-16T11:00:00Z");
        UUID flowDue = UUID.randomUUID();
        UUID flowFuture = UUID.randomUUID();
        flowStore.insertFlow(new LearningFlowStore.FlowRecord(
                flowDue, learnerId, conceptDue, FlowStatus.READY, LearningStage.DIAGNOSTIC, now));
        flowStore.insertFlow(new LearningFlowStore.FlowRecord(
                flowFuture, learnerId, conceptFuture, FlowStatus.READY, LearningStage.DIAGNOSTIC, now));
        jdbc.update("""
                        INSERT INTO review_tasks (id, learner_id, concept_id, flow_id, review_number,
                                                  status, due_at, created_at)
                        VALUES (?, ?, ?, ?, 1, 'SCHEDULED', ?, ?)
                        """,
                UUID.randomUUID(), learnerId, conceptDue, flowDue,
                java.sql.Timestamp.from(now.minus(Duration.ofHours(1))), java.sql.Timestamp.from(now));
        jdbc.update("""
                        INSERT INTO review_tasks (id, learner_id, concept_id, flow_id, review_number,
                                                  status, due_at, created_at)
                        VALUES (?, ?, ?, ?, 1, 'SCHEDULED', ?, ?)
                        """,
                UUID.randomUUID(), learnerId, conceptFuture, flowFuture,
                java.sql.Timestamp.from(now.plus(Duration.ofHours(1))), java.sql.Timestamp.from(now));

        int transitions = reviewStore.markDueReviewsDue(now);

        assertEquals(1, transitions, "only the arrived Scheduled Review may become Due");
        List<ReviewTask> reviews = reviewStore.unfinishedReviewsFor(learnerId);
        assertEquals(2, reviews.size());
        assertEquals(ReviewTaskStatus.DUE,
                reviews.stream().filter(review -> review.conceptId().equals(conceptDue))
                        .findFirst().orElseThrow().status());
        assertEquals(ReviewTaskStatus.SCHEDULED,
                reviews.stream().filter(review -> review.conceptId().equals(conceptFuture))
                        .findFirst().orElseThrow().status(),
                "the pre-due Review must stay Scheduled");
        assertEquals(0, reviewStore.markDueReviewsDue(now), "repeated ticks must be idempotent");
        assertEquals(2, reviewStore.unfinishedReviewsFor(learnerId).size(),
                "the at-most-one-unfinished-per-learner-and-Concept invariant must survive the tick");
    }

    @Test
    void aDueReviewStartDurablyBindsPackageAttemptExposureStartedStateInteractionAndCommand() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();

        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, startKey);
        UUID flowId = started.interaction().flowId();
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        useCase.submit(flowId, 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        ReviewTask due = reviewStore.unfinishedReviewsFor(learnerId).get(0);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        UUID reviewKey = UUID.randomUUID();
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary) reviewStart.start(
                due.reviewId(), reviewKey);
        ApplyFlowInteraction reviewInteraction = reviewBoundary.interaction();
        assertEquals(LearningStage.DELAYED_REVIEW, reviewInteraction.stage());
        assertEquals(AttemptPurpose.REVIEW, reviewInteraction.attemptPurpose());
        assertEquals(4, reviewInteraction.interactionVersion());

        ReviewTask startedReview = reviewStore.findReview(due.reviewId()).orElseThrow();
        assertEquals(ReviewTaskStatus.STARTED, startedReview.status());
        assertNotNull(startedReview.startedAt());
        assertEquals(3, artifacts.allPackages().size(),
                "the durable start must persist exactly one Review Package");
        assertEquals(AttemptStatus.OPEN,
                artifacts.findAttempt(reviewInteraction.attemptId()).orElseThrow().status());
        assertEquals(3, flowStore.exposedTaskFingerprints(flowId).size(),
                "the durable start must record the Review exposure");
        assertEquals(3, flowStore.exposedSolutionFingerprints(flowId).size());
        assertEquals(reviewInteraction, flowStore.latestInteraction(flowId).orElseThrow());
        assertEquals(reviewInteraction, useCase.query(flowId),
                "query must recover the exact Review interaction after commit");
        assertEquals(reviewInteraction, flowStore.findCommand(reviewKey).orElseThrow().response(),
                "the start command must be durably persisted with its idempotency key");

        ReviewStartResult.Boundary replayed = (ReviewStartResult.Boundary) reviewStart.start(
                due.reviewId(), reviewKey);
        assertEquals(reviewInteraction, replayed.interaction());
        assertEquals(3, artifacts.allPackages().size(),
                "a durable replay must never create a second Package or Attempt");

        assertThrows(ApplicationException.class,
                () -> reviewStart.start(due.reviewId(), UUID.randomUUID()),
                "a different-key second start must conflict");
        assertEquals(3, artifacts.allPackages().size());
        assertEquals(1, reviewStore.unfinishedReviewsFor(learnerId).size(),
                "the at-most-one unfinished Review invariant must survive the start");
    }

    @Test
    void aBindAgainstANonDueReviewWritesNothingAndNeverCreatesOrphans() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, startKey);
        LearningFlowStore.FlowRecord flow = flowStore.findFlow(started.interaction().flowId()).orElseThrow();
        ReviewTask scheduled = reviewStore.acceptEvidenceAndScheduleFirstReview(
                evidence(flow.learnerId(), flow.conceptId(), flow.flowId(),
                        started.interaction().attemptId(), Instant.parse("2026-08-15T00:00:00Z")),
                Instant.parse("2026-08-16T00:00:00Z"));
        UUID bindKey = UUID.randomUUID();
        ReviewTaskStore.ReviewStartBind bind = new ReviewTaskStore.ReviewStartBind(
                scheduled.reviewId(),
                Instant.parse("2026-08-16T02:00:00Z"),
                flow.flowId(),
                artifacts.allPackages().get(0),
                99,
                bindKey,
                "hash");

        assertTrue(reviewStore.bindStartedReview(bind).isEmpty(),
                "a not-yet-Due Review must never be claimed by a bind");
        assertEquals(ReviewTaskStatus.SCHEDULED, reviewStore.findReview(scheduled.reviewId()).orElseThrow().status());
        assertEquals(1, artifacts.allPackages().size(),
                "a refused bind must never persist a Package or Attempt");
        assertEquals(1, flowStore.latestInteraction(flow.flowId()).orElseThrow().interactionVersion(),
                "a refused bind must never advance the Flow interaction");
        assertTrue(flowStore.findCommand(bindKey).isEmpty(),
                "a refused bind must never persist its command");
        assertEquals(0, flowStore.exposedTaskFingerprints(flow.flowId()).size(),
                "a refused bind must never record Exposure");
    }

    private AcceptedLearningEvidence evidence(UUID learnerId, UUID conceptId, UUID flowId, UUID taskAttemptId, Instant acceptedAt) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(), taskAttemptId, flowId, conceptId, learnerId,
                LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0, List.of(), acceptedAt);
    }

    @Test
    void aRestartedFlowResumesAnOpenAttemptAndAReplayedKeyReturnsTheOriginalResult() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();

        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, startKey);
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, started.interaction().status());
        assertEquals(AttemptStatus.OPEN,
                artifacts.findAttempt(started.interaction().attemptId()).orElseThrow().status());

        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(2, transitioned.interaction().interactionVersion());
        assertEquals(2, artifacts.allPackages().size());

        ApplyFlowResult.Boundary replayed = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(transitioned.interaction(), replayed.interaction(),
                "a replayed key must return the original result");

        UUID independentKey = UUID.randomUUID();
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 2, independentKey, transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        assertEquals(1, flowStore.allEvidence().size());
        assertTrue(flowStore.evidenceExists(transitioned.interaction().attemptId()));
        assertTrue(artifacts.assessmentsFor(transitioned.interaction().attemptId()).size() >= 1,
                "the isolated assessment record must be persisted");
        assertTrue(artifacts.verificationsFor(artifacts.findAttempt(started.interaction().attemptId())
                .orElseThrow().taskPackageId()).size() >= 1,
                "the pre-delivery Task Verification record must be persisted");

        ApplyFlowInteraction recoveredQuery = useCase.query(started.interaction().flowId());
        assertEquals(completed.interaction(), recoveredQuery,
                "query must recover the exact terminal interaction after commit");

        ApplyFlowResult.Boundary replayedIndependent = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 2, independentKey, transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(completed.interaction(), replayedIndependent.interaction());
        assertEquals(1, flowStore.allEvidence().size(),
                "a replayed evidence-accepting key must never create a second Evidence");
    }
}
