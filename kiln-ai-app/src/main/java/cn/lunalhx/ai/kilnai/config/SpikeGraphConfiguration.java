package cn.lunalhx.ai.kilnai.config;

import cn.lunalhx.ai.kilnai.application.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedScenario;
import cn.lunalhx.ai.kilnai.application.fake.ScriptedTeachingModel;
import cn.lunalhx.ai.kilnai.application.graph.LearningGraphRuntimePort;
import cn.lunalhx.ai.kilnai.application.kernel.LearningNodeKernel;
import cn.lunalhx.ai.kilnai.application.kernel.PendingCommitBuffer;
import cn.lunalhx.ai.kilnai.application.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.application.kernel.PendingCommandHolder;
import cn.lunalhx.ai.kilnai.application.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.application.usecase.LearningFlowUseCase;
import cn.lunalhx.ai.kilnai.graph.saa.ApplicationCheckpointSaver;
import cn.lunalhx.ai.kilnai.graph.saa.LearningBlackboardMapper;
import cn.lunalhx.ai.kilnai.graph.saa.LearningStateGraphFactory;
import cn.lunalhx.ai.kilnai.graph.saa.SpringAiAlibabaGraphRuntime;
import cn.lunalhx.ai.kilnai.application.store.InMemorySpikeStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

@Configuration
public class SpikeGraphConfiguration {

    @Bean
    ScriptedScenario scriptedScenario() {
        return ScriptedScenario.HAPPY;
    }

    @Bean
    PendingCommitBuffer pendingCommitBuffer() {
        return new PendingCommitBuffer();
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
    ScriptedPedagogyModel scriptedPedagogyModel(ScriptedScenario scenario) {
        return new ScriptedPedagogyModel(scenario);
    }

    @Bean
    LearningNodeKernel learningNodeKernel(
            PendingCommitBuffer buffer,
            ScriptedPedagogyModel pedagogyModel,
            SpikeStorePort store,
            ScriptedScenario scenario,
            Clock clock
    ) {
        return new LearningNodeKernel(
                buffer, pedagogyModel, new ScriptedTeachingModel(scenario),
                new ScriptedAssessmentModel(), store, scenario, true, clock
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
            LearningStateGraphFactory factory
    ) {
        return new SpringAiAlibabaGraphRuntime(store, events, mapper, factory);
    }

    @Bean
    LearningFlowUseCase learningFlowUseCase(
            LearningGraphRuntimePort runtime,
            SpikeStorePort store,
            PendingCommandHolder pendingCommands,
            Clock clock
    ) {
        return new LearningFlowUseCase(runtime, store, pendingCommands, clock);
    }
}
