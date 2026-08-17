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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one HTTP contract of the unified Learning Flow API (spec: "one HTTP
 * contract for unified Learning Flow creation, status, commands, interaction
 * variants, status codes, idempotency behavior, private-field absence, and
 * removal of the old Apply endpoints"). Scripted graph ports drive the whole
 * guarded Learning StateGraph through {@code /api/learning/flows} and
 * {@code /api/review-tasks}; every learner-visible response is a projection
 * of committed durable state with no private assessor facts, and the removed
 * {@code /api/apply/**} endpoints are gone.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedLearningGraphPortsConfiguration.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class LearningFlowHttpTest {

    @Autowired
    TestRestTemplate http;

    @Autowired
    ReviewTaskStore reviewStore;

    @Autowired
    ScriptedLearningGraphPortsConfiguration config;

    @Test
    void theUnifiedFlowStartsQueriesSubmitsAndReplaysWithOnlySafeProjections() {
        UUID learnerId = UUID.randomUUID();
        UUID startKey = UUID.randomUUID();

        LearningFlowResponse started = start(learnerId, startKey);
        assertEquals("task", started.kind());
        assertEquals("AWAITING_LEARNER_INPUT", started.status());
        assertEquals("DIAGNOSTIC", started.stage());
        assertEquals(1, started.interactionVersion());
        assertNotNull(started.attemptId());
        assertEquals("DIAGNOSTIC", started.attemptPurpose());
        assertNotNull(started.task());
        assertEquals("zh-CN", started.task().locale());
        assertEquals(ScriptedApplyPortsConfiguration.DIAGNOSTIC_TASK, started.task().taskText());
        assertEquals(List.of("answer_submitted", "clarification_asked", "flow_control_requested"),
                started.allowedEvents(), "a Diagnostic task never permits hints");
        assertNull(started.teaching());
        assertNull(started.consent());
        assertNull(started.hint());
        assertFalse(serialize(started).contains(ScriptedApplyPortsConfiguration.DIAGNOSTIC_EXPECTED),
                "the expected answer must never reach the learner");
        assertFalse(serialize(started).contains("openstax"));
        assertFalse(serialize(started).contains("fingerprint"));
        assertFalse(serialize(started).contains("assessment"));
        assertFalse(serialize(started).contains("rubric"));

        LearningFlowResponse replayedStart = start(learnerId, startKey);
        assertEquals(started.flowId(), replayedStart.flowId());
        assertEquals(started.interactionVersion(), replayedStart.interactionVersion());

        LearningFlowResponse queried = http.getForObject(
                "/api/learning/flows/" + started.flowId(), LearningFlowResponse.class);
        assertEquals(started, queried, "query must recover the exact latest interaction");

        ResponseEntity<LearningFlowResponse> rejected = submitRaw(
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
        LearningFlowResponse transitioned = submit(
                started.flowId(), submitKey, started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals("task", transitioned.kind());
        assertEquals("AWAITING_LEARNER_INPUT", transitioned.status());
        assertEquals("INDEPENDENT_TEST", transitioned.stage());
        assertEquals(2, transitioned.interactionVersion());
        assertNotNull(transitioned.task());
        assertEquals(ScriptedApplyPortsConfiguration.INDEPENDENT_TASK, transitioned.task().taskText());
        assertTrue(transitioned.learnerMessage().contains("独立练习"),
                "the neutral transition message states only the next interaction");
        assertFalse(serialize(transitioned).contains(ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED));

        LearningFlowResponse replayedSubmit = submit(
                started.flowId(), submitKey, started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(transitioned, replayedSubmit,
                "a replayed idempotency key must return the original result");

        ResponseEntity<Map> duplicate = submitRawMap(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode(),
                "a duplicate submission for the already closed Diagnostic attempt must conflict");

        LearningFlowResponse completed = submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        assertEquals("transition", completed.kind(),
                "the terminal completion is the message-only transition union member");
        assertEquals("TERMINAL", completed.status());
        assertEquals(3, completed.interactionVersion());
        assertNull(completed.task());
        assertNull(completed.attemptId());
        assertTrue(completed.learnerMessage().contains("已完成"));
        assertEquals(List.of(), completed.allowedEvents(),
                "a terminal transition offers no further commands");
        assertFalse(serialize(completed).contains(ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED));
        assertFalse(serialize(completed).contains("fingerprint"));
        assertFalse(serialize(completed).contains("assessment"));
        assertEquals("INDEPENDENT", completed.progress().currentMilestone());
        assertEquals("INDEPENDENT", completed.progress().highestMilestoneReached());
        assertEquals("DELAYED_REVIEW", completed.progress().stage());
    }

    @Test
    void theClosedCommandDiscriminatorsRouteTeachingHintConsentAndFlowControlOverTheUnifiedEndpoint() {
        UUID learnerId = UUID.randomUUID();
        LearningFlowResponse started = start(learnerId, UUID.randomUUID());

        LearningFlowResponse teaching = command(started.flowId(), UUID.randomUUID(), Map.of(
                "command", "answer_submitted",
                "interactionVersion", started.interactionVersion(),
                "attemptId", started.attemptId().toString(),
                "rawAnswer", "3*x^2",
                "confirmedCanonical", "3*x^2"));
        assertEquals("teaching", teaching.kind(),
                "a conclusive Diagnostic failure opens the teaching union member");
        assertNotNull(teaching.teaching());
        assertNotNull(teaching.teaching().principleSummary());
        assertEquals(4, teaching.teaching().workedExample().steps().size(),
                "exactly one complete worked example");
        assertEquals(List.of("continue_requested", "clarification_asked", "flow_control_requested"),
                teaching.allowedEvents(), "Explain permits Continue, Clarification, and Flow Control");
        assertNull(teaching.attemptId());
        assertFalse(serialize(teaching).contains("openstax"));
        assertFalse(serialize(teaching).contains("fingerprint"));
        assertFalse(serialize(teaching).contains("source_trace"));
        assertFalse(serialize(teaching).contains("rule_identification"));
        assertFalse(serialize(teaching).contains(ScriptedLearningGraphPortsConfiguration.PRACTICE_EXPECTED));

        LearningFlowResponse practice = command(teaching.flowId(), UUID.randomUUID(), Map.of(
                "command", "continue_requested",
                "interactionVersion", teaching.interactionVersion()));
        assertEquals("task", practice.kind());
        assertEquals("PRACTICE", practice.attemptPurpose());
        assertEquals(ScriptedLearningGraphPortsConfiguration.PRACTICE_TASK, practice.task().taskText());
        assertTrue(practice.allowedEvents().contains("hint_requested"),
                "an open Apply Practice Attempt permits hints (ADR-0065)");
        assertFalse(serialize(practice).contains(ScriptedLearningGraphPortsConfiguration.PRACTICE_EXPECTED));

        LearningFlowResponse hinted = command(practice.flowId(), UUID.randomUUID(), Map.of(
                "command", "hint_requested",
                "interactionVersion", practice.interactionVersion(),
                "attemptId", practice.attemptId().toString(),
                "answerRequested", false));
        assertEquals("task", hinted.kind());
        assertEquals(1, hinted.hint().level());
        assertEquals("orient", hinted.hint().disclosureKind());
        assertNull(hinted.hint().proposedFinalAnswer(),
                "only the exposed H1 level may reach the learner");
        assertFalse(serialize(hinted).contains("scaffold"), "unexposed ladder levels must never leak");
        assertFalse(serialize(hinted).contains("proposed_final_answer"));
        assertFalse(serialize(hinted).contains("18*x^2-4"));

        LearningFlowResponse left = command(hinted.flowId(), UUID.randomUUID(), Map.of(
                "command", "flow_control_requested",
                "interactionVersion", hinted.interactionVersion()));
        assertEquals("transition", left.kind());
        assertEquals("TERMINAL", left.status());
        assertTrue(left.learnerMessage().contains("已离开"));
        assertFalse(serialize(left).contains(ScriptedLearningGraphPortsConfiguration.PRACTICE_EXPECTED));

        LearningFlowResponse replayed = command(hinted.flowId(), UUID.randomUUID(), Map.of(
                "command", "flow_control_requested",
                "interactionVersion", left.interactionVersion()));
        assertEquals(6, replayed.interactionVersion(),
                "a new leave after the committed leave must advance the committed transition");
    }

    @Test
    void aSubstantiveClarificationOnAnOpenIndependentAttemptProjectsTheConsentUnionMember() {
        UUID learnerId = UUID.randomUUID();
        LearningFlowResponse started = start(learnerId, UUID.randomUUID());
        LearningFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);

        LearningFlowResponse consented = command(transitioned.flowId(), UUID.randomUUID(), Map.of(
                "command", "clarification_asked",
                "interactionVersion", transitioned.interactionVersion(),
                "attemptId", transitioned.attemptId().toString(),
                "message", "为什么幂法则适用？"));
        assertEquals("assistance_consent", consented.kind(),
                "a substantive clarification on Independent first projects the consent union member");
        assertNotNull(consented.consent());
        assertTrue(consented.consent().warning().contains("转为练习"));
        assertEquals(List.of("assistance_decided", "flow_control_requested"),
                consented.allowedEvents(), "the consent boundary offers accept, refuse, or leave");
        assertEquals(transitioned.attemptId(), consented.attemptId(),
                "the consent boundary keeps the untouched attempt");

        LearningFlowResponse refused = command(consented.flowId(), UUID.randomUUID(), Map.of(
                "command", "assistance_decided",
                "interactionVersion", consented.interactionVersion(),
                "attemptId", consented.attemptId().toString(),
                "accept", false));
        assertEquals("task", refused.kind());
        assertEquals(transitioned.attemptId(), refused.attemptId(),
                "refusal must preserve the unchanged Independent attempt");
        assertTrue(refused.learnerMessage().contains("保持不变"));
        assertFalse(serialize(refused).contains(ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED));

        LearningFlowResponse converted = command(consented.flowId(), UUID.randomUUID(), Map.of(
                "command", "assistance_decided",
                "interactionVersion", refused.interactionVersion(),
                "attemptId", consented.attemptId().toString(),
                "accept", true));
        assertEquals("teaching", converted.kind(),
                "accepted assistance converts once before any teaching content is exposed");
        assertNotNull(converted.teaching());
    }

    @Test
    void anUnavailableGenerationProjectsTheUnavailableUnionMemberAndRetryRecovers() {
        UUID learnerId = UUID.randomUUID();
        LearningFlowResponse started = start(learnerId, UUID.randomUUID());
        config.failNextExplainGeneration();

        UUID failKey = UUID.randomUUID();
        ResponseEntity<LearningFlowResponse> failed = commandRaw(started.flowId(), failKey, Map.of(
                "command", "answer_submitted",
                "interactionVersion", started.interactionVersion(),
                "attemptId", started.attemptId().toString(),
                "rawAnswer", "3*x^2",
                "confirmedCanonical", "3*x^2"));
        assertEquals(HttpStatus.OK, failed.getStatusCode());
        LearningFlowResponse unavailable = failed.getBody();
        assertEquals("unavailable", unavailable.kind(),
                "a failed teaching generation projects the unavailable union member");
        assertEquals("TERMINAL", unavailable.status());
        assertEquals(List.of("flow_control_requested"), unavailable.allowedEvents(),
                "an unavailable boundary offers Flow Control; a safe retry re-issues the command");
        assertNull(unavailable.teaching());
        assertNull(unavailable.task());
        assertFalse(serialize(unavailable).contains("openstax"));
        assertFalse(serialize(unavailable).contains("fingerprint"));

        ResponseEntity<LearningFlowResponse> replayed = commandRaw(started.flowId(), failKey, Map.of(
                "command", "answer_submitted",
                "interactionVersion", started.interactionVersion(),
                "attemptId", started.attemptId().toString(),
                "rawAnswer", "3*x^2",
                "confirmedCanonical", "3*x^2"));
        assertEquals(unavailable, replayed.getBody(),
                "the same Idempotency-Key after a failed generation replays the committed unavailable boundary");

        ResponseEntity<Map> freshRetry = commandRawMap(started.flowId(), UUID.randomUUID(), Map.of(
                "command", "answer_submitted",
                "interactionVersion", unavailable.interactionVersion(),
                "attemptId", started.attemptId().toString(),
                "rawAnswer", "3*x^2",
                "confirmedCanonical", "3*x^2"));
        assertEquals(HttpStatus.CONFLICT, freshRetry.getStatusCode(),
                "a fresh-key retry of the closed Diagnostic attempt is ignored without a second evaluation");

        LearningFlowResponse left = command(started.flowId(), UUID.randomUUID(), Map.of(
                "command", "flow_control_requested",
                "interactionVersion", unavailable.interactionVersion()));
        assertEquals("transition", left.kind(),
                "Flow Control leaves the unavailable boundary through the closed command surface");
        assertTrue(left.learnerMessage().contains("已离开"));
    }

    @Test
    void anUnknownCommandDiscriminatorIsRejected() {
        LearningFlowResponse started = start(UUID.randomUUID(), UUID.randomUUID());
        ResponseEntity<Map> unknown = commandRawMap(started.flowId(), UUID.randomUUID(), Map.of(
                "command", "bogus_command",
                "interactionVersion", started.interactionVersion()));
        assertEquals(HttpStatus.BAD_REQUEST, unknown.getStatusCode(),
                "an unknown closed command discriminator must be rejected");
    }

    @Test
    void theReviewCollectionExposesOnlyScheduledWorkAndSafeProgress() {
        UUID learnerId = UUID.randomUUID();
        UUID flowId = completeIndependentPass(learnerId);

        ResponseEntity<ReviewTaskView[]> response = http.getForEntity(
                "/api/review-tasks?learnerId=" + learnerId, ReviewTaskView[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReviewTaskView[] reviews = response.getBody();
        assertNotNull(reviews);
        assertEquals(1, reviews.length, "exactly one scheduled Review must exist");
        ReviewTaskView review = reviews[0];
        assertEquals("SCHEDULED", review.status());
        assertEquals(1, review.reviewNumber());
        assertFalse(review.startable(), "Scheduled work is upcoming, never startable");
        assertNotNull(review.reviewId());
        assertNotNull(review.conceptId());
        assertNotNull(review.dueAt());
        assertEquals("INDEPENDENT", review.progress().currentMilestone());
        assertEquals("DELAYED_REVIEW", review.progress().stage());
        assertFalse(flowId.equals(review.reviewId()));
    }

    @Test
    void aDueReviewCanBeStartedPassedAndReplayedOverTheUnifiedCommands() {
        UUID learnerId = UUID.randomUUID();
        completeIndependentPass(learnerId);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ReviewTaskView due = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];
        assertTrue(due.startable(), "the Due Review must be startable");

        UUID startKey = UUID.randomUUID();
        LearningFlowResponse review = startReview(due.reviewId(), startKey);
        assertEquals("task", review.kind());
        assertEquals("AWAITING_LEARNER_INPUT", review.status());
        assertEquals("DELAYED_REVIEW", review.stage());
        assertEquals("REVIEW", review.attemptPurpose());
        assertNotNull(review.attemptId());
        assertEquals(ScriptedApplyPortsConfiguration.REVIEW_TASK, review.task().taskText());
        assertFalse(serialize(review).contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED),
                "the Review expected answer must never reach the learner");
        assertEquals("INDEPENDENT", review.progress().currentMilestone());
        assertEquals("DELAYED_REVIEW", review.progress().stage());

        LearningFlowResponse replayed = startReview(due.reviewId(), startKey);
        assertEquals(review, replayed, "a replayed start key must return the original interaction");

        ResponseEntity<Map> secondStart = startReviewRawMap(due.reviewId(), UUID.randomUUID());
        assertEquals(HttpStatus.CONFLICT, secondStart.getStatusCode(),
                "a different-key second start must conflict without a second attempt");

        UUID reviewKey = UUID.randomUUID();
        LearningFlowResponse completed = submit(
                review.flowId(), reviewKey, review.interactionVersion(), review.attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED, null);
        assertEquals("transition", completed.kind());
        assertEquals("TERMINAL", completed.status());
        assertTrue(completed.learnerMessage().contains("复习已完成"));
        assertFalse(serialize(completed).contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED));

        LearningFlowResponse replayedReview = submit(
                review.flowId(), reviewKey, review.interactionVersion(), review.attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED, null);
        assertEquals(completed, replayedReview,
                "a replayed Review submission key must return the original result");

        ReviewTaskView[] remaining = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals(1, remaining.length, "exactly one successor Review must be scheduled");
        assertEquals(2, remaining[0].reviewNumber());
        assertEquals("SCHEDULED", remaining[0].status());
        assertEquals("INDEPENDENT", remaining[0].progress().currentMilestone());
    }

    @Test
    void aReviewFailOverHttpStopsTheCadenceWithLearningProgressAndNoUnfinishedWork() {
        UUID learnerId = UUID.randomUUID();
        completeIndependentPass(learnerId);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ReviewTaskView due = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];
        LearningFlowResponse review = startReview(due.reviewId(), UUID.randomUUID());
        LearningFlowResponse failed = submit(
                review.flowId(), UUID.randomUUID(), review.interactionVersion(), review.attemptId(),
                "9*x^2", "9*x^2", null);
        assertEquals("transition", failed.kind());
        assertEquals("TERMINAL", failed.status());
        assertTrue(failed.learnerMessage().contains("复习已结束"));
        assertFalse(serialize(failed).contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED));
        assertEquals("LEARNING", failed.progress().currentMilestone(),
                "a conclusive Review failure must drop Current Milestone to Learning");
        assertEquals("INDEPENDENT", failed.progress().highestMilestoneReached());
        assertEquals("LEARNING_AND_PRACTICE", failed.progress().stage());

        ReviewTaskView[] remaining = http.getForEntity("/api/review-tasks?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals(0, remaining.length,
                "a Review failure must leave no actionable Review work and schedule no successor");
    }

    @Test
    void theOldApplyEndpointsAreRemovedWithoutAliasesOrFallbackMappings() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", UUID.randomUUID().toString());
        assertEquals(HttpStatus.NOT_FOUND, http.exchange(
                "/api/apply/flows",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("learnerId", UUID.randomUUID()), headers),
                Map.class).getStatusCode(),
                "the old Apply flow creation endpoint must be removed");
        assertEquals(HttpStatus.NOT_FOUND, http.getForEntity(
                "/api/apply/flows/" + UUID.randomUUID(), Map.class).getStatusCode(),
                "the old Apply flow query endpoint must be removed");
        assertEquals(HttpStatus.NOT_FOUND, http.exchange(
                "/api/apply/flows/" + UUID.randomUUID() + "/submissions",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                Map.class).getStatusCode(),
                "the old Apply submission endpoint must be removed");
        assertEquals(HttpStatus.NOT_FOUND, http.getForEntity(
                "/api/apply/reviews?learnerId=" + UUID.randomUUID(), Map.class).getStatusCode(),
                "the old Apply review collection must be removed");
    }

    private UUID completeIndependentPass(UUID learnerId) {
        LearningFlowResponse started = start(learnerId, UUID.randomUUID());
        LearningFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        return started.flowId();
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

    private ResponseEntity<Map> startReviewRawMap(UUID reviewId, UUID idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        return http.exchange(
                "/api/review-tasks/" + reviewId + "/start",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                Map.class);
    }

    private LearningFlowResponse submit(
            UUID flowId, UUID idempotencyKey, int interactionVersion, UUID attemptId,
            String raw, String confirmed, String rationale
    ) {
        ResponseEntity<LearningFlowResponse> response = submitRaw(
                flowId, idempotencyKey, interactionVersion, attemptId, raw, confirmed, rationale);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<LearningFlowResponse> submitRaw(
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
        return http.exchange(
                "/api/learning/flows/" + flowId + "/commands",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                LearningFlowResponse.class);
    }

    private ResponseEntity<Map> submitRawMap(
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
        return http.exchange(
                "/api/learning/flows/" + flowId + "/commands",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);
    }

    private LearningFlowResponse command(UUID flowId, UUID idempotencyKey, Map<String, Object> body) {
        ResponseEntity<LearningFlowResponse> response = commandRaw(flowId, idempotencyKey, body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<LearningFlowResponse> commandRaw(UUID flowId, UUID idempotencyKey, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        return http.exchange(
                "/api/learning/flows/" + flowId + "/commands",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                LearningFlowResponse.class);
    }

    private ResponseEntity<Map> commandRawMap(UUID flowId, UUID idempotencyKey, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        return http.exchange(
                "/api/learning/flows/" + flowId + "/commands",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);
    }

    private String serialize(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
