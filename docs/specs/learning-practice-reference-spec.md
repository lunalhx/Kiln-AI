# Spec: Learning/Practice reference implementation

## Problem Statement

Kiln-AI currently has an end-to-end Apply reference, but a failed Diagnostic still cannot enter a real learning loop. Explain, Hint, and Teach-back have no executable Profile contract; Practice has no dedicated Apply Blueprint; the Pedagogy Agent has no guarded runtime in which to choose a legal next action. The learner therefore cannot move from a demonstrated gap through instruction and assisted practice to a fresh Independent Test.

The missing path is also an architectural boundary. A multi-profile learning loop needs an application-owned Learning StateGraph, least-privilege Context Views, durable checkpoints, and a single public command surface. Profiles and model calls may propose typed artifacts, but they must not select illegal transitions, mutate Learning State, expose private assessment facts, or weaken the existing exactly-once command and evidence rules.

This spec adds the smallest complete Learning/Practice slice on the exact polynomial-differentiation Concept already used by the Apply reference. It defines one reference method for each of Explain, Hint, and Teach-back, reuses Apply through a Practice Blueprint, and places those capabilities behind the Pedagogy Agent and Workflow Guard. Retrieve and broader Profile or Skill catalogs remain deferred.

## Solution

Restore the application-owned Learning StateGraph described by ADR-0064 and implement one closed reference contract for each of Explain, Hint, and Teach-back. Add `apply.polynomial-differentiation.practice@1.0.0` as a Practice-purpose Blueprint over the existing Apply Profile and frozen five-Bundle Stack. Give each new Profile exactly one reference Action Bundle and `subject.calculus-notation@1.0.0`; do not create alternative teaching strategies or generic capability Bundles.

After a failed Diagnostic has been accepted, the Workflow Guard derives legal next actions from committed state. The Pedagogy Agent receives only sanitized Feedback Facts and those legal actions, then chooses one action. Explain teaches one principle through one complete worked example. Hint generates and privately persists one five-level ladder for an open Apply Practice Attempt, revealing levels deterministically on demand. Teach-back creates one verified short-text task anchored to the most recently exposed Explain example or H5 reveal in the same Flow. Apply Practice creates accepted assisted Learning Evidence and is the only remediation activity that can satisfy the prerequisite for a new Independent Test.

Replace the Apply-specific public API with one Learning Flow command API. Every command rehydrates committed state, runs only the legal graph step, atomically commits its outcome, and projects the next learner-visible interaction. Model generation and verification always precede the intended durable transition, and replay never re-runs an already committed transition. Existing Flows may persist only a safe unavailable boundary and pending operation when the next transition cannot be completed.

## User Stories

1. As a learner, I want a failed Diagnostic to lead into a real learning activity, so that the Flow does not end at the first demonstrated gap.
2. As a learner, I want the system to choose between legal Explain and Practice actions, so that the next activity can respond to my sanitized learning need.
3. As a learner, I want an explanation to state the relevant principle clearly, so that I can understand the rule before trying again.
4. As a learner, I want an explanation to include exactly one materially different complete worked example, so that I can see the principle applied without seeing my prior task repeated.
5. As a learner, I want every worked-example step tied to an approved rule, so that the explanation remains auditable and within the Concept.
6. As a learner, I want to continue, ask a clarification, or leave after an explanation, so that teaching is interactive without becoming an assessment task.
7. As a learner, I do not want an explanation to disguise an assessable question, so that teaching and evidence collection remain separate.
8. As a learner, I want a fresh Practice task over the same polynomial-differentiation scope, so that I can apply what I have learned.
9. As a learner, I want to revise my draft or request help before submitting Practice, so that assisted learning can happen inside the open attempt.
10. As a learner, I want exactly one formal Practice submission, so that submission closes and assesses the attempt once.
11. As a learner, I want additional practice after submission to use a fresh verified Task and Attempt, so that a closed result is never rewritten.
12. As a learner, I want to request hints only while an Apply Practice Attempt is open, so that hints do not leak into Independent or Teach-back assessment.
13. As a learner, I want hints to unfold from orientation through increasingly explicit help, so that I can stop at the least assistance I need.
14. As a learner, I want repeated hint requests on the same task to reveal the next persisted level, so that the system does not generate a changing ladder around me.
15. As a learner, I want an explicit request for the answer to be able to reveal H5, so that the ladder can bottom out when I choose.
16. As a learner, I want H1 through H4 to leave Practice open for submission, so that partial assistance still permits application.
17. As a learner, I want an H5 answer reveal to close the current Practice Attempt without assessing it, so that a shown solution is never treated as my demonstrated answer.
18. As a learner, I want a Teach-back activity after a worked example or H5 reveal, so that I can explain why the shown method works.
19. As a learner, I want Teach-back to ask which rules apply, why they apply, and how the steps connect, so that merely repeating a final derivative is insufficient.
20. As a learner, I want Teach-back to use one short-text response and one formal submission, so that the interaction stays focused.
21. As a learner, I want a conclusive Teach-back result to contribute understanding evidence without pretending it is Independent Evidence, so that different demonstrations retain their meaning.
22. As a learner, I want an inconclusive Practice or Teach-back judgment to create no evidence and offer a fresh task, so that evaluator uncertainty is not counted against me.
23. As a learner, I want a conclusive Practice pass or fail to be retained as assisted Learning Evidence, so that the learning loop records what happened without changing my current mastery downward.
24. As a learner, I want Practice feedback to reflect missing criteria after a conclusive failure, so that the next legal activity can target the gap.
25. As a learner, I want an assisted Practice pass at H1 through H4 to count toward readiness, so that successful supported application can reopen Independent testing.
26. As a learner, I want an H5 reveal or Teach-back pass not to substitute for successful application, so that Independent testing is not reopened by answer exposure or explanation alone.
27. As a learner, I want at least one conclusive Apply Practice pass after the triggering failure before a fresh Independent Test becomes legal, so that remediation has an application checkpoint.
28. As a learner, I want the system to be able to recommend more learning even after readiness, so that the Pedagogy Agent can choose among all currently legal actions.
29. As a learner, I want every post-remediation Independent Test to be fresh relative to all earlier tasks, examples, hints, and revealed solutions, so that evidence does not measure recall.
30. As a learner, I want an Independent pass to create the existing evidence and Review 1 transition, so that successful remediation rejoins the established review loop.
31. As a learner, I want a conclusive no-hint Independent failure to be recorded once and return me to Learning, so that a new demonstrated gap starts remediation rather than disappearing.
32. As a learner, I want Blocked or Inconclusive Independent assessment to create no evidence and prepare a fresh replacement, so that technical or evaluative uncertainty does not become failure.
33. As a learner, I want help requested during an open Independent or Review Attempt to require my consent before conversion to Practice, so that an evidence attempt is not silently changed.
34. As a learner, I want refusing assistance to leave the current Independent or Review Attempt unchanged, so that I remain in control of the attempt.
35. As a learner, I want accepting assistance to convert the current attempt once before any teaching content is exposed, so that the evidence boundary remains explicit.
36. As a learner, I want conversion from a started Review to cancel that ReviewTask without evidence or milestone changes, so that a converted review is not counted as completed.
37. As a learner, I want a later Independent pass after Review conversion to restart at Review 1, so that the review cadence is based on newly established evidence.
38. As a learner, I want a failed generation or unavailable node to preserve committed progress and offer a safe retry or Flow Control action, so that the system never fabricates an interaction.
39. As an operator, I want Explain, Hint, Teach-back, and Practice limited to the existing polynomial-differentiation Concept, Rubric, and Source Pack, so that this slice does not silently broaden curriculum scope.
40. As an operator, I want every teaching claim, worked step, hint, and Teach-back task grounded in approved source material, so that generated instruction remains attributable.
41. As an operator, I want worked examples, Practice tasks, hint ladders, and solution reveals fingerprinted, so that later freshness checks exclude exposed material.
42. As an operator, I want only exposed hint levels recorded as assistance, so that the audit trail reflects what the learner actually saw.
43. As an operator, I want substantive clarification or Explain content shown inside an open Practice Attempt recorded as assistance, so that assisted evidence is not misclassified.
44. As an operator, I want Profile outputs validated through closed typed contracts with bounded repair, so that malformed model output cannot reach the learner or state transition logic.
45. As an operator, I want every formal Apply Practice and Teach-back task independently verified before exposure, so that task consistency is checked outside generation.
46. As an operator, I want Explain and Hint validation specialized to their artifacts rather than forced through Task Verification, so that non-task teaching content retains an appropriate boundary.
47. As an operator, I want a Flow to pin model snapshots, Profile versions, Bundle versions, Blueprint versions, and prompt compiler limits, so that execution is reproducible and auditable.
48. As an operator, I want output token ceilings configured and traced by the operator, so that learners cannot change model budgets.
49. As an operator, I want scheduler activity to remain model-free, so that delayed review scheduling cannot trigger unbounded generation.
50. As a platform developer, I want the Workflow Guard to derive legal actions before the Pedagogy Agent runs, so that the model can choose only among valid transitions.
51. As a platform developer, I want the Pedagogy Agent to return concise feedback, one legal action, intent, required capability, preferred strategy tags, and a reason, so that planning remains typed and inspectable.
52. As a platform developer, I want invalid Pedagogy output discarded after one repair and replaced by a deterministic fallback, so that invalid model text cannot influence routing.
53. As a platform developer, I want Profiles and the Pedagogy Agent unable to mutate Learning State directly, so that state changes remain application-owned.
54. As a platform developer, I want a typed Blackboard and least-privilege node Context Views, so that raw answers, private expected answers, and assessment reasoning do not spread through the graph.
55. As a platform developer, I want durable graph checkpoints at interaction boundaries, so that a learner command can resume exactly once after a crash.
56. As a platform developer, I want one public Learning Flow command endpoint with typed command discriminators, so that Apply, teaching, assistance, and Flow Control share replay and response mapping.
57. As a platform developer, I want one closed learner interaction union, so that clients render only committed `task`, `teaching`, `assistance_consent`, `transition`, or `unavailable` state.
58. As a platform developer, I want the old Apply endpoints and `ApplyFlowResponse` removed, so that the implementation has one authoritative API rather than compatibility paths.
59. As a platform developer, I want separate generation contracts for Explain, Hint, and Teach-back, so that unrelated Profiles do not share a speculative generic union.
60. As a platform developer, I want one whole-graph scripted contract seam, so that the remediation path, privacy boundaries, replay behavior, and evidence effects can be verified without live-model variance.

## Implementation Decisions

### Scope and reference artifacts

- This spec implements Explain, Hint, and Teach-back Profiles, one reference Action Bundle for each Profile, the Apply Practice Blueprint, the Pedagogy Agent integration, and the Learning StateGraph needed to run them end to end.
- Retrieve remains outside this spec and requires a later independent design. This spec also does not create additional methods, strategy variants, or a general teaching-skill catalog.
- All four activities use the existing `calculus.polynomial-differentiation@1.0.0` Concept Contract, `differentiate-polynomial@1.0.0` Mastery Rubric, and `openstax-calculus-v1-3.3@1.0.0` Concept Source Pack with passage `sec-3.3-differentiation-rules`.
- Mathematical scope remains constant, constant-multiple, sum/difference, and power rules for polynomial terms. Product, quotient, chain, trigonometric, exponential, and logarithmic differentiation remain excluded.
- The existing Apply Profile and its frozen five-Bundle Stack remain the generation capability for Diagnostic, Independent, Review, and Practice tasks. Practice does not become a sixth Profile.
- Add the exact frozen Blueprint `apply.polynomial-differentiation.practice@1.0.0`. It reuses the Independent task shape, mathematics scope, difficulty, notation, answer representation, required derivative, optional rationale, source scope, and Task Verification. It changes Attempt Purpose to Practice, uses Practice assessment policy, and permits Hint plus substantive clarification interactions before submission.
- Practice novelty exclusions include all exposed Diagnostic, Independent, Review, and Practice tasks, Explain worked examples, generated hint ladders, and revealed solutions in the Flow.
- Each new Profile composes exactly one reference Action Bundle with a new immutable `subject.calculus-notation@1.0.0`. No new reasoning, representation, or verification capability Bundle is introduced for these Profiles. Existing `0.1.0` Bundles remain immutable and Apply-specific.
- Exact semantic IDs for the three reference Action Bundles are intentionally not prescribed here. Selecting repository-consistent identifiers for the confirmed single methods is a mechanical naming task and must not introduce additional product strategies.

### Learning StateGraph and command execution

- Restore an application-owned Learning StateGraph as accepted by ADR-0064. It contains the Workflow Guard, typed Blackboard, durable checkpoints, profile nodes, assessment and verification nodes, and least-privilege Context Views. This spec does not select a graph library.
- Reuse the existing Apply generation, Task Verification, submission, Assessment, evidence, `FlowCommandReplay`, and Idempotency-Key contracts as Apply node capabilities. Do not duplicate replay or response projection per Profile.
- A graph wake handles one learner command and pauses at the next committed learner interaction. Scheduler processing may make state eligible but must never call a model.
- Resolve the Model Profile and generate, gate, and verify the first Diagnostic before any Start mutation. A failed Start returns a generic 503 and leaves no Flow, Source Pack, Task Package, Attempt, interaction, checkpoint, processed command, exposure, or verification audit. Once a Flow exists, the claim, generated artifacts, Attempt, evidence, checkpoint, and state changes belonging to one successful transition commit atomically. A failed operation persists only the safe Unavailable Interaction and Pending Operation of ADR-0069, never a partial generated artifact, Attempt, exposure, Assessment, Evidence, or cadence change.
- On replay, rehydrate committed state first. Return the original committed result for an already completed command; resume from a saved Attempt after an interrupted multi-commit flow; never rerun a committed transition. `retry_requested` is a new idempotent command that resumes only the saved Pending Operation with no client-supplied answer or original body. It is legal only on an Unavailable Interaction and permits three failed retries per Retry Chain.
- At most one Active Learning Work item exists per learner and Target Concept. A non-terminal Flow or an unfinished Review Task retains the durable claim. A different-key Start races through that same claim, returns 409 with the existing Flow ID, and does not create another Flow. A terminal Flow with no unfinished Review Task releases it.
- The Blackboard carries typed facts and artifact references. Each node receives only a declared Context View. Profiles, classifiers, the Pedagogy Agent, Assessment, and verifiers cannot write Learning State directly.
- A failed submitted Diagnostic stays closed and is never retroactively converted. After an accepted Diagnostic failure, the Workflow Guard derives legal remediation actions from committed state before the Pedagogy Agent is invoked.
- When Explain and fresh Apply Practice are both legal, the Pedagogy Agent selects one. The Guard, not the Agent, owns legality and state-transition authorization.

### Pedagogy Agent

- The Pedagogy Agent receives sanitized satisfied and missing criteria, error dimensions, relevant assistance and readiness facts, and the closed set of legal actions. It receives no raw learner answer, expected answer, assessment reasoning, Skills, or unrestricted state history.
- A valid result contains concise learner feedback, exactly one legal action, pedagogy intent, required capability, preferred strategy tags, and a private reason. It cannot select Bundles, assess the learner, mutate state, or invoke itself recursively.
- Permit one initial generation and at most one same-plan repair. There is no autonomous agent loop.
- After a second invalid result, discard the entire invalid output, including its feedback, action, reason, and tags. Use deterministic neutral feedback and these fallbacks:
  - Diagnostic, Practice, or Teach-back failure: Explain.
  - Explain completion or Teach-back pass without a qualifying Practice pass: fresh Apply Practice.
  - H5 solution reveal: Teach-back.
  - Qualifying Practice pass: fresh Independent Test.
  - Temporary Explain inside an open Practice Attempt: return to the same Practice interaction.
- If the deterministic fallback node or its required capability is unavailable, retain committed state and project a safe retry or Flow Control interaction. Never fabricate teaching content or a task.

### Explain Profile

- Explain is a pure teaching action. It creates a durable teaching artifact and learner interaction, but no Task Package, Attempt, Assessment, or Evidence.
- The reference method gives a targeted principle explanation plus exactly one materially different complete worked example. Every worked step maps to a rule approved by the Concept and Mastery Rubric.
- Explain must not include an assessable learner question. Its legal learner events are Continue, procedural Clarification, and Flow Control; later Teach-back or Apply Practice is a separate graph action. Explain clarification addresses the current Interaction rather than an Attempt. It deterministically restates only displayed content or format; substantive or uncertain requests receive no new teaching content and leave the teaching boundary unchanged.
- The closed `explain_generation/v1` contract contains `principle_summary`, `worked_example.problem`, ordered `steps` with `expression`, `rule_reference`, and `explanation`, `final_result`, and `source_trace`, or a closed source-gap outcome. Unknown fields and generic private maps are rejected.
- Its Context View contains the Concept Contract, Mastery Rubric, pedagogy intent, sanitized satisfied and missing criteria and error dimensions, bounded approved source passages, novelty Fingerprints, learner locale, and the Flow-frozen plan and Skill Stack.
- Explain receives no raw learner answers, private expected answers, assessment reasoning, complete Learning State, or unrestricted Flow history.
- An Explain Output Gate checks contract closure, source grounding, rule mapping, scope, one-example cardinality, completeness, and novelty. Permit one same-plan repair. A repeated invalid result becomes `NodeExecutionFailed`; no separate model Task Verifier runs for Explain.
- A source gap produces no teaching artifact and follows the node-unavailable retry or Flow Control behavior.

### Hint Profile

- As accepted by ADR-0065, the reference Hint ladder is available only for an open Apply Practice Attempt. Teach-back has no Hint event.
- The first hint request makes one model call to generate the full private H1-H5 ladder for the current Practice task. After validation, persist the stable ladder and expose only the requested legal level. Later requests reveal persisted levels deterministically without another model call.
- H1 through H5 use these disclosure kinds in order: `orient`, `cue`, `strategy`, `scaffold`, and `reveal`. An explicit learner request for the answer may jump directly to H5.
- The closed `hint_generation/v1` contract contains exactly five ordered entries with `level`, `disclosure_kind`, `learner_content`, and `source_trace`. H5 additionally contains `reasoning_steps` and `proposed_final_answer`. It may instead return a closed source-gap outcome.
- The Hint Context View contains the current learner-visible Apply task, private canonical answer and Rubric, verified source passages, already exposed levels, next legal level, learner locale, and the Flow-frozen plan and Skill Stack. It does not contain assessment reasoning, other raw answers, the full Blackboard or Flow history, or the learner's unsubmitted draft.
- The Hint Gate validates contract closure, level order, source grounding, progressive disclosure, H1-H4 answer leakage, and polynomial H5 answer equivalence deterministically. Permit one same-plan repair. A repeated invalid result becomes `NodeExecutionFailed`; the Practice Attempt remains open and no hint is exposed.
- Only exposed levels are recorded in `AssistanceTrace`. H1-H4 leave the Attempt open and may precede submission. H5 exposure atomically records assistance and closes the Attempt as `SolutionRevealed` without Assessment.
- After H5, Teach-back and fresh Apply Practice may both be legal. The Pedagogy Agent chooses when both are legal; deterministic fallback chooses Teach-back.
- Showing substantive Explain or clarification content during an open Practice Attempt also records the corresponding assistance before later Practice assessment.

### Teach-back Profile

- Teach-back is legal only when the same Flow contains an eligible, most recently exposed Explain worked example or H5 solution reveal. The Guard does not offer Teach-back without such an anchor.
- The reference method asks the learner to identify the rules used, explain why each applies, and connect the worked steps to the result. Reproducing the final derivative is not the primary task.
- Teach-back generation emits one verified rubric task. Delivery creates a new Practice-purpose Attempt with one required short-text response, no required derivative field, and exactly one formal submission. Hint is not a legal Teach-back event; only procedural Clarification and Flow Control remain legal. Diagnostic likewise permits only procedural Clarification. A substantive or uncertain clarification in either task produces no teaching content and does not alter the Attempt purpose or evidence eligibility.
- The closed `teach_back_generation/v1` draft contains the learner prompt, Rubric mapping, source trace, and anchor reference, or a closed source-gap outcome. It must not contain a verbatim expected explanation or generic private artifact map.
- Its Context View contains the exact Concept Contract and Mastery Rubric, pedagogy intent, learner locale, Flow-frozen plan and Skill Stack, and only the eligible exposed anchor content, anchor ID, and source trace. It receives no raw learner answers, assessment reasoning, unexposed hints, or private expected answer beyond content the learner has already seen.
- The Task Rubric has three mandatory dimensions: correct rule identification, correct explanation of applicability, and a coherent, noncontradictory connection between steps and result. All three must pass for a Teach-back pass. A clearly missing or wrong dimension fails. An unreliable or disputed judgment is Inconclusive.
- Run the typed generation gate and isolated Task Verification before delivery. Source Gap ends the cycle without retry. A repairable draft permits one same-plan repair. A verifier rejection or inconclusive result discards the candidate and permits one fresh second candidate. Exhaustion produces Task Generation Exhausted with no Attempt, exposure, or Evidence.
- One formal submission closes the Attempt and invokes isolated semantic Assessment. A conclusive pass or fail creates accepted understanding-dimension Learning Evidence, never Independent Evidence, and never lowers Current Mastery. An Inconclusive result creates no Evidence and requires a fresh Teach-back task if repeated.

### Practice, evidence, and transitions

- An Apply Practice Attempt may span multiple learner commands before submission. Hints, clarification, temporary Explain, and draft edits occur before the single formal submission. Submission closes and assesses the Attempt exactly once.
- H1-H4-assisted Practice may be submitted. H5 exposure cannot be submitted because the Attempt has already closed as `SolutionRevealed`.
- A conclusive Practice pass or fail creates accepted assisted Learning Evidence with its exposed `AssistanceTrace`. Neither result lowers Current Mastery. A conclusive fail supplies sanitized Feedback Facts for the next guarded decision. An Inconclusive result creates no Evidence and requires a fresh verified task.
- For the current application Mastery Criterion, at least one conclusive Apply Practice pass after the triggering failure is required before fresh Independent testing becomes legal. A pass after H1-H4 assistance satisfies this prerequisite. H5 reveal and Teach-back pass do not.
- Once the prerequisite is satisfied, fresh Independent and any other guarded learning actions may simultaneously be legal; the Pedagogy Agent chooses from the legal set.
- A post-remediation Independent pass follows the existing Independent Evidence and Review 1 transition.
- A conclusive, valid, no-hint Independent fail accepts exactly one Independent-fail Evidence item, changes Current from the independent state to Learning, and begins remediation. It never reruns the committed Independent transition.
- Blocked assessment under ADR-0061 and Inconclusive Independent assessment create no Evidence or milestone change. Generate a fresh verified Independent replacement using all applicable novelty exclusions.
- A learner request for assistance while an Independent or Review Attempt is open first projects an assistance-consent interaction. Refusal preserves the attempt unchanged. Acceptance converts it one-way to Practice before any assistance content is exposed.
- For a started Review, accepted conversion atomically converts the Attempt, cancels its ReviewTask, creates no Review Evidence, and leaves review milestones unchanged. A later Independent pass restarts Review 1.

### Generation, verification, models, and budgets

- Keep separate closed `explain_generation/v1`, `hint_generation/v1`, and `teach_back_generation/v1` contracts. Do not introduce a generic teaching-output union.
- Reuse one typed artifact gate pipeline for contract parsing, normalized violations, bounded repair, source-gap handling, and atomic persistence. Profile-specific gates own semantic checks. Infrastructure returns raw model content; the Domain is the strict closed-contract parser for every model responsibility.
- Apply Practice and Teach-back require isolated pre-delivery Task Verification. Explain uses its Output Gate only. Hint uses its specialized Gate plus deterministic H5 equivalence and no model Task Verifier. Invalid Task Verification is an inconclusive candidate verification. Response Assessment, Response Verification, and Teach-back Assessment each allow one same-profile repair and become Inconclusive when still invalid. Pedagogy and Clarification retain their safe fallbacks. No malformed model content becomes a learner-visible provider error.
- A formal task Source Gap ends generation immediately. A repairable task draft permits one repair. A Task Verification reject or inconclusive result permits one fresh second candidate. Exhaustion creates no Attempt, exposure, or Evidence and projects the established neutral unavailable interaction.
- Freeze Strong and Small model snapshots with the Flow. Strong is used for Apply generation, Explain generation, the first Hint generation, Teach-back generation, Task Verification, Assessment, and Response Verification where the assessment policy requires it. Small is used for the Pedagogy Agent and an input or clarification classifier only when classification is needed.
- Each model node gets one initial call and at most one repair. Assessment, Response Verification, and Teach-back Assessment each own that budget independently. The Pedagogy Agent does not recurse, and the graph does not run an autonomous model loop. Invalid configuration and provider availability failures on an existing Flow commit a bounded-retry Unavailable Interaction; only pre-binding Start failures return generic 503. Raw model output, prompts, learner responses, provider details, and parser errors never enter logs, artifacts, or HTTP responses; audit metadata is limited to identities, responsibility, normalized violation codes, repair count, correlation ID, and provider-health category.
- New Profile prompt compilers use a 16,000-character instruction cap. Output-token ceilings are frozen operator configuration recorded in execution trace, not learner configuration.

### Public API and learner projection

- Remove `/api/apply/**`, `/api/apply/reviews`, and `ApplyFlowResponse` without aliases, compatibility controllers, or fallback mappings.
- Provide these public resources:
  - `POST /api/learning/flows`
  - `GET /api/learning/flows/{flowId}`
  - `POST /api/learning/flows/{flowId}/commands`
  - `POST /api/review-tasks/{reviewId}/cancel`
  - Review collection and start resources under `/api/review-tasks`
- Learning commands use a closed discriminator with `answer_submitted`, `hint_requested`, `clarification_asked`, `assistance_decided`, `continue_requested`, `retry_requested`, and `flow_control_requested`.
- Every command carries the existing Idempotency-Key and expected `interactionVersion`; commands targeting an open Attempt carry `attemptId`, while Explain clarification and `retry_requested` target the current interaction without one. `answer_submitted` reuses the existing raw answer, learner-confirmed canonical representation, and optional rationale contract. `retry_requested` has no business payload and rehydrates its Pending Operation. Review cancellation has its own Idempotency-Key contract and is terminal-state idempotent.
- `LearningFlowResponse` exposes one closed committed-interaction union: `task`, `teaching`, `assistance_consent`, `transition`, or `unavailable`. An unavailable interaction is `AWAITING_LEARNER_INPUT`, not terminal by default, and advertises `retry_requested` until its Retry Chain reaches three failed retries plus `flow_control_requested` throughout. `allowedEvents` are derived from the active Profile and Guard. Private answers, unexposed hints, Rubric internals, source passages, assessment reasoning, model reasoning, Blackboard content, and execution traces never appear in the learner projection.
- Phase 0 deliberately has no authentication or authorization. `learnerId` and possession of a Flow ID form its explicit trust boundary; a Start conflict may return only the safe existing Flow ID. This does not authorize any private projection and must be replaced by a future authenticated ownership boundary.
- Update the reference learner UI to render only committed interaction state and allowed events. It persists learner ID, active Flow ID, and a pending network mutation; restores the active Flow before allowing Start; sends a new key for an explicit retry command; reuses the original key/body for network or uncommitted-503 retransmission; and shows explicit cancellation with confirmation for every unfinished Review Task. It must not invent other Profile behavior.

## Testing Decisions

- The primary stable acceptance seam is a public domain Learning Flow command followed by the committed learner interaction and durable state. Script all model ports and use durable in-memory stores so the test crosses the whole guarded Learning StateGraph without depending on prompt snapshots or live-model variance.
- The main graph contract must cover failed Diagnostic routing through Explain, Apply Practice, Hint, Teach-back, and fresh Independent Test, including both valid Pedagogy choices and deterministic fallback.
- Cover H1-H4 progressive reveal and submission, direct H5 reveal, stable persisted ladder reuse, H5 closure without Assessment, exposed-only `AssistanceTrace`, and Hint unavailability outside open Apply Practice.
- Cover Explain teaching-only behavior, one complete novel worked example, Continue and procedural Clarification without an Attempt, rejection of substantive/uncertain clarification without added teaching content, return to the same open Practice interaction after temporary Explain, source gap, repair exhaustion, and absence of Task, Attempt, Assessment, and Evidence.
- Cover Teach-back anchor eligibility, all three rubric dimensions, pass, fail, Inconclusive, procedural-only clarification, source gap, repair, malformed assessment repair/inconclusive handling, first verifier failure followed by a fresh valid candidate, exhausted generation, and the absence of Hint.
- Cover Practice pass, Practice fail, Inconclusive, all assistance levels, readiness gating, fresh-task creation, and the rule that H5 or Teach-back alone cannot reopen Independent testing.
- Cover Independent pass, conclusive no-hint fail into remediation, Blocked replacement, Inconclusive replacement, and novelty exclusions across every exposed task, example, ladder, and solution.
- Cover assistance consent and refusal for open Independent and Review Attempts, one-way conversion before exposure, ReviewTask cancellation, unchanged Review evidence and milestones, and restart at Review 1 after later Independent success.
- Cover invalid Pedagogy output after one repair and verify that none of its feedback, action, reason, or tags reaches the learner or state. Cover each deterministic fallback and capability-unavailable safe retry.
- Cover command replay, crash recovery around committed halves, stale `interactionVersion`, wrong `attemptId`, duplicate Idempotency-Key, and exactly-once Evidence and transition effects. Cover atomic failed Start, provider/configuration unavailable boundaries, three-retry exhaustion, saved-submission recovery without a client body, and no persistence of raw invalid model JSON.
- Cover the active-work claim under concurrent different-key Starts, 409 recovery via existing Flow ID, terminal release, Scheduled/Due/Started Review cancellation, Started cancellation's atomic Attempt abandonment and terminal Flow boundary, terminal cancel replay, and no milestone or Evidence mutation from cancellation.
- Cover learner/private projection boundaries for every interaction variant and prove that unexposed ladder levels, canonical answers, source passages, Rubric internals, assessment facts, Blackboard entries, and execution traces are absent.
- Add focused `ExplainProfileContractTest`, `HintProfileContractTest`, and `TeachBackProfileContractTest` suites only for Profile-specific Context View, generation contract, Gate, repair, source-gap, and visibility behavior. Do not duplicate whole-flow state assertions in each Profile suite.
- Add one HTTP contract for unified Learning Flow creation, status, commands, interaction variants, status codes, idempotency behavior, private-field absence, and removal of the old Apply endpoints.
- Add one PostgreSQL recovery contract for graph checkpoint, Blackboard artifact references, Attempt, Evidence, ReviewTask conversion, and command replay across process restart.
- Keep ArchUnit coverage for module boundaries, Agent/Profile prohibition on state writes, reusable replay/response mapping, and the scheduler no-model-call invariant.
- Scripted fixtures and committed-state assertions are the regression oracle. Prompt snapshots and live-model smoke tests are not acceptance evidence.

## Out of Scope

- Retrieve Profile, Retrieve Diagnostic behavior, and its Skills or Profile Contract Test.
- Any Concept, Mastery Rubric, source, notation system, or subject beyond the existing polynomial-differentiation reference.
- Multiple Explain, Hint, or Teach-back strategies; dynamic Skill routing; strategy catalogs; learner-selectable teaching methods; or new generic reasoning, representation, and verification capability Bundles.
- Hint support for Teach-back, Independent, Diagnostic, or Review Attempts; substantive clarification for Diagnostic, Teach-back, or standalone Explain; and an autonomous or unlimited retry policy.
- Multiple worked examples in one Explain result, free-form tutoring chat, or converting Explain itself into an assessable task.
- A learner-configurable model picker, learner-configurable token budget, autonomous Pedagogy loops, or model calls from the scheduler.
- Selecting or adopting a third-party graph framework. The application-owned graph contracts are normative; the underlying implementation mechanism is not chosen here.
- RAG, embedding retrieval, learner source uploads, automated source ingestion, source-authoring UI, cross-Flow Learner Memory, authentication, and authorization.
- Backward compatibility for `/api/apply/**`, `/api/apply/reviews`, `ApplyFlowResponse`, old Profile contracts, or immutable `0.1.0` Bundle manifests.
- Live-model output as a contract-test oracle or prompt-text snapshot tests.

## Further Notes

- ADR-0064 records the accepted restoration of the Learning StateGraph for this multi-profile runtime. ADR-0065 records the accepted decision to limit the reference Hint ladder to Apply Practice, superseding the broader optional Hint wording in ADR-0030 for this implementation.
- `CONTEXT.md` defines Explain, Hint, Teach-back, and the generalized formal-task meaning of Task Generation Exhausted. Implementations must use those terms rather than introduce synonyms.
- Established hint-ladder and worked-example research informed the available design patterns, but does not add requirements beyond the decisions above. The normative behavior is this spec and the accepted ADR baseline.
- Exact Action Bundle identifiers, database table names, class names, HTTP error-body layout, and graph-library choice are non-product implementation details left to repository conventions. They must not be used to introduce behavior outside this spec.
- Product and architecture discovery for this slice is converged. The accepted additions in ADR-0069 through ADR-0071 are normative: durable unavailable retry, the one Active Learning Work claim with explicit Review cancellation, and strict model-contract recovery. Remaining choices are implementation details such as the concrete pending-operation table shape, lock primitive, error-body DTO layout, and UI component structure.
