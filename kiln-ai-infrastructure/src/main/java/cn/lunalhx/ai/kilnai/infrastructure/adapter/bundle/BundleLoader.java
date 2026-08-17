package cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle;

import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleSlot;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class BundleLoader {

    private static final ObjectMapper YAML = new ObjectMapper(YAMLFactory.builder().build())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public SkillBundleSource load(String pinnedId) {
        int at = pinnedId.lastIndexOf('@');
        if (at <= 0 || at == pinnedId.length() - 1) {
            throw new IllegalArgumentException("invalid pinned bundle id: " + pinnedId);
        }
        String bundleId = pinnedId.substring(0, at);
        String version = pinnedId.substring(at + 1);
        // Each immutable release version lives at its own resource path, so
        // two versions of one Bundle id coexist without ambiguity.
        String resourcePath = "skills/" + pinnedId + "/SKILL.md";
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
            ManifestDto dto = YAML.readValue(split.frontmatter(), ManifestDto.class);
            manifest = dto.toManifest();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("invalid manifest frontmatter in " + resourcePath, exception);
        }
        if (!bundleId.equals(manifest.id())) {
            throw new IllegalArgumentException(
                    "bundle id mismatch: pinned " + bundleId + " but resource declares " + manifest.id());
        }
        if (!version.equals(manifest.version())) {
            throw new IllegalArgumentException(
                    "bundle version mismatch: pinned " + version + " but resource declares " + manifest.version());
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

    private record ManifestDto(
            String schema,
            String id,
            String version,
            String slot,
            String summary,
            List<String> requires_context,
            List<String> output_contribution,
            PermissionsDto permissions,
            CompatibilityDto compatibility,
            List<String> resources
    ) {
        BundleManifest toManifest() {
            return new BundleManifest(
                    schema, id, version, BundleSlot.valueOf(slot.toUpperCase()), summary,
                    requires_context, output_contribution,
                    new BundleManifest.Permissions(permissions == null ? null : permissions.tools()),
                    new BundleManifest.Compatibility(
                            compatibility == null ? null : compatibility.profiles(),
                            compatibility == null ? null : compatibility.response_draft()),
                    resources);
        }
    }

    private record PermissionsDto(List<String> tools) {
    }

    private record CompatibilityDto(List<String> profiles, String response_draft) {
    }
}
