package cn.lunalhx.ai.kilnai.infrastructure.adapter.repository;

import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyLearnerEvent;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceTraceEntry;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.FinalExpressionJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintLevel;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintRequestRecord;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintView;
import cn.lunalhx.ai.kilnai.domain.apply.model.InteractionKind;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.RationaleJudgment;
import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplyFlowStoreJsonRoundTripTest {

    private static final ModelExecution MODEL_EXECUTION = new ModelExecution(
            "acme/gpt-strong", "acme/gpt-small", 2048, 16_000, 0);

    private final ObjectMapper json = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @Test
    void roundTripsEveryPersistedApplyRecord() throws Exception {
        LearnerProjection projection = new LearnerProjection(
                "zh-CN",
                "设 f(x) = 4x³ − 3x² + 7x − 5，求 f'(x)。",
                List.of(
                        new LearnerProjection.AnswerField("final_derivative", "f'(x)", "mathematical_expression",
                                List.of("x"), List.of("plain_text", "unicode_math", "latex_like"), true),
                        new LearnerProjection.AnswerField("rule_rationale", "理由（可选）", "short_text",
                                null, null, false)),
                List.of(ApplyLearnerEvent.ANSWER_SUBMITTED, ApplyLearnerEvent.PROCEDURAL_CLARIFICATION,
                        ApplyLearnerEvent.FLOW_CONTROL),
                new LearnerProjection.SubmissionRule(1));
        TaskPackage taskPackage = new TaskPackage(
                TaskPackage.SCHEMA, UUID.randomUUID(), AttemptPurpose.DIAGNOSTIC, projection,
                new PrivateAssessorProjection(
                        new PrivateAssessorProjection.CanonicalExpectedAnswer(
                                "12*x^2 - 6*x + 7", List.of("x"), "real"),
                        List.of(), List.of(),
                        new PrivateAssessorFacts.EquivalenceDeclaration(
                                "symbolic_expression", List.of("x"), "real"),
                        new PrivateAssessorProjection.TaskFingerprint("profile", "fp-task"),
                        new PrivateAssessorProjection.SolutionFingerprint("profile", "fp-solution"),
                        new PrivateAssessorProjection.ExecutionTrace("apply@1.0.0", "bp@1.0.0",
                                List.of("apply.task-first@0.1.0"), MODEL_EXECUTION)));

        assertEquals(taskPackage, roundTrip(taskPackage));
        assertEquals(projection, roundTrip(projection));

        TaskAttempt openAttempt = new TaskAttempt(
                UUID.randomUUID(), taskPackage.taskPackageId(), AttemptPurpose.DIAGNOSTIC,
                AttemptStatus.OPEN, Instant.parse("2026-08-15T00:00:00Z"), null, null, List.of());
        assertEquals(openAttempt, roundTrip(openAttempt));

        TaskSubmission submission = new TaskSubmission(
                new MathematicalAnswer("12x²−6x+7", "12*x^2-6*x+7", AnswerInputFamily.UNICODE_MATH),
                "利用幂法则逐项求导", Instant.parse("2026-08-15T00:00:01Z"));
        assertEquals(submission, roundTrip(submission));

        TaskAttempt closedAttempt = new TaskAttempt(
                openAttempt.attemptId(), taskPackage.taskPackageId(), AttemptPurpose.DIAGNOSTIC,
                AttemptStatus.SUBMITTED, openAttempt.openedAt(), submission.submittedAt(), submission,
                List.of(AssistanceTraceEntry.hint(HintLevel.H1, Instant.parse("2026-08-15T00:00:00Z")),
                        AssistanceTraceEntry.hint(HintLevel.H3, Instant.parse("2026-08-15T00:00:05Z"))));
        assertEquals(closedAttempt, roundTrip(closedAttempt));
        assertEquals(new AttemptCloseOutcome(AttemptCloseOutcome.Result.CLOSED, closedAttempt),
                roundTrip(new AttemptCloseOutcome(AttemptCloseOutcome.Result.CLOSED, closedAttempt)));

        ResponseAssessment assessment = new ResponseAssessment(
                ResponseAssessment.SCHEMA, FinalExpressionJudgment.NOT_REQUESTED,
                RationaleJudgment.NOT_PROVIDED, List.of());
        assertEquals(assessment, roundTrip(assessment));

        TaskVerificationVerdict verdict = new TaskVerificationVerdict(
                TaskVerificationVerdict.SCHEMA, TaskVerificationVerdict.Verdict.PASS,
                Map.of("answer_correctness", TaskVerificationVerdict.CheckResult.PASS), List.of());
        assertEquals(verdict, roundTrip(verdict));

        SourceArtifact source = new SourceArtifact("openstax-calculus-v1-3.3", "1.0.0",
                List.of(new ApplyExecutionContext.SourcePassage(
                        "openstax-calculus-v1", "1.0.0", "sec-3.3", "en", "Differentiation rules.")));
        assertEquals(source, roundTrip(source));

        AcceptedLearningEvidence evidence = new AcceptedLearningEvidence(
                UUID.randomUUID(), closedAttempt.attemptId(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0,
                List.of(), Instant.parse("2026-08-15T00:00:02Z"));
        assertEquals(evidence, roundTrip(evidence));

        UUID flowId = UUID.randomUUID();
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                InteractionKind.TASK, flowId, 1, FlowStatus.AWAITING_LEARNER_INPUT, LearningStage.DIAGNOSTIC,
                openAttempt.attemptId(), AttemptPurpose.DIAGNOSTIC, projection, null, null, null, null);
        assertEquals(interaction, roundTrip(interaction));

        TeachingProjection teaching = new TeachingProjection(
                "多项式求导逐项进行。",
                new TeachingProjection.WorkedExample(
                        "设 f(x) = 5x³ − 2x² + 7，求 f'(x)。",
                        List.of(new TeachingProjection.Step(
                                "d/dx[5x³] = 5 · d/dx[x³]", "constant-multiple rule", "提出系数 5。")),
                        "15x² − 4x"),
                List.of(ApplyLearnerEvent.CONTINUE_REQUESTED, ApplyLearnerEvent.CLARIFICATION_ASKED,
                        ApplyLearnerEvent.FLOW_CONTROL));
        LearningFlowInteraction teachingInteraction = new LearningFlowInteraction(
                InteractionKind.TEACHING, flowId, 2, FlowStatus.AWAITING_LEARNER_INPUT,
                LearningStage.LEARNING_AND_PRACTICE,
                null, null, null, null, teaching, null, null);
        assertEquals(teachingInteraction, roundTrip(teachingInteraction));

        HintView hint = new HintView(3, "strategy", "先对每一项分别求导。",
                null, null);
        assertEquals(hint, roundTrip(hint));
        HintView reveal = new HintView(5, "reveal", "完整解答", List.of("步骤一", "步骤二"), "12*x^2-6*x+7");
        assertEquals(reveal, roundTrip(reveal));

        HintRequestRecord request = new HintRequestRecord(
                openAttempt.attemptId(), UUID.randomUUID(), 2, 2,
                Instant.parse("2026-08-15T00:00:05Z"));
        assertEquals(request, roundTrip(request));

        LearningCheckpoint checkpoint = new LearningCheckpoint(
                UUID.randomUUID(), flowId, 1, Instant.parse("2026-08-15T00:00:03Z"));
        assertEquals(checkpoint, roundTrip(checkpoint));

        LearningFlowStore.ProcessedCommand command = new LearningFlowStore.ProcessedCommand(
                UUID.randomUUID(), "hash", flowId, interaction, Instant.parse("2026-08-15T00:00:04Z"));
        assertEquals(command, roundTrip(command));

        ReviewTask review = new ReviewTask(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), flowId, 1,
                ReviewTaskStatus.SCHEDULED, Instant.parse("2026-08-16T00:00:00Z"),
                Instant.parse("2026-08-15T00:00:00Z"), null, null, null, null);
        assertEquals(review, roundTrip(review));

        ReviewTask startedWithReplacement = new ReviewTask(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), flowId, 1,
                ReviewTaskStatus.STARTED, Instant.parse("2026-08-16T00:00:00Z"),
                Instant.parse("2026-08-15T00:00:00Z"), Instant.parse("2026-08-16T01:00:00Z"),
                UUID.randomUUID(), null, null);
        assertEquals(startedWithReplacement, roundTrip(startedWithReplacement));

        TeachBackAnchor anchor = new TeachBackAnchor(
                TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                UUID.randomUUID(), Instant.parse("2026-08-16T02:00:00Z"));
        assertEquals(anchor, roundTrip(anchor));
        TeachBackAnchor revealAnchor = new TeachBackAnchor(
                TeachBackAnchor.TeachBackAnchorKind.H5_SOLUTION_REVEAL,
                UUID.randomUUID(), Instant.parse("2026-08-16T02:01:00Z"));
        assertEquals(revealAnchor, roundTrip(revealAnchor));

        TeachBackTaskPackage teachBackPackage = new TeachBackTaskPackage(
                TeachBackTaskPackage.SCHEMA, UUID.randomUUID(), AttemptPurpose.PRACTICE, projection,
                new TeachBackTaskPackage.TeachBackPrivateProjection(
                        List.of(
                                new TeachBackTaskPackage.RubricDimension("rule_identification", "differentiate-polynomial"),
                                new TeachBackTaskPackage.RubricDimension("applicability_explanation", "differentiate-polynomial"),
                                new TeachBackTaskPackage.RubricDimension("steps_result_coherence", "differentiate-polynomial")),
                        List.of(new TeachBackTaskPackage.SourceTraceEntry("openstax-calculus-v1", "sec-3.3")),
                        new TeachBackTaskPackage.AnchorReference(UUID.randomUUID(), "EXPLAIN_WORKED_EXAMPLE"),
                        new TeachBackTaskPackage.ExecutionTrace("teach-back@1.0.0",
                                List.of("teach-back.anchored-explanation@1.0.0"), MODEL_EXECUTION)));
        assertEquals(teachBackPackage, roundTrip(teachBackPackage));
        assertEquals(teachBackPackage.privateProjection(), roundTrip(teachBackPackage.privateProjection()));

        TeachBackAssessment teachBackAssessment = new TeachBackAssessment(
                TeachBackAssessment.SCHEMA,
                TeachBackAssessment.DimensionJudgment.PASS,
                TeachBackAssessment.DimensionJudgment.PASS,
                TeachBackAssessment.DimensionJudgment.INCONCLUSIVE,
                List.of("unreliable_judgment"));
        assertEquals(teachBackAssessment, roundTrip(teachBackAssessment));

        TeachBackAssessmentContext teachBackContext = new TeachBackAssessmentContext(
                "请解释刚才的解题思路。", "完整解答：p'(x) = 18x² − 4。", "用了幂法则与和差法则。",
                AttemptPurpose.PRACTICE);
        assertEquals(teachBackContext, roundTrip(teachBackContext));
    }

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(T value) throws Exception {
        String payload = json.writeValueAsString(value);
        return (T) json.readValue(payload, value.getClass());
    }
}
