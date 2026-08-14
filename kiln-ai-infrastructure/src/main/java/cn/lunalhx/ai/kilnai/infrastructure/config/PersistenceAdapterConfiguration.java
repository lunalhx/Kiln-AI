package cn.lunalhx.ai.kilnai.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnBean(DataSource.class)
@MapperScan("cn.lunalhx.ai.kilnai.infrastructure.adapter.repository")
public class PersistenceAdapterConfiguration {
}
