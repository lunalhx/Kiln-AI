package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.apply.ModelProviderFailure;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.HintFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningCheckpoint;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssessmentOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceConsentView;
import cn.lunalhx.ai.kilnai.domain.apply.model.AssistanceTraceEntry;
import cn.lunalhx.ai.kilnai.domain.apply.model.AttemptConversionOutcome;
import cn.lunalhx.ai.kilnai.domain.apply.model.CommittedEvaluationResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.DiagnosticSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ExplainDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.HintView;
import cn.lunalhx.ai.kilnai.domain.apply.model.IndependentSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.InteractionKind;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractAudit;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import cn.lunalhx.ai.kilnai.domain.apply.model.PendingOperation;
import cn.lunalhx.ai.kilnai.domain.apply.model.PostSubmissionEvaluationUnavailableException;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.PracticeSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ReviewSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.SourceArtifact;
import cn.lunalhx.ai.kilnai.domain.apply.model.SubmissionIgnoreReason;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskAttempt;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskSubmission;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAnchor;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackDeliveryResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackSubmissionResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackTaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachingProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore.ProcessedCommand;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.FeedbackFacts;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPlanner;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPort;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.TeachingAction;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The plain Java Learning StateGraph runner (ADR-0066). It owns one Graph Run
 * per learner command: rehydrates the Learning State from the durable records
 * and the saved checkpoint, runs the deterministic input gate, routes the
 * command through the single legal node (Diagnostic, Explain, Practice,
 * Independent submission, Hint, or Teach-back), and stops at the next Learner
 * Interaction Boundary by committing the learner interaction, its checkpoint,
 * and the processed command atomically.
 *
 * <p>At every guarded decision node — an accepted Diagnostic Not Passed result, Explain
 * completion, a Practice or Teach-back result, an H5 reveal, and readiness —
 * the deterministic Workflow Guard first derives the closed legal next-move
 * set from committed state, and the bounded Pedagogy Agent then selects one
 * move only within that set (one initial plan and at most one same-plan
 * repair; an invalid output is discarded entirely and the spec's deterministic
 * fallback runs; an unavailable fallback node stops at a safe boundary). The
 * Guard bypasses model-based selection when exactly one move is legal. The
 * Agent receives only sanitized Feedback Facts and the legal-action set —
 * never raw answers, expected answers, assessment reasoning, Skill ids, or
 * state access — and never writes Learning State; only this runner and its
 * nodes own the store. A crash between the two committed halves of a
 * submission or hint exposure resumes from the saved Attempt or saved hint
 * request through the reused node capability.
 */
public final class LearningStateGraph {

    public static final String RESUME_PRACTICE_MESSAGE = "请继续完成当前练习题。";

    /**
     * The generic learner-safe message of an initial Start preparation
     * failure (spec: "Initial failure returns a generic 503 and the client
     * reuses the original Idempotency-Key"). The failed Start persists
     * nothing, so the learner message never exposes provider, model, source,
     * or parser details.
     */
    public static final String START_UNAVAILABLE_MESSAGE = "暂时无法开始学习，请稍后重试。";

    /**
     * The neutral learner-safe boundary for a submitted Attempt whose
     * evaluation responsibility could not complete. The saved Attempt and
     * Pending Operation, rather than this message, carry the recovery state.
     */
    public static final String POST_SUBMISSION_EVALUATION_UNAVAILABLE_MESSAGE =
            ModelContractInvalidException.LEARNER_SAFE_MESSAGE;

    /**
     * The teaching intent of every clarification-driven temporary Explain: a
     * pure teaching action answering the learner's substantive question, never
     * an assessment or a new task.
     */
    public static final String CLARIFICATION_INTENT = "clarification_assistance";

    /**
     * The learner-visible consequence of accepting help on an open
     * Independent Test or Review Attempt: the attempt will be converted
     * one-way to Practice before any assistance content is exposed
     * (ADR-0014).
     */
    public static final String CONSENT_WARNING_MESSAGE =
            "请求帮助将不再计入独立成绩：本次尝试将不可逆地转为练习。是否继续？";

    public static final String ASSISTANCE_REFUSED_MESSAGE = "已放弃帮助，本次尝试保持不变，请继续作答。";

    public static final String ASSISTANCE_CONVERTED_MESSAGE =
            "本次尝试已转为练习，请先阅读下面的讲解，之后可继续作答或请求提示。";

    public static final String CLARIFICATION_EXPLAIN_MESSAGE =
            "以下是针对您疑问的讲解，之后请继续完成当前题目。";

    /**
     * The learner-visible refusal of a substantive or uncertain clarification
     * on a Diagnostic or Teach-back task (ticket 06): this stage offers no
     * concept help, so the message adds no teaching content and the open
     * Attempt's purpose and evidence eligibility are never changed.
     */
    public static final String TASK_CLARIFICATION_NOT_OFFERED_MESSAGE =
            "当前任务阶段不提供概念讲解。请按题目中已展示的格式与记号继续作答。";

    /**
     * The learner-visible refusal of a substantive or uncertain clarification
     * on a standalone Explain teaching interaction (ticket 06): the
     * explanation adds no further teaching content and the teaching boundary
     * is left unchanged.
     */
    public static final String TEACHING_CLARIFICATION_NOT_OFFERED_MESSAGE =
            "本次讲解不回答额外概念问题。您可以继续进入下一步，或就已展示内容的格式、记号与界面操作提问。";

    /**
     * The learner-visible message of an explicit leave (ADR-0015): any open
     * Attempt is closed as Abandoned without submission, Assessment, or
     * Evidence, and the Flow stops at a terminal transition boundary.
     */
    public static final String FLOW_LEAVE_MESSAGE = "已离开本次学习，未作答的题目已放弃。";

    private final ArtifactStore artifactStore;
    private final LearningFlowStore flowStore;
    private final ReviewTaskStore reviewStore;
    private final DiagnosticFlow diagnosticFlow;
    private final IndependentSubmissionFlow independentFlow;
    private final PracticeSubmissionFlow practiceFlow;
    private final ReviewSubmissionFlow reviewFlow;
    private final ExplainFlow explainFlow;
    private final HintFlow hintFlow;
    private final TeachBackFlow teachBackFlow;
    private final WorkflowGuard guard;
    private final PedagogyPlanner planner;
    private final ClarificationGate clarificationGate;
    private final Clock clock;

    public LearningStateGraph(
            ArtifactStore artifactStore,
            LearningFlowStore flowStore,
            ReviewTaskStore reviewStore,
            DiagnosticFlow diagnosticFlow,
            IndependentSubmissionFlow independentFlow,
            PracticeSubmissionFlow practiceFlow,
            ReviewSubmissionFlow reviewFlow,
            ExplainFlow explainFlow,
            HintFlow hintFlow,
            TeachBackFlow teachBackFlow,
            PedagogyPort pedagogyPort,
            ClarificationClassifierPort clarificationClassifier,
            Clock clock
    ) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore must not be null");
        this.flowStore = Objects.requireNonNull(flowStore, "flowStore must not be null");
        this.reviewStore = Objects.requireNonNull(reviewStore, "reviewStore must not be null");
        this.diagnosticFlow = Objects.requireNonNull(diagnosticFlow, "diagnosticFlow must not be null");
        this.independentFlow = Objects.requireNonNull(independentFlow, "independentFlow must not be null");
        this.practiceFlow = Objects.requireNonNull(practiceFlow, "practiceFlow must not be null");
        this.reviewFlow = Objects.requireNonNull(reviewFlow, "reviewFlow must not be null");
        this.explainFlow = Objects.requireNonNull(explainFlow, "explainFlow must not be null");
        this.hintFlow = Objects.requireNonNull(hintFlow, "hintFlow must not be null");
        this.teachBackFlow = Objects.requireNonNull(teachBackFlow, "teachBackFlow must not be null");
        this.guard = new WorkflowGuard();
        this.planner = new PedagogyPlanner(Objects.requireNonNull(pedagogyPort, "pedagogyPort must not be null"));
        this.clarificationGate = new ClarificationGate(
                Objects.requireNonNull(clarificationClassifier, "clarificationClassifier must not be null"));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * The Graph Run of a flow start: the Diagnostic node is fully prepared —
     * profile resolution, generation, Output Gate, and Task Verification all
     * complete first — and the whole Start binds atomically in one durable
     * commit: the Flow record, Source Pack, Task Package, open Attempt,
     * exposure, first learner interaction, checkpoint, and processed command
     * (ADR-0063). An initial preparation failure returns the generic 503 with
     * no durable trace, and the client reuses the original Idempotency-Key.
     */
    public LearningFlowResult start(
            UUID flowId,
            UUID learnerId,
            UUID conceptId,
            ModelProfile profile,
            SourceArtifact source,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(source, "source must not be null");
        ApplyProfileExecutor.PreparedDelivery prepared = diagnosticFlow.prepareDiagnostic(profile);
        return switch (prepared) {
            case ApplyProfileExecutor.PreparedDelivery.Unavailable unavailable ->
                    throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, START_UNAVAILABLE_MESSAGE);
            case ApplyProfileExecutor.PreparedDelivery.TaskReady ready -> {
                LearningFlowInteraction interaction = flowStore.bindStart(new LearningFlowStore.StartBind(
                        flowId, learnerId, conceptId, profile, source, ready.taskPackage(),
                        ready.verdict(), idempotencyKey, requestHash));
                yield new LearningFlowResult.Boundary(interaction);
            }
        };
    }

    /**
     * The Graph Run of an answer-submitted command: rehydrate the committed
     * state, reject a stale interaction version or an unknown Attempt, route
     * through the single legal Apply node for the Attempt's purpose, and stop
     * at the next committed Learner Interaction Boundary.
     */
    public LearningFlowResult submitAnswer(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        if (state.latestInteraction().kind() == InteractionKind.UNAVAILABLE) {
            return new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION);
        }
        Optional<SubmissionIgnoreReason> ownership = ignoredAttemptOwnership(state, attemptId);
        if (ownership.isPresent()) {
            return new LearningFlowResult.SubmissionIgnored(ownership.get());
        }
        boolean evaluationRecovery = hasCommittedEvaluationCheckpoint(attemptId);
        AttemptPurpose purpose = artifactStore.findAttempt(attemptId)
                .map(TaskAttempt::purpose)
                .orElseThrow();
        try {
            return switch (purpose) {
                case DIAGNOSTIC -> submitDiagnostic(state, attemptId, rawDerivative, confirmedCanonical, rationale,
                        evaluationRecovery, idempotencyKey, requestHash);
                case INDEPENDENT_TEST -> submitIndependent(state, attemptId, rawDerivative, confirmedCanonical, rationale,
                        evaluationRecovery, idempotencyKey, requestHash);
                case REVIEW -> submitReview(state, attemptId, rawDerivative, confirmedCanonical, rationale,
                        idempotencyKey, requestHash);
                case PRACTICE -> isTeachBackAttempt(attemptId)
                        ? submitTeachBack(state, attemptId, rawDerivative, confirmedCanonical,
                                evaluationRecovery, idempotencyKey, requestHash)
                        : submitPractice(state, attemptId, rawDerivative, confirmedCanonical, rationale,
                                evaluationRecovery, idempotencyKey, requestHash);
                default -> new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
            };
        } catch (PostSubmissionEvaluationUnavailableException unavailable) {
            return commitPostSubmissionEvaluationUnavailable(state, unavailable, idempotencyKey, requestHash);
        }
    }

    /**
     * An Attempt must belong to the current Flow and be the Attempt addressed
     * by the current Interaction. An unknown Attempt is not found; an Attempt
     * already replaced by a later Interaction cannot be routed again.
     */
    private Optional<SubmissionIgnoreReason> ignoredAttemptOwnership(LearningState state, UUID attemptId) {
        if (artifactStore.findAttempt(attemptId).isEmpty()) {
            return Optional.of(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        }
        if (!Objects.equals(state.latestInteraction().attemptId(), attemptId)) {
            return Optional.of(SubmissionIgnoreReason.NOT_LEGAL_FOR_INTERACTION);
        }
        return Optional.empty();
    }

    /**
     * The Teach-back Attempt is Practice-purpose but lives behind a
     * teach-back task package; the graph discriminates by the package type so
     * an Apply Practice submission is never routed into the Teach-back node
     * and vice versa.
     */
    private boolean isTeachBackAttempt(UUID attemptId) {
        return artifactStore.findAttempt(attemptId)
                .map(TaskAttempt::taskPackageId)
                .flatMap(artifactStore::findTeachBackPackage)
                .isPresent();
    }

    private boolean hasCommittedEvaluationCheckpoint(UUID attemptId) {
        return artifactStore.findCommittedEvaluationResult(
                        attemptId, CommittedEvaluationResult.RESPONSE_ASSESSMENT,
                        CommittedEvaluationResult.EVALUATION_VERSION).isPresent()
                || artifactStore.findCommittedEvaluationResult(
                        attemptId, CommittedEvaluationResult.RESPONSE_VERIFICATION,
                        CommittedEvaluationResult.EVALUATION_VERSION).isPresent()
                || artifactStore.findCommittedEvaluationResult(
                        attemptId, CommittedEvaluationResult.RATIONALE_ASSESSMENT,
                        CommittedEvaluationResult.EVALUATION_VERSION).isPresent()
                || artifactStore.findCommittedEvaluationResult(
                        attemptId, CommittedEvaluationResult.RATIONALE_SUFFICIENCY_VERIFICATION,
                        CommittedEvaluationResult.EVALUATION_VERSION).isPresent()
                || artifactStore.findCommittedEvaluationResult(
                        attemptId, CommittedEvaluationResult.TEACH_BACK_ASSESSMENT,
                        CommittedEvaluationResult.EVALUATION_VERSION).isPresent();
    }

    /**
     * The Graph Run of a continue-requested command: rehydrate the committed
     * state, reject a stale interaction version, and only a teaching boundary
     * may be continued. When a temporary Explain was shown inside an open
     * Apply Practice Attempt, the Guard derives the single legal move back to
     * the same Practice interaction and the model is bypassed; otherwise the
     * legal moves are derived from committed state and the Pedagogy Agent
     * selects the next teaching node. Continue on any other boundary is
     * ignored without state change.
     */
    public LearningFlowResult continueRequested(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        if (state.latestInteraction().teachingProjection() == null) {
            return new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.CONTINUE_NOT_LEGAL);
        }
        Decision decision = decide(state, WorkflowGuard.DecisionContext.EXPLAIN_COMPLETED, continueFacts(state));
        return executeMove(state, decision, null, idempotencyKey, requestHash);
    }

    /**
     * The Graph Run of a hint-requested command: the Hint node generates and
     * gates the private ladder on the first request, exposes only the
     * requested legal level, and the run stops at the next committed Learner
     * Interaction Boundary. H1-H4 keep the Practice Attempt open for a later
     * formal submission; an H5 reveal atomically closes the Attempt as
     * Solution Revealed without Assessment or Evidence, records it as the
     * eligible Teach-back anchor, and the guarded decision routes the next
     * move (Teach-back and fresh Apply Practice are both legal; the
     * deterministic fallback chooses Teach-back). A failed ladder exposes
     * nothing and keeps the open Attempt at a safe message boundary. Hints
     * are never legal for Diagnostic, Independent, Review, or Teach-back
     * Attempts.
     */
    public LearningFlowResult requestHint(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            boolean answerRequested,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        Optional<SubmissionIgnoreReason> ownership = ignoredAttemptOwnership(state, attemptId);
        if (ownership.isPresent()) {
            return new LearningFlowResult.HintIgnored(ownership.get());
        }
        TaskAttempt attempt = artifactStore.findAttempt(attemptId).orElseThrow();
        HintResult result = hintFlow.requestHint(state.flow().modelProfile(), attempt, answerRequested, idempotencyKey);
        return switch (result) {
            case HintResult.Revealed revealed -> revealBoundary(state, revealed, idempotencyKey, requestHash);
            case HintResult.Unavailable unavailable -> boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, attemptId, AttemptPurpose.PRACTICE,
                    projectionOf(attemptId), unavailable.learnerMessage(), null,
                    InteractionKind.TASK, idempotencyKey, requestHash);
            case HintResult.Ignored ignored -> new LearningFlowResult.HintIgnored(ignored.reason());
        };
    }

    /**
     * The Graph Run of a clarification-asked command: the Clarification Gate
     * classifies the free-form message and routes by the current Interaction.
     * On a standalone Explain teaching interaction the command addresses the
     * current Interaction Boundary, never an Attempt (spec): a procedural
     * request is answered with a deterministic restatement of the displayed
     * teaching conditions and leaves the teaching boundary unchanged; a
     * substantive or uncertain request adds no teaching content and keeps the
     * same teaching boundary. On a Diagnostic or Teach-back task only
     * procedural clarification is allowed — it restates the Task Package's own
     * format contract and records the procedural assistance; a substantive or
     * uncertain request adds no teaching content and never changes the Attempt
     * purpose or its evidence eligibility (ticket 06). On an open Apply
     * Practice Attempt a substantive or uncertain request records the
     * assistance and delivers a temporary Explain teaching boundary; on an
     * open Independent Test or Review Attempt it first projects an
     * assistance-consent request with no conversion, recording, or teaching
     * content. Practice, Independent, and Review keep their existing
     * clarification and consent rules.
     */
    public LearningFlowResult clarificationAsked(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            String message,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(message, "message must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        // A standalone Explain (or any teaching interaction) addresses the
        // current Interaction Boundary without an Attempt ID: its
        // clarification neither requires nor uses one — any supplied Attempt
        // ID is ignored because the current interaction is authoritative.
        if (state.latestInteraction().kind() == InteractionKind.TEACHING) {
            return explainClarification(state, message, idempotencyKey, requestHash);
        }
        if (attemptId == null) {
            return new LearningFlowResult.ClarificationIgnored(SubmissionIgnoreReason.ATTEMPT_NOT_FOUND);
        }
        Optional<SubmissionIgnoreReason> ownership = ignoredAttemptOwnership(state, attemptId);
        if (ownership.isPresent()) {
            return new LearningFlowResult.ClarificationIgnored(ownership.get());
        }
        TaskAttempt attempt = artifactStore.findAttempt(attemptId).orElseThrow();
        if (!attempt.isOpen()) {
            return new LearningFlowResult.ClarificationIgnored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        ClarificationClassification classification = classifyClarification(state, attempt, null, message);
        // Diagnostic and Teach-back accept procedural clarification only: a
        // substantive or uncertain request adds no teaching content and changes
        // neither the Attempt purpose nor its evidence eligibility.
        if (attempt.purpose() == AttemptPurpose.DIAGNOSTIC || isTeachBackAttempt(attemptId)) {
            return switch (classification) {
                case PROCEDURAL -> answerProcedurally(state, attempt, idempotencyKey, requestHash);
                case SUBSTANTIVE, UNCERTAIN ->
                        taskClarificationRefused(state, attempt, idempotencyKey, requestHash);
            };
        }
        return switch (classification) {
            case PROCEDURAL -> answerProcedurally(state, attempt, idempotencyKey, requestHash);
            case SUBSTANTIVE, UNCERTAIN -> switch (attempt.purpose()) {
                case PRACTICE -> deliverClarificationExplain(state, attempt, idempotencyKey, requestHash);
                case INDEPENDENT_TEST, REVIEW ->
                        consentBoundary(state, attempt, idempotencyKey, requestHash);
                default -> new LearningFlowResult.ClarificationIgnored(
                        SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
            };
        };
    }

    /**
     * One classification of a clarification message against the learner-visible
     * displayed content (a Task Package's task text or an Explain teaching
     * interaction's content). A Model Contract Invalid falls back to
     * {@code UNCERTAIN} and is recorded as a bounded clarification audit with
     * only the available identity — never a raw invalid payload.
     */
    private ClarificationClassification classifyClarification(
            LearningState state,
            TaskAttempt attempt,
            TeachingProjection teaching,
            String message
    ) {
        String displayed = attempt != null ? taskTextOf(attempt) : teachingTextOf(teaching);
        UUID auditAttemptId = attempt == null ? null : attempt.attemptId();
        UUID auditTaskPackageId = attempt == null ? null : attempt.taskPackageId();
        try {
            return clarificationGate.classify(state.flow().modelProfile(), message, displayed);
        } catch (ModelContractInvalidException exception) {
            artifactStore.recordModelContractAudit(new ModelContractAudit(
                    state.flow().flowId(), auditAttemptId, auditTaskPackageId,
                    ModelContractAudit.CLARIFICATION, exception.violationCodes(), 0,
                    UUID.randomUUID().toString(), ModelContractAudit.PROVIDER_CATEGORY));
            return ClarificationClassification.UNCERTAIN;
        }
    }

    /**
     * The standalone Explain clarification path: the command addresses the
     * current teaching Interaction Boundary and carries no Attempt ID. A
     * procedural request is answered directly with a deterministic restatement
     * of the displayed teaching conditions; a substantive or uncertain request
     * is refused with no added teaching content. Either way the same teaching
     * boundary is committed again, so the auditable record is the committed
     * interaction and processed command (ticket 06).
     */
    private LearningFlowResult explainClarification(
            LearningState state,
            String message,
            UUID idempotencyKey,
            String requestHash
    ) {
        TeachingProjection teaching = state.latestInteraction().teachingProjection();
        ClarificationClassification classification = classifyClarification(state, null, teaching, message);
        return switch (classification) {
            case PROCEDURAL -> explainProceduralAnswer(state, teaching, idempotencyKey, requestHash);
            case SUBSTANTIVE, UNCERTAIN ->
                    teachingClarificationRefused(state, teaching, idempotencyKey, requestHash);
        };
    }

    /**
     * The procedural Explain clarification: the gate restates the displayed
     * teaching conditions and the same teaching boundary is committed again.
     * No Teaching Node Profile is loaded and no new Explain is generated.
     */
    private LearningFlowResult explainProceduralAnswer(
            LearningState state,
            TeachingProjection teaching,
            UUID idempotencyKey,
            String requestHash
    ) {
        String answer = clarificationGate.proceduralTeachingAnswer(teaching);
        return teachingBoundary(state, teaching, answer, idempotencyKey, requestHash);
    }

    /**
     * The substantive or uncertain Explain clarification: no teaching content
     * is added and the same teaching boundary is committed again with the
     * refusal message.
     */
    private LearningFlowResult teachingClarificationRefused(
            LearningState state,
            TeachingProjection teaching,
            UUID idempotencyKey,
            String requestHash
    ) {
        return teachingBoundary(state, teaching, TEACHING_CLARIFICATION_NOT_OFFERED_MESSAGE,
                idempotencyKey, requestHash);
    }

    /**
     * The substantive or uncertain clarification refusal on a Diagnostic or
     * Teach-back task: no teaching content is added, no assistance is recorded,
     * and the open Attempt's purpose and evidence eligibility are unchanged.
     * The committed same-task boundary and processed command are the auditable
     * record.
     */
    private LearningFlowResult taskClarificationRefused(
            LearningState state,
            TaskAttempt attempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        return taskBoundary(state, attempt, TASK_CLARIFICATION_NOT_OFFERED_MESSAGE,
                idempotencyKey, requestHash);
    }

    private String teachingTextOf(TeachingProjection teaching) {
        return teaching.principleSummary() + " " + teaching.workedExample().problem()
                + " 结果：" + teaching.workedExample().finalResult();
    }

    /**
     * The Graph Run of an assistance-decided command over an open Independent
     * Test or Review Attempt. A refusal preserves the attempt completely
     * unchanged and returns to the same task boundary. An acceptance
     * generates the temporary Explain first, then atomically converts the
     * Attempt one-way to Practice (recording the substantive clarification
     * and the temporary Explain), cancels the STARTED Review Task when the
     * Attempt was a Review — with no Review Evidence and no milestone change
     * (ADR-0062) — and exposes the teaching boundary. A failed generation
     * converts nothing and returns to the unchanged task; the learner can
     * retry or refuse. A retry of an acceptance whose conversion already
     * committed (the process crashed between the conversion and its boundary)
     * resumes the same teaching boundary through the Already-Practice path:
     * the trace is never appended twice and the command never 409s a
     * committed half.
     */
    public LearningFlowResult assistanceDecided(
            UUID flowId,
            int interactionVersion,
            UUID attemptId,
            boolean accept,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(attemptId, "attemptId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        Optional<SubmissionIgnoreReason> ownership = ignoredAttemptOwnership(state, attemptId);
        if (ownership.isPresent()) {
            return new LearningFlowResult.AssistanceIgnored(ownership.get());
        }
        TaskAttempt attempt = artifactStore.findAttempt(attemptId).orElseThrow();
        if (!attempt.isOpen()) {
            return new LearningFlowResult.AssistanceIgnored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        if (isTeachBackAttempt(attemptId) || attempt.purpose() == AttemptPurpose.DIAGNOSTIC) {
            return new LearningFlowResult.AssistanceIgnored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
        }
        if (attempt.purpose() == AttemptPurpose.PRACTICE) {
            // An open Practice attempt is either the retried half of a
            // committed conversion or a wrong-purpose client request; an
            // accepted retry resumes the assistance exposure, a refusal can
            // never undo a committed one-way conversion.
            return accept
                    ? acceptAssistance(state, attempt, idempotencyKey, requestHash)
                    : new LearningFlowResult.AssistanceIgnored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
        }
        if (!accept) {
            return taskBoundary(state, attempt, ASSISTANCE_REFUSED_MESSAGE, idempotencyKey, requestHash);
        }
        return acceptAssistance(state, attempt, idempotencyKey, requestHash);
    }

    /**
     * The Graph Run of a flow-control-requested command: the learner
     * explicitly leaves the Flow (ADR-0015). Any open Attempt is closed as
     * Abandoned first — no submission, Assessment, Evidence, or Mastery
     * Milestone change — while a Started Review remains Started until the
     * learner explicitly uses the independent Review cancellation resource
     * (ADR-0073). The run then stops at a terminal transition boundary
     * carrying only the leave message; a network disconnect or ordinary delay
     * is never an explicit Flow Control Requested event and leaves the attempt
     * open.
     */
    public LearningFlowResult flowControlRequested(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        UUID attemptId = state.latestInteraction().attemptId();
        if (attemptId != null) {
            artifactStore.findAttempt(attemptId)
                    .filter(TaskAttempt::isOpen)
                    .ifPresent(attempt -> {
                        artifactStore.abandonAttempt(attempt.attemptId());
                    });
        }
        return boundary(
                state, state.latestInteraction().stage(), null, null, null,
                FLOW_LEAVE_MESSAGE, null, InteractionKind.TRANSITION, idempotencyKey, requestHash);
    }

    /**
     * The Graph Run of a retry-requested command: legal only on an
     * {@code unavailable} Interaction whose Retry Chain has not been
     * exhausted. It rehydrates the saved Pending Operation and resumes that
     * operation from durable state with no client answer payload (ADR-0069).
     * A failed retry increments the chain and commits a new unavailable
     * version; a successful next interaction clears the pending operation.
     */
    public LearningFlowResult retryRequested(
            UUID flowId,
            int interactionVersion,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        LearningState state = LearningState.rehydrate(flowStore, flowId);
        if (state.latestInteraction().interactionVersion() != interactionVersion) {
            throw new ApplicationException(ErrorCode.CONFLICT, "stale interactionVersion");
        }
        if (state.latestInteraction().kind() != InteractionKind.UNAVAILABLE) {
            return new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.RETRY_NOT_LEGAL);
        }
        PendingOperation pending = flowStore.pendingOperation(flowId).orElse(null);
        if (pending == null || !pending.retryAdvertised()) {
            return new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.RETRY_NOT_LEGAL);
        }
        try {
            return resumePending(state, pending, idempotencyKey, requestHash);
        } catch (PostSubmissionEvaluationUnavailableException unavailable) {
            return commitPostSubmissionEvaluationUnavailable(state, unavailable, idempotencyKey, requestHash);
        }
    }

    /**
     * The accepted assistance conversion: the temporary Explain teaching
     * content is generated, gated, and persisted before any durable mutation,
     * and only a delivered artifact converts the Attempt and cancels the
     * Review Task. An unavailable generation keeps the independent attempt
     * untouched at a safe retry boundary. An Already-Practice conversion — a
     * retried acceptance whose conversion already committed — resumes the
     * same teaching boundary without appending the trace again.
     */
    private LearningFlowResult acceptAssistance(
            LearningState state,
            TaskAttempt attempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        ExplainDeliveryResult delivery = explainFlow.deliverExplain(
                state.flow().flowId(), state.flow().modelProfile(), CLARIFICATION_INTENT, continueFacts(state));
        if (delivery instanceof ExplainDeliveryResult.Unavailable unavailable) {
            return taskBoundary(state, attempt, unavailable.learnerMessage(), idempotencyKey, requestHash);
        }
        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered) delivery;
        AttemptConversionOutcome conversion = artifactStore.convertToPractice(attempt.attemptId(), recordedClarification());
        if (conversion instanceof AttemptConversionOutcome.Ignored ignored) {
            return new LearningFlowResult.AssistanceIgnored(ignored.reason());
        }
        if (attempt.purpose() == AttemptPurpose.REVIEW) {
            reviewStore.cancelStartedReview(
                    state.flow().learnerId(), state.flow().conceptId(), clock.instant());
        }
        flowStore.recordAnchor(state.flow().flowId(),
                new TeachBackAnchor(
                        TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                        delivered.artifact().artifactId(),
                        clock.instant()));
        return teachingBoundary(
                state, delivered.artifact().learnerProjection(), ASSISTANCE_CONVERTED_MESSAGE,
                idempotencyKey, requestHash);
    }

    /**
     * The substantive-clarification path of an open Apply Practice Attempt:
     * the temporary Explain is generated and persisted first, the recorded
     * assistance (the substantive clarification and the temporary Explain)
     * is appended to the open Attempt's Trace, and the teaching boundary is
     * committed. The Workflow Guard's single legal next move — resuming the
     * SAME open Practice interaction — is then reached through the existing
     * Continue command. A failed generation appends nothing and keeps the
     * open Attempt at a safe retry boundary.
     */
    private LearningFlowResult deliverClarificationExplain(
            LearningState state,
            TaskAttempt attempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        ExplainDeliveryResult delivery = explainFlow.deliverExplain(
                state.flow().flowId(), state.flow().modelProfile(), CLARIFICATION_INTENT, continueFacts(state));
        if (delivery instanceof ExplainDeliveryResult.Unavailable unavailable) {
            return boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, attempt.attemptId(), attempt.purpose(),
                    projectionOf(attempt), unavailable.learnerMessage(), null,
                    InteractionKind.TASK, idempotencyKey, requestHash);
        }
        ExplainDeliveryResult.Delivered delivered = (ExplainDeliveryResult.Delivered) delivery;
        Optional<TaskAttempt> extended = artifactStore.appendAssistance(attempt.attemptId(), recordedClarification());
        if (extended.isEmpty()) {
            return new LearningFlowResult.ClarificationIgnored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        flowStore.recordAnchor(state.flow().flowId(),
                new TeachBackAnchor(
                        TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                        delivered.artifact().artifactId(),
                        clock.instant()));
        return teachingBoundary(
                state, delivered.artifact().learnerProjection(), CLARIFICATION_EXPLAIN_MESSAGE,
                idempotencyKey, requestHash);
    }

    /**
     * The assistance entries recorded whenever substantive clarification
     * content and its temporary Explain are shown inside an open Attempt:
     * one entry per actually delivered assistance, so the later Practice
     * assessment honestly carries both.
     */
    private List<AssistanceTraceEntry> recordedClarification() {
        return List.of(
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.SUBSTANTIVE_CLARIFICATION,
                        clock.instant()),
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.TEMPORARY_EXPLAIN,
                        clock.instant()));
    }

    /**
     * The procedural-clarification path: the gate answers directly with a
     * deterministic restatement of the Task Package's own exposed format
     * contract, the answer is recorded as procedural assistance on the open
     * Attempt, and the run stops at the same task boundary. No Teaching Node
     * Profile is loaded and the Attempt's purpose is untouched.
     */
    private LearningFlowResult answerProcedurally(
            LearningState state,
            TaskAttempt attempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        String answer = clarificationGate.proceduralAnswer(projectionOf(attempt));
        Optional<TaskAttempt> extended = artifactStore.appendAssistance(attempt.attemptId(), List.of(
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.PROCEDURAL_CLARIFICATION,
                        clock.instant())));
        if (extended.isEmpty()) {
            return new LearningFlowResult.ClarificationIgnored(SubmissionIgnoreReason.ALREADY_SUBMITTED);
        }
        return taskBoundary(state, attempt, answer, idempotencyKey, requestHash);
    }

    private String taskTextOf(TaskAttempt attempt) {
        return projectionOf(attempt).taskText();
    }

    private LearningFlowResult taskBoundary(
            LearningState state,
            TaskAttempt attempt,
            String learnerMessage,
            UUID idempotencyKey,
            String requestHash
    ) {
        return boundary(
                state, state.latestInteraction().stage(), attempt.attemptId(), attempt.purpose(),
                projectionOf(attempt), learnerMessage, null, InteractionKind.TASK, idempotencyKey, requestHash);
    }

    /**
     * The assistance-consent Learner Interaction Boundary: the open attempt
     * stays untouched and the learner-visible consent projection states the
     * one-way conversion consequence. No teaching content, trace entry, or
     * conversion ever precedes this boundary (ADR-0014).
     */
    private LearningFlowResult consentBoundary(
            LearningState state,
            TaskAttempt attempt,
            UUID idempotencyKey,
            String requestHash
    ) {
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                InteractionKind.ASSISTANCE_CONSENT, state.flow().flowId(),
                state.latestInteraction().interactionVersion() + 1,
                FlowStatus.AWAITING_LEARNER_INPUT, state.latestInteraction().stage(),
                attempt.attemptId(), attempt.purpose(), null, null, null, null,
                new AssistanceConsentView(CONSENT_WARNING_MESSAGE, attempt.attemptId(), attempt.purpose()));
        return commitBoundary(interaction, null, idempotencyKey, requestHash);
    }

    private LearningFlowResult revealBoundary(
            LearningState state,
            HintResult.Revealed revealed,
            UUID idempotencyKey,
            String requestHash
    ) {
        TaskAttempt attempt = revealed.attempt();
        // The generated ladder's content fingerprint enters the Flow's
        // novelty ledger once it exists; the H5 reveal additionally records
        // the revealed-solution fingerprint, so later generation never
        // reuses the exposed hint content or the revealed answer.
        artifactStore.findLadder(attempt.attemptId()).ifPresent(ladder -> {
            flowStore.recordHintLadderExposure(state.flow().flowId(), ladder.fingerprint());
            if (!attempt.isOpen()) {
                flowStore.recordRevealedSolutionExposure(state.flow().flowId(), ladder.revealFingerprint());
            }
        });
        if (attempt.isOpen()) {
            return boundary(
                    state, LearningStage.LEARNING_AND_PRACTICE, attempt.attemptId(), attempt.purpose(),
                    projectionOf(attempt), null, revealed.hint(), InteractionKind.TASK,
                    idempotencyKey, requestHash);
        }
        // The H5 reveal closes the attempt as Solution Revealed and becomes
        // the most recently exposed eligible Teach-back anchor; the graph
        // records it durably (idempotent per anchor id) before the guarded
        // decision routes the next move.
        flowStore.recordAnchor(state.flow().flowId(),
                new TeachBackAnchor(
                        TeachBackAnchor.TeachBackAnchorKind.H5_SOLUTION_REVEAL,
                        attempt.attemptId(),
                        clock.instant()));
        Decision decision = decide(state, WorkflowGuard.DecisionContext.H5_REVEALED, revealFacts(state, attempt));
        return switch (decision.action()) {
            case TeachingAction.TEACH_BACK ->
                    deliverTeachBackMove(state, decision, null, revealed.hint(), idempotencyKey, requestHash);
            case TeachingAction.APPLY_PRACTICE ->
                    deliverApplyPracticeMove(state, decision, null, revealed.hint(), idempotencyKey, requestHash);
            case TeachingAction.INDEPENDENT_TEST ->
                    deliverIndependentMove(state, decision, null, revealed.hint(), idempotencyKey, requestHash);
            default -> throw new IllegalStateException(
                    "the H5 reveal guard only offers Teach-back, Apply Practice, or Independent: " + decision.action());
        };
    }

    private LearnerProjection projectionOf(UUID attemptId) {
        return projectionOf(artifactStore.findAttempt(attemptId).orElseThrow());
    }

    private LearnerProjection projectionOf(TaskAttempt attempt) {
        return artifactStore.findTeachBackPackage(attempt.taskPackageId())
                .map(TeachBackTaskPackage::learnerProjection)
                .orElseGet(() -> packageOf(attempt).learnerProjection());
    }

    private TaskPackage packageOf(TaskAttempt attempt) {
        return artifactStore.findPackage(attempt.taskPackageId()).orElseThrow();
    }

    private LearningFlowResult submitDiagnostic(
            LearningState state,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            boolean evaluationRecovery,
            UUID idempotencyKey,
            String requestHash
    ) {
        DiagnosticSubmissionResult result = diagnosticFlow.submitDiagnostic(
                state.flow().flowId(), state.flow().modelProfile(),
                attemptId, rawDerivative, confirmedCanonical, rationale);
        return switch (result) {
            // The Neutral Transition message is learner-visible content
            // (CONTEXT.md) and is projected on the boundary; the legacy Apply
            // seam dropped it at the interaction level. It states only the
            // next interaction and carries no feedback.
            case DiagnosticSubmissionResult.Passed passed -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, passed.independentAttempt().attemptId(),
                    passed.independentAttempt().purpose(), passed.independentLearnerProjection(),
                    passed.neutralTransitionMessage(), null, InteractionKind.TASK, idempotencyKey, requestHash);
            // A failed submitted Diagnostic stays closed and is never
            // retroactively converted. The Workflow Guard derives the legal
            // remediation actions from committed state and the Pedagogy Agent
            // selects the next teaching node from that closed set.
            case DiagnosticSubmissionResult.Failed failed ->
                    executeMove(state, chooseDecision(state, WorkflowGuard.DecisionContext.DIAGNOSTIC_NOT_PASSED,
                                    failed.facts(), evaluationRecovery),
                            null, idempotencyKey, requestHash);
            case DiagnosticSubmissionResult.Unconfirmed unconfirmed ->
                    executeMove(state, chooseDecision(state, WorkflowGuard.DecisionContext.DIAGNOSTIC_NOT_PASSED,
                                    unconfirmed.facts(), evaluationRecovery),
                            null, idempotencyKey, requestHash);
            case DiagnosticSubmissionResult.IndependentUnavailable unavailable -> commitUnavailable(
                    state, LearningStage.DIAGNOSTIC, unavailable.learnerMessage(), null,
                    new PendingOperation(PendingOperation.Kind.DELIVER_INDEPENDENT, null, null, null,
                            unavailable.learnerMessage(), null, null, null,
                            null, null, null, 0),
                    idempotencyKey, requestHash);
            case DiagnosticSubmissionResult.NotSubmittable notSubmittable ->
                    new LearningFlowResult.SubmissionRejected(notSubmittable.reason());
            case DiagnosticSubmissionResult.Ignored ignored ->
                    new LearningFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    private LearningFlowResult submitIndependent(
            LearningState state,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            boolean evaluationRecovery,
            UUID idempotencyKey,
            String requestHash
    ) {
        IndependentSubmissionResult result = independentFlow.submitIndependent(
                state.flow(), attemptId, rawDerivative, confirmedCanonical, rationale);
        return switch (result) {
            case IndependentSubmissionResult.EvidenceAccepted accepted -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, null, null, null,
                    accepted.learnerMessage(), null, InteractionKind.TRANSITION, idempotencyKey, requestHash);
            // A conclusive no-hint Independent failure: accept exactly one
            // fail Evidence (only after the chosen remediation node's
            // generation succeeds), drop Current Milestone to Learning, and
            // begin remediation through the Guard — Explain and fresh Apply
            // Practice are both legal and the Pedagogy Agent selects one.
            case IndependentSubmissionResult.FailureEvidenceAccepted failed ->
                    executeMove(state, chooseDecision(state, WorkflowGuard.DecisionContext.INDEPENDENT_FAILED,
                                    failed.facts(), evaluationRecovery),
                            failed.evidence(), idempotencyKey, requestHash);
            // A Blocked or Inconclusive judgment creates no Evidence and no
            // milestone change: deliver a fresh verified Independent
            // replacement using all applicable novelty exclusions.
            case IndependentSubmissionResult.ReplacementRequired replacement ->
                    deliverIndependentReplacement(state, replacement.learnerMessage(),
                            idempotencyKey, requestHash);
            case IndependentSubmissionResult.NoEvidence noEvidence -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, null, null, null,
                    noEvidence.learnerMessage(), null, InteractionKind.TRANSITION, idempotencyKey, requestHash);
            case IndependentSubmissionResult.NotSubmittable notSubmittable ->
                    new LearningFlowResult.SubmissionRejected(notSubmittable.reason());
            case IndependentSubmissionResult.Ignored ignored ->
                    new LearningFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    /**
     * The mandated fresh verified Independent replacement of a Blocked or
     * Inconclusive judgment: generated with all applicable novelty exclusions,
     * or a recoverable unavailable boundary when no task can be prepared. No
     * Evidence and no milestone change on either path.
     */
    private LearningFlowResult deliverIndependentReplacement(
            LearningState state,
            String learnerMessage,
            UUID idempotencyKey,
            String requestHash
    ) {
        ApplyDeliveryResult delivery = practiceFlow.deliverIndependent(state.flow().flowId(), state.flow().modelProfile());
        return switch (delivery) {
            case ApplyDeliveryResult.Delivered delivered -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, delivered.attempt().attemptId(),
                    delivered.attempt().purpose(), delivered.learnerProjection(),
                    learnerMessage, null, InteractionKind.TASK, idempotencyKey, requestHash);
            case ApplyDeliveryResult.Unavailable unavailable -> commitUnavailable(
                    state, LearningStage.LEARNING_AND_PRACTICE, unavailable.learnerMessage(), null,
                    new PendingOperation(PendingOperation.Kind.DELIVER_INDEPENDENT_REPLACEMENT, null, null, null,
                            learnerMessage, null, null, null,
                            null, null, null, 0),
                    idempotencyKey, requestHash);
        };
    }

    /**
     * The Review node of one submission: the flow closes and assesses the
     * Attempt and advances the cadence exactly once. A conclusive pass or
     * fail accepts exactly one Review Evidence record and stops at the safe
     * terminal transition; an Inconclusive judgment creates no Evidence and
     * the store durably binds the verified replacement (or the neutral
     * continuation when none could be prepared). A duplicate or unclosed
     * submission never creates Evidence.
     */
    private LearningFlowResult submitReview(
            LearningState state,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            UUID idempotencyKey,
            String requestHash
    ) {
        ReviewSubmissionResult result = reviewFlow.submitReview(
                state.flow(), idempotencyKey, requestHash, attemptId, rawDerivative, confirmedCanonical, rationale);
        return switch (result) {
            case ReviewSubmissionResult.EvidenceAccepted accepted -> boundary(
                    state, LearningStage.DELAYED_REVIEW, null, null, null,
                    accepted.learnerMessage(), null, InteractionKind.TRANSITION, idempotencyKey, requestHash);
            case ReviewSubmissionResult.FailureEvidenceAccepted failed -> boundary(
                    state, LearningStage.DELAYED_REVIEW, null, null, null,
                    failed.learnerMessage(), null, InteractionKind.TRANSITION, idempotencyKey, requestHash);
            case ReviewSubmissionResult.NoEvidence noEvidence -> boundary(
                    state, LearningStage.DELAYED_REVIEW, null, null, null,
                    noEvidence.learnerMessage(), null, InteractionKind.TRANSITION, idempotencyKey, requestHash);
            // The inconclusive replacement boundary was committed atomically
            // by the Review store itself (task or neutral continuation).
            case ReviewSubmissionResult.ReplacementBound bound ->
                    new LearningFlowResult.Boundary(bound.interaction());
            case ReviewSubmissionResult.ReplacementUnavailable unavailable ->
                    new LearningFlowResult.Boundary(unavailable.interaction());
            case ReviewSubmissionResult.NotSubmittable notSubmittable ->
                    new LearningFlowResult.SubmissionRejected(notSubmittable.reason());
            case ReviewSubmissionResult.Ignored ignored ->
                    new LearningFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    /**
     * The Apply Practice node of one submission: the flow closes and assesses
     * the Attempt, the Workflow Guard derives the legal next moves from the
     * outcome and committed state, and the Pedagogy Agent selects one — a
     * conclusive pass can make the fresh Independent Test legal (readiness),
     * a conclusive fail or an Inconclusive judgment can never. The Evidence
     * is accepted only after the chosen follow-up node's generation
     * succeeds, so a failed generation leaves no Evidence and a retry
     * recovers the original outcome.
     */
    private LearningFlowResult submitPractice(
            LearningState state,
            UUID attemptId,
            String rawDerivative,
            String confirmedCanonical,
            String rationale,
            boolean evaluationRecovery,
            UUID idempotencyKey,
            String requestHash
    ) {
        PracticeSubmissionResult result = practiceFlow.submitPractice(
                state.flow(), attemptId, rawDerivative, confirmedCanonical, rationale);
        return switch (result) {
            case PracticeSubmissionResult.PracticeAssessed assessed ->
                    routePracticeDecision(state, assessed, evaluationRecovery, idempotencyKey, requestHash);
            case PracticeSubmissionResult.NotSubmittable notSubmittable ->
                    new LearningFlowResult.SubmissionRejected(notSubmittable.reason());
            case PracticeSubmissionResult.Ignored ignored ->
                    new LearningFlowResult.SubmissionIgnored(ignored.reason());
        };
    }

    private LearningFlowResult routePracticeDecision(
            LearningState state,
            PracticeSubmissionResult.PracticeAssessed assessed,
            boolean evaluationRecovery,
            UUID idempotencyKey,
            String requestHash
    ) {
        WorkflowGuard.DecisionContext context = switch (assessed.outcome()) {
            case AssessmentOutcome.Passed passed -> WorkflowGuard.DecisionContext.PRACTICE_PASSED;
            case AssessmentOutcome.Failed failed -> WorkflowGuard.DecisionContext.PRACTICE_FAILED;
            // ADR-0067: a clearly contradictory rationale over a correct
            // final answer is a conclusive failure in Practice too.
            case AssessmentOutcome.Blocked blocked -> WorkflowGuard.DecisionContext.PRACTICE_FAILED;
            case AssessmentOutcome.Inconclusive inconclusive ->
                    WorkflowGuard.DecisionContext.PRACTICE_INCONCLUSIVE;
            case AssessmentOutcome.Unconfirmed unconfirmed -> throw new IllegalStateException(
                    "an unconfirmed outcome is only valid for Diagnostic");
        };
        Decision decision = chooseDecision(state, context, assessed.facts(), evaluationRecovery);
        return executeMove(state, decision, assessed.evidence(), idempotencyKey, requestHash);
    }

    /**
     * The Teach-back node of one submission: the flow closes and assesses the
     * Attempt, the Workflow Guard derives the legal next moves from the
     * outcome and committed state, and the Pedagogy Agent selects one. A
     * conclusive pass or fail accepts exactly one understanding-dimension
     * Evidence record — never Independent Evidence, because a Teach-back pass
     * does not satisfy Apply Practice readiness — while an Inconclusive
     * judgment creates no Evidence and the single legal move is a fresh
     * Teach-back replacement over the same anchor.
     */
    private LearningFlowResult submitTeachBack(
            LearningState state,
            UUID attemptId,
            String rawText,
            String confirmedText,
            boolean evaluationRecovery,
            UUID idempotencyKey,
            String requestHash
    ) {
        TeachBackSubmissionResult result = teachBackFlow.submitTeachBack(
                state.flow(), attemptId, rawText, confirmedText);
        return switch (result) {
            case TeachBackSubmissionResult.TeachBackAssessed assessed ->
                    routeTeachBackDecision(state, assessed, evaluationRecovery, idempotencyKey, requestHash);
            case TeachBackSubmissionResult.Unavailable unavailable -> commitUnavailable(
                    state, LearningStage.LEARNING_AND_PRACTICE, unavailable.learnerMessage(), null,
                    executeMoveSeed(new Decision(TeachingAction.TEACH_BACK, unavailable.learnerMessage(),
                            fallbackIntent(WorkflowGuard.DecisionContext.TEACH_BACK_INCONCLUSIVE),
                            continueFacts(state), WorkflowGuard.DecisionContext.TEACH_BACK_INCONCLUSIVE),
                            null, null),
                    idempotencyKey, requestHash);
            case TeachBackSubmissionResult.Ignored ignored ->
                    new LearningFlowResult.SubmissionIgnored(ignored.reason());
            case TeachBackSubmissionResult.NotSubmittable notSubmittable ->
                    new LearningFlowResult.SubmissionRejected(notSubmittable.reason());
        };
    }

    private LearningFlowResult routeTeachBackDecision(
            LearningState state,
            TeachBackSubmissionResult.TeachBackAssessed assessed,
            boolean evaluationRecovery,
            UUID idempotencyKey,
            String requestHash
    ) {
        WorkflowGuard.DecisionContext context = assessed.assessment() == null
                ? WorkflowGuard.DecisionContext.TEACH_BACK_INCONCLUSIVE
                : switch (assessed.assessment().outcome()) {
                    case PASS -> WorkflowGuard.DecisionContext.TEACH_BACK_PASSED;
                    case FAIL -> WorkflowGuard.DecisionContext.TEACH_BACK_FAILED;
                    case INCONCLUSIVE -> WorkflowGuard.DecisionContext.TEACH_BACK_INCONCLUSIVE;
                };
        Decision decision = chooseDecision(state, context, assessed.facts(), evaluationRecovery);
        return executeMove(state, decision, assessed.evidence(), idempotencyKey, requestHash);
    }

    /**
     * One guarded pedagogy decision: the deterministic Workflow Guard derives
     * the closed legal move set from committed state (eligible anchor, open
     * Practice Attempt, readiness), and the bounded Pedagogy Agent selects
     * one move only within that set — one initial plan and at most one
     * same-plan repair, after which the entire invalid output is discarded
     * and the spec's deterministic fallback runs with neutral learner
     * feedback. A single legal move bypasses the model entirely. The Agent
     * receives only the sanitized Feedback Facts and the legal set, never
     * answers, assessment reasoning, or Skill ids.
     */
    private Decision decide(
            LearningState state,
            WorkflowGuard.DecisionContext context,
            FeedbackFacts facts
    ) {
        return chooseDecision(state, context, facts, false);
    }

    /**
     * Replays a post-submission route from committed evaluation checkpoints.
     * The guard owns the deterministic fallback; a replay never asks the
     * Pedagogy Agent to make a second decision for a route that has not yet
     * reached its learner boundary.
     */
    private Decision chooseDecision(
            LearningState state,
            WorkflowGuard.DecisionContext context,
            FeedbackFacts facts,
            boolean evaluationRecovery
    ) {
        WorkflowGuard.GuardFacts guardFacts = new WorkflowGuard.GuardFacts(
                flowStore.latestAnchor(state.flow().flowId()).isPresent(),
                openPracticeAttempt(state).isPresent(),
                readinessSatisfied(state.flow().flowId()));
        WorkflowGuard.LegalMoves moves = guard.derive(context, guardFacts);
        if (moves.single() || evaluationRecovery) {
            return new Decision(moves.fallback(),
                    moves.fallback() == TeachingAction.RESUME_PRACTICE
                            ? RESUME_PRACTICE_MESSAGE
                            : neutralMessage(context),
                    fallbackIntent(context),
                    facts, context);
        }
        return switch (planner.plan(state.flow().modelProfile(), facts, moves.legalActions(), moves.fallback())) {
            case PedagogyPlanner.PedagogyDecision.PlanAccepted accepted ->
                    new Decision(accepted.plan().action(), accepted.plan().feedbackSummary(),
                            accepted.plan().intent(), facts, context);
            case PedagogyPlanner.PedagogyDecision.Fallback fb ->
                    new Decision(fb.action(), neutralMessage(context), fallbackIntent(context), facts, context);
        };
    }

    /**
     * Executes the guarded decision by invoking the single legal Teaching
     * Node. The submission Evidence candidate (null for a Diagnostic Not Passed result
     * or an Inconclusive judgment) is accepted only after the chosen node's
     * generation, gating, and verification succeed, so a failed generation
     * leaves no Evidence and the command can be retried.
     */
    private LearningFlowResult executeMove(
            LearningState state,
            Decision decision,
            AcceptedLearningEvidence evidence,
            UUID idempotencyKey,
            String requestHash
    ) {
        return switch (decision.action()) {
            case TeachingAction.EXPLAIN ->
                    deliverExplainMove(state, decision, evidence, idempotencyKey, requestHash);
            case TeachingAction.APPLY_PRACTICE ->
                    deliverApplyPracticeMove(state, decision, evidence, null, idempotencyKey, requestHash);
            case TeachingAction.TEACH_BACK ->
                    deliverTeachBackMove(state, decision, evidence, null, idempotencyKey, requestHash);
            case TeachingAction.INDEPENDENT_TEST ->
                    deliverIndependentMove(state, decision, evidence, null, idempotencyKey, requestHash);
            case TeachingAction.RESUME_PRACTICE -> resumeOpenPractice(state, idempotencyKey, requestHash);
        };
    }

    /**
     * Accepts the submission Evidence candidate after the chosen follow-up
     * node's generation, gating, and verification already succeeded, so a
     * failed generation leaves no Evidence and the command can be retried.
     * Returns the already-submitted ignore outcome when a concurrent or
     * replayed command already accepted Evidence for the same Attempt.
     */
    private Optional<LearningFlowResult> acceptEvidenceOrIgnore(AcceptedLearningEvidence evidence) {
        if (evidence != null && !flowStore.acceptEvidence(evidence)) {
            return Optional.of(new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.ALREADY_SUBMITTED));
        }
        return Optional.empty();
    }

    /**
     * The Explain move: a pure teaching action that delivers one
     * source-grounded teaching interaction with the guarded intent and
     * sanitized Feedback Facts, or a recoverable unavailable boundary when no
     * teaching content can be prepared. It never creates a Task Package,
     * Attempt, Assessment, or Evidence. The delivered worked example becomes
     * the most recently exposed eligible Teach-back anchor.
     */
    private LearningFlowResult deliverExplainMove(
            LearningState state,
            Decision decision,
            AcceptedLearningEvidence evidence,
            UUID idempotencyKey,
            String requestHash
    ) {
        ExplainDeliveryResult delivery = explainFlow.deliverExplain(
                state.flow().flowId(), state.flow().modelProfile(), decision.intent(), decision.facts());
        return switch (delivery) {
            case ExplainDeliveryResult.Delivered delivered -> {
                flowStore.recordAnchor(state.flow().flowId(),
                        new TeachBackAnchor(
                                TeachBackAnchor.TeachBackAnchorKind.EXPLAIN_WORKED_EXAMPLE,
                                delivered.artifact().artifactId(),
                                clock.instant()));
                Optional<LearningFlowResult> rejected = acceptEvidenceOrIgnore(evidence);
                if (rejected.isPresent()) {
                    yield rejected.get();
                }
                yield teachingBoundary(
                        state, delivered.artifact().learnerProjection(), decision.learnerMessage(),
                        idempotencyKey, requestHash);
            }
            case ExplainDeliveryResult.Unavailable unavailable -> commitUnavailable(
                    state, LearningStage.LEARNING_AND_PRACTICE, unavailable.learnerMessage(), null,
                    executeMoveSeed(decision, evidence, null),
                    idempotencyKey, requestHash);
        };
    }

    /**
     * The Apply Practice move: delivers a fresh verified task over the frozen
     * Practice Blueprint, or a recoverable unavailable boundary when no task can
     * be prepared. An optional reveal hint (H5 continuation) is projected on
     * the same boundary.
     */
    private LearningFlowResult deliverApplyPracticeMove(
            LearningState state,
            Decision decision,
            AcceptedLearningEvidence evidence,
            HintView hint,
            UUID idempotencyKey,
            String requestHash
    ) {
        ApplyDeliveryResult delivery = practiceFlow.deliverPractice(state.flow().flowId(), state.flow().modelProfile());
        return switch (delivery) {
            case ApplyDeliveryResult.Delivered delivered -> {
                Optional<LearningFlowResult> rejected = acceptEvidenceOrIgnore(evidence);
                if (rejected.isPresent()) {
                    yield rejected.get();
                }
                yield boundary(
                        state, LearningStage.LEARNING_AND_PRACTICE, delivered.attempt().attemptId(),
                        delivered.attempt().purpose(), delivered.learnerProjection(),
                        decision.learnerMessage(), hint, InteractionKind.TASK, idempotencyKey, requestHash);
            }
            case ApplyDeliveryResult.Unavailable unavailable -> commitUnavailable(
                    state, LearningStage.LEARNING_AND_PRACTICE, unavailable.learnerMessage(), hint,
                    executeMoveSeed(decision, evidence, hint),
                    idempotencyKey, requestHash);
        };
    }

    /**
     * The Teach-back move: delivers one anchored verified short-text task, or
     * a recoverable unavailable boundary when no anchored task can be prepared.
     * An optional reveal hint (H5 continuation) is projected on the same
     * boundary.
     */
    private LearningFlowResult deliverTeachBackMove(
            LearningState state,
            Decision decision,
            AcceptedLearningEvidence evidence,
            HintView hint,
            UUID idempotencyKey,
            String requestHash
    ) {
        TeachBackDeliveryResult delivery = teachBackFlow.deliverTeachBack(state.flow().flowId(), state.flow().modelProfile());
        return switch (delivery) {
            case TeachBackDeliveryResult.Delivered delivered -> {
                Optional<LearningFlowResult> rejected = acceptEvidenceOrIgnore(evidence);
                if (rejected.isPresent()) {
                    yield rejected.get();
                }
                yield boundary(
                        state, LearningStage.LEARNING_AND_PRACTICE, delivered.attempt().attemptId(),
                        delivered.attempt().purpose(), delivered.learnerProjection(),
                        decision.learnerMessage(), hint, InteractionKind.TASK, idempotencyKey, requestHash);
            }
            case TeachBackDeliveryResult.Unavailable unavailable -> commitUnavailable(
                    state, LearningStage.LEARNING_AND_PRACTICE, unavailable.learnerMessage(), hint,
                    executeMoveSeed(decision, evidence, hint),
                    idempotencyKey, requestHash);
        };
    }

    /**
     * The fresh Independent Test move: delivers a fresh verified Independent
     * task — legal only once the qualifying Apply Practice pass prerequisite
     * of the current remediation cycle is satisfied — or a recoverable
     * unavailable boundary when no task can be prepared. An optional reveal
     * hint (H5 continuation) is projected on the same boundary.
     */
    private LearningFlowResult deliverIndependentMove(
            LearningState state,
            Decision decision,
            AcceptedLearningEvidence evidence,
            HintView hint,
            UUID idempotencyKey,
            String requestHash
    ) {
        ApplyDeliveryResult delivery = practiceFlow.deliverIndependent(state.flow().flowId(), state.flow().modelProfile());
        return switch (delivery) {
            case ApplyDeliveryResult.Delivered delivered -> {
                Optional<LearningFlowResult> rejected = acceptEvidenceOrIgnore(evidence);
                if (rejected.isPresent()) {
                    yield rejected.get();
                }
                yield boundary(
                        state, LearningStage.INDEPENDENT_TEST, delivered.attempt().attemptId(),
                        delivered.attempt().purpose(), delivered.learnerProjection(),
                        decision.learnerMessage(), hint, InteractionKind.TASK, idempotencyKey, requestHash);
            }
            case ApplyDeliveryResult.Unavailable unavailable -> commitUnavailable(
                    state, LearningStage.LEARNING_AND_PRACTICE, unavailable.learnerMessage(), hint,
                    executeMoveSeed(decision, evidence, hint),
                    idempotencyKey, requestHash);
        };
    }

    /**
     * The single legal move after a temporary Explain shown inside an open
     * Apply Practice Attempt: re-projects the same open Practice interaction
     * without generating anything. If the open Attempt is no longer present
     * in committed state, the Continue is ignored without state change.
     */
    private LearningFlowResult resumeOpenPractice(
            LearningState state,
            UUID idempotencyKey,
            String requestHash
    ) {
        Optional<TaskAttempt> open = openPracticeAttempt(state);
        if (open.isEmpty()) {
            return new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.CONTINUE_NOT_LEGAL);
        }
        TaskAttempt attempt = open.get();
        return boundary(
                state, LearningStage.LEARNING_AND_PRACTICE, attempt.attemptId(), attempt.purpose(),
                projectionOf(attempt), RESUME_PRACTICE_MESSAGE, null, InteractionKind.TASK,
                idempotencyKey, requestHash);
    }

    /**
     * The current Flow's open Apply Practice Attempt, scoped through the
     * Flow's exposure ledger so a Continue can never resume an Attempt from
     * another Flow.
     */
    private Optional<TaskAttempt> openPracticeAttempt(LearningState state) {
        return artifactStore.findOpenPracticeAttempt(
                flowStore.exposedTaskPackageIds(state.flow().flowId()));
    }

    /**
     * The committed-state facts of an Explain completion: no new assessment
     * has run, so the sanitized criteria and error dimensions stay empty and
     * only the readiness fact is carried.
     */
    private FeedbackFacts continueFacts(LearningState state) {
        return new FeedbackFacts(List.of(), List.of(), List.of(), 0, List.of(),
                readinessSatisfied(state.flow().flowId()));
    }

    /**
     * The committed-state facts of an H5 reveal: the closed Solution Revealed
     * Attempt's assistance is carried (the highest exposed level and the
     * exposed-only trace), with the readiness fact.
     */
    private FeedbackFacts revealFacts(LearningState state, TaskAttempt closedAttempt) {
        return new FeedbackFacts(List.of(), List.of(), List.of(),
                closedAttempt.highestHintLevel(),
                closedAttempt.assistanceTraceStrings(),
                readinessSatisfied(state.flow().flowId()));
    }

    /**
     * The readiness fact of the current remediation cycle, derived from the
     * single shared rule: at least one conclusive Apply Practice pass accepted
     * after the latest triggering failure of this Flow (Diagnostic Not Passed
     * starts the first cycle; an accepted no-hint Independent failure
     * starts a new cycle). A qualifying pass must follow the failure, so the
     * learner cannot re-enter fresh Independent testing on an old pass.
     */
    private boolean readinessSatisfied(UUID flowId) {
        return flowStore.qualifyingPracticePassExists(flowId);
    }

    private static String fallbackIntent(WorkflowGuard.DecisionContext context) {
        return "remediate_" + context.name().toLowerCase();
    }

    /**
     * The deterministic neutral learner feedback of each guarded decision
     * context, used when the plan is invalid after the one allowed repair or
     * when a single legal move bypasses the model.
     */
    private static String neutralMessage(WorkflowGuard.DecisionContext context) {
        return switch (context) {
            case DIAGNOSTIC_NOT_PASSED -> ExplainFlow.EXPLAIN_START_MESSAGE;
            case EXPLAIN_COMPLETED -> PracticeSubmissionFlow.PRACTICE_START_MESSAGE;
            case H5_REVEALED -> TeachBackFlow.TEACH_BACK_AFTER_REVEAL_MESSAGE;
            case PRACTICE_PASSED -> PracticeSubmissionFlow.INDEPENDENT_READY_MESSAGE;
            case PRACTICE_FAILED -> ExplainFlow.EXPLAIN_START_MESSAGE;
            case PRACTICE_INCONCLUSIVE -> PracticeSubmissionFlow.PRACTICE_REPLACEMENT_MESSAGE;
            case TEACH_BACK_PASSED -> TeachBackFlow.TEACH_BACK_FOLLOW_UP_MESSAGE;
            case TEACH_BACK_FAILED -> ExplainFlow.EXPLAIN_START_MESSAGE;
            case TEACH_BACK_INCONCLUSIVE -> TeachBackFlow.TEACH_BACK_REPLACEMENT_MESSAGE;
            case INDEPENDENT_FAILED -> ExplainFlow.EXPLAIN_START_MESSAGE;
        };
    }

    /**
     * One guarded decision: the selected legal Teaching Action, the
     * learner-visible message (the plan's feedback summary or the
     * deterministic neutral feedback), the teaching intent, and the sanitized
     * Feedback Facts supplied to the selected node. The invalid plan output —
     * its feedback, action, reason, and tags — never reaches the learner or
     * state.
     */
    private record Decision(
            TeachingAction action,
            String learnerMessage,
            String intent,
            FeedbackFacts facts,
            WorkflowGuard.DecisionContext context
    ) {

        private Decision {
            Objects.requireNonNull(action, "action must not be null");
            Objects.requireNonNull(learnerMessage, "learnerMessage must not be null");
            Objects.requireNonNull(intent, "intent must not be null");
            Objects.requireNonNull(facts, "facts must not be null");
            Objects.requireNonNull(context, "context must not be null");
        }
    }

    private LearningFlowResult.Boundary boundary(
            LearningState state,
            LearningStage stage,
            UUID attemptId,
            AttemptPurpose purpose,
            LearnerProjection learnerProjection,
            String learnerMessage,
            HintView hint,
            InteractionKind kind,
            UUID idempotencyKey,
            String requestHash
    ) {
        FlowStatus status = learnerProjection == null
                ? FlowStatus.TERMINAL
                : FlowStatus.AWAITING_LEARNER_INPUT;
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                kind, state.flow().flowId(), state.latestInteraction().interactionVersion() + 1, status, stage,
                attemptId, purpose, learnerProjection, learnerMessage, null, hint, null);
        return commitBoundary(interaction, null, idempotencyKey, requestHash);
    }

    /**
     * The teaching Learner Interaction Boundary of an Explain node: the flow
     * pauses awaiting learner input with the learner-visible teaching
     * projection, no Task Attempt, and the decision's learner message.
     */
    private LearningFlowResult.Boundary teachingBoundary(
            LearningState state,
            TeachingProjection teachingProjection,
            String learnerMessage,
            UUID idempotencyKey,
            String requestHash
    ) {
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                InteractionKind.TEACHING, state.flow().flowId(),
                state.latestInteraction().interactionVersion() + 1,
                FlowStatus.AWAITING_LEARNER_INPUT, LearningStage.LEARNING_AND_PRACTICE,
                null, null, null, learnerMessage, teachingProjection, null, null);
        return commitBoundary(interaction, null, idempotencyKey, requestHash);
    }

    /**
     * The durable Unavailable Interaction of an existing Flow (ADR-0069):
     * {@code AWAITING_LEARNER_INPUT} with a saved Pending Operation. A retry
     * of an already-unavailable boundary increments the Retry Chain; the
     * first unavailable boundary starts at count zero.
     */
    private LearningFlowResult.Boundary commitUnavailable(
            LearningState state,
            LearningStage stage,
            String learnerMessage,
            HintView hint,
            PendingOperation seed,
            UUID idempotencyKey,
            String requestHash
    ) {
        PendingOperation pending = seed;
        if (state.latestInteraction().kind() == InteractionKind.UNAVAILABLE) {
            PendingOperation previous = flowStore.pendingOperation(state.flow().flowId())
                    .orElseThrow(() -> new IllegalStateException(
                            "unavailable interaction missing pending operation"));
            pending = samePendingOperation(previous, seed)
                    ? previous.withFailedRetry()
                    : previous.withFailedRetryAs(seed);
        }
        LearningFlowInteraction interaction = new LearningFlowInteraction(
                InteractionKind.UNAVAILABLE, state.flow().flowId(),
                state.latestInteraction().interactionVersion() + 1,
                FlowStatus.AWAITING_LEARNER_INPUT, stage,
                null, null, null, learnerMessage, null, hint, null);
        return commitBoundary(interaction, pending, idempotencyKey, requestHash);
    }

    private static boolean samePendingOperation(PendingOperation left, PendingOperation right) {
        if (left.kind() != right.kind()) {
            return false;
        }
        if (left.kind() != PendingOperation.Kind.RESUME_SUBMISSION_EVALUATION) {
            return true;
        }
        return Objects.equals(left.attemptId(), right.attemptId())
                && Objects.equals(left.responsibility(), right.responsibility())
                && Objects.equals(left.evaluationVersion(), right.evaluationVersion());
    }

    private LearningFlowResult.Boundary commitPostSubmissionEvaluationUnavailable(
            LearningState state,
            PostSubmissionEvaluationUnavailableException unavailable,
            UUID idempotencyKey,
            String requestHash
    ) {
        return commitUnavailable(
                state,
                state.latestInteraction().stage(),
                POST_SUBMISSION_EVALUATION_UNAVAILABLE_MESSAGE,
                null,
                PendingOperation.resumeSubmissionEvaluation(
                        unavailable.attemptId(), unavailable.responsibility(), unavailable.evaluationVersion()),
                idempotencyKey,
                requestHash);
    }

    private PendingOperation executeMoveSeed(
            Decision decision,
            AcceptedLearningEvidence evidence,
            HintView hint
    ) {
        return new PendingOperation(
                PendingOperation.Kind.EXECUTE_MOVE,
                decision.action(),
                decision.context().name(),
                decision.facts(),
                decision.learnerMessage(),
                decision.intent(),
                evidence,
                hint,
                null,
                null,
                null,
                0);
    }

    private LearningFlowResult resumePending(
            LearningState state,
            PendingOperation pending,
            UUID idempotencyKey,
            String requestHash
    ) {
        return switch (pending.kind()) {
            case EXECUTE_MOVE -> executeMove(
                    state,
                    new Decision(
                            pending.action(),
                            pending.learnerMessage(),
                            pending.intent(),
                            pending.facts(),
                            WorkflowGuard.DecisionContext.valueOf(pending.decisionContext())),
                    pending.evidence(),
                    idempotencyKey,
                    requestHash);
            case DELIVER_INDEPENDENT -> deliverIndependentAfterDiagnostic(state, idempotencyKey, requestHash);
            case DELIVER_INDEPENDENT_REPLACEMENT ->
                    deliverIndependentReplacement(state, pending.learnerMessage(), idempotencyKey, requestHash);
            case RESUME_SUBMISSION_EVALUATION ->
                    resumeSubmissionEvaluation(state, pending, idempotencyKey, requestHash);
        };
    }

    /**
     * Rehydrates the saved formal submission for an evaluation retry. The
     * pending operation carries no learner answer or evaluation payload; the
     * closed Attempt is the sole source of both, and each submission flow
     * skips any responsibility already present in {@code evaluation_results}.
     */
    private LearningFlowResult resumeSubmissionEvaluation(
            LearningState state,
            PendingOperation pending,
            UUID idempotencyKey,
            String requestHash
    ) {
        TaskAttempt attempt = artifactStore.findAttempt(pending.attemptId())
                .orElseThrow(() -> new IllegalStateException(
                        "evaluation resume references an unknown Attempt"));
        TaskSubmission submission = Objects.requireNonNull(attempt.submission(),
                "evaluation resume requires a saved submission");
        String raw = submission.finalDerivative().raw();
        String confirmed = submission.finalDerivative().confirmedCanonical();
        String rationale = submission.rationale();
        return switch (attempt.purpose()) {
            case DIAGNOSTIC -> submitDiagnostic(
                    state, attempt.attemptId(), raw, confirmed, rationale, true, idempotencyKey, requestHash);
            case INDEPENDENT_TEST -> submitIndependent(
                    state, attempt.attemptId(), raw, confirmed, rationale, true, idempotencyKey, requestHash);
            case PRACTICE -> isTeachBackAttempt(attempt.attemptId())
                    ? submitTeachBack(state, attempt.attemptId(), raw, confirmed, true, idempotencyKey, requestHash)
                    : submitPractice(state, attempt.attemptId(), raw, confirmed, rationale,
                            true, idempotencyKey, requestHash);
            case REVIEW -> submitReview(
                    state, attempt.attemptId(), raw, confirmed, rationale, idempotencyKey, requestHash);
            default -> new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.WRONG_ATTEMPT_PURPOSE);
        };
    }

    /**
     * Resumes a Diagnostic pass whose Independent generation failed: the
     * closed Diagnostic Attempt stays closed and a fresh Independent task is
     * prepared from durable novelty exclusions.
     */
    private LearningFlowResult deliverIndependentAfterDiagnostic(
            LearningState state,
            UUID idempotencyKey,
            String requestHash
    ) {
        ApplyDeliveryResult delivery = practiceFlow.deliverIndependent(
                state.flow().flowId(), state.flow().modelProfile());
        return switch (delivery) {
            case ApplyDeliveryResult.Delivered delivered -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, delivered.attempt().attemptId(),
                    delivered.attempt().purpose(), delivered.learnerProjection(),
                    DiagnosticFlow.NEUTRAL_TRANSITION_MESSAGE, null, InteractionKind.TASK,
                    idempotencyKey, requestHash);
            case ApplyDeliveryResult.Unavailable unavailable -> commitUnavailable(
                    state, LearningStage.DIAGNOSTIC, unavailable.learnerMessage(), null,
                    new PendingOperation(PendingOperation.Kind.DELIVER_INDEPENDENT, null, null, null,
                            unavailable.learnerMessage(), null, null, null,
                            null, null, null, 0),
                    idempotencyKey, requestHash);
        };
    }

    /**
     * The single durable commit of one Learner Interaction Boundary: the
     * learner-visible interaction, its checkpoint, and the processed command
     * persist atomically, so a replay always returns the original result and a
     * restart resumes exactly from this point. A null pending operation
     * clears any saved Pending Operation.
     */
    private LearningFlowResult.Boundary commitBoundary(
            LearningFlowInteraction interaction,
            PendingOperation pending,
            UUID idempotencyKey,
            String requestHash
    ) {
        LearningFlowInteraction committed = flowStore.commitBoundary(
                interaction,
                new LearningCheckpoint(UUID.randomUUID(), interaction.flowId(),
                        interaction.interactionVersion(), clock.instant()),
                new ProcessedCommand(idempotencyKey, requestHash, interaction.flowId(),
                        interaction, clock.instant()),
                pending);
        return new LearningFlowResult.Boundary(committed);
    }
}
