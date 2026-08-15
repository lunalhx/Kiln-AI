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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ScriptedApplyPortsConfiguration.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class ApplyLearnerUiTest {

    @LocalServerPort
    int port;

    @Autowired
    ReviewTaskStore reviewStore;

    @Test
    void uiCompletesTheApplyFlowWithoutPrivateFields() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.navigate("http://127.0.0.1:" + port + "/");
            page.click("#start");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 f(x)')");
            String diagnostic = page.innerText("#view");
            assertTrue(diagnostic.contains("DIAGNOSTIC"));
            assertFalse(diagnostic.contains("12*x^2 - 6*x + 7"), "expected answer must not reach the UI");
            assertFalse(diagnostic.contains("openstax"), "source identities must not reach the UI");
            assertFalse(diagnostic.contains("fingerprint"), "fingerprints must not reach the UI");

            page.fill("#derivative", "12*x^2-6*x+7");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 g(x)')");
            String independent = page.innerText("#view");
            assertTrue(independent.contains("INDEPENDENT_TEST"));
            assertFalse(independent.contains("15*x^2 - 2"), "expected answer must not reach the UI");

            page.fill("#derivative", "15*x^2 - 2");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('已完成')");
            String terminal = page.innerText("#view");
            assertTrue(terminal.contains("TERMINAL"));
            assertTrue(terminal.contains("INDEPENDENT"), "the safe milestone must be visible");
            assertFalse(terminal.contains("15*x^2 - 2"), "no answer facts in the terminal message");
            assertFalse(terminal.contains("fingerprint"));
            assertFalse(terminal.contains("assessment"));

            page.waitForFunction("() => document.getElementById('reviews').textContent.includes('Review 1')");
            String upcoming = page.innerText("#reviews");
            assertTrue(upcoming.contains("SCHEDULED"), "the upcoming Review must be visible");
            assertTrue(upcoming.contains("即将到来，暂不可操作"),
                    "Scheduled Review work is upcoming and never actionable");
            assertFalse(upcoming.contains("15*x^2 - 2"), "no answer facts in the Review collection");
            assertFalse(upcoming.contains("fingerprint"));

            page.click("#refresh");
            page.waitForFunction("() => document.getElementById('view').textContent.includes('TERMINAL')");
            String recovered = page.innerText("#view");
            assertTrue(recovered.contains("已完成"),
                    "a refresh must recover the same terminal interaction");

            reviewStore.markDueReviewsDue(Instant.now().plus(Duration.ofHours(25)));
            page.reload();
            page.waitForFunction("() => document.getElementById('reviews').textContent.includes('可以开始')");
            String ready = page.innerText("#reviews");
            assertTrue(ready.contains("DUE"), "the arrived Review must be Due");
            assertTrue(ready.contains("可以开始"),
                    "the reference UI must switch from upcoming to ready-to-start");
            assertFalse(ready.contains("暂不可操作"));
            assertFalse(ready.contains("15*x^2 - 2"));
            assertFalse(ready.contains("fingerprint"));

            page.click(".start-review");
            page.waitForFunction("() => document.getElementById('task').textContent.includes('设 h(x)')");
            String reviewView = page.innerText("#view");
            assertTrue(reviewView.contains("DELAYED_REVIEW"), "the Review interaction must be in Delayed Review");
            assertTrue(reviewView.contains("REVIEW"), "the Review attempt purpose must be visible");
            assertTrue(reviewView.contains("AWAITING_LEARNER_INPUT"));
            assertFalse(reviewView.contains("8*x^3 - 6*x"), "the Review expected answer must never reach the UI");
            assertFalse(reviewView.contains("fingerprint"));
            assertFalse(reviewView.contains("openstax"));
            assertFalse(page.isDisabled("#derivative"),
                    "the learner must be able to enter answering from the started Due Review");
            page.waitForFunction("() => document.getElementById('reviews').textContent.includes('STARTED')");
            String bound = page.innerText("#reviews");
            assertTrue(bound.contains("STARTED"), "the started Review must show as bound work");
            assertFalse(bound.contains("可以开始"), "a Started Review must no longer offer a start action");
            assertFalse(bound.contains("8*x^3 - 6*x"), "no answer facts in the Review collection after start");
        }
    }
}
