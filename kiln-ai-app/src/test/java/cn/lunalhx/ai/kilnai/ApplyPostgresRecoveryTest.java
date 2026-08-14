package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.flow.ApplyFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(ScriptedApplyPortsConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplyPostgresRecoveryTest {

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
    ApplyFlowUseCase useCase;

    @Autowired
    ArtifactStore artifacts;

    @Autowired
    LearningFlowStore flowStore;

    @Test
    void aRestartedFlowResumesAnOpenAttemptAndAReplayedKeyReturnsTheOriginalResult() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();

        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, startKey);
        assertEquals(FlowStatus.AWAITING_LEARNER_INPUT, started.interaction().status());
        assertEquals(AttemptStatus.OPEN,
                artifacts.findAttempt(started.interaction().attemptId()).orElseThrow().status());

        UUID submitKey = UUID.randomUUID();
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(2, transitioned.interaction().interactionVersion());
        assertEquals(2, artifacts.allPackages().size());

        ApplyFlowResult.Boundary replayed = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 1, submitKey, started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(transitioned.interaction(), replayed.interaction(),
                "a replayed key must return the original result");

        UUID independentKey = UUID.randomUUID();
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 2, independentKey, transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(FlowStatus.TERMINAL, completed.interaction().status());
        assertEquals(1, flowStore.allEvidence().size());
        assertTrue(flowStore.evidenceExists(transitioned.interaction().attemptId()));
        assertTrue(artifacts.assessmentsFor(transitioned.interaction().attemptId()).size() >= 1,
                "the isolated assessment record must be persisted");
        assertTrue(artifacts.verificationsFor(artifacts.findAttempt(started.interaction().attemptId())
                .orElseThrow().taskPackageId()).size() >= 1,
                "the pre-delivery Task Verification record must be persisted");

        ApplyFlowInteraction recoveredQuery = useCase.query(started.interaction().flowId());
        assertEquals(completed.interaction(), recoveredQuery,
                "query must recover the exact terminal interaction after commit");

        ApplyFlowResult.Boundary replayedIndependent = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 2, independentKey, transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals(completed.interaction(), replayedIndependent.interaction());
        assertEquals(1, flowStore.allEvidence().size(),
                "a replayed evidence-accepting key must never create a second Evidence");
    }
}
