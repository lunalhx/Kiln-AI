package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.AnswerInputFamily;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptCloseOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.MathematicalAnswer;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionRejectionReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessmentContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext.AnchorView;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackExecutionContext.SourceTraceRef;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackUnavailableReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintView;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackAssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The reference Teach-back flow: the Guard offers Teach-back only while the
 * same Flow carries an eligible, most recently exposed Explain worked example
 * or H5 solution reveal anchor; delivery generates one verified short-text
 * task anchored to that content and opens a Practice-purpose Attempt with
 * exactly one formal submission and no Hint event. One formal submission
 * closes the Attempt and invokes the isolated semantic Assessment whose three
 * Rubric dimensions must all pass for a Teach-back pass; a conclusive pass or
 * fail builds exactly one understanding-dimension Evidence candidate (never
 * Independent Evidence, never lowering Current Mastery) and an Inconclusive
 * judgment builds none. The follow-up Teaching Node is never selected here:
 * the Learning StateGraph derives the legal next moves through the Workflow
 * Guard and the Pedagogy Agent, then accepts the Evidence only after the
 * chosen follow-up node's generation, gating, and verification succeed, so a
 * failed generation leaves no Evidence and the same command can be retried.
 * The flow writes no Learning State; the graph owns the boundary.
 */
public final class TeachBackFlow {

    public static final String TEACH_BACK_AFTER_REVEAL_MESSAGE = "已展示完整解答，请用简短文字解释解题思路与所用规则。";
    public static final String TEACH_BACK_FOLLOW_UP_MESSAGE = "请完成一道新的练习题。";
    public static final String TEACH_BACK_REPLACEMENT_MESSAGE = "请重新用简短文字解释刚才的解题思路。";

    private final TeachBackProfileExecutor executor;
    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final TeachBackAssessmentPort assessmentPort;
    private final TeachBackExecutionContext contextTemplate;
    private final Clock clock;

    public TeachBackFlow(
            TeachBackProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            TeachBackAssessmentPort assessmentPort,
            TeachBackExecutionContext contextTemplate,
            Clock clock
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.assessmentPort = Objects.requireNonNull(assessmentPort, "assessmentPort must not be null");
        this.contextTemplate = Objects.requireNonNull(contextTemplate, "contextTemplate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * The guarded Teach-back delivery: the Workflow Guard offers Teach-back
     * only while the Flow carries an eligible anchor, and the delivered task
     * is generated, gated, and verified before its Attempt opens. Without an
     * eligible anchor, or when the anchor content cannot be resolved, nothing
     * is generated and the neutral unavailable outcome is returned.
     */
    public TeachBackDeliveryResult deliverTeachBack(UUID flowId, ModelProfile profile) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Optional<AnchorView> anchorView = latestAnchorView(flowId);
        if (anchorView.isEmpty()) {
            return new TeachBackDeliveryResult.Unavailable(
                    TeachBackUnavailableReason.NO_ELIGIBLE_ANCHOR,
                    TeachBackDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE);
        }
        return executor.deliver(profile, contextTemplate.withAnchor(anchorView.get()));
    }

    /**
     * The one formal Teach-back submission: atomically closes the open
     * Practice-purpose Attempt with the confirmed short-text response and
     * resolves the isolated semantic Assessment. An already-closed Attempt
     * whose submission produced Evidence is a duplicate and is ignored;
     * otherwise the saved submission's evaluation is resumed.
     */
    public TeachBackSubmissionResult submitTeachBack(
            LearningFlowStore.FlowRecord flow,
            UUID attemptId,
            String rawText,
            String confirmedText
    ) {
        Objects.requireNonNull(flow, "flow must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        CloseOutcome closed = close(attemptId, rawText, confirmedText);
        return switch (closed) {
            case CloseOutcome.Ignored ignored -> new TeachBackSubmissionResult.Ignored(ignored.reason());
            case CloseOutcome.NotSubmittable notSubmittable ->
                    new TeachBackSubmissionResult.NotSubmittable(notSubmittable.reason());
            case CloseOutcome.Closed closedAttempt -> assessAndReturn(flow, closedAttempt.attempt());
            case CloseOutcome.Recovered recovered -> recoverOrIgnore(flow, recovered.attempt());
        };
    }

    private Optional<AnchorView> latestAnchorView(UUID flowId) {
        Optional<TeachBackAnchor> anchor = flowStore.latestAnchor(flowId);
        if (anchor.isEmpty()) {
            return Optional.empty();
        }
        return resolveAnchor(anchor.get());
    }

    /**
     * Resolves the durable anchor into the learner-visible content the
     * learner already saw: the Explain artifact's teaching projection or the
     * H5 reveal view of the closed Practice attempt's stable ladder.
     */
    private Optional<AnchorView> resolveAnchor(TeachBackAnchor anchor) {
        return switch (anchor.kind()) {
            case EXPLAIN_WORKED_EXAMPLE -> artifactStore.findExplainArtifact(anchor.anchorId())
                    .map(artifact -> new AnchorView(
                            artifact.artifactId(),
                            anchor.kind().name(),
                            renderExplain(artifact.learnerProjection()),
                            artifact.sourceTrace().stream()
                                    .map(entry -> new SourceTraceRef(entry.sourceDocumentId(), entry.passageId()))
                                    .toList()));
            case H5_SOLUTION_REVEAL -> artifactStore.findLadder(anchor.anchorId())
                    .map(ladder -> new AnchorView(
                            anchor.anchorId(),
                            anchor.kind().name(),
                            renderH5(ladder.view(5)),
                            ladder.entry(5).sourceTrace().stream()
                                    .map(entry -> new SourceTraceRef(entry.sourceDocumentId(), entry.passageId()))
                                    .toList()));
        };
    }

    private String renderExplain(TeachingProjection projection) {
        StringBuilder text = new StringBuilder(projection.principleSummary());
        TeachingProjection.WorkedExample example = projection.workedExample();
        if (example != null) {
            text.append(' ').append(example.problem());
            for (TeachingProjection.Step step : example.steps()) {
                text.append(' ').append(step.expression())
                        .append(' ').append(step.ruleReference())
                        .append(' ').append(step.explanation());
            }
            text.append(' ').append(example.finalResult());
        }
        return text.toString();
    }

    private String renderH5(HintView reveal) {
        StringBuilder text = new StringBuilder(reveal.learnerContent());
        if (reveal.reasoningSteps() != null) {
            for (String step : reveal.reasoningSteps()) {
                text.append(' ').append(step);
            }
        }
        if (reveal.proposedFinalAnswer() != null) {
            text.append(' ').append(reveal.proposedFinalAnswer());
        }
        return text.toString();
    }

    private CloseOutcome close(UUID attemptId, String rawText, String confirmedText) {
        Optional<TaskAttempt> maybeAttempt = artifactStore.findAttempt(attemptId);
        if (maybeAttempt.isEmpty()) {
            return new CloseOutcome.Ignored(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        }
        TaskAttempt attempt = maybeAttempt.get();
        if (attempt.purpose() != AttemptPurpose.PRACTICE
                || artifactStore.findTeachBackPackage(attempt.taskPackageId()).isEmpty()) {
            return new CloseOutcome.Ignored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
        }
        if (rawText == null || rawText.isBlank() || confirmedText == null || confirmedText.isBlank()) {
            return new CloseOutcome.NotSubmittable(SubmissionRejectionReason.UNPARSEABLE_RAW);
        }
        TaskSubmission submission = new TaskSubmission(
                new MathematicalAnswer(rawText, confirmedText, AnswerInputFamily.PLAIN_TEXT),
                null,
                clock.instant());
        AttemptCloseOutcome closeOutcome = artifactStore.closeAttempt(attemptId, submission);
        if (closeOutcome.result() == AttemptCloseOutcome.Result.ALREADY_CLOSED) {
            return new CloseOutcome.Recovered(closeOutcome.attempt());
        }
        if (closeOutcome.result() != AttemptCloseOutcome.Result.CLOSED) {
            return new CloseOutcome.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return new CloseOutcome.Closed(closeOutcome.attempt());
    }

    private TeachBackSubmissionResult recoverOrIgnore(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        if (flowStore.evidenceExists(closedAttempt.attemptId())) {
            return new TeachBackSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return assessAndReturn(flow, closedAttempt);
    }

    private TeachBackSubmissionResult assessAndReturn(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        Optional<TeachBackTaskPackage> maybePackage =
                artifactStore.findTeachBackPackage(closedAttempt.taskPackageId());
        if (maybePackage.isEmpty()) {
            return new TeachBackSubmissionResult.Unavailable(
                    TeachBackUnavailableReason.NO_ELIGIBLE_ANCHOR,
                    TeachBackDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE);
        }
        TeachBackTaskPackage taskPackage = maybePackage.get();
        Optional<AnchorView> anchorView = latestAnchorView(flow.flowId());
        if (anchorView.isEmpty()) {
            return new TeachBackSubmissionResult.Unavailable(
                    TeachBackUnavailableReason.NO_ELIGIBLE_ANCHOR,
                    TeachBackDeliveryResult.UNAVAILABLE_LEARNER_MESSAGE);
        }
        String learnerResponse = closedAttempt.submission().finalDerivative().confirmedCanonical();
        TeachBackAssessmentContext context = new TeachBackAssessmentContext(
                taskPackage.learnerProjection().taskText(),
                anchorView.get().learnerContent(),
                learnerResponse,
                closedAttempt.purpose());
        TeachBackAssessment assessment = assessmentPort.assess(flow.modelProfile(), context);
        artifactStore.recordTeachBackAssessment(closedAttempt.attemptId(), assessment);
        return new TeachBackSubmissionResult.TeachBackAssessed(
                closedAttempt,
                assessment,
                evidenceCandidate(flow, closedAttempt, assessment),
                facts(flow, assessment));
    }

    /**
     * The understanding-dimension Evidence candidate of one closed Teach-back
     * Attempt: a Practice-purpose record with no assistance, since Teach-back
     * never exposes hints, that never lowers Current Mastery and never counts
     * as an Apply Practice pass for readiness. An Inconclusive judgment
     * builds none.
     */
    private AcceptedLearningEvidence evidenceCandidate(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            TeachBackAssessment assessment
    ) {
        return switch (assessment.outcome()) {
            case PASS -> understandingEvidence(flow, closedAttempt, LearningResult.PASS);
            case FAIL -> understandingEvidence(flow, closedAttempt, LearningResult.FAIL);
            case INCONCLUSIVE -> null;
        };
    }

    private FeedbackFacts facts(LearningFlowStore.FlowRecord flow, TeachBackAssessment assessment) {
        boolean satisfied = assessment.outcome() == TeachBackAssessment.TeachBackOutcome.PASS;
        List<String> criterionIds = criterionIds();
        return new FeedbackFacts(
                satisfied ? criterionIds : List.of(),
                satisfied ? List.of() : criterionIds,
                assessment.reasonCodes(),
                0,
                List.of(),
                practicePassEvidenceExists(flow));
    }

    private List<String> criterionIds() {
        return contextTemplate.masteryRubric().criteria().stream()
                .map(criterion -> criterion.id())
                .toList();
    }

    private boolean practicePassEvidenceExists(LearningFlowStore.FlowRecord flow) {
        return flowStore.allEvidence().stream().anyMatch(evidence ->
                evidence.flowId().equals(flow.flowId())
                        && evidence.attemptPurpose() == AttemptPurpose.PRACTICE
                        && evidence.result() == LearningResult.PASS);
    }

    /**
     * The understanding-dimension Evidence of one closed Teach-back Attempt:
     * a Practice-purpose record with no assistance, since Teach-back never
     * exposes hints, that never lowers Current Mastery and never counts as an
     * Apply Practice pass for readiness.
     */
    private AcceptedLearningEvidence understandingEvidence(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            LearningResult result
    ) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(),
                closedAttempt.attemptId(),
                flow.flowId(),
                flow.conceptId(),
                flow.learnerId(),
                result,
                AttemptPurpose.PRACTICE,
                0,
                List.of(),
                clock.instant());
    }
    private sealed interface CloseOutcome
            permits CloseOutcome.Closed, CloseOutcome.Recovered, CloseOutcome.NotSubmittable, CloseOutcome.Ignored {

        record Closed(TaskAttempt attempt) implements CloseOutcome {
        }

        record Recovered(TaskAttempt attempt) implements CloseOutcome {
        }

        record NotSubmittable(SubmissionRejectionReason reason) implements CloseOutcome {
        }

        record Ignored(SubmissionIgnoreReason reason) implements CloseOutcome {
        }
    }
}
