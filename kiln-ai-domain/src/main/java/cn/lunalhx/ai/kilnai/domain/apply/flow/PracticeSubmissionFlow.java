package cn.lunalhx.ai.kilnai.domain.apply.flow;

import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyExecutionContext;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.PracticeSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The Apply Practice submission flow: one formal submission atomically closes
 * the Practice Attempt and runs the isolated Assessment under the Practice
 * final-derivative policy. A conclusive pass builds exactly one assisted
 * Practice PASS Evidence candidate — the only outcome that can make fresh
 * Independent testing legal in the current remediation cycle — a conclusive
 * fail (including a clearly contradictory rationale over a correct final
 * answer, ADR-0067) builds exactly one assisted Practice FAIL Evidence
 * candidate, and an Inconclusive judgment builds no Evidence candidate. The
 * follow-up Teaching Node is never selected here: the Learning StateGraph
 * derives the legal next moves through the Workflow Guard and the Pedagogy
 * Agent, then accepts the Evidence only after the chosen follow-up node's
 * generation, gating, and verification succeed, so a failed generation leaves
 * no Evidence and the same command can be retried. Neither a pass nor a fail
 * ever lowers Current Mastery, and Practice evidence never touches the Review
 * cadence. All state is persisted durably; the flow carries no in-memory
 * state.
 */
public final class PracticeSubmissionFlow {

    public static final String PRACTICE_START_MESSAGE = "本次诊断已结束，请先完成一道练习题。";
    public static final String PRACTICE_REPLACEMENT_MESSAGE = "请继续完成一道新的练习题。";
    public static final String INDEPENDENT_READY_MESSAGE = "接下来是一道新的独立练习题，请独立完成。";

    private final ApplyProfileExecutor executor;
    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final AssessmentRunner assessmentRunner;
    private final SubmissionCloser submissionCloser;
    private final ApplyExecutionContext practiceContextTemplate;
    private final ApplyExecutionContext independentContextTemplate;
    private final Clock clock;

    public PracticeSubmissionFlow(
            ApplyProfileExecutor executor,
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            AssessmentPort assessmentPort,
            ResponseVerificationPort verificationPort,
            ApplyExecutionContext practiceContextTemplate,
            ApplyExecutionContext independentContextTemplate,
            Clock clock
    ) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.assessmentRunner = new AssessmentRunner(
                Objects.requireNonNull(assessmentPort, "assessmentPort must not be null"),
                Objects.requireNonNull(verificationPort, "verificationPort must not be null"));
        this.submissionCloser = new SubmissionCloser(artifactStore, clock);
        this.practiceContextTemplate = Objects.requireNonNull(
                practiceContextTemplate, "practiceContextTemplate must not be null");
        this.independentContextTemplate = Objects.requireNonNull(
                independentContextTemplate, "independentContextTemplate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Delivers a fresh verified Apply Practice task over the frozen Practice
     * Blueprint, excluding every task, example, and solution already exposed
     * in the Flow. Called by the Graph when the guarded decision selects
     * Apply Practice, and reused for every fresh replacement after a
     * conclusive fail or an Inconclusive judgment.
     */
    public ApplyDeliveryResult deliverPractice(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        return deliverAndRecordExposure(flowId, practiceContextTemplate);
    }

    /**
     * Delivers a fresh verified Independent Test over the frozen Independent
     * Blueprint, excluding every task, example, and solution already exposed
     * in the Flow. Called by the Graph only when the guarded decision selects
     * the Independent Test — the sole outcome that makes fresh Independent
     * testing legal in the current remediation cycle.
     */
    public ApplyDeliveryResult deliverIndependent(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        return deliverAndRecordExposure(flowId, independentContextTemplate);
    }

    public PracticeSubmissionResult submitPractice(
            LearningFlowStore.FlowRecord flow,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale
    ) {
        Objects.requireNonNull(flow, "flow must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        SubmissionCloser.CloseResult closed = submissionCloser.close(
                attemptId, AttemptPurpose.PRACTICE, rawDerivative, confirmedCanonical, rationale);
        return switch (closed) {
            case SubmissionCloser.CloseResult.Ignored ignored ->
                    new PracticeSubmissionResult.Ignored(ignored.reason());
            case SubmissionCloser.CloseResult.NotSubmittable notSubmittable ->
                    new PracticeSubmissionResult.NotSubmittable(notSubmittable.reason());
            case SubmissionCloser.CloseResult.Closed closedAttempt ->
                    assessAndReturn(flow, closedAttempt.attempt());
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
     * exactly-once Evidence guard and the closed Attempt make the resumed
     * transition idempotent.
     */
    private PracticeSubmissionResult recoverOrIgnore(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        if (flowStore.evidenceExists(closedAttempt.attemptId())) {
            return new PracticeSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return assessAndReturn(flow, closedAttempt);
    }

    private PracticeSubmissionResult assessAndReturn(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        AssessmentOutcome outcome = assessmentRunner.run(closedAttempt, packageOf(closedAttempt));
        AssessmentRunner.recordAssessments(artifactStore, closedAttempt.attemptId(), outcome);
        return new PracticeSubmissionResult.PracticeAssessed(
                closedAttempt,
                outcome,
                evidenceCandidate(flow, closedAttempt, outcome),
                facts(flow, closedAttempt, outcome));
    }

    /**
     * The assisted Practice Evidence candidate of one closed Attempt: only
     * the hint levels that were actually exposed are recorded, so the audit
     * trail reflects what the learner saw, and the highest exposed level
     * feeds the readiness and eligibility rules. An Inconclusive judgment
     * builds none.
     */
    private AcceptedLearningEvidence evidenceCandidate(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            AssessmentOutcome outcome
    ) {
        return switch (outcome) {
            case AssessmentOutcome.Passed passed -> practiceEvidence(flow, closedAttempt, LearningResult.PASS);
            case AssessmentOutcome.Failed failed -> practiceEvidence(flow, closedAttempt, LearningResult.FAIL);
            case AssessmentOutcome.Blocked blocked -> practiceEvidence(flow, closedAttempt, LearningResult.FAIL);
            case AssessmentOutcome.Inconclusive inconclusive -> null;
        };
    }

    private FeedbackFacts facts(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            AssessmentOutcome outcome
    ) {
        boolean satisfied = outcome instanceof AssessmentOutcome.Passed;
        List<String> criterionIds = criterionIds();
        return new FeedbackFacts(
                satisfied ? criterionIds : List.of(),
                satisfied ? List.of() : criterionIds,
                AssessmentRunner.errorDimensions(outcome),
                closedAttempt.highestHintLevel(),
                closedAttempt.assistanceTraceStrings(),
                satisfied || practicePassEvidenceExists(flow));
    }

    private List<String> criterionIds() {
        return practiceContextTemplate.masteryRubric().criteria().stream()
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
     * The assisted Practice Evidence carries the attempt's actual Assistance
     * Trace: only the hint levels that were actually exposed are recorded, so
     * the audit trail reflects what the learner saw, and the highest exposed
     * level feeds the readiness and eligibility rules.
     */
    private AcceptedLearningEvidence practiceEvidence(
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
                closedAttempt.highestHintLevel(),
                closedAttempt.assistanceTraceStrings(),
                clock.instant());
    }

    private ApplyDeliveryResult deliverAndRecordExposure(
            UUID flowId,
            ApplyExecutionContext contextTemplate
    ) {
        ApplyExecutionContext context = contextTemplate.withNoveltyExclusions(
                flowStore.exposedTaskFingerprints(flowId),
                flowStore.exposedSolutionFingerprints(flowId));
        ApplyDeliveryResult result = executor.deliver(context);
        if (result instanceof ApplyDeliveryResult.Delivered delivered) {
            recordExposure(flowId, delivered.attempt().taskPackageId());
        }
        return result;
    }

    private void recordExposure(UUID flowId, UUID taskPackageId) {
        flowStore.recordTaskExposure(flowId, packageOf(taskPackageId));
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return packageOf(attempt.taskPackageId());
    }

    private TaskPackage packageOf(UUID taskPackageId) {
        return artifactStore.findPackage(taskPackageId).orElseThrow();
    }
}
