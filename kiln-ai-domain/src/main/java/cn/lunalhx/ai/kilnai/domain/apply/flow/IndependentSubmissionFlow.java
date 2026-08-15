package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.IndependentSubmissionResult;
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
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The Independent-Test submission flow: one formal submission atomically
 * closes the Independent Attempt and runs the isolated Assessment. Only a
 * passing final-expression channel with a non-contradictory rationale accepts
 * exactly one Independent Evidence record, atomically schedules the unique
 * Review 1 due 24 hours later, and projects the updated Concept Progress.
 * Every other outcome—failed, Inconclusive, blocked by a clearly
 * contradictory rationale, duplicate submission, or unclosed attempt—never
 * creates Evidence. The learner sees only a safe continue-or-end message.
 * All state is persisted durably; the flow carries no in-memory state.
 */
public final class IndependentSubmissionFlow {

    public static final String INDEPENDENT_COMPLETE_MESSAGE = "本次独立练习已完成，请继续下一步学习。";
    public static final String SAFE_END_MESSAGE = "本次独立练习已结束，请继续下一步学习。";

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
        };
    }

    private IndependentSubmissionResult assessAndAcceptEvidence(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
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
                    AttemptPurpose.INDEPENDENT_TEST,
                    0,
                    List.of(),
                    clock.instant());
            ReviewTask scheduledReview = reviewScheduler.acceptEvidenceAndScheduleFirstReview(evidence);
            return new IndependentSubmissionResult.EvidenceAccepted(
                    closedAttempt,
                    evidence,
                    scheduledReview,
                    projectProgress(flow.learnerId(), flow.conceptId()),
                    INDEPENDENT_COMPLETE_MESSAGE);
        }
        return new IndependentSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
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
