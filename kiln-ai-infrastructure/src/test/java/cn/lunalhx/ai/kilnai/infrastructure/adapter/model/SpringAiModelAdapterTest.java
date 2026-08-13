package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.learning.fixture.SpikeFixture;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.GraphRunBudgetHolder;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.LearningNodeKernel;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.ModelCallObservationHolder;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommandHolder;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommitBuffer;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.model.ModelBindingSnapshot;
import cn.lunalhx.ai.kilnai.domain.learning.model.PublicTraceView;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.learning.service.LearningFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.service.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.service.StartGraphRun;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.ApplicationCheckpointSaver;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.LearningBlackboardMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.LearningStateGraphFactory;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.SpringAiAlibabaGraphRuntime;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.InMemorySpikeStore;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiModelAdapterTest {

    private static final String SECRET_ENV = "KILN_ADAPTER_TEST_KEY";
    private static final String SECRET = "sk-test-secret-must-not-leak";

    @Test
    void catalogResolvesStrongAndSmall() {
        OperatorCatalog catalog = catalog("acme/gpt-strong", "acme/gpt-small", 8);
        var profile = catalog.resolve(secrets());
        assertEquals("acme/gpt-strong", profile.strong().identity());
        assertEquals("acme/gpt-small", profile.small().identity());
        assertEquals("https://api.acme.test/v1", profile.strong().endpoint());
        assertEquals(SECRET_ENV, profile.strong().secretEnvVar());
        assertEquals(OperatorCatalog.OPENAI_COMPATIBLE, profile.strong().protocol());
    }

    @Test
    void missingCatalogFailsClosed() {
        OperatorCatalog catalog = new OperatorCatalog(List.of(), "acme/gpt-strong", "acme/gpt-small", 8);
        ApplicationException error = assertThrows(ApplicationException.class, () -> catalog.resolve(secrets()));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("catalog"));
    }

    @Test
    void incompleteProfileFailsClosed() {
        OperatorCatalog catalog = catalog("acme/gpt-strong", "", 8);
        ApplicationException error = assertThrows(ApplicationException.class, () -> catalog.resolve(secrets()));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("profile"));
    }

    @Test
    void missingSecretFailsClosed() {
        OperatorCatalog catalog = catalog("acme/gpt-strong", "acme/gpt-small", 8);
        ApplicationException error = assertThrows(
                ApplicationException.class,
                () -> catalog.resolve(name -> null)
        );
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("secret"));
    }

    @Test
    void unknownModelFailsClosed() {
        OperatorCatalog catalog = catalog("acme/missing", "acme/gpt-small", 8);
        ApplicationException error = assertThrows(ApplicationException.class, () -> catalog.resolve(secrets()));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("unknown model"));
    }

    @Test
    void missingToolBudgetFailsClosed() {
        OperatorCatalog catalog = catalog("acme/gpt-strong", "acme/gpt-small", null);
        ApplicationException error = assertThrows(ApplicationException.class, () -> catalog.resolve(secrets()));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("tool budget"));
    }

    @Test
    void startRejectedWhenCatalogMissingDoesNotTeach() {
        Fixture fixture = fixture(new OperatorCatalog(List.of(), "acme/gpt-strong", "acme/gpt-small", 8), false, 8);
        ApplicationException error = assertThrows(ApplicationException.class, () -> fixture.useCase.start(new StartGraphRun(
                UUID.randomUUID(), SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), null
        )));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(fixture.model.prompts.isEmpty());
        assertTrue(fixture.factory.bindings.isEmpty());
    }

    @Test
    void freezeIsUsedOnResumeAfterLiveCatalogEdit() {
        Fixture fixture = fixture(catalog("acme/gpt-strong", "acme/gpt-small", 8), false, 8);
        LearnerVisibleInteraction explained = fixture.useCase.start(new StartGraphRun(
                UUID.randomUUID(), SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), null
        ));
        fixture.catalog.replace(
                fixture.catalog.providers(),
                "acme/gpt-strong-b",
                "acme/gpt-small",
                8
        );
        LearnerVisibleInteraction practice = fixture.useCase.resume(new ResumeGraphRun(
                explained.flowId(), UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));
        assertTrue(practice.visibleContent().contains("80 to 100"));
        List<String> strongIds = fixture.factory.bindings.stream()
                .filter(binding -> "gpt-strong".equals(binding.modelId()) || "gpt-strong-b".equals(binding.modelId()))
                .map(ModelBindingSnapshot::modelId)
                .toList();
        assertFalse(strongIds.isEmpty());
        assertTrue(strongIds.stream().allMatch("gpt-strong"::equals));
        assertEquals(
                "acme/gpt-strong",
                fixture.store.findFlow(explained.flowId()).orElseThrow().frozenProfile().strong().identity()
        );
    }

    @Test
    void teachingRequestGetsAuthorizedToolsAndOtherNodesGetNone() {
        Fixture fixture = fixture(catalog("acme/gpt-strong", "acme/gpt-small", 8), false, 8);
        LearnerVisibleInteraction explained = fixture.useCase.start(new StartGraphRun(
                UUID.randomUUID(), SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), null
        ));
        LearnerVisibleInteraction practice = fixture.useCase.resume(new ResumeGraphRun(
                explained.flowId(), UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));
        fixture.useCase.resume(new ResumeGraphRun(
                practice.flowId(), UUID.randomUUID(), practice.interactionVersion(),
                LearnerInputKind.ANSWER_SUBMITTED, "25"
        ));

        List<List<String>> pedagogyTools = new ArrayList<>();
        List<List<String>> explainTools = new ArrayList<>();
        List<List<String>> applyTools = new ArrayList<>();
        List<List<String>> assessTools = new ArrayList<>();
        for (Prompt prompt : fixture.model.prompts) {
            String contents = prompt.getContents() == null ? "" : prompt.getContents();
            List<String> tools = ScriptedChatModel.toolNames(prompt);
            if (contents.contains("Legal actions:")) {
                pedagogyTools.add(tools);
            } else if (contents.contains("\"answer\"")) {
                assessTools.add(tools);
            } else if (contents.contains("\"action\":\"APPLY\"")) {
                applyTools.add(tools);
            } else if (contents.contains("\"action\":\"EXPLAIN\"")) {
                explainTools.add(tools);
            }
        }
        assertFalse(pedagogyTools.isEmpty());
        assertFalse(explainTools.isEmpty());
        assertFalse(applyTools.isEmpty());
        assertFalse(assessTools.isEmpty());
        assertTrue(pedagogyTools.stream().allMatch(List::isEmpty));
        assertTrue(explainTools.stream().allMatch(List::isEmpty));
        assertTrue(assessTools.stream().allMatch(List::isEmpty));
        assertTrue(applyTools.stream().allMatch(tools -> tools.equals(List.of("calculator_1"))));
    }

    @Test
    void twoCalculatorCallsWithToolCeilingOneStopsTheGraphRun() {
        Fixture fixture = fixture(catalog("acme/gpt-strong", "acme/gpt-small", 8), true, 1);
        LearnerVisibleInteraction explained = fixture.useCase.start(new StartGraphRun(
                UUID.randomUUID(), SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), null
        ));
        ApplicationException error = assertThrows(ApplicationException.class, () -> fixture.useCase.resume(new ResumeGraphRun(
                explained.flowId(), UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        )));
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, error.errorCode());
        assertEquals(explained.visibleContent(), fixture.store.latestInteraction(explained.flowId()).orElseThrow().visibleContent());
        assertTrue(error.getMessage().contains("tool budget"));
    }

    @Test
    void twoCalculatorCallsDoNotConsumeNodeBudget() {
        Fixture fixture = fixture(catalog("acme/gpt-strong", "acme/gpt-small", 8), true, 8);
        LearnerVisibleInteraction explained = fixture.useCase.start(new StartGraphRun(
                UUID.randomUUID(), SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), null
        ));
        LearnerVisibleInteraction practice = fixture.useCase.resume(new ResumeGraphRun(
                explained.flowId(), UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));
        assertTrue(practice.visibleContent().contains("80 to 100"));
        PublicTraceView trace = fixture.store.publicTrace(explained.flowId()).orElseThrow();
        assertTrue(trace.budget().contains("nodes="));
        assertTrue(trace.budget().contains("tools=2"));
    }

    @Test
    void publicTraceContainsFrozenIdentityAndUsageWithoutSecrets() {
        Fixture fixture = fixture(catalog("acme/gpt-strong", "acme/gpt-small", 8), false, 8);
        LearnerVisibleInteraction explained = fixture.useCase.start(new StartGraphRun(
                UUID.randomUUID(), SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), null
        ));
        PublicTraceView trace = fixture.store.publicTrace(explained.flowId()).orElseThrow();
        assertTrue(trace.models().contains("acme/gpt-strong"));
        assertFalse(trace.usage().isEmpty());
        assertTrue(trace.usage().getFirst().contains("promptTokens="));
        assertFalse(trace.toString().contains(SECRET));
        assertFalse(trace.models().toString().contains(SECRET_ENV));
        LearnerVisibleInteraction practice = fixture.useCase.resume(new ResumeGraphRun(
                explained.flowId(), UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));
        fixture.useCase.resume(new ResumeGraphRun(
                practice.flowId(), UUID.randomUUID(), practice.interactionVersion(),
                LearnerInputKind.ANSWER_SUBMITTED, "25"
        ));
        PublicTraceView finished = fixture.store.publicTrace(explained.flowId()).orElseThrow();
        assertTrue(finished.models().contains("acme/gpt-strong"));
        assertTrue(finished.models().contains("acme/gpt-small"));
        assertFalse(finished.toString().contains(SECRET));
    }

    private Fixture fixture(OperatorCatalog catalog, boolean twoCalculatorCalls, int toolLimit) {
        PendingCommandHolder commands = new PendingCommandHolder();
        InMemorySpikeStore store = new InMemorySpikeStore(commands);
        ScriptedChatModel model = new ScriptedChatModel(twoCalculatorCalls);
        RecordingChatClientFactory factory = new RecordingChatClientFactory(model);
        ModelCallObservationHolder observations = new ModelCallObservationHolder();
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(store, factory, observations, secrets());
        PendingCommitBuffer buffer = new PendingCommitBuffer();
        GraphRunBudgetHolder budgets = new GraphRunBudgetHolder();
        PendingLearnerEventHolder events = new PendingLearnerEventHolder();
        LearningBlackboardMapper mapper = new LearningBlackboardMapper();
        LearningNodeKernel kernel = new LearningNodeKernel(
                buffer, budgets, adapter, adapter, adapter, store, true, Clock.systemUTC(), observations
        );
        ApplicationCheckpointSaver saver = new ApplicationCheckpointSaver(store, buffer, mapper, Clock.systemUTC());
        SpringAiAlibabaGraphRuntime runtime = new SpringAiAlibabaGraphRuntime(
                store, events, mapper, new LearningStateGraphFactory(kernel, events, mapper, saver),
                budgets, toolLimit
        );
        LearningFlowUseCase useCase = new LearningFlowUseCase(
                runtime, store, new CatalogModelProfilePort(catalog, secrets()), commands, Clock.systemUTC()
        );
        return new Fixture(store, catalog, model, factory, useCase);
    }

    private static OperatorCatalog catalog(String strong, String small, Integer toolBudget) {
        return new OperatorCatalog(List.of(provider()), strong, small, toolBudget);
    }

    private static CatalogProvider provider() {
        return new CatalogProvider(
                "acme",
                OperatorCatalog.OPENAI_COMPATIBLE,
                "https://api.acme.test/v1",
                SECRET_ENV,
                List.of("gpt-strong", "gpt-small", "gpt-strong-b")
        );
    }

    private static Function<String, String> secrets() {
        return name -> SECRET_ENV.equals(name) ? SECRET : null;
    }

    private record Fixture(
            InMemorySpikeStore store,
            OperatorCatalog catalog,
            ScriptedChatModel model,
            RecordingChatClientFactory factory,
            LearningFlowUseCase useCase
    ) {
    }

    private static final class RecordingChatClientFactory implements ChatClientFactory {

        private final ChatModel model;
        private final List<ModelBindingSnapshot> bindings = new CopyOnWriteArrayList<>();

        private RecordingChatClientFactory(ChatModel model) {
            this.model = model;
        }

        @Override
        public ChatClient create(ModelBindingSnapshot binding, String apiKey) {
            bindings.add(binding);
            return ChatClient.create(model);
        }
    }

    private static final class ScriptedChatModel implements ChatModel {

        private static final String EXPLAIN = """
                {"action":"EXPLAIN","learnerVisibleContent":"Percent change is (new - old) / old × 100. For an increase, the value is positive.","privateArtifacts":{"hiddenReasoning":"do not expose"},"sourceTrace":["source-pack-percent-change"],"allowedEventKinds":["CONTINUE_REQUESTED","CLARIFICATION_ASKED"],"hiddenReasoning":"internal-explain-trace"}
                """;
        private static final String APPLY = """
                {"action":"APPLY","learnerVisibleContent":"A quantity grows from 80 to 100. What is the percent change?","privateArtifacts":{"answerKey":"25","taskRubric":"correct percent change from 80 to 100"},"sourceTrace":["source-pack-percent-change"],"allowedEventKinds":["ANSWER_SUBMITTED","HINT_REQUESTED"],"hiddenReasoning":"internal-apply-trace"}
                """;
        private static final String PEDAGOGY = """
                {"feedbackSummary":"Continue with a quantitative practice task.","nextAction":"APPLY","teachingIntent":"practice-percent-change","requiredCapabilityTags":["quantitative"],"preferredStrategyTags":["worked-example"],"reasonCode":"continue-after-explain"}
                """;

        private final boolean twoCalculatorCalls;
        private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
        private final ToolCallingManager toolCallingManager = OpenAiCompatibleChatClientFactory.toolCallingManager();

        private ScriptedChatModel(boolean twoCalculatorCalls) {
            this.twoCalculatorCalls = twoCalculatorCalls;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse response = generate(prompt);
            if (response.hasToolCalls() && prompt.getOptions() != null
                    && ToolCallingChatOptions.isInternalToolExecutionEnabled(prompt.getOptions())) {
                ToolExecutionResult execution = toolCallingManager.executeToolCalls(prompt, response);
                return call(new Prompt(execution.conversationHistory(), prompt.getOptions()));
            }
            return response;
        }

        private ChatResponse generate(Prompt prompt) {
            prompts.add(prompt);
            String contents = prompt.getContents() == null ? "" : prompt.getContents();
            if (contents.contains("\"answer\"")) {
                boolean pass = contents.contains("25");
                String json = pass
                        ? "{\"result\":\"PASS\",\"satisfiedCriteria\":[\"percent-change\"],\"missingCriteria\":[],\"rationale\":\"deterministic-fake\"}"
                        : "{\"result\":\"FAIL\",\"satisfiedCriteria\":[],\"missingCriteria\":[\"percent-change\"],\"rationale\":\"deterministic-fake\"}";
                return text(json);
            }
            if (contents.contains("Legal actions:")) {
                return text(PEDAGOGY);
            }
            if (contents.contains("\"action\":\"APPLY\"")) {
                int needed = twoCalculatorCalls ? 2 : 1;
                if (toolResponses(prompt) < needed) {
                    return toolCall();
                }
                return text(APPLY);
            }
            return text(EXPLAIN);
        }

        private ChatResponse toolCall() {
            AssistantMessage message = AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(new AssistantMessage.ToolCall(
                            "call-" + prompts.size(),
                            "function",
                            "calculator_1",
                            "{\"old\":80,\"new\":100}"
                    )))
                    .build();
            return ChatResponse.builder()
                    .generations(List.of(new Generation(message)))
                    .metadata(metadata())
                    .build();
        }

        private ChatResponse text(String json) {
            return ChatResponse.builder()
                    .generations(List.of(new Generation(new AssistantMessage(json))))
                    .metadata(metadata())
                    .build();
        }

        private ChatResponseMetadata metadata() {
            return ChatResponseMetadata.builder()
                    .model("scripted")
                    .usage(new DefaultUsage(10, 5))
                    .build();
        }

        private static int toolResponses(Prompt prompt) {
            int count = 0;
            for (Message message : prompt.getInstructions()) {
                if (message instanceof ToolResponseMessage toolMessage) {
                    count += toolMessage.getResponses().size();
                }
            }
            return count;
        }

        private static List<String> toolNames(Prompt prompt) {
            if (prompt.getOptions() instanceof ToolCallingChatOptions options && options.getToolCallbacks() != null) {
                return options.getToolCallbacks().stream()
                        .map(callback -> callback.getToolDefinition().name())
                        .toList();
            }
            return List.of();
        }
    }
}
