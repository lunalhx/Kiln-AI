package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor.PreparedDelivery;
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
 * inconsistent with their rationale.
 *
 * <p>An Inconclusive assessment accepts no Evidence and changes no milestone
 * or cadence position: the flow prepares a new verified Fresh Equivalent task
 * and the store durably binds it as the Review's single new OPEN Attempt with
 * its exposure and a continuing learner interaction. When that replacement
 * cannot be prepared, the store commits a neutral continuation interaction and
 * the Review stays Started with no open Attempt, resumable through the same
 * start endpoint. Every other outcome — duplicate submission or unclosed
 * attempt — never creates Evidence and never advances the cadence. The
 * learner sees only safe completion messages that never leak answers,
 * assessment facts, or reason codes. All state is persisted durably; the flow
 * carries no in-memory state.
 */
public final class ReviewSubmissionFlow {

    public static final String REVIEW_COMPLETE_MESSAGE = "本次复习已完成，请继续下一步学习。";
    public static final String SAFE_END_MESSAGE = "本次复习已结束，请继续下一步学习。";
    public static final String RATIONALE_CONTRADICTION_MESSAGE = "本次复习已结束：您的最终答案与给出的理由不一致。";
    public static final String INCONCLUSIVE_REPLACEMENT_MESSAGE =
            "系统未能确定本次作答的结果，已为你准备一道新的等价题目，请继续作答。";
    public static final String INCONCLUSIVE_UNAVAILABLE_MESSAGE =
            "系统未能确定本次作答的结果，暂时也无法准备新的等价题目。请稍后在复习列表中继续。";

    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final AssessmentRunner assessmentRunner;
    private final SubmissionCloser submissionCloser;
    private final ReviewTaskScheduler reviewScheduler;
    private final ApplyProfileExecutor executor;
    private final ReviewTaskStore reviewStore;
    private final ApplyExecutionContext reviewContextTemplate;
    private final Clock clock;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public ReviewSubmissionFlow(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            ReviewTaskScheduler reviewScheduler,
            ApplyProfileExecutor executor,
            ReviewTaskStore reviewStore,
            ApplyExecutionContext reviewContextTemplate,
            Clock clock
    ) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.assessmentRunner = new AssessmentRunner(
                Objects.requireNonNull(assessmentPort, "assessmentPort must not be null"),
                Objects.requireNonNull(verificationPort, "verificationPort must not be null"));
        this.submissionCloser = new SubmissionCloser(artifactStore, clock);
        this.reviewScheduler = Objects.requireNonNull(reviewScheduler, "reviewScheduler must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
        this.reviewContextTemplate = Objects.requireNonNull(reviewContextTemplate, "reviewContextTemplate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ReviewSubmissionResult submitReview(
            LearningFlowStore.FlowRecord flow,
            UUID idempotencyKey,
            String requestHash,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(flow, "flow must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(requestHash, "requestHash must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        SubmissionCloser.CloseResult closed = submissionCloser.close(
                attemptId, AttemptPurpose.REVIEW, rawDerivative, confirmedCanonical, rationale);
        return switch (closed) {
            case SubmissionCloser.CloseResult.Ignored ignored ->
                    new ReviewSubmissionResult.Ignored(ignored.reason());
            case SubmissionCloser.CloseResult.NotSubmittable notSubmittable ->
                    new ReviewSubmissionResult.NotSubmittable(notSubmittable.reason());
            case SubmissionCloser.CloseResult.Closed closedAttempt ->
                    assessAndAdvanceCadence(flow, closedAttempt.attempt(), idempotencyKey, requestHash);
            case SubmissionCloser.CloseResult.Recovered recovered ->
                    recoverOrIgnore(flow, recovered.attempt(), idempotencyKey, requestHash);
        };
    }

    /**
     * An already-closed Attempt carries its saved submission. When that
     * submission already produced its outcome — Evidence was accepted, or the
     * Review no longer points at it as the open attempt — the command is a
     * duplicate and nothing is re-run. Otherwise the process crashed between
     * closing the Attempt and committing the result, and the evaluation of
     * the saved submission is resumed so the retry recovers the original
     * outcome; the exactly-once Evidence guard and the open-attempt claim
     * make the resumed transition idempotent.
     */
    private ReviewSubmissionResult recoverOrIgnore(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        if (reviewOutcomeAlreadyProduced(flow, closedAttempt)) {
            return new ReviewSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return assessAndAdvanceCadence(flow, closedAttempt, idempotencyKey, requestHash);
    }

    private boolean reviewOutcomeAlreadyProduced(LearningFlowStore.FlowRecord flow, TaskAttempt closedAttempt) {
        if (flowStore.evidenceExists(closedAttempt.attemptId())) {
            return true;
        }
        return reviewStore.findStartedReview(flow.learnerId(), flow.conceptId())
                .map(started -> !Objects.equals(started.openAttemptId(), closedAttempt.attemptId()))
                .orElse(true);
    }

    private ReviewSubmissionResult assessAndAdvanceCadence(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        AssessmentOutcome outcome = assessmentRunner.run(closedAttempt, packageOf(closedAttempt));
        AssessmentRunner.recordAssessments(artifactStore, closedAttempt.attemptId(), outcome);
        return switch (outcome) {
            case AssessmentOutcome.Passed passed -> acceptReviewPass(flow, closedAttempt);
            case AssessmentOutcome.Failed failed ->
                    failAndStopCadence(flow, closedAttempt, SAFE_END_MESSAGE);
            case AssessmentOutcome.Blocked blocked -> {
                // ADR-0061: in Review only, the answer-rationale contradiction
                // is a conclusive no-hint FAIL; the learner is told that their
                // final answer contradicts their rationale, with no assessment
                // facts or reason codes exposed. Independent Test keeps its
                // existing Blocked behavior of no evidence.
                yield failAndStopCadence(flow, closedAttempt, RATIONALE_CONTRADICTION_MESSAGE);
            }
            case AssessmentOutcome.Inconclusive inconclusive ->
                    resolveInconclusive(flow, closedAttempt, idempotencyKey, requestHash);
        };
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

    /**
     * Resolves an Inconclusive Review submission: prepares a new verified
     * Fresh Equivalent task and atomically binds it as the single new OPEN
     * Attempt of the same Started Review, or commits the neutral continuation
     * interaction when no replacement could be prepared. Either way no
     * Evidence is accepted and the cadence position is untouched. When the
     * atomic claim fails — the Review was cancelled or resolved elsewhere —
     * the flow falls back to the shared safe-end message, exactly like every
     * other submission outcome; the same close-then-boundary write pattern
     * and its crash window are shared with the Diagnostic, Independent, and
     * Review pass paths.
     */
    private ReviewSubmissionResult resolveInconclusive(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        Optional<ReviewTask> started = reviewStore.findStartedReview(flow.learnerId(), flow.conceptId());
        if (started.isEmpty()) {
            return new ReviewSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
        }
        ReviewTask review = started.get();
        ApplyExecutionContext reviewContext = reviewContextTemplate.withNoveltyExclusions(
                flowStore.noveltyExclusions(flow.flowId()));
        PreparedDelivery prepared = executor.prepareTask(reviewContext);
        TaskPackage replacement = prepared instanceof PreparedDelivery.TaskReady ready
                ? ready.taskPackage()
                : null;
        int interactionVersion = latestInteractionVersion(flow.flowId()) + 1;
        String message = replacement == null
                ? INCONCLUSIVE_UNAVAILABLE_MESSAGE
                : INCONCLUSIVE_REPLACEMENT_MESSAGE;
        Optional<ApplyFlowInteraction> bound = reviewStore.resolveInconclusiveSubmission(
                new ReviewTaskStore.ResolveInconclusiveBind(
                        review.reviewId(),
                        closedAttempt.attemptId(),
                        replacement,
                        interactionVersion,
                        message,
                        idempotencyKey,
                        requestHash));
        if (bound.isEmpty()) {
            return new ReviewSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
        }
        if (replacement == null) {
            return new ReviewSubmissionResult.ReplacementUnavailable(closedAttempt, bound.get());
        }
        return new ReviewSubmissionResult.ReplacementBound(closedAttempt, bound.get());
    }

    private int latestInteractionVersion(UUID flowId) {
        return flowStore.latestInteraction(flowId)
                .map(ApplyFlowInteraction::interactionVersion)
                .orElseThrow(() -> new IllegalStateException("flow not found"));
    }

    private ConceptProgress projectProgress(UUID learnerId, UUID conceptId) {
        return progressProjector.projectFor(flowStore, learnerId, conceptId);
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return artifactStore.findPackage(attempt.taskPackageId()).orElseThrow();
    }
}
