package cn.lunalhx.ai.kilnai;
import cn.lunalhx.ai.kilnai.domain.apply.model.ModelProfile;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.DiagnosticApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ExplainApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.IndependentApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.PracticeApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.ReviewApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.fixture.TeachBackApplyFixture;
import cn.lunalhx.ai.kilnai.domain.apply.flow.DiagnosticFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ExplainFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.HintFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.IndependentSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.PracticeSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.ReviewSubmissionFlow;
import cn.lunalhx.ai.kilnai.domain.apply.flow.TeachBackFlow;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearningFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.model.LearnerProjection;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExplainGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.HintGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackAssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackTaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfile;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ApplyProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.ExplainProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.profile.TeachBackProfileExecutor;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassification;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassifierPort;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningFlowCommandUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.graph.LearningStateGraph;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPort;
import cn.lunalhx.ai.kilnai.domain.learning.service.ReviewTaskScheduler;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle.BundleLoader;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle.SkillBundleSource;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.ApplyModelAdapter;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OpenAiCompatibleChatClientFactory;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorCatalog;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorCatalogProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real-model smoke test for the Learning Flow reference. It compiles the
 * real Apply five-Bundle prompt, resolves the operator-configured model from
 * {@code deploy/local/.env}, and runs a complete Diagnostic start through the
 * Learning Flow command surface against the provider with zero tools in
 * ephemeral in-memory storage. It is never a CI oracle: it runs only when
 * {@code KILN_LIVE_SMOKE=true}, it is non-blocking, and it creates no Learning
 * Evidence.
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = "KILN_LIVE_SMOKE", matches = "true")
class ApplyProfileLiveSmokeTest {

    @Test
    void theCompiledApplyPromptDeliversOrGracefullyDeclinesADiagnosticTask() {
        Map<String, Object> env = DotEnv.read();
        OperatorCatalog catalog = catalogFrom(env);
        Function<String, String> secrets = name -> {
            Object value = env.get(name);
            return value == null ? null : String.valueOf(value);
        };
        ApplyModelAdapter model = new ApplyModelAdapter(
                catalog, new OpenAiCompatibleChatClientFactory(), secrets);

        ArtifactStore artifacts = new InMemoryArtifactStore(Clock.systemUTC());
        LearningFlowStore flowStore = new InMemoryLearningFlowStore();
        ApplyProfileExecutor executor = new ApplyProfileExecutor(
                referenceStack(), model, model, artifacts);
        DiagnosticFlow diagnosticFlow = new DiagnosticFlow(
                executor, artifacts, flowStore, model, model,
                DiagnosticApplyFixture.diagnosticContext(),
                IndependentApplyFixture.independentContext(),
                Clock.systemUTC());
        IndependentSubmissionFlow independentFlow = new IndependentSubmissionFlow(
                artifacts, flowStore, model, model,
                new ReviewTaskScheduler((ReviewTaskStore) flowStore), Clock.systemUTC());
        PracticeSubmissionFlow practiceFlow = new PracticeSubmissionFlow(
                executor, artifacts, flowStore, model, model,
                PracticeApplyFixture.practiceContext(), IndependentApplyFixture.independentContext(),
                Clock.systemUTC());
        ExplainFlow explainFlow = new ExplainFlow(
                new ExplainProfileExecutor(RecoveryTestBundles.explainStack(), failClosedExplain()),
                artifacts, flowStore, ExplainApplyFixture.explainContext());
        HintFlow hintFlow = new HintFlow(
                failClosedHint(), artifacts,
                PracticeApplyFixture.practiceContext().conceptSourcePack());
        TeachBackFlow teachBackFlow = new TeachBackFlow(
                new TeachBackProfileExecutor(RecoveryTestBundles.teachBackStack(),
                        failClosedTeachBackGeneration(), failClosedTeachBackVerifier(), artifacts),
                artifacts, flowStore, failClosedTeachBackAssessment(),
                TeachBackApplyFixture.teachBackContext(), Clock.systemUTC());
        ReviewSubmissionFlow reviewFlow = new ReviewSubmissionFlow(
                artifacts, flowStore, model, model,
                new ReviewTaskScheduler((ReviewTaskStore) flowStore),
                executor, (ReviewTaskStore) flowStore, ReviewApplyFixture.reviewContext(), Clock.systemUTC());
        OperatorModelProfilePort profilePort = () -> new ModelProfile(
                new ModelProfile.ModelBinding("openai-compatible", "https://api.smoke.test/v1",
                        "smoke", "smoke-strong", "KILN_APPLY_SMOKE_STRONG"),
                new ModelProfile.ModelBinding("openai-compatible", "https://api.smoke.test/v1",
                        "smoke", "smoke-small", "KILN_APPLY_SMOKE_SMALL"),
                2048);
        LearningStateGraph graph = new LearningStateGraph(
                artifacts, flowStore, (ReviewTaskStore) flowStore, diagnosticFlow, independentFlow,
                practiceFlow, reviewFlow, explainFlow, hintFlow, teachBackFlow,
                failClosedPedagogy(), failClosedClassifier(), Clock.systemUTC());
        LearningFlowCommandUseCase useCase = new LearningFlowCommandUseCase(
                artifacts, flowStore, graph, DiagnosticApplyFixture.diagnosticContext(),
                profilePort, Clock.systemUTC());

        LearningFlowResult result = useCase.start(UUID.randomUUID(), UUID.randomUUID());

        assertInstanceOf(LearningFlowResult.Boundary.class, result);
        LearningFlowResult.Boundary boundary = (LearningFlowResult.Boundary) result;
        LearnerProjection projection = boundary.interaction().learnerProjection();
        if (projection != null) {
            assertEquals("zh-CN", projection.locale());
            assertFalse(projection.taskText().contains("12*x^2 - 6*x + 7"), "no expected answer");
            assertFalse(projection.taskText().contains("openstax"), "no source identities");
            assertFalse(projection.taskText().contains("sec-3.3"), "no source anchors");
            assertFalse(projection.taskText().contains("fingerprint"), "no fingerprints");
        } else {
            assertNotNull(boundary.interaction().learnerMessage(),
                    "a declined task must still surface the neutral message");
        }
        assertTrue(flowStore.allEvidence().isEmpty(), "a smoke start must never create evidence");
        assertNotNull(useCase.query(boundary.interaction().flowId()));
    }

    private static ExplainGenerationPort failClosedExplain() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new IllegalStateException("the smoke Diagnostic start must never run Explain");
        };
    }

    private static HintGenerationPort failClosedHint() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new IllegalStateException("the smoke Diagnostic start must never run Hint");
        };
    }

    private static TeachBackGenerationPort failClosedTeachBackGeneration() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new IllegalStateException("the smoke Diagnostic start must never run Teach-back");
        };
    }

    private static TeachBackTaskVerifierPort failClosedTeachBackVerifier() {
        return (profile, taskPackage, context) -> {
            throw new IllegalStateException("the smoke Diagnostic start must never verify a Teach-back task");
        };
    }

    private static TeachBackAssessmentPort failClosedTeachBackAssessment() {
        return (profile, context) -> {
            throw new IllegalStateException("the smoke Diagnostic start must never assess Teach-back");
        };
    }

    private static PedagogyPort failClosedPedagogy() {
        return (profile, compiledSystemPrompt, executionContextJson) -> {
            throw new IllegalStateException("the smoke Diagnostic start must never run the Pedagogy Agent");
        };
    }

    private static ClarificationClassifierPort failClosedClassifier() {
        return (profile, message, taskText) -> ClarificationClassification.SUBSTANTIVE;
    }

    private static OperatorCatalog catalogFrom(Map<String, Object> env) {
        OperatorCatalogProperties properties = new Binder(new MapConfigurationPropertySource(env))
                .bind("kiln.catalog", OperatorCatalogProperties.class)
                .get();
        return properties.toCatalog();
    }

    private static BundleStack referenceStack() {
        BundleLoader loader = new BundleLoader();
        return new BundleStack(ApplyProfile.FIXED_STACK.stream()
                .map(loader::load)
                .map(SkillBundleSource::toBundle)
                .toList());
    }
}
