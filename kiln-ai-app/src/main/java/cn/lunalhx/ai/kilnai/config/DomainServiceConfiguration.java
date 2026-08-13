package cn.lunalhx.ai.kilnai.config;

import cn.lunalhx.ai.kilnai.domain.content.adapter.port.ConceptRepository;
import cn.lunalhx.ai.kilnai.domain.content.service.ConceptService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DomainServiceConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnBean(ConceptRepository.class)
    ConceptService conceptService(ConceptRepository conceptRepository, Clock clock) {
        return new ConceptService(conceptRepository, clock);
    }
}
