package cn.lunalhx.ai.kilnai.config;

import cn.lunalhx.ai.kilnai.domain.content.adapter.port.ConceptRepository;
import cn.lunalhx.ai.kilnai.domain.content.service.ConceptService;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearnerConceptProgressRepository;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningEventRepository;
import cn.lunalhx.ai.kilnai.domain.learning.service.LearningProgressService;
import cn.lunalhx.ai.kilnai.domain.pedagogy.service.LearningWorkflow;
import cn.lunalhx.ai.kilnai.domain.review.adapter.port.ReviewTaskRepository;
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
    ConceptService conceptService(ConceptRepository conceptRepository, Clock clock) {
        return new ConceptService(conceptRepository, clock);
    }

    @Bean
    LearningWorkflow learningWorkflow() {
        return new LearningWorkflow();
    }

    @Bean
    LearningProgressService learningProgressService(
            ConceptRepository conceptRepository,
            LearnerConceptProgressRepository progressRepository,
            LearningEventRepository learningEventRepository,
            ReviewTaskRepository reviewTaskRepository,
            LearningWorkflow learningWorkflow,
            Clock clock
    ) {
        return new LearningProgressService(
                conceptRepository, progressRepository, learningEventRepository,
                reviewTaskRepository, learningWorkflow, clock
        );
    }
}
