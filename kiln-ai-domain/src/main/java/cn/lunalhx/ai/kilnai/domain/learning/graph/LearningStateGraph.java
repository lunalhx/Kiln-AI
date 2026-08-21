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
import cn.lunalhx.ai.kilnai.domain.learning.diagnostic.DiagnosticPlan;
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
 * 学习状态图运行器（ADR-0066）。每条学习者命令执行一次 Graph Run：
 * 从持久化记录和已保存的检查点恢复学习状态，跑确定性入门检查，把命令
 * 路由到唯一合法的节点（诊断、讲解、练习、独立提交、提示或 Teach-back），
 * 然后原子提交下一条学习者交互边界（交互、检查点、已处理命令一起落库）。
 *
 * <p>在每个需要教学决策的节点（诊断未通过、讲解完成、练习或 Teach-back
 * 结果、H5 揭示、是否够格做独立测试），先由确定性的 Workflow Guard 从
 * 已提交状态推导出合法下一步集合，有界 Pedagogy Agent 只能在集合内选一个
 * （一次初始规划 + 至多一次同规划修复；输出非法就整体丢弃，走 spec 的
 * 确定性 fallback；fallback 节点不可用时停在安全边界）。只有一个合法动作
 * 时 Guard 直接绕过模型。Agent 只收到清理过的 FeedbackFacts 和合法动作
 * 集合——永远看不到原始答案、期望答案、评估理由、Skill id，也不能访问
 * 状态——它从不写学习状态，只有本运行器及其节点能操作 store。提交或提示
 * 暴露两次提交之间崩溃时，从已保存的 Attempt 或提示请求恢复。
 */
public final class LearningStateGraph {

    public static final String RESUME_PRACTICE_MESSAGE = "请继续完成当前练习题。";

    /**
     * 开始流程时初始准备失败的通用提示（spec："Initial failure returns a
     * generic 503 and the client reuses the original Idempotency-Key"）。
     * 失败的 Start 不落库，所以这条提示永远不会暴露 provider、模型、
     * 来源或解析器细节。
     */
    public static final String START_UNAVAILABLE_MESSAGE = "暂时无法开始学习，请稍后重试。";

    /**
     * 提交后评估无法完成时的中性提示。恢复状态在已保存的 Attempt 和
     * Pending Operation 里，不在这条消息里。
     */
    public static final String POST_SUBMISSION_EVALUATION_UNAVAILABLE_MESSAGE =
            ModelContractInvalidException.LEARNER_SAFE_MESSAGE;

    /**
     * 澄清驱动的临时讲解的教学意图：纯粹回答学习者的实质问题，
     * 不是评估，也不是新题目。
     */
    public static final String CLARIFICATION_INTENT = "clarification_assistance";

    /**
     * 学习者接受帮助时看到的后果提示：开放的独立测试或复习 Attempt 会在
     * 展示任何帮助内容之前单向转为练习（ADR-0014）。
     */
    public static final String CONSENT_WARNING_MESSAGE =
            "请求帮助将不再计入独立成绩：本次尝试将不可逆地转为练习。是否继续？";

    public static final String ASSISTANCE_REFUSED_MESSAGE = "已放弃帮助，本次尝试保持不变，请继续作答。";

    public static final String ASSISTANCE_CONVERTED_MESSAGE =
            "本次尝试已转为练习，请先阅读下面的讲解，之后可继续作答或请求提示。";

    public static final String CLARIFICATION_EXPLAIN_MESSAGE =
            "以下是针对您疑问的讲解，之后请继续完成当前题目。";

    /**
     * 诊断或 Teach-back 任务上拒绝实质/不确定澄清的提示（ticket 06）：
     * 该阶段不提供概念讲解，不补充教学内容，开放 Attempt 的用途和
     * 证据资格永远不变。
     */
    public static final String TASK_CLARIFICATION_NOT_OFFERED_MESSAGE =
            "当前任务阶段不提供概念讲解。请按题目中已展示的格式与记号继续作答。";

    /**
     * 独立讲解交互上拒绝实质/不确定澄清的提示（ticket 06）：
     * 讲解不再补充教学内容，教学边界保持不变。
     */
    public static final String TEACHING_CLARIFICATION_NOT_OFFERED_MESSAGE =
            "本次讲解不回答额外概念问题。您可以继续进入下一步，或就已展示内容的格式、记号与界面操作提问。";

    /**
     * 显式离开流程的提示（ADR-0015）：任何开放的 Attempt 以 Abandoned
     * 关闭——不提交、不评估、不产生证据——流程停在终结性 transition 边界。
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
     * 流程开始的 Graph Run：先完整准备诊断节点——解析配置、生成、输出
     * 门控、任务验证全部完成（不落库）——然后整个 Start 原子提交一次：
     * Flow 记录、来源包、任务包、开放的 Attempt、曝光、第一条学习者交互、
     * 检查点和已处理命令（ADR-0063）。初始准备失败返回通用 503，不留任何
     * 痕迹，客户端复用原 Idempotency-Key 重试。
     */
    public LearningFlowResult start(
            UUID flowId,
            UUID learnerId,
            UUID conceptId,
            ModelProfile profile,
            DiagnosticPlan diagnosticPlan,
            SourceArtifact source,
            UUID idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(flowId, "flowId must not be null");
        Objects.requireNonNull(learnerId, "learnerId must not be null");
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(diagnosticPlan, "diagnosticPlan must not be null");
        Objects.requireNonNull(source, "source must not be null");
        ApplyProfileExecutor.PreparedDelivery prepared = diagnosticFlow.prepareDiagnostic(profile);
        return switch (prepared) {
            case ApplyProfileExecutor.PreparedDelivery.Unavailable unavailable ->
                    throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, START_UNAVAILABLE_MESSAGE);
            case ApplyProfileExecutor.PreparedDelivery.TaskReady ready -> {
                LearningFlowInteraction interaction = flowStore.bindStart(new LearningFlowStore.StartBind(
                        flowId, learnerId, conceptId, profile, diagnosticPlan, source, ready.taskPackage(),
                        ready.verdict(), idempotencyKey, requestHash));
                yield new LearningFlowResult.Boundary(interaction);
            }
        };
    }

    /**
     * 提交答案的 Graph Run：恢复已提交状态，拒绝过期 interactionVersion
     * 或未知 Attempt，按 Attempt 的 purpose 路由到唯一合法的节点，
     * 停在下一个已提交的学习者交互边界。
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
     * Attempt 必须属于当前 Flow，且必须是当前交互指向的那个。
     * 未知 Attempt 视为找不到；已被后续交互替换的 Attempt 不能再路由。
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
     * Teach-back Attempt 的 purpose 是 PRACTICE，但它后面是 teach-back
     * 任务包；图按包类型区分，普通练习提交永远不会路由进 Teach-back 节点，
     * 反之亦然。
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
     * continue-requested 的 Graph Run：恢复已提交状态，拒绝过期版本，
     * 只有教学边界可以继续。开放练习 Attempt 内的临时讲解结束时，Guard
     * 推导出唯一合法动作——回到同一个练习交互——直接绕过模型；否则从
     * 已提交状态推导合法动作，由 Pedagogy Agent 选择下一个教学节点。
     * 在其他边界上 continue 被忽略，状态不变。
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
     * hint-requested 的 Graph Run：第一次请求时生成并门控整条私有阶梯，
     * 只揭示请求的合法级别，停在下一个已提交的交互边界。H1-H4 保持练习
     * Attempt 开放，供之后正式提交；H5 揭示原子地以 Solution Revealed
     * 关闭 Attempt——不评估、不产生证据——并把它记为 Teach-back anchor，
     * 然后 Guard 推导下一步（Teach-back 和新练习都合法，确定性 fallback
     * 选 Teach-back）。阶梯生成失败不暴露任何内容，Attempt 保持在安全的
     * 消息边界。诊断、独立、复习、Teach-back Attempt 永远不允许提示。
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
     * clarification-asked 的 Graph Run：Clarification Gate 对自由文本分类，
     * 按当前交互路由。独立讲解交互上，命令指向当前交互边界而不是 Attempt
     * （spec）：程序性问题用确定性重述回答、教学边界不变；实质/不确定问题
     * 不补充教学内容。诊断或 Teach-back 任务只允许程序性澄清——重述题目
     * 格式契约并记录帮助；实质/不确定请求不改变 Attempt 用途和证据资格
     * （ticket 06）。开放的练习 Attempt 上，实质/不确定请求记录帮助并给出
     * 临时讲解教学边界；开放的独立或复习 Attempt 上，先展示帮助-同意请求
     * （不转换、不记录、不教学）。
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
        // 独立讲解（或任何教学交互）指向当前交互边界，不需要 Attempt ID：
        // 它的澄清既不要求也不使用它——任何传入的 Attempt ID 都被忽略，
        // 因为当前交互才是权威。
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
        // 诊断和 Teach-back 只接受程序性澄清：实质或不确定请求不补充
        // 教学内容，不改变 Attempt 用途和证据资格。
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
     * 对澄清消息做一次分类，对照学习者可见的展示内容（题目文本或讲解
     * 内容）。模型契约非法时回退为 UNCERTAIN，并记录一条有界澄清审计
     * （只含可用身份，绝不含原始非法载荷）。
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
     * 独立讲解上的澄清路径：命令指向当前教学交互边界，不带 Attempt ID。
     * 程序性问题直接用确定性重述回答；实质/不确定问题拒绝且不补充内容。
     * 两种情况都重新提交同一个教学边界，可审计记录就是已提交的交互和
     * 已处理命令（ticket 06）。
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
     * 程序性讲解澄清：gate 重述展示的教学条件，重新提交同一个教学边界。
     * 不加载任何教学 Profile，也不生成新的讲解。
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
     * 实质/不确定讲解澄清：不补充教学内容，用拒绝消息重新提交同一个
     * 教学边界。
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
     * 诊断或 Teach-back 任务上拒绝实质/不确定澄清：不补充内容、不记录
     * 帮助，开放 Attempt 的用途和证据资格不变。提交的同一任务边界和
     * 已处理命令就是可审计记录。
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
     * assistance-decided 的 Graph Run，作用于开放的独立或复习 Attempt。
     * 拒绝时 Attempt 完全不变，回到同一任务边界。接受时先生成临时讲解，
     * 然后原子地把 Attempt 单向转为练习（记录实质澄清和临时讲解），若原来
     * 是复习还取消已开始的复习任务——不产生复习证据、milestone 不变
     * （ADR-0062）——再展示教学边界。生成失败不转换任何东西，回到原任务，
     * 学习者可以重试或拒绝。接受后转换已提交但进程崩溃的（转换和它的边界
     * 之间），重试走 Already-Practice 路径恢复同一个教学边界：轨迹不会
     * 追加两次，命令也不会对已提交的一半报 409。
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
            // 开放的练习 Attempt 要么是已提交转换的重试的一半，要么是
            // 客户端发错了 purpose；接受的重试恢复帮助展示，拒绝永远不能
            // 撤销已提交的单向转换。
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
     * flow-control-requested 的 Graph Run：学习者显式离开流程（ADR-0015）。
     * 任何开放的 Attempt 先以 Abandoned 关闭——不提交、不评估、不产生证据、
     * milestone 不变——已开始的复习保持 Started，直到学习者用独立的复习
     * 取消接口（ADR-0073）。然后停在只带离开消息的终结性 transition 边界；
     * 网络断开或普通延迟不是显式离开事件，Attempt 保持开放。
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
     * retry-requested 的 Graph Run：只在 unavailable 交互上合法，且重试链
     * 未耗尽。它从持久状态恢复保存的 Pending Operation 并继续，不带任何
     * 客户端答案载荷（ADR-0069）。重试失败递增重试链并提交新的 unavailable
     * 版本；成功进入下一条交互时清除 Pending Operation。
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
     * 接受帮助的转换：先生成、门控、持久化临时讲解，只有交付成功才转换
     * Attempt 并取消复习任务。生成失败时独立 Attempt 保持原样，停在可
     * 重试的边界。Already-Practice 转换——重试时转换已提交——恢复同一个
     * 教学边界，不重复追加轨迹。
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
     * 开放练习 Attempt 上的实质澄清路径：先生成并持久化临时讲解，把记录的
     * 帮助（实质澄清和临时讲解）追加到开放 Attempt 的轨迹，再提交教学边界。
     * Guard 的唯一合法下一步——回到同一个开放练习交互——由现有 Continue
     * 命令到达。生成失败不追加任何东西，Attempt 停在可重试边界。
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
     * 在开放 Attempt 内展示实质澄清内容和临时讲解时记录的帮助条目：
     * 每次实际交付的帮助一条，之后的练习评估会如实带上两者。
     */
    private List<AssistanceTraceEntry> recordedClarification() {
        return List.of(
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.SUBSTANTIVE_CLARIFICATION,
                        clock.instant()),
                AssistanceTraceEntry.clarification(AssistanceTraceEntry.AssistanceKind.TEMPORARY_EXPLAIN,
                        clock.instant()));
    }

    /**
     * 程序性澄清路径：gate 用确定性重述直接回答题目自身的格式契约，该回答
     * 作为程序性帮助记录在开放 Attempt 上，然后停在同一个任务边界。不加载
     * 任何教学 Profile，Attempt 的用途不变。
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
     * 帮助-同意学习者交互边界：开放 Attempt 保持原样，学习者看到的同意
     * 投影说明单向转换的后果。任何教学内容、轨迹或转换都不会先于这个
     * 边界（ADR-0014）。
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
        // 生成的阶梯出现后，其内容指纹进入 Flow 的 novelty 账本；H5 揭示
        // 还额外记录已揭示答案的指纹，之后生成永远不会复用已暴露的提示
        // 内容或已揭示的答案。
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
        // H5 揭示以 Solution Revealed 关闭 Attempt，成为最近暴露的合法
        // Teach-back anchor；图在 Guard 决策下一步之前持久化记录它
        // （按 anchor id 幂等）。
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
            // Neutral Transition 消息是学习者可见内容（CONTEXT.md），
            // 投影在边界上；旧的 Apply seam 在交互层面丢掉了它。它只说
            // 下一步交互，不带任何反馈。
            case DiagnosticSubmissionResult.Passed passed -> boundary(
                    state, LearningStage.INDEPENDENT_TEST, passed.independentAttempt().attemptId(),
                    passed.independentAttempt().purpose(), passed.independentLearnerProjection(),
                    passed.neutralTransitionMessage(), null, InteractionKind.TASK, idempotencyKey, requestHash);
            // 已提交的诊断失败保持关闭，永远不会追溯转换。Workflow Guard
            // 从已提交状态推导合法补救动作，Pedagogy Agent 从该封闭集合
            // 中选择下一个教学节点。
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

    /**
     * 独立测试节点的一次提交：Flow 关闭并评估 Attempt，证据被原子接受，
     * 停在安全的终结 transition。结论性通过或失败恰好接受一条独立证据；
     * Inconclusive 判定不产生证据，store 原子地绑定已验证的替换题（或
     * 无法准备时的中性继续）。重复提交或未关闭的提交永远不产生证据。
     */
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
            // 结论性无提示独立失败：恰好接受一条失败证据（只在所选补救
            // 节点生成成功之后），Current Milestone 降回 Learning，通过
            // Guard 开始补救——Explain 和新练习都合法，Pedagogy Agent 选一个。
            case IndependentSubmissionResult.FailureEvidenceAccepted failed ->
                    executeMove(state, chooseDecision(state, WorkflowGuard.DecisionContext.INDEPENDENT_FAILED,
                                    failed.facts(), evaluationRecovery),
                            failed.evidence(), idempotencyKey, requestHash);
            // Blocked 或 Inconclusive 判定不产生证据、milestone 不变：
            // 用所有适用的 novelty 排除交付一道全新已验证独立替换题。
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
     * Blocked 或 Inconclusive 判定要求的全新已验证独立替换题：用所有适用
     * novelty 排除生成；无法准备时给出可恢复的 unavailable 边界。
     * 两条路径都不产生证据、milestone 不变。
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
     * 复习节点的一次提交：Flow 关闭并评估 Attempt，恰好推进一次 cadence。
     * 结论性通过或失败恰好接受一条复习证据并停在安全的终结 transition；
     * Inconclusive 判定不产生证据，store 原子地绑定已验证的替换题（或
     * 无法准备时的中性继续）。重复提交或未关闭的提交永远不产生证据。
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
            // Inconclusive 替换边界由复习 store 自己原子提交
            // （任务或中性继续）。
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
     * 练习节点的一次提交：Flow 关闭并评估 Attempt，Workflow Guard 从结果
     * 和已提交状态推导合法下一步，Pedagogy Agent 选一个——结论性通过可能
     * 让新的独立测试合法（readiness），结论性失败或 Inconclusive 永远不能。
     * 证据只在所选后续节点生成成功后才接受，所以生成失败不留下证据，
     * 重试会恢复原始结果。
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
            // ADR-0067：答案正确但理由明显矛盾，在练习里同样是结论性失败。
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
     * Teach-back 节点的一次提交：Flow 关闭并评估 Attempt，Workflow Guard
     * 推导合法下一步，Pedagogy Agent 选一个。结论性通过或失败恰好接受一条
     * 理解维度证据——绝不是独立证据，因为 Teach-back 通过不算练习
     * readiness——Inconclusive 判定不产生证据，唯一合法动作是同一 anchor
     * 上的全新 Teach-back 替换。
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
     * 一次受 Guard 约束的教学决策：确定性 Workflow Guard 从已提交状态
     * 推导合法动作集合（合法 anchor、开放练习 Attempt、readiness），有界
     * Pedagogy Agent 只在集合内选一个——一次初始规划 + 至多一次同规划修复，
     * 之后整个非法输出丢弃，走 spec 的确定性 fallback 并给出中性反馈。
     * 只有一个合法动作时完全绕过模型。Agent 只收到清理过的 FeedbackFacts
     * 和合法集合，永远看不到答案、评估理由或 Skill id。
     */
    private Decision decide(
            LearningState state,
            WorkflowGuard.DecisionContext context,
            FeedbackFacts facts
    ) {
        return chooseDecision(state, context, facts, false);
    }

    /**
     * 从已提交的评估检查点重放提交后的路由。fallback 由 Guard 决定；
     * 重放永远不会为尚未到达学习者边界的路由让 Pedagogy Agent 做第二次
     * 决策。
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
     * 执行受 Guard 约束的决策，调用唯一合法的教学节点。提交的证据候选
     * （诊断未通过或 Inconclusive 判定时为 null）只在所选节点生成、门控、
     * 验证成功之后接受，所以生成失败不留下证据，命令可以重试。
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
     * 在所选后续节点生成、门控、验证成功后接受提交的证据候选，所以生成
     * 失败不接受证据，命令可以重试。并发或重放的命令已为同一 Attempt 接受
     * 证据时，返回已提交的 ignore 结果。
     */
    private Optional<LearningFlowResult> acceptEvidenceOrIgnore(AcceptedLearningEvidence evidence) {
        if (evidence != null && !flowStore.acceptEvidence(evidence)) {
            return Optional.of(new LearningFlowResult.SubmissionIgnored(SubmissionIgnoreReason.ALREADY_SUBMITTED));
        }
        return Optional.empty();
    }

    /**
     * Explain 动作：纯教学，交付一次来源支撑的教学交互，带 Guard 的意图和
     * 清理过的 FeedbackFacts；无法准备时给出可恢复的 unavailable 边界。
     * 它从不创建任务包、Attempt、评估或证据。交付的工作示例成为最近暴露的
     * 合法 Teach-back anchor。
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
     * 练习动作：交付一个全新已验证的练习任务（冻结的 Practice Blueprint），
     * 无法准备时给出可恢复的 unavailable 边界。可选的揭示提示（H5 继续）
     * 投影在同一个边界上。
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
     * Teach-back 动作：交付一个锚定的已验证短文本任务，无法准备时给出
     * 可恢复的 unavailable 边界。可选的揭示提示投影在同一个边界上。
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
     * 全新独立测试动作：交付一个全新已验证的独立任务——只在当前
     * remediation cycle 的练习通过前提满足后合法——无法准备时给出可恢复的
     * unavailable 边界。可选的揭示提示投影在同一个边界上。
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
     * 开放练习 Attempt 内展示临时讲解后的唯一合法动作：不生成任何东西，
     * 重新投影同一个开放的练习交互。开放 Attempt 已不在已提交状态中时，
     * Continue 被忽略，状态不变。
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
     * 当前 Flow 的开放练习 Attempt，通过 Flow 的曝光账本限定范围，
     * 所以 Continue 永远不会恢复另一个 Flow 的 Attempt。
     */
    private Optional<TaskAttempt> openPracticeAttempt(LearningState state) {
        return artifactStore.findOpenPracticeAttempt(
                flowStore.exposedTaskPackageIds(state.flow().flowId()));
    }

    /**
     * 讲解完成时的已提交状态事实：没有跑新评估，所以清理过的 criteria 和
     * 错误维度为空，只带 readiness 事实。
     */
    private FeedbackFacts continueFacts(LearningState state) {
        return new FeedbackFacts(List.of(), List.of(), List.of(), 0, List.of(),
                readinessSatisfied(state.flow().flowId()));
    }

    /**
     * H5 揭示时的已提交状态事实：携带已关闭的 Solution Revealed Attempt
     * 的帮助信息（最高暴露级别和仅暴露过的轨迹），加上 readiness 事实。
     */
    private FeedbackFacts revealFacts(LearningState state, TaskAttempt closedAttempt) {
        return new FeedbackFacts(List.of(), List.of(), List.of(),
                closedAttempt.highestHintLevel(),
                closedAttempt.assistanceTraceStrings(),
                readinessSatisfied(state.flow().flowId()));
    }

    /**
     * 当前 remediation cycle 的 readiness 事实，来自单一共享规则：Flow
     * 最近一次触发失败（诊断未通过开始第一个 cycle；无提示独立失败开始
     * 新 cycle）之后至少接受过一次结论性练习通过。合格通过必须跟在失败
     * 之后，学习者不能用旧的通过重新进入独立测试。
     */
    private boolean readinessSatisfied(UUID flowId) {
        return flowStore.qualifyingPracticePassExists(flowId);
    }

    private static String fallbackIntent(WorkflowGuard.DecisionContext context) {
        return "remediate_" + context.name().toLowerCase();
    }

    /**
     * 每个 Guard 决策上下文的确定性中性反馈：规划在允许的一次修复后仍非法、
     * 或唯一合法动作绕过模型时使用。
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
     * 一次受 Guard 约束的决策：选中的合法教学动作、学习者可见消息（规划的
     * 反馈摘要或确定性中性反馈）、教学意图、以及提供给所选节点的清理过的
     * FeedbackFacts。非法规划输出——它的反馈、动作、理由和标签——永远不会
     * 到达学习者或状态。
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
     * Explain 节点的教学交互边界：流程暂停等待学习者输入，带学习者可见的
     * 教学投影、无任务 Attempt、以及决策的学习者消息。
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
     * 现有 Flow 的持久化 unavailable 交互（ADR-0069）：AWAITING_LEARNER_INPUT
     * 并保存 Pending Operation。对已 unavailable 的边界重试会递增重试链；
     * 第一个 unavailable 边界从 0 开始。
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
     * 为评估重试恢复已保存的正式提交。Pending Operation 不带学习者答案或
     * 评估载荷；已关闭的 Attempt 是两者的唯一来源，各提交 Flow 会跳过
     * evaluation_results 里已有的责任项。
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
     * 恢复诊断通过但独立题生成失败的情况：诊断 Attempt 保持关闭，
     * 用持久化的 novelty 排除准备一道全新独立题。
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
     * 一条学习者交互边界的唯一原子提交点：学习者可见交互、检查点和已处理
     * 命令一起持久化，所以重放总是返回原始结果，重启正好从这里恢复。
     * pending 为 null 时清除已保存的 Pending Operation。
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
