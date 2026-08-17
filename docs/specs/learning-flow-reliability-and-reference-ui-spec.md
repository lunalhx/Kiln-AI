# Spec: Learning Flow reliability and reference UI

## Problem Statement

Kiln-AI already contains the domain pieces for the reference Learning Flow:
Diagnostic, Learning and Practice, Independent Test, and Delayed Review. The
learner-facing page and several durable command paths do not yet express or
protect that complete lifecycle reliably.

The current reference UI exposes raw response JSON as its primary view, does
not render the returned answer-field contract dynamically, does not preserve
an active Flow across reloads, and does not present Hint, Teach-back, consent,
transition, and unavailable interactions as distinct learner experiences. A
learner can also trigger a second Start from the page even when durable work
already exists.

The backend has corresponding reliability gaps. Initial Flow Start can persist
Flow and source records before the first Diagnostic is generated. A submitted
Attempt can be closed before model assessment, while a provider failure can
prevent the saved submission from being recovered. Model contract errors are
currently confused with Provider failures, and malformed Assessment output does
not receive the bounded repair and Inconclusive treatment required by the
domain contracts. Several transitions still use separated durable mutations
where claim, artifacts, Attempt, Evidence, checkpoint, interaction, and
processed command must be committed as one transition.

The result is that a learner may see an empty or misleading state, lose a
recoverable submission after a network/provider failure, receive implementation
details in an error response, or accidentally create competing learning work.

## Solution

Make the unified Learning Flow API and reference UI projections of committed
durable state across the complete reference lifecycle:

`Diagnostic -> Learning and Practice -> Independent Test -> Delayed Review`

The UI will render the closed interaction union as purpose-specific learner
experiences, preserve and restore the active Flow, dynamically honor each
Task Package's answer fields, and make network and durable retry behavior
explicit without exposing private assessor data.

The backend will enforce generate-and-verify-before-mutation, exactly-once
replay and crash recovery, atomic transition binding, strict model contracts,
and one Active Learning Work item per learner and Target Concept. Existing Flow
failures will become a durable `unavailable` Interaction Boundary with a saved
Pending Operation and a bounded `retry_requested` command. Initial Start
failure remains an atomic generic 503 with no durable Flow.

Delayed Review will remain a durable work-item cadence. Learners can explicitly
cancel Scheduled, Due, or Started Review work; cancellation does not fabricate
evidence or change mastery, and cancellation of Started work abandons its open
Attempt and terminates the old Flow atomically.

## User Stories

1. As a learner, I want the page to show the current Concept, so that I know what capability I am working on.
2. As a learner, I want to see Diagnostic, Learning and Practice, Independent Test, and Delayed Review as the four major Learning Stages, so that the complete lifecycle is understandable.
3. As a learner, I want only the current stage marked as current, so that the page does not claim that a stage is permanently completed when remediation can return me to Learning and Practice.
4. As a learner, I want to see Current Milestone separately from Highest Milestone Reached, so that current capability is not confused with historical achievement.
5. As a learner, I want to see the current milestone, highest milestone, stage, Attempt Purpose, and current task meaning in learner language, so that I understand why the current interaction is being shown.
6. As a learner, I want a Diagnostic to be visibly no-hint, so that I understand that it is an initial signal rather than a taught exercise.
7. As a learner, I want a passing Diagnostic to lead through a neutral transition to a fresh Independent Test, so that Diagnostic feedback cannot assist the evidence attempt.
8. As a learner, I want a failed Diagnostic to lead into Explain or Apply Practice according to the guarded learning policy, so that a demonstrated gap does not end the Flow.
9. As a learner, I want Explain to show one source-grounded principle and one complete worked example, so that I can understand the relevant method without receiving an assessable question disguised as teaching.
10. As a learner, I want to continue after Explain, so that the graph can choose the next legal Teaching Action.
11. As a learner, I want procedural clarification to restate displayed format, notation, or interface conditions, so that I can use the interaction without receiving unrequested conceptual help.
12. As a learner, I want substantive or uncertain clarification on Diagnostic, Teach-back, or standalone Explain not to silently add assistance, so that evidence and Attempt boundaries remain honest.
13. As a learner, I want Apply Practice to display a fresh verified task, so that supported application is not merely repetition of a prior task.
14. As a learner, I want Practice to expose only the fields declared by the Task Package, so that the UI does not force a mathematical answer field onto a Teach-back task.
15. As a learner, I want to enter a mathematical expression using the accepted input families, so that I am not forced into one keyboard syntax.
16. As a learner, I want to see a canonical expression preview before submission, so that I can confirm the answer of record.
17. As a learner, I want to submit exactly one formal response for a displayed Task Attempt, so that later editing cannot manufacture stronger evidence.
18. As a learner, I want to request H1 through H4 in order on an open Apply Practice Attempt, so that I can receive only as much assistance as I need.
19. As a learner, I want a direct answer request to reveal H5, so that the Hint Ladder has an explicit end state.
20. As a learner, I want H5 to remain visible while the following Teach-back task is shown, so that the content I must explain does not disappear.
21. As a learner, I want H5 to close the current Practice Attempt without assessment, so that revealed solutions are never treated as my demonstrated performance.
22. As a learner, I want Teach-back to use one short-text response, so that I explain the rules, their applicability, and the connection between steps and result in a focused way.
23. As a learner, I want Teach-back to show no Hint control, so that the Teach-back boundary is not confused with Apply Practice.
24. As a learner, I want an open Independent or Review Attempt to warn me before assistance converts it to Practice, so that I understand the evidence consequence before accepting help.
25. As a learner, I want refusing assistance to preserve the current Independent or Review Attempt unchanged, so that I remain in control of the evidence boundary.
26. As a learner, I want accepting assistance to convert an Attempt one way before teaching content is exposed, so that the conversion is explicit and auditable.
27. As a learner, I want an Independent pass to create Independent Evidence only after deterministic and isolated assessment rules pass, so that the milestone reflects coherent independent performance.
28. As a learner, I want a conclusive Independent failure to return me to Learning and Practice, so that a new gap starts remediation rather than disappearing.
29. As a learner, I want an Inconclusive Assessment to produce no Evidence and a Fresh Equivalent replacement, so that evaluator uncertainty is not recorded as learner failure.
30. As a learner, I want Review 1 to be scheduled after an Independent pass, so that delayed retention work is created automatically.
31. As a learner, I want Scheduled Review work to show its due time but remain non-startable, so that early practice is not mistaken for delayed evidence.
32. As a learner, I want Due and overdue Review work to remain actionable, so that missing the intended time does not discard the retention check.
33. As a learner, I want each Review to use a Fresh Equivalent Task, so that it measures retained capability rather than recognition.
34. As a learner, I want the Review cadence to use the fixed 1, 3, 7, and 21 day intervals, so that the Phase 0 retention policy is predictable.
35. As a learner, I want the next Review due time to be based on actual completion time, so that lateness does not compress future intervals.
36. As a learner, I want the fourth qualifying Review success to project Durable, so that retained capability is recognized after the complete cadence.
37. As a learner, I want Review failure to lower Current Milestone while preserving Highest Milestone Reached, so that current guidance is honest without erasing achievement.
38. As a learner, I want to explicitly cancel an unfinished Review Cadence, so that I can intentionally end retention work without fabricating pass, failure, or mastery change.
39. As a learner, I want cancellation of Started Review work to abandon the open Attempt and end the old Flow, so that I can begin a new Diagnostic without leaving stuck work behind.
40. As a learner, I want cancellation to be confirmed before it changes durable Review state, so that an accidental click cannot discard my cadence.
41. As a learner, I want the page to retain my learner identity across reloads, so that the reference UI can discover my Review work.
42. As a learner, I want the page to retain the active Flow ID, so that refreshing the page restores the current committed interaction instead of starting a new Flow.
43. As a learner, I want a network failure to preserve my pending mutation, so that I can retry without losing my answer or accidentally creating a duplicate command.
44. As a learner, I want a network retry to reuse the original Idempotency-Key and body, so that an unconfirmed command cannot execute twice.
45. As a learner, I want a durable unavailable state to offer an explicit retry, so that a committed infrastructure failure does not strand the Flow.
46. As a learner, I want durable retry to resume the saved operation without re-entering my answer, so that the server's committed submission remains authoritative.
47. As a learner, I want unavailable retry to stop after three failed retries for one continuous unavailable boundary, so that the system does not repeatedly consume resources without progress.
48. As a learner, I want to leave an unavailable Flow safely after retry is unavailable, so that I am never forced to wait on a broken operation.
49. As a learner, I want HTTP errors to use neutral messages, so that provider names, endpoints, model IDs, secret names, parser errors, and stack traces are not exposed.
50. As a learner, I want a stale interaction response to refresh from the latest committed Flow state, so that an old browser tab cannot overwrite newer work.
51. As a learner, I want a duplicate Start to recover the existing Flow instead of creating a second Flow, so that concurrent tabs cannot compete for one Concept.
52. As an operator, I want one Active Learning Work item per learner and Target Concept, so that concurrent commands cannot create competing diagnostics or Review cadences.
53. As an operator, I want initial Start generation failure to leave no durable records, so that failed preparation cannot create orphan Flow state.
54. As an operator, I want existing Flow provider and configuration failures to be recoverable unavailable states, so that operational incidents do not become learner failures.
55. As an operator, I want malformed model contracts separated from Provider failures, so that contract defects receive domain-safe recovery rather than misleading 503 responses.
56. As an operator, I want each Assessment, Response Verification, and Teach-back Assessment responsibility to receive at most one repair, so that model cost and behavior remain bounded.
57. As an operator, I want malformed Assessment output that remains invalid to become Inconclusive, so that no invalid judgment reaches Evidence acceptance.
58. As an operator, I want malformed Task Verification output to invalidate only its candidate and use the existing fresh-candidate policy, so that an invalid verifier response cannot expose a task.
59. As an operator, I want audit metadata to retain normalized violation codes and repair counts without raw model content, so that failures remain diagnosable without retaining sensitive payloads.
60. As an operator, I want the scheduler to remain model-free, so that time-based Review orchestration cannot trigger uncontrolled generation.
61. As a platform developer, I want learner responses to be projections of committed durable state, so that the UI never fabricates an interaction.
62. As a platform developer, I want one shared replay and response-mapping contract for every Learning Flow command, so that idempotency behavior is not reimplemented per Teaching Action.
63. As a platform developer, I want generated artifacts, Task Verification, Attempt binding, Evidence, checkpoint, interaction, and processed command to commit atomically, so that crashes cannot expose only half of a transition.
64. As a platform developer, I want a closed `retry_requested` command with no business payload, so that retry cannot override saved learner input or select an arbitrary operation.
65. As a platform developer, I want model adapters to own transport and the Domain to own strict contract parsing, so that module boundaries preserve model-contract semantics.
66. As a platform developer, I want private assessor projections, source traces, Fingerprints, expected answers, assessment facts, model reasoning, and execution traces excluded from every learner projection, so that the evidence boundary remains protected.
67. As a platform developer, I want Review cancellation to use its own idempotent resource command, so that cancellation of coordination state is not confused with a learner answer command.
68. As a platform developer, I want the reference UI to use `textContent` or equivalent safe text insertion for server-generated content, so that learner-visible model text cannot become executable markup.
69. As a platform developer, I want live-model smoke tests to use the operator-resolved Model Profile without creating Evidence, so that provider compatibility is observed without weakening the scripted regression oracle.

## Implementation Decisions

- The scope is the existing polynomial-differentiation reference Concept and its complete Learning Flow. Retrieve, new Concepts, new mathematical task families, authentication, authorization, Learner Memory, and adaptive review algorithms remain outside this spec.
- The public API remains the destructive unified Learning Flow API. Obsolete Apply endpoints and compatibility aliases are not preserved.
- The closed Learning Flow command discriminator is `answer_submitted`, `hint_requested`, `clarification_asked`, `assistance_decided`, `continue_requested`, `retry_requested`, and `flow_control_requested`.
- Every Learning Flow command uses the existing Idempotency-Key and expected `interactionVersion`. Commands that target an open Task Attempt carry `attemptId`; Explain clarification and `retry_requested` target the current Interaction Boundary without an Attempt ID.
- `retry_requested` carries no learner answer or original command body. It is legal only on an `unavailable` Interaction Boundary and resumes the durable Pending Operation saved by the server.
- An `unavailable` Interaction Boundary is committed with `AWAITING_LEARNER_INPUT`, not `TERMINAL`, when an operation against an existing Flow cannot safely produce its next interaction. It advertises retry and Flow Control until its Retry Chain reaches three failed retries. After that it remains safe to leave but no longer advertises retry.
- Initial Start is atomic with respect to preparation. Model Profile resolution, Diagnostic generation, Output Gate, and Task Verification complete before Flow, Source Pack, Task Package, Attempt, Exposure, Interaction, Checkpoint, processed command, or verification audit is durably created. Initial failure returns a generic 503 and the client reuses the original Idempotency-Key.
- Provider network failures, timeouts, upstream 5xx responses, and runtime Model Profile configuration failures on an existing Flow become durable unavailable states with a Pending Operation. These failures do not create partial generated artifacts, new Attempts, Evidence, or cadence transitions.
- Model error categories remain distinct: `MODEL_CONFIGURATION_INVALID`, `MODEL_PROVIDER_UNAVAILABLE`, `MODEL_CONTRACT_INVALID`, `TASK_UNAVAILABLE`, `INTERNAL_ERROR`, plus the existing public validation and conflict categories as appropriate. HTTP error bodies are generic and learner-safe.
- The infrastructure model adapter owns transport and returns raw model content to the Domain. The Domain strictly parses every closed model contract and rejects missing or wrong schema, null required fields, invalid enums, invalid collection shapes, and unknown fields.
- Existing generation Profiles retain their one same-plan repair through the Typed Artifact Gate Pipeline. A malformed Task Verification response is an inconclusive verification of that candidate and follows the existing fresh-candidate generation policy.
- Response Assessment, Response Verification, and Teach-back Assessment each allow one repair using the same frozen Model Profile, responsibility, and context. A still-invalid result is Inconclusive, creates no Evidence, and follows the existing replacement behavior. Pedagogy and Clarification keep their safe fallback behavior.
- The Domain does not retain raw invalid model JSON, prompts, or learner responses as contract-error audit data. Audit metadata is limited to Flow or Attempt identity, responsibility, normalized violation codes, repair count, correlation ID, and provider-health category.
- A failed submitted Diagnostic remains closed. A recovered closed Attempt resumes Assessment from the database-saved submission. The retry request body cannot replace or override that saved submission. The same recovery rule applies to Practice, Independent Test, Review, and Teach-back submission paths.
- Attempt ownership is enforced by Flow and current Interaction state. An Attempt must belong to the current Flow and be the Attempt addressed by the current Interaction. An Attempt already replaced by a later Interaction cannot be routed again.
- All transition-specific operations preserve the persistence invariant: generation and verification precede durable mutation; the claim, artifacts, Attempt, exposure, Evidence, checkpoint, interaction, processed command, and state transition commit atomically where they belong to one transition.
- The application-owned Learning StateGraph remains the orchestration boundary. It owns Workflow Guard legality, learner interaction boundaries, replay, checkpoints, node routing, and durable state coordination. No new graph framework or event-sourcing system is introduced.
- Active Learning Work is either a non-terminal Flow or an unfinished Review Task in `Scheduled`, `Due`, or `Started` state belonging to its terminal Flow. At most one Active Learning Work item exists per learner and Target Concept. The domain and durable store enforce this claim atomically; UI state and local storage are not authority.
- A different-key Start that races with existing Active Learning Work returns a learner-safe 409 containing only the existing Flow ID needed for recovery. Phase 0 explicitly retains its trust boundary of caller-supplied `learnerId` and possession of Flow ID; authentication and authorization remain future work.
- A terminal Flow with no unfinished Review Task releases the Active Learning Work claim and permits a new Diagnostic. An unfinished Review Task prevents a new Diagnostic until the Review completes, fails, or is explicitly cancelled.
- Review cancellation is a separate idempotent operation at `POST /api/review-tasks/{reviewId}/cancel`. It is available for Scheduled, Due, and Started Review Tasks after learner confirmation. Scheduled and Due cancellation marks the Review Task Cancelled. Started cancellation atomically Abandons the open Review Attempt, marks the Review Task Cancelled, and commits a terminal Flow transition.
- Review cancellation creates no Learning Evidence and changes neither Current Milestone nor Highest Milestone Reached. Completed or Cancelled Review Task cancellation is terminal-state idempotent and returns the committed state without another effect.
- The reference UI is a static HTML/CSS/JavaScript client. It renders `task`, `teaching`, `assistance_consent`, `transition`, and `unavailable` as separate regions rather than showing raw JSON as the primary interface.
- Stage navigation uses the server-returned `stage`; the UI does not infer completed stages. `transition` and `unavailable` messages do not render as task prompts.
- The UI renders progress from `currentMilestone`, `highestMilestoneReached`, `stage`, and `attemptPurpose` without exposing internal enum meanings as the primary learner language.
- The UI dynamically renders every returned `answerField`. Mathematical expression fields use raw and learner-confirmed canonical values; rationale fields use optional text; Teach-back uses one short-text response mapped to both raw and confirmed text. Unknown answer-field kinds produce a safe non-submit error.
- H1-H4 render only `learnerContent`. H5 additionally renders `reasoningSteps` and `proposedFinalAnswer`, and the Hint region remains visible alongside any following Teach-back task.
- The UI persists `kiln.learnerId`, `kiln.activeFlowId`, and `kiln.pendingMutation`. On startup it restores the active Flow before allowing a new Start. Only a confirmed 404 clears the active Flow ID; network failure preserves it.
- The UI disables all write controls while a write is in flight, prevents duplicate Start clicks, preserves user input after 422, refreshes from GET after 409, and reuses the original request body and Idempotency-Key after a network error or uncommitted 5xx.
- The UI uses a new Idempotency-Key for explicit durable `retry_requested`, asks for confirmation before Flow leave and Review cancellation, moves focus to the new interaction heading, and exposes stage/error changes through accessible live regions.
- Server-generated learner text is inserted as text, never as HTML. Provider, Jackson, parser, UUID, source trace, Fingerprint, expected-answer, and stack-trace details are not rendered.
- Configuration wiring must expose every real Model Port through the operator Model Catalog and frozen Model Profile. The live smoke test resolves the real operator profile, covers at least one generation and one submission path, remains non-blocking, uses ephemeral state, and creates no Evidence.
- No new compatibility layer, fallback endpoint, old response mapping, or historical data migration is added.

## Testing Decisions

- The primary acceptance seam is one highest-level whole-flow scripted contract around a public Learning Flow command and its committed learner interaction plus durable state. It uses scripted model ports and durable in-memory stores, and verifies the complete guarded path without asserting internal call order or prompt text.
- Existing domain graph contract coverage is extended rather than creating separate whole-flow oracles for Diagnostic, Practice, Independent, Review, Explain, Hint, and Teach-back. Profile-specific tests remain focused on their own closed contract, Gate, Context View, repair, source-gap, and visibility rules.
- The primary whole-flow seam covers the successful lifecycle from Diagnostic through remediation, Independent Evidence, Review cadence, and Durable, plus failed Diagnostic routing, Hints, H5 Teach-back, assistance consent, transitions, and unavailable boundaries.
- The primary whole-flow seam covers replay of completed commands, stale interaction versions, wrong Attempt IDs, reused Idempotency-Key payload conflicts, crash recovery after a closed Attempt with a saved submission, and exactly-once Evidence and transition effects.
- The primary whole-flow seam covers atomic initial Start failure and asserts absence of Flow, Source Pack, Task Package, Attempt, Exposure, Interaction, Checkpoint, Processed Command, and verification audit.
- The primary whole-flow seam covers existing-Flow Provider and configuration unavailable outcomes, Pending Operation recovery, three failed retry requests, retry without client answer payload, and safe Flow Control after retry exhaustion.
- The primary whole-flow seam covers malformed Task Verification, one fresh candidate after verifier invalidity, malformed Assessment/Verification repaired once, persistent invalid Assessment becoming Inconclusive, no invalid Assessment persistence, and no Evidence from Inconclusive outcomes.
- The primary whole-flow seam covers Attempt ownership across Flow and Interaction, Diagnostic recovery, Practice/Independent/Review/Teach-back saved-submission recovery, and prevention of re-routing replaced Attempts.
- The primary whole-flow seam covers concurrent different-key Start behavior, one Active Learning Work claim, 409 response with existing Flow ID, terminal Flow release, and blocking Start while unfinished Review work exists.
- The primary whole-flow seam covers Review cancellation for Scheduled, Due, and Started Tasks, Started Attempt abandonment, terminal Flow transition, no Evidence/milestone changes, and terminal cancellation replay.
- The HTTP contract seam verifies command discriminators, status codes, Idempotency-Key and `interactionVersion` behavior, learner-safe 409/422/503 mapping, existing Flow recovery, Review cancellation, old endpoint removal, and strict absence of private fields.
- The PostgreSQL recovery seam verifies transaction atomicity, prepared-before-bind behavior, active-work uniqueness, pending operation recovery, Attempt and submission recovery across process restart, Review cancellation, ReviewTask transitions, and no duplicate Evidence or successor Review work.
- The Playwright reference UI seam verifies stage navigation, refresh recovery, dynamic answer fields, Teach-back short text, Hint/H5 persistence, simultaneous H5 and Teach-back rendering, assistance consent, transition/unavailable separation, retry key reuse, 409 refresh, 422 input preservation, double-click Start prevention, Review cancellation, and private-content absence from the DOM.
- Focused model contract tests cover strict schema closure, missing fields, nulls, invalid enums, unknown fields, one repair, two invalid responses, violation-code normalization, and responsibility-specific fallback without exposing raw model output.
- The live smoke test remains non-blocking and is not a CI oracle. It uses the operator-resolved Model Profile, exercises at least one real generation and one real submission, safely handles valid output, contract invalidity, and Provider failure, and creates no Evidence.
- Good tests observe learner-visible interactions and committed durable state. They do not assert prompt wording, private helper calls, mapper implementation, repository call order, lock primitive, database table names, or exact UI component structure.

## Out of Scope

- Retrieve Profile, Retrieve Diagnostic behavior, and any broader teaching-method or Skill catalog.
- New Concepts, subjects, source packs, mathematical task families, or task-semantic hardening beyond the current reference Concept.
- Authentication, authorization, learner accounts, cross-learner privacy enforcement, and a general dashboard. Phase 0 keeps the explicit caller-supplied learner identity trust boundary.
- Learner-configurable model selection, token budgets, cadence intervals, retry budgets, or teaching strategies.
- Adaptive spaced repetition, FSRS, SM-2, interval fuzzing, snooze, reschedule, skip, delete, notification, calendar, or review analytics.
- Hint support for Diagnostic, Independent Test, Review, Teach-back, or standalone Explain.
- Substantive clarification for Diagnostic, Teach-back, or standalone Explain; free-form tutoring chat; or autonomous clarification-to-teaching loops.
- Multiple Explain examples, multiple reference methods, dynamic strategy routing, autonomous Pedagogy loops, persistent Agents, or model calls from the scheduler.
- Background task generation, background Assessment, pre-generation of Review Task Packages, or model work outside a learner command or explicit Flow operation.
- A new graph framework, event-sourcing system, generic teaching-output union, or compatibility layer for obsolete Apply endpoints/contracts.
- Raw model response retention, prompt retention, model chain-of-thought, raw learner-response logging as contract-error audit data, or provider diagnostic details in learner responses.
- Formula editor, handwriting, OCR, and other future answer adapters beyond the existing learner-confirmed canonical-answer boundary.
- Historical Review backfill, compatibility migration, or cadence creation for evidence accepted before this spec.

## Further Notes

- The repository glossary in `CONTEXT.md` is normative. The terms Active Learning Work, Unavailable Interaction, Pending Operation, Retry Chain, Model Contract Invalid, Task Attempt, Learning Evidence, Current Milestone, and Highest Milestone Reached must retain their defined boundaries.
- The accepted architectural baseline is recorded in ADR-0069 for unavailable retry, ADR-0070 for Active Learning Work and explicit Review cancellation, and ADR-0071 for strict model-contract recovery. Existing ADRs on exactly-once replay, source visibility, isolated assessment, Hint Ladder scope, and Review cadence continue to apply.
- The key product change from the earlier plan is that Diagnostic and Teach-back do not remove clarification entirely: they allow procedural clarification only. The key state change is that existing-Flow unavailable outcomes are recoverable `AWAITING_LEARNER_INPUT` boundaries, while initial Start failures remain non-persistent 503 responses.
- The concrete Pending Operation schema, database locking mechanism, error DTO layout, UI component decomposition, and test fixture organization are intentionally left to implementation planning because they do not change the settled product semantics or architecture boundaries.
- The reference UI must never infer state that is not returned by the durable API. In particular, it must not infer completed stages, fabricate retryable interactions, or treat a learner's local pending request as a committed outcome.
