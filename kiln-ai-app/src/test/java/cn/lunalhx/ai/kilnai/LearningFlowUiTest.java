package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reference UI coverage of the unified Learning Flow API: committed
 * interactions are rendered as learner-facing regions and the closed
 * commands drive the loop — answer submission, review start and submission,
 * and the explicit leave — while private fields never reach the DOM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedLearningGraphPortsConfiguration.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class LearningFlowUiTest {

    @LocalServerPort
    int port;

    @Autowired
    ReviewTaskStore reviewStore;

    @Test
    void uiCompletesTheLearningLoopWithoutPrivateFields() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.navigate("http://127.0.0.1:" + port + "/");
            page.click("#start");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 f(x)')");
            String diagnostic = page.innerText("#view");
            assertTrue(diagnostic.contains("当前阶段：诊断"));
            assertTrue(diagnostic.contains("当前任务：诊断"));
            assertFalse(diagnostic.contains("12*x^2 - 6*x + 7"), "expected answer must not reach the UI");
            assertFalse(diagnostic.contains("openstax"), "source identities must not reach the UI");
            assertFalse(diagnostic.contains("fingerprint"), "fingerprints must not reach the UI");
            assertTrue(page.locator("[data-answer-field='final_derivative']").count() == 1,
                    "the returned mathematical answer field must be rendered dynamically");
            assertTrue(page.locator("[data-answer-field='rule_rationale']").count() == 1,
                    "the returned rationale field must be rendered dynamically");

            page.fill("#derivative", "f'(x) = 12x²−6x+7");
            assertTrue(page.inputValue("#canonical").equals("12*x^2-6*x+7"),
                    "the UI must submit the learner-confirmed canonical expression");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 g(x)')");
            String independent = page.innerText("#view");
            assertTrue(independent.contains("当前阶段：独立测试"));
            assertTrue(page.innerText("#notice").contains("独立练习"),
                    "the neutral transition message must state only the next interaction");
            assertFalse(independent.contains("15*x^2 - 2"), "expected answer must not reach the UI");

            page.fill("#derivative", "15*x^2 - 2");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('view').textContent.includes('当前状态：已完成')");
            String terminal = page.innerText("#view");
            assertEquals("独立", page.innerText("#current-milestone"), "the safe milestone must be visible");
            assertFalse(terminal.contains("15*x^2 - 2"), "no answer facts in the terminal message");
            assertFalse(terminal.contains("fingerprint"));
            assertFalse(terminal.contains("assessment"));

            page.waitForFunction("() => document.getElementById('reviews').textContent.includes('Review 1')");
            String upcoming = page.innerText("#reviews");
            assertTrue(upcoming.contains("已安排"), "the upcoming Review must be visible");
            assertTrue(upcoming.contains("即将到来，暂不可操作"),
                    "Scheduled Review work is upcoming and never actionable");
            assertFalse(upcoming.contains("15*x^2 - 2"), "no answer facts in the Review collection");
            assertFalse(upcoming.contains("fingerprint"));

            reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));
            page.reload();
            page.waitForFunction("() => document.getElementById('reviews').textContent.includes('可以开始')");
            String ready = page.innerText("#reviews");
            assertTrue(ready.contains("可以开始"), "the arrived Review must be Due");
            assertTrue(ready.contains("可以开始"),
                    "the reference UI must switch from upcoming to ready-to-start");
            assertFalse(ready.contains("15*x^2 - 2"));
            assertFalse(ready.contains("fingerprint"));

            page.click(".start-review");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 h(x)')");
            String reviewView = page.innerText("#view");
            assertTrue(reviewView.contains("当前阶段：延迟复习"), "the Review interaction must be in Delayed Review");
            assertTrue(reviewView.contains("当前任务：复习"), "the Review attempt purpose must be visible");
            assertTrue(reviewView.contains("当前状态：等待作答"));
            assertFalse(reviewView.contains("8*x^3 - 6*x"), "the Review expected answer must never reach the UI");
            assertFalse(reviewView.contains("fingerprint"));
            assertFalse(reviewView.contains("openstax"));
            assertFalse(page.isDisabled("#derivative"),
                    "the learner must be able to enter answering from the started Due Review");

            page.fill("#derivative", "8*x^3 - 6*x");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('view').textContent.includes('当前状态：已完成')");
            String afterReview1 = page.innerText("#view");
            assertEquals("独立", page.innerText("#current-milestone"),
                    "three Review passes keep Current Milestone Independent");
            assertFalse(afterReview1.contains("8*x^3 - 6*x"), "no answer facts in the Review completion message");

            passReviewInUi(page, 2, "15*x^4 - 4", Duration.ofDays(4));
            passReviewInUi(page, 3, "24*x^3 + 10*x", Duration.ofDays(8));
            passReviewInUi(page, 4, "21*x^2 - 2", Duration.ofDays(22));

            String durable = page.innerText("#view");
            assertEquals("持久", page.innerText("#current-milestone"),
                    "the fourth Review pass must show Durable in the reference UI");
            page.waitForFunction("() => document.getElementById('reviews').textContent.includes('暂无即将到来的复习')");
            assertTrue(page.innerText("#reviews").contains("暂无即将到来的复习"),
                    "Durable must end the cadence with no unfinished Review work");
            assertFalse(durable.contains("21*x^2 - 2"), "no answer facts in the Durable terminal message");
            assertFalse(durable.contains("fingerprint"));
        }
    }

    @Test
    void uiRendersTheTeachingUnionMemberAndTheExplicitLeave() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.onDialog(dialog -> dialog.accept());
            page.navigate("http://127.0.0.1:" + port + "/");
            page.click("#start");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 f(x)')");

            page.fill("#derivative", "3*x^2");
            page.fill("#rationale", "我猜的");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('teaching-region').hidden === false");
            String teaching = page.innerText("#view");
            assertTrue(teaching.contains("当前阶段：学习与练习"));
            assertTrue(page.innerText("#teaching").contains("例题"),
                    "the worked example must be rendered for the learner");
            assertTrue(page.innerText("#teaching").contains("15x² − 4x"),
                    "the worked example final result is learner-visible teaching content");
            assertFalse(teaching.contains("openstax"));
            assertFalse(teaching.contains("fingerprint"));
            assertFalse(teaching.contains("source_trace"));
            assertFalse(page.isDisabled("#continue"),
                    "Continue must be offered on the teaching boundary");

            page.click("#leave");
            page.waitForFunction("() => document.getElementById('view').textContent.includes('当前状态：已完成')");
            String left = page.innerText("#view");
            assertTrue(page.innerText("#transition-message").contains("已离开"),
                    "the explicit leave must render its transition message");
            assertTrue(left.contains("当前状态：已完成"));
            assertFalse(left.contains("15x² − 4x"), "the teaching content must not persist after leaving");
        }
    }

    @Test
    void uiConfirmsImplicitMultiplicationBeforeSubmitting() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.navigate("http://127.0.0.1:" + port + "/");
            page.click("#start");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 f(x)')");

            page.fill("#derivative", "x(x+1)");
            Response response = page.waitForResponse(
                    candidate -> candidate.url().contains("/api/learning/flows/")
                            && "POST".equals(candidate.request().method()),
                    () -> page.click("#submit"));
            assertEquals(200, response.status(),
                    "a parseable expression must not be rejected as an invalid submission");
            page.waitForFunction("() => document.getElementById('teaching-region').hidden === false");
            assertEquals("", page.innerText("#error"));
        }
    }

    @Test
    void uiConfirmsSupportedLatexBeforeSubmitting() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.navigate("http://127.0.0.1:" + port + "/");
            page.click("#start");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 f(x)')");

            page.fill("#derivative", "f^{\\prime}(x) = 12 \\cdot x^{2} - 6x + 7");
            assertEquals("12 * x^2 - 6*x + 7", page.inputValue("#canonical"));
            Response response = page.waitForResponse(
                    candidate -> candidate.url().contains("/api/learning/flows/")
                            && "POST".equals(candidate.request().method()),
                    () -> page.click("#submit"));
            assertEquals(200, response.status(),
                    "a supported LaTeX-like expression must not be rejected as an invalid submission");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 g(x)')");
        }
    }

    /**
     * Marks the next cadence step Due, reloads the reference UI, starts the
     * ready Review, and submits the correct expected derivative for that step.
     */
    private void passReviewInUi(Page page, int reviewNumber, String expected, Duration dueOffset) {
        reviewStore.markDueReviewsDue(Instant.now().plus(dueOffset));
        page.reload();
        page.waitForFunction("() => document.getElementById('reviews').textContent.includes('Review "
                + reviewNumber + "')");
        page.waitForFunction("() => document.getElementById('reviews').textContent.includes('可以开始')");
        page.click(".start-review");
        page.waitForFunction("() => { const field = document.getElementById('derivative'); return field !== null && field.disabled === false; }");
        page.fill("#derivative", expected);
        page.click("#submit");
        page.waitForFunction("() => document.getElementById('view').textContent.includes('当前状态：已完成')");
    }
}
