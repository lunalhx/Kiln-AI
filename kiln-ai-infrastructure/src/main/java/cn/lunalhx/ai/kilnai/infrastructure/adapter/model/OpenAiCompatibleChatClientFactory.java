package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;

public final class OpenAiCompatibleChatClientFactory implements ChatClientFactory {

    static final ResponseFormat JSON_OBJECT = ResponseFormat.builder()
            .type(ResponseFormat.Type.JSON_OBJECT)
            .build();

    @Override
    public ChatClient create(ModelBindingSnapshot binding, String apiKey, int maxTokens, double temperature) {
        if (!OperatorCatalog.OPENAI_COMPATIBLE.equals(binding.protocol())) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "unsupported protocol: " + binding.protocol());
        }
        if (maxTokens <= 0) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "output token ceiling is not configured");
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(binding.endpoint())
                .apiKey(apiKey)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(requestOptions(binding.modelId(), maxTokens, temperature))
                .build();
        return ChatClient.create(model);
    }

    static OpenAiChatOptions requestOptions(String modelId, int maxTokens, double temperature) {
        return OpenAiChatOptions.builder()
                .model(modelId)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .responseFormat(JSON_OBJECT)
                .build();
    }
}
