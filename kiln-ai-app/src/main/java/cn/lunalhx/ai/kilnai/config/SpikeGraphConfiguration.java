package cn.lunalhx.ai.kilnai.config;

import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.AssessmentModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.LearningGraphRuntimePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.ModelProfilePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.PedagogyModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.TeachingModelPort;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.GraphRunBudgetHolder;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.LearningNodeKernel;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.ModelCallObservationHolder;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommandHolder;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommitBuffer;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.domain.learning.service.LearningFlowUseCase;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.ApplicationCheckpointSaver;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.LearningBlackboardMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.LearningStateGraphFactory;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.SpringAiAlibabaGraphRuntime;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.CatalogModelProfilePort;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.ChatClientFactory;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OpenAiCompatibleChatClientFactory;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorCatalog;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorCatalogProperties;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.SpringAiModelAdapter;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.repository.InMemorySpikeStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(OperatorCatalogProperties.class)
public class SpikeGraphConfiguration {

    @Bean
    PendingCommitBuffer pendingCommitBuffer() {
        return new PendingCommitBuffer();
    }

    @Bean
    GraphRunBudgetHolder graphRunBudgetHolder() {
        return new GraphRunBudgetHolder();
    }

    @Bean
    ModelCallObservationHolder modelCallObservationHolder() {
        return new ModelCallObservationHolder();
    }

    @Bean
    PendingLearnerEventHolder pendingLearnerEventHolder() {
        return new PendingLearnerEventHolder();
    }

    @Bean
    LearningBlackboardMapper learningBlackboardMapper() {
        return new LearningBlackboardMapper();
    }

    @Bean
    PendingCommandHolder pendingCommandHolder() {
        return new PendingCommandHolder();
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    SpikeStorePort spikeStorePort(PendingCommandHolder pendingCommands) {
        return new InMemorySpikeStore(pendingCommands);
    }

    @Bean
    OperatorCatalog operatorCatalog(OperatorCatalogProperties properties) {
        return properties.toCatalog();
    }

    @Bean
    ChatClientFactory chatClientFactory() {
        return new OpenAiCompatibleChatClientFactory();
    }

    @Bean
    SpringAiModelAdapter springAiModelAdapter(
            SpikeStorePort store,
            ChatClientFactory chatClientFactory,
            ModelCallObservationHolder observations,
            Environment environment
    ) {
        return new SpringAiModelAdapter(store, chatClientFactory, observations, environment::getProperty);
    }

    @Bean
    ModelProfilePort modelProfilePort(OperatorCatalog catalog, Environment environment) {
        return new CatalogModelProfilePort(catalog, environment::getProperty);
    }

    @Bean
    LearningNodeKernel learningNodeKernel(
            PendingCommitBuffer buffer,
            GraphRunBudgetHolder budgets,
            PedagogyModelPort pedagogyModel,
            TeachingModelPort teachingModel,
            AssessmentModelPort assessmentModel,
            SpikeStorePort store,
            ModelCallObservationHolder observations,
            Clock clock
    ) {
        return new LearningNodeKernel(
                buffer, budgets, pedagogyModel, teachingModel, assessmentModel, store, true, clock, observations
        );
    }

    @Bean
    ApplicationCheckpointSaver applicationCheckpointSaver(
            SpikeStorePort store,
            PendingCommitBuffer buffer,
            LearningBlackboardMapper mapper,
            Clock clock
    ) {
        return new ApplicationCheckpointSaver(store, buffer, mapper, clock);
    }

    @Bean
    LearningStateGraphFactory learningStateGraphFactory(
            LearningNodeKernel kernel,
            PendingLearnerEventHolder events,
            LearningBlackboardMapper mapper,
            ApplicationCheckpointSaver saver
    ) {
        return new LearningStateGraphFactory(kernel, events, mapper, saver);
    }

    @Bean
    LearningGraphRuntimePort learningGraphRuntimePort(
            SpikeStorePort store,
            PendingLearnerEventHolder events,
            LearningBlackboardMapper mapper,
            LearningStateGraphFactory factory,
            GraphRunBudgetHolder budgets,
            OperatorCatalog catalog
    ) {
        return new SpringAiAlibabaGraphRuntime(store, events, mapper, factory, budgets, catalog::requiredToolBudget);
    }

    @Bean
    LearningFlowUseCase learningFlowUseCase(
            LearningGraphRuntimePort runtime,
            SpikeStorePort store,
            ModelProfilePort modelProfiles,
            PendingCommandHolder pendingCommands,
            Clock clock
    ) {
        return new LearningFlowUseCase(runtime, store, modelProfiles, pendingCommands, clock);
    }
}
