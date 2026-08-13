package cn.lunalhx.ai.kilnai.domain.learning.kernel;

import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;
import cn.lunalhx.ai.kilnai.domain.artifact.PedagogyPlan;
import cn.lunalhx.ai.kilnai.domain.artifact.TeachingResultEnvelope;
import cn.lunalhx.ai.kilnai.domain.blackboard.BlackboardDelta;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.gate.EvidenceCandidateGatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.GateViolation;
import cn.lunalhx.ai.kilnai.domain.gate.PedagogyPlanGatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.TeachingResultGatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.AssessmentModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.PedagogyModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.TeachingModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ToolSession;
import cn.lunalhx.ai.kilnai.domain.learning.model.AssessmentContextView;
import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.ModelCallObservation;
import cn.lunalhx.ai.kilnai.domain.learning.model.PedagogyContextView;
import cn.lunalhx.ai.kilnai.domain.learning.model.TeachingContextView;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ConceptProgress;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningStage;
import cn.lunalhx.ai.kilnai.domain.learning.service.ConceptProgressProjector;
import cn.lunalhx.ai.kilnai.domain.learning.service.EvidenceEligibility;
import cn.lunalhx.ai.kilnai.domain.learning.service.GuardSnapshot;
import cn.lunalhx.ai.kilnai.domain.learning.service.WorkflowGuard;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import cn.lunalhx.ai.kilnai.domain.skill.PromptCompiler;
import cn.lunalhx.ai.kilnai.domain.skill.SkillManifest;
import cn.lunalhx.ai.kilnai.domain.skill.SkillResolver;
import cn.lunalhx.ai.kilnai.domain.skill.SkillSlot;
import cn.lunalhx.ai.kilnai.domain.skill.SkillStack;
import cn.lunalhx.ai.kilnai.domain.tool.CalculatorToolSession;
import cn.lunalhx.ai.kilnai.domain.tool.ToolHandle;
import cn.lunalhx.ai.kilnai.domain.tool.ToolPermissionSet;
import cn.lunalhx.ai.kilnai.domain.tool.ToolResolver;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class LearningNodeKernel {

    private final WorkflowGuard guard = new WorkflowGuard();
    private final BlackboardApplier applier = new BlackboardApplier();
    private final SkillResolver skillResolver = new SkillResolver();
    private final ToolResolver toolResolver = new ToolResolver();
    private final PromptCompiler promptCompiler = new PromptCompiler();
    private final ValidatedNodeExecutor executor = new ValidatedNodeExecutor(new TypedArtifactGatePipeline());
    private final PedagogyPlanGatePolicy pedagogyPolicy = new PedagogyPlanGatePolicy();
    private final TeachingResultGatePolicy teachingPolicy = new TeachingResultGatePolicy();
    private final EvidenceCandidateGatePolicy evidencePolicy = new EvidenceCandidateGatePolicy();
    private final EvidenceEligibility eligibility = new EvidenceEligibility();
    private final ConceptProgressProjector projector = new ConceptProgressProjector();
    private final PendingCommitBuffer buffer;
    private final GraphRunBudgetHolder budgets;
    private final PedagogyModelPort pedagogyModel;
    private final TeachingModelPort teachingModel;
    private final AssessmentModelPort assessmentModel;
    private final SpikeStorePort store;
    private final boolean calculatorAvailable;
    private final Clock clock;
    private final ModelCallObservationHolder observations;
    private final List<SkillManifest> registry;

    public LearningNodeKernel(
            PendingCommitBuffer buffer,
            GraphRunBudgetHolder budgets,
            PedagogyModelPort pedagogyModel,
            TeachingModelPort teachingModel,
            AssessmentModelPort assessmentModel,
            SpikeStorePort store,
            boolean calculatorAvailable,
            Clock clock
    ) {
        this(buffer, budgets, pedagogyModel, teachingModel, assessmentModel, store, calculatorAvailable, clock, new ModelCallObservationHolder());
    }

    public LearningNodeKernel(
            PendingCommitBuffer buffer,
            GraphRunBudgetHolder budgets,
            PedagogyModelPort pedagogyModel,
            TeachingModelPort teachingModel,
            AssessmentModelPort assessmentModel,
            SpikeStorePort store,
            boolean calculatorAvailable,
            Clock clock,
            ModelCallObservationHolder observations
    ) {
        this.buffer = buffer;
        this.budgets = budgets;
        this.pedagogyModel = pedagogyModel;
        this.teachingModel = teachingModel;
        this.assessmentModel = assessmentModel;
        this.store = store;
        this.calculatorAvailable = calculatorAvailable;
        this.clock = clock;
        this.observations = observations;
        this.registry = defaultRegistry();
    }

    public String nextRoute(LearningBlackboard blackboard) {
        GuardSnapshot snapshot = snapshot(blackboard);
        if (!guard.isLegalInput(snapshot) && blackboard.pendingInput() != null) {
            return "end";
        }
        if (guard.shouldAssess(snapshot)) {
            return "assess";
        }
        List<TeachingAction> legal = guard.legalCandidates(snapshot);
        if (legal.size() == 1 && legal.get(0) == TeachingAction.EXPLAIN) {
            return "explain";
        }
        if (legal.size() > 1) {
            return "pedagogy";
        }
        return "end";
    }

    public AuthorizedNodeResult ingest(LearningBlackboard blackboard, LearnerInputKind kind, String text) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("pendingInput", kind);
        fields.put("lastRoute", "ingest");
        LearningBlackboard withInput = applier.apply(blackboard, new BlackboardDelta("input", fields));
        fields.put("legalCandidates", guard.legalCandidates(snapshot(withInput)));
        BlackboardDelta delta = new BlackboardDelta("input", fields);
        LearningBlackboard updated = applier.apply(blackboard, delta);
        if (kind != null && !guard.isLegalInput(snapshot(updated))) {
            throw new ApplicationException(ErrorCode.UNPROCESSABLE, "illegal event for current learning state");
        }
        if (kind != null && kind != LearnerInputKind.CONTINUE_REQUESTED && kind != LearnerInputKind.ANSWER_SUBMITTED) {
            CommitEffects effects = interactionEffects(updated, "This spike only advances CONTINUE_REQUESTED and ANSWER_SUBMITTED.");
            buffer.put(blackboard.flowId(), effects);
            return new AuthorizedNodeResult(updated, delta, effects, "end");
        }
        return new AuthorizedNodeResult(updated, delta, null, nextRoute(updated));
    }

    public AuthorizedNodeResult pedagogy(LearningBlackboard blackboard) {
        chargeNode(blackboard);
        PedagogyContextView view = new PedagogyContextView(
                blackboard.flowId(), blackboard.stage(), blackboard.legalCandidates(), blackboard.compactFeedbackFacts()
        );
        String prompt = "Legal actions: " + view.legalCandidates()
                + ". Feedback: " + view.compactFeedbackFacts()
                + (blackboard.explanationDelivered()
                ? " Explanation already delivered; choose APPLY."
                : " Choose EXPLAIN.");
        PedagogyPlan candidate = pedagogyModel.propose(view, prompt);
        GateContext context = new GateContext(Set.copyOf(blackboard.legalCandidates().stream().map(Enum::name).toList()));
        GateResult<PedagogyPlan> result = executor.execute(
                candidate,
                pedagogyPolicy,
                context,
                (ignored, violations) -> pedagogyModel.propose(view, withViolations(prompt, violations))
        );
        TeachingAction action = result.outcome() == GateOutcome.PASSED ? result.artifact().nextAction() : guard.fallbackAction();
        BlackboardDelta delta = new BlackboardDelta("pedagogy", Map.of(
                "acceptedAction", action,
                "lastRoute", "pedagogy",
                "compactFeedbackFacts", List.of(candidate.feedbackSummary())
        ));
        LearningBlackboard updated = applier.apply(blackboard, delta);
        return new AuthorizedNodeResult(updated, delta, null, action == TeachingAction.APPLY ? "apply" : "explain");
    }

    public AuthorizedNodeResult explain(LearningBlackboard blackboard) {
        return teach(blackboard, TeachingAction.EXPLAIN);
    }

    public AuthorizedNodeResult apply(LearningBlackboard blackboard) {
        return teach(blackboard, TeachingAction.APPLY);
    }

    public AuthorizedNodeResult assess(LearningBlackboard blackboard, String answer) {
        chargeNode(blackboard);
        Map<String, Object> taskPackage = blackboard.taskPackageArtifactId() == null
                ? Map.of()
                : store.artifact(blackboard.taskPackageArtifactId()).orElse(Map.of());
        Map<String, Object> isolatedPackage = new LinkedHashMap<>(taskPackage);
        isolatedPackage.remove("hiddenReasoning");
        AssessmentContextView view = new AssessmentContextView(blackboard.flowId(), isolatedPackage, answer, List.of());
        String prompt = "Assess the learner answer against the isolated task package.";
        EvidenceCandidate candidate = assessmentModel.assess(view, prompt);
        GateResult<EvidenceCandidate> gated = executor.execute(
                candidate,
                evidencePolicy,
                GateContext.empty(),
                (ignored, violations) -> assessmentModel.assess(view, withViolations(prompt, violations))
        );
        boolean already = blackboard.openAttemptId() != null && store.evidenceExists(blackboard.openAttemptId());
        AcceptedLearningEvidence evidence = null;
        ConceptProgress progress = projector.project(blackboard.learnerId(), blackboard.conceptId(), List.of());
        if (gated.outcome() == GateOutcome.PASSED
                && eligibility.eligible(AttemptStatus.SUBMITTED, AttemptPurpose.PRACTICE, gated.artifact(), already)) {
            evidence = new AcceptedLearningEvidence(
                    UUID.randomUUID(), blackboard.openAttemptId(), blackboard.flowId(), blackboard.conceptId(),
                    blackboard.learnerId(), gated.artifact().result(), AttemptPurpose.PRACTICE, 0, List.of(), clock.instant()
            );
            progress = projector.project(blackboard.learnerId(), blackboard.conceptId(), List.of(evidence));
        }
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", FlowStatus.TERMINAL);
        fields.put("stage", LearningStage.LEARNING_AND_PRACTICE);
        fields.put("interactionVersion", blackboard.interactionVersion() + 1);
        fields.put("pendingInput", null);
        fields.put("visibleContent", evidence == null
                ? "Assessment was inconclusive. No evidence was accepted."
                : "Practice complete. Current milestone: " + progress.currentMilestone() + ".");
        fields.put("allowedEventKinds", List.of());
        fields.put("lastRoute", "assess");
        BlackboardDelta delta = new BlackboardDelta("evidence", fields);
        LearningBlackboard updated = applier.apply(blackboard, delta);
        CommitEffects effects = new CommitEffects(
                updated.flowId(), updated.visibleContent(), updated.allowedEventKinds(), updated.status(),
                updated.stage(), updated.interactionVersion(), null, updated.taskPackageArtifactId(),
                updated.openAttemptId(), evidence, progress,
                Map.of("node", "assess", "candidatePresentInState", false),
                publicTracePayload(updated.flowId(), Map.of(
                        "route", "assess",
                        "skills", List.of(),
                        "budget", budgetTrace(updated.flowId())
                )),
                delta
        );
        buffer.put(updated.flowId(), effects);
        return new AuthorizedNodeResult(updated, delta, effects, "end");
    }

    private AuthorizedNodeResult teach(LearningBlackboard blackboard, TeachingAction action) {
        chargeNode(blackboard);
        SkillStack stack = skillResolver.resolve(
                action,
                action == TeachingAction.APPLY ? Set.of("quantitative") : Set.of(),
                action == TeachingAction.APPLY ? Set.of("worked-example") : Set.of(),
                registry
        );
        Set<String> toolsNeeded = new java.util.HashSet<>(stack.actionSkill().requiredTools());
        stack.capabilitySkills().forEach(skill -> toolsNeeded.addAll(skill.requiredTools()));
        Set<ToolHandle> runtime = calculatorAvailable
                ? Set.of(new ToolHandle(
                        "calculator",
                        1,
                        "{\"type\":\"object\",\"properties\":{\"old\":{\"type\":\"number\"},\"new\":{\"type\":\"number\"}},\"required\":[\"old\",\"new\"]}"
                ))
                : Set.of();
        List<ToolHandle> tools = toolResolver.resolve(
                new ToolPermissionSet(action == TeachingAction.APPLY ? Set.of("calculator@1") : Set.of()),
                toolsNeeded,
                runtime
        );
        String prompt = promptCompiler.compile(
                stack,
                promptCompiler.isolate(stack, teachingInstructions(action), "quantitative arithmetic")
        );
        ToolSession session = new BudgetedToolSession(
                new CalculatorToolSession(calculatorAvailable),
                budgets.required(blackboard.flowId())
        );
        TeachingContextView view = new TeachingContextView(blackboard.flowId(), action, blackboard.stage());
        TeachingResultEnvelope candidate = teachingModel.teach(action, view, stack, prompt, tools, session);
        GateResult<TeachingResultEnvelope> gated = executor.execute(
                candidate,
                teachingPolicy,
                GateContext.empty(),
                (ignored, violations) -> teachingModel.teach(action, view, stack, withViolations(prompt, violations), tools, session)
        );
        if (gated.outcome() != GateOutcome.PASSED) {
            String details = gated.violations().stream()
                    .map(violation -> violation.code() + ": " + violation.message())
                    .reduce((left, right) -> left + "; " + right)
                    .orElse(gated.outcome().name());
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "NodeExecutionFailed: " + details);
        }
        TeachingResultEnvelope accepted = gated.artifact();
        UUID taskPackageId = action == TeachingAction.APPLY ? UUID.randomUUID() : blackboard.taskPackageArtifactId();
        UUID attemptId = action == TeachingAction.APPLY ? UUID.randomUUID() : blackboard.openAttemptId();
        Map<String, Object> fields = new HashMap<>();
        fields.put("visibleContent", accepted.learnerVisibleContent());
        fields.put("status", FlowStatus.AWAITING_LEARNER_INPUT);
        fields.put("allowedEventKinds", accepted.allowedEventKinds());
        fields.put("explanationDelivered", true);
        fields.put("acceptedAction", action);
        fields.put("interactionVersion", blackboard.interactionVersion() + 1);
        fields.put("pendingInput", null);
        fields.put("lastRoute", action.name().toLowerCase());
        if (action == TeachingAction.APPLY) {
            fields.put("openAttemptId", attemptId);
            fields.put("taskPackageArtifactId", taskPackageId);
        }
        BlackboardDelta delta = new BlackboardDelta("teaching", fields);
        LearningBlackboard updated = applier.apply(blackboard, delta);
        Map<String, Object> packagePayload = null;
        if (action == TeachingAction.APPLY) {
            packagePayload = new LinkedHashMap<>(accepted.privateArtifacts());
            packagePayload.put("learnerTask", accepted.learnerVisibleContent());
            packagePayload.put("sourceTrace", accepted.sourceTrace());
        }
        CommitEffects effects = new CommitEffects(
                updated.flowId(), updated.visibleContent(), updated.allowedEventKinds(), updated.status(),
                updated.stage(), updated.interactionVersion(), packagePayload, taskPackageId, attemptId,
                null, null,
                Map.of(
                        "node", action.name().toLowerCase(),
                        "skills", List.of(stack.actionSkill().id()),
                        "hiddenReasoning", accepted.hiddenReasoning(),
                        "rawCandidate", "never-persisted"
                ),
                publicTracePayload(updated.flowId(), Map.of(
                        "route", action.name(),
                        "skills", Stream.concat(
                                Stream.of(stack.actionSkill().id()),
                                stack.capabilitySkills().stream().map(SkillManifest::id)
                        ).toList(),
                        "budget", budgetTrace(updated.flowId()),
                        "validation", "PASSED"
                )),
                delta
        );
        buffer.put(updated.flowId(), effects);
        return new AuthorizedNodeResult(updated, delta, effects, "ingest");
    }

    private static String teachingInstructions(TeachingAction action) {
        if (action == TeachingAction.APPLY) {
            return "Give one practice question: a quantity grows from 80 to 100. Call the calculator tool with old=80 and new=100. Put the tool result in privateArtifacts.answerKey as a string, and put a short taskRubric for percent change from 80 to 100. Do not include the numeric answer in learnerVisibleContent. allowedEventKinds must be ANSWER_SUBMITTED and HINT_REQUESTED.";
        }
        return "Teach the percent-change formula (new - old) / old × 100 with a short worked example. Do not ask the learner to submit a scored answer yet. allowedEventKinds must be CONTINUE_REQUESTED and CLARIFICATION_ASKED.";
    }

    private void chargeNode(LearningBlackboard blackboard) {
        try {
            budgets.required(blackboard.flowId()).enterNode();
        } catch (GraphRunBudgetExhausted exhausted) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "graph run budget exhausted");
        }
    }

    private String budgetTrace(UUID flowId) {
        return budgets.required(flowId).trace();
    }

    private Map<String, Object> publicTracePayload(UUID flowId, Map<String, Object> base) {
        Map<String, Object> payload = new LinkedHashMap<>(base);
        List<ModelCallObservation> calls = observations.drain(flowId);
        if (!calls.isEmpty()) {
            payload.put("models", calls.stream().map(ModelCallObservation::identity).toList());
            payload.put("usage", calls.stream().map(ModelCallObservation::usage).toList());
        }
        return Map.copyOf(payload);
    }

    private String withViolations(String prompt, List<GateViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            return prompt;
        }
        String details = violations.stream()
                .map(violation -> violation.code() + ": " + violation.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
        return prompt + "\nRepair the previous artifact. Violations: " + details;
    }

    private GuardSnapshot snapshot(LearningBlackboard blackboard) {
        return new GuardSnapshot(
                blackboard.status(),
                blackboard.stage(),
                blackboard.openAttemptId() != null,
                blackboard.openAttemptId() == null ? null : AttemptPurpose.PRACTICE,
                blackboard.explanationDelivered(),
                blackboard.pendingInput()
        );
    }

    private CommitEffects interactionEffects(LearningBlackboard blackboard, String message) {
        return new CommitEffects(
                blackboard.flowId(), message, blackboard.allowedEventKinds(), blackboard.status(),
                blackboard.stage(), blackboard.interactionVersion(), null, null, null, null, null,
                Map.of("node", "ingest"), Map.of("route", "safe-no-op"), new BlackboardDelta("input", Map.of())
        );
    }

    public LearnerVisibleInteraction visible(LearningBlackboard blackboard) {
        return new LearnerVisibleInteraction(
                blackboard.flowId(), blackboard.status(), blackboard.stage(), blackboard.interactionVersion(),
                blackboard.visibleContent(), blackboard.allowedEventKinds()
        );
    }

    public PendingCommitBuffer buffer() {
        return buffer;
    }

    private List<SkillManifest> defaultRegistry() {
        List<SkillManifest> manifests = new ArrayList<>();
        manifests.add(new SkillManifest(
                "explain.direct", 1, SkillSlot.ACTION, TeachingAction.EXPLAIN, Set.of(), Set.of(),
                List.of(), List.of(), Set.of(), true, 10
        ));
        manifests.add(new SkillManifest(
                "apply.worked-example", 1, SkillSlot.ACTION, TeachingAction.APPLY, Set.of(), Set.of("worked-example"),
                List.of(), List.of(), Set.of("calculator@1"), true, 10
        ));
        manifests.add(new SkillManifest(
                "capability.quantitative", 1, SkillSlot.REASONING, null, Set.of("quantitative"), Set.of(),
                List.of(), List.of(), Set.of("calculator@1"), false, 5
        ));
        return manifests;
    }
}
