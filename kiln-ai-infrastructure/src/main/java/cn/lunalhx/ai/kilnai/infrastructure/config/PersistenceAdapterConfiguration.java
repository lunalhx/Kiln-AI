package cn.lunalhx.ai.kilnai.infrastructure.config;

import cn.lunalhx.ai.kilnai.domain.content.adapter.port.ConceptRepository;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearnerConceptProgressRepository;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningEventRepository;
import cn.lunalhx.ai.kilnai.domain.review.adapter.port.ReviewTaskRepository;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.ConceptMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.LearnerConceptProgressMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.LearningEventMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.MyBatisConceptRepository;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.MyBatisLearnerConceptProgressRepository;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.MyBatisLearningEventRepository;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.MyBatisReviewTaskRepository;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.ReviewTaskMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("cn.lunalhx.ai.kilnai.infrastructure.adapter.repository")
public class PersistenceAdapterConfiguration {

    @Bean
    ConceptRepository conceptRepository(ConceptMapper mapper) {
        return new MyBatisConceptRepository(mapper);
    }

    @Bean
    LearnerConceptProgressRepository learnerConceptProgressRepository(LearnerConceptProgressMapper mapper) {
        return new MyBatisLearnerConceptProgressRepository(mapper);
    }

    @Bean
    LearningEventRepository learningEventRepository(LearningEventMapper mapper) {
        return new MyBatisLearningEventRepository(mapper);
    }

    @Bean
    ReviewTaskRepository reviewTaskRepository(ReviewTaskMapper mapper) {
        return new MyBatisReviewTaskRepository(mapper);
    }
}
