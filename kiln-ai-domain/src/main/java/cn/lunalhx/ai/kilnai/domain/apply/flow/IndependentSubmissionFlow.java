package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.IndependentSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorFacts;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The Independent-Test submission flow: one formal submission atomically
 * closes the Independent Attempt and runs the isolated Assessment. Only a
 * passing final-expression channel with a non-contradictory rationale accepts
 * exactly one Independent Evidence record, atomically schedules the unique
 * Review 1 due 24 hours later, and projects the updated Concept Progress. A
 * conclusive no-hint failure builds exactly one Independent-fail Evidence
 * candidate with its sanitized Feedback Facts — the Graph accepts it only
 * after the chosen remediation node's generation succeeds, drops Current
 * Milestone to Learning, and begins remediation. A Blocked judgment (ADR-0061:
 * the final expression alone cannot be trusted without a coherent rationale)
 * and an Inconclusive judgment create no Evidence and no milestone change;
 * the Graph delivers a fresh verified Independent replacement using all
 * applicable novelty exclusions. Every other outcome—duplicate submission or
 * unclosed attempt—never creates Evidence. A submission replayed after the
 * process crashed between closing the Attempt and committing the result
 * resumes the evaluation of the saved submission, so the learner's retry
 * recovers the outcome instead of being told the Attempt was already
 * submitted. The learner sees only safe continue-or-end messages. All state
 * is persisted durably; the flow carries no in-memory state.
 */
public final class IndependentSubmissionFlow {

    public static final String INDEPENDENT_COMPLETE_MESSAGE = "本次独立练习已完成，请继续下一步学习。";
    public static final String SAFE_END_MESSAGE = "本次独立练习已结束，请继续下一步学习。";
    public static final String REPLACEMENT_MESSAGE = "系统未能确定本次作答的结果，已为你准备一道新的等价题目，请继续作答。";

    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final AssessmentRunner assessmentRunner;
    private final SubmissionCloser submissionCloser;
    private final ReviewTaskScheduler reviewScheduler;
    private final Clock clock;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public IndependentSubmissionFlow(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            ReviewTaskScheduler reviewScheduler,
            Clock clock
    ) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.assessmentRunner = new AssessmentRunner(
                Objects.requireNonNull(assessmentPort, "assessmentPort must not be null"),
                Objects.requireNonNull(verificationPort, "verificationPort must not be null"));
        this.submissionCloser = new SubmissionCloser(artifactStore, clock);
        this.reviewScheduler = Objects.requireNonNull(reviewScheduler, "reviewScheduler must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public IndependentSubmissionResult submitIndependent(
            LearningFlowStore.FlowRecord flow,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(flow, "flow must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        SubmissionCloser.CloseResult closed = submissionCloser.close(
                attemptId, AttemptPurpose.INDEPENDENT_TEST, rawDerivative, confirmedCanonical, rationale);
        return switch (closed) {
            case SubmissionCloser.CloseResult.Ignored ignored ->
                    new IndependentSubmissionResult.Ignored(ignored.reason());
            case SubmissionCloser.CloseResult.NotSubmittable notSubmittable ->
                    new IndependentSubmissionResult.NotSubmittable(notSubmittable.reason());
            case SubmissionCloser.CloseResult.Closed closedAttempt ->
                    assessAndAcceptEvidence(flow, closedAttempt.attempt());
            case SubmissionCloser.CloseResult.Recovered recovered ->
                    recoverOrIgnore(flow, recovered.attempt());
        };
    }

    /**
     * An already-closed Attempt carries its saved submission. When that
     * submission already produced Evidence, the command is a duplicate whose
     * outcome exists and nothing is re-run; otherwise the process crashed
     * between closing and committing, and the evaluation of the saved
     * submission is resumed so the retry recovers the original result. The
     * exactly-once Evidence guard makes the resumed transition idempotent.
     */
    private IndependentSubmissionResult recoverOrIgnore(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        if (flowStore.evidenceExists(closedAttempt.attemptId())) {
            return new IndependentSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return assessAndAcceptEvidence(flow, closedAttempt);
    }

    private IndependentSubmissionResult assessAndAcceptEvidence(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        AssessmentOutcome outcome = assessmentRunner.run(flow.modelProfile(), closedAttempt, packageOf(closedAttempt));
        AssessmentRunner.recordAssessments(artifactStore, closedAttempt.attemptId(), outcome);
        return switch (outcome) {
            case AssessmentOutcome.Passed passed -> acceptPass(flow, closedAttempt);
            // A conclusive no-hint Independent failure: exactly one fail
            // Evidence candidate with the sanitized Feedback Facts. The Graph
            // accepts the Evidence only after the chosen remediation node's
            // generation succeeds, drops Current Milestone to Learning, and
            // begins remediation.
            case AssessmentOutcome.Failed failed ->
                    new IndependentSubmissionResult.FailureEvidenceAccepted(
                            closedAttempt,
                            failEvidence(flow, closedAttempt),
                            failureFacts(closedAttempt, outcome));
            // ADR-0061: Independent keeps Blocked as no Evidence and no
            // milestone change, because the final expression alone cannot be
            // trusted without a coherent rationale.
            case AssessmentOutcome.Blocked blocked ->
                    new IndependentSubmissionResult.ReplacementRequired(closedAttempt, REPLACEMENT_MESSAGE);
            case AssessmentOutcome.Inconclusive inconclusive ->
                    new IndependentSubmissionResult.ReplacementRequired(closedAttempt, REPLACEMENT_MESSAGE);
        };
    }

    private IndependentSubmissionResult acceptPass(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        AcceptedLearningEvidence evidence = new AcceptedLearningEvidence(
                UUID.randomUUID(),
                closedAttempt.attemptId(),
                flow.flowId(),
                flow.conceptId(),
                flow.learnerId(),
                LearningResult.PASS,
                AttemptPurpose.INDEPENDENT_TEST,
                0,
                List.of(),
                clock.instant());
        Optional<ReviewTask> scheduledReview = reviewScheduler.acceptEvidenceAndScheduleFirstReview(evidence);
        if (scheduledReview.isEmpty()) {
            // The attempt already has Evidence — the transition is
            // exactly once, so this run must not reschedule or stack.
            return new IndependentSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
        }
        return new IndependentSubmissionResult.EvidenceAccepted(
                closedAttempt,
                evidence,
                scheduledReview.get(),
                projectProgress(flow.learnerId(), flow.conceptId()),
                INDEPENDENT_COMPLETE_MESSAGE);
    }

    /**
     * Exactly one no-hint Independent-fail Evidence candidate: the no-hint
     * qualifier is guaranteed because hints are never legal for an
     * Independent Attempt (ADR-0065), so the closed attempt's trace is always
     * empty and its highest hint level is always zero.
     */
    private AcceptedLearningEvidence failEvidence(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(),
                closedAttempt.attemptId(),
                flow.flowId(),
                flow.conceptId(),
                flow.learnerId(),
                LearningResult.FAIL,
                AttemptPurpose.INDEPENDENT_TEST,
                closedAttempt.highestHintLevel(),
                closedAttempt.assistanceTraceStrings(),
                clock.instant());
    }

    private FeedbackFacts failureFacts(TaskAttempt closedAttempt, AssessmentOutcome outcome) {
        List<String> criterionIds = packageOf(closedAttempt).privateAssessorProjection().rubricMapping().stream()
                .map(PrivateAssessorFacts.RubricMapping::masteryCriterionId)
                .distinct()
                .toList();
        return new FeedbackFacts(
                List.of(),
                criterionIds,
                AssessmentRunner.errorDimensions(outcome),
                closedAttempt.highestHintLevel(),
                closedAttempt.assistanceTraceStrings(),
                false);
    }

    private ConceptProgress projectProgress(UUID learnerId, UUID conceptId) {
        return progressProjector.projectFor(flowStore, learnerId, conceptId);
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return artifactStore.findPackage(attempt.taskPackageId()).orElseThrow();
    }
}
