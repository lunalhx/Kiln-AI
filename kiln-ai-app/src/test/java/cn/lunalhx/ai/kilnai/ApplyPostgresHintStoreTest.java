package cn.lunalhx.ai.kilnai;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;

import cn.lunalhx.ai.kilnai.domain.apply.model.LearningCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintExposureOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintGenerationDraft;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintRequestRecord;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintView;
import cn.lunalhx.ai.kilnai.domain.apply.model.InteractionKind;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
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
 * The PostgreSQL store contract of the Hint Ladder slice: the stable ladder,
 * the append-only Assistance Trace, the request record, and the Solution
 * Revealed close persist atomically, round-trip across a fresh store, and a
 * crashed command resumes its original exposed level instead of advancing.
 */
@SpringBootTest
@Import(ScriptedApplyPortsConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplyPostgresHintStoreTest {
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
                TRUNCATE active_learning_work, hint_requests, hint_ladders, review_tasks,
                         exposures, commands, checkpoints,
                         interactions, evidence, assessments, verifications,
                         attempts, packages, sources, flows RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void hintExposurePersistsLadderTraceAndRequestAtomicallyAndResumesExactly() {
        TaskPackage practicePackage = practicePackage();
        TaskAttempt attempt = artifacts.openAttempt(practicePackage);
        UUID attemptId = attempt.attemptId();
        HintLadder ladder = ladder(attemptId);

        UUID firstKey = UUID.randomUUID();
        HintExposureOutcome h1 = artifacts.exposeHint(attemptId, ladder, 1, firstKey);
        assertInstanceOf(HintExposureOutcome.Exposed.class, h1);
        TaskAttempt afterH1 = artifacts.findAttempt(attemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, afterH1.status());
        assertEquals(1, afterH1.highestHintLevel());
        assertEquals(List.of("H1:orient"), afterH1.assistanceTraceStrings());
        assertEquals(ladder, artifacts.findLadder(attemptId).orElseThrow(),
                "the stable ladder must persist for later deterministic reveal");

        UUID crashKey = UUID.randomUUID();
        assertInstanceOf(HintExposureOutcome.Exposed.class,
                artifacts.exposeHint(attemptId, ladder, 2, crashKey),
                "the crashed half of the command exposes level two and records the request");
        HintExposureOutcome resumed = artifacts.exposeHint(attemptId, ladder, 2, crashKey);
        HintExposureOutcome.AlreadyExposed already = assertInstanceOf(
                HintExposureOutcome.AlreadyExposed.class, resumed);
        assertEquals(2, already.request().exposedLevel(),
                "the retried command must resume the same exposed level");
        assertEquals(2, artifacts.findAttempt(attemptId).orElseThrow().highestHintLevel(),
                "the resumed exposure must not duplicate a trace entry");
        HintRequestRecord request = artifacts.findHintRequest(attemptId, crashKey).orElseThrow();
        assertEquals(2, request.exposedLevel());
        assertEquals(crashKey, request.commandKey());

        HintExposureOutcome h5 = artifacts.exposeHint(attemptId, ladder, 5, UUID.randomUUID());
        assertInstanceOf(HintExposureOutcome.Exposed.class, h5);
        TaskAttempt revealed = artifacts.findAttempt(attemptId).orElseThrow();
        assertEquals(AttemptStatus.SOLUTION_REVEALED, revealed.status(),
                "the H5 reveal must close the attempt as Solution Revealed");
        assertEquals(5, revealed.highestHintLevel());
        assertTrue(revealed.closedAt() != null);
    }

    @Test
    void aHintInteractionRoundTripsThroughPostgresAndSurvivesARehydratedStore() {
        TaskPackage practicePackage = practicePackage();
        TaskAttempt attempt = artifacts.openAttempt(practicePackage);
        HintLadder ladder = ladder(attempt.attemptId());
        artifacts.exposeHint(attempt.attemptId(), ladder, 1, UUID.randomUUID());

        UUID flowId = UUID.randomUUID();
        flowStore.insertFlow(new LearningFlowStore.FlowRecord(
                flowId, UUID.randomUUID(), UUID.randomUUID(),
                FlowStatus.READY, LearningStage.LEARNING_AND_PRACTICE, PROFILE,
                Instant.parse("2026-08-15T00:00:00Z")));
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                InteractionKind.TASK, flowId, 3, FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.LEARNING_AND_PRACTICE,
                attempt.attemptId(), AttemptPurpose.PRACTICE,
                practicePackage.learnerProjection(),
                null,
                null,
                new HintView(1, "orient", "先明确目标。", null, null),
                null);
        UUID key = UUID.randomUUID();
        flowStore.commitBoundary(interaction,
                new LearningCheckpoint(UUID.randomUUID(), flowId, 3, Instant.parse("2026-08-15T00:00:01Z")),
                new LearningFlowStore.ProcessedCommand(
                        key, "hash", flowId, interaction, Instant.parse("2026-08-15T00:00:01Z")));

        assertEquals(interaction, flowStore.latestInteraction(flowId).orElseThrow(),
                "the committed hint interaction must round-trip exactly");
        assertEquals(interaction, flowStore.findCommand(key).orElseThrow().response());
    }

    private TaskPackage practicePackage() {
        LearnerProjection projection = new LearnerProjection(
                "zh-CN",
                "设 p(x) = 6x³ − 4x + 3，求 p'(x)。",
                List.of(
                        new LearnerProjection.AnswerField("final_derivative", "p'(x)", "mathematical_expression",
                                List.of("x"), List.of("plain_text", "unicode_math", "latex_like"), true),
                        new LearnerProjection.AnswerField("rule_rationale", "理由（可选）", "short_text",
                                null, null, false)),
                List.of(ApplyLearnerEvent.ANSWER_SUBMITTED, ApplyLearnerEvent.PROCEDURAL_CLARIFICATION,
                        ApplyLearnerEvent.FLOW_CONTROL, ApplyLearnerEvent.HINT_REQUESTED),
                new LearnerProjection.SubmissionRule(1));
        return new TaskPackage(
                TaskPackage.SCHEMA, UUID.randomUUID(), AttemptPurpose.PRACTICE, projection,
                new PrivateAssessorProjection(
                        new PrivateAssessorProjection.CanonicalExpectedAnswer("18*x^2 - 4", List.of("x"), "real"),
                        List.of(new PrivateAssessorFacts.RubricMapping(
                                "differentiate-polynomial", List.of("final_derivative"))),
                        List.of(new PrivateAssessorProjection.SourceTraceEntry(
                                "openstax-calculus-v1", "1.0.0", "sec-3.3-differentiation-rules")),
                        new PrivateAssessorFacts.EquivalenceDeclaration("symbolic_expression", List.of("x"), "real"),
                        new PrivateAssessorProjection.TaskFingerprint("profile", "fp-task"),
                        new PrivateAssessorProjection.SolutionFingerprint("profile", "fp-solution"),
                        new PrivateAssessorProjection.ExecutionTrace("apply@1.0.0",
                                "apply.polynomial-differentiation.practice@1.0.0",
                                List.of("apply.task-first@0.1.0"), MODEL_EXECUTION)));
    }

    private HintLadder ladder(UUID attemptId) {
        String json = """
                {
                  "schema": "hint_generation/v1",
                  "outcome": "ladder_ready",
                  "entries": [
                    { "level": 1, "disclosure_kind": "orient", "learner_content": "先明确目标。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ] },
                    { "level": 2, "disclosure_kind": "cue", "learner_content": "使用幂法则与和差法则。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ] },
                    { "level": 3, "disclosure_kind": "strategy", "learner_content": "逐项求导后合并。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ] },
                    { "level": 4, "disclosure_kind": "scaffold", "learner_content": "6x³ 的导数是 18x²。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ] },
                    { "level": 5, "disclosure_kind": "reveal", "learner_content": "完整解答。", "source_trace": [ { "source_document_id": "openstax-calculus-v1", "passage_id": "sec-3.3-differentiation-rules" } ], "reasoning_steps": ["步骤一"], "proposed_final_answer": "18*x^2-4" }
                  ]
                }
                """;
        return HintLadder.from(attemptId, (HintGenerationDraft.LadderReady) HintGenerationDraft.parse(json));
    }
}
