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
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.domain.pedagogy.model.valobj.TeachingAction;
import cn.lunalhx.ai.kilnai.domain.skill.SkillStack;
import cn.lunalhx.ai.kilnai.domain.tool.ToolHandle;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        this(store, clients, observations, secrets, artifactMapper());
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
                .system(jsonInstructions(type))
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
            return parseArtifact(content, type, contextView);
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
                                providerToolName(handle.qualifiedId()),
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

    static String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int newline = trimmed.indexOf('\n');
            int fence = trimmed.lastIndexOf("```");
            if (newline > 0 && fence > newline) {
                trimmed = trimmed.substring(newline + 1, fence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private <T> T parseArtifact(String content, Class<T> type, Object contextView) throws JsonProcessingException {
        JsonNode node = json.readTree(extractJson(content));
        if (!node.isObject()) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "provider returned empty content");
        }
        ObjectNode obj = (ObjectNode) node;
        if (type == TeachingResultEnvelope.class) {
            coerceStringArray(obj, "sourceTrace");
            coerceEnumNames(obj, "allowedEventKinds", LearnerInputKind.class);
            coerceObject(obj, "privateArtifacts");
            coerceVisibleText(obj, "learnerVisibleContent");
            defaultText(obj, "hiddenReasoning");
            if (contextView instanceof TeachingContextView teaching && teaching.action() != null) {
                obj.put("action", teaching.action().name());
            }
        } else if (type == PedagogyPlan.class) {
            coerceStringArray(obj, "requiredCapabilityTags");
            coerceStringArray(obj, "preferredStrategyTags");
            defaultText(obj, "feedbackSummary");
            defaultText(obj, "teachingIntent");
            defaultText(obj, "reasonCode");
        } else if (type == EvidenceCandidate.class) {
            coerceStringArray(obj, "satisfiedCriteria");
            coerceStringArray(obj, "missingCriteria");
            defaultText(obj, "rationale");
        }
        T parsed = json.treeToValue(obj, type);
        if (parsed instanceof TeachingResultEnvelope envelope) {
            @SuppressWarnings("unchecked")
            T normalized = (T) normalizeTeaching(
                    envelope,
                    contextView instanceof TeachingContextView teaching ? teaching.action() : envelope.action()
            );
            return normalized;
        }
        return parsed;
    }

    private void coerceStringArray(ObjectNode obj, String field) {
        JsonNode value = obj.get(field);
        ArrayNode arr = json.createArrayNode();
        if (value == null || value.isNull()) {
            obj.set(field, arr);
            return;
        }
        if (value.isArray()) {
            value.forEach(item -> {
                if (item.isTextual()) {
                    arr.add(item.asText());
                } else if (!item.isNull()) {
                    arr.add(item.toString());
                }
            });
            obj.set(field, arr);
            return;
        }
        if (value.isTextual()) {
            arr.add(value.asText());
        } else {
            arr.add(value.toString());
        }
        obj.set(field, arr);
    }

    private void coerceEnumNames(ObjectNode obj, String field, Class<? extends Enum<?>> enumType) {
        coerceStringArray(obj, field);
        ArrayNode arr = json.createArrayNode();
        JsonNode value = obj.get(field);
        if (value != null && value.isArray()) {
            value.forEach(item -> {
                if (!item.isTextual()) {
                    return;
                }
                String raw = item.asText().trim();
                for (Enum<?> constant : enumType.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(raw)) {
                        arr.add(constant.name());
                        return;
                    }
                }
            });
        }
        obj.set(field, arr);
    }

    private static TeachingResultEnvelope normalizeTeaching(TeachingResultEnvelope envelope, TeachingAction expected) {
        TeachingAction action = expected == null ? envelope.action() : expected;
        Set<LearnerInputKind> legal = action == TeachingAction.APPLY
                ? Set.of(
                LearnerInputKind.ANSWER_SUBMITTED,
                LearnerInputKind.HINT_REQUESTED,
                LearnerInputKind.CLARIFICATION_ASKED,
                LearnerInputKind.FLOW_CONTROL_REQUESTED
        )
                : Set.of(
                LearnerInputKind.CONTINUE_REQUESTED,
                LearnerInputKind.CLARIFICATION_ASKED,
                LearnerInputKind.FLOW_CONTROL_REQUESTED
        );
        List<LearnerInputKind> filtered = new ArrayList<>();
        if (envelope.allowedEventKinds() != null) {
            envelope.allowedEventKinds().forEach(kind -> {
                if (legal.contains(kind) && !filtered.contains(kind)) {
                    filtered.add(kind);
                }
            });
        }
        if (filtered.isEmpty()) {
            if (action == TeachingAction.APPLY) {
                filtered.add(LearnerInputKind.ANSWER_SUBMITTED);
                filtered.add(LearnerInputKind.HINT_REQUESTED);
            } else {
                filtered.add(LearnerInputKind.CONTINUE_REQUESTED);
                filtered.add(LearnerInputKind.CLARIFICATION_ASKED);
            }
        }
        List<LearnerInputKind> kinds = List.copyOf(filtered);
        Map<String, Object> privateArtifacts = envelope.privateArtifacts() == null
                ? Map.of()
                : new LinkedHashMap<>(envelope.privateArtifacts());
        if (action != TeachingAction.APPLY) {
            privateArtifacts.remove("answerKey");
        }
        return new TeachingResultEnvelope(
                action,
                envelope.learnerVisibleContent(),
                Map.copyOf(privateArtifacts),
                envelope.sourceTrace(),
                kinds,
                envelope.hiddenReasoning()
        );
    }

    private void coerceObject(ObjectNode obj, String field) {
        JsonNode value = obj.get(field);
        if (value == null || value.isNull() || !value.isObject()) {
            obj.set(field, json.createObjectNode());
        }
    }

    private void coerceVisibleText(ObjectNode obj, String field) {
        JsonNode value = obj.get(field);
        if (value == null || value.isNull()) {
            obj.put(field, "");
            return;
        }
        if (value.isTextual()) {
            return;
        }
        if (value.isArray()) {
            StringBuilder joined = new StringBuilder();
            value.forEach(item -> {
                if (joined.length() > 0) {
                    joined.append('\n');
                }
                joined.append(item.isTextual() ? item.asText() : item.toString());
            });
            obj.put(field, joined.toString());
            return;
        }
        if (value.isObject() && value.hasNonNull("text") && value.get("text").isTextual()) {
            obj.put(field, value.get("text").asText());
            return;
        }
        obj.put(field, value.toString());
    }

    private void defaultText(ObjectNode obj, String field) {
        if (!obj.hasNonNull(field) || !obj.get(field).isTextual()) {
            obj.put(field, "");
        }
    }

    private static String jsonInstructions(Class<?> type) {
        if (type == PedagogyPlan.class) {
            return JSON_ONLY + """
                     Fields: feedbackSummary, nextAction, teachingIntent, requiredCapabilityTags, preferredStrategyTags, reasonCode.
                    nextAction must be one of EXPLAIN, RETRIEVE, APPLY, TEACH_BACK, HINT.
                    If the user prompt says explanation already delivered, nextAction must be APPLY.
                    """;
        }
        if (type == TeachingResultEnvelope.class) {
            return JSON_ONLY + """
                     Fields: action, learnerVisibleContent, privateArtifacts, sourceTrace, allowedEventKinds, hiddenReasoning.
                    action must be one of EXPLAIN, RETRIEVE, APPLY, TEACH_BACK, HINT.
                    If action is APPLY, privateArtifacts must include answerKey and taskRubric, and allowedEventKinds must be ANSWER_SUBMITTED and HINT_REQUESTED.
                    If action is EXPLAIN, allowedEventKinds must be CONTINUE_REQUESTED and CLARIFICATION_ASKED.
                    Do not put answerKey in learnerVisibleContent.
                    """;
        }
        if (type == EvidenceCandidate.class) {
            return JSON_ONLY + """
                     Fields: result, satisfiedCriteria, missingCriteria, rationale.
                    result must be one of PASS, PARTIAL, FAIL.
                    """;
        }
        return JSON_ONLY;
    }

    private static String providerToolName(String qualifiedId) {
        return qualifiedId.replace('@', '_');
    }

    private static ObjectMapper artifactMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }

    public static final class ToolInput extends LinkedHashMap<String, Object> {
    }
}
