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

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The Apply Practice submission flow: one formal submission atomically closes
 * the Practice Attempt and runs the isolated Assessment under the Practice
 * final-derivative policy. A conclusive pass accepts exactly one assisted
 * Practice PASS Evidence record and delivers a fresh verified Independent
 * Test — the only outcome that makes fresh Independent testing legal in the
 * current remediation cycle. A conclusive fail — including a clearly
 * contradictory rationale over a correct final answer (ADR-0067) — accepts
 * exactly one assisted Practice FAIL Evidence record and delivers a fresh
 * Practice task. An Inconclusive judgment accepts no Evidence and delivers a
 * fresh verified Practice replacement. The fresh follow-up task is always
 * generated, gated, and verified before its Evidence is accepted, so a failed
 * generation leaves no Evidence and the same command can be retried; the
 * exactly-once Evidence guard and the closed Attempt make every resumed
 * transition idempotent. Neither a pass nor a fail ever lowers Current
 * Mastery, and Practice evidence never touches the Review cadence. The
 * learner sees only safe neutral messages; assessment facts and reason codes
 * stay private. All state is persisted durably; the flow carries no
 * in-memory state.
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
     * in the Flow. Called by the Graph after an accepted Diagnostic failure to
     * open the remediation cycle, and reused internally for every fresh
     * replacement after a conclusive fail or an Inconclusive judgment.
     */
    public ApplyDeliveryResult deliverPractice(UUID flowId) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        return deliverAndRecordExposure(flowId, practiceContextTemplate);
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
                    assessAndResolve(flow, closedAttempt.attempt());
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
        return assessAndResolve(flow, closedAttempt);
    }

    private PracticeSubmissionResult assessAndResolve(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt
    ) {
        AssessmentOutcome outcome = assessmentRunner.run(closedAttempt, packageOf(closedAttempt));
        AssessmentRunner.recordAssessments(artifactStore, closedAttempt.attemptId(), outcome);
        return switch (outcome) {
            case AssessmentOutcome.Passed passed ->
                    deliverIndependent(flow, closedAttempt, practiceEvidence(flow, closedAttempt, LearningResult.PASS));
            case AssessmentOutcome.Failed failed ->
                    deliverReplacement(flow, closedAttempt,
                            practiceEvidence(flow, closedAttempt, LearningResult.FAIL));
            case AssessmentOutcome.Blocked blocked ->
                    // ADR-0067: a clearly contradictory rationale over a
                    // correct final answer is a conclusive failure, exactly as
                    // in Review; evaluative uncertainty is never involved.
                    deliverReplacement(flow, closedAttempt,
                            practiceEvidence(flow, closedAttempt, LearningResult.FAIL));
            case AssessmentOutcome.Inconclusive inconclusive -> deliverReplacement(flow, closedAttempt, null);
        };
    }

    /**
     * The fresh Independent task is generated, gated, and verified before the
     * assisted PASS Evidence is accepted, so a failed generation leaves no
     * Evidence and the same command can be retried. Only this branch can
     * deliver a fresh Independent Test; a conclusive fail or an Inconclusive
     * judgment never may.
     */
    private PracticeSubmissionResult deliverIndependent(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            AcceptedLearningEvidence evidence
    ) {
        ApplyDeliveryResult delivery = deliverAndRecordExposure(flow.flowId(), independentContextTemplate);
        if (delivery instanceof ApplyDeliveryResult.Unavailable unavailable) {
            return new PracticeSubmissionResult.PracticeUnavailable(
                    unavailable.reason(), unavailable.learnerMessage());
        }
        ApplyDeliveryResult.Delivered delivered = (ApplyDeliveryResult.Delivered) delivery;
        if (!flowStore.acceptEvidence(evidence)) {
            return new PracticeSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return new PracticeSubmissionResult.PracticePassed(
                closedAttempt,
                evidence,
                delivered.attempt(),
                delivered.learnerProjection(),
                INDEPENDENT_READY_MESSAGE);
    }

    /**
     * Delivers a fresh verified Practice replacement and, for a conclusive
     * fail, accepts exactly one assisted FAIL Evidence record — the caller
     * passes the already-built Evidence — while an Inconclusive judgment
     * passes null and accepts nothing. The replacement is generated before any
     * Evidence is accepted, so a failed generation leaves no Evidence.
     */
    private PracticeSubmissionResult deliverReplacement(
            LearningFlowStore.FlowRecord flow,
            TaskAttempt closedAttempt,
            AcceptedLearningEvidence evidence
    ) {
        ApplyDeliveryResult delivery = deliverPractice(flow.flowId());
        if (delivery instanceof ApplyDeliveryResult.Unavailable unavailable) {
            return new PracticeSubmissionResult.PracticeUnavailable(
                    unavailable.reason(), unavailable.learnerMessage());
        }
        ApplyDeliveryResult.Delivered delivered = (ApplyDeliveryResult.Delivered) delivery;
        if (evidence != null && !flowStore.acceptEvidence(evidence)) {
            return new PracticeSubmissionResult.Ignored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        if (evidence == null) {
            return new PracticeSubmissionResult.PracticeInconclusive(
                    closedAttempt,
                    delivered.attempt(),
                    delivered.learnerProjection(),
                    PRACTICE_REPLACEMENT_MESSAGE);
        }
        return new PracticeSubmissionResult.PracticeFailed(
                closedAttempt,
                evidence,
                delivered.attempt(),
                delivered.learnerProjection(),
                PRACTICE_REPLACEMENT_MESSAGE);
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
