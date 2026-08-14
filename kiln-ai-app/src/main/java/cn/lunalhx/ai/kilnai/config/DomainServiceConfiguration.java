package cn.lunalhx.ai.kilnai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DomainServiceConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
