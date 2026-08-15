package cn.lunalhx.ai.kilnai;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

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
        }
    }
}
