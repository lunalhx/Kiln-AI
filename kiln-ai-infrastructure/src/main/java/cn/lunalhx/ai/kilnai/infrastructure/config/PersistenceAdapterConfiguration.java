package cn.lunalhx.ai.kilnai.infrastructure.config;

import cn.lunalhx.ai.kilnai.domain.content.adapter.port.ConceptRepository;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.ConceptMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.MyBatisConceptRepository;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnBean(DataSource.class)
@MapperScan("cn.lunalhx.ai.kilnai.infrastructure.adapter.repository")
public class PersistenceAdapterConfiguration {

    @Bean
    ConceptRepository conceptRepository(ConceptMapper mapper) {
        return new MyBatisConceptRepository(mapper);
    }
}
