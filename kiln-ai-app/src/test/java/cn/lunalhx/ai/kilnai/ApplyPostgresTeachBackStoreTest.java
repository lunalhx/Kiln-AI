package cn.lunalhx.ai.kilnai;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PostgreSQL store contract of the anchored Teach-back slice: the anchor
 * ledger (ordering and idempotency), the Teach-back task package with its
 * Practice-purpose Attempt, the one short-text formal submission, and the
 * isolated Teach-back Assessment records all persist atomically and
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

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE teach_back_assessments, teach_back_packages,
                         teach_back_anchors, hint_requests, hint_ladders,
                         review_tasks, exposures, commands, checkpoints,
                         interactions, evidence, assessments, verifications,
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
        artifacts.recordTeachBackAssessment(attemptId, assessment);
        assertEquals(List.of(assessment), artifacts.teachBackAssessmentsFor(attemptId));
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
        artifacts.recordTeachBackAssessment(attempt.attemptId(), new TeachBackAssessment(
                TeachBackAssessment.SCHEMA,
                TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                List.of("unreliable_judgment")));

        assertEquals(new TeachBackAnchor(
                        TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                        anchorId, Instant.parse("2026-08-16T00:01:00Z")),
                flowStore.latestAnchor(flowId).orElseThrow(),
                "the anchor must survive a fresh read");
        assertEquals(taskPackage, artifacts.findTeachBackPackage(taskPackage.taskPackageId()).orElseThrow(),
                "the Teach-back package must survive a fresh read");
        assertEquals(AttemptStatus.OPEN, artifacts.findAttempt(attempt.attemptId()).orElseThrow().status());
        assertEquals(1, artifacts.teachBackAssessmentsFor(attempt.attemptId()).size(),
                "the isolated assessment must survive a fresh read");
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
