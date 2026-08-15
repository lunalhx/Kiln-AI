package cn.lunalhx.ai.kilnai.domain.apply.bundle;

import cn.lunalhx.ai.kilnai.domain.apply.ApplyHash;

import java.util.List;

/**
 * Synthetic reference bundles mirroring the released five-Skill Apply stack.
 * Domain tests build stacks without infrastructure: the real SKILL.md files
 * are exercised by the infrastructure loader tests and the app-level smoke
 * test.
 */
public final class ReferenceBundles {

    private static final List<SkillBundle> BUNDLES = List.of(
            bundle("apply.task-first", BundleSlot.ACTION,
                    "Generate one bounded task that elicits application of an approved concept.",
                    List.of("concept_contract", "task_blueprint", "concept_source_pack",
                            "novelty_exclusions", "learner_locale"),
                    List.of("learner_task_text",
                            "private_assessor_facts.expected_answer",
                            "private_assessor_facts.rubric_mapping",
                            "private_assessor_facts.source_trace",
                            "private_assessor_facts.equivalence_declaration",
                            "source_gap"),
                    """
                    # Apply Task-First

                    Generate exactly one bounded, self-contained learner task in
                    `learner_locale` that measures every required Rubric criterion
                    from the approved Concept scope. Provide the private expected
                    answer facts, Rubric mapping, source trace, and equivalence
                    declaration. Keep learner-visible text free of sources,
                    solutions, named methods, hints, feedback, and correctness
                    cues. Return Source Gap when the approved material cannot
                    support a valid task; never fill a gap with general knowledge.
                    """),
            bundle("reasoning.rule-application", BundleSlot.REASONING,
                    "Require observable application of a source-grounded rule without teaching it.",
                    List.of("concept_contract", "mastery_rubric", "task_blueprint"),
                    List.of(),
                    """
                    # Rule Application

                    Constrain task generation so the learner must apply a rule
                    already grounded in the approved Concept scope. Never name,
                    paraphrase, or teach the rule in learner-visible text. Do not
                    invent reasoning criteria outside the supplied Concept
                    Contract and Mastery Rubric.
                    """),
            bundle("representation.formal-expression", BundleSlot.REPRESENTATION,
                    "Render formal-expression tasks unambiguously without imposing answer syntax.",
                    List.of("answer_representation_contract", "learner_locale", "task_blueprint"),
                    List.of(),
                    """
                    # Formal Expression

                    Constrain task rendering so its formal objects and requested
                    answer are unambiguous, without imposing one keyboard syntax
                    or changing correctness assessment.
                    """),
            bundle("verification.structured-task-contract", BundleSlot.VERIFICATION,
                    "Require complete private task facts for later validation and assessment.",
                    List.of("answer_representation_contract", "concept_source_pack",
                            "mastery_rubric", "task_blueprint"),
                    List.of(),
                    """
                    # Structured Task Contract

                    Constrain task generation so the Action supplies complete,
                    structured private facts required by the Output Gate,
                    isolated Task Verification, and later assessment.
                    """),
            bundle("subject.calculus-notation", BundleSlot.SUBJECT,
                    "Apply the declared derivative function-prime notation for the current fixture.",
                    List.of("learner_locale", "task_blueprint"),
                    List.of(),
                    """
                    # Calculus Notation

                    Apply the current fixture's declared calculus notation
                    convention so its task is unambiguous. Supply notation only;
                    no differentiation facts, solution method, or teaching
                    strategy.
                    """)
    );

    private ReferenceBundles() {
    }

    public static SkillBundle bundle(String pinnedId) {
        return BUNDLES.stream()
                .filter(bundle -> pinnedId.equals(bundle.pinnedId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no reference bundle: " + pinnedId));
    }

    public static List<SkillBundle> all() {
        return BUNDLES;
    }

    public static BundleStack stack() {
        return new BundleStack(BUNDLES);
    }

    public static SkillBundle rewrap(BundleManifest manifest, SkillBundle bundle) {
        return new SkillBundle(manifest, bundle.coreMarkdown(), bundle.contentHash());
    }

    private static SkillBundle bundle(String id, BundleSlot slot, String summary,
            List<String> requiresContext, List<String> outputContribution, String markdown) {
        BundleManifest manifest = new BundleManifest(
                "kiln.skill/v1", id, "0.1.0", slot, summary, requiresContext, outputContribution,
                new BundleManifest.Permissions(List.of()),
                new BundleManifest.Compatibility(List.of("apply"), "apply_generation/v1"),
                List.of());
        return new SkillBundle(manifest, markdown, ApplyHash.sha256Hex(markdown));
    }
}
