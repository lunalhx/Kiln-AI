package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.ModelBindingSnapshot;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;

import java.util.List;

public final class OpenAiCompatibleChatClientFactory implements ChatClientFactory {

    @Override
    public ChatClient create(ModelBindingSnapshot binding, String apiKey) {
        if (!OperatorCatalog.OPENAI_COMPATIBLE.equals(binding.protocol())) {
            throw new ApplicationException(ErrorCode.INVALID_ARGUMENT, "unsupported protocol: " + binding.protocol());
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(binding.endpoint())
                .apiKey(apiKey)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(binding.modelId()).build())
                .toolCallingManager(toolCallingManager())
                .build();
        return ChatClient.create(model);
    }

    static ToolCallingManager toolCallingManager() {
        return DefaultToolCallingManager.builder()
                .toolExecutionExceptionProcessor(
                        DefaultToolExecutionExceptionProcessor.builder()
                                .alwaysThrow(true)
                                .rethrowExceptions(List.of(ApplicationException.class))
                                .build()
                )
                .build();
    }
}

