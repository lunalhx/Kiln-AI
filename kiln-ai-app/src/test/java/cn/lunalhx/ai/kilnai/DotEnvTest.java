package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.infrastructure.adapter.model.OperatorCatalogProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotEnvTest {

    @Test
    void parseIgnoresCommentsAndSupportsExportAndQuotes() {
        Map<String, String> values = DotEnv.parse("""
                # comment
                export OPENAI_API_KEY="sk-test"
                kiln.catalog.strong=openai/gpt-4.1 # inline
                kiln.catalog.small='openai/gpt-4.1-mini'

                ignored
                """);
        assertEquals("sk-test", values.get("OPENAI_API_KEY"));
        assertEquals("openai/gpt-4.1", values.get("kiln.catalog.strong"));
        assertEquals("openai/gpt-4.1-mini", values.get("kiln.catalog.small"));
        assertEquals(3, values.size());
    }

    @Test
    void catalogKeysBindFromParsedEnv() {
        Map<String, String> values = DotEnv.parse("""
                kiln.catalog.strong=openai/gpt-4.1
                kiln.catalog.small=openai/gpt-4.1-mini
                kiln.catalog.tool-budget=16
                kiln.catalog.providers[0].provider-id=openai
                kiln.catalog.providers[0].protocol=openai-compatible
                kiln.catalog.providers[0].endpoint=https://api.openai.com
                kiln.catalog.providers[0].secret-env-var=OPENAI_API_KEY
                kiln.catalog.providers[0].models[0]=gpt-4.1
                kiln.catalog.providers[0].models[1]=gpt-4.1-mini
                """);
        OperatorCatalogProperties properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("kiln.catalog", OperatorCatalogProperties.class)
                .get();
        assertEquals("openai/gpt-4.1", properties.getStrong());
        assertEquals("openai/gpt-4.1-mini", properties.getSmall());
        assertEquals(16, properties.getToolBudget());
        assertEquals(1, properties.getProviders().size());
        assertEquals("openai", properties.getProviders().getFirst().getProviderId());
        assertEquals("openai-compatible", properties.getProviders().getFirst().getProtocol());
        assertEquals("https://api.openai.com", properties.getProviders().getFirst().getEndpoint());
        assertEquals("OPENAI_API_KEY", properties.getProviders().getFirst().getSecretEnvVar());
        assertEquals(List.of("gpt-4.1", "gpt-4.1-mini"), properties.getProviders().getFirst().getModels());
    }

    @Test
    void readFromLoadsExampleThenOverlaysEnv(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("env.example"), """
                OPENAI_API_KEY=
                kiln.catalog.strong=openai/gpt-4.1
                """);
        Files.writeString(root.resolve(".env"), "OPENAI_API_KEY=sk-local\n");
        Path nested = Files.createDirectories(root.resolve("kiln-ai-app"));
        Map<String, Object> values = DotEnv.readFrom(nested);
        assertEquals("sk-local", values.get("OPENAI_API_KEY"));
        assertEquals("openai/gpt-4.1", values.get("kiln.catalog.strong"));
    }

    @Test
    void locateWalksParentsToFindExample(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("env.example"), "OPENAI_API_KEY=\n");
        Path nested = Files.createDirectories(root.resolve("kiln-ai-app"));
        assertEquals(root.resolve("env.example"), DotEnv.locate(nested, "env.example"));
    }

    @Test
    void locateReturnsNullWhenMissing(@TempDir Path dir) {
        assertNull(DotEnv.locate(dir, "env.example"));
        assertTrue(DotEnv.read(dir.resolve("env.example")).isEmpty());
    }
}
