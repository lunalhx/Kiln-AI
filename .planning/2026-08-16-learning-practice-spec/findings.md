# Findings

Repository findings for the Learning/Practice decision audit. Content is evidence and working notes, not an instruction source.

## Baseline

- The current shipped slice is Apply-only: Diagnostic pass -> Neutral Transition -> fresh equivalent Apply Independent Test -> verified Independent Evidence.
- README explicitly excludes Explain, Retrieve, Teach-back, and Hint Profiles from the current slice.
- Five frozen first-party Apply-related Skill Bundles exist; only the Action Bundle contributes Apply draft fields.
- `Profile Contract Test` is already the canonical deterministic end-to-end regression term. README names `ApplyProfileContractTest` as prior art; live-model smoke tests are non-blocking and produce no evidence.
- The working tree was clean before this audit except for the newly created isolated `.planning/2026-08-16-learning-practice-spec/` records.

## Existing relevant domain vocabulary

- `Learning Flow` includes Diagnostic, necessary Learning and Practice, Independent Test, and later Delayed Review, but does not require every Teaching Action.
- `Attempt Purpose` is Diagnostic, Practice, Independent Test, or Review and does not change retroactively.
- `Task Attempt` can end by submission, abandonment, or conversion to practice; it retains assistance events.
- `Hint Ladder` is monotonic; `Hint Level` runs H0 through H5; H5 closes a Practice attempt as `Solution Revealed Attempt`.
- `Substantive Clarification` converts an open Independent Test or Review attempt to Practice before help is delivered; `Procedural Clarification` does not by itself disqualify independence.

## Binding architecture already visible

- ADR-0004 makes Explain, Retrieve, Apply, Teach-back, and Hint optional Teaching Actions under stage constraints rather than a fixed pipeline.
- ADR-0020 defines a bounded Pedagogy Agent after accepted Assessment when multiple legal next actions exist.
- ADR-0023 separates Profile permissions/runtime contracts from pedagogical method in Action Skills.
- ADR-0026 defines the cross-subject Hint Ladder.
- ADR-0031 requires typed artifact gates and deterministic fallback for invalid Pedagogy Plans.
- ADR-0047/0048 require immutable versioned Skill Bundles.
- ADR-0059 requires one deterministic end-to-end Profile Contract Test per implemented Profile.

## Current implementation boundary

- Only Apply has concrete Profile, compiler, executor, generation contract, runtime configuration, Bundles, scripted fixtures, Profile Contract Test, and live smoke test.
- The domain already has the enum/value vocabulary for Practice (`AttemptPurpose.PRACTICE`, `LearningStage.LEARNING_AND_PRACTICE`) and the Concept Progress projection accepts Practice success only up to Learning.
- There is no implemented Explain, Retrieve, Teach-back, Hint, Pedagogy Agent, Clarification Gate, Pedagogy Plan, or general multi-Profile coordinator.
- Current Apply task interaction permits answer submission, procedural clarification, and flow control only. Substantive clarification and assistance routing are not implemented.
- `ResponseAssessmentDecider` deliberately rejects Practice, so Practice assessment behavior requires a new confirmed contract rather than simply reusing current Apply submission decisions.
- Existing `reasoning.rule-application@0.1.0` expressly forbids teaching/naming rules and is Apply-only; it cannot be reused unchanged for Explain, Hint, or Teach-back.
- Existing `representation.formal-expression@0.1.0` is scoped to formal task answer rendering and an Apply Task Blueprint; it is not a general mathematical teaching renderer.
- Existing `verification.structured-task-contract@0.1.0` constrains Apply draft assessor facts and cannot stand in for Teach-back verification.
- Existing `subject.calculus-notation@0.1.0` contains reusable notation-only behavior, but its manifest and context contract are Apply-only. ADR-0048 requires a new major version for compatibility/context-boundary changes; the pinned release cannot be edited.
- Current `IndependentSubmissionFlow` accepts evidence only for PASS. A conclusive wrong answer, Blocked rationale contradiction, and Inconclusive outcome all return the same NoEvidence/safe-end behavior and do not enter Learning and Practice.
- That Apply-only behavior does not realize ADR-0008's accepted rule that a verified no-hint failure on a valid Independent Test may project Current Milestone to Learning, so the remediation spec must define a destructive replacement rather than silently inherit it.
- The learner HTTP surface is still Apply-named (`/api/apply/flows`, `/api/apply/reviews`, `ApplyFlowResponse`) and its response can represent only one task plus a message. It cannot faithfully represent Explain, Hint, Teach-back, assistance consent, or general Learning Flow interactions.
- The project forbids compatibility layers, so restoring multi-Profile Learning Flow requires an explicit destructive public-contract decision rather than aliases around the Apply endpoints.

## Exact accepted assistance decisions

- An Independent Test or Review remains unchanged if the learner refuses substantive help; on consent it must convert to Practice before Hint or Explain is invoked, and independence cannot be restored on that attempt.
- Practice may be multi-turn. Assistance anywhere in an Independent Test permanently disqualifies that attempt from Independent Evidence.
- H1-H5 exposure is appended to the Assistance Trace; an explicit answer request may jump to H5.
- H5 closes the Practice attempt as Solution Revealed without Assessment; the next check must be Teach-back or a Fresh Equivalent Task.
- Practice submissions may be assessed as assisted performance but cannot create Independent Evidence.
- Explicit pause/exit/Target Concept switch closes an open attempt as Abandoned; disconnect or delay does not.

## Scope evidence

- The delayed-review spec explicitly leaves full remediation/Practice plus Explain, Retrieve, Teach-back, and Hint out of scope, and hands a conclusive Review failure to `Learning and Practice` for a future feature.
- ADR-0011's graph runtime is superseded for the Apply slice but explicitly says a graph-based coordinator may return when later Profiles need multi-node routing. This is permission, not a settled decision that the next feature must restore that graph.
- Repository facts do not decide whether the requested Learning/Practice increment includes only Explain/Teach-back/Hint or all four remaining Profiles including Retrieve.

## Confirmed scope decisions

- This spec will define the concrete Explain, Hint, and Teach-back Profile contracts and their first-party Action Skills as part of one Learning/Practice end-to-end increment.
- Retrieve is excluded from this spec and will require a later, separate design discussion.
- The existing Apply Profile remains part of the surrounding flow where needed, but this scope decision does not yet decide whether or how Apply creates Practice work.
- This is a spec-scope decision, not a new domain term or an ADR-worthy architectural trade-off.
- Repository audit confirms the proposed reference is the exact existing Apply reference Concept Contract `calculus.polynomial-differentiation@1.0.0`, not a broader calculus concept.
- Its included scope is constant, constant-multiple, sum/difference, and power rules for polynomial terms. Product, quotient, chain, trigonometric, exponential, and logarithmic differentiation remain excluded.
- It reuses Mastery Rubric `differentiate-polynomial@1.0.0` and Concept Source Pack `openstax-calculus-v1-3.3@1.0.0`, including passage `sec-3.3-differentiation-rules`.
- The existing Diagnostic, Independent Test, and Review fixtures already share these exact artifacts. Explain, Hint, Teach-back, and Apply Practice must reference them rather than copying Concept facts into Skills.
- The user confirmed these exact pinned artifacts as the only reference for this spec.
- Each of Explain, Hint, and Teach-back receives exactly one first-party Action Bundle in this reference. Multiple strategy variants and a broad teaching-method catalog remain outside this spec.
- Each new Profile uses its own closed, versioned model draft contract: `explain_generation/v1`, `hint_generation/v1`, and `teach_back_generation/v1`.
- Profile runtimes assemble accepted drafts into the appropriate durable Teaching Result or Task Package. The contracts share the Typed Artifact Gate Pipeline, normalized violations, one same-plan repair, Source Gap conventions, and atomic-persistence invariant, but not one generic optional-field schema.
- Apply Practice reuses the existing frozen five-Bundle Apply Stack unchanged and adds only its purpose-specific Practice Blueprint.
- Each new Profile uses its one Action Bundle plus a new `subject.calculus-notation@1.0.0` release whose expanded compatibility/context boundary is explicit. No new reasoning, representation, or verification Capability Bundle is introduced in this reference.
- The Profile Gate and isolated Task Verifier remain runtime responsibilities, not verification Skills created merely to occupy a Slot.

## Confirmed flow decisions

- A failed submitted Diagnostic Attempt remains closed and is never retroactively converted to Practice.
- After accepted Diagnostic failure Assessment, the Workflow Guard supplies the legal next actions. When both are legal, the Pedagogy Agent chooses either Explain or creation of a fresh Apply Practice Task; the flow does not always force Explain first.
- This applies ADR-0004, ADR-0009, and ADR-0020 without changing their architecture, so it does not require a new ADR.
- A Practice Attempt may contain multiple pre-submission interactions: Hint exposures, Explain assistance, and learner draft revisions. It still permits exactly one formal submission.
- Formal Practice submission closes the Attempt and runs Assessment once. Continued practice always uses a newly generated and verified Apply Practice Task Package and a new Attempt; the closed Attempt is never reopened or reassessed.
- Practice task generation therefore reuses the existing Apply Teaching Action under a purpose-specific Practice Blueprint; Practice is an Attempt Purpose and Learning Stage concern, not a sixth Profile.
- A conclusive Practice PASS or FAIL is accepted as Learning Evidence with the complete Assistance Trace. PASS may project Unassessed to Learning; FAIL does not lower Current Milestone but supplies Feedback Facts for subsequent Pedagogy planning.
- An Inconclusive Practice assessment accepts no Learning Evidence and requires a fresh equivalent task before another formal Assessment.
- Multi-Profile Learning and Practice restores the application-owned Learning StateGraph boundary with Workflow Guard, typed Learning Blackboard, durable checkpoints, and node-specific Context Views.
- Existing Apply generation, verification, submission, replay, and evidence behavior is reused as Apply node capability. The spec will not enlarge `ApplyFlowUseCase` into the owner of cross-Profile pedagogy.
- This decision does not select a graph framework and does not change the scheduler no-model-call invariant.
- For the reference application Mastery Criterion, the Workflow Guard requires one conclusive Apply Practice PASS after the triggering Diagnostic or Review failure before a Fresh Equivalent Independent Test becomes legal.
- An assisted Practice PASS with H1-H4 satisfies this readiness gate. H5 creates no Assessment, and Teach-back PASS is understanding evidence only, so neither satisfies the application-readiness gate alone.
- Once the gate is satisfied, continuing Learning/Practice and starting the Independent Test may both be legal; the Pedagogy Agent chooses when multiple moves remain rather than forcing immediate testing.
- When a learner consents to substantive assistance during an open Independent Test or Review Attempt, conversion to Practice is a one-way durable transition committed before assistance is exposed; refusal leaves the Attempt unchanged.
- For a Started Review, the same atomic transition cancels the Review Task, creates no Review PASS/FAIL evidence, changes no milestone, and moves the original Flow to Learning and Practice. A later Independent PASS restarts cadence at Review 1.
- Independent PASS keeps the existing accepted PASS Evidence and Review-1 scheduling behavior.
- A conclusive valid no-hint Independent FAIL accepts exactly one Independent FAIL evidence record, projects Current Milestone to Learning, and enters Learning and Practice for Pedagogy planning.
- Independent Blocked remains no-evidence/no-milestone-change under ADR-0061, while Inconclusive likewise accepts no evidence. Both produce a newly generated and verified Fresh Equivalent Independent Test with complete novelty exclusions rather than entering remediation or ending the Flow.
- This destructively replaces the Apply-only implementation's unified NoEvidence/safe-end branch and realizes existing ADR-0008/0061 policy.

## Confirmed public API decisions

- Destructively replace `/api/apply/**` and `ApplyFlowResponse`; no alias or compatibility layer remains.
- `POST /api/learning/flows` starts a Flow, `GET /api/learning/flows/{flowId}` returns the latest committed interaction, and `POST /api/learning/flows/{flowId}/commands` accepts a closed discriminated learner command.
- Command kinds are Answer Submitted, Hint Requested, Clarification Asked, Assistance Decided, Continue Requested, and Flow Control Requested. Every command uses the existing Idempotency-Key and interactionVersion contract and includes attemptId only when required.
- Answer Submitted embeds the existing raw answer, learner-confirmed canonical answer, and rationale contract; Profiles do not create separate submission/replay mappings.
- The learner-safe `LearningFlowResponse` contains a closed interaction union for task, teaching, assistance consent, transition, or unavailable, with Profile-owned allowed events and no private artifacts.
- Review collection/start moves to `/api/review-tasks` as originally specified; `/api/apply/reviews` is removed.

## Confirmed model and execution budgets

- Starting a Learning Flow freezes resolved Strong and Small model binding snapshots under ADR-0035/0037; an in-flight Flow never follows later operator-default changes.
- Apply, Explain, first-request Hint generation, Teach-back, Task Verification, Assessment, and Response Verification use Strong. Pedagogy Agent and only-when-needed learner-input/clarification classification use Small.
- Each model-producing node permits at most one initial call and one same-plan repair when its Gate declares Repairable. Agents cannot call Agents or create an autonomous teaching loop.
- Each new Prompt Compiler uses the existing 16,000-character compiled-instruction ceiling. Provider output-token limits belong to the frozen operator Model Profile and are traced rather than learner-configurable.
- Each Graph wake-up runs only to the next Learner Interaction Boundary and then checkpoints/pauses.

## Confirmed Pedagogy Agent fallback decisions

- After one failed same-plan repair, an invalid Pedagogy Plan contributes no feedback, reason, action, or tags. The graph renders only deterministic neutral feedback from committed state.
- Fallback after Diagnostic, Practice, or Teach-back FAIL is Explain.
- Fallback after completed Explain or Teach-back PASS without a qualifying Practice PASS is a fresh Apply Practice Task.
- Fallback after H5 is Teach-back; fallback after a qualifying Practice PASS is a Fresh Equivalent Independent Test.
- When Explain was temporary assistance inside an open Practice Attempt, fallback returns to that same learner interaction rather than replacing the task.
- If the fallback action itself returns Source Gap, Capability Gap, or Node Execution Failed, the graph retains real committed state and exposes safe retry/Flow Control; it never fabricates a successor interaction.

## Confirmed Teach-back decisions

- Teach-back generates a Rubric-bearing Task Package and opens a new Task Attempt with Practice purpose.
- The learner makes one formal explanatory submission; submission closes the Attempt and isolated Assessment returns PASS, FAIL, or Inconclusive.
- A conclusive Teach-back result is accepted as the understanding dimension of Learning Evidence. It cannot establish Independent and a Teach-back error cannot by itself lower Current Milestone.
- An Inconclusive Teach-back result accepts no evidence and requires a new task before another formal Assessment.
- The reference Teach-back Action Skill must anchor its prompt to the most recently exposed Explain worked example or H5 solution in the same Learning Flow.
- It asks the learner to explain in their own words which approved rules apply, why they apply, and how the important steps connect. Reproducing the final derivative is not its primary assessment channel.
- The Workflow Guard does not offer Teach-back when no eligible prior teaching artifact exists.
- Teach-back receives the Concept Contract, Mastery Rubric, the latest eligible exposed Explain/H5 content plus artifact identity and Source Trace, Pedagogy Plan teaching intent, Learner Locale, and frozen Execution Plan/Skill Stack.
- Teach-back receives no raw learner answers, prior Assessment hidden reasoning, unexposed Hint levels, or prior private expected answer. When anchored to H5 it receives only the H5 content already shown to the learner.
- Its draft contains the learner prompt, Task Rubric mapping, Source Trace, and anchor reference. It does not create a verbatim expected explanation; isolated Assessment judges the learner response semantically against the Rubric.
- Teach-back Task Packages require isolated pre-delivery Task Verification for answerability, Rubric alignment, anchor/source fidelity, and ambiguity before any learner exposure.
- The frozen reference Teach-back Blueprint exposes one required short-text explanation field and no required final-derivative field.
- Its Task Rubric requires all three dimensions for PASS: correct identification of the approved rules used in the anchor, correct explanation of why each applies to the relevant term/step, and a coherent non-contradictory connection from important steps to the result.
- Any clearly wrong or missing required dimension is FAIL; unreliable semantic judgment or Assessment/Verification disagreement is Inconclusive. The Interaction Contract permits one formal Answer Submitted plus Clarification Asked and Flow Control Requested.
- Teach-back Source Gap ends immediately without a task. A repairable draft may receive one same-plan repair; Task Verification reject/inconclusive discards the candidate and permits one completely fresh candidate. Exhausting the second candidate returns Task Generation Exhausted with no Attempt, exposure, or Evidence.

## Confirmed Explain decisions

- Explain returns a validated, durably persisted learner-visible Teaching Result grounded in permitted Source Passages, with its Source Trace kept outside the learner projection.
- Explain creates no Task Package or Task Attempt, performs no Assessment, and produces no Learning Evidence.
- Explain does not embed an assessable question. Understanding checks and practice are explicit later Teach-back or Apply Practice actions.
- Its learner interaction permits Continue Requested, Clarification Asked, and Flow Control Requested; these events return to guarded routing rather than letting Explain select its successor.
- The single reference Explain Action Skill uses a targeted principle explanation, one complete worked example materially different from exposed tasks, and explicit mapping from each solution step to approved in-scope rules.
- Explain embeds no question or Assessment. The exposed example and solution path are recorded for later novelty exclusions.
- Explain receives the Concept Contract, Mastery Rubric, Pedagogy Plan teaching intent, sanitized satisfied/missing criteria and error dimensions, bounded Source Passages, exposed task/example/solution Fingerprints, Learner Locale, and frozen Execution Plan/Skill Stack.
- Explain receives no raw learner answer, canonical expected answer, Assessment hidden reasoning, unrelated Concept state, or complete history.
- Explain uses the Output Gate and at most the common one same-plan repair; it has no routine independent model-verifier call because it does not create assessed evidence.
- `explain_generation/v1` is structured rather than free-form Markdown. Its ready outcome contains `principle_summary`, exactly one worked example with problem, ordered steps (`expression`, approved `rule_reference`, explanation), final result, and Source Trace; its only other outcome is the closed Source Gap shape.
- The Explain runtime validates rule/source identities and the one-example boundary, derives example and solution-path Fingerprints, and assembles the learner-visible Teaching Result, private trace, and Profile-owned Interaction Contract.
- Explain receives only the common one same-plan Gate repair; repeated invalid output becomes Node Execution Failed rather than starting a second fresh candidate cycle.

## ADRs created during design

- ADR-0064 records restoration of the application-owned Learning StateGraph boundary for multi-Profile Learning and Practice.
- ADR-0065 records the deliberate reference boundary that Hint serves Apply Practice but not Teach-back, superseding the broader Teach-back event assumption in ADR-0030.

## Glossary changes during design

- Added canonical domain definitions for Explain, Hint, and Teach-back to `CONTEXT.md` after their responsibility and evidence boundaries were confirmed.
- Generalized the existing Task Generation Exhausted definition from Apply-only generation to any formal task requiring pre-delivery Task Verification; Apply and Teach-back use it, while Explain/Hint use Node Execution Failed.

## Missing action contracts

- The initial gap is now resolved for Explain, Hint, and Teach-back by the confirmed decisions and `docs/specs/learning-practice-reference-spec.md`.
- Retrieve remains intentionally unresolved and out of scope for this spec; it requires a later independent design.
- Exact semantic IDs for the three single reference Action Bundles were not product decisions. The spec leaves those identifiers to repository-consistent mechanical naming and prohibits using them to introduce unconfirmed strategies.
- The ADR-0030 conflict is resolved by ADR-0065: Teach-back does not permit Hint Requested in this reference.

## Spec status

- `docs/specs/learning-practice-reference-spec.md` now contains only the confirmed scope, user behavior, implementation decisions, testing seam, and explicit non-goals.
- No unresolved product or architecture decision materially blocks this reference slice. Tracker publication remains operational work rather than a spec-design gap.

## Existing test seam prior art

- `ApplyProfileContractTest` is a domain-level deterministic end-to-end seam around one Profile executor with a frozen Bundle Stack and scripted generation/verifier ports. It observes compiled prompt/context separation, validated learner/private projections, persistence-after-verification, retry/failure behavior, visibility, Attempts, and evidence.
- The eventual Learning/Practice acceptance likely needs two layers already used by the repository: one Profile Contract Test per Profile plus one highest flow/use-case contract that proves Diagnostic failure can durably route through learning/practice to a fresh Independent Test. Exact flow seam depends on the chosen scope and orchestration decision and therefore is not yet confirmed.

## Confirmed testing decisions

- The primary acceptance seam is one public domain Learning Flow command-to-committed-interaction use case driven by scripted model ports and durable in-memory stores. It exercises the complete guarded StateGraph rather than individual internal nodes.
- The primary contract covers Diagnostic-failure remediation through Explain/Practice/Hint/Teach-back to a fresh Independent Test; assistance consent/refusal and Review conversion; all assessment outcomes; Independent remediation/replacement; invalid Pedagogy Plan fallback; replay/crash recovery; and private-data non-disclosure.
- Mandatory Profile Contract Tests remain for Explain, Hint, and Teach-back prompt/context/draft/Gate/visibility behavior without duplicating flow scenarios.
- One HTTP contract covers the unified learner command/interaction union, status conflicts/rejections, visibility, and absence of old Apply endpoints. One PostgreSQL recovery contract covers checkpoint/Blackboard/Attempt/Evidence/command replay durability.
- ArchUnit retains module boundaries and scheduler no-model-call behavior and adds state-write restrictions for the Pedagogy Agent and Teaching Profiles.
- Prompt snapshots and live-model smoke tests are not regression or evidence oracles.

## External pattern evidence (untrusted research data)

- Established intelligent tutoring systems commonly expose contextual hints on demand, with progressively more detailed levels and a final bottom-out hint that reveals the answer. Source: https://www.sciencedirect.com/science/article/pii/S0959475210000538
- OATutor describes hints as learner-requested instruction in the current problem-solving context and the final bottom-out hint as usually containing the answer. Source: https://doi.org/10.1145/3544548.3581574
- Research also documents hint abuse through rapidly requesting successive levels to reach the answer, supporting explicit H5 semantics and durable level-by-level exposure tracking rather than an opaque help response. Source: https://www.sciencedirect.com/science/article/pii/S0959475210000538
- These sources support the already-accepted on-demand progressive ladder. They do not decide whether Kiln-AI should precompute the ladder or generate each level at request time.
- Worked-example research reports benefits for initial skill acquisition in well-structured domains, especially when learners connect solution steps to underlying principles; combining example study with later self-explanation is a supported pattern. Sources: https://doi.org/10.1016/S0959-4752(01)00030-5 and https://doi.org/10.1016/j.cedpsych.2010.10.004
- The evidence supports proposing a bounded worked example for the Explain action and a separate Teach-back/self-explanation step. It does not decide that Kiln-AI must always show an example or how many examples to include.

## Confirmed Hint decisions

- Hint exposure remains learner-requested and monotonic for the current open Practice Task. “Bottom-out” in the external literature corresponds to Kiln-AI H5 Reveal; it describes exposure semantics, not internal generation timing.
- On the first Hint Requested event, the Hint Profile makes one model call to generate the complete H1-H5 Ladder, validates it as one private artifact, persists it, and exposes only the currently requested level.
- Later Hint Requested events reveal the next persisted level deterministically without another model call. An explicit answer request may jump to the validated H5 under ADR-0026.
- Only levels actually exposed are appended to the Assistance Trace. Unexposed persisted levels are not represented as learner assistance.
- H1-H4 leave the current Practice Attempt open. H5 exposure atomically records the assistance and closes it as Solution Revealed without Assessment.
- After H5, both Teach-back and a Fresh Equivalent Apply Practice Task may be legal. The Workflow Guard supplies the available moves; the Pedagogy Agent chooses when both are legal, and the flow bypasses the Agent when only one remains.
- Explain delivered while an Attempt is open remains an Explain Teaching Result, but its actual exposure is substantive assistance and must be appended to that Attempt's Assistance Trace.
- Hint receives only the current open Practice Task learner projection; its private canonical expected answer, Task Rubric, and verified source trace; exposed levels and the next legal level; Learner Locale; and the frozen Execution Plan/Skill Stack.
- Hint receives no Assessment hidden reasoning, other Attempt raw answers, complete Blackboard/history, or unsubmitted learner draft. The resulting Ladder is stable for the task rather than personalized to a transient draft.
- Private expected-answer access exists only to make and validate the complete H5 solution; earlier learner projections must not leak it.
- Hint uses a specialized Gate rather than an independent model verifier. For the polynomial reference, H5 correctness is checked deterministically against the canonical expected answer; H1-H4 are checked for monotonic assistance, source support, and premature solution leakage.
- The reference Hint Profile serves only an open Apply Practice Task Attempt. Teach-back does not permit Hint Requested; its Interaction Contract permits Answer Submitted, Clarification Asked, and Flow Control Requested.
- `hint_generation/v1` ready contains exactly five ordered H1-H5 entries with the matching closed disclosure kind, learner content, and approved Source Trace. H5 additionally contains structured reasoning steps and a proposed final answer.
- The runtime deterministically checks the H5 proposed final answer against the Task Package's canonical answer, rejects H1-H4 full-solution/final-answer leakage, and rejects missing, duplicate, unordered, mislabeled, or source-invalid levels. At most one same-plan repair is allowed.
- The model never chooses which level is exposed; the runtime derives H1, the next unused level, or an explicit H5 jump from committed Assistance Trace and the current learner command.
- Hint receives only the common one same-plan Gate repair; repeated invalid output becomes Node Execution Failed with the Practice Attempt still open and no new assistance exposure.

## Confirmed Apply Practice decisions

- Every Apply Practice Task remains a formal Apply task and must pass the existing isolated Task Verification before exposure.
- Add frozen Blueprint `apply.polynomial-differentiation.practice@1.0.0`, reusing the Independent reference's Concept Contract, Mastery Rubric, Source Pack, task shape, mathematical scope, difficulty, notation, answer representation, required final derivative, and optional concise rationale.
- The Practice Blueprint changes Attempt Purpose, Practice PASS/FAIL/Inconclusive assessment policy, and Interaction Contract for Hint Requested and substantive clarification.
- Its novelty exclusions cover every prior Diagnostic, Independent, Review, Practice, Explain worked example, Hint, and solution exposure. H1-H4 leave formal submission available; H5 closes the Attempt and removes submission eligibility.
