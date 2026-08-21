# Bounded pre-learning Diagnostic Flow specification

## Problem Statement

Kiln-AI currently treats Diagnostic as one no-assistance Task Attempt whose submission immediately routes either to a fresh Independent Test or to one shared Learning and Practice Guard route. That shape cannot perform the broader pre-learning assessment the learner needs: it cannot cheaply reuse prior Concept Progress, distinguish Required Supporting Concept readiness from Target readiness, collect several bounded Diagnostic Findings, or adapt later teaching from confirmed strengths and gaps.

The single immediate route also gives Conclusive Diagnostic Gap and Unconfirmed Diagnostic Performance the same next moves even though they make different claims. It can teach unnecessarily after evaluator uncertainty, and a one-task outcome can carry too much routing weight. At the same time, turning Diagnostic into an exhaustive placement exam would delay the learner, duplicate Independent Test, and violate the requirement that Diagnostic remain non-evidentiary.

The product therefore needs a bounded, source-grounded, Agent-authored pre-learning Diagnostic stage. It must obtain only the minimum information sufficient for a safe recommendation, preserve learner control to start learning directly, adapt Target pedagogy without manufacturing mastery, and retain the repository's exactly-once, privacy, source, and no-Evidence invariants.

## Solution

The Concept Preparation Agent derives a frozen, versioned Diagnostic Plan from approved Normalized Source Documents. Type-specific Gates validate its Target Readiness Set, Required Supporting Concepts, prerequisite dependency order, source basis, rationale policy, coverage and termination rules, and worst-case Task Attempt count. A Plan may contain at most eight Diagnostic Task Attempts across its complete stage for one Learning Flow and Plan version, including learner-controlled resume.

At runtime, Diagnostic first consults accepted Concept Progress for every Required Supporting Concept. Independent or Durable progress establishes Prerequisite Readiness without another probe only when the Supporting Concept identity and relevant Mastery Rubric, criterion, and source-basis versions match the current Plan. Otherwise the learner receives a minimal Prerequisite Readiness Check: a self-report of not knowing or being unsure produces a neutral Prerequisite Learning Recommendation without a task, while a claim of knowing leads to one small representative no-assistance probe.

Remaining Required Supporting Concepts are checked in acyclic dependency order. The first conclusive, learner-declared, or terminally Unconfirmed prerequisite result stops further Diagnostic probing and presents a recommendation. The current Flow never teaches or automatically starts the Supporting Concept. The learner may explicitly start that Concept as a separate Learning Flow, choose Direct Learning for the current Target, or leave. Direct Learning is also available before and during Diagnostic. It creates no Prerequisite Readiness or Learning Evidence, preserves committed Diagnostic Findings, abandons an open unsubmitted Diagnostic Attempt without a Finding, and enters Target Learning and Practice through the normal post-Practice path rather than direct Independent eligibility.

Once prerequisite readiness is confirmed, Diagnostic probes the Agent-authored, Gate-validated Target Readiness Set rather than exhaustively testing the Mastery Rubric. Positive confirmation of the complete Set authorizes only a Neutral Transition to a fresh Independent Test. A Conclusive Target gap enters Target Learning and Practice and may focus or compress teaching through a learner-safe Diagnostic Summary. Unconfirmed performance receives a fresh Plan-authorized probe while budget remains; at the eight-Attempt or smaller Plan limit, Target uncertainty enters Learning and Practice neutrally, while prerequisite uncertainty produces an overridable neutral recommendation. Technical failure remains Unavailable and makes no readiness claim.

Every submitted Diagnostic Attempt closes permanently and produces a Flow-scoped Diagnostic Finding or passing observation, never Learning Evidence. Transitions between Diagnostic tasks and before a fresh Independent Test reveal no correctness, answer, rule, prerequisite finding, or targeted feedback. A learner-safe Diagnostic Summary is shown only after the route enters Target Learning and Practice. Diagnostic rationale is Blueprint-selected and always optional: simple prerequisite checks disable it by default, while a Target task enables it only when its Task Rubric can distinguish conceptual readiness from an execution slip. A proven-incorrect primary answer is rescued only by the existing two isolated Applicable rationale judgments.

## User Stories

1. As a learner, I want Diagnostic to assess my preparation before teaching begins, so that my learning path can reflect what I already know.
2. As a learner, I want Diagnostic to use several questions when necessary, so that one response does not carry the whole routing decision.
3. As a learner, I want Diagnostic to stop as soon as it has enough information, so that I am not trapped in a long pre-test.
4. As a learner, I want to see the Plan-specific maximum question count before Diagnostic begins, so that I know the worst-case commitment.
5. As a learner, I want to see completed Diagnostic Attempts against that maximum, so that variable length remains understandable.
6. As a learner, I want the maximum to remain eight Attempts or fewer even after resume, so that pausing cannot silently extend the assessment.
7. As a learner, I want to choose Direct Learning before answering Diagnostic questions, so that I can begin learning immediately.
8. As a learner, I want Direct Learning to remain available during Diagnostic, so that I can stop the pre-test without leaving the Target Flow.
9. As a learner, I want an open unsubmitted Diagnostic Attempt to close without a result when I choose Direct Learning, so that an unanswered task is not treated as performance.
10. As a learner, I want my submitted Diagnostic Attempts to remain immutable, so that later routing never rewrites what I did.
11. As a learner, I want Diagnostic to reuse relevant prior learning, so that I do not repeat prerequisite checks unnecessarily.
12. As a learner, I want only version-aligned Independent or Durable progress to bypass a prerequisite check, so that stale learning records do not make an unsupported readiness claim.
13. As a learner, I want a brief recheck when the relevant prerequisite Concept, Rubric, criterion, or source basis changed, so that version changes do not force a full prerequisite exam.
14. As a learner, I want prerequisite screening to stay small, so that the Target Concept remains the focus.
15. As a learner, I want to report that I do not know or am unsure about a prerequisite, so that I can receive a recommendation without taking an unnecessary task.
16. As a learner, I want a claim that I know a prerequisite to lead to one representative check, so that self-report helps routing without becoming proof.
17. As a learner, I want a confirmed prerequisite response to establish readiness without a redundant Independent Test, so that a lightweight check stays lightweight.
18. As a learner, I want a prerequisite Diagnostic check to create no Evidence, milestone, or review schedule, so that it is not mistaken for mastery of a secondary Concept.
19. As a learner, I want Required Supporting Concepts checked in dependency order, so that the system recommends the most foundational unresolved need first.
20. As a learner, I want Diagnostic to stop at the first sufficient prerequisite recommendation, so that it does not profile unrelated unknowns after the route is already known.
21. As a learner, I want lower-priority unprobed prerequisites to remain Unknown, so that the system does not claim facts it never checked.
22. As a learner, I want a prerequisite recommendation to explain why the Concept may matter, so that I can make an informed learning choice.
23. As a learner, I want a prerequisite recommendation to remain neutral when the system could not confirm performance, so that evaluator uncertainty is not called my failure.
24. As a learner, I want a prerequisite recommendation to offer Direct Learning, so that the final decision remains mine.
25. As a learner, I want a recommended Supporting Concept Flow to start only when I explicitly start it, so that the system does not create an automatic curriculum for me.
26. As a learner, I want the current Target Flow to avoid teaching a Supporting Concept, so that each Flow retains one Target Concept.
27. As a learner, I want the original Target Flow to suspend if I choose the separate Supporting Concept Flow, so that I can return to the work I started.
28. As a learner, I want aligned Independent or Durable progress from the Supporting Concept Flow to satisfy readiness on return, so that I do not repeat the same prerequisite check.
29. As a learner, I want a brief readiness recheck on return when progress is lower, absent, external, or version-misaligned, so that I can continue without a second full assessment.
30. As a learner, I want resumed Diagnostic to retain confirmed areas and consumed Attempts, so that resume neither repeats work nor resets the maximum.
31. As a learner, I want a book-external prerequisite to use approved source material, so that the system does not test me on model-invented curriculum.
32. As a learner, I want a missing prerequisite source to be reported as a Source Gap, so that missing content is not mislabeled as my knowledge gap.
33. As a learner, I want the Target Readiness Set to test only representative readiness criteria, so that Diagnostic does not duplicate a full Independent Test.
34. As a learner, I want one task to cover several readiness criteria when appropriate, so that Diagnostic remains concise.
35. As a learner, I want every Target Readiness Set criterion positively confirmed before a direct Fresh Independent Test, so that the shortcut is not based on an isolated success.
36. As a learner, I want Diagnostic success to lead to a fresh Independent Test rather than mastery, so that Evidence comes from a separate no-assistance task.
37. As a learner, I want a Conclusive Target gap to focus later teaching, so that I spend time on confirmed needs.
38. As a learner, I want confirmed strengths to compress or skip corresponding instruction, so that teaching does not repeat what Diagnostic already established.
39. As a learner, I want unprobed Target criteria to remain Unknown, so that compressed teaching does not become a false mastery claim.
40. As a learner, I want Unconfirmed Target performance to receive another fresh probe while budget remains, so that evaluator uncertainty does not immediately trigger remediation.
41. As a learner, I want terminal Target uncertainty to enter Learning and Practice neutrally, so that I can continue without a deficit label.
42. As a learner, I want terminal prerequisite uncertainty to produce an overridable recommendation, so that safety and learner control are both preserved.
43. As a learner, I want technical Provider or model-contract failure to produce Unavailable rather than a learning route, so that system failure is never attributed to me.
44. As a learner, I want transitions between Diagnostic tasks to reveal no correctness or targeted feedback, so that later Diagnostic performance remains no-assistance.
45. As a learner, I want the transition to a Fresh Independent Test to reveal no Diagnostic feedback, so that the evidence attempt is not assisted.
46. As a learner, I want a Diagnostic Summary only after entering Target Learning and Practice, so that feedback can personalize teaching without contaminating formal testing.
47. As a learner, I want the Diagnostic Summary to distinguish strengths, teaching priorities, and Unknowns, so that I understand the personalized path.
48. As a learner, I want the Diagnostic Summary to omit answers, solutions, private answer facts, and evaluator reasoning, so that private assessment data stays private.
49. As a learner, I want a rationale field only when it can improve the interpretation of that task, so that I am not asked for unnecessary prose.
50. As a learner, I want Diagnostic rationale to remain optional, so that omitting it is not treated as failure to follow instructions.
51. As a learner, I want the rationale field to explain its possible effect, so that its routing significance is not hidden.
52. As a learner, I want a correct primary answer to pass the Attempt without rationale assessment, so that optional extra text cannot create a new risk.
53. As a learner, I want an incorrect primary answer rescued only after two isolated rationale judgments agree, so that one model opinion cannot create readiness.
54. As a learner, I want a rationale disagreement or uncertainty represented as Unconfirmed, so that it is not converted into a conclusive deficit.
55. As a learner, I want Direct Learning to create no readiness or Evidence, so that exercising control does not fabricate an assessment result.
56. As a learner, I want normal Practice to make me eligible for a later Independent Test after Direct Learning, so that skipping Diagnostic does not permanently limit progression.
57. As a content owner, I want the Concept Preparation Agent to derive Diagnostic artifacts from approved Normalized Source Documents, so that per-book plans do not require routine manual authoring.
58. As a content owner, I want internal preparation artifacts accepted only after type-specific Gates pass, so that Agent authorship does not bypass source and contract checks.
59. As a content owner, I want the learner to confirm only the learner-visible Concept Contract, so that internal Rubrics and Plans remain Agent-authored implementation artifacts.
60. As a content owner, I want a suspected plan-external prerequisite to produce a new validated Plan version, so that runtime assessment cannot silently expand curriculum.
61. As an operator, I want every Diagnostic Plan frozen for its Flow version, so that runtime selection is reproducible and bounded.
62. As an operator, I want a Gate to reject Plans requiring more than eight Diagnostic Attempts, so that the learner-facing maximum is enforceable.
63. As an operator, I want the Plan to declare an acyclic prerequisite order, so that prerequisite routing cannot loop.
64. As an operator, I want unsupported prerequisite dependencies and tasks to fail as Source Gap, so that model memory is not subject authority.
65. As an operator, I want Diagnostic Findings scoped to one Flow, so that this feature does not introduce unrestricted cross-Flow learner memory.
66. As an operator, I want cross-Flow return to read only aligned committed Concept Progress, so that raw responses and Blackboards do not leak between Concepts.
67. As a platform developer, I want the Diagnostic Routing Decision derived from accumulated committed facts rather than the last Attempt alone, so that multi-Attempt routing is deterministic.
68. As a platform developer, I want Diagnostic Attempt outcomes separated from stage routing, so that passing, Conclusive, and Unconfirmed observations can obey the Plan's stopping rule.
69. As a platform developer, I want every Diagnostic command exactly-once under replay, so that retry cannot duplicate Attempts, Findings, or transitions.
70. As a platform developer, I want generation and verification completed before durable mutation, so that failed question preparation leaves no partial learner interaction.
71. As a platform developer, I want each committed transition to atomically bind the closed Attempt, Finding, next verified task or terminal interaction, checkpoint, and processed command, so that crash recovery observes one authoritative state.
72. As a platform developer, I want Direct Learning to reuse the existing command replay and interaction contracts, so that skip behavior does not implement a second idempotency path.
73. As a platform developer, I want learner-visible responses projected only from committed state, so that progress, summaries, and recommendations are never fabricated.
74. As a platform developer, I want technical Unavailable recovery to resume saved responsibilities, so that a submitted answer is not replaced or reevaluated unnecessarily.
75. As a platform developer, I want the obsolete one-probe `Diagnostic Not Passed` route removed, so that implementation has one authoritative multi-Attempt state machine.

## Implementation Decisions

- Replace the shipped one-probe Diagnostic orchestration destructively. Do not retain `Diagnostic Not Passed` as a compatibility route, add aliases, or maintain per-flow replay and response-mapping variants.
- Retain one Target Concept per Learning Flow. Supporting Concepts may receive Flow-scoped Diagnostic Findings but receive no Concept Progress, Evidence, milestone, or review work in the Target Flow.
- Add Concept Preparation Agent output for a frozen, versioned Diagnostic Plan derived from approved Normalized Source Documents. The Plan contains the Target Readiness Set, prepared Supporting Concepts, Required designations, source basis, acyclic dependency order, prerequisite recommendation rules, rationale enablement, coverage rules, termination rules, and worst-case Attempt count.
- Type-specific Gates validate every Agent-authored preparation artifact before acceptance. They reject unsupported source traces, invalid references, cycles, Rubric expansion, an unsafe Target Readiness Set, unjustified rationale enablement, and a Plan whose complete worst case exceeds eight Diagnostic Task Attempts.
- Eight is the platform hard ceiling for one Flow's complete Diagnostic stage under one frozen Plan version. The count includes every exposed Diagnostic Task Attempt before and after suspension/resume. Resume never resets or expands it. A Plan may declare a smaller learner-visible maximum and may stop early.
- Runtime question selection may adapt only inside the frozen Plan. A suspected plan-external prerequisite or Rubric gap returns to the Concept Preparation Agent for a new validated version; runtime code cannot silently add coverage or change Plan version.
- A book-external prerequisite may be proposed from Target requirements only after a Gate validates the dependency against approved source authority. Its readiness task and any separate Learning Flow require an approved Concept Source Pack. Missing support produces Source Gap, not a readiness result or model-memory fallback.
- Before any prerequisite task is generated, read accepted Concept Progress for all Required Supporting Concepts. Direct reuse requires Independent or Durable plus matching Supporting Concept identity and relevant Mastery Rubric, criterion, and source-basis versions. Any relevant version change, lower milestone, absent progress, or externally learned knowledge uses the brief Prerequisite Readiness Check.
- A Prerequisite Readiness Check uses self-report before task generation. `not known` or `unsure` ends the check neutrally with a Prerequisite Learning Recommendation. `known` permits one small representative no-assistance task; self-report alone never establishes readiness.
- Remaining Required Supporting Concepts are checked in the Plan's dependency order. The first Conclusive gap, learner-declared unknown/unsure result, or terminal Unconfirmed result stops additional Diagnostic probing. Unchecked prerequisites and Target criteria remain Unknown.
- A Prerequisite Learning Recommendation names the Supporting Concept and explains why it is recommended without exposing the prior answer or solution. It offers only learner-controlled outcomes already decided here: explicitly start the Supporting Concept separately, choose Direct Learning for the current Target, or leave. The system never auto-creates or auto-starts the Supporting Flow.
- Explicitly starting the recommended Supporting Concept suspends the original Target Flow without an open Attempt. A return is learner-controlled. Version-aligned Independent or Durable progress satisfies readiness; otherwise the original Flow performs a brief recheck and resumes from committed Diagnostic state without repeating confirmed areas or resetting the Attempt count.
- Direct Learning is available before and at every Diagnostic learner interaction. It ends Diagnostic and enters Target Learning and Practice despite unknown, Unconfirmed, or Conclusive readiness. An open unsubmitted Diagnostic Attempt becomes Abandoned and creates no Finding. Submitted Attempts and Findings remain immutable. Direct Learning creates neither readiness nor Evidence and cannot authorize the direct Fresh Independent path; later normal Practice can establish Independent-test eligibility.
- Each submitted Diagnostic Attempt produces a passing observation, Conclusive Diagnostic Gap, or Unconfirmed Diagnostic Performance. These are per-Attempt facts; the application-owned Diagnostic Routing Decision aggregates committed prior progress, self-report, Findings, Direct Learning, coverage, and termination facts.
- Conclusive and Unconfirmed remain semantically distinct. Conclusive Findings may carry sanitized missing Rubric criteria and error dimensions. Unconfirmed carries neutral Feedback Facts only and requests a fresh Plan-authorized probe while budget remains. Technical or contract failure after submission remains Unavailable and produces no Finding or learner-failure route.
- After prerequisites are ready, Diagnostic probes only the Target Readiness Set. The complete Set must be positively confirmed before the Diagnostic Routing Decision may authorize a fresh Independent Test. Individual successful Attempts do not end the stage unless the Plan's accumulated coverage rule is satisfied.
- A direct post-Diagnostic Independent transition always generates and verifies a Fresh Equivalent Task. Diagnostic never establishes Independent and creates no Learning Evidence.
- A Conclusive Target gap routes to Target Learning and Practice. Confirmed strengths may compress or skip corresponding instruction, confirmed gaps may focus it, and unprobed criteria remain Unknown. Terminal Target Unconfirmed also enters Learning and Practice, but neutrally and without a deficit claim.
- Between submitted Diagnostic Attempts and before Fresh Independent, expose only a Neutral Transition plus completed-versus-maximum progress. Do not expose correctness, answers, solutions, rules, targeted feedback, or prerequisite Findings.
- Show a learner-safe Diagnostic Summary only after routing to Target Learning and Practice. It may contain confirmed strengths, teaching priorities, and Unknowns; it excludes raw responses, expected-answer facts, assessment output, evaluator reasoning, answers, solutions, and mastery claims.
- Each Diagnostic Task Blueprint explicitly disables rationale or enables `diagnostic.primary-or-corroborated-rationale@1`. Rationale is never required. Simple prerequisite tasks disable it by default. Enabling it requires a rationale-relevant Task Rubric mapping, and the learner projection explains that it is optional and may help interpret an incorrect primary answer.
- A confirmed-correct primary answer passes its Attempt and ignores rationale. A proven-incorrect primary answer is rescued only when both isolated Rationale Assessment and Rationale Sufficiency Verification return Applicable. Missing or definitively Not Applicable rationale produces a Conclusive Finding; semantic uncertainty or failed corroboration produces Unconfirmed. `Cannot Decide` never enables rationale rescue.
- Diagnostic produces no Learning Evidence on any branch. A submitted Attempt closes permanently, and later Findings, retries, routing, or Direct Learning never change its Attempt Purpose or retroactively convert it.
- Reuse the existing unified Learning Flow command boundary, Idempotency-Key, expected interaction version, FlowCommandReplay, submission contract, durable checkpoints, and learner-interaction projection. Exact new discriminator and record names are implementation details, not separate product decisions.
- Generate, gate, and verify every fresh Diagnostic task before its durable transition. Atomically commit the prior Attempt outcome, Diagnostic Finding, new Task Package and Attempt or terminal interaction, exposure, checkpoint, state, and processed command whenever they belong to one transition. Replay returns the committed result, and recovery resumes only missing work.
- Keep Diagnostic Findings and its Blackboard Flow-scoped. Cross-Flow coordination reads only committed, version-aligned Concept Progress and explicit Target-to-Supporting references, never raw responses, prompts, evaluator output, or another Flow's Blackboard.
- Learner-visible messages and interactions are projections of committed durable state. Model output cannot directly select an illegal transition, expand the Plan, write readiness, accept Evidence, or fabricate learner feedback.

## Testing Decisions

- The primary acceptance seam is the existing highest-level whole-flow scripted contract around the public Learning Flow command, committed learner interaction, and durable state. Extend that seam instead of creating a second Diagnostic end-to-end oracle.
- Primary tests assert externally observable behavior and durable effects, not internal method calls, prompt wording, state-reducer order, table layout, or model-call order.
- The whole-flow seam covers an Agent-authored, Gate-accepted Plan; learner-visible maximum; early stopping; neutral sequential Diagnostic tasks; Target Readiness Set completion; and Neutral Transition to a fresh Independent Test with no Diagnostic Evidence.
- The whole-flow seam covers prior Independent/Durable prerequisite progress under matching versions, version-mismatch recheck, lower/absent progress, learner self-report branches, representative prerequisite probe, first-recommendation stopping, Unknown preservation, and learner-controlled return without repeated confirmed work or count reset.
- The whole-flow seam covers Prerequisite Learning Recommendation followed separately by Direct Learning, leave/suspension, and explicit Supporting Concept Flow start. It asserts that the current Flow never teaches or auto-starts the Supporting Concept.
- The whole-flow seam covers Direct Learning before the first task, during an open Diagnostic task, and after committed Findings. It asserts open Attempt abandonment, committed Finding preservation, no readiness or Evidence, Target Learning entry, and later normal post-Practice Independent eligibility.
- The whole-flow seam covers Conclusive and Unconfirmed Target and prerequisite behavior, including fresh probing while budget remains, terminal neutral routes, the eight-Attempt ceiling, technical Unavailable separation, and no learner-failure claim from uncertainty.
- The whole-flow seam covers Neutral Transition privacy between tasks and before Independent, plus learner-safe Diagnostic Summary and Prerequisite Learning Recommendation projections after terminal routing. It asserts strict absence of answers, solutions, expected-answer facts, raw assessment data, evaluator reasoning, and private source trace.
- The whole-flow seam covers exactly-once replay, stale interaction version, wrong Attempt identity, idempotency payload conflict, crash recovery after a closed submitted Attempt, retry from saved evaluation responsibilities, and absence of duplicate Attempts, Findings, tasks, transitions, or Evidence.
- Focused Concept Preparation and Gate contract tests cover valid Plans; cyclic prerequisite dependencies; missing approved source; invalid Target Readiness Set; Rubric expansion; unjustified rationale enablement; Plan maximums from one through eight; rejection above eight; and frozen version behavior. These focused tests do not duplicate whole-flow routing assertions.
- Focused rationale contract tests retain the existing deterministic scripted seams for correct-primary short circuit, optional omission, two Applicable judgments, first Not Applicable, semantic Inconclusive, failed corroboration, `Cannot Decide`, contract repair, committed evaluation reuse, and technical Unavailable.
- Focused public API contract tests cover the closed command and interaction projections chosen during implementation, Idempotency-Key and interaction-version behavior, learner-safe error mapping, and absence of private fields. They do not create another state-machine oracle.
- PostgreSQL-backed recovery tests prove that Diagnostic count, accumulated Findings, suspension/resume, Direct Learning, saved submissions, and next-interaction transitions survive restart with the same exactly-once effects as in-memory behavior.
- ArchUnit tests continue to enforce module boundaries and the scheduler no-model-call invariant.
- Scripted fixtures remain the stable regression oracle. Live-model smoke tests are non-blocking, create no Evidence, and cannot replace deterministic acceptance.
- Run the repository's full clean test command after implementation, with PostgreSQL-backed tests enabled under the documented Docker environment.

## Out of Scope

- Learner-upload ingestion UI, concrete PDF/Markdown adapters, ownership, copyright, access control, sharing, retention, deletion, and source-safety policy. This Spec begins with approved Normalized Source Documents and Concept Source Packs.
- A psychometrically calibrated CAT/CCT, item-response parameters, a large calibrated item bank, confidence scores, entropy thresholds, or a claim that Diagnostic measures complete mastery.
- Global Learner Memory, inferred learner traits, cross-Flow raw conversation access, or reuse of another Flow's raw Diagnostic Findings.
- Automatic creation, automatic start, or in-Flow teaching of a Required Supporting Concept.
- Evidence, Mastery Milestones, or review scheduling for prerequisite checks or any other Diagnostic outcome.
- Multiple Target Concepts or multiple learner-visible Learning Objectives inside one Learning Flow.
- Runtime curriculum expansion, model-selected new prerequisites, model-memory source fallback, or mutation of a frozen Diagnostic Plan.
- Exact next-question heuristics inside an accepted Plan, concrete schemas, table/column names, class names, public discriminator names, and final UI copy.
- Compatibility layers, migration aliases, or preservation of the obsolete single-probe `Diagnostic Not Passed` route.
- Changes to Independent, Practice, Teach-back, Hint, Evidence acceptance, or Delayed Review semantics except where this Spec explicitly changes entry into those existing stages.

## Further Notes

- This Spec supersedes older README, reference-Spec, ADR, and ticket clauses that describe Diagnostic as one task, route every pass immediately to Independent, or combine Conclusive and Unconfirmed under one immediate `Diagnostic Not Passed` Guard route. Implementation removes the obsolete path rather than maintaining compatibility.
- The current manually prepared calculus artifacts remain a tracer fixture, not a permanent content-authoring decision. The product boundary is Agent-authored Concept Preparation from approved source material.
- The fixed eight-Attempt ceiling is a product limit for this Spec, not a model-selected value or runtime configuration escape hatch. A smaller Plan maximum is permitted.
- External assessment research informed the bounded stopping pattern, but this Spec deliberately does not claim psychometric adaptivity. Its routing semantics are the accepted domain decisions above.
- No new ADR is required. The decision baseline is carried by the amended existing ADRs for one Target Concept per Flow, Concept Contract preparation, cross-Flow memory, fresh post-Diagnostic Independent evidence, Neutral Transition, rationale-channel separation, and rationale corroboration.
