package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPort;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorModelProfileAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
        "kiln.catalog.enabled=true",
        "kiln.catalog.strong=test/test-model",
        "kiln.catalog.small=test/test-model",
        "kiln.catalog.output-token-ceiling=1024",
        "kiln.catalog.providers[0].provider-id=test",
        "kiln.catalog.providers[0].protocol=openai-compatible",
        "kiln.catalog.providers[0].endpoint=https://example.invalid/v1",
        "kiln.catalog.providers[0].secret-env-var=OPENAI_API_KEY",
        "kiln.catalog.providers[0].models[0]=test-model",
        "OPENAI_API_KEY=test-key"
})
class OperatorModelConfigurationTest {

    @Autowired
    OperatorModelProfilePort modelProfile;

    @Autowired
    ApplyGenerationPort generation;

    @Autowired
    PedagogyPort pedagogy;

    @Test
    void configuredCatalogRegistersTheRealModelPortsWithoutCallingTheProvider() {
        assertInstanceOf(OperatorModelProfileAdapter.class, modelProfile);
        assertEquals("test", modelProfile.resolve().strong().providerId());
        assertEquals("test-model", modelProfile.resolve().strong().modelId());
        assertNotNull(generation);
        assertNotNull(pedagogy);
    }
}
