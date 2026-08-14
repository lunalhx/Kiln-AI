package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.IndependentSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.EvidenceStorePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskAttemptStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The Independent-Test submission flow: one formal submission atomically
 * closes the Independent Attempt and runs the isolated Assessment. Only a
 * passing final-expression channel with a non-contradictory rationale accepts
 * exactly one Independent Evidence record and projects the updated Concept
 * Progress. Every other outcome—failed, Inconclusive, blocked by a clearly
 * contradictory rationale, duplicate submission, or unclosed attempt—never
 * creates Evidence. The learner sees only a safe continue-or-end message.
 */
public final class IndependentSubmissionFlow {

    public static final String INDEPENDENT_COMPLETE_MESSAGE = "本次独立练习已完成，请继续下一步学习。";
    public static final String SAFE_END_MESSAGE = "本次独立练习已结束，请继续下一步学习。";

    private final TaskAttemptStore attemptStore;
    private final EvidenceStorePort evidenceStore;
    private final AssessmentRunner assessmentRunner;
    private final SubmissionCloser submissionCloser;
    private final Clock clock;
    private final UUID learnerId;
    private final UUID flowId;
    private final UUID conceptId;
    private final ConceptProgressProjector progressProjector = new ConceptProgressProjector();

    public IndependentSubmissionFlow(
            TaskAttemptStore attemptStore,
            EvidenceStorePort evidenceStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            Clock clock,
            UUID learnerId,
            UUID flowId,
            UUID conceptId
    ) {
        this.attemptStore = Objects.requireNonNull(attemptStore, "attemptStore must not be null");
        this.evidenceStore = Objects.requireNonNull(evidenceStore, "evidenceStore must not be null");
        this.assessmentRunner = new AssessmentRunner(
                Objects.requireNonNull(assessmentPort, "assessmentPort must not be null"),
                Objects.requireNonNull(verificationPort, "verificationPort must not be null"));
        this.submissionCloser = new SubmissionCloser(attemptStore, clock);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.learnerId = Objects.requireNonNull(learnerId, "learnerId must not be null");
        this.flowId = Objects.requireNonNull(flowId, "flowId must not be null");
        this.conceptId = Objects.requireNonNull(conceptId, "conceptId must not be null");
    }

    public IndependentSubmissionResult submitIndependent(
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        SubmissionCloser.CloseResult closed = submissionCloser.close(
                attemptId, AttemptPurpose.INDEPENDENT_TEST, rawDerivative, confirmedCanonical, rationale);
        return switch (closed) {
            case SubmissionCloser.CloseResult.Ignored ignored ->
                    new IndependentSubmissionResult.Ignored(ignored.reason());
            case SubmissionCloser.CloseResult.NotSubmittable notSubmittable ->
                    new IndependentSubmissionResult.NotSubmittable(notSubmittable.reason());
            case SubmissionCloser.CloseResult.Closed closedAttempt -> assessAndAcceptEvidence(closedAttempt.attempt());
        };
    }

    private IndependentSubmissionResult assessAndAcceptEvidence(TaskAttempt closedAttempt) {
        AssessmentOutcome outcome = assessmentRunner.run(closedAttempt, packageOf(closedAttempt));
        if (outcome instanceof AssessmentOutcome.Passed) {
            AcceptedLearningEvidence evidence = new AcceptedLearningEvidence(
                    UUID.randomUUID(),
                    closedAttempt.attemptId(),
                    flowId,
                    conceptId,
                    learnerId,
                    LearningResult.PASS,
                    AttemptPurpose.INDEPENDENT_TEST,
                    0,
                    List.of(),
                    clock.instant());
            evidenceStore.accept(evidence);
            return new IndependentSubmissionResult.EvidenceAccepted(
                    closedAttempt,
                    evidence,
                    projectProgress(),
                    INDEPENDENT_COMPLETE_MESSAGE);
        }
        return new IndependentSubmissionResult.NoEvidence(closedAttempt, SAFE_END_MESSAGE);
    }

    private ConceptProgress projectProgress() {
        List<AcceptedLearningEvidence> conceptEvidence = evidenceStore.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId) && item.conceptId().equals(conceptId))
                .sorted(java.util.Comparator.comparing(AcceptedLearningEvidence::acceptedAt))
                .toList();
        return progressProjector.project(learnerId, conceptId, conceptEvidence);
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return attemptStore.findPackage(attempt.taskPackageId()).orElseThrow();
    }
}
