package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.HintFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewStartFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceTraceEntry;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainTeachingArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLadder;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintRequestRecord;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewStartResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExplainGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.HintGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackAssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackTaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ExplainApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.PracticeApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.TeachBackApplyFixture;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassifierPort;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningFlowCommandUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningStateGraph;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.MasteryMilestone;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPort;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.ApplyFlowMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.PostgresApplyFlowStore;
import cn.lunalhx.ai.kilnai.types.error.ActiveWorkConflictException;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PostgreSQL recovery contract of the Learning/Practice graph (spec:
 * "one PostgreSQL recovery contract for graph checkpoint, Blackboard artifact
 * references, Attempt, Evidence, ReviewTask conversion, and command replay
 * across process restart"). Scripted model ports drive the whole guarded
 * Learning StateGraph, so the contract is the stable regression oracle for
 * exactly-once recovery:
 *
 * <ul>
 *   <li>a failed generation leaves no unaccepted artifact, Attempt, or
 *       Evidence, and a completed command always replays its original
 *       committed interaction;</li>
 *   <li>a crash after the submission closed the Attempt, and a crash after a
 *       Review conversion, are both recoverable with every side effect
 *       happening exactly once;</li>
 *   <li>a fresh store instance (process restart) recovers the graph
 *       checkpoint, Blackboard and artifact references, Hint and teaching
 *       artifacts, the novelty ledgers, Evidence, and the Review Task.</li>
 * </ul>
 */
@SpringBootTest
@Import(ScriptedLearningGraphPortsConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class LearningFlowPostgresRecoveryContractTest {

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
    PostgresApplyFlowStore store;

    @Autowired
    ApplyFlowMapper mapper;

    @Autowired
    ObjectMapper json;

    @Autowired
    Clock clock;

    @Autowired
    ApplyProfileExecutor executor;

    @Autowired
    AssessmentPort assessmentPort;

    @Autowired
    ResponseVerificationPort verificationPort;

    @Autowired
    ExplainGenerationPort explainGeneration;

    @Autowired
    HintGenerationPort hintGeneration;

    @Autowired
    TeachBackGenerationPort teachBackGeneration;

    @Autowired
    TeachBackTaskVerifierPort teachBackVerifier;

    @Autowired
    TeachBackAssessmentPort teachBackAssessment;

    @Autowired
    PedagogyPort pedagogy;

    @Autowired
    ClarificationClassifierPort classifier;

    @Autowired
    ReviewStartFlow reviewStart;

    @Autowired
    ScriptedLearningGraphPortsConfiguration config;

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
    void aFailedReviewGenerationLeavesNoUnacceptedArtifactAttemptOrEvidenceAndACompletedCommandReplaysItsOriginalInteraction() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) graph(store).start(learnerId, startKey);
        UUID flowId = started.interaction().flowId();
        UUID diagnosticKey = UUID.randomUUID();
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) graph(store).submitAnswer(
                flowId, 1, diagnosticKey, started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        UUID independentKey = UUID.randomUUID();
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) graph(store).submitAnswer(
                flowId, 2, independentKey, transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());

        ReviewTask review = store.unfinishedReviewsFor(learnerId).get(0);
        store.markDueReviewsDue(review.dueAt().plusSeconds(1));
        int packagesBefore = store.allPackages().size();
        int attemptsBefore = attemptCount();
        int evidenceBefore = store.allEvidence().size();
        int exposuresBefore = store.exposedTaskFingerprints(flowId).size();

        config.failNextApplyGeneration();
        UUID startReviewKey = UUID.randomUUID();
        ReviewStartResult unavailable = reviewStart.start(review.reviewId(), startReviewKey);
        assertInstanceOf(ReviewStartResult.Unavailable.class, unavailable,
                "a failed Review generation must return the neutral unavailable outcome");

        assertEquals(ReviewTaskStatus.DUE, store.findReview(review.reviewId()).orElseThrow().status(),
                "a failed generation must never claim or advance the Review Task");
        assertEquals(packagesBefore, store.allPackages().size(),
                "a failed generation must not persist an unaccepted Task Package");
        assertEquals(attemptsBefore, attemptCount(),
                "a failed generation must not open a Task Attempt");
        assertEquals(evidenceBefore, store.allEvidence().size(),
                "a failed generation must not create Learning Evidence");
        assertEquals(exposuresBefore, store.exposedTaskFingerprints(flowId).size(),
                "a failed generation must not record an Exposure");
        assertTrue(store.findCommand(startReviewKey).isEmpty(),
                "a failed generation must not record a processed command");

        ReviewStartResult.Boundary retried = (ReviewStartResult.Boundary)
                reviewStart.start(review.reviewId(), startReviewKey);
        assertEquals(ReviewTaskStatus.STARTED, store.findReview(review.reviewId()).orElseThrow().status(),
                "the retried start must claim the untouched Review exactly once");
        assertEquals(packagesBefore + 1, store.allPackages().size(),
                "the retried start must persist exactly one Task Package");
        assertEquals(attemptsBefore + 1, attemptCount(),
                "the retried start must open exactly one Task Attempt");
        assertEquals(evidenceBefore, store.allEvidence().size(),
                "a Review start must never create Evidence");
        ReviewStartResult.Boundary startReplay = (ReviewStartResult.Boundary)
                reviewStart.start(review.reviewId(), startReviewKey);
        assertEquals(retried.interaction(), startReplay.interaction(),
                "a replayed start must return the original committed interaction");
        assertEquals(packagesBefore + 1, store.allPackages().size(),
                "a replayed start must never persist a second Package");

        LearningFlowResult.Boundary replayed = (LearningFlowResult.Boundary) graph(store).submitAnswer(
                flowId, 2, independentKey, transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(completed.interaction(), replayed.interaction(),
                "a completed command must always return its original committed interaction");
        assertEquals(evidenceBefore, store.allEvidence().size(),
                "a replay must never duplicate Evidence");
    }

    @Test
    void aFailedExplainGenerationLeavesNoTeachingArtifactExposureOrAttemptAndTheTerminalBoundaryReplays() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        LearningFlowCommandUseCase useCase = graph(store);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) useCase.start(learnerId, startKey);
        UUID flowId = started.interaction().flowId();

        config.failNextExplainGeneration();
        UUID failKey = UUID.randomUUID();
        LearningFlowResult.Boundary unavailable = (LearningFlowResult.Boundary) useCase.submitAnswer(
                flowId, 1, failKey, started.interaction().attemptId(),
                "3*x^2", "3*x^2", "我猜的");
        assertEquals(FlowStatus.TERMINAL, unavailable.interaction().status(),
                "a failed Explain generation must stop at the terminal unavailable boundary");
        assertEquals(ExplainDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE,
                unavailable.interaction().learnerMessage());

        assertEquals(1, store.allPackages().size(),
                "a failed teaching generation must leave only the Diagnostic Package");
        assertEquals(1, attemptCount(),
                "a failed teaching generation must not open a second Attempt");
        assertTrue(store.allEvidence().isEmpty(),
                "a failed teaching generation must never create Evidence");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM explain_artifacts", Integer.class),
                "a failed teaching generation must not persist an unaccepted teaching artifact");
        assertTrue(store.exposedExampleFingerprints(flowId).isEmpty(),
                "a failed teaching generation must not record a worked-example exposure");
        assertTrue(store.latestAnchor(flowId).isEmpty(),
                "a failed teaching generation must not record a Teach-back anchor");

        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) useCase.submitAnswer(
                flowId, 1, failKey, started.interaction().attemptId(),
                "3*x^2", "3*x^2", "我猜的");
        assertEquals(unavailable.interaction(), replay.interaction(),
                "the replayed failure command must return its original committed terminal interaction");
        assertEquals(1, store.allPackages().size(),
                "a replay must never generate a second candidate package");
    }

    @Test
    void aCrashAfterClosingTheAttemptResumesFromTheSavedSubmissionAndEverySideEffectHappensExactlyOnce() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        LearningFlowCommandUseCase useCase = graph(store);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) useCase.start(learnerId, startKey);
        UUID flowId = started.interaction().flowId();
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) useCase.submitAnswer(
                flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        UUID attemptId = transitioned.interaction().attemptId();

        // The process crashed after the close committed but before the
        // outcome boundary: the Attempt carries its saved submission and the
        // command is unprocessed.
        store.closeAttempt(attemptId, new TaskSubmission(
                new MathematicalAnswer(ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                        ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, AnswerInputFamily.PLAIN_TEXT),
                null, clock.instant()));
        assertEquals(AttemptStatus.SUBMITTED, store.findAttempt(attemptId).orElseThrow().status());
        assertTrue(store.allEvidence().isEmpty(),
                "the crash must leave the closed Attempt without committed Evidence");

        UUID submitKey = UUID.randomUUID();
        LearningFlowResult.Boundary recovered = (LearningFlowResult.Boundary) useCase.submitAnswer(
                flowId, 2, submitKey, attemptId,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(3, recovered.interaction().interactionVersion());
        assertEquals(FlowStatus.TERMINAL, recovered.interaction().status());
        assertEquals(1, store.allEvidence().size(),
                "the retry must resume the evaluation of the saved submission exactly once");
        assertEquals(1, store.unfinishedReviewsFor(learnerId).size(),
                "the resumed transition must still schedule the unique Review 1");
        assertEquals(1, store.assessmentsFor(attemptId).size(),
                "the resumed transition must run exactly one isolated Assessment");

        LearningFlowResult.Boundary replay = (LearningFlowResult.Boundary) useCase.submitAnswer(
                flowId, 2, submitKey, attemptId,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(recovered.interaction(), replay.interaction(),
                "a replay after the commit must return the original committed interaction");
        assertEquals(1, store.allEvidence().size());
        assertEquals(1, store.assessmentsFor(attemptId).size(),
                "a replay must never re-run the assessment");
        assertEquals(1, store.unfinishedReviewsFor(learnerId).size(),
                "a replay must never schedule a second Review");

        LearningFlowCommandUseCase restarted = freshUseCase();
        assertEquals(recovered.interaction(), restarted.query(flowId),
                "a restart must recover the exact committed terminal interaction");
        assertEquals(1, freshStore().allEvidence().size(),
                "the Evidence must survive the restart exactly once");
        assertEquals(1, freshStore().unfinishedReviewsFor(learnerId).size(),
                "the Review 1 must survive the restart exactly once");
    }

    @Test
    void aCrashAfterTheReviewConversionResumesTheTeachingBoundaryAndCancelsTheReviewExactlyOnce() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        LearningFlowCommandUseCase useCase = graph(store);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) useCase.start(learnerId, startKey);
        UUID flowId = started.interaction().flowId();
        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) useCase.submitAnswer(
                flowId, 1, UUID.randomUUID(), started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) useCase.submitAnswer(
                flowId, 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        ReviewTask review = store.unfinishedReviewsFor(learnerId).get(0);
        store.markDueReviewsDue(review.dueAt().plusSeconds(1));
        ReviewStartResult.Boundary reviewBoundary = (ReviewStartResult.Boundary)
                reviewStart.start(review.reviewId(), UUID.randomUUID());
        UUID reviewAttemptId = reviewBoundary.interaction().attemptId();

        LearningFlowResult.Boundary consented = (LearningFlowResult.Boundary) useCase.clarificationAsked(
                flowId, reviewBoundary.interaction().interactionVersion(),
                reviewAttemptId, "为什么幂法则适用？", UUID.randomUUID());
        assertNotNull(consented.interaction().assistanceConsent(),
                "the substantive clarification on the open Review Attempt must project the consent boundary");

        // The process crashed after the conversion half committed but before
        // its boundary: the Attempt is durably Practice with its recorded
        // assistance, the STARTED Review is cancelled, and the command is
        // unprocessed.
        UUID assistKey = UUID.randomUUID();
        store.convertToPractice(reviewAttemptId, List.of(
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.SUBSTANTIVE_CLARIFICATION,
                        clock.instant()),
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.TEMPORARY_EXPLAIN,
                        clock.instant())));
        store.cancelStartedReview(learnerId, DiagnosticApplyFixture.CONCEPT_ID, clock.instant());

        LearningFlowResult.Boundary resumed = (LearningFlowResult.Boundary) useCase.assistanceDecided(
                flowId, consented.interaction().interactionVersion(),
                reviewAttemptId, true, assistKey);
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, resumed.interaction().status());
        assertEquals(LearningStage.LEARNING_AND_PRACTICE, resumed.interaction().stage());
        assertNotNull(resumed.interaction().teachingProjection(),
                "the retried acceptance must resume the committed conversion half as the teaching boundary");

        TaskAttempt attempt = store.findAttempt(reviewAttemptId).orElseThrow();
        assertEquals(AttemptPurpose.PRACTICE, attempt.purpose(),
                "the one-way conversion must never be doubled back");
        assertEquals(AttemptStatus.OPEN, attempt.status());
        assertEquals(List.of("substantive_clarification", "temporary_explain"),
                attempt.assistanceTraceStrings(),
                "the resumed conversion must never append its trace entries twice");
        assertEquals(ReviewTaskStatus.CANCELLED,
                store.findReview(review.reviewId()).orElseThrow().status(),
                "the STARTED Review must be cancelled exactly once");
        assertTrue(store.findStartedReview(learnerId, DiagnosticApplyFixture.CONCEPT_ID).isEmpty(),
                "no STARTED Review may survive the conversion");
        assertEquals(1, store.allEvidence().size(),
                "the conversion must create no Review Evidence");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM explain_artifacts", Integer.class),
                "the resumed acceptance must persist exactly one teaching artifact");
        assertEquals(1, store.exposedExampleFingerprints(flowId).size(),
                "the resumed acceptance must record exactly one worked-example exposure");
        assertEquals(1, jdbc.queryForObject(
                        "SELECT count(*) FROM teach_back_anchors WHERE flow_id = ?",
                        Integer.class, flowId),
                "the resumed acceptance must record exactly one Teach-back anchor");
        ConceptProgress progress =
                new ConceptProgressProjector().projectFor(store, learnerId, DiagnosticApplyFixture.CONCEPT_ID);
        assertEquals(MasteryMilestone.INDEPENDENT, progress.currentMilestone(),
                "the conversion must leave the milestones unchanged");

        LearningFlowCommandUseCase restarted = freshUseCase();
        assertEquals(resumed.interaction(), restarted.query(flowId),
                "a restart must recover the committed teaching boundary");
        PostgresApplyFlowStore fresh = freshStore();
        assertEquals(ReviewTaskStatus.CANCELLED,
                fresh.findReview(review.reviewId()).orElseThrow().status(),
                "the cancellation must survive the restart exactly once");
        assertEquals(1, fresh.allEvidence().size(),
                "the conversion must leave exactly the original Independent Evidence");
    }

    @Test
    void aRestartRecoversTheCheckpointBlackboardReferencesHintAndTeachingArtifactsNoveltyAndEvidence() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        LearningFlowCommandUseCase useCase = graph(store);
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) useCase.start(learnerId, startKey);
        UUID flowId = started.interaction().flowId();
        UUID failKey = UUID.randomUUID();
        LearningFlowResult.Boundary explained = (LearningFlowResult.Boundary) useCase.submitAnswer(
                flowId, 1, failKey, started.interaction().attemptId(),
                "3*x^2", "3*x^2", "我猜的");
        assertNotNull(explained.interaction().teachingProjection(),
                "the failed Diagnostic must route into the Explain teaching boundary");
        TeachBackAnchor anchor = store.latestAnchor(flowId).orElseThrow();
        assertEquals(TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE, anchor.kind());

        UUID continueKey = UUID.randomUUID();
        LearningFlowResult.Boundary practice = (LearningFlowResult.Boundary) useCase.continueRequested(
                flowId, 2, continueKey);
        UUID practiceAttemptId = practice.interaction().attemptId();
        assertNotNull(practiceAttemptId);
        assertEquals(AttemptPurpose.PRACTICE, practice.interaction().attemptPurpose());

        UUID hintKey = UUID.randomUUID();
        LearningFlowResult.Boundary hinted = (LearningFlowResult.Boundary) useCase.requestHint(
                flowId, 3, practiceAttemptId, false, hintKey);
        assertEquals(1, hinted.interaction().hint().level(),
                "the first hint request must expose the persisted H1 level");
        assertEquals(4, hinted.interaction().interactionVersion());

        LearningFlowCommandUseCase restarted = freshUseCase();
        PostgresApplyFlowStore fresh = freshStore();

        LearningFlowInteraction recovered = restarted.query(flowId);
        assertEquals(hinted.interaction(), recovered,
                "a restart must recover the exact committed hint boundary");
        assertEquals(4, fresh.latestCheckpoint(flowId).orElseThrow().interactionVersion(),
                "the graph checkpoint must survive the restart");
        assertEquals(practiceAttemptId, recovered.attemptId(),
                "the Blackboard's open Attempt reference must survive the restart");

        TaskAttempt attempt = fresh.findAttempt(practiceAttemptId).orElseThrow();
        assertEquals(AttemptStatus.OPEN, attempt.status());
        TaskPackage taskPackage = fresh.findPackage(attempt.taskPackageId()).orElseThrow();
        assertEquals(AttemptPurpose.PRACTICE, taskPackage.attemptPurpose());
        assertEquals(ScriptedLearningGraphPortsConfiguration.PRACTICE_TASK,
                taskPackage.learnerProjection().taskText(),
                "the Practice Package must round-trip through the restarted store");

        HintLadder ladder = fresh.findLadder(practiceAttemptId).orElseThrow();
        assertEquals(5, ladder.entries().size(),
                "the persisted Hint ladder must survive the restart");
        HintRequestRecord request = fresh.findHintRequest(practiceAttemptId, hintKey).orElseThrow();
        assertEquals(1, request.exposedLevel(),
                "the Hint request record must survive the restart");
        assertTrue(fresh.findAttempt(practiceAttemptId).orElseThrow().assistanceTraceStrings()
                        .contains("H1:orient"),
                "the exposed H1 assistance must survive the restart");

        ExplainTeachingArtifact artifact = fresh.findExplainArtifact(anchor.anchorId()).orElseThrow();
        assertEquals(anchor.anchorId(), artifact.artifactId(),
                "the teaching artifact referenced by the Teach-back anchor must round-trip");
        assertEquals(anchor, fresh.latestAnchor(flowId).orElseThrow(),
                "the Teach-back anchor ledger must survive the restart");

        assertEquals(2, fresh.exposedTaskFingerprints(flowId).size(),
                "the task novelty ledger must survive the restart");
        assertEquals(2, fresh.exposedSolutionFingerprints(flowId).size(),
                "the solution novelty ledger must survive the restart");
        assertEquals(1, fresh.exposedExampleFingerprints(flowId).size(),
                "the worked-example novelty ledger must survive the restart");
        assertEquals(1, fresh.exposedHintLadderFingerprints(flowId).size(),
                "the hint-ladder novelty ledger must survive the restart");
        assertTrue(fresh.exposedRevealedSolutionFingerprints(flowId).isEmpty(),
                "an H1 exposure must not record a revealed-solution fingerprint");

        assertTrue(fresh.allEvidence().isEmpty(),
                "teaching and hints must never create Evidence");

        LearningFlowResult.Boundary hintReplay = (LearningFlowResult.Boundary) restarted.requestHint(
                flowId, 3, practiceAttemptId, false, hintKey);
        assertEquals(hinted.interaction(), hintReplay.interaction(),
                "a replayed hint command after the restart must return the original committed interaction");
        assertNull(hintReplay.interaction().hint().proposedFinalAnswer(),
                "the learner projection must never expose an unexposed ladder level");
        assertTrue(fresh.allEvidence().isEmpty(),
                "a hint replay must never create Evidence");
    }

    @Test
    void aFailedStartPreparationLeavesNoDurableTraceAndTheOriginalKeyBindsExactlyOnce() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        config.failNextApplyGeneration();

        ApplicationException unavailable = assertThrows(ApplicationException.class,
                () -> graph(store).start(learnerId, startKey));
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, unavailable.errorCode(),
                "an initial Start preparation failure must return the generic 503");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM flows", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM sources", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM packages", Integer.class));
        assertEquals(0, attemptCount());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exposures", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM interactions", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM checkpoints", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM commands", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM verifications", Integer.class),
                "an atomic Start failure must leave no verification audit either");
        assertTrue(store.findCommand(startKey).isEmpty(),
                "an atomic Start failure must not process the command");

        LearningFlowResult.Boundary retried = (LearningFlowResult.Boundary) graph(store).start(learnerId, startKey);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flows", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM sources", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM packages", Integer.class));
        assertEquals(1, attemptCount());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM exposures", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM interactions", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM checkpoints", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM commands", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM verifications", Integer.class),
                "the retried Start must bind the accepted candidate's verification audit once");
        assertEquals(retried.interaction(), store.findCommand(startKey).orElseThrow().response(),
                "the retried Start must process the original Idempotency-Key exactly once");

        LearningFlowResult.Boundary replayed = (LearningFlowResult.Boundary) graph(store).start(learnerId, startKey);
        assertEquals(retried.interaction(), replayed.interaction(),
                "a replayed original key after recovery must return the original committed boundary");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flows", Integer.class),
                "a replay must never bind a second Flow");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM packages", Integer.class),
                "a replay must never bind a second Package");
    }

    @Test
    void aStartConflictReturnsTheExistingFlowIdAndTheClaimIsEnforcedByTheDatabase() {
        UUID learnerId = UUID.randomUUID();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) graph(store).start(learnerId, UUID.randomUUID());
        ActiveWorkConflictException conflict = assertThrows(ActiveWorkConflictException.class,
                () -> graph(store).start(learnerId, UUID.randomUUID()));
        assertEquals(started.interaction().flowId(), conflict.existingFlowId(),
                "the learner-safe conflict must carry only the existing Flow id");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM flows", Integer.class),
                "a conflicting Start must never create a second Flow");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM packages", Integer.class),
                "a conflicting Start must never bind a second Package");
    }

    @Test
    void aTerminalFlowReleasesTheClaimAndAnUnfinishedReviewBlocksANewDiagnostic() {
        UUID learnerId = UUID.randomUUID();
        LearningFlowResult.Boundary started = (LearningFlowResult.Boundary) graph(store).start(learnerId, UUID.randomUUID());
        UUID flowId = started.interaction().flowId();
        LearningFlowResult.Boundary left = (LearningFlowResult.Boundary) graph(store).flowControlRequested(
                flowId, 1, UUID.randomUUID());
        assertEquals(FlowStatus.TERMINAL, left.interaction().status());
        assertEquals(FlowStatus.TERMINAL, store.findFlow(flowId).orElseThrow().status(),
                "a committed terminal boundary must mark the Flow terminal");
        assertTrue(store.activeWorkFlowId(learnerId, DiagnosticApplyFixture.CONCEPT_ID).isEmpty(),
                "a terminal Flow with no unfinished Review releases the Active Work claim");
        LearningFlowResult.Boundary restarted = (LearningFlowResult.Boundary) graph(store).start(learnerId, UUID.randomUUID());
        assertNotEquals(flowId, restarted.interaction().flowId(),
                "the released claim permits a fresh Diagnostic");

        LearningFlowResult.Boundary transitioned = (LearningFlowResult.Boundary) graph(store).submitAnswer(
                restarted.interaction().flowId(), 1, UUID.randomUUID(), restarted.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        LearningFlowResult.Boundary completed = (LearningFlowResult.Boundary) graph(store).submitAnswer(
                restarted.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        assertEquals(1, store.unfinishedReviewsFor(learnerId).size(),
                "the fresh Independent pass must schedule the unique Review 1");
        ActiveWorkConflictException blocked = assertThrows(ActiveWorkConflictException.class,
                () -> graph(store).start(learnerId, UUID.randomUUID()));
        assertEquals(restarted.interaction().flowId(), blocked.existingFlowId(),
                "the unfinished Review blocks a new Diagnostic through its terminal Flow id");
    }

    private int attemptCount() {
        return Integer.valueOf(jdbc.queryForObject(
                "SELECT count(*) FROM attempts", Integer.class));
    }

    private LearningFlowCommandUseCase graph(PostgresApplyFlowStore flowStore) {
        ReviewTaskScheduler scheduler = new ReviewTaskScheduler(flowStore);
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, flowStore, flowStore, assessmentPort, verificationPort,
                DiagnosticApplyFixture.diagnosticContext(), IndependentApplyFixture.independentContext(), clock);
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                flowStore, flowStore, assessmentPort, verificationPort, scheduler, clock);
        PracticeSubmissionFlow practiceFlow = new PracticeSubmissionFlow(
                executor, flowStore, flowStore, assessmentPort, verificationPort,
                PracticeApplyFixture.practiceContext(), IndependentApplyFixture.independentContext(), clock);
        ExplainFlow explainFlow = new ExplainFlow(
                new ExplainProfileExecutor(RecoveryTestBundles.explainStack(), explainGeneration),
                flowStore, flowStore, ExplainApplyFixture.explainContext());
        HintFlow hintFlow = new HintFlow(
                hintGeneration, flowStore, PracticeApplyFixture.practiceContext().conceptSourcePack());
        TeachBackFlow teachBackFlow = new TeachBackFlow(
                new TeachBackProfileExecutor(RecoveryTestBundles.teachBackStack(),
                        teachBackGeneration, teachBackVerifier, flowStore),
                flowStore, flowStore, teachBackAssessment,
                TeachBackApplyFixture.teachBackContext(), clock);
        ReviewSubmissionFlow reviewSubmissionFlow = new ReviewSubmissionFlow(
                flowStore, flowStore, assessmentPort, verificationPort, scheduler, executor, flowStore,
                ReviewApplyFixture.reviewContext(), clock);
        LearningStateGraph graph = new LearningStateGraph(
                flowStore, flowStore, flowStore, diagnosticFlow, independentFlow, practiceFlow,
                reviewSubmissionFlow, explainFlow, hintFlow, teachBackFlow, pedagogy, classifier, clock);
        return new LearningFlowCommandUseCase(
                flowStore, graph, DiagnosticApplyFixture.diagnosticContext(),
                (cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort) () -> new cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile(
                        new cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile.ModelBinding(
                                "openai-compatible", "https://api.test/v1", "acme", "scripted-strong", "TEST_STRONG"),
                        new cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile.ModelBinding(
                                "openai-compatible", "https://api.test/v1", "acme", "scripted-small", "TEST_SMALL"),
                        2048));
    }

    private LearningFlowCommandUseCase freshUseCase() {
        return graph(freshStore());
    }

    private PostgresApplyFlowStore freshStore() {
        return new PostgresApplyFlowStore(mapper, json, clock);
    }
}