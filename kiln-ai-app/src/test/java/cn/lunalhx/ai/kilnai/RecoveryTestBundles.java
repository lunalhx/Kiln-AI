package cn.lunalhx.ai.kilnai;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleManifest;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleSlot;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.BundleStack;
import cn.lunalhx.ai.kilnai.domain.apply.bundle.SkillBundle;

import java.util.List;

/**
 * The synthetic Explain and Teach-back reference bundles of the PostgreSQL
 * recovery contract. The real released bundle files only exist for the Apply
 * stack; the Explain and Teach-back Action stacks of the Learning/Practice
 * reference are test fixtures (like the domain {@code ReferenceBundles}), so
 * the recovery test builds them here to avoid shipping test-only resources as
 * product bundles.
 */
public final class RecoveryTestBundles {

    private RecoveryTestBundles() {
    }

    private static final List<SkillBundle> EXPLAIN_BUNDLES = List.of(
            bundle("explain.worked-example", BundleSlot.ACTION, "1.0.0",
                    "Teach one targeted principle explanation with exactly one complete worked example.",
                    List.of("concept_contract", "mastery_rubric", "pedagogy_intent", "concept_source_pack",
                            "novelty_exclusions", "learner_locale"),
                    List.of("principle_summary", "worked_example", "source_trace", "source_gap"),
                    """
                    # Explain Worked Example

                    Produce one targeted principle explanation in `learner_locale`
                    and exactly one complete worked example whose ordered steps
                    each map to an approved rule from the Concept Contract's
                    included scope. Keep every claim traceable to the approved
                    source passages. Never ask the learner a question, assess,
                    or expose source identities; return Source Gap when the
                    approved material cannot ground the teaching content.
                    """,
                    List.of("explain"), "explain_generation/v1"),
            bundle("subject.calculus-notation", BundleSlot.SUBJECT, "1.0.0",
                    "Apply the declared calculus notation convention in teaching content.",
                    List.of("learner_locale", "concept_contract"),
                    List.of(),
                    """
                    # Calculus Notation

                    Apply the current fixture's declared calculus notation
                    convention so its teaching content is unambiguous. Supply
                    notation only; no differentiation facts, solution method,
                    or teaching strategy.
                    """,
                    List.of("explain", "hint", "teach_back"), null)
    );

    private static final List<SkillBundle> TEACH_BACK_BUNDLES = List.of(
            bundle("teach-back.anchored-explanation", BundleSlot.ACTION, "1.0.0",
                    "Generate one short-text Teach-back task anchored to the exposed Explain or H5 content.",
                    List.of("concept_contract", "mastery_rubric", "pedagogy_intent", "anchor",
                            "learner_locale"),
                    List.of("learner_prompt", "rubric_mapping", "source_trace", "anchor_reference",
                            "source_gap"),
                    """
                    # Teach-back Anchored Explanation

                    Generate exactly one short-text learner task in
                    `learner_locale` that asks the learner to explain, in their
                    own words, the reasoning of the supplied anchor content:
                    which rules apply, why they apply, and how the steps connect
                    to the result. Map every one of the three Rubric dimensions
                    (`rule_identification`, `applicability_explanation`,
                    `steps_result_coherence`) to a Mastery Rubric criterion,
                    reference only the supplied anchor, and ground every source
                    trace entry in the anchor's source trace. Keep learner-visible
                    text free of sources, anchor ids, Rubric internals, expected
                    explanations, solutions, and feedback. Return Source Gap when
                    the approved anchor cannot support a valid task; never fill a
                    gap with general knowledge.
                    """,
                    List.of("teach_back"), "teach_back_generation/v1")
    );

    public static BundleStack explainStack() {
        return new BundleStack(EXPLAIN_BUNDLES);
    }

    /**
     * The frozen Teach-back stack: exactly one reference Action Bundle and
     * the shared immutable {@code subject.calculus-notation@1.0.0}.
     */
    public static BundleStack teachBackStack() {
        return new BundleStack(List.of(
                TEACH_BACK_BUNDLES.get(0),
                EXPLAIN_BUNDLES.get(1)));
    }

    private static SkillBundle bundle(String id, BundleSlot slot, String version, String summary,
            List<String> requiresContext, List<String> outputContribution, String markdown,
            List<String> profiles, String responseDraft) {
        BundleManifest manifest = new BundleManifest(
                "kiln.skill/v1", id, version, slot, summary, requiresContext, outputContribution,
                new BundleManifest.Permissions(List.of()),
                new BundleManifest.Compatibility(profiles, responseDraft),
                List.of());
        return new SkillBundle(manifest, markdown, ApplyHash.sha256Hex(markdown));
    }
}