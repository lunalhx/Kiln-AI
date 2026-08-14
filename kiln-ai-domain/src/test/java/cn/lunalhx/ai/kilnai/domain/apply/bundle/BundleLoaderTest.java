package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.skill.CapabilityGap;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleLoaderTest {

    private static final List<String> REFERENCE_BUNDLE_IDS = List.of(
            "apply.task-first",
            "reasoning.rule-application",
            "representation.formal-expression",
            "verification.structured-task-contract",
            "subject.calculus-notation"
    );

    private final BundleLoader loader = new BundleLoader();

    @Test
    void loadsAllFiveReferenceBundlesWithDeclaredManifestFields() {
        for (String bundleId : REFERENCE_BUNDLE_IDS) {
            SkillBundleSource source = loader.load(bundleId);
            BundleManifest manifest = source.manifest();
            assertEquals("kiln.skill/v1", manifest.schema());
            assertEquals(bundleId, manifest.id());
            assertEquals("0.1.0", manifest.version());
            assertEquals(bundleId + "@0.1.0", manifest.pinnedId());
            assertFalse(manifest.summary().isBlank());
            assertFalse(manifest.requiresContext().isEmpty(), bundleId + " must declare context requirements");
            assertTrue(manifest.compatibility().profiles().contains("apply"),
                    bundleId + " must declare Apply profile compatibility");
            assertEquals("apply_generation/v1", manifest.compatibility().responseDraft());
            assertTrue(manifest.permissions().tools().isEmpty(), bundleId + " must declare tools: []");
            assertTrue(manifest.resources().isEmpty(), bundleId + " declares no lazy resources");
            assertFalse(source.coreMarkdown().isBlank());
        }
    }

    @Test
    void onlyTheActionBundleContributesDraftFields() {
        for (String bundleId : REFERENCE_BUNDLE_IDS) {
            BundleManifest manifest = loader.load(bundleId).manifest();
            if (manifest.slot() == BundleSlot.ACTION) {
                assertFalse(manifest.outputContribution().isEmpty());
                assertTrue(manifest.outputContribution().contains("learner_task_text"));
            } else {
                assertTrue(manifest.outputContribution().isEmpty(),
                        bundleId + " must declare output_contribution: []");
            }
        }
    }

    @Test
    void bundlesOccupyTheirDeclaredSlots() {
        assertEquals(BundleSlot.ACTION, loader.load("apply.task-first").manifest().slot());
        assertEquals(BundleSlot.REASONING, loader.load("reasoning.rule-application").manifest().slot());
        assertEquals(BundleSlot.REPRESENTATION, loader.load("representation.formal-expression").manifest().slot());
        assertEquals(BundleSlot.VERIFICATION, loader.load("verification.structured-task-contract").manifest().slot());
        assertEquals(BundleSlot.SUBJECT, loader.load("subject.calculus-notation").manifest().slot());
    }

    @Test
    void registryComputesTheContentHashOfTheWholeRelease() throws Exception {
        BundleRegistry registry = new BundleRegistry();
        registry.register(loader.load("apply.task-first"));
        SkillBundle bundle = registry.resolve("apply.task-first", "0.1.0");

        String expected;
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("skills/apply.task-first/SKILL.md")) {
            expected = ApplyHash.sha256Hex(stream.readAllBytes());
        }
        assertEquals(expected, bundle.contentHash());

        SkillBundle reloaded = new BundleRegistry().register(loader.load("apply.task-first"));
        assertEquals(bundle.contentHash(), reloaded.contentHash());
    }

    @Test
    void contentHashDiffersBetweenDistinctBundles() {
        SkillBundleSource action = loader.load("apply.task-first");
        SkillBundleSource subject = loader.load("subject.calculus-notation");
        assertNotEquals(
                ApplyHash.sha256Hex(action.fullFileContent()),
                ApplyHash.sha256Hex(subject.fullFileContent()));
    }

    @Test
    void registryRejectsDuplicateRegistration() {
        BundleRegistry registry = new BundleRegistry();
        registry.register(loader.load("apply.task-first"));
        assertThrows(IllegalArgumentException.class, () -> registry.register(loader.load("apply.task-first")));
    }

    @Test
    void registryReportsUnregisteredVersionsAsCapabilityGap() {
        BundleRegistry registry = new BundleRegistry();
        registry.register(loader.load("apply.task-first"));
        assertThrows(CapabilityGap.class, () -> registry.resolve("apply.task-first", "9.9.9"));
    }
}
