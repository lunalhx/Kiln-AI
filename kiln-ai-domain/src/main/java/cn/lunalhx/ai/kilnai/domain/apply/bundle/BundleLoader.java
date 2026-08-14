package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class BundleLoader {

    private static final ObjectMapper YAML = new ObjectMapper(YAMLFactory.builder().build())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public SkillBundleSource load(String bundleId) {
        String resourcePath = "skills/" + bundleId + "/SKILL.md";
        byte[] fullContent;
        try (InputStream stream = BundleLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("bundle resource not found: " + resourcePath);
            }
            fullContent = stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read bundle " + resourcePath, exception);
        }
        String fileText = new String(fullContent, StandardCharsets.UTF_8);
        Frontmatter split = splitFrontmatter(fileText, bundleId);
        BundleManifest manifest;
        try {
            manifest = YAML.readValue(split.frontmatter(), BundleManifest.class);
        } catch (IOException exception) {
            throw new IllegalStateException("invalid manifest frontmatter in " + resourcePath, exception);
        }
        if (!bundleId.equals(manifest.id())) {
            throw new IllegalArgumentException(
                    "bundle id mismatch: directory " + bundleId + " declares " + manifest.id());
        }
        return new SkillBundleSource(manifest, split.body(), fullContent);
    }

    private Frontmatter splitFrontmatter(String fileText, String bundleId) {
        if (!fileText.startsWith("---\n")) {
            throw new IllegalArgumentException("bundle " + bundleId + " must start with YAML frontmatter");
        }
        int end = fileText.indexOf("\n---", 4);
        if (end < 0) {
            throw new IllegalArgumentException("bundle " + bundleId + " has unterminated frontmatter");
        }
        String frontmatter = fileText.substring(4, end);
        String body = fileText.substring(end + 4);
        if (body.startsWith("\n")) {
            body = body.substring(1);
        }
        return new Frontmatter(frontmatter, body);
    }

    private record Frontmatter(String frontmatter, String body) {
    }
}
