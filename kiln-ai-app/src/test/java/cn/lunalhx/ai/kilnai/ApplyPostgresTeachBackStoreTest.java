package cn.lunalhx.ai.kilnai;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyJson;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.CommittedEvaluationResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
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

import java.time.Instant;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PostgreSQL store contract of the anchored Teach-back slice: the anchor
 * ledger (ordering and idempotency), the Teach-back task package with its
 * Practice-purpose Attempt, the one short-text formal submission, and the
 * committed Teach-back evaluation results all persist atomically and
 * round-trip across a fresh store.
 */
@SpringBootTest
@Import(ScriptedApplyPortsConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplyPostgresTeachBackStoreTest {
    private static final ModelProfile PROFILE = new ModelProfile(
            new ModelProfile.ModelBinding("openai-compatible", "https://api.test/v1", "acme", "scripted-strong", "TEST_STRONG"),
            new ModelProfile.ModelBinding("openai-compatible", "https://api.test/v1", "acme", "scripted-small", "TEST_SMALL"),
            2048);

    private static final ModelExecution MODEL_EXECUTION = new ModelExecution(
            "acme/scripted-strong", "acme/scripted-small", 2048, 16_000, 0);


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
    ArtifactStore artifacts;

    @Autowired
    LearningFlowStore flowStore;

    @Autowired
    ApplyFlowMapper mapper;

    @Autowired
    ObjectMapper json;

    @Autowired
    Clock clock;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE active_learning_work, evaluation_results, teach_back_packages,
                         teach_back_anchors, hint_requests, hint_ladders,
                         review_tasks, exposures, commands, checkpoints,
                         interactions, evidence, verifications,
                         attempts, packages, sources, flows RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void theAnchorLedgerPersistsOrderingAndIdempotency() {
        UUID flowId = UUID.randomUUID();
        flowStore.insertFlow(new LearningFlowStore.FlowRecord(
                flowId, UUID.randomUUID(), UUID.randomUUID(),
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.READY,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.LEARNING_AND_PRACTICE,
                PROFILE, Instant.parse("2026-08-16T00:00:00Z")));
        UUID explainAnchorId = UUID.randomUUID();
        UUID revealAnchorId = UUID.randomUUID();
        flowStore.recordAnchor(flowId, new TeachBackAnchor(
                TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                explainAnchorId, Instant.parse("2026-08-16T00:01:00Z")));
        flowStore.recordAnchor(flowId, new TeachBackAnchor(
                TeachBackAnchor.TeachBackAnchorKind.H5_SOLUTION_REVEAL,
                revealAnchorId, Instant.parse("2026-08-16T00:02:00Z")));
        assertEquals(revealAnchorId, flowStore.latestAnchor(flowId).orElseThrow().anchorId(),
                "the most recently exposed eligible anchor wins");
        assertEquals(TeachBackAnchor.TeachBackAnchorKind.H5_SOLUTION_REVEAL,
                flowStore.latestAnchor(flowId).orElseThrow().kind());

        flowStore.recordAnchor(flowId, new TeachBackAnchor(
                TeachBackAnchor.TeachBackAnchorKind.H5_SOLUTION_REVEAL,
                revealAnchorId, Instant.parse("2026-08-16T00:02:00Z")));
        assertEquals(revealAnchorId, flowStore.latestAnchor(flowId).orElseThrow().anchorId(),
                "re-recording the same anchor id must be idempotent for a crashed command");
        assertEquals(2, jdbc.queryForObject(
                "SELECT count(*) FROM teach_back_anchors WHERE flow_id = ?", Integer.class, flowId));
    }

    @Test
    void theTeachBackPackageAndAttemptPersistWithOneShortTextSubmission() {
        TeachBackTaskPackage taskPackage = teachBackPackage();
        TaskAttempt attempt = artifacts.openAttempt(taskPackage);
        UUID attemptId = attempt.attemptId();
        assertEquals(taskPackage, artifacts.findTeachBackPackage(taskPackage.taskPackageId()).orElseThrow(),
                "the Teach-back package must round-trip exactly");
        TaskAttempt opened = artifacts.findAttempt(attemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, opened.status());
        assertEquals(AttemptPurpose.PRACTICE, opened.purpose());
        assertTrue(artifacts.findPackage(taskPackage.taskPackageId()).isEmpty(),
                "a Teach-back package must never surface as an Apply package");

        AttemptCloseOutcome closed = artifacts.closeAttempt(attemptId, new TaskSubmission(
                new MathematicalAnswer("用了幂法则与和差法则。", "用了幂法则与和差法则。", AnswerInputFamily.PLAIN_TEXT),
                null, Instant.parse("2026-08-16T00:05:00Z")));
        assertInstanceOf(AttemptCloseOutcome.Result.class, closed.result());
        TaskAttempt submitted = artifacts.findAttempt(attemptId).orElseThrow();
        assertEquals(AttemptStatus.SUBMITTED, submitted.status());
        assertEquals("用了幂法则与和差法则。",
                submitted.submission().finalDerivative().confirmedCanonical());

        TeachBackAssessment assessment = new TeachBackAssessment(
                TeachBackAssessment.SCHEMA,
                TeachBackAssessment.DimensionJudgment.PASS,
                TeachBackAssessment.DimensionJudgment.PASS,
                TeachBackAssessment.DimensionJudgment.PASS,
                List.of());
        CommittedEvaluationResult committed = artifacts.saveOrReturnCommittedEvaluationResult(
                attemptId, CommittedEvaluationResult.TEACH_BACK_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION, assessment.schema(), ApplyJson.writeContract(assessment));
        CommittedEvaluationResult replayed = artifacts.saveOrReturnCommittedEvaluationResult(
                attemptId, CommittedEvaluationResult.TEACH_BACK_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION,
                TeachBackAssessment.SCHEMA,
                ApplyJson.writeContract(new TeachBackAssessment(
                        TeachBackAssessment.SCHEMA,
                        TeachBackAssessment.DimensionJudgment.FAIL,
                        TeachBackAssessment.DimensionJudgment.FAIL,
                        TeachBackAssessment.DimensionJudgment.FAIL,
                        List.of("different_candidate"))));
        assertEquals(committed.resultId(), replayed.resultId(),
                "a replayed responsibility must return the committed unique-key winner");
        assertEquals(committed.attemptId(), replayed.attemptId());
        assertEquals(committed.responsibility(), replayed.responsibility());
        assertEquals(committed.evaluationVersion(), replayed.evaluationVersion());
        assertEquals(assessment, TeachBackAssessment.parse(replayed.resultPayload()),
                "JSONB normalization must not change the committed evaluation semantics");
        assertEquals(1, artifacts.committedEvaluationResultsFor(attemptId).size());
    }

    @Test
    void concurrentResponsibilitiesReturnOneCommittedUniqueKeyWinner() throws Exception {
        UUID attemptId = artifacts.openAttempt(teachBackPackage()).attemptId();
        artifacts.closeAttempt(attemptId, new TaskSubmission(
                new MathematicalAnswer("用了幂法则与和差法则。", "用了幂法则与和差法则。", AnswerInputFamily.PLAIN_TEXT),
                null, Instant.parse("2026-08-16T00:05:00Z")));
        TeachBackAssessment pass = new TeachBackAssessment(
                TeachBackAssessment.SCHEMA,
                TeachBackAssessment.DimensionJudgment.PASS,
                TeachBackAssessment.DimensionJudgment.PASS,
                TeachBackAssessment.DimensionJudgment.PASS,
                List.of());
        TeachBackAssessment fail = new TeachBackAssessment(
                TeachBackAssessment.SCHEMA,
                TeachBackAssessment.DimensionJudgment.FAIL,
                TeachBackAssessment.DimensionJudgment.FAIL,
                TeachBackAssessment.DimensionJudgment.FAIL,
                List.of("different_candidate"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<CommittedEvaluationResult>> futures = new ArrayList<>();
            for (TeachBackAssessment candidate : List.of(pass, fail)) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    assertTrue(go.await(30, TimeUnit.SECONDS));
                    return artifacts.saveOrReturnCommittedEvaluationResult(
                            attemptId, CommittedEvaluationResult.TEACH_BACK_ASSESSMENT,
                            CommittedEvaluationResult.EVALUATION_VERSION,
                            candidate.schema(), ApplyJson.writeContract(candidate));
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS));
            go.countDown();
            CommittedEvaluationResult first = futures.get(0).get(30, TimeUnit.SECONDS);
            CommittedEvaluationResult second = futures.get(1).get(30, TimeUnit.SECONDS);

            assertEquals(first.resultId(), second.resultId(),
                    "the database unique key must return the committed winner to both callers");
            assertEquals(1, jdbc.queryForObject(
                    "SELECT count(*) FROM evaluation_results WHERE attempt_id = ?",
                    Integer.class, attemptId));
            TeachBackAssessment winner = TeachBackAssessment.parse(second.resultPayload());
            assertTrue(List.of(pass, fail).contains(winner),
                    "downstream callers must receive one of the committed candidates");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void anOpenAttemptCannotCommitAnEvaluationResult() {
        UUID attemptId = artifacts.openAttempt(teachBackPackage()).attemptId();
        TeachBackAssessment assessment = new TeachBackAssessment(
                TeachBackAssessment.SCHEMA,
                TeachBackAssessment.DimensionJudgment.PASS,
                TeachBackAssessment.DimensionJudgment.PASS,
                TeachBackAssessment.DimensionJudgment.PASS,
                List.of());

        assertThrows(IllegalStateException.class, () -> artifacts.saveOrReturnCommittedEvaluationResult(
                attemptId, CommittedEvaluationResult.TEACH_BACK_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION, assessment.schema(), ApplyJson.writeContract(assessment)));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM evaluation_results WHERE attempt_id = ?",
                Integer.class, attemptId));
    }

    @Test
    void teachBackArtifactsSurviveFreshReads() {
        UUID flowId = UUID.randomUUID();
        flowStore.insertFlow(new LearningFlowStore.FlowRecord(
                flowId, UUID.randomUUID(), UUID.randomUUID(),
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus.READY,
                cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage.LEARNING_AND_PRACTICE,
                PROFILE, Instant.parse("2026-08-16T00:00:00Z")));
        UUID anchorId = UUID.randomUUID();
        flowStore.recordAnchor(flowId, new TeachBackAnchor(
                TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                anchorId, Instant.parse("2026-08-16T00:01:00Z")));
        TeachBackTaskPackage taskPackage = teachBackPackage();
        TaskAttempt attempt = artifacts.openAttempt(taskPackage);
        artifacts.closeAttempt(attempt.attemptId(), new TaskSubmission(
                new MathematicalAnswer("用了幂法则与和差法则。", "用了幂法则与和差法则。", AnswerInputFamily.PLAIN_TEXT),
                null, Instant.parse("2026-08-16T00:05:00Z")));
        TeachBackAssessment assessment = new TeachBackAssessment(
                TeachBackAssessment.SCHEMA,
                TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                List.of("unreliable_judgment"));
        artifacts.saveOrReturnCommittedEvaluationResult(
                attempt.attemptId(), CommittedEvaluationResult.TEACH_BACK_ASSESSMENT,
                CommittedEvaluationResult.EVALUATION_VERSION, assessment.schema(), ApplyJson.writeContract(assessment));

        assertEquals(new TeachBackAnchor(
                        TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                        anchorId, Instant.parse("2026-08-16T00:01:00Z")),
                flowStore.latestAnchor(flowId).orElseThrow(),
                "the anchor must survive a fresh read");
        assertEquals(taskPackage, artifacts.findTeachBackPackage(taskPackage.taskPackageId()).orElseThrow(),
                "the Teach-back package must survive a fresh read");
        assertEquals(AttemptStatus.SUBMITTED, artifacts.findAttempt(attempt.attemptId()).orElseThrow().status());
        PostgresApplyFlowStore restartedStore = new PostgresApplyFlowStore(mapper, json, clock);
        CommittedEvaluationResult recovered = restartedStore.findCommittedEvaluationResult(
                        attempt.attemptId(), CommittedEvaluationResult.TEACH_BACK_ASSESSMENT,
                        CommittedEvaluationResult.EVALUATION_VERSION)
                .orElseThrow();
        assertEquals(assessment, TeachBackAssessment.parse(recovered.resultPayload()),
                "a fresh store instance must recover the committed assessment after restart");
        assertEquals(1, restartedStore.committedEvaluationResultsFor(attempt.attemptId()).size(),
                "the isolated assessment must survive a fresh store instance");
    }

    private TeachBackTaskPackage teachBackPackage() {
        LearnerProjection projection = new LearnerProjection(
                "zh-CN",
                "请用简短文字解释刚才的例题中使用了哪些求导法则、为什么这些法则适用，以及这些步骤如何"
                        + "最终得出结果。",
                List.of(new LearnerProjection.AnswerField(
                        "short_text_response", "简短回答", "short_text", null, null, true)),
                List.of(ApplyLearnerEvent.ANSWER_SUBMITTED, ApplyLearnerEvent.CLARIFICATION_ASKED,
                        ApplyLearnerEvent.FLOW_CONTROL),
                new LearnerProjection.SubmissionRule(1));
        return new TeachBackTaskPackage(
                TeachBackTaskPackage.SCHEMA, UUID.randomUUID(), AttemptPurpose.PRACTICE, projection,
                new TeachBackTaskPackage.TeachBackPrivateProjection(
                        List.of(
                                new TeachBackTaskPackage.RubricDimension("rule_identification", "differentiate-polynomial"),
                                new TeachBackTaskPackage.RubricDimension("applicability_explanation", "differentiate-polynomial"),
                                new TeachBackTaskPackage.RubricDimension("steps_result_coherence", "differentiate-polynomial")),
                        List.of(new TeachBackTaskPackage.SourceTraceEntry(
                                "openstax-calculus-v1", "sec-3.3-differentiation-rules")),
                        new TeachBackTaskPackage.AnchorReference(UUID.randomUUID(), "EXPLAIN_WORKED_EXAMPLE"),
                        new TeachBackTaskPackage.ExecutionTrace("teach-back@1.0.0",
                                List.of("teach-back.anchored-explanation@1.0.0",
                                        "subject.calculus-notation@1.0.0"),
                                MODEL_EXECUTION)));
    }
}
