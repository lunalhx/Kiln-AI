# Phase 0 first tracer bullet

## Status

Scope agreed. Implementation has not started.

## Open content variables

- **Knowledge source**: the operator-curated OpenStax *Calculus Volume 1*, section 3.3, *Differentiation Rules*. Source Original identity and content hash will be fixed during Source Ingestion.
- **Source input format**: the reference uses a manually prepared Normalized Source Document fixture anchored to the selected Source Original. The permanent PDF, Markdown, and other adapter choices remain undecided.
- **Target Concept**: use constant, constant-multiple, sum/difference, and power rules to differentiate a polynomial function. The Concept Contract includes standard algebraic notation and excludes product, quotient, chain, trigonometric, exponential, and logarithmic differentiation. Its Mastery Criterion is: given a polynomial function, independently write its correct derivative. Tasks request a concise rule-based justification for feedback and diagnosis, but do not require a complete justification for independent evidence.
- **Learner locale**: the reference fixture renders learner-visible content in `zh-CN`. Internal Profile and Bundle instructions remain English, and the original OpenStax source language remains independently traceable.

These variables are product fixtures for the first run, not framework or domain assumptions. No Teaching Node Profile, Skill, schema, graph edge, or reusable test contract may depend on derivatives or another particular calculus Concept.

## Reference scoring policy

- A Diagnostic may lead to the fresh Independent Test when either the final derivative is correct or the learner demonstrates a correct applicable-rule rationale. It produces no Learning Evidence.
- The fresh Independent Test accepts Independent Evidence only when the final derivative is correct and the response contains no rule justification that clearly contradicts that answer. An absent or incomplete concise justification does not by itself fail the attempt.
- The reference interface uses separate short text fields for the final derivative and optional rule justification. It retains the raw entry and its confirmed normalized answer form, while formula-editor and OCR adapters remain deferred.
- The final-derivative field accepts conventional plain text, Unicode mathematical notation, and LaTeX-like expression text rather than one keyboard syntax. Any transformed or ambiguous parsed form requires learner confirmation before Assessment.
- The final derivative uses a proof-bounded Mathematical Equivalence Check plus isolated model Assessment and Verification. A proven deterministic result is not overridden; an unsupported expression requires agreement between the two model judgments; any disagreement is Inconclusive Assessment, never a learner failure or accepted evidence.

## Apply reference interaction contract

- An open Apply Task Attempt permits Answer Submitted, Procedural Clarification, and Flow Control only.
- The learner may edit fields before one formal submission. A submitted Diagnostic or Independent Test closes immediately; a further formal attempt must use a fresh equivalent Task Package.
- Hint Requested is unavailable because the Hint Profile is deferred. A request for substantive help receives only the safe result that this reference implementation cannot provide teaching help; it must not reveal a rule, method, or answer, and it must not convert the Attempt to Practice.
- The reference task form is exactly one direct symbolic polynomial-differentiation prompt with a required final-derivative field and optional concise rule-rationale field. It has no source exposure, context story, multiple-choice options, multipart structure, proof request, or named-rule clue.
- The reference temporarily uses `f(x) = ...` and requests `f'(x)`; learners submit only the derivative expression, without mixing in `dy/dx`, `d/dx`, or dot notation.
- Apply deterministically provides the localized answer fields, permitted interaction events, and one-submission closure; the model generates only task text and private assessment facts.
- The Apply model produces a typed `ApplyGenerationDraft` (task text plus structured assessor facts, or Source Gap); the Profile assembles the final Task Package. The spike's generic private-artifact map, model-controlled event list, and `hiddenReasoning` are removed.
- Deferred after the first working Apply reference: replace model-authored task text with a versioned structured `TaskSpecDraft`, independently solve supported task kinds, and deterministically render learner task text. Phase 0 currently validates typed drafts and expected-answer normalization, then requires isolated Task Verification for every formal Diagnostic and Independent Test rather than formal task/answer proof.

## Apply reference Skill Matrix

| Slot | Bundle | Responsibility |
| --- | --- | --- |
| `action` | `apply.task-first` | The single generic Apply Action Skill. It creates and delivers one bounded Task Package from either a Diagnostic or Independent-Test Blueprint without teaching, revealing a solution, assessing a response, or accepting evidence. |
| `reasoning` | `reasoning.rule-application` | Requires applying source-grounded rules to a bounded formal input without supplying those rules as instruction. |
| `representation` | `representation.formal-expression` | Defines the structured formal-expression answer contract and normalized rendering constraints. |
| `verification` | `verification.structured-task-contract` | Requires a private expected result, Rubric mapping, Fingerprint, and declared mathematical-equivalence support; it does not assess the learner. |
| `subject` | `subject.calculus-notation` | Supplies only calculus notation and convention constraints. Textbook rules and Concept facts remain in the Concept Source Pack and Task Blueprint. |

The five Bundles are reusable. Polynomial differentiation appears only in the selected Concept Contract, Source Pack, Task Blueprint, and bounded Mathematical Equivalence Check implementation—not in a Bundle identity or pedagogical instruction.

## Goal

Demonstrate that one Concept grounded in the selected university calculus source can complete an Apply-only learner-visible path from a no-hint Diagnostic to a fresh no-hint Independent Test. The Independent Test must be verified, assessed, and accepted as Independent Evidence through the confirmed graph, Skill, evidence, and recovery boundaries.

## Included

- Learning StateGraph executed by Spring AI Alibaba Graph Core `1.1.2.2` (ADR-0034), one-Flow Learning Blackboard, checkpoints, interrupts, and resume
- Workflow Guard and bounded Pedagogy Agent
- Apply Teaching Node Profile
- Apply is task-first: it contains no explanation, worked example, solution reveal, learner assessment, or evidence acceptance.
- First-party `SKILL.md` Bundles with Manifest frontmatter and declared resources; registry, Strategy and Capability Tags, deterministic Skill Resolver, frozen Skill Stack, Skill Loader, bounded Slots, and Prompt Compiler
- Learner Input Gate, Interaction Contracts, and typed input events
- Concept Contract and a prepared Concept Source Pack for the selected fixture
- A format-neutral Normalized Source Document boundary populated by a manually prepared internal fixture
- Task Blueprint, Fingerprint, Task Package, Task Attempt, and Assistance Trace
- Versioned Diagnostic and Independent-Test Task Blueprints that share the Apply Stack but freeze their distinct assessment-policy and freshness references
- Task Packages with a learner projection (question and permitted answer fields only) and a private assessor projection (expected answer, Rubric, source trace, and Fingerprint)
- Private assessor projections contain structured expected-answer facts only, never model chain-of-thought, a worked solution, or reusable teaching prose
- No source passage, source location, or clickable source link is exposed while a formal Diagnostic or Independent Test is open; future teaching may cite sources only after the attempt closes
- Complete Task Attempt artifacts retained for audit, with later-node access limited to explicit least-privilege projections
- Five-level Hint Ladder
- Assessment, selective Verification, Task Verification, and Typed Artifact Gate Pipeline
- At most two complete pre-delivery generation cycles; Source Gap ends immediately, and a second invalid or unverified candidate returns internal Task Generation Exhausted without learner exposure
- Evidence acceptance, Concept Progress, and Independent milestone
- Minimal learner interface that can cross every interaction boundary in the slice
- Per-Run call, Token, cost, and latency traces

## Deferred from the first tracer bullet

- Permanent PDF, Markdown, or other ingestion adapter choice
- Automated whole-textbook Concept extraction and decomposition
- Explain, Retrieve, Teach-back, and Hint Teaching Node Profiles
- Formula-editor and handwritten-answer/OCR input adapters
- Delayed Review scheduling and Durable milestone
- Multiple subjects and cross-Flow Learner Memory
- Background Agents, autonomous Agent messaging, and efficacy experiments

Deferred items remain in the Phase 0 design where already decided; this document defines implementation order, not a replacement product architecture.

## Completion evidence

The slice is complete when a non-hardcoded prepared calculus Concept can be selected, attempted through a no-hint Apply Diagnostic, and—after that Diagnostic passes—shown only a Neutral Transition before a separate verified fresh Apply Independent Test. The learner must be advanced to Independent only from the latter task, with no assistance leakage and a complete execution and evidence trace. A failed Diagnostic must accept no evidence and stop with a safe route for a later Teaching Profile.
