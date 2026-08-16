package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.ReferenceBundles;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ApplyScriptData;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedApplyGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedClarificationClassifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedExplainGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedHintGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedResponseVerificationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackGenerationModel;
import cn.lunalhx.ai.kilnai.domain.apply.fake.ScriptedTeachBackTaskVerifier;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ExplainApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.PracticeApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.TeachBackApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.HintFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelExecution;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.model.PrivateAssessorProjection;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskPackage;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyPromptCompiler;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ticket 11 contract: when the operator starts a Learning Flow, the Flow
 * freezes the operator-owned Model Profile (Strong/Small bindings plus the
 * output-token ceiling), every model call receives that frozen profile, and
 * the artifact execution traces record the frozen model runtime — the Strong
 * and Small identities, the operator output ceiling, the 16,000-character
 * instruction cap, and the per-node repair count actually used. The gates
 * enforce the recorded trace against the frozen profile, so a delivered
 * artifact is evidence that the node ran on the frozen configuration.
 */
class FrozenModelRuntimeContractTest {

    private static final UUID LEARNER_ID = UUID.randomUUID();

    @Test
    void theFlowFreezesTheModelProfileAndTheArtifactTraceRecordsTheFrozenRuntime() {
        Harness harness = new Harness(new ScriptedApplyGenerationModel(List.of(
                ApplyScriptData.taskReadyJson("设 p(x) = 3x²，求 p'(x)。", "6*x"))));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase()
                .start(LEARNER_ID, UUID.randomUUID());
        ApplyFlowInteraction interaction = started.interaction();

        LearningFlowStore.FlowRecord flow = harness.flowStore()
                .findFlow(interaction.flowId()).orElseThrow();
        assertEquals(ScriptedModelProfile.PROFILE, flow.modelProfile(),
                "starting a Flow must freeze the operator Model Profile onto the Flow");

        TaskPackage package_ = harness.artifacts().allPackages().get(0);
        PrivateAssessorProjection.ExecutionTrace trace = package_.privateAssessorProjection().executionTrace();
        ModelExecution model = trace.model();
        assertNotNull(model, "the execution trace must record the frozen model runtime");
        assertEquals("acme/scripted-strong", model.strongModel(),
                "the Apply generation node must record the Strong slot it used");
        assertEquals("acme/scripted-small", model.smallModel(),
                "the trace must record the Small slot frozen on the Flow");
        assertEquals(2048, model.outputTokenCeiling(),
                "the operator-owned output ceiling must be recorded in the execution trace");
        assertEquals(ApplyPromptCompiler.INSTRUCTION_BUDGET, model.instructionCap(),
                "the 16,000-character instruction cap must be recorded in the execution trace");
        assertEquals(0, model.repairCount(),
                "an accepted first candidate records zero repairs");
        assertTrue(model.usesFrozenProfile(ScriptedModelProfile.PROFILE),
                "the recorded trace must match the Flow-frozen profile");
    }

    @Test
    void aRepairedCandidateRecordsExactlyOneAllowedRepair() {
        // The first draft is invalid ("{}" is not a closed apply_generation/v1
        // contract), so the executor uses its single allowed same-plan repair
        // and the accepted second candidate must record repairCount = 1.
        Harness harness = new Harness(new ScriptedApplyGenerationModel(List.of(
                "{}",
                ApplyScriptData.taskReadyJson("设 p(x) = 3x²，求 p'(x)。", "6*x"))));
        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) harness.useCase()
                .start(LEARNER_ID, UUID.randomUUID());

        TaskPackage package_ = harness.artifacts().allPackages().get(0);
        assertEquals(1, package_.privateAssessorProjection().executionTrace().model().repairCount(),
                "the per-node repair ceiling of one is enforced and recorded in the trace");
    }

    private static final class Harness {

        private final InMemoryArtifactStore artifacts = new InMemoryArtifactStore(Clock.systemUTC());
        private final InMemoryLearningFlowStore flowStore = new InMemoryLearningFlowStore(Clock.systemUTC(), artifacts);
        private final LearningFlowCommandUseCase useCase;

        Harness(ScriptedApplyGenerationModel generation) {
            ScriptedTaskVerifier verifier = new ScriptedTaskVerifier(List.of(ApplyScriptData.passVerdict()));
            ScriptedAssessmentModel assessment = new ScriptedAssessmentModel(List.of());
            ScriptedResponseVerificationModel verification = new ScriptedResponseVerificationModel(List.of());
            ReviewTaskScheduler scheduler = new ReviewTaskScheduler(flowStore);
            ApplyProfileExecutor executor = new ApplyProfileExecutor(
                    ReferenceBundles.stack(), generation, verifier, artifacts);
            DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                    executor, artifacts, flowStore, assessment, verification,
                    DiagnosticApplyFixture.diagnosticContext(),
                    IndependentApplyFixture.independentContext(), Clock.systemUTC());
            IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                    artifacts, flowStore, assessment, verification, scheduler, Clock.systemUTC());
            PracticeSubmissionFlow practiceFlow = new PracticeSubmissionFlow(
                    executor, artifacts, flowStore, assessment, verification,
                    PracticeApplyFixture.practiceContext(),
                    IndependentApplyFixture.independentContext(), Clock.systemUTC());
            ExplainFlow explainFlow = new ExplainFlow(
                    new ExplainProfileExecutor(ReferenceBundles.explainStack(),
                            new ScriptedExplainGenerationModel(List.of())),
                    artifacts, flowStore, ExplainApplyFixture.explainContext());
            HintFlow hintFlow = new HintFlow(
                    new ScriptedHintGenerationModel(List.of()),
                    artifacts, PracticeApplyFixture.practiceContext().conceptSourcePack());
            TeachBackFlow teachBackFlow = new TeachBackFlow(
                    new TeachBackProfileExecutor(
                            ReferenceBundles.teachBackStack(),
                            new ScriptedTeachBackGenerationModel(List.of()),
                            new ScriptedTeachBackTaskVerifier(List.of()),
                            artifacts),
                    artifacts, flowStore, new ScriptedTeachBackAssessmentModel(List.of()),
                    TeachBackApplyFixture.teachBackContext(), Clock.systemUTC());
            LearningStateGraph graph = new LearningStateGraph(
                    artifacts, flowStore, flowStore, diagnosticFlow, independentFlow, practiceFlow,
                    explainFlow, hintFlow, teachBackFlow,
                    new ScriptedPedagogyModel(), new ScriptedClarificationClassifier(),
                    Clock.systemUTC());
            useCase = new LearningFlowCommandUseCase(
                    artifacts, flowStore, graph, DiagnosticApplyFixture.diagnosticContext(),
                    (OperatorModelProfilePort) () -> ScriptedModelProfile.PROFILE,
                    Clock.systemUTC());
        }

        LearningFlowCommandUseCase useCase() {
            return useCase;
        }

        LearningFlowStore flowStore() {
            return flowStore;
        }

        ArtifactStore artifacts() {
            return artifacts;
        }
    }
}
