# Progress Log: Diagnostic Flow optimization discovery

## Session: 2026-08-21

### Phase 1: Repository and research discovery
- **Status:** complete
- Read the user-supplied AGENTS.md and the complete `grill-with-docs`, `grilling`, `domain-modeling`, and `planning-with-files` skill instructions and required formats/templates.
- Restored and inspected prior planning context; the prior active plan is complete and concerns ticket 5 implementation.
- Created this isolated planning record and made it active without altering older plan records.
- Read the repository rules, architecture/runbook, and the complete domain glossary in chunks; recorded the already-existing Conclusive/Unconfirmed and rationale-corroboration distinctions.
- Enumerated all ADRs/specs/tickets mentioning Diagnostic and read the two most direct normative specs; identified where the newer corroboration spec supersedes the older Apply reference wording.
- Read the current Learning/Practice and reliability specs with emphasis on the post-Diagnostic Guard, Pedagogy Agent, immutable submitted Attempt, retry boundary, and Independent readiness rule.
- Read the accepted ADR baseline and relevant tickets; mapped likely new decisions to existing ADR owners and found several stale historical clauses/statuses that should not be mistaken for live semantics.
- Located and read the Diagnostic orchestration, assessment combination, and Workflow Guard implementation; confirmed single-probe structure and where Conclusive/Unconfirmed are collapsed.
- Traced Diagnostic result handling through the Learning StateGraph and checked for durable probe/stopping state; confirmed no intermediate Diagnostic outcome or counter exists.
- Read the public interaction record and whole-flow/Guard tests, then checked recent commit history to confirm the rationale-corroboration baseline is shipped on `main`.
- Researched authoritative adaptive/classification assessment patterns and a mature product implementation (ETS, ALEKS, and peer-reviewed cognitive-diagnostic CAT); recorded the implications and the limits of transferring full CAT architecture to this Phase 0 system.
- Added mature-product comparisons from Khan Academy and Duolingo, focusing on multiple observations per skill, disagreement handling, and the false-negative cost of binary routing.
- Inspected the reference UI and Diagnostic Blueprint; confirmed the rationale is visibly optional, the rubric is one criterion, and sequential Diagnostic tasks fit the existing task interaction shape.

### Phase 2: Decision-tree grilling
- **Status:** in_progress
- Derived the high-impact decision tree from repository and research evidence.
- Prepared the first one-at-a-time question: whether a first Diagnostic Not Passed should receive one bounded Fresh Diagnostic Confirmation.
- User rejected the narrow one/two-item gate framing and defined Diagnostic as a pre-learning assessment that may probe prerequisite knowledge and adapt the later learning phase.
- Re-read ADR-0001/0003/0006/0007 and Supporting Concept boundaries. The current accepted model permits Supporting Concepts as context but gives them no state or Evidence in the Target Concept's Flow; teaching or assessing one as a target requires a separate Flow.
- Repository search found Supporting Concept only in glossary/ADR design, not in the current runtime slice, so prerequisite diagnostic execution is a new product capability rather than an unused code path.
- Updated the `Diagnostic` glossary definition inline to reflect the resolved multi-Attempt pre-learning assessment meaning without prematurely deciding cross-Concept state ownership.
- User accepted the single-Target ownership boundary: non-blocking prerequisite findings adapt the current Flow; a blocking prerequisite uses a separate Supporting Concept Flow.
- Added `Diagnostic Finding` to the glossary and amended ADR-0001 and ADR-0022. No new ADR was created.
- User clarified that the target product uses an Agent as the preparation author after a user supplies a book; manual reference artifacts are temporary fixtures, not the desired authority model.
- Verified ADR-0033 already records the fixture as non-normative for product input, while ADR-0041 keeps upload ownership and safety as a separate future boundary.
- Added `Concept Preparation Agent` to the glossary; no ADR amendment was needed for this already-compatible intent.
- User accepted automatic publication of Gate-accepted internal preparation artifacts with learner confirmation limited to the visible Concept Contract.
- Tightened the `Concept Preparation Agent` definition and amended ADR-0016; no new ADR was created.
- User accepted frozen-plan runtime authority: Diagnostic adapts only inside an accepted Plan and returns plan-external gaps for a new Concept Preparation version.
- Added `Diagnostic Plan` to the glossary and extended ADR-0016; no new ADR was created.
- User accepted minimum-sufficient early stopping and added a hard product requirement that genuinely necessary prerequisite knowledge be repaired before Target learning.
- Added `Required Supporting Concept` and refined the `Diagnostic` glossary definition. The treatment of persistent Unconfirmed prerequisite readiness remains the next decision.
- User accepted positive Prerequisite Readiness for every Required Supporting Concept; persistent Unconfirmed withholds eligibility neutrally, while technical failure remains Unavailable.
- Added `Prerequisite Readiness` and amended ADR-0001; no new ADR was created.
- Verified the accepted readiness rule is present in the glossary and ADR-0001; prepared the next product decision on the committed return threshold from a Required Supporting Concept Flow.
- User clarified that Required Supporting Concepts are only brief prerequisites, not secondary mastery goals: a positive Diagnostic check must continue the Target Flow without a redundant Independent Test. Retracted the stronger Independent-milestone recommendation before documenting it in an ADR.
- User clarified the hard-gap route: the current Target Flow blocks and recommends learning the prerequisite, but does not remediate it in-flow or auto-create another Flow; the learner must explicitly start the Supporting Concept Flow.
- Added `Prerequisite Learning Recommendation` and refined ADR-0001/0022; no new ADR was created.
- User prioritized database-backed personalization and a very small prerequisite screening budget: aligned prior Concept Progress may bypass testing, while unknown prerequisites receive only a brief check.
- Refined `Diagnostic Plan`, `Prerequisite Readiness`, ADR-0001, and ADR-0016; no new ADR was created.
- User accepted that self-report may route and shorten prerequisite screening but cannot positively establish readiness by itself.
- Added `Prerequisite Readiness Check` and refined `Prerequisite Learning Recommendation`/ADR-0001; no new ADR was created.
- User accepted the source-authority boundary for book-external prerequisites: Gate-validated approved material is required for blocking, testing, and teaching; otherwise the system reports Source Gap.
- Refined `Required Supporting Concept` and amended ADR-0016/0001; no new ADR was created.
- User accepted distinct Conclusive/Unconfirmed continuation semantics and target-versus-prerequisite terminal routes.
- Replaced the obsolete single-result `Diagnostic Not Passed` glossary term with `Diagnostic Routing Decision` and amended ADR-0042/0043/0075; no new ADR was created.
- User accepted prerequisite-first dependency order and early termination at the first sufficient block; unprobed dimensions stay Unknown and later resume from committed state.
- Recorded the previously accepted return rule: aligned Independent/Durable prerequisite progress bypasses rechecking; other cases receive only a brief readiness recheck.
- Refined `Diagnostic Plan`, `Prerequisite Readiness`, `Diagnostic Routing Decision`, and ADR-0001/0016/0022; no new ADR was created.
- User accepted an Agent-authored, Gate-validated Target Readiness Set as the minimum representative Target coverage for direct Fresh Independent eligibility.
- Added `Target Readiness Set` and amended ADR-0001/0016/0042; no new ADR was created.
- User accepted Plan-specific visible maximum length under a platform hard ceiling, with early stopping and no runtime extension.
- Refined `Diagnostic Plan`/`Diagnostic`, amended ADR-0016, and removed a remaining obsolete `Diagnostic Not Passed` clause from ADR-0075; no new ADR was created.
- User stated that a learner may skip Diagnostic and begin Target learning even without prerequisite knowledge. This conflicts with the earlier unconditional prerequisite gate; paused documentation changes pending confirmation of an explicit learner-override model.
- User confirmed the reconciliation: prerequisite readiness controls the recommended/direct-Independent route, while an explicit learner override may enter Target Learning and Practice without manufacturing readiness or Evidence.
- Added `Direct Learning Choice` and amended `Required Supporting Concept`, `Prerequisite Readiness`, `Prerequisite Learning Recommendation`, `Diagnostic Routing Decision`, `Unconfirmed Diagnostic Performance`, and ADR-0001/0016/0022/0075; no new ADR was created.
- User accepted neutral between-task and pre-Independent transitions, with sanitized Diagnostic Summary feedback only after entering Target Learning and Practice.
- Added `Diagnostic Summary`, broadened `Neutral Transition`, and amended ADR-0001/0043; no new ADR was created.
- User accepted Blueprint-selected, always-optional Diagnostic rationale with learner-visible effect and existing two-evaluator corroboration.
- Refined `Task Blueprint` and amended ADR-0016/0057/0075, correcting ADR-0057's superseded single-applicable wording; no new ADR was created.
- Began convergence audit; corrected one stale discovery-summary clause that still described prerequisite recommendations as unconditional Flow blocks.
- Completed the full glossary/ADR consistency audit: no modified ADR retains the old single `Diagnostic Not Passed` immediate route or a non-overridable prerequisite gate.
- Identified README, existing reference specs, and rationale-corroboration tickets that still describe the shipped single-probe baseline; recorded them as explicit supersession work for `/to-spec`, not as an unresolved product decision.
- `git diff --check` passes. No code or tests were run because this user-requested session is discovery/documentation only.
- Initial grilling converged with the numeric cap and prior-progress version-alignment policy explicitly deferred; the subsequent `/to-spec` readiness review reopened both before publication.
- User delegated the final recommendations: fixed the platform hard ceiling at eight Diagnostic Attempts per frozen Plan version including resume, and fixed strict prior-progress reuse to matching Supporting Concept plus relevant Mastery Rubric/criterion/source-basis versions.
- Configured GitHub Issues as the repository tracker with default triage labels and single-context domain documentation.
- Drafted and locally validated `docs/specs/diagnostic-flow-optimization-spec.md`.
- Published the Spec as GitHub Issue #7 with `ready-for-agent`; remote verification confirmed the title, open state, label, required template sections, eight-Attempt ceiling, and strict version-alignment clause.
- `./mvnw clean test` reached `kiln-ai-app` after the first six reactor modules passed, then failed for an environment-level Mockito/Byte Buddy self-attach restriction: 29 application-test initialization errors, zero assertion failures; PostgreSQL-backed tests were skipped because Docker was unavailable.

### Files created/modified
- `CONTEXT.md`
- `docs/adr/0001-one-target-concept-per-learning-flow.md`
- `docs/adr/0016-confirm-a-concept-contract-before-the-first-learning-flow.md`
- `docs/adr/0022-defer-cross-flow-learner-memory-in-phase-0.md`
- `docs/adr/0042-confirm-independent-evidence-on-a-fresh-post-diagnostic-task.md`
- `docs/adr/0043-use-a-neutral-transition-from-diagnostic-to-independent-test.md`
- `docs/adr/0057-separate-apply-final-expression-and-rationale-assessment-channels.md`
- `docs/adr/0075-compose-rationale-verification-through-an-evaluation-skill-stack.md`
- `docs/specs/diagnostic-flow-optimization-spec.md`
- `docs/agents/issue-tracker.md`
- `docs/agents/triage-labels.md`
- `docs/agents/domain.md`
- `AGENTS.md`
- `.planning/2026-08-21-diagnostic-flow-optimization-discovery/task_plan.md`
- `.planning/2026-08-21-diagnostic-flow-optimization-discovery/findings.md`
- `.planning/2026-08-21-diagnostic-flow-optimization-discovery/progress.md`
- `.planning/.active_plan`

### Errors
| Error | Resolution |
|-------|------------|
| Initial planning-file patch could not delete and add each same path in one patch | Used an in-place update patch |
| Combined README/CONTEXT read exceeded direct output limits | Re-read CONTEXT in bounded chunks and used targeted line searches; continue with smaller ranges |
| `./mvnw clean test` could not initialize Mockito's inline MockMaker in `kiln-ai-app` | Recorded the pre-existing local JVM attach restriction; no product code was changed for this documentation-only task |
| A findings patch used a resource anchor that was not present | Re-read the planning file and applied a narrower patch |
| One inspection used `domain/learning/model/LearningFlowInteraction.java`, but the file lives under `domain/apply/model` | Used repository search output to correct the path for the next read |
| A combined glossary/planning patch failed on an overly broad findings anchor | Re-read exact context and applied narrower hunks |
| Agent-authoring documentation patch used an overly broad findings anchor | Re-read the exact section and split the update |
| A shell read contained an unmatched double quote due to a backtick in its pattern | Re-ran with a single-quoted pattern and avoided command substitution |
| ADR inspection reused stale descriptive filenames for ADR-0042/0043/0075 | Located actual filenames with `rg --files` and re-read current contents; ADR-0075 does own the rationale-result route semantics |
| A combined documentation patch targeted this file in two update operations | The patch was rejected atomically; reapplied as separate documentation and planning patches |
| A planning-record patch used an inexact progress-line anchor | Re-read the exact tail and reapplied against the current wording |
