package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.api.dto.LearningFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.ReviewTaskView;
import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Public HTTP coverage of the Inconclusive Review path over the unified
 * Learning Flow API: the submission closes the attempt and continues with the
 * verified replacement task and the system-uncertainty notice, never exposing
 * private projections; an unprepared replacement keeps the Review Started and
 * resumable through the same start endpoint, and the collection advertises
 * exactly that continuation state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(InconclusiveReviewGraphPortsConfiguration.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class LearningFlowReviewReplacementHttpTest {

    @Autowired
    TestRestTemplate http;

    @Autowired
    ReviewTaskStore reviewStore;

    @Autowired
    InconclusiveReviewGraphPortsConfiguration ports;

    @Test
    void anInconclusiveSubmissionContinuesWithTheVerifiedReplacementTaskOverHttp() {
        UUID learnerId = UUID.randomUUID();
        UUID flowId = completeIndependentPass(learnerId);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));
        ReviewTaskView due = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];

        LearningFlowResponse review = startReview(due.reviewId(), UUID.randomUUID());
        UUID submitKey = UUID.randomUUID();
        LearningFlowResponse replaced = submit(flowId, submitKey, review.interactionVersion(), review.attemptId(),
                "x^2^3", "x^2^3", null);

        assertEquals("task", replaced.kind(),
                "the Inconclusive Review continues with the verified replacement task");
        assertEquals("AWAITING_LEARNER_INPUT", replaced.status());
        assertEquals("DELAYED_REVIEW", replaced.stage());
        assertEquals("REVIEW", replaced.attemptPurpose());
        assertNotNull(replaced.attemptId());
        assertFalse(replaced.attemptId().equals(review.attemptId()),
                "the replacement must be a new Attempt");
        assertNotNull(replaced.task());
        assertEquals(ScriptedApplyPortsConfiguration.REVIEW_TASK_2, replaced.task().taskText(),
                "the replacement must be a fresh equivalent task");
        assertTrue(replaced.learnerMessage().contains("未能确定"),
                "the system-uncertainty notice must distinguish from failure");
        assertFalse(replaced.learnerMessage().contains("失败"));
        assertFalse(serialize(replaced).contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2));
        assertFalse(serialize(replaced).contains("fingerprint"));
        assertFalse(serialize(replaced).contains("assessment"));
        assertEquals("INDEPENDENT", replaced.progress().currentMilestone(),
                "an inconclusive submission must not change the milestone");

        LearningFlowResponse replayed = submit(flowId, submitKey, review.interactionVersion(), review.attemptId(),
                "x^2^3", "x^2^3", null);
        assertEquals(replaced, replayed, "a replayed key must return the original replacement");

        ReviewTaskView[] remaining = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals(1, remaining.length, "the cadence must not advance");
        assertEquals("STARTED", remaining[0].status());
        assertFalse(remaining[0].startable(),
                "a Started Review with an open replacement attempt is not startable again");

        ResponseEntity<String> raw = http.getForEntity("/api/review-tasks?learnerId=" + learnerId, String.class);
        assertFalse(raw.getBody().contains(learnerId.toString()), "the learner UUID must never leak");
        assertFalse(raw.getBody().contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2));
        assertFalse(raw.getBody().contains("fingerprint"));

        LearningFlowResponse completed = submit(flowId, UUID.randomUUID(), replaced.interactionVersion(),
                replaced.attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2, null);
        assertEquals("transition", completed.kind());
        assertEquals("TERMINAL", completed.status());
        assertEquals(2, http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0].reviewNumber(),
                "the completed Review must schedule its successor");
    }

    @Test
    void anUnpreparedReplacementStaysStartedAndTheCollectionAdvertisesTheContinuation() {
        UUID learnerId = UUID.randomUUID();
        UUID flowId = completeIndependentPass(learnerId);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));
        ReviewTaskView due = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];

        LearningFlowResponse review = startReview(due.reviewId(), UUID.randomUUID());
        ports.failNextReviewGeneration();
        LearningFlowResponse unavailable = submit(flowId, UUID.randomUUID(), review.interactionVersion(),
                review.attemptId(), "x^2^3", "x^2^3", null);

        assertEquals("unavailable", unavailable.kind(),
                "an unprepared replacement projects the unavailable union member");
        assertEquals("TERMINAL", unavailable.status());
        assertTrue(unavailable.learnerMessage().contains("未能确定"));
        assertTrue(unavailable.learnerMessage().contains("继续"));
        assertFalse(unavailable.learnerMessage().contains("失败"));

        ReviewTaskView[] resumed = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals("STARTED", resumed[0].status());
        assertTrue(resumed[0].startable(),
                "the resumable Started Review must advertise the continuation action");

        LearningFlowResponse continued = startReview(due.reviewId(), UUID.randomUUID());
        assertEquals("task", continued.kind());
        assertEquals("AWAITING_LEARNER_INPUT", continued.status());
        assertNotNull(continued.task());
        assertEquals(ScriptedApplyPortsConfiguration.REVIEW_TASK_2, continued.task().taskText());

        ReviewTaskView[] bound = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals("STARTED", bound[0].status());
        assertFalse(bound[0].startable(),
                "once resumed, the Review is bound to its open attempt again");

        LearningFlowResponse completed = submit(flowId, UUID.randomUUID(), continued.interactionVersion(),
                continued.attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2, null);
        assertEquals("transition", completed.kind());
        assertEquals("INDEPENDENT", completed.progress().currentMilestone());
        assertNull(completed.task());
    }

    private UUID completeIndependentPass(UUID learnerId) {
        LearningFlowResponse started = start(learnerId, UUID.randomUUID());
        UUID flowId = started.flowId();
        LearningFlowResponse transitioned = submit(flowId, UUID.randomUUID(), started.interactionVersion(),
                started.attemptId(), "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(flowId, UUID.randomUUID(), transitioned.interactionVersion(), transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        return flowId;
    }

    private LearningFlowResponse start(UUID learnerId, UUID idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        ResponseEntity<LearningFlowResponse> response = http.exchange(
                "/api/learning/flows",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("learnerId", learnerId), headers),
                LearningFlowResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private LearningFlowResponse startReview(UUID reviewId, UUID idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        ResponseEntity<LearningFlowResponse> response = http.exchange(
                "/api/review-tasks/" + reviewId + "/start",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                LearningFlowResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private LearningFlowResponse submit(
            UUID flowId, UUID idempotencyKey, int interactionVersion, UUID attemptId,
            String raw, String confirmed, String rationale
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("command", "answer_submitted");
        body.put("interactionVersion", interactionVersion);
        body.put("attemptId", attemptId);
        body.put("rawAnswer", raw);
        body.put("confirmedCanonical", confirmed);
        if (rationale != null) {
            body.put("rationale", rationale);
        }
        ResponseEntity<LearningFlowResponse> response = http.exchange(
                "/api/learning/flows/" + flowId + "/commands",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                LearningFlowResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private String serialize(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
