package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.api.dto.ApplyFlowResponse;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedApplyPortsConfiguration.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class ApplyLearnerHttpTest {

    @Autowired
    TestRestTemplate http;

    @Autowired
    ReviewTaskStore reviewStore;

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
        assertEquals("INDEPENDENT", completed.progress().currentMilestone());
        assertEquals("INDEPENDENT", completed.progress().highestMilestoneReached());
        assertEquals("DELAYED_REVIEW", completed.progress().stage());
    }

    @Test
    void theReviewCollectionExposesOnlyTheScheduledUpcomingReviewAndSafeProgress() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);

        ResponseEntity<ReviewTaskView[]> response = http.getForEntity(
                "/api/apply/reviews?learnerId=" + learnerId, ReviewTaskView[].class);

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
        assertEquals("INDEPENDENT", review.progress().highestMilestoneReached());
        assertEquals("DELAYED_REVIEW", review.progress().stage());
    }

    @Test
    void aDueReviewIsMarkedStartableInTheCollection() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);

        ResponseEntity<ReviewTaskView[]> upcoming = http.getForEntity(
                "/api/apply/reviews?learnerId=" + learnerId, ReviewTaskView[].class);
        assertEquals(1, upcoming.getBody().length);
        assertEquals("SCHEDULED", upcoming.getBody()[0].status());
        assertFalse(upcoming.getBody()[0].startable());

        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ResponseEntity<ReviewTaskView[]> due = http.getForEntity(
                "/api/apply/reviews?learnerId=" + learnerId, ReviewTaskView[].class);
        assertEquals(1, due.getBody().length);
        ReviewTaskView view = due.getBody()[0];
        assertEquals("DUE", view.status());
        assertTrue(view.startable(), "a Due Review is the only startable work");
        assertNotNull(view.reviewId());
        assertNotNull(view.dueAt());
    }

    @Test
    void theReviewCollectionNeverLeaksPrivateFacts() {        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);

        ResponseEntity<String> raw = http.getForEntity(
                "/api/apply/reviews?learnerId=" + learnerId, String.class);

        assertEquals(HttpStatus.OK, raw.getStatusCode());
        assertFalse(raw.getBody().contains(learnerId.toString()),
                "the learner UUID must never appear in the collection");
        assertFalse(raw.getBody().contains("15*x^2 - 2"), "expected answers must never leak");
        assertFalse(raw.getBody().contains("fingerprint"));
        assertFalse(raw.getBody().contains("openstax"));
        assertFalse(raw.getBody().contains("assessment"));
        assertFalse(raw.getBody().contains("evidence"));
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

    @Test
    void aDueReviewCanBeStartedOverHttpWithAnIdempotentReviewInteraction() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ReviewTaskView due = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];
        assertTrue(due.startable(), "the Due Review must be startable");

        UUID startKey = UUID.randomUUID();
        ApplyFlowResponse review = startReview(due.reviewId(), startKey);
        assertEquals("AWAITING_LEARNER_INPUT", review.status());
        assertEquals("DELAYED_REVIEW", review.stage());
        assertEquals("REVIEW", review.attemptPurpose());
        assertNotNull(review.attemptId());
        assertNotNull(review.task());
        assertEquals(ScriptedApplyPortsConfiguration.REVIEW_TASK, review.task().taskText());
        assertFalse(review.task().taskText().contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED),
                "the Review expected answer must never reach the learner");
        assertEquals(4, review.interactionVersion(), "the Review interaction appends the original Flow");
        assertEquals("INDEPENDENT", review.progress().currentMilestone());
        assertEquals("DELAYED_REVIEW", review.progress().stage());

        ApplyFlowResponse replayed = startReview(due.reviewId(), startKey);
        assertEquals(review, replayed, "a replayed start key must return the original interaction");

        ResponseEntity<Map> secondStart = startReviewRawMap(due.reviewId(), UUID.randomUUID());
        assertEquals(HttpStatus.CONFLICT, secondStart.getStatusCode(),
                "a different-key second start must conflict without a second attempt");

        ResponseEntity<ReviewTaskView[]> after = http.getForEntity(
                "/api/apply/reviews?learnerId=" + learnerId, ReviewTaskView[].class);
        assertEquals("STARTED", after.getBody()[0].status());
        assertFalse(after.getBody()[0].startable(), "a Started Review is bound and not startable again");
    }

    @Test
    void theReviewStartResponseNeverLeaksPrivateFacts() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        UUID reviewId = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0].reviewId();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<String> raw = http.exchange(
                "/api/apply/reviews/" + reviewId + "/start",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                String.class);

        assertEquals(HttpStatus.OK, raw.getStatusCode());
        assertFalse(raw.getBody().contains(learnerId.toString()),
                "the learner UUID must never appear in the start response");
        assertFalse(raw.getBody().contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED),
                "expected answers must never leak");
        assertFalse(raw.getBody().contains("fingerprint"));
        assertFalse(raw.getBody().contains("openstax"));
        assertFalse(raw.getBody().contains("assessment"));
        assertFalse(raw.getBody().contains("evidence"));
    }

    @Test
    void aScheduledOrUnknownReviewCannotBeStarted() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);

        ReviewTaskView scheduled = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];
        assertEquals(HttpStatus.CONFLICT, startReviewRawMap(scheduled.reviewId(), UUID.randomUUID()).getStatusCode(),
                "pre-due work must never be startable");
        assertEquals("SCHEDULED",
                http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                        ReviewTaskView[].class).getBody()[0].status(),
                "a refused start must not change the Review");

        assertEquals(HttpStatus.NOT_FOUND, startReviewRawMap(UUID.randomUUID(), UUID.randomUUID()).getStatusCode(),
                "an unknown Review Task must be not found");
    }

    @Test
    void aReviewPassThroughTheApplySubmissionEndpointSchedulesTheNextReviewAndShowsSafeProgress() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ReviewTaskView due = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];
        ApplyFlowResponse review = startReview(due.reviewId(), UUID.randomUUID());

        Instant before = Instant.now();
        UUID submitKey = UUID.randomUUID();
        ApplyFlowResponse completed = submit(
                review.flowId(), submitKey, review.interactionVersion(), review.attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED, null);
        Instant after = Instant.now();

        assertEquals("TERMINAL", completed.status());
        assertEquals("DELAYED_REVIEW", completed.stage());
        assertTrue(completed.learnerMessage().contains("复习已完成"));
        assertFalse(completed.learnerMessage().contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED),
                "no answer facts in the Review completion message");
        assertFalse(completed.learnerMessage().contains("fingerprint"));
        assertFalse(completed.learnerMessage().contains("assessment"));
        assertEquals("INDEPENDENT", completed.progress().currentMilestone());
        assertEquals("INDEPENDENT", completed.progress().highestMilestoneReached());
        assertEquals("DELAYED_REVIEW", completed.progress().stage());

        ApplyFlowResponse replayed = submit(
                review.flowId(), submitKey, review.interactionVersion(), review.attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED, null);
        assertEquals(completed, replayed,
                "a replayed Review submission key must return the original result");

        ReviewTaskView[] remaining = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals(1, remaining.length, "exactly one successor Review must be scheduled");
        ReviewTaskView successor = remaining[0];
        assertEquals(2, successor.reviewNumber());
        assertEquals("SCHEDULED", successor.status());
        assertFalse(successor.startable());
        assertNotNull(successor.dueAt());
        assertTrue(successor.dueAt().isAfter(before.plus(Duration.ofDays(3))),
                "Review 2 must be due 3 days after the actual Review 1 completion");
        assertTrue(successor.dueAt().isBefore(after.plus(Duration.ofDays(3)).plusSeconds(1)),
                "Review 2 must be due 3 days after the actual Review 1 completion");
        assertEquals("INDEPENDENT", successor.progress().currentMilestone());
    }

    @Test
    void theFourReviewCadenceOverHttpProjectsDurableAndEndsWithNoUnfinishedWork() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ApplyFlowResponse afterReview1 = passReview(learnerId, 1,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED);
        assertEquals("INDEPENDENT", afterReview1.progress().currentMilestone(),
                "three Review passes keep the milestone Independent");

        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofDays(4)));
        ApplyFlowResponse afterReview2 = passReview(learnerId, 2,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_2);
        assertEquals("INDEPENDENT", afterReview2.progress().currentMilestone());

        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofDays(8)));
        ApplyFlowResponse afterReview3 = passReview(learnerId, 3,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_3);
        assertEquals("INDEPENDENT", afterReview3.progress().currentMilestone());

        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofDays(22)));
        ApplyFlowResponse durable = passReview(learnerId, 4,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_4);
        assertEquals("TERMINAL", durable.status());
        assertEquals("DURABLE", durable.progress().currentMilestone(),
                "the fourth Review pass must project Durable");
        assertEquals("DURABLE", durable.progress().highestMilestoneReached());
        assertEquals("DELAYED_REVIEW", durable.progress().stage());
        assertFalse(durable.learnerMessage().contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED_4));
        assertFalse(durable.learnerMessage().contains("fingerprint"));

        ReviewTaskView[] finished = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals(0, finished.length,
                "Durable must end the cadence with no unfinished Review work");
    }

    @Test
    void aReviewFailOverHttpStopsTheCadenceWithLearningProgressAndNoUnfinishedWork() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ReviewTaskView due = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];
        ApplyFlowResponse review = startReview(due.reviewId(), UUID.randomUUID());
        ApplyFlowResponse failed = submit(
                review.flowId(), UUID.randomUUID(), review.interactionVersion(), review.attemptId(),
                "9*x^2", "9*x^2", null);

        assertEquals("TERMINAL", failed.status());
        assertEquals("DELAYED_REVIEW", failed.stage());
        assertTrue(failed.learnerMessage().contains("复习已结束"),
                "a conclusive Review failure must end with the safe learner outcome");
        assertFalse(failed.learnerMessage().contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED),
                "no answer facts may leak into a Review failure message");
        assertFalse(failed.learnerMessage().contains("fingerprint"));
        assertFalse(failed.learnerMessage().contains("assessment"));
        assertEquals("LEARNING", failed.progress().currentMilestone(),
                "a conclusive Review failure must drop Current Milestone to Learning");
        assertEquals("INDEPENDENT", failed.progress().highestMilestoneReached(),
                "a Review failure must preserve Highest Milestone Reached");
        assertEquals("LEARNING_AND_PRACTICE", failed.progress().stage());

        ReviewTaskView[] remaining = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals(0, remaining.length,
                "a Review failure must leave no actionable Review work and schedule no successor");
    }

    @Test
    void aRationaleContradictionOverHttpIsAConclusiveReviewFailWithTheInconsistencyMessage() {
        UUID learnerId = UUID.randomUUID();
        ApplyFlowResponse started = start(learnerId, UUID.randomUUID());
        ApplyFlowResponse transitioned = submit(
                started.flowId(), UUID.randomUUID(), started.interactionVersion(), started.attemptId(),
                "12x²−6x+7", "12*x^2-6*x+7", null);
        submit(
                started.flowId(), UUID.randomUUID(), transitioned.interactionVersion(),
                transitioned.attemptId(),
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED,
                ScriptedApplyPortsConfiguration.INDEPENDENT_EXPECTED, null);
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));

        ReviewTaskView due = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];
        ApplyFlowResponse review = startReview(due.reviewId(), UUID.randomUUID());
        ApplyFlowResponse failed = submit(
                review.flowId(), UUID.randomUUID(), review.interactionVersion(), review.attemptId(),
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED,
                ScriptedApplyPortsConfiguration.REVIEW_EXPECTED,
                ScriptedApplyPortsConfiguration.CONTRADICTORY_RATIONALE_MARKER);

        assertEquals("TERMINAL", failed.status());
        assertTrue(failed.learnerMessage().contains("最终答案与给出的理由不一致"),
                "the learner must clearly receive the answer-rationale inconsistency notice");
        assertFalse(failed.learnerMessage().contains(ScriptedApplyPortsConfiguration.REVIEW_EXPECTED),
                "the contradiction message must never leak the expected answer");
        assertFalse(failed.learnerMessage().contains("fingerprint"));
        assertFalse(failed.learnerMessage().contains("assessment"));
        assertEquals("LEARNING", failed.progress().currentMilestone());
        assertEquals("INDEPENDENT", failed.progress().highestMilestoneReached());

        ReviewTaskView[] remaining = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody();
        assertEquals(0, remaining.length,
                "a contradiction failure must stop the cadence exactly like a Review FAIL");
    }

    /** Starts the currently Due Review with the given number and submits its passing answer. */
    private ApplyFlowResponse passReview(UUID learnerId, int reviewNumber, String expectedAnswer) {
        ReviewTaskView due = http.getForEntity("/api/apply/reviews?learnerId=" + learnerId,
                ReviewTaskView[].class).getBody()[0];
        assertEquals(reviewNumber, due.reviewNumber());
        assertTrue(due.startable(), "the Review must be Due and startable");
        ApplyFlowResponse review = startReview(due.reviewId(), UUID.randomUUID());
        return submit(
                review.flowId(), UUID.randomUUID(), review.interactionVersion(), review.attemptId(),
                expectedAnswer, expectedAnswer, null);
    }

    private ApplyFlowResponse startReview(UUID reviewId, UUID idempotencyKey) {
        ResponseEntity<ApplyFlowResponse> response = startReviewRaw(reviewId, idempotencyKey);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<ApplyFlowResponse> startReviewRaw(UUID reviewId, UUID idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        return http.exchange(
                "/api/apply/reviews/" + reviewId + "/start",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                ApplyFlowResponse.class);
    }

    private ResponseEntity<Map> startReviewRawMap(UUID reviewId, UUID idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Idempotency-Key", idempotencyKey.toString());
        return http.exchange(
                "/api/apply/reviews/" + reviewId + "/start",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                Map.class);
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
