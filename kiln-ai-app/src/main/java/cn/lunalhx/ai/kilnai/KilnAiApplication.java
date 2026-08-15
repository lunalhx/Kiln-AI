package cn.lunalhx.ai.kilnai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@EnableScheduling
@SpringBootApplication(excludeName = {
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
        "org.springframework.ai.model.chat.memory.autoconfigure.ChatMemoryAutoConfiguration"
})
public class KilnAiApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(KilnAiApplication.class);
        Map<String, Object> local = DotEnv.read();
        if (!local.isEmpty()) {
            application.setDefaultProperties(local);
        }
        application.run(args);
    }
}
