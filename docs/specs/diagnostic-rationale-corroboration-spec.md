# Spec: Corroborated rationale rescue for Diagnostic

> Status: draft. Two closed-contract details remain unresolved and are listed
> under Further Notes. This spec is not yet ready for implementation.

## Problem Statement

The current Diagnostic may advance to a Fresh Independent Test when either its
final-expression channel passes or one model judgment calls the learner's
rationale applicable. When the Mathematical Equivalence Check has already
proved the final answer wrong, a weak rationale such as `我不知道` can still be
misclassified as applicable and send the learner to Independent Test. This is
too permissive: an incorrect primary answer and an insufficient rationale do
not provide enough confirmed performance to bypass Learning and Practice.

The existing Response Verification does not protect this branch. It is invoked
only when the mathematical result is `Cannot Decide`; when invoked, the real
adapter currently gives Assessment and Response Verification the same Strong
Model Binding, system prompt, and context. Repeating that judgment does not
provide the counterexample-oriented corroboration required for rationale
rescue.

The current recovery behavior also mixes semantic uncertainty with technical
failure. A valid semantic `inconclusive` is an evaluation result, while a
Provider failure or a model response that remains outside its closed contract
after repair is a system problem. A system problem after formal submission
must not route the learner into remediation or cause a replacement task.

Finally, the append-only Assessment store cannot distinguish versioned
evaluation responsibilities. It cannot reliably resume between the first
rationale judgment, the corroborating judgment, and the final state transition
without rerunning already committed work.

## Solution

Replace the current Diagnostic rationale-rescue policy with the explicitly
opted-in `diagnostic.primary-or-corroborated-rationale@1` Assessment Policy.
The current polynomial Diagnostic uses its existing deterministic Mathematical
Equivalence Check as the Trusted Primary-Answer Check.

A proven-correct primary answer continues to pass immediately. A
proven-incorrect answer may be rescued only when a non-empty rationale is
independently judged Applicable by both Rationale Assessment and Rationale
Sufficiency Verification. The first judgment uses a normal evaluation method;
the second uses a counterexample-oriented method and cannot see the first
result. Both use the same cross-subject sufficiency criteria and the same
least-privilege task facts. Merely naming a concept or matching a keyword is
never sufficient.

A missing or conclusively Not Applicable rationale produces a Conclusive
Diagnostic Gap. Semantic uncertainty, evaluator disagreement, or an unresolved
primary answer produces Unconfirmed Diagnostic Performance. Both are
Diagnostic Not Passed and route through the same Workflow Guard to Learning
and Practice, but only the conclusive branch may carry sanitized missing
criteria and error dimensions. Technical failure reaches a durable Unavailable
Interaction with learner-controlled Retry.

The evaluation method is packaged in frozen Evaluation Skill Stacks separate
from every Teaching Skill Stack. Successful post-submission evaluation results
are committed by Attempt, responsibility, and evaluation version before the
next model call or state transition, so replay and Retry resume only missing or
technically failed responsibilities.

## User Stories

1. As a learner, I want an incorrect Diagnostic answer with no rationale to
   lead to Learning and Practice, so that missing performance is not sent to an
   Independent Test.
2. As a learner, I want an incorrect answer with `我不知道` as its complete
   rationale to remain in Learning and Practice, so that a content-free reason
   cannot rescue my answer.
3. As a learner, I want the complete meaning of my rationale evaluated, so that
   the system does not classify me through a keyword blacklist.
4. As a learner, I want hesitant wording followed by a complete, correct
   justification to remain eligible for rescue, so that tone is not confused
   with capability.
5. As a learner, I want merely naming a rule, principle, concept, or source to
   be insufficient, so that passing requires an actual connection to the task.
6. As a learner, I want a materially incorrect, incomplete, or contradictory
   rule application to be Not Applicable, so that incorrect reasoning cannot
   rescue an incorrect answer.
7. As a learner, I want a proven-correct primary answer to pass without optional
   rationale evaluation, so that the optional field does not become a hidden
   proof requirement.
8. As a learner, I want an incorrect primary answer with two independently
   confirmed Applicable judgments to reach a Fresh Independent Test, so that a
   demonstrated method can still reveal readiness after an execution or
   transcription mistake.
9. As a learner, I want evaluator disagreement to route me to Learning and
   Practice without claiming a specific deficit, so that evaluator uncertainty
   is not presented as my failure.
10. As a learner, I want an unresolved primary answer to avoid rationale rescue,
    so that uncertainty in the primary channel cannot be bypassed by a separate
    semantic judgment.
11. As a learner, I want every passing Diagnostic to use the existing Neutral
    Transition and a Fresh Independent Test, so that the Diagnostic itself does
    not become Independent Evidence or assistance.
12. As a learner, I want Provider, timeout, configuration, or malformed-model
    failures after submission to produce a retryable Unavailable Interaction,
    so that a system problem does not change my learning path.
13. As a learner, I want Retry to use my saved submission rather than request a
    replacement answer, so that the formal Attempt remains immutable.
14. As a learner, I want replayed commands to return the committed interaction,
    so that retries cannot create duplicate tasks, evidence, or transitions.
15. As an operator, I want the rationale-sufficiency method to be free of
    calculus-specific instructions and fields, so that the capability has a
    subject-neutral architecture.
16. As an operator, I want subject truth to remain in the Task Rubric, expected
    answer facts, and approved Source Passages, so that reusable Skills do not
    become hidden answer keys.
17. As an operator, I want the current Flow-frozen Strong Model Binding reused
    for both rationale judgments in V1, so that this change does not add a third
    model or Provider binding.
18. As an operator, I want the two judgments to use different Evaluation Skills
    but one shared sufficiency Skill, so that their criteria agree while their
    review methods differ.
19. As an operator, I want bounded model-call and repair budgets, so that the
    system cannot keep asking until it obtains an Applicable verdict.
20. As an operator, I want no hidden Provider retry loop, so that Unavailable
    behavior and model cost remain explicit.
21. As a platform developer, I want rationale rescue enabled only by an explicit
    Assessment Policy reference, so that other Diagnostics do not inherit it by
    default.
22. As a platform developer, I want a Trusted Primary-Answer Check to be a
    precondition for this policy, so that model votes cannot manufacture the
    proven-incorrect branch.
23. As a platform developer, I want Evaluation Profiles separated from Teaching
    Node Profiles, so that judging cannot teach, route, mutate state, or accept
    Evidence.
24. As a platform developer, I want Evaluation Skill Stacks to reuse the current
    Manifest, Registry, Loader, SemVer identity, and content hash, so that there
    is no parallel plugin mechanism.
25. As a platform developer, I want the existing Teaching `BundleStack` action
    invariant preserved, so that adding evaluation does not weaken the meaning
    of Teaching Action Skills.
26. As a platform developer, I want both rationale evaluators to receive the
    same least-privilege facts without seeing one another, so that the
    corroboration remains isolated and reproducible.
27. As a platform developer, I want closed dimension checks and deterministic
    verdict derivation, so that a model cannot declare Applicable while its own
    checks disagree.
28. As a platform developer, I want every successful evaluation responsibility
    committed before the next responsibility runs, so that crash recovery can
    skip completed judgments.
29. As a platform developer, I want one durable evaluation-result identity per
    Attempt, responsibility, and version, so that concurrent or replayed work
    cannot commit duplicate outcomes.
30. As a platform developer, I want the exact model-invocation guarantee stated
    honestly, so that state transitions are exactly-once without claiming an
    unavailable distributed transaction with model Providers.
31. As a maintainer, I want Independent, Practice, Review, and pre-delivery Task
    Verification policies preserved, so that this Diagnostic correction does
    not silently change unrelated assessment behavior.
32. As a maintainer, I want obsolete policy and Assessment storage paths removed
    instead of dual-written or aliased, so that the repository retains one
    current behavior.

## Implementation Decisions

- Scope is the existing Apply Diagnostic and the shared post-submission
  evaluation-recovery infrastructure required by this change. No new public
  learner command or response field was decided.
- The accepted domain language is Diagnostic Not Passed, Conclusive Diagnostic
  Gap, Unconfirmed Diagnostic Performance, Applicable Rationale, Rationale
  Assessment, Rationale Sufficiency Verification, Rationale Evaluation Context,
  Rationale Evaluation Result, Trusted Primary-Answer Check, Evaluation Profile,
  Evaluation Skill, Evaluation Skill Stack, Committed Evaluation Result, and
  Assessment Policy.
- An Applicable Rationale supplies the task-relevant knowledge, rules,
  principles, or evidence required by the Task Rubric; connects them to the
  specific task; and contains no material gap, error, or contradiction. It may
  coexist with an incorrect primary answer, but a concept name or keyword match
  is insufficient.
- The current Diagnostic destructively replaces
  `diagnostic.final-or-applicable-rationale@1` with
  `diagnostic.primary-or-corroborated-rationale@1`. There is no alias, fallback,
  or compatibility mapping.
- The policy is explicit opt-in and requires a Trusted Primary-Answer Check.
  For V1, the existing proof-bounded Mathematical Equivalence Check is the only
  integrated check. Model agreement and `Cannot Decide` are not proof of an
  incorrect primary answer.
- A proven-correct primary answer passes immediately, ignores the optional
  rationale, creates no Diagnostic Evidence, and uses the Neutral Transition to
  a Fresh Independent Test.
- A proven-incorrect primary answer with a `null`, empty, or whitespace-only
  rationale becomes a Conclusive Diagnostic Gap without a model call.
- Every non-empty rationale is evaluated as a whole. There is no keyword,
  phrase, hedge, or sentence-shape classifier before Rationale Assessment.
- A proven-incorrect answer with a first `not_applicable` becomes a Conclusive
  Diagnostic Gap and skips corroboration.
- A first `inconclusive` skips corroboration and becomes Unconfirmed Diagnostic
  Performance.
- A first `applicable` invokes Rationale Sufficiency Verification. Only a second
  `applicable` passes; second `not_applicable` or `inconclusive` becomes
  Unconfirmed Diagnostic Performance.
- `Cannot Decide` never enables rationale rescue. The existing isolated
  primary-answer judgments must both confirm correctness for the Diagnostic to
  pass; otherwise the result is Unconfirmed Diagnostic Performance.
- Conclusive Diagnostic Gap and Unconfirmed Diagnostic Performance both map to
  Diagnostic Not Passed, create no Learning Evidence, and expose only Explain
  and Apply Practice through a `DIAGNOSTIC_NOT_PASSED` Workflow Guard context.
- Conclusive Diagnostic Gap may provide sanitized missing Rubric criteria and
  error dimensions to the Pedagogy Agent. Unconfirmed Diagnostic Performance
  supplies neutral Feedback Facts and makes no claimed learner deficit.
- The first Evaluation Stack is fixed to
  `evaluation.rationale-assessment@1.0.0` and
  `verification.rationale-sufficiency@1.0.0`.
- The corroborating Evaluation Stack is fixed to
  `evaluation.counterexample-review@1.0.0` and the same
  `verification.rationale-sufficiency@1.0.0`.
- Neither V1 Stack loads a Subject Skill or performs dynamic Skill routing.
  The Evaluation Profile, Skills, context, and result contract contain no
  calculus-, derivative-, or polynomial-specific term or field.
- Both Profiles use the same Flow-frozen Strong Model Binding. The first
  Evaluation Skill performs normal rationale assessment; the second actively
  searches for missing support, misapplication, and contradiction. The second
  does not see the first result.
- Both Profiles receive separate instances of the same closed Rationale
  Evaluation Context containing learner-visible task text, the complete
  rationale, rationale-relevant Task Rubric criteria, necessary private
  expected-answer facts, bounded approved Source Passages, and Learner Locale.
- The context excludes the learner's primary answer, the Trusted
  Primary-Answer Check result, both evaluator results and reason codes, prior
  Attempts and feedback, Learning State, and generator reasoning. The outer
  Assessment Policy owns invocation and combination, so the evaluators are not
  told that the rationale may rescue an incorrect answer.
- Both Profiles return `rationale_evaluation/v1` with a declared verdict,
  dimension checks for Rubric basis, task connection, and coherence, and closed
  reason codes. The result contains no feedback, routing, Evidence decision,
  confidence score, or model reasoning.
- Dimension values are `pass`, `fail`, or `inconclusive`. All passing derives
  `applicable`; any failed check derives `not_applicable`; no failed check with
  at least one inconclusive check derives `inconclusive`. A declared verdict
  inconsistent with its checks is Model Contract Invalid.
- `BundleSlot` adds `EVALUATION`. Existing Teaching `BundleStack` rules stay
  unchanged. A separate `EvaluationBundleStack` requires exactly one
  `EVALUATION` and one `VERIFICATION` Skill and rejects every other Slot in V1.
- Evaluation composition reuses the existing Manifest schema, Registry, Loader,
  immutable SemVer identity, and content hash.
- A dedicated Rationale Evaluation Prompt Compiler assembles Profile,
  Evaluation Skill, Verification Skill, and response contract in that fixed
  order under its own instruction budget. A universal cross-Profile compiler
  is not introduced.
- The normal proven-incorrect rescue path makes at most two model calls. Each
  responsibility permits at most one contract repair with the same Strong
  Binding, Evaluation Profile, Evaluation Skill Stack, and Rationale Evaluation
  Context, plus normalized contract violations.
- A valid `not_applicable` or `inconclusive` is never semantically retried.
  Provider failure or timeout has no hidden automatic retry. The worst path in
  which both responsibilities require their one contract repair makes at most
  four calls.
- A Provider, timeout, or configuration failure, or a post-submission result
  that remains Model Contract Invalid after repair, commits an Unavailable
  Interaction with a Pending Operation. It creates no Evidence, replacement
  task, Diagnostic Not Passed transition, or learner-failure signal.
- A valid closed-contract `inconclusive` remains a semantic evaluation result.
  Pre-delivery Task Verification continues to treat an invalid result as an
  inconclusive candidate verification. Pedagogy and Clarification retain their
  existing safe fallbacks.
- Every post-submission evaluation responsibility commits one Committed
  Evaluation Result before the next responsibility or state transition. Its
  identity is Attempt, responsibility, and evaluation version.
- Durable storage destructively replaces the append-only `assessments` path
  with `evaluation_results`, including Attempt identity, responsibility,
  evaluation version, result schema, closed JSON payload, and creation time.
  `(attempt, responsibility, evaluation version)` is unique.
- Response Assessment, Response Verification, Rationale Assessment, Rationale
  Sufficiency Verification, and Teach-back Assessment use one
  save-or-return-committed persistence contract. A concurrent unique-key winner
  is the result used downstream.
- The old Assessment table and `recordResponseAssessment`/`assessmentsFor`
  paths are removed without dual writes or data migration. Model Contract Audit
  remains separate.
- Pending Operation adds `RESUME_SUBMISSION_EVALUATION` and carries only the
  Attempt and required-responsibility identities. It does not copy the learner
  submission or evaluation JSON.
- Replay and Retry first rehydrate the closed Attempt, Task Package, saved
  submission, and Committed Evaluation Results. Completed responsibilities are
  skipped; explicit Retry invokes only a missing or technically failed
  responsibility.
- The original Idempotency-Key replay returns its original committed
  interaction. Learning Evidence, Learning Stage, and learner interaction
  transitions remain exactly-once.
- A Provider call can repeat only in the unavoidable crash window after the
  Provider returns and before the result commits. V1 does not claim
  exactly-once external model invocation because the Provider Catalog has no
  common transactional idempotency contract.
- Independent, Practice, Review, and their Assessment Policies remain unchanged
  except that the shared post-submission malformed-contract recovery rule now
  reaches Unavailable as defined by ADR-0076.

## Testing Decisions

- The primary acceptance seam is the existing public Learning Flow command
  followed by its committed learner interaction and durable state. Tests use
  scripted model ports and assert learner-visible behavior, routing, call
  counts, durable evaluation results, and Evidence effects rather than prompt
  snapshots or hidden implementation details.
- The new Profile Contract seam covers both frozen Evaluation Skill Stacks,
  context visibility, subject-neutral prompt artifacts, closed result parsing,
  deterministic verdict derivation, repair, Provider failure, and absence of
  state-write or learner-feedback authority.
- The PostgreSQL recovery seam covers Committed Evaluation Results, unique
  responsibility/version identity, Pending Operation recovery, process restart,
  and exactly-once committed interactions and Evidence effects.
- A proven-correct primary answer with any optional rationale makes no Rationale
  Assessment or Rationale Sufficiency Verification call and opens a Fresh
  Independent Test through the Neutral Transition.
- A proven-incorrect answer with no rationale makes no evaluation call and
  routes to Learning and Practice as a Conclusive Diagnostic Gap.
- A proven-incorrect answer with `我不知道` receives first `not_applicable`,
  makes no corroboration call, and routes to Learning and Practice.
- A hesitant but complete correct rationale receives two `applicable` results
  and opens a Fresh Independent Test.
- First `inconclusive`, first `applicable` plus second `not_applicable`, and first
  `applicable` plus second `inconclusive` each produce Unconfirmed Diagnostic
  Performance, neutral Feedback Facts, and Learning and Practice routing.
- `Cannot Decide` passes only when both existing primary-answer judgments
  confirm correctness; every other semantic result becomes Unconfirmed
  Diagnostic Performance and cannot use rationale rescue.
- Every Diagnostic branch asserts that no Learning Evidence is created.
- Contract tests cover each Profile's initial malformed result followed by a
  valid repair, and a second malformed result followed by Unavailable.
- Provider, timeout, and configuration failures for each post-submission
  responsibility produce Unavailable without Evidence, replacement task, or
  Learning and Practice routing.
- A first committed Rationale Assessment followed by unavailable corroboration
  resumes only corroboration on explicit Retry.
- Two committed rationale results followed by a simulated crash before routing
  resume the deterministic combination without another model call.
- Original-command replay returns the committed interaction without another
  evaluation or state transition.
- PostgreSQL restart tests prove the same behavior across process recovery and
  prove the unique evaluation-result identity prevents duplicate committed
  results.
- Regression contracts prove that Independent, Practice, Review, Task
  Verification, Pedagogy, and Clarification retain the behaviors not amended by
  this spec.
- Structural tests prove the Evaluation Profile, both Evaluation Skill cores,
  result contract, and context field names contain no calculus-, derivative-,
  or polynomial-specific contract or instruction content. They do not claim
  cross-subject model quality.
- Scripted contract fixtures remain the stable regression oracle. Live-model
  smoke tests remain non-blocking and create no evidence.
- Verification completes with `./mvnw clean test`; PostgreSQL-backed recovery
  tests require the repository's documented Docker Compose service.

## Out of Scope

- A non-mathematical Evaluation Profile fixture or another product Concept.
- Demonstrated cross-subject live-model quality.
- A new non-mathematical Trusted Primary-Answer Check or general checker
  registry.
- A third model, a second Provider binding, model voting, or learner-selectable
  evaluation models.
- Dynamic Evaluation Skill routing, model-selected Skill IDs, or a Subject
  Skill in either V1 Evaluation Stack.
- Reusing the Apply Teaching Skill Stack for Assessment.
- Keyword blacklists, phrase classifiers, or deterministic classification of
  any non-empty rationale.
- Changes to Independent, Practice, Review, or pre-delivery Task Verification
  assessment semantics beyond ADR-0076's shared post-submission recovery rule.
- Diagnostic Learning Evidence or direct Independent milestone establishment.
- A new public command, answer field, or learner-facing exposure of evaluation
  facts.
- Backward-compatible aliases for the old Assessment Policy, dual writes to the
  old Assessment store, or migration of obsolete Assessment rows.
- Exactly-once Provider invocation across a crash between Provider response and
  local commit.
- Live-model smoke output as acceptance evidence.
- A ticket or issue-tracker publication for this spec.

## Further Notes

- This spec implements the accepted decisions in ADR-0075, ADR-0076, and
  ADR-0077 and amends the rationale-rescue portions of ADR-0057 and the
  post-submission contract-recovery portion of ADR-0071.
- The confirmed testing seams were established during the preceding design
  session; no additional seam decision was introduced while synthesizing this
  document.
- The exact closed `reason_codes` enumeration for
  `rationale_evaluation/v1` remains undecided. The spec records only the agreed
  categories—missing support, misapplication, factual error, material gap, and
  contradiction—and does not invent enum identifiers.
- The closed representation of `necessary private expected-answer facts` in
  the Rationale Evaluation Context remains undecided, including how the current
  Trusted Primary-Answer Check basis is represented in that data. The spec does
  not introduce an open map or a generic checker-reference schema.
- Until those two closed-contract details are resolved, this spec must not be
  treated as ready for implementation.
