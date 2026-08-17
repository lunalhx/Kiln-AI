# Spec: Apply Profile reference implementation

## Problem Statement

Kiln-AI needs its first genuinely end-to-end Teaching Node Profile. The current runnable spike hard-codes an Apply path, mixes instruction and execution data, exposes a generic model-result envelope, and has no reusable Bundle contract. It cannot safely establish Independent Evidence from a learner-visible task grounded in a curated source.

The learner needs a bounded, no-hint task flow that can begin with a Diagnostic, transition neutrally to a fresh Independent Test, and accept evidence only after a valid response has been assessed. The operator needs auditable source grounding, private answer facts, deterministic interaction boundaries, and regression tests that do not depend on model randomness.

This spec delivers only the Apply reference implementation. It uses one curated calculus fixture: OpenStax *Calculus Volume 1*, section 3.3, for differentiating in-scope polynomial functions with constant, constant-multiple, sum/difference, and power rules.

## Solution

Implement a versioned Apply Profile that compiles an immutable English system prompt from one Profile contract and a frozen five-Bundle Skill Stack. It receives a least-privilege JSON execution context and emits a closed `ApplyGenerationDraft`. The Profile validates the draft, normalizes its proposed expected expression, derives its Task Fingerprint, builds a two-projection Task Package, and exposes a single formal Task Attempt only after pre-delivery Task Verification passes.

Diagnostic and Independent Test use the same Apply Profile and Skill Stack. They differ only by a frozen, versioned Task Blueprint and their subsequent assessment policy. Diagnostic is no-hint and advances neutrally when either the final derivative or an applicable rationale is correct. Independent Test is a fresh, no-hint equivalent task that may create Independent Evidence only when its final derivative is correct and its optional rationale is not clearly contradictory.

The reference uses deterministic gates and bounded isolated model judgments. It never exposes source provenance, expected answers, Fingerprints, model reasoning, feedback, or assessment facts while an attempt is open.

## User Stories

1. As a learner, I want to receive one self-contained polynomial-differentiation task, so that I can demonstrate application without being given a hint or worked method.
2. As a learner, I want the task and answer fields rendered in Chinese, so that I can focus on the mathematics rather than internal system language.
3. As a learner, I want to enter a derivative using conventional plain text, Unicode math, or LaTeX-like text, so that I am not forced into one keyboard syntax.
4. As a learner, I want an optional short rationale field, so that I may show my rule application without being forced to write a proof.
5. As a learner, I want exactly one formal submission per displayed task, so that the task remains an independent attempt rather than an answer-revision exercise.
6. As a learner, I want a passing Diagnostic to lead directly to a new task without correctness feedback, so that no diagnostic information assists the Independent Test.
7. As a learner, I want an Independent Test to be genuinely fresh, so that later evidence reflects a new application rather than recall of a prior task or solution.
8. As a learner, I want no source links, answer keys, rule names, or solution cues while a formal task is open, so that the task remains unassisted.
9. As a learner, I want a correct Diagnostic rationale to count even when my final derivative is not correct, so that the Diagnostic remains a low-stakes signal rather than evidence.
10. As a learner, I want an omitted or incomplete optional rationale not to invalidate a correct Independent-Test derivative, so that the optional field is not a hidden proof requirement.
11. As a learner, I want a clearly contradictory rationale not to be silently ignored, so that Independent Evidence represents a coherent demonstrated answer.
12. As a learner, I want an uncertain mathematical judgment to result in a fresh task rather than a failure label, so that evaluator uncertainty is not treated as my mistake.
13. As a learner, I want a neutral retry-or-leave option if the system cannot prepare a verified task, so that I am not left on an empty or diagnostic-error screen.
14. As an operator, I want the first reference to use only operator-curated OpenStax material, so that source provenance and permissions are controlled in Phase 0.
15. As an operator, I want the raw source, Normalized Source Document, Source Passages, and Concept Source Pack to remain authoritative outside any retrieval index, so that later RAG remains a rebuildable optimization rather than source truth.
16. As an operator, I want every displayed task to retain private source trace, Bundle versions, Blueprint version, and derived Task Fingerprint, so that the run is auditable and freshness can be enforced.
17. As an operator, I want every formal Apply task to pass Output Gate and isolated Task Verification before it reaches a learner, so that generated task text and proposed answer facts receive an independent quality check.
18. As an operator, I want rejected or inconclusive generated tasks discarded before exposure, so that a failed pre-delivery validation never creates a learner Attempt or evidence.
19. As an operator, I want task generation to retry at most once after a non-Source-Gap rejection, so that reliability is bounded in cost and behavior.
20. As an operator, I want Source Gap and exhausted generation to reveal the same neutral learner message while retaining detailed reason codes privately, so that system faults are not presented as learner failure.
21. As a platform developer, I want Profile constraints, Bundle instructions, and response contract in the system layer while execution data is separate JSON, so that source or learner data cannot act as executable instructions.
22. As a platform developer, I want only the Action Bundle to contribute generation-draft fields, so that capability Bundles can be composed without field-merge ambiguity.
23. As a platform developer, I want the Profile, not the model, to own learner fields, events, submission closure, canonical expected answer, and Task Fingerprint, so that important interaction and evidence boundaries cannot be altered by a prompt response.
24. As a platform developer, I want every Apply execution to pin Bundle identities and immutable versions, so that a Task Package can be reproduced and audited.
25. As a platform developer, I want an Independent invocation to receive only the Diagnostic-pass fact and prior exposed Fingerprints, so that raw answers, rationales, conclusions, keys, and feedback cannot leak across the evidence boundary.
26. As a platform developer, I want mathematical answers to preserve raw input and use learner-confirmed canonical expressions for Assessment, so that future formula-editor and OCR transformations do not become authoritative without consent.
27. As a platform developer, I want deterministic mathematical equivalence to return only Proven Equivalent, Proven Not Equivalent, or Cannot Decide, so that unsupported syntax is never guessed to be incorrect.
28. As a platform developer, I want a deterministic Profile Contract Test driven by scripted model fixtures, so that the accepted behavior is stable in CI.
29. As a platform developer, I want an optional real-model smoke test kept separate from the contract test, so that provider compatibility can be observed without making model variance a regression oracle.
30. As a future Profile implementer, I want Apply to establish reusable Bundle, Manifest, execution-context, draft, Task Package, and testing patterns, so that Explain, Retrieve, Teach-back, and Hint can later gain equivalent end-to-end contracts without copying this calculus fixture.

## Implementation Decisions

- Scope is only the Apply reference implementation. Explain, Retrieve, Teach-back, and Hint are not implemented by this spec; each will later require its own Profile Contract Test.
- The reference Concept is “use constant, constant-multiple, sum/difference, and power rules to differentiate a polynomial function.” Product, quotient, chain, trigonometric, exponential, and logarithmic differentiation are excluded.
- Source scope is operator-curated OpenStax *Calculus Volume 1*, section 3.3. Phase 0 uses a manually prepared internal Normalized Source Document and Concept Source Pack. No learner source uploads, RAG, permanent PDF/Markdown adapter, or automated textbook decomposition is included.
- Apply is task-first. It must not explain, hint, reveal a rule, show a worked solution, assess the learner, accept evidence, select routing, or mutate Learning State.
- The Apply Profile is the immutable system-level constitution. Its first-party instructions and the Bundle instructions are English. `learner_locale` controls every learner-visible rendering; the reference fixture uses `zh-CN`.
- Each Apply execution uses this frozen five-Slot Stack: `apply.task-first@0.1.0`, `reasoning.rule-application@0.1.0`, `representation.formal-expression@0.1.0`, `verification.structured-task-contract@0.1.0`, and `subject.calculus-notation@0.1.0`.
- First-party Bundles use `kiln.skill/v1` YAML frontmatter, an immutable semantic ID and SemVer version, a short always-loaded Markdown core, explicit tool permission, context requirements, compatibility, and declared resources. The reference declares no lazy resources and no tools. Evaluation fixtures are not runtime-loadable.
- For this Apply reference, the Profile's fixed composition selects the Stack. Only the Action Slot contributes `ApplyGenerationDraft` fields. Reasoning, representation, verification, and subject Bundles declare no draft fields and constrain the Action's task generation.
- The Apply Profile receives a closed `apply_execution_context/v1` JSON object containing the Concept Contract, Mastery Rubric, Task Blueprint, approved Concept Source Pack passages, novelty exclusions, Answer Representation Contract, and Learner Locale. It does not receive raw Diagnostic answers, rationales, assessment conclusion, feedback, or answer key in an Independent invocation.
- Profile instructions, frozen Bundle cores, activated resources, and the response contract are compiled into namespaced system content. The execution context is a separate user JSON message. Every execution-data string is data, never instruction.
- A generation result is a closed `apply_generation/v1` discriminated union. `task_ready` contains learner task text plus private proposed expected expression, Rubric mapping, source trace, and equivalence declaration. `source_gap` contains only reason code and missing requirement IDs. Unknown fields, learner events, generic private maps, final canonical answers, Fingerprints, and model reasoning are rejected.
- Phase 0 intentionally preserves model-authored learner task text. The Output Gate validates the typed result and normalizes the proposed expected expression, but the Profile does not yet derive a structured TaskSpec, independently solve the stated task, or deterministically render task text.
- The Profile builds `task_package/v1` only after validation. Its learner projection contains locale-rendered task text, a required final-derivative mathematical-expression field over `x`, an optional `理由（可选）` short-text field, legal events, and one formal submission. Its private assessor projection contains canonical expected-answer facts, Rubric mapping, source trace, equivalence declaration, Profile-derived Task Fingerprint, and execution trace.
- The optional rationale label deliberately avoids naming a mathematical rule. The formal task itself uses `f(x) = ...` and requests `f'(x)`; it does not mix derivative notations or show a source, context story, answer choices, proof prompt, multipart task, named-rule cue, hint, feedback, score, or correctness cue.
- Diagnostic and Independent Test share the same Profile and Stack, but use separate frozen `kiln.task-blueprint/v1` artifacts. They differ in Attempt Purpose, assessment-policy reference, and freshness exclusion scope. Independent excludes all previously exposed task and solution Fingerprints.
- The Diagnostic Blueprint permits either a correct final derivative or applicable rationale to advance to a Neutral Transition. Diagnostic creates no Learning Evidence and supplies no correctness, rule, solution, or targeted feedback.
- The Independent Blueprint requires a fresh equivalent task and a correct final derivative. It may create Independent Evidence only after Assessment, response Verification where required, deterministic guards, and evidence acceptance. An omitted or non-substantive rationale is allowed; a clearly contradictory rationale prevents evidence but is not learner failure.
- Mathematical learner input retains raw text or future formula/OCR representations. Assessment uses a learner-confirmed canonical expression. Formula-editor submission confirms authored structure; transformed text and OCR require learner confirmation or correction before Assessment.
- The deterministic Mathematical Equivalence Check returns only Proven Equivalent, Proven Not Equivalent, or Cannot Decide. A proven result cannot be overridden. On Cannot Decide, isolated Assessment and independent Response Verification receive the same original input without seeing one another and must both return equivalent; any other outcome is Inconclusive Assessment.
- Assessment returns only a closed `response_assessment/v1` contract. It separates final-expression judgment from rationale judgment, provides no learner feedback or model reasoning, and cannot award evidence or modify state. The Domain strictly validates raw responses; each Assessment or Response Verification responsibility may make one same-profile repair, and a still-invalid result is Inconclusive rather than a provider outage. Diagnostic passes when either channel passes. Independent requires a passing final-expression channel and a non-blocking rationale state.
- Every formal Diagnostic and Independent task must pass the typed Output Gate and one isolated Task Verification call before learner exposure. The Task Verifier checks answer consistency, Rubric alignment, source grounding, Blueprint compliance, and learner-boundary protection. It returns only `task_verification/v1` pass, reject, or inconclusive checks and closed reason codes; it cannot repair or rewrite a task. A malformed verifier result is an inconclusive verification of that candidate, not a 503.
- Source Gap ends generation immediately. Invalid Output Gate result, Task Verification rejection, or inconclusive verification discards the candidate and permits one fresh full generation cycle. A second failed cycle returns internal Task Generation Exhausted; it creates no Task Attempt or evidence.
- Source Gap and Task Generation Exhausted map to the same learner-safe message: “暂时无法准备一道可验证的题目。请稍后重试。” Before first Flow binding, Start returns that generic 503 outcome without persisting a Flow or source artifact and is retried with the original Idempotency-Key. Once a Flow exists, the same outcome becomes its durable Unavailable Interaction and follows ADR-0069. Source, model, validation, and technical details remain audit-only.
- The implementation replaces obsolete spike paths rather than preserving compatibility layers, including the generic Teaching Result Envelope fields for model-controlled events, generic private artifacts, and hidden reasoning.

## Testing Decisions

- The primary and highest stable acceptance seam is `ApplyProfileContractTest`: an end-to-end Apply flow driven by scripted generation, Task Verification, Assessment, and Response Verification fixtures. It validates externally observable learner projection, typed outputs, gate behavior, Attempt lifecycle, state transition, visibility boundaries, and evidence eligibility rather than internal prompt implementation details.
- One deterministic contract-test seam is preferred over separate Bundle-specific end-to-end seams. Bundle cores may have focused tests only where a contract cannot be covered at the Profile seam.
- The contract test must cover: valid Diagnostic task delivery; atomic failed Start with no durable records; Source Gap; first-candidate verification failure followed by a valid second candidate; malformed Task Verification; two failed candidates; Diagnostic passing via final derivative; Diagnostic passing via applicable rationale; malformed Assessment/Verification repaired once or resulting in Inconclusive; Independent success with omitted/non-substantive rationale; Independent contradiction; Cannot Decide with evaluator disagreement; and least-privilege Independent context.
- Contract assertions must confirm that learners never receive expected answers, source passages or locations, source links, Fingerprints, execution trace, answer key, feedback facts, or model reasoning while a formal attempt is open.
- The test must confirm one formal submission closes the Task Attempt and that subsequent formal testing uses a Fresh Equivalent Task.
- `ApplyProfileLiveSmokeTest` is a separate ephemeral, non-blocking test using the compiled real prompt and an operator-configured model. It must not create Learning Evidence and is not a CI regression oracle.
- Existing test patterns provide prior art: the HTTP spike checks learner-visible/private-field separation and idempotent interaction; Prompt Compiler tests check namespaced prompt sections; Typed Artifact Gate Pipeline tests check passed, repairable, and rejected candidate behavior. The new contract test supersedes the spike's hard-coded task semantics for this reference.

## Out of Scope

- Implementing Explain, Retrieve, Teach-back, Hint, or their Profile Contract Tests.
- RAG, vector stores, embedding retrieval, learner uploads, permanent PDF/Markdown/OCR adapters, and automated textbook Concept extraction.
- The deferred `TaskSpecDraft` hardening: independently solving generated task specifications, formally proving task/answer consistency, and deterministically rendering learner task text.
- Formula editor and handwriting/OCR implementation. Their future learner-confirmation rule is in scope as a boundary, but adapters are not.
- Cross-Flow Learner Memory, authentication, source authoring UI, and source-management workflows.
- New provider/model selection, Token/cost/latency threshold selection, or future Profile dynamic Skill-routing policy.
- Backward compatibility with the current hard-coded spike envelope, Skill representation, or task-flow behavior.

## Further Notes

- All resulting source, Task Package, Task Attempt, assessment, verification, Bundle-version, and execution artifacts must remain auditable in the Artifact Store. Later Nodes receive only explicit least-privilege Context Views.
- RAG is a future derivative of authoritative normalized source artifacts. It must never become the source of truth.
- The current free-text task/answer validation is deliberately a Phase 0 compromise. Mandatory Task Verification mitigates it, but the deferred TaskSpec and deterministic solver remain the known next hardening step.
- Product and architecture discovery for this Apply reference is converged. ADR-0069 through ADR-0071 define the durable unavailable retry, active-work, cancellation, and strict model-contract behavior that extends this reference. The remaining choices are implementation details such as the pending-operation record shape and concrete error DTO.
