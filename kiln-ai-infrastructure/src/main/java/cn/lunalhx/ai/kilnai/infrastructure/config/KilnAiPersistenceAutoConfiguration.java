package cn.lunalhx.ai.kilnai.infrastructure.config;

import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.store.InMemoryLearningFlowStore;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.ApplyFlowMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.PostgresApplyFlowStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Selects the durable store implementations after the DataSource
 * auto-configuration has registered its bean definitions. {@code ConditionalOn*}
 * on plain user configurations is evaluated before auto-configuration beans
 * exist, so the Postgres store could never match; here the conditions see the
 * DataSource definition and choose exactly one path: Postgres when a
 * DataSource exists, otherwise the in-memory reference stores.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
public class KilnAiPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    PostgresApplyFlowStore postgresApplyFlowStore(ApplyFlowMapper mapper, ObjectMapper json, Clock clock) {
        return new PostgresApplyFlowStore(mapper, json, clock);
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    InMemoryLearningFlowStore inMemoryLearningFlowStore(InMemoryArtifactStore artifactStore, Clock clock) {
        return new InMemoryLearningFlowStore(clock, artifactStore);
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    InMemoryArtifactStore inMemoryArtifactStore(Clock clock) {
        return new InMemoryArtifactStore(clock);
    }
}
