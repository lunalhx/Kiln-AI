package cn.lunalhx.ai.kilnai.application.kernel;

import cn.lunalhx.ai.kilnai.application.fake.CalculatorToolSession;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedScenario;
import cn.lunalhx.ai.kilnai.application.graph.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.application.port.AssessmentModelPort;
import cn.lunalhx.ai.kilnai.application.port.PedagogyModelPort;
import cn.lunalhx.ai.kilnai.application.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.application.port.TeachingModelPort;
import cn.lunalhx.ai.kilnai.application.port.ToolSession;
import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;
import cn.lunalhx.ai.kilnai.domain.artifact.PedagogyPlan;
import cn.lunalhx.ai.kilnai.domain.artifact.TeachingResultEnvelope;
import cn.lunalhx.ai.kilnai.domain.blackboard.BlackboardDelta;
import cn.lunalhx.ai.kilnai.domain.blackboard.LearningBlackboard;
import cn.lunalhx.ai.kilnai.domain.gate.EvidenceCandidateGatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.GateContext;
import cn.lunalhx.ai.kilnai.domain.gate.GateOutcome;
import cn.lunalhx.ai.kilnai.domain.gate.GateResult;
import cn.lunalhx.ai.kilnai.domain.gate.PedagogyPlanGatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.TeachingResultGatePolicy;
import cn.lunalhx.ai.kilnai.domain.gate.TypedArtifactGatePipeline;
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
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;
import cn.lunalhx.ai.kilnai.domain.skill.PromptCompiler;
import cn.lunalhx.ai.kilnai.domain.skill.SkillManifest;
import cn.lunalhx.ai.kilnai.domain.skill.SkillResolver;
import cn.lunalhx.ai.kilnai.domain.skill.SkillSlot;
import cn.lunalhx.ai.kilnai.domain.skill.SkillStack;
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

    public static final int ORDINARY_CALL_LIMIT = 4;

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
    private final PedagogyModelPort pedagogyModel;
    private final TeachingModelPort teachingModel;
    private final AssessmentModelPort assessmentModel;
    private final SpikeStorePort store;
    private final ScriptedScenario scenario;
    private final boolean calculatorAvailable;
    private final Clock clock;
    private final List<SkillManifest> registry;

    public LearningNodeKernel(
            PendingCommitBuffer buffer,
            PedagogyModelPort pedagogyModel,
            TeachingModelPort teachingModel,
            AssessmentModelPort assessmentModel,
            SpikeStorePort store,
            ScriptedScenario scenario,
            boolean calculatorAvailable,
            Clock clock
    ) {
        this.buffer = buffer;
        this.pedagogyModel = pedagogyModel;
        this.teachingModel = teachingModel;
        this.assessmentModel = assessmentModel;
        this.store = store;
        this.scenario = scenario;
        this.calculatorAvailable = calculatorAvailable;
        this.clock = clock;
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
        chargeModel(blackboard);
        PedagogyPlan candidate = pedagogyModel.propose(blackboard);
        GateContext context = new GateContext(Set.copyOf(blackboard.legalCandidates().stream().map(Enum::name).toList()));
        GateResult<PedagogyPlan> result = executor.execute(candidate, pedagogyPolicy, context, ignored -> candidate);
        TeachingAction action = result.outcome() == GateOutcome.PASSED ? result.artifact().nextAction() : guard.fallbackAction();
        BlackboardDelta delta = new BlackboardDelta("pedagogy", Map.of(
                "acceptedAction", action,
                "modelCallCount", blackboard.modelCallCount() + 1,
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
        chargeModel(blackboard);
        Map<String, Object> taskPackage = blackboard.taskPackageArtifactId() == null
                ? Map.of()
                : store.artifact(blackboard.taskPackageArtifactId()).orElse(Map.of());
        Map<String, Object> isolatedPackage = new LinkedHashMap<>(taskPackage);
        isolatedPackage.remove("hiddenReasoning");
        EvidenceCandidate candidate = assessmentModel.assess(blackboard, isolatedPackage, answer, List.of());
        GateResult<EvidenceCandidate> gated = executor.execute(candidate, evidencePolicy, GateContext.empty(), ignored -> candidate);
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
        fields.put("modelCallCount", blackboard.modelCallCount() + 1);
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
                Map.of("route", "assess", "skills", List.of(), "budget", updated.modelCallCount() + " calls"),
                delta
        );
        buffer.put(updated.flowId(), effects);
        return new AuthorizedNodeResult(updated, delta, effects, "end");
    }

    private AuthorizedNodeResult teach(LearningBlackboard blackboard, TeachingAction action) {
        if (budgetExhausted(blackboard)) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "graph run budget exhausted");
        }
        if (scenario == ScriptedScenario.CAPABILITY_GAP && action == TeachingAction.APPLY) {
            throw new CapabilityGap("required tool unavailable: calculator@1");
        }
        SkillStack stack = skillResolver.resolve(
                action,
                action == TeachingAction.APPLY ? Set.of("quantitative") : Set.of(),
                action == TeachingAction.APPLY ? Set.of("worked-example") : Set.of(),
                registry
        );
        Set<String> toolsNeeded = new java.util.HashSet<>(stack.actionSkill().requiredTools());
        stack.capabilitySkills().forEach(skill -> toolsNeeded.addAll(skill.requiredTools()));
        Set<ToolHandle> runtime = calculatorAvailable
                ? Set.of(new ToolHandle("calculator", 1, "{\"type\":\"object\"}"))
                : Set.of();
        List<ToolHandle> tools = toolResolver.resolve(
                new ToolPermissionSet(action == TeachingAction.APPLY ? Set.of("calculator@1") : Set.of()),
                toolsNeeded,
                runtime,
                true
        );
        String prompt = promptCompiler.compile(
                stack,
                promptCompiler.isolate(stack, action.name() + " instructions", "quantitative arithmetic")
        );
        ToolSession session = new CalculatorToolSession(calculatorAvailable);
        TeachingResultEnvelope candidate = teachingModel.teach(action, blackboard, stack, prompt, tools, session);
        GateResult<TeachingResultEnvelope> gated = executor.execute(
                candidate,
                teachingPolicy,
                GateContext.empty(),
                ignored -> teachingModel.teach(action, blackboard, stack, prompt, tools, session)
        );
        if (gated.outcome() != GateOutcome.PASSED) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "NodeExecutionFailed");
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
        fields.put("modelCallCount", blackboard.modelCallCount() + 1);
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
                Map.of(
                        "route", action.name(),
                        "skills", Stream.concat(
                                Stream.of(stack.actionSkill().id()),
                                stack.capabilitySkills().stream().map(SkillManifest::id)
                        ).toList(),
                        "budget", updated.modelCallCount() + " calls",
                        "validation", "PASSED"
                ),
                delta
        );
        buffer.put(updated.flowId(), effects);
        return new AuthorizedNodeResult(updated, delta, effects, "ingest");
    }

    private void chargeModel(LearningBlackboard blackboard) {
        if (budgetExhausted(blackboard)) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "graph run budget exhausted");
        }
    }

    private boolean budgetExhausted(LearningBlackboard blackboard) {
        return blackboard.modelCallCount() >= ORDINARY_CALL_LIMIT
                || (scenario == ScriptedScenario.BUDGET_EXHAUSTION && blackboard.explanationDelivered());
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
