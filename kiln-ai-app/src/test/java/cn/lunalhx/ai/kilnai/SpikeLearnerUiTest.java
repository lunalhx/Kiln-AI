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
@Import(ScriptedSpikePortsConfiguration.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class SpikeLearnerUiTest {

    @LocalServerPort
    int port;

    @Test
    void uiCompletesPreparedSpikeWithoutPrivateFields() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.navigate("http://127.0.0.1:" + port + "/");
            page.click("#start");
            page.waitForFunction("() => document.getElementById('view').textContent.includes('Percent change')");
            String explained = page.innerText("#view");
            assertFalse(explained.contains("answerKey"));
            page.click("#continue");
            page.waitForFunction("() => document.getElementById('view').textContent.includes('80 to 100')");
            page.click("#submit");
            page.waitForFunction("() => document.getElementById('view').textContent.includes('TERMINAL')");
            String terminal = page.innerText("#view");
            assertTrue(terminal.contains("LEARNING"));
            assertFalse(terminal.contains("answerKey"));
            assertFalse(terminal.contains("hiddenReasoning"));
            page.click("#trace");
            page.waitForFunction("() => document.getElementById('view').textContent.includes('apply.worked-example@1')");
            String trace = page.innerText("#view");
            assertFalse(trace.contains("hiddenReasoning"));
        }
    }
}
