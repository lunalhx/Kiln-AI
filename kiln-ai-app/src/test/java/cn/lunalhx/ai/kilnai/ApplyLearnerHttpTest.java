package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse;
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

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedApplyPortsConfiguration.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class ApplyLearnerHttpTest {

    @Autowired
    TestRestTemplate http;

    @Test
    void learnerHttpFlowStartsQueriesSubmitsAndProtectsPrivateProjections() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();

        ApplyFlowResponse started = start(learnerId, startKey);
        assertEquals("AWAITING_LEARNER_INPUT", started.status());
        assertEquals("DIAGNOSTIC", started.stage());
        assertEquals(1, started.interactionVersion());
        assertNotNull(started.attemptId());
        assertEquals("DIAGNOSTIC", started.attemptPurpose());
        assertNotNull(started.task());
        assertEquals("zh-CN", started.task().locale());
        assertEquals(ScriptedApplyPortsConfiguration.DIAGNOSTIC_TASK, started.task().taskText());
        assertFalse(started.task().taskText().contains(ScriptedApplyPortsConfiguration.DIAGNOSTIC_EXPECTED),
                "the expected answer must never reach the learner");
        assertFalse(started.task().taskText().contains("openstax"));
        assertFalse(started.task().taskText().contains("fingerprint"));

        ApplyFlowResponse replayedStart = start(learnerId, startKey);
        assertEquals(started.flowId(), replayedStart.flowId());
        assertEquals(started.interactionVersion(), replayedStart.interactionVersion());

        ApplyFlowResponse queried = http.getForObject(
                "/api/apply/flows/" + started.flowId(), ApplyFlowResponse.class);
        assertEquals(started, queried, "query must recover the exact latest interaction");

        ResponseEntity<ApplyFlowResponse> rejected = submitRaw(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7 + 1", null);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, rejected.getStatusCode(),
                "a confirmation mismatch must be a rejected submission");

        ResponseEntity<Map> stale = submitRawMap(
                started.flowId(), UUID.randomUUID(), started.interactionVersion() - 1, started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(HttpStatus.CONFLICT, stale.getStatusCode(),
                "a stale interaction version must conflict");

        UUID submitKey = UUID.randomUUID();
        ApplyFlowResponse transitioned = submit(
                started.flowId(), submitKey, started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals("AWAITING_LEARNER_INPUT", transitioned.status());
        assertEquals("INDEPENDENT_TEST", transitioned.stage());
        assertEquals(2, transitioned.interactionVersion());
        assertNotNull(transitioned.task());
        assertEquals(ScriptedApplyPortsConfiguration.INDEPENDENT_TASK, transitioned.task().taskText());
        assertFalse(transitioned.task().taskText().contains(ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED));

        ApplyFlowResponse replayedSubmit = submit(
                started.flowId(), submitKey, started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(transitioned, replayedSubmit,
                "a replayed idempotency key must return the original result");

        ApplyFlowResponse completed = submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals("TERMINAL", completed.status());
        assertEquals(3, completed.interactionVersion());
        assertNull(completed.task());
        assertTrue(completed.learnerMessage().contains("已完成"));
        assertFalse(completed.learnerMessage().contains("15*x^2 - 2"), "no answer facts in terminal message");
        assertFalse(completed.learnerMessage().contains("fingerprint"));
        assertFalse(completed.learnerMessage().contains("assessment"));
    }

    @Test
    void aDuplicateSubmissionWithANewKeyConflictsWithoutASecondEvaluation() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);

        ResponseEntity<Map> duplicate = submitRawMap(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode(),
                "a duplicate submission for the already closed Diagnostic attempt must conflict");

        ApplyFlowResponse again = http.getForObject("/api/apply/flows/" + started.flowId(), ApplyFlowResponse.class);
        assertEquals(transitioned.interactionVersion(), again.interactionVersion(),
                "the ignored submission must not advance the flow");
        assertEquals(transitioned.attemptId(), again.attemptId(),
                "the open Independent attempt must remain current");
    }

    private ApplyFlowResponse start(UUID learnerId, UUID idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        ResponseEntity<ApplyFlowResponse> response = http.exchange(
                "/api/apply/flows",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("learnerId", learnerId), headers),
                ApplyFlowResponse.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private ApplyFlowResponse submit(
            UUID flowId, UUID idempotencyKey, int interactionVersion, UUID attemptId,
            String raw, String confirmed, String rationale
    ) {
        ResponseEntity<ApplyFlowResponse> response = submitRaw(
                flowId, idempotencyKey, interactionVersion, attemptId, raw, confirmed, rationale);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<ApplyFlowResponse> submitRaw(
            UUID flowId, UUID idempotencyKey, int interactionVersion, UUID attemptId,
            String raw, String confirmed, String rationale
    ) {
        return http.exchange(
                "/api/apply/flows/" + flowId + "/submissions",
                HttpMethod.POST,
                submitEntity(idempotencyKey, interactionVersion, attemptId, raw, confirmed, rationale),
                ApplyFlowResponse.class
        );
    }

    private ResponseEntity<Map> submitRawMap(
            UUID flowId, UUID idempotencyKey, int interactionVersion, UUID attemptId,
            String raw, String confirmed, String rationale
    ) {
        return http.exchange(
                "/api/apply/flows/" + flowId + "/submissions",
                HttpMethod.POST,
                submitEntity(idempotencyKey, interactionVersion, attemptId, raw, confirmed, rationale),
                Map.class
        );
    }

    private HttpEntity<Map<String, Object>> submitEntity(
            UUID idempotencyKey, int interactionVersion, UUID attemptId,
            String raw, String confirmed, String rationale
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        java.util.HashMap<String, Object> body = new java.util.HashMap<>();
        body.put("interactionVersion", interactionVersion);
        body.put("attemptId", attemptId);
        body.put("rawDerivative", raw);
        body.put("confirmedCanonical", confirmed);
        if (rationale != null) {
            body.put("rationale", rationale);
        }
        return new HttpEntity<>(body, headers);
    }
}
