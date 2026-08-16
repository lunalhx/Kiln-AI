package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public final class OpenAiCompatibleChatClientFactory implements ChatClientFactory {

    @Override
    public ChatClient create(ModelBindingSnapshot binding, String apiKey, int maxTokens) {
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
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(binding.modelId())
                        .maxTokens(maxTokens)
                        .build())
                .build();
        return ChatClient.create(model);
    }
}
