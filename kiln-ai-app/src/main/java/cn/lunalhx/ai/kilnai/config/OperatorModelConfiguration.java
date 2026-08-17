package cn.lunalhx.ai.kilnai.config;

import cn.lunalhx.ai.kilnai.domain.apply.model.ResponseAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.model.TaskVerificationVerdict;
import cn.lunalhx.ai.kilnai.domain.apply.model.TeachBackAssessment;
import cn.lunalhx.ai.kilnai.domain.apply.port.ApplyGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.AssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ExplainGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.HintGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.OperatorModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.apply.port.ResponseVerificationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackAssessmentPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackGenerationPort;
import cn.lunalhx.ai.kilnai.domain.apply.port.TeachBackTaskVerifierPort;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassification;
import cn.lunalhx.ai.kilnai.domain.learning.graph.ClarificationClassifierPort;
import cn.lunalhx.ai.kilnai.domain.learning.pedagogy.PedagogyPort;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.ApplyModelAdapter;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OpenAiCompatibleChatClientFactory;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorCatalog;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorCatalogProperties;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorModelProfileAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Registers the real model ports only when the operator has supplied both
 * model slots. Missing configuration is handled by the fail-closed ports.
 */
@Configuration
@EnableConfigurationProperties(OperatorCatalogProperties.class)
@ConditionalOnProperty(prefix = "kiln.catalog", name = "enabled", havingValue = "true")
public class OperatorModelConfiguration {

    @Bean
    OperatorCatalog operatorCatalog(OperatorCatalogProperties properties) {
        return properties.toCatalog();
    }

    @Bean
    OperatorModelPorts operatorModelPorts(OperatorCatalog catalog, Environment environment) {
        return new OperatorModelPorts(
                new ApplyModelAdapter(catalog, new OpenAiCompatibleChatClientFactory(), environment::getProperty));
    }

    @Bean
    OperatorModelProfilePort operatorModelProfilePort(OperatorCatalog catalog, Environment environment) {
        return new OperatorModelProfileAdapter(catalog, environment::getProperty);
    }

    @Bean
    ApplyGenerationPort applyGenerationPort(OperatorModelPorts ports) {
        return ports.adapter()::generate;
    }

    @Bean
    TaskVerifierPort taskVerifierPort(OperatorModelPorts ports) {
        return (profile, pkg, ctx) -> TaskVerificationVerdict.parse(ports.adapter().verify(profile, pkg, ctx));
    }

    @Bean
    AssessmentPort assessmentPort(OperatorModelPorts ports) {
        return (profile, ctx) -> ResponseAssessment.parse(ports.adapter().assess(profile, ctx));
    }

    @Bean
    ResponseVerificationPort responseVerificationPort(OperatorModelPorts ports) {
        return (profile, ctx) -> ResponseAssessment.parse(ports.adapter().verifyResponse(profile, ctx));
    }

    @Bean
    ExplainGenerationPort explainGenerationPort(OperatorModelPorts ports) {
        return ports.adapter()::generate;
    }

    @Bean
    HintGenerationPort hintGenerationPort(OperatorModelPorts ports) {
        return ports.adapter()::generate;
    }

    @Bean
    TeachBackGenerationPort teachBackGenerationPort(OperatorModelPorts ports) {
        return ports.adapter()::generate;
    }

    @Bean
    TeachBackTaskVerifierPort teachBackTaskVerifierPort(OperatorModelPorts ports) {
        return (profile, pkg, ctx) -> TaskVerificationVerdict.parse(ports.adapter().verify(profile, pkg, ctx));
    }

    @Bean
    TeachBackAssessmentPort teachBackAssessmentPort(OperatorModelPorts ports) {
        return (profile, ctx) -> TeachBackAssessment.parse(ports.adapter().assess(profile, ctx));
    }

    @Bean
    PedagogyPort pedagogyPort(OperatorModelPorts ports) {
        return ports.adapter()::generatePlan;
    }

    @Bean
    ClarificationClassifierPort clarificationClassifierPort(OperatorModelPorts ports) {
        return (profile, message, taskText) -> ClarificationClassification.parse(
                ports.adapter().classify(profile, message, taskText));
    }

    record OperatorModelPorts(ApplyModelAdapter adapter) {
    }
}
