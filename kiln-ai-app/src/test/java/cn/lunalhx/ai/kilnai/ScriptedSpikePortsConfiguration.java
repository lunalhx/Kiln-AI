package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.AssessmentModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.PedagogyModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.TeachingModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedScenario;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedTeachingModel;
import cn.lunalhx.ai.kilnai.domain.learning.fake.SkippingModelProfilePort;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorCatalog;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration
public class ScriptedSpikePortsConfiguration {

    @Bean
    @Primary
    PedagogyModelPort scriptedPedagogyModelPort() {
        return new ScriptedPedagogyModel(ScriptedScenario.HAPPY);
    }

    @Bean
    @Primary
    TeachingModelPort scriptedTeachingModelPort() {
        return new ScriptedTeachingModel(ScriptedScenario.HAPPY);
    }

    @Bean
    @Primary
    AssessmentModelPort scriptedAssessmentModelPort() {
        return new ScriptedAssessmentModel();
    }

    @Bean
    @Primary
    ModelProfilePort scriptedModelProfilePort() {
        return new SkippingModelProfilePort();
    }

    @Bean
    @Primary
    OperatorCatalog scriptedOperatorCatalog() {
        return new OperatorCatalog(List.of(), null, null, 8);
    }
}
