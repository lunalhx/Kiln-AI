package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.api.dto.SpikeFlowResponse;
import cn.lunalhx.ai.kilnai.api.dto.SpikeTraceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class SpikeLearnerHttpTest {

    @Autowired
    TestRestTemplate http;

    @Test
    void learnerHttpFlowCompletesAndProtectsPrivateFields() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();
        SpikeFlowResponse started = start(learnerId, startKey);
        assertEquals("AWAITING_LEARNER_INPUT", started.status());
        assertTrue(started.visibleContent().contains("Percent change"));
        assertFalse(started.visibleContent().contains("25"));
        assertEquals(started.flowId(), start(learnerId, startKey).flowId());

        ResponseEntity<Map> illegal = eventRaw(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), "ANSWER_SUBMITTED", "25"
        );
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, illegal.getStatusCode());

        UUID continueKey = UUID.randomUUID();
        SpikeFlowResponse practice = event(started, continueKey, "CONTINUE_REQUESTED", null);
        assertTrue(practice.visibleContent().contains("80 to 100"));
        assertFalse(practice.visibleContent().contains("answerKey"));
        assertEquals(practice.interactionVersion(), event(started, continueKey, "CONTINUE_REQUESTED", null).interactionVersion());

        ResponseEntity<Map> stale = eventRaw(
                practice.flowId(), UUID.randomUUID(), started.interactionVersion(), "CONTINUE_REQUESTED", null
        );
        assertEquals(HttpStatus.CONFLICT, stale.getStatusCode());

        SpikeFlowResponse terminal = event(practice, UUID.randomUUID(), "ANSWER_SUBMITTED", "25");
        assertEquals("TERMINAL", terminal.status());
        assertTrue(terminal.visibleContent().contains("LEARNING"));
        assertFalse(terminal.visibleContent().contains("answerKey"));
        assertFalse(terminal.visibleContent().contains("hiddenReasoning"));

        SpikeTraceResponse trace = http.getForObject("/api/spike/flows/" + terminal.flowId() + "/trace", SpikeTraceResponse.class);
        assertNotNull(trace);
        assertTrue(trace.selectedSkills().contains("apply.worked-example@1"));
        assertFalse(trace.toString().contains("answerKey"));
        assertFalse(trace.toString().contains("hiddenReasoning"));
    }

    private SpikeFlowResponse start(UUID learnerId, UUID idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        ResponseEntity<SpikeFlowResponse> response = http.exchange(
                "/api/spike/flows",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("learnerId", learnerId, "fixtureId", "percent-change-v1"), headers),
                SpikeFlowResponse.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private SpikeFlowResponse event(SpikeFlowResponse current, UUID idempotencyKey, String kind, String text) {
        ResponseEntity<SpikeFlowResponse> response = http.exchange(
                "/api/spike/flows/" + current.flowId() + "/events",
                HttpMethod.POST,
                eventEntity(idempotencyKey, current.interactionVersion(), kind, text),
                SpikeFlowResponse.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<Map> eventRaw(UUID flowId, UUID idempotencyKey, int version, String kind, String text) {
        return http.exchange(
                "/api/spike/flows/" + flowId + "/events",
                HttpMethod.POST,
                eventEntity(idempotencyKey, version, kind, text),
                Map.class
        );
    }

    private HttpEntity<Map<String, Object>> eventEntity(UUID idempotencyKey, int version, String kind, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        java.util.HashMap<String, Object> body = new java.util.HashMap<>();
        body.put("interactionVersion", version);
        body.put("kind", kind);
        if (text != null) {
            body.put("text", text);
        }
        return new HttpEntity<>(body, headers);
    }
}
