package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ApplyFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.ApplyFlowMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.PostgresApplyFlowStore;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Learning Flow success-path contract over PostgreSQL: the destructive
 * baseline persists Flow, Interaction, Checkpoint, Command, Attempt,
 * Evidence, and Review Task; the committed state of
 * "Diagnostic PASS -&gt; Independent PASS -&gt; Review 1" survives a fresh
 * store instance (process restart) and the Review cadence stays continuous;
 * and concurrent commands never create a duplicate open Attempt, Evidence, or
 * unfinished Review Task.
 */
@SpringBootTest
@Import(ScriptedApplyPortsConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class LearningFlowPostgresSuccessPathTest {

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
    ReviewStartFlow reviewStart;

    @Autowired
    ArtifactStore artifacts;

    @Autowired
    LearningFlowStore flowStore;

    @Autowired
    ReviewTaskStore reviewStore;

    @Autowired
    ApplyFlowMapper mapper;

    @Autowired
    ObjectMapper json;

    @Autowired
    Clock clock;

    @Autowired
    AssessmentPort assessmentPort;

    @Autowired
    ResponseVerificationPort verificationPort;

    @Autowired
    ApplyProfileExecutor executor;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE review_tasks, hint_requests, hint_ladders, teach_back_anchors,
                         teach_back_packages, teach_back_assessments, explain_artifacts,
                         revealed_solution_exposures, hint_ladder_exposures, example_exposures,
                         exposures, commands, checkpoints, interactions, evidence, assessments,
                         verifications, attempts, packages, sources, flows RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void theBaselineKeepsNoApplyPrefixedTablesAndPersistsTheFullLearningFlowSchema() {
        List<String> applyTables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name LIKE 'apply\\_%'
                """, String.class);
        assertTrue(applyTables.isEmpty(),
                "the destructive baseline must not keep any apply_* table: " + applyTables);

        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public'
                """, String.class);
        for (String required : List.of("flows", "interactions", "checkpoints", "commands",
                "attempts", "evidence", "review_tasks")) {
            assertTrue(tables.contains(required),
                    "the baseline must persist the " + required + " table");
        }
    }

    @Test
    void startingAnotherFlowReusesTheImmutableReferenceSourcePack() {
        useCase.start(UUID.randomUUID(), UUID.randomUUID());
        useCase.start(UUID.randomUUID(), UUID.randomUUID());

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM sources", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM flows", Integer.class));
    }

    @Test
    void aRestartRecoversTheCommittedSuccessPathAndTheReviewCadenceContinues() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, UUID.randomUUID());
        UUID flowId = started.interaction().flowId();
        UUID diagnosticKey = UUID.randomUUID();
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, 1, diagnosticKey, started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        UUID independentKey = UUID.randomUUID();
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, 2, independentKey, transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());

        RestartRuntime restarted = restartRuntime();
        ApplyFlowInteraction recovered = restarted.useCase.query(flowId);
        assertEquals(completed.interaction(), recovered,
                "after a restart the learner must see the exact committed terminal interaction");

        LearningFlowStore.FlowRecord flow = restarted.store.findFlow(flowId).orElseThrow();
        assertEquals(learnerId, flow.learnerId());
        assertEquals(DiagnosticApplyFixture.CONCEPT_ID, flow.conceptId());

        ApplyCheckpoint checkpoint = restarted.store.latestCheckpoint(flowId).orElseThrow();
        assertEquals(3, checkpoint.interactionVersion(),
                "the checkpoint must recover the committed boundary");

        TaskAttempt independentAttempt = restarted.store.findAttempt(
                transitioned.interaction().attemptId()).orElseThrow();
        assertEquals(AttemptStatus.SUBMITTED, independentAttempt.status());
        assertNotNull(independentAttempt.submission(),
                "the closed Attempt must retain its committed submission");
        assertNotNull(restarted.store.findPackage(independentAttempt.taskPackageId()).orElseThrow(),
                "the committed Task Package must round-trip through the restarted store");

        ApplyFlowResult.Boundary replayed = (ApplyFlowResult.Boundary) restarted.useCase.submit(
                flowId, 2, independentKey, transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(completed.interaction(), replayed.interaction(),
                "a replayed Idempotency-Key after the restart must return the original committed interaction");
        assertEquals(transitioned.interaction(),
                restarted.store.findCommand(diagnosticKey).orElseThrow().response(),
                "the committed Diagnostic command must survive the restart");

        List<AcceptedLearningEvidence> evidence = restarted.store.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId)).toList();
        assertEquals(1, evidence.size(), "exactly one Independent PASS Evidence must survive the restart");
        assertEquals(LearningResult.PASS, evidence.get(0).result());
        assertEquals(AttemptPurpose.INDEPENDENT_TEST, evidence.get(0).attemptPurpose());

        List<ReviewTask> reviews = restarted.store.unfinishedReviewsFor(learnerId);
        assertEquals(1, reviews.size(), "exactly one Review 1 must survive the restart");
        ReviewTask reviewOne = reviews.get(0);
        assertEquals(ReviewTaskStatus.SCHEDULED, reviewOne.status());
        assertEquals(1, reviewOne.reviewNumber());
        assertEquals(evidence.get(0).acceptedAt().plus(Duration.ofHours(24)), reviewOne.dueAt(),
                "Review 1 must still be due exactly 24 hours after the accepted Independent pass");

        restarted.store.markDueReviewsDue(evidence.get(0).acceptedAt().plus(Duration.ofHours(25)));
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary)
                restarted.reviewStart.start(reviewOne.reviewId(), UUID.randomUUID());
        assertEquals(LearningStage.DELAYED_REVIEW, reviewBoundary.interaction().stage());
        assertEquals(AttemptPurpose.REVIEW, reviewBoundary.interaction().attemptPurpose());

        ApplyFlowResult.Boundary reviewDone = (ApplyFlowResult.Boundary) restarted.useCase.submit(
                flowId, reviewBoundary.interaction().interactionVersion(), UUID.randomUUID(),
                reviewBoundary.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, reviewDone.interaction().status());

        List<ReviewTask> afterPass = restarted.store.unfinishedReviewsFor(learnerId);
        assertEquals(1, afterPass.size(),
                "the Review cadence must stay continuous across the restart");
        assertEquals(2, afterPass.get(0).reviewNumber());
        assertEquals(ReviewTaskStatus.SCHEDULED, afterPass.get(0).status());
        assertEquals(2, restarted.store.allEvidence().stream()
                        .filter(item -> item.learnerId().equals(learnerId)).count(),
                "exactly one Review PASS Evidence must be accepted after the restart");
    }

    @Test
    void concurrentSubmissionsNeverDuplicateEvidenceOrUnfinishedReviewTasks() throws Exception {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, UUID.randomUUID());
        UUID flowId = started.interaction().flowId();
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        UUID attemptId = transitioned.interaction().attemptId();
        UUID key = UUID.randomUUID();

        race(() -> useCase.submit(flowId, 2, key, attemptId,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null));

        assertEquals(1,
                Integer.valueOf(jdbc.queryForObject(
                        "SELECT count(*) FROM evidence WHERE learner_id = ?", Integer.class, learnerId)),
                "concurrent submissions of one Attempt must accept exactly one Evidence");
        assertEquals(1,
                Integer.valueOf(jdbc.queryForObject("""
                                SELECT count(*) FROM review_tasks
                                WHERE learner_id = ? AND status IN ('SCHEDULED', 'DUE', 'STARTED')
                                """, Integer.class, learnerId)),
                "concurrent submissions must schedule exactly one unfinished Review Task");
        assertEquals(AttemptStatus.SUBMITTED,
                artifacts.findAttempt(attemptId).orElseThrow().status(),
                "the single Attempt must be closed exactly once");
        assertEquals(3, flowStore.latestInteraction(flowId).orElseThrow().interactionVersion(),
                "the flow must advance to exactly one committed terminal interaction");
    }

    @Test
    void concurrentReviewStartsNeverCreateADuplicateOpenAttempt() throws Exception {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, UUID.randomUUID());
        UUID flowId = started.interaction().flowId();
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        useCase.submit(flowId, 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        Instant acceptedAt = flowStore.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId))
                .findFirst().orElseThrow().acceptedAt();
        reviewStore.markDueReviewsDue(acceptedAt.plus(Duration.ofHours(25)));
        ReviewTask due = reviewStore.unfinishedReviewsFor(learnerId).get(0);
        int packagesBefore = artifacts.allPackages().size();

        race(() -> reviewStart.start(due.reviewId(), UUID.randomUUID()));

        ReviewTask review = reviewStore.findReview(due.reviewId()).orElseThrow();
        assertEquals(ReviewTaskStatus.STARTED, review.status());
        assertNotNull(review.openAttemptId(),
                "the Review must hold exactly one open Attempt");
        assertEquals(1,
                Integer.valueOf(jdbc.queryForObject(
                        "SELECT count(*) FROM attempts WHERE id = ? AND status = 'OPEN'",
                        Integer.class, review.openAttemptId())));
        assertEquals(packagesBefore + 1, artifacts.allPackages().size(),
                "racing starts must persist exactly one Review Package");
        assertEquals(1,
                Integer.valueOf(jdbc.queryForObject("""
                                SELECT count(*) FROM interactions
                                WHERE flow_id = ? AND interaction_version = 4
                                """, Integer.class, flowId)),
                "racing starts must commit exactly one Delayed Review interaction");
        assertEquals(4, flowStore.latestInteraction(flowId).orElseThrow().interactionVersion());
    }

    /**
     * Fires {@code action} from eight threads at the same instant and waits
     * for every racer to finish. Losing racers may be rejected by the
     * database's uniqueness guards; the committed durable state is what the
     * concurrent contract asserts, so their exceptions are swallowed here.
     */
    private static void race(Runnable action) throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    assertTrue(go.await(30, TimeUnit.SECONDS));
                    try {
                        action.run();
                    } catch (RuntimeException lostRace) {
                        // The committed state is what the contract asserts.
                    }
                    return null;
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS));
            go.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private RestartRuntime restartRuntime() {
        PostgresApplyFlowStore store = new PostgresApplyFlowStore(mapper, json, clock);
        ReviewTaskScheduler scheduler = new ReviewTaskScheduler(store);
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, store, store, assessmentPort, verificationPort,
                DiagnosticApplyFixture.diagnosticContext(),
                IndependentApplyFixture.independentContext(), clock);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                store, store, assessmentPort, verificationPort, scheduler, clock);
        ReviewSubmissionFlow reviewFlow = new ReviewSubmissionFlow(
                store, store, assessmentPort, verificationPort, scheduler, executor, store,
                ReviewApplyFixture.reviewContext(), clock);
        ApplyFlowUseCase freshUseCase = new ApplyFlowUseCase(
                store, store, diagnosticFlow, independentFlow, reviewFlow,
                DiagnosticApplyFixture.diagnosticContext(),
                (cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort) () -> new cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile(
                        new cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile.ModelBinding(
                                "openai-compatible", "https://api.test/v1", "acme", "scripted-strong", "TEST_STRONG"),
                        new cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile.ModelBinding(
                                "openai-compatible", "https://api.test/v1", "acme", "scripted-small", "TEST_SMALL"),
                        2048),
                clock);
        ReviewStartFlow freshReviewStart = new ReviewStartFlow(
                executor, store, store, ReviewApplyFixture.reviewContext(), clock);
        return new RestartRuntime(store, freshUseCase, freshReviewStart);
    }

    private record RestartRuntime(
            PostgresApplyFlowStore store,
            ApplyFlowUseCase useCase,
            ReviewStartFlow reviewStart
    ) {
    }
}
