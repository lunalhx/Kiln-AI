package cn.lunalhx.ai.kilnai.infrastructure.adapter.bundle;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleSlot;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleLoaderTest {

    private static final List<String> REFERENCE_PINS = List.of(
            "apply.task-first@0.1.0",
            "reasoning.rule-application@0.1.0",
            "representation.formal-expression@0.1.0",
            "verification.structured-task-contract@0.1.0",
            "subject.calculus-notation@0.1.0"
    );

    private final BundleLoader loader = new BundleLoader();

    @Test
    void loadsAllFiveReferenceBundlesWithDeclaredManifestFields() {
        for (String pin : REFERENCE_PINS) {
            SkillBundleSource source = loader.load(pin);
            BundleManifest manifest = source.manifest();
            assertEquals("kiln.skill/v1", manifest.schema());
            assertEquals(pin, manifest.pinnedId());
            assertEquals("0.1.0", manifest.version());
            assertFalse(manifest.summary().isBlank());
            assertFalse(manifest.requiresContext().isEmpty(), pin + " must declare context requirements");
            assertTrue(manifest.compatibility().profiles().contains("apply"),
                    pin + " must declare Apply profile compatibility");
            assertEquals("apply_generation/v1", manifest.compatibility().responseDraft());
            assertTrue(manifest.permissions().tools().isEmpty(), pin + " must declare tools: []");
            assertTrue(manifest.resources().isEmpty(), pin + " declares no lazy resources");
            assertFalse(source.coreMarkdown().isBlank());
        }
    }

    @Test
    void onlyTheActionBundleContributesDraftFields() {
        for (String pin : REFERENCE_PINS) {
            BundleManifest manifest = loader.load(pin).manifest();
            if (manifest.slot() == BundleSlot.ACTION) {
                assertFalse(manifest.outputContribution().isEmpty());
                assertTrue(manifest.outputContribution().contains("learner_task_text"));
            } else {
                assertTrue(manifest.outputContribution().isEmpty(),
                        pin + " must declare output_contribution: []");
            }
        }
    }

    @Test
    void bundlesOccupyTheirDeclaredSlots() {
        assertEquals(BundleSlot.ACTION, loader.load("apply.task-first@0.1.0").manifest().slot());
        assertEquals(BundleSlot.REASONING, loader.load("reasoning.rule-application@0.1.0").manifest().slot());
        assertEquals(BundleSlot.REPRESENTATION, loader.load("representation.formal-expression@0.1.0").manifest().slot());
        assertEquals(BundleSlot.VERIFICATION, loader.load("verification.structured-task-contract@0.1.0").manifest().slot());
        assertEquals(BundleSlot.SUBJECT, loader.load("subject.calculus-notation@0.1.0").manifest().slot());
    }

    @Test
    void toBundleHashesTheWholeReleaseFile() throws Exception {
        SkillBundle bundle = loader.load("apply.task-first@0.1.0").toBundle();

        String expected;
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("skills/apply.task-first@0.1.0/SKILL.md")) {
            expected = ApplyHash.sha256Hex(stream.readAllBytes());
        }
        assertEquals(expected, bundle.contentHash());
    }

    @Test
    void contentHashDiffersBetweenDistinctBundles() {
        SkillBundle action = loader.load("apply.task-first@0.1.0").toBundle();
        SkillBundle subject = loader.load("subject.calculus-notation@0.1.0").toBundle();
        assertNotEquals(action.contentHash(), subject.contentHash());
    }

    @Test
    void rejectsPinnedIdWithoutVersion() {
        assertThrows(IllegalArgumentException.class, () -> loader.load("apply.task-first"));
    }

    @Test
    void rejectsPinnedVersionMismatch() {
        assertThrows(IllegalArgumentException.class, () -> loader.load("apply.task-first@9.9.9"));
    }

    @Test
    void rejectsUnknownBundleId() {
        assertThrows(IllegalArgumentException.class, () -> loader.load("no.such-bundle@0.1.0"));
    }
}
