package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedAssessmentModel;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedPedagogyModel;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedScenario;
import cn.lunalhx.ai.kilnai.domain.learning.fake.ScriptedTeachingModel;
import cn.lunalhx.ai.kilnai.domain.learning.fixture.SpikeFixture;
import cn.lunalhx.ai.kilnai.domain.learning.model.LearnerVisibleInteraction;
import cn.lunalhx.ai.kilnai.domain.learning.service.ResumeGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.service.StartGraphRun;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.LearningNodeKernel;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingCommitBuffer;
import cn.lunalhx.ai.kilnai.domain.learning.kernel.PendingLearnerEventHolder;
import cn.lunalhx.ai.kilnai.domain.learning.adapter.port.SpikeStorePort;
import cn.lunalhx.ai.kilnai.domain.learning.service.LearningFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearnerInputKind;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.ApplicationCheckpointSaver;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.LearningBlackboardMapper;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.LearningStateGraphFactory;
import cn.lunalhx.ai.kilnai.infrastructure.adapter.graph.SpringAiAlibabaGraphRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresCheckpointRecoveryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kiln_ai")
            .withUsername("kiln_ai")
            .withPassword("kiln_ai");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    LearningFlowUseCase useCase;

    @Autowired
    SpikeStorePort store;

    @Test
    void newGraphRuntimeResumesFromPostgresCheckpoint() {
        LearnerVisibleInteraction explained = useCase.start(new StartGraphRun(
                UUID.randomUUID(), SpikeFixture.PERCENT_CHANGE_V1, UUID.randomUUID(), null
        ));
        SpringAiAlibabaGraphRuntime replacement = newRuntime();
        LearnerVisibleInteraction practice = replacement.resume(new ResumeGraphRun(
                explained.flowId(), UUID.randomUUID(), explained.interactionVersion(),
                LearnerInputKind.CONTINUE_REQUESTED, null
        ));
        assertTrue(practice.visibleContent().contains("80 to 100"));
        SpringAiAlibabaGraphRuntime third = newRuntime();
        LearnerVisibleInteraction terminal = third.resume(new ResumeGraphRun(
                explained.flowId(), UUID.randomUUID(), practice.interactionVersion(),
                LearnerInputKind.ANSWER_SUBMITTED, "25"
        ));
        assertEquals("TERMINAL", terminal.status().name());
        assertTrue(store.evidenceExists(store.latest(explained.flowId()).orElseThrow().blackboard().openAttemptId()));
    }

    private SpringAiAlibabaGraphRuntime newRuntime() {
        PendingCommitBuffer buffer = new PendingCommitBuffer();
        PendingLearnerEventHolder events = new PendingLearnerEventHolder();
        LearningBlackboardMapper mapper = new LearningBlackboardMapper();
        LearningNodeKernel kernel = new LearningNodeKernel(
                buffer,
                new ScriptedPedagogyModel(ScriptedScenario.HAPPY),
                new ScriptedTeachingModel(ScriptedScenario.HAPPY),
                new ScriptedAssessmentModel(),
                store,
                ScriptedScenario.HAPPY,
                true,
                Clock.systemUTC()
        );
        ApplicationCheckpointSaver saver = new ApplicationCheckpointSaver(store, buffer, mapper, Clock.systemUTC());
        return new SpringAiAlibabaGraphRuntime(
                store, events, mapper, new LearningStateGraphFactory(kernel, events, mapper, saver)
        );
    }
}
