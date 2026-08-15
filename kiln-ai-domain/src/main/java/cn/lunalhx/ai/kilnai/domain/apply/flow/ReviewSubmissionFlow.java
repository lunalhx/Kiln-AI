package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewSubmissionResult;
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
 * (Review 4 schedules nothing).
 *
 * <p>An Inconclusive assessment accepts no Evidence and changes no milestone
 * or cadence position: the flow prepares a new verified Fresh Equivalent task
 * and the store durably binds it as the Review's single new OPEN Attempt with
 * its exposure and a continuing learner interaction. When that replacement
 * cannot be prepared, the store commits a neutral continuation interaction and
 * the Review stays Started with no open Attempt, resumable through the same
 * start endpoint. Only a conclusive failure ends the interaction without a
 * replacement. All state is persisted durably; the flow carries no in-memory
 * state.
 */
public final class ReviewSubmissionFlow {

    public static final String REVIEW_COMPLETE_MESSAGE = "本次复习已完成，请继续下一步学习。";
    public static final String SAFE_END_MESSAGE = "本次复习已结束，请继续下一步学习。";
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
        };
    }

    private ReviewSubmissionResult assessAndAdvanceCadence(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        AssessmentOutcome outcome = assessmentRunner.run(closedAttempt, packageOf(closedAttempt));
        AssessmentRunner.recordAssessments(artifactStore, closedAttempt.attemptId(), outcome);
        if (outcome instanceof AssessmentOutcome.Passed) {
            AcceptedLearningEvidence evidence = new AcceptedLearningEvidence(
                    UUID.randomUUID(),
                    closedAttempt.attemptId(),
                    flow.flowId(),
                    flow.conceptId(),
                    flow.learnerId(),
                    LearningResult.PASS,
                    AttemptPurpose.REVIEW,
                    0,
                    List.of(),
                    clock.instant());
            ReviewTaskStore.ReviewAdvance advance = reviewScheduler.acceptEvidenceAndAdvanceReview(evidence)
                    .orElseThrow(() -> new IllegalStateException(
                            "a qualifying Review pass requires a started Review to complete"));
            return new ReviewSubmissionResult.EvidenceAccepted(
                    closedAttempt,
                    evidence,
                    advance.completedReview(),
                    advance.successor(),
                    projectProgress(flow.learnerId(), flow.conceptId()),
                    REVIEW_COMPLETE_MESSAGE);
        }
        if (outcome instanceof AssessmentOutcome.Inconclusive) {
            return resolveInconclusive(flow, closedAttempt, idempotencyKey, requestHash);
        }
        return new ReviewSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
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
                flowStore.exposedTaskFingerprints(flow.flowId()),
                flowStore.exposedSolutionFingerprints(flow.flowId()));
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
        List<AcceptedLearningEvidence> conceptEvidence = flowStore.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId) && item.conceptId().equals(conceptId))
                .toList();
        return progressProjector.project(learnerId, conceptId, conceptEvidence);
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return artifactStore.findPackage(attempt.taskPackageId()).orElseThrow();
    }
}
