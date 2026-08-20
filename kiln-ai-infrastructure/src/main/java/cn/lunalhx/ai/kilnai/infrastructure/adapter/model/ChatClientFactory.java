package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import org.springframework.ai.chat.client.ChatClient;

public interface ChatClientFactory {

    ChatClient create(ModelBindingSnapshot binding, String apiKey, int maxTokens, double temperature);
}
