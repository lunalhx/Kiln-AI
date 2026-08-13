package cn.lunalhx.ai.kilnai.infrastructure.adapter.model;

import cn.lunalhx.ai.kilnai.domain.learning.model.ModelBindingSnapshot;
import org.springframework.ai.chat.client.ChatClient;

public interface ChatClientFactory {

    ChatClient create(ModelBindingSnapshot binding, String apiKey);
}
