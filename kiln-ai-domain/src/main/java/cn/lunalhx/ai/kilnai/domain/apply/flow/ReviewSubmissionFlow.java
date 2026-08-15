package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The Review submission flow: one formal submission atomically closes the
 * Review Attempt and runs the same isolated Assessment as Independent Test. A
 * conclusive no-hint passing final-expression channel with a non-contradictory
 * rationale accepts exactly one Review PASS evidence record and advances the
 * cadence atomically — the started Review is completed at the actual
 * acceptance time and the successor is scheduled 3, 7, or 21 days later
 * (Review 4 schedules nothing). A conclusive no-hint failure — a failed
 * final-expression channel, or a clearly contradictory rationale over a
 * correct final answer (ADR-0061) — accepts exactly one Review FAIL evidence
 * record, completes the started Review, defensively cancels any other
 * unfinished Review, and stops the cadence with a safe end message; the
 * contradiction failure also tells the learner that their final answer is
 * inconsistent with their rationale. Every other outcome — Inconclusive,
 * duplicate submission, or unclosed attempt — never creates Evidence and
 * never advances the cadence. The learner sees only safe completion messages
 * that never leak answers, assessment facts, or reason codes. All state is
 * persisted durably; the flow carries no in-memory state.
 */
public final class ReviewSubmissionFlow {

    public static final String REVIEW_COMPLETE_MESSAGE = "本次复习已完成，请继续下一步学习。";
    public static final String SAFE_END_MESSAGE = "本次复习已结束，请继续下一步学习。";
    public static final String RATIONALE_CONTRADICTION_MESSAGE = "本次复习已结束：您的最终答案与给出的理由不一致。";

    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final AssessmentRunner assessmentRunner;
    private final SubmissionCloser submissionCloser;
    private final ReviewTaskScheduler reviewScheduler;
    private final Clock clock;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public ReviewSubmissionFlow(
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

    public ReviewSubmissionResult submitReview(
            LearningFlowStore.FlowRecord flow,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(flow, "flow must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        SubmissionCloser.CloseResult closed = submissionCloser.close(
                attemptId, AttemptPurpose.REVIEW, rawDerivative, confirmedCanonical, rationale);
        return switch (closed) {
            case SubmissionCloser.CloseResult.Ignored ignored ->
                    new ReviewSubmissionResult.Ignored(ignored.reason());
            case SubmissionCloser.CloseResult.NotSubmittable notSubmittable ->
                    new ReviewSubmissionResult.NotSubmittable(notSubmittable.reason());
            case SubmissionCloser.CloseResult.Closed closedAttempt ->
                    assessAndAdvanceCadence(flow, closedAttempt.attempt());
        };
    }

    private ReviewSubmissionResult assessAndAdvanceCadence(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        AssessmentOutcome outcome = assessmentRunner.run(closedAttempt, packageOf(closedAttempt));
        AssessmentRunner.recordAssessments(artifactStore, closedAttempt.attemptId(), outcome);
        if (outcome instanceof AssessmentOutcome.Passed) {
            return acceptReviewPass(flow, closedAttempt);
        }
        if (outcome instanceof AssessmentOutcome.Failed) {
            return failAndStopCadence(flow, closedAttempt, SAFE_END_MESSAGE);
        }
        if (outcome instanceof AssessmentOutcome.Blocked) {
            // ADR-0061: in Review only, the answer-rationale contradiction is a
            // conclusive no-hint FAIL; the learner is told that their final
            // answer contradicts their rationale, with no assessment facts or
            // reason codes exposed. Independent Test keeps its existing Blocked
            // behavior of no evidence.
            return failAndStopCadence(flow, closedAttempt, RATIONALE_CONTRADICTION_MESSAGE);
        }
        return new ReviewSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
    }

    private ReviewSubmissionResult acceptReviewPass(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        AcceptedLearningEvidence evidence = reviewEvidence(flow, closedAttempt, LearningResult.PASS);
        Optional<ReviewTaskStore.ReviewAdvance> advance = reviewScheduler.acceptEvidenceAndAdvanceReview(evidence);
        if (advance.isEmpty()) {
            // The STARTED Review no longer exists — for example it was
            // cancelled by a fresh Independent pass — so this submission
            // creates no Evidence and cannot advance a cadence.
            return new ReviewSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
        }
        return new ReviewSubmissionResult.EvidenceAccepted(
                closedAttempt,
                evidence,
                advance.get().completedReview(),
                advance.get().successor(),
                projectProgress(flow.learnerId(), flow.conceptId()),
                REVIEW_COMPLETE_MESSAGE);
    }

    private ReviewSubmissionResult failAndStopCadence(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            String learnerMessage
    ) {
        AcceptedLearningEvidence evidence = reviewEvidence(flow, closedAttempt, LearningResult.FAIL);
        Optional<ReviewTaskStore.ReviewStop> stop = reviewScheduler.acceptEvidenceAndFailReview(evidence);
        if (stop.isEmpty()) {
            // The STARTED Review no longer exists — for example it was
            // cancelled by a fresh Independent pass — so this submission
            // creates no Evidence and cannot stop a cadence.
            return new ReviewSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
        }
        return new ReviewSubmissionResult.FailureEvidenceAccepted(
                closedAttempt,
                evidence,
                stop.get().completedReview(),
                projectProgress(flow.learnerId(), flow.conceptId()),
                learnerMessage);
    }

    private AcceptedLearningEvidence reviewEvidence(
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
                AttemptPurpose.REVIEW,
                0,
                List.of(),
                clock.instant());
    }

    private ConceptProgress projectProgress(UUID learnerId, UUID conceptId) {
        List<AcceptedLearningEvidence> conceptEvidence = flowStore.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId) && item.conceptId().equals(conceptId))
                .toList();
        return progressProjector.project(learnerId, conceptId, conceptEvidence);
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return artifactStore.findPackage(attempt.taskPackageId()).orElseThrow();
    }
}
