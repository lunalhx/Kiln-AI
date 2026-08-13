package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.artifact.EvidenceCandidate;
import cn.lunalhx.ai.kilnai.domain.artifact.PedagogyPlan;
import cn.lunalhx.ai.kilnai.domain.artifact.TeachingResultEnvelope;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.AssessmentModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.PedagogyModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.TeachingModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ToolSession;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.ModelCallObservationHolder;
import cn.lunalhx.ai.kilnai.domain.learning.model.AssessmentContextView;
import cn.lunalhx.ai.kilnai.domain.learning.model.FrozenModelProfile;
import cn.lunalhx.ai.kilnai.domain.learning.model.ModelBindingSnapshot;
import cn.lunalhx.ai.kilnai.domain.learning.model.ModelCallObservation;
import cn.lunalhx.ai.kilnai.domain.learning.model.ModelSlot;
import cn.lunalhx.ai.kilnai.domain.learning.model.PedagogyContextView;
import cn.lunalhx.ai.kilnai.domain.learning.model.TeachingContextView;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import cn.lunalhx.ai.kilnai.domain.skill.SkillStack;
import cn.lunalhx.ai.kilnai.domain.tool.ToolHandle;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class SpringAiModelAdapter implements TeachingModelPort, PedagogyModelPort, AssessmentModelPort {

    private static final String JSON_ONLY = "Return only a JSON object for the requested artifact.";

    private final SpikeStorePort store;
    private final ChatClientFactory clients;
    private final ModelCallObservationHolder observations;
    private final Function<String, String> secrets;
    private final ObjectMapper json;

    public SpringAiModelAdapter(
            SpikeStorePort store,
            ChatClientFactory clients,
            ModelCallObservationHolder observations,
            Function<String, String> secrets
    ) {
        this(store, clients, observations, secrets, new ObjectMapper());
    }

    public SpringAiModelAdapter(
            SpikeStorePort store,
            ChatClientFactory clients,
            ModelCallObservationHolder observations,
            Function<String, String> secrets,
            ObjectMapper json
    ) {
        this.store = store;
        this.clients = clients;
        this.observations = observations;
        this.secrets = secrets;
        this.json = json;
    }

    @Override
    public PedagogyPlan propose(PedagogyContextView context, String compiledPrompt) {
        return complete(ModelSlot.SMALL, context.flowId(), compiledPrompt, context, List.of(), null, PedagogyPlan.class);
    }

    @Override
    public TeachingResultEnvelope teach(
            TeachingAction action,
            TeachingContextView context,
            SkillStack stack,
            String compiledPrompt,
            List<ToolHandle> tools,
            ToolSession toolSession
    ) {
        return complete(ModelSlot.STRONG, context.flowId(), compiledPrompt, context, tools, toolSession, TeachingResultEnvelope.class);
    }

    @Override
    public EvidenceCandidate assess(AssessmentContextView context, String compiledPrompt) {
        return complete(ModelSlot.STRONG, context.flowId(), compiledPrompt, context, List.of(), null, EvidenceCandidate.class);
    }

    private <T> T complete(
            ModelSlot slot,
            UUID flowId,
            String compiledPrompt,
            Object contextView,
            List<ToolHandle> tools,
            ToolSession session,
            Class<T> type
    ) {
        ModelBindingSnapshot binding = binding(flowId, slot);
        String apiKey = secrets.apply(binding.secretEnvVar());
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "provider secret is missing");
        }
        ChatClient client = clients.create(binding, apiKey);
        ChatClient.ChatClientRequestSpec spec = client.prompt()
                .system(JSON_ONLY)
                .user(compiledPrompt + "\n" + writeJson(contextView));
        List<ToolCallback> callbacks = callbacks(tools, session);
        if (!callbacks.isEmpty()) {
            spec = spec.toolCallbacks(callbacks);
        }
        long started = System.nanoTime();
        try {
            ChatResponse response = spec.call().chatResponse();
            record(flowId, slot, binding, response, started);
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? null
                    : response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "provider returned empty content");
            }
            return json.readValue(content, type);
        } catch (ApplicationException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw providerFailure(exception);
        } catch (RuntimeException exception) {
            throw providerFailure(exception);
        }
    }

    private ModelBindingSnapshot binding(UUID flowId, ModelSlot slot) {
        FrozenModelProfile profile = store.findFlow(flowId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FLOW_NOT_FOUND, "flow not found"))
                .frozenProfile();
        return slot == ModelSlot.SMALL ? profile.small() : profile.strong();
    }

    private List<ToolCallback> callbacks(List<ToolHandle> tools, ToolSession session) {
        if (tools == null || tools.isEmpty() || session == null) {
            return List.of();
        }
        return tools.stream()
                .map(handle -> FunctionToolCallback.<ToolInput, Map<String, Object>>builder(
                                handle.qualifiedId(),
                                input -> session.call(handle.qualifiedId(), input == null ? Map.of() : input)
                        )
                        .description("Authorized tool " + handle.qualifiedId())
                        .inputSchema(handle.inputSchema())
                        .inputType(ToolInput.class)
                        .build())
                .map(ToolCallback.class::cast)
                .toList();
    }

    private void record(
            UUID flowId,
            ModelSlot slot,
            ModelBindingSnapshot binding,
            ChatResponse response,
            long startedNanos
    ) {
        Usage usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
        observations.add(new ModelCallObservation(
                flowId,
                slot,
                binding.protocol(),
                binding.endpoint(),
                binding.providerId(),
                binding.modelId(),
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                (System.nanoTime() - startedNanos) / 1_000_000
        ));
    }

    private String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw providerFailure(exception);
        }
    }

    private static ApplicationException providerFailure(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ApplicationException application) {
                return application;
            }
            current = current.getCause();
        }
        ApplicationException wrapped = new ApplicationException(
                ErrorCode.SERVICE_UNAVAILABLE,
                "provider call failed: " + exception.getMessage()
        );
        wrapped.initCause(exception);
        return wrapped;
    }

    public static final class ToolInput extends LinkedHashMap<String, Object> {
    }
}
