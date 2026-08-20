package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleChatClientFactoryTest {

    @Test
    void everyCallRequestsAJsonObjectAtTheSuppliedTemperature() {
        OpenAiChatOptions generation = OpenAiCompatibleChatClientFactory.requestOptions(
                "deepseek-v4-flash", 4096, 0.7);
        OpenAiChatOptions judgment = OpenAiCompatibleChatClientFactory.requestOptions(
                "deepseek-v4-flash", 4096, 0.2);

        assertEquals("deepseek-v4-flash", generation.getModel());
        assertEquals(4096, generation.getMaxTokens());
        assertEquals(0.7, generation.getTemperature());
        assertEquals(0.2, judgment.getTemperature());
        assertEquals(ResponseFormat.Type.JSON_OBJECT, generation.getResponseFormat().getType());
        assertEquals(OpenAiCompatibleChatClientFactory.JSON_OBJECT, generation.getResponseFormat());
    }

    @Test
    void unsupportedProtocolFailsClosed() {
        OpenAiCompatibleChatClientFactory factory = new OpenAiCompatibleChatClientFactory();
        ModelBindingSnapshot binding = new ModelBindingSnapshot(
                "dashscope", "https://api.acme.test/v1", "acme", "gpt-strong", "SECRET");

        ApplicationException error = assertThrows(ApplicationException.class,
                () -> factory.create(binding, "sk-test", 2048, 0.2));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("unsupported protocol"));
    }

    @Test
    void missingOutputCeilingFailsClosed() {
        OpenAiCompatibleChatClientFactory factory = new OpenAiCompatibleChatClientFactory();
        ModelBindingSnapshot binding = new ModelBindingSnapshot(
                OperatorCatalog.OPENAI_COMPATIBLE, "https://api.acme.test/v1",
                "acme", "gpt-strong", "SECRET");

        ApplicationException error = assertThrows(ApplicationException.class,
                () -> factory.create(binding, "sk-test", 0, 0.2));
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode());
        assertTrue(error.getMessage().contains("output token ceiling"));
    }
}
