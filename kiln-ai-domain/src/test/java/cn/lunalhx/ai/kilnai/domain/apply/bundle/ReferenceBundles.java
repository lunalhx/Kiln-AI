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
            bundle("apply.task-first", BundleSlot.ACTION, "0.1.0",
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
                    """,
                    List.of("apply"), "apply_generation/v1"),
            bundle("reasoning.rule-application", BundleSlot.REASONING, "0.1.0",
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
                    """,
                    List.of("apply"), "apply_generation/v1"),
            bundle("representation.formal-expression", BundleSlot.REPRESENTATION, "0.1.0",
                    "Render formal-expression tasks unambiguously without imposing answer syntax.",
                    List.of("answer_representation_contract", "learner_locale", "task_blueprint"),
                    List.of(),
                    """
                    # Formal Expression

                    Constrain task rendering so its formal objects and requested
                    answer are unambiguous, without imposing one keyboard syntax
                    or changing correctness assessment.
                    """,
                    List.of("apply"), "apply_generation/v1"),
            bundle("verification.structured-task-contract", BundleSlot.VERIFICATION, "0.1.0",
                    "Require complete private task facts for later validation and assessment.",
                    List.of("answer_representation_contract", "concept_source_pack",
                            "mastery_rubric", "task_blueprint"),
                    List.of(),
                    """
                    # Structured Task Contract

                    Constrain task generation so the Action supplies complete,
                    structured private facts required by the Output Gate,
                    isolated Task Verification, and later assessment.
                    """,
                    List.of("apply"), "apply_generation/v1"),
            bundle("subject.calculus-notation", BundleSlot.SUBJECT, "0.1.0",
                    "Apply the declared derivative function-prime notation for the current fixture.",
                    List.of("learner_locale", "task_blueprint"),
                    List.of(),
                    """
                    # Calculus Notation

                    Apply the current fixture's declared calculus notation
                    convention so its task is unambiguous. Supply notation only;
                    no differentiation facts, solution method, or teaching
                    strategy.
                    """,
                    List.of("apply"), "apply_generation/v1")
    );

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

    private static final List<SkillBundle> RATIONALE_EVALUATION_BUNDLES = List.of(
            bundle("evaluation.rationale-assessment", BundleSlot.EVALUATION, "1.0.0",
                    "Judge a complete learner rationale against supplied task-owned facts.",
                    List.of("task_text", "rationale", "task_rubric", "expected_answer_facts",
                            "source_passages", "learner_locale"),
                    List.of(),
                    """
                    # Rationale Assessment

                    Judge the complete rationale against the supplied Task Rubric,
                    task, expected-answer facts, and approved source passages. Do
                    not use keywords as a shortcut and do not return reasoning.
                    """,
                    List.of("rationale_evaluation"), "rationale_evaluation/v1"),
            bundle("verification.rationale-sufficiency", BundleSlot.VERIFICATION, "1.0.0",
                    "Verify support, task connection, and coherence of one rationale.",
                    List.of("task_text", "rationale", "task_rubric", "expected_answer_facts",
                            "source_passages", "learner_locale"),
                    List.of(),
                    """
                    # Rationale Sufficiency

                    Verify the same three dimensions from the supplied facts.
                    Treat uncertainty as inconclusive and never invent missing
                    support or expose private evaluation details.
                    """,
                    List.of("rationale_evaluation"), "rationale_evaluation/v1")
    );

    private ReferenceBundles() {
    }

    public static SkillBundle bundle(String pinnedId) {
        return java.util.stream.Stream.concat(BUNDLES.stream(),
                        java.util.stream.Stream.concat(EXPLAIN_BUNDLES.stream(),
                                java.util.stream.Stream.concat(TEACH_BACK_BUNDLES.stream(),
                                        RATIONALE_EVALUATION_BUNDLES.stream())))
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

    public static EvaluationBundleStack rationaleEvaluationStack() {
        return new EvaluationBundleStack(RATIONALE_EVALUATION_BUNDLES);
    }

    public static SkillBundle rewrap(BundleManifest manifest, SkillBundle bundle) {
        return new SkillBundle(manifest, bundle.coreMarkdown(), bundle.contentHash());
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
