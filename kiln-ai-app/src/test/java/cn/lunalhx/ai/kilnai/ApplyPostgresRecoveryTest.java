package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.flow.ApplyFlowUseCase;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowInteraction;
import cn.lunalhx.ai.kilnai.domain.apply.model.ApplyFlowResult;
import cn.lunalhx.ai.kilnai.domain.apply.port.ArtifactStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.LearningFlowStore;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.AcceptedLearningEvidence;
import cn.lunalhx.ai.kilnai.domain.learning.model.entity.ReviewTask;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptPurpose;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.AttemptStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.FlowStatus;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.LearningResult;
import cn.lunalhx.ai.kilnai.domain.learning.model.valobj.ReviewTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Autowired
    ReviewTaskStore reviewStore;

    @Test
    void anIndependentPassAtomicallySchedulesOneReviewOneDue24HoursLater() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();

        ApplyFlowResult.Boundary started = (ApplyFlowResult.Boundary) useCase.start(learnerId, startKey);
        ApplyFlowResult.Boundary transitioned = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 1, UUID.randomUUID(), started.interaction().attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        ApplyFlowResult.Boundary completed = (ApplyFlowResult.Boundary) useCase.submit(
                started.interaction().flowId(), 2, UUID.randomUUID(), transitioned.interaction().attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);

        AcceptedLearningEvidence evidence = flowStore.allEvidence().stream()
                .filter(item -> item.learnerId().equals(learnerId))
                .findFirst().orElseThrow();
        List<ReviewTask> reviews = reviewStore.unfinishedReviewsFor(learnerId);
        assertEquals(1, reviews.size(), "the independent pass must schedule exactly one Review");
        ReviewTask review = reviews.get(0);
        assertEquals(ReviewTaskStatus.SCHEDULED, review.status());
        assertEquals(1, review.reviewNumber());
        assertEquals(evidence.acceptedAt().plus(Duration.ofHours(24)), review.dueAt(),
                "Review 1 must be due exactly 24 hours after the actual acceptance time");
        assertEquals(completed.interaction().flowId(), review.flowId());
        assertEquals(learnerId, review.learnerId());
    }

    @Test
    void theDatabaseEnforcesAtMostOneUnfinishedReviewPerLearnerAndConcept() {
        UUID learnerId = UUID.randomUUID();
        UUID flowId = UUID.randomUUID();
        UUID conceptId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-15T00:00:00Z");

        reviewStore.acceptEvidenceAndScheduleFirstReview(
                evidence(learnerId, conceptId, flowId, now.minusSeconds(3600)), now.plus(Duration.ofHours(24)));
        reviewStore.acceptEvidenceAndScheduleFirstReview(
                evidence(learnerId, conceptId, flowId, now), now.plus(Duration.ofHours(25)));

        assertEquals(1, reviewStore.unfinishedReviewsFor(learnerId).size(),
                "the scheduler cancels stale work so at most one unfinished Review survives");
        assertThrows(DuplicateKeyException.class, () -> reviewStore.acceptEvidenceAndScheduleFirstReview(
                evidence(learnerId, conceptId, flowId, now.plusSeconds(1)), now.plus(Duration.ofHours(26))),
                "the partial unique index must reject a second unfinished Review for the same learner and Concept");
    }

    private AcceptedLearningEvidence evidence(UUID learnerId, UUID conceptId, UUID flowId, Instant acceptedAt) {
        return new AcceptedLearningEvidence(
                UUID.randomUUID(), UUID.randomUUID(), flowId, conceptId, learnerId,
                LearningResult.PASS, AttemptPurpose.INDEPENDENT_TEST, 0, List.of(), acceptedAt);
    }

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
