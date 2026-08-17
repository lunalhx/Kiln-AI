package cn.lunalhx.ai.kilnai;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class FailClosedCatalogHttpTest {

    @Autowired
    TestRestTemplate http;

    @Test
    void startFailsClosedWhenNoModelAdapterIsConfigured() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<Map> response = http.exchange(
                "/api/learning/flows",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("learnerId", UUID.randomUUID()), headers),
                Map.class
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode(),
                "an unconfigured operator Model Profile fails closed at Start with the generic 503");
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("code"));
        String message = String.valueOf(response.getBody().get("message"));
        assertFalse(message.contains("not configured"),
                "the generic 503 must not leak operator configuration state to the learner");
        assertFalse(message.contains("adapter"), "no adapter or provider details may reach the learner");
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void theOldSpikePathIsInaccessible() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<Map> response = http.exchange(
                "/api/spike/flows",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("learnerId", UUID.randomUUID(), "fixtureId", "percent-change-v1"), headers),
                Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void theTraceEndpointIsInaccessible() {
        ResponseEntity<Map> response = http.getForEntity(
                "/api/spike/flows/" + UUID.randomUUID() + "/trace", Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
