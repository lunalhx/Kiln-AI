package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.port.ReviewTaskStore;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reference UI coverage of the Inconclusive Review continuation states over
 * the unified Learning Flow API: the system-uncertainty notice accompanies
 * the replacement task, and a Review whose replacement could not be prepared
 * is clearly shown as resumable — never as a learner failure — and continued
 * through the same start action.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(InconclusiveReviewGraphPortsConfiguration.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class LearningFlowReviewReplacementUiTest {

    @LocalServerPort
    int port;

    @Autowired
    ReviewTaskStore reviewStore;

    @Autowired
    InconclusiveReviewGraphPortsConfiguration ports;

    @Test
    void uiShowsTheSystemUncertaintyNoticeAndContinuesWithTheReplacementTask() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.navigate("http://127.0.0.1:" + port + "/");
            completeIndependentPass(page);

            page.click(".start-review");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 h(x)')");

            page.fill("#derivative", "x^2^3");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 p(x)')");

            String notice = page.innerText("#notice");
            assertTrue(notice.contains("未能确定"),
                    "the system-uncertainty notice must be shown with the replacement task");
            assertFalse(notice.contains("失败"), "system uncertainty must never read as learner failure");
            assertFalse(page.isDisabled("#derivative"),
                    "the learner must be able to answer the replacement task");
            String view = page.innerText("#view");
            assertFalse(view.contains("15*x^4 - 4"), "the replacement expected answer must never reach the UI");
            assertFalse(view.contains("fingerprint"));
        }
    }

    @Test
    void uiShowsAnUnpreparedReplacementAsResumableAndContinuesIt() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.navigate("http://127.0.0.1:" + port + "/");
            completeIndependentPass(page);

            page.click(".start-review");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 h(x)')");

            ports.failNextReviewGeneration();
            page.fill("#derivative", "x^2^3");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('unavailable-region').hidden === false");
            String unavailable = page.innerText("#view");
            assertTrue(page.innerText("#notice").contains("未能确定"),
                    "the neutral unavailable message must be shown");
            assertFalse(unavailable.contains("失败"));

            page.waitForFunction("() => document.getElementById('reviews').textContent.includes('可继续')");
            String reviews = page.innerText("#reviews");
            assertTrue(reviews.contains("系统未能确定上次结果，可继续"),
                    "the Review must be clearly shown as continuable, not failed");
            assertTrue(reviews.contains("继续复习"),
                    "the same start action must offer the continuation");
            assertFalse(reviews.contains("开始复习"));

            page.click(".start-review");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 p(x)')");
            assertFalse(page.isDisabled("#derivative"),
                    "the resumed Review must be answerable");
            page.waitForFunction("() => document.getElementById('reviews').textContent.includes('已开始')");

            String bound = page.innerText("#reviews");
            assertTrue(bound.contains("已开始"),
                    "after resuming, the Review shows as bound work");
            assertFalse(bound.contains("可继续"),
                    "a resumed Review with an open attempt must not advertise a continuation");
        }
    }

    private void completeIndependentPass(Page page) {
        page.click("#start");
        page.waitForFunction("() => document.getElementById('task').textContent.includes('设 f(x)')");
        page.fill("#derivative", "12*x^2-6*x+7");
        page.click("#submit");
        page.waitForFunction("() => document.getElementById('continue').disabled === false");
        page.click("#continue");
        page.waitForFunction("() => document.getElementById('task').textContent.includes('设 g(x)')");
        page.fill("#derivative", "15*x^2 - 2");
        page.click("#submit");
        page.waitForFunction("() => document.getElementById('reviews').textContent.includes('Review 1')");
        reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));
        page.reload();
        page.waitForFunction("() => document.getElementById('reviews').textContent.includes('可以开始')");
    }
}
