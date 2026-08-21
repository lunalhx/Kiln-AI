# Findings & Decisions: Diagnostic Flow optimization discovery

## Requirements
- Improve the Diagnostic Flow whose current summary is: Start generates one Diagnostic Task; submission passes directly to a Fresh Independent Test, while Failed or Unconfirmed offers Explain and Apply Practice.
- Diagnostic produces no Evidence.
- Inconclusive/Unconfirmed and Failed currently share remediation routing.
- A submitted Diagnostic failure never converts retroactively.
- Investigate whether a one-item Diagnostic is sufficient and whether Failed and Unconfirmed should share remediation.
- Investigate facts independently; ask the user only high-impact product, UX, architecture-boundary, security/permission, compatibility, or hard-to-reverse questions.
- Proactively expose edge cases and implicit choices.
- Ask one decision question at a time and provide a recommendation.
- Stop when high-impact Spec questions are resolved; do not implement.

## Research Findings
- `grill-with-docs` composes a one-question-at-a-time `grilling` session with active domain modeling.
- Domain terms must be reconciled with `CONTEXT.md`; resolved terms are updated inline and contain no implementation details.
- ADR changes are appropriate only for decisions that are simultaneously hard to reverse, surprising without context, and the result of a real trade-off.
- An isolated planning record was created because the root planning files and the previously active plan belong to completed implementation tasks.
- The checked-in glossary already distinguishes `Diagnostic Not Passed` into `Conclusive Diagnostic Gap` and `Unconfirmed Diagnostic Performance`; they share the broad Learning and Practice destination but intentionally differ in the claims and Feedback Facts they may carry.
- The repository is already beyond the user's abbreviated baseline: an incorrect primary answer may be rescued toward a Fresh Independent Test only when an Applicable Rationale is corroborated by two isolated evaluation responsibilities. A failed corroboration becomes Unconfirmed, not Conclusive.
- `Diagnostic` is currently defined as one initial, brief, no-hint attempt; a pass cannot establish Independent and must be followed by a Fresh Independent Test.
- The existing invariant that Attempt Purpose never changes retroactively already expresses the requested “submitted Diagnostic failure never converts” boundary.
- The README still describes a passing Diagnostic as a direct neutral transition, but the detailed glossary adds a rationale-rescue path. Specs, ADRs, code, and tests must determine the normative current behavior.
- The worktree was clean apart from this session's isolated planning directory and active-plan pointer.
- The normative rationale-corroboration spec explicitly settles the current branches: primary proven correct passes immediately; primary proven incorrect plus missing/first Not Applicable rationale is Conclusive; first Inconclusive, evaluator disagreement, or unresolved primary answer is Unconfirmed; two isolated Applicable judgments can rescue a proven-incorrect answer.
- Both Conclusive and Unconfirmed currently enter one `DIAGNOSTIC_NOT_PASSED` Guard context exposing Explain and Apply Practice, but their downstream inputs differ: Conclusive may carry sanitized missing Rubric criteria/error dimensions; Unconfirmed must carry neutral facts and make no deficit claim.
- Technical/model-contract failure is not Unconfirmed: after a submission it commits a durable Unavailable Interaction and can resume the saved evaluation. This is a distinct recovery branch and must remain separate from product-semantic uncertainty.
- The older Apply reference spec still says a Diagnostic passes on one Applicable rationale and treats still-invalid assessment contracts as Inconclusive; these clauses were destructively superseded by the newer diagnostic-rationale spec and ADR-0076/0078. Discovery must avoid treating the older wording as current behavior.
- The latest spec constrains rationale rescue but does not address diagnostic test length/adaptive stopping; that is a genuinely open product design area rather than an implementation gap already answered in documents.
- The Learning/Practice spec confirms that `Diagnostic Not Passed` is not a literal fixed next screen: the Guard exposes legal remediation actions and the Pedagogy Agent may choose Explain or fresh Apply Practice. A later Independent Test is gated by at least one conclusive Apply Practice pass after the triggering failure.
- The existing product already differentiates the two not-passed classes through pedagogy inputs, even though both have the same candidate actions. Therefore the real open question is whether Unconfirmed needs a different allowed action/state transition (for example another Diagnostic probe), not whether the system can present different feedback today.
- A submitted Diagnostic is immutable and closed in the current graph. Recovery resumes assessment from the saved submission; it does not reopen or replace the learner's answer. This is both product semantics and an exactly-once durability boundary.
- The reliability spec's older clause that persistent malformed Assessment becomes Inconclusive is superseded for post-submission responsibilities by ADR-0076/the newer rationale spec: persistent contract invalidity reaches Unavailable. Any optimization must preserve this semantic-vs-technical-failure separation.
- The existing remediation loop is deliberately stronger than “one failure then any learning”: a Fresh Independent Test after remediation remains illegal until a conclusive Apply Practice pass, regardless of Explain or Teach-back outcomes.
- Existing ADR ownership is already sufficient for most likely changes: ADR-0042 owns the fresh post-Diagnostic Independent boundary; ADR-0043 owns neutral transition; ADR-0057 owns Diagnostic answer/rationale channel combination; ADR-0075 owns Conclusive vs Unconfirmed semantics and the common Guard route. A new ADR is unlikely unless discovery introduces a genuinely new multi-probe/adaptive-diagnostic architecture.
- ADR-0057 still contains the superseded single-rationale-pass wording despite ADR-0075 replacing it. If documentation is updated, the cleanest approach is to amend the existing ADR (or mark its affected clause superseded), not add another ADR for the same decision.
- ADR-0043's “failing Diagnostic ends the Apply-only reference path” is historical and no longer describes the shipped Learning/Practice flow. It is another existing ADR that may need a narrow amendment if the new Spec depends on its routing language.
- ADR-0009 fixes the formal-attempt boundary: one submission closes one Diagnostic Attempt; another probe must be a new Fresh Equivalent Task/Attempt, not an edit or retroactive conversion of the failed one.
- ADR-0004 explicitly avoids a fixed teaching-action pipeline. Therefore making Explain mandatory for every Not Passed outcome would conflict with the accepted adaptive-action principle unless justified as a Diagnostic-specific Guard constraint.
- The rationale-corroboration ticket set is still marked `ready-for-agent`/`needs-decision` although the corresponding implementation and ADR-0078 are already committed. This is documentation staleness, not an open product decision for this discovery.
- The older completed Apply ticket says a single applicable rationale passes and “Diagnostic 未通过时安全结束”; both statements are superseded by the newer corroboration and Learning/Practice baselines.
- Code confirms the current Diagnostic is structurally single-probe: one frozen Diagnostic context starts one Attempt, and every semantic assessment outcome immediately becomes either Independent delivery or Diagnostic Not Passed. There is no diagnostic probe index, stopping rule, accumulated diagnostic observation, or “ask another Diagnostic” Guard action.
- `DiagnosticFlow` preserves the no-Evidence boundary and returns neutral `FeedbackFacts` for Inconclusive/Unconfirmed, but returns criterion/error dimensions for Failed. The public result types distinguish those outcomes before the graph maps them.
- `WorkflowGuard` has only one `DIAGNOSTIC_NOT_PASSED` decision context and always offers `{Explain, Apply Practice}` with Explain fallback. Consequently Unconfirmed cannot currently request another diagnostic probe even though its feedback payload is neutral.
- The Guard documentation says Explain is legal only after a conclusive Diagnostic failure, while its code also permits Explain for Unconfirmed because both collapse to the same decision context. This mismatch highlights a latent product question, not just a code-comment defect.
- `DiagnosticFlow` owns immediate Fresh Independent generation after a pass. A multi-probe design would therefore change an established orchestration/state boundary: an intermediate Diagnostic result must commit a fresh Diagnostic task rather than enter the Independent or remediation path.
- Current assessment semantics are exact in code: proven correct short-circuits without a model; proven incorrect + blank rationale is Failed; proven incorrect + first Not Applicable is Failed; proven incorrect + first Inconclusive or failed corroboration is Unconfirmed; `Cannot Decide` requires both primary evaluators to say equivalent or becomes Unconfirmed.
- `DiagnosticSubmissionResult` is a closed union with `Passed`, `Failed`, and `Unconfirmed`; it has no intermediate/continue-diagnosis outcome. Adding another probe is therefore a product-visible state-machine extension, not a tuning flag.
- Both Failed and Unconfirmed currently call the Pedagogy Planner when the common Guard exposes two moves. Unconfirmed's neutral facts prevent targeted deficit claims, but the deterministic fallback message/action is still Explain, meaning uncertainty can cause potentially unnecessary teaching if planning fails.
- The graph's persisted state has no diagnostic-count/stopping-rule fact. Novelty/exposure infrastructure already tracks prior task and solution fingerprints, so fresh additional probes could reuse existing freshness concepts; the missing pieces are product policy and durable orchestration state, not a new task-generation mechanism.
- Whole-flow tests make the current UX concrete: both conclusive missing-rationale failure and Unconfirmed semantic uncertainty normally land on an Explain teaching interaction because Explain is the Guard fallback. The learner does not receive an explicit “we could not confirm this response” choice or a new probe.
- The committed learner interaction model can already represent another Diagnostic task without a new public union member: `kind=task`, `stage=DIAGNOSTIC`, and `attemptPurpose=DIAGNOSTIC`. A multi-probe design would still require new transition semantics and state, but not necessarily a new HTTP interaction shape or learner command.
- Existing exactly-once tests bind each Diagnostic submission result to its Idempotency-Key and interaction version. Any new probe must commit the closed prior Attempt, new verified Task Package/Attempt, exposure, state/checkpoint, and processed command atomically, and replay must return that same probe.
- Recent history shows the corroboration work landed as a sequence of destructive incremental commits and is current on `main`; the relevant implementation is not hypothetical. The stale ticket/older-spec wording should be treated as documentation debt around a shipped baseline.
- External measurement research does not support using “one item” as a generally reliable mastery/non-mastery classifier. Computerized classification tests normally continue until a classification rule is satisfied or a maximum item count is reached; early estimates are explicitly rough and improve with more targeted items.
- Mature ALEKS uses a variable-length adaptive assessment: each question depends on prior answers, it stops when remaining knowledge-state uncertainty falls below a threshold, and it probes inconsistent response areas further. Its 15–35 question scope is for broad knowledge-state mapping, so the number is not transferable to Kiln-AI's one-Concept flow, but the pattern “probe inconsistency/uncertainty, stop on confidence or cap” is directly relevant.
- Cognitive-diagnostic CAT research frames false mastery and false non-mastery as having different product costs. For a remedial program, false mastery may deny needed help, while false non-mastery imposes unnecessary work. Item selection/stopping should minimize the expected cost appropriate to the product rather than treat both mistakes symmetrically.
- For Kiln-AI, the fresh Independent Test already contains the risk of a false-positive Diagnostic: a lucky/easy Diagnostic pass still cannot establish Independent. The more exposed risk is false-negative/Unconfirmed routing, which can impose needless Explain/Practice. This asymmetry supports adding confirmation only to non-passing/uncertain cases rather than making every learner complete a longer fixed Diagnostic.
- A full psychometric CAT/CCT would require calibrated item parameters, a large item pool, classification thresholds, and validated scoring models; ETS describes it as highly complex and costly. The current generated-task/fingerprint system lacks that calibration. Calling a simple bounded second probe “adaptive testing” would overclaim; it should be specified as a product routing confirmation rule unless a future validated measurement architecture is chosen.
- Khan Academy's current Mastery Challenge assigns two questions per skill: two correct raises the level, two incorrect lowers it, and a split result leaves the level unchanged. This is not a Diagnostic flow, but it is strong mature-product precedent against changing a skill-level claim from one item and for representing disagreement as unresolved rather than failure.
- Duolingo's placement-test account documents the false-negative UX directly: occasional binary-graded errors placed a knowledgeable learner too low and made later lessons unnecessarily easy. Its response was to use error severity/partial correctness and simulation, not simply equate every miss with the same learning need.
- Kiln-AI already captures some response severity through Trusted Primary-Answer Check, Applicable Rationale, and sanitized error dimensions. However, a proven-wrong answer with omitted optional rationale is currently “Conclusive” after one item; this can still be a transcription/execution slip. The optional rationale therefore has a surprisingly large routing effect even though the UI labels it optional.
- Two defensible lightweight patterns now emerge without adopting CAT infrastructure: (A) add a Fresh Diagnostic Confirmation only for Unconfirmed outcomes; or (B) add one fresh confirmation after every first-item Diagnostic Not Passed, using the second item to protect against slips and over-remediation. Pattern B addresses the user's one-item concern more completely; Pattern A is shorter but leaves false-negative conclusive misses untouched.
- If a second probe is introduced, a mixed result cannot honestly become a Conclusive Diagnostic Gap. The consequential policy question is whether “one pass among two” is enough to proceed to the already-protective Fresh Independent Test, or whether mixed performance routes neutrally to Learning/Practice. That choice expresses the relative cost of unnecessary remediation versus an extra Independent attempt.
- The learner UI literally labels the rationale `理由（可选）` and gives no explanation that a rationale can rescue a wrong primary answer. This avoids turning it into a hidden required field for a correct answer, but makes the routing consequence of omission non-obvious after a slip.
- The current Diagnostic Blueprint has exactly one Rubric criterion and one direct symbolic-expression item shape. This is not a multi-attribute diagnosis; it is a narrow readiness gateway for one Concept. A lightweight two-probe confirmation can therefore remain much simpler than ALEKS-style knowledge-state assessment.
- The current UI renders task/stage/purpose from server state and can display sequential Diagnostic tasks without a new control concept. A product decision is still needed on whether learners should be told “one more diagnostic question” and whether the maximum length is disclosed upfront.
- ADR-0001 is the decisive existing architecture boundary: one Flow owns exactly one Target Concept; Supporting Concepts may inform it but receive no state or Evidence unless they become the Target of a separate Flow. The user's prerequisite-diagnostic goal can either stay within this boundary as current-Flow planning context or intentionally add cross-Flow sequencing.
- `Suspended Learning Flow` already exists in the glossary and ADR-0015 allows suspension on Target Concept switching, but the current runtime slice does not implement prerequisite-driven suspension/resumption. Choosing a separate Supporting Concept Flow would therefore be a real new capability.
- ADR-0022 explicitly defers cross-Flow learner memory. Diagnostic findings used only inside the current Target Flow fit the existing Blackboard boundary; carrying prerequisite findings across Concept Flows requires an explicit amendment to that accepted boundary.
- ADR-0033 already states that the manually prepared fixture is a tracer bullet and not a product input decision. The user's desired Agent-authored future is consistent with the accepted ingestion boundary.
- Current specs exclude learner uploads and automated textbook decomposition from the shipped slice. Their absence is implementation scope, not evidence that humans should permanently author per-book artifacts.
- A future learner-upload path triggers the source ownership, access-control, retention, deletion, and safety boundary in ADR-0041. This Diagnostic discovery assumes a validated Normalized Source Document and does not design upload permissions or copyright handling.

## Product Decisions
| Decision | Rationale |
|----------|-----------|
| Diagnostic is a pre-learning assessment phase, not merely a one-item gate to Independent Test | User clarified it should probe Target Concept readiness and relevant prerequisite knowledge, then inform adaptation during Learning and Practice |
| Non-blocking Supporting Concept findings adapt the current Target Flow; a Required Supporting Concept recommendation uses a separate Flow only if the learner explicitly starts it | Preserves single-Target Evidence ownership while allowing Direct Learning Choice in the current Flow |
| A Concept Preparation Agent authors Concept decomposition, Mastery Rubrics, Supporting Concept relations, and Diagnostic Plans from supplied source material | User clarified the target product model; current hand-authored calculus artifacts exist only because this preparation capability is not implemented yet |
| Agent-authored internal preparation artifacts are accepted automatically after type-specific Gates; the learner confirms only the visible Concept Contract | User accepted the recommended publication/confirmation authority boundary; routine expert or learner approval of internal Rubrics and plans is not required |
| Runtime Diagnostic adapts only within the accepted Diagnostic Plan; a suspected plan-external gap returns to Concept Preparation for a new version | User accepted the boundary between source-grounded content authoring and runtime assessment |
| Diagnostic stops once it has minimum sufficient information for a safe next route; unprobed or unresolved areas remain unknown | User confirmed that only necessary prerequisites should be tested and learners must not be trapped in Diagnostic |
| Required Supporting Concepts govern the recommended route and direct post-Diagnostic Independent eligibility, but do not forbid Target learning | User superseded the earlier unconditional gate by accepting an explicit learner override into Target Learning and Practice |
| Every Required Supporting Concept requires positive Prerequisite Readiness only for the recommended/direct-Independent route | Unconfirmed remains neutral; override creates no readiness or Evidence, and normal post-Practice Independent eligibility remains available later |
| A Conclusive prerequisite gap stops further Diagnostic probing and recommends a separate learner-started Supporting Concept Flow | The current Flow does not teach or auto-open the prerequisite; the learner may instead choose Direct Learning or leave |
| Existing aligned Concept Progress is checked before any prerequisite probe; otherwise prerequisite screening stays minimal | User wants prior learning in the database to personalize and shorten Diagnostic rather than make prerequisite assessment the focus |
| Learner self-report routes but cannot positively establish a hard prerequisite | User accepted that “not known/unsure” may skip testing and recommend learning, while “known” receives one minimal representative probe |
| A book-external prerequisite needs approved source authority before it can block, be tested, or be taught | User accepted platform-approved or newly accepted source material as the boundary; model memory cannot silently fill the gap |
| Conclusive and Unconfirmed Findings no longer share one immediate remediation route | User accepted continued Plan-authorized probing for Unconfirmed; terminal Target uncertainty routes neutrally to learning, while terminal prerequisite uncertainty yields an overridable neutral recommendation |
| Diagnostic checks unresolved Required Supporting Concepts in dependency order and stops at the first sufficient recommendation | User accepted minimum-sufficient routing over a complete prerequisite profile; unprobed areas stay Unknown |
| Direct Learning Choice may end or skip Diagnostic at any learner interaction | User accepted learner control despite unknown or missing prerequisites; open Diagnostic Attempts abandon without Findings, while committed Findings remain immutable |
| Diagnostic transitions stay neutral until the route enters learning | User accepted no per-question feedback before another Diagnostic or Independent task; only the Learning route receives a sanitized strengths/gaps/unknowns summary |
| Diagnostic rationale is Blueprint-selected, never required, and learner-visible in effect | User accepted default-disabled prerequisite rationale and optional Target rationale only when it can distinguish understanding from execution error; existing two-evaluator corroboration remains |
| Agent-authored, Gate-validated Target Readiness Set defines minimum Target coverage for direct Independent eligibility | User accepted representative rather than exhaustive Target assessment; confirmed areas may compress teaching but create no Evidence or mastery claim |
| Diagnostic length uses a Plan-specific visible maximum under a platform hard ceiling | User accepted adaptive early stopping with transparent worst-case length; exact ceiling is deferred to fixture and UX calibration |

## Architecture / Documentation Decisions
| Decision | Rationale |
|----------|-----------|
| Amend existing ADRs where they already own the decision | Explicit user preference |
| Redefine `Diagnostic` as a bounded, multi-Attempt pre-learning assessment stage | The prior glossary definition as one brief Attempt conflicts with the clarified product intent; the update does not yet decide cross-Concept state ownership |
| Introduce `Diagnostic Finding` as Flow-scoped and non-evidentiary | The broader Diagnostic needs durable, usable observations without manufacturing Evidence or cross-Flow learner traits |
| Amend ADR-0001 and ADR-0022 rather than create a new ADR | Those ADRs already own single-Target Flow and cross-Flow memory boundaries; the accepted behavior is an explicit extension of those decisions |
| Name the authoring role `Concept Preparation Agent` | It extends the established Concept Preparation process and avoids confusing the role with the book's author or a runtime Teaching Node |
| Amend ADR-0016 rather than add an authoring ADR | ADR-0016 already owns Concept Preparation output and confirmation authority; its amendment records Agent authorship, Gate acceptance, and learner-visible confirmation |
| Add `Diagnostic Plan` to the glossary and extend ADR-0016 | The term captures the frozen runtime curriculum boundary; ADR-0016 already owns preparation versioning and confirmation |
| Add `Required Supporting Concept` and refine `Diagnostic` | The domain must distinguish hard prerequisites from merely relevant Supporting Concepts and define Diagnostic as minimum-sufficient rather than exhaustive |
| Add `Prerequisite Readiness` and amend ADR-0001 | The accepted readiness rule for recommendations and direct Independent routing is owned by the single-Target Flow ADR, not a new architecture record |
| Add `Prerequisite Learning Recommendation` and refine ADR-0001/0022 | The existing ADRs already own single-Target and cross-Flow boundaries; the recommendation is durable coordination, not a new automatic curriculum architecture |
| Refine `Diagnostic Plan`, `Prerequisite Readiness`, ADR-0001, and ADR-0016 for progress-first minimal screening | The existing preparation and single-Target ADRs already own the planning and readiness policy; no new architecture ADR is needed |
| Add `Prerequisite Readiness Check` and refine `Prerequisite Learning Recommendation`/ADR-0001 | The term separates low-cost screening and neutral self-report from mastery assessment and Conclusive Diagnostic Gap |
| Refine `Required Supporting Concept` and amend ADR-0016/0001 for book-external prerequisites | Existing preparation/source and single-Target ADRs own the decision; an unsupported prerequisite remains a Source Gap rather than a new curriculum path |
| Replace `Diagnostic Not Passed` with stage-level `Diagnostic Routing Decision`; amend ADR-0042/0043/0075 | Multi-Attempt Diagnostic requires accumulated Plan-owned routing instead of treating the last Attempt result as the stage result; the existing ADRs already own these boundaries |
| Refine Diagnostic Plan and ADR-0001/0016/0022 for ordered early stopping and return | Existing ADRs own preparation order, single-Target routing, and permitted cross-Flow progress reads; no new ADR is needed |
| Add `Target Readiness Set` and amend ADR-0001/0016/0042 | The existing preparation, single-Target, and post-Diagnostic Independent ADRs own this minimum-coverage boundary; no new ADR is needed |
| Refine `Diagnostic Plan`/`Diagnostic` and amend ADR-0016 for bounded learner-visible length | Concept Preparation already owns Plan termination and confirmation UX; no new ADR is needed |
| Add `Direct Learning Choice` and amend ADR-0001/0016/0022/0075 | Existing ADRs own learner-controlled routing, single-Target/cross-Flow boundaries, and terminal Unconfirmed semantics; no new ADR is needed |
| Add `Diagnostic Summary`, broaden `Neutral Transition`, and amend ADR-0001/0043 | The existing neutral-transition and single-Target ADRs own learner-visible feedback timing; no new ADR is needed |
| Refine `Task Blueprint` and amend ADR-0016/0057/0075 for optional rationale selection | Existing preparation, channel-separation, and corroboration ADRs own the decision; ADR-0057's superseded single-applicable wording is corrected instead of adding an ADR |

## Open High-impact Questions

None after the learner-override, feedback-timing, and rationale decisions.

## Non-blocking Implementation Questions
- Exact class/record names, database column/table shapes, and fixture organization.
- Whether the probe number is derived from exposed Diagnostic Attempts or stored as a dedicated field, provided exactly-once recovery remains provable.
- Exact learner-message wording after product semantics are settled.
- Exact command/interaction discriminators and UI control placement for Direct Learning Choice, Prerequisite Learning Recommendation, Diagnostic Summary, and progress display.
- Concrete source adapters, extraction pipeline, upload UI, and source-ownership controls remain outside this Diagnostic Spec.
- Exact Gate schemas and validation algorithms for each Concept Preparation artifact remain implementation planning after their product contracts are specified.
- Exact mechanism for returning a plan-external gap to Concept Preparation is deferred; it must produce a new version and cannot mutate a running Plan.
- Exact next-question selection heuristic is deferred; it must honor minimum-sufficient stopping and the Plan's declared termination limits.
- README, existing reference specs, and rationale-corroboration tickets still describe the shipped single-probe `Diagnostic Not Passed` baseline. The new Spec must explicitly supersede those clauses and later ticketing must remove obsolete paths rather than add compatibility behavior.

## Current Decision Recommendation
- Confirmed prerequisite performance inside the current Diagnostic establishes readiness without a redundant Independent Test, Evidence, milestone, or review schedule.
- A Conclusive or terminally Unconfirmed Required Supporting Concept result stops further Diagnostic probing and emits a recommendation. The learner may start that Concept separately, choose Direct Learning for the current Target, or leave; the current Flow never auto-starts prerequisite teaching.
- Existing aligned accepted Concept Progress is the first readiness source. Only an unknown prerequisite receives a minimal Plan-bounded screening check; broad prerequisite profiling is excluded.
- Self-report is a routing input, not proof: “not known/unsure” yields a neutral recommendation without a task, while “known” receives one minimal representative no-assistance probe.
- A prerequisite omitted by the uploaded book may be discovered, but its blocking relationship, probe, and later Flow require approved platform or newly accepted source material; no model-memory fallback is allowed.
- Conclusive and Unconfirmed are per-Attempt Findings, not one immediate `Diagnostic Not Passed` route. A stage-level Diagnostic Routing Decision aggregates all findings and termination facts; Unconfirmed continues probing within budget before a neutral terminal route.
- All existing Required Supporting Concept progress is checked first; remaining checks follow an acyclic dependency order and stop at the first sufficient recommendation. On learner-controlled return, aligned Independent/Durable satisfies readiness directly; otherwise only a brief recheck is required.
- The Agent-authored Target Readiness Set is the minimum positive Target coverage for Fresh Independent eligibility. Conclusive gaps focus Target teaching, confirmed areas may be compressed, and unprobed criteria remain Unknown.
- Every Plan declares a worst-case count of at most eight Attempts across its complete Diagnostic stage, including resume; learners see “completed / maximum,” may finish early, and no bound is extended or reset at runtime.
- Prerequisite Readiness controls the recommended route and direct post-Diagnostic Independent eligibility. Direct Learning Choice may enter Target Learning and Practice with skipped/unready areas Unknown or retained as risks; it creates no Evidence or readiness, but later normal Practice can still lead to Independent.
- Between Diagnostic tasks and before Fresh Independent, transitions show only progress and no feedback. Once Target Learning begins, a sanitized Diagnostic Summary may expose strengths, teaching priorities, and unknowns; prerequisite routes expose only the recommendation.
- Each Diagnostic Blueprint disables rationale or enables the always-optional corroborated policy when justified by its Rubric; prerequisite checks default to none, enabled fields explain their effect, and only two isolated Applicable judgments rescue a proven-wrong primary answer.
- Prior Independent/Durable Concept Progress bypasses prerequisite screening only when Supporting Concept identity and relevant Mastery Rubric, criterion, and source-basis versions align; any relevant change triggers a brief recheck.

## Superseded Discovery Branches
- The prior “one or at most two Target Concept questions” framing is superseded by the user's clarified goal. Research about bounded probing and misclassification remains useful, but the product is now a broader pre-learning assessment.
- The earlier mixed-two-probe decision tree is no longer the primary design tree.

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| Initial planning-file patch was structurally invalid | Retried as an in-place update and recorded the failure |
| A findings update assumed a resource line that had not yet been added | Re-read the planning file and patched against its actual contents |
| A combined documentation patch targeted `progress.md` twice | The patch was rejected atomically and split into one operation per file |
| A planning update used an inexact progress-line anchor | Re-read the exact file tail and retried with the current wording |

## Convergence Audit
- No high-impact product, UX, architecture-boundary, source-authority, privacy, compatibility, or state-transition question remains open.
- `git diff --check` passes.
- Exactly seven existing ADRs were amended; no new ADR was created.
- Current code and older specs remain the shipped single-probe baseline and are intentionally not implemented or rewritten during discovery; `/to-spec` should establish the new normative change set.
- Remaining questions are bounded implementation planning, fixture calibration, UI wording, or separately deferred upload security/permission work.

## Resources
- `AGENTS.md`
- `README.md`
- `CONTEXT.md`
- `docs/specs/diagnostic-rationale-corroboration-spec.md`
- `docs/specs/apply-profile-reference-spec.md` (older baseline; partially superseded)
- `docs/specs/learning-practice-reference-spec.md`
- `docs/specs/learning-flow-reliability-and-reference-ui-spec.md`
- `docs/adr/0004`, `0005`, `0009`, `0042`, `0043`, `0045`, `0057`, `0063`, `0069`, and `0071`
- `docs/adr/0075` through `0078`
- `docs/tickets/diagnostic-rationale-corroboration/*`
- [ETS — Practical Considerations in Computer-Based Testing](https://www.ets.org/Media/Research/pdf/CBT-2011.pdf)
- [ALEKS Teacher's Guide — adaptive assessment behavior](https://www.aleks.com/manual/pdf/educators.pdf)
- [Hsu & Wang (2022) — misclassification costs in cognitive-diagnostic CAT](https://pmc.ncbi.nlm.nih.gov/articles/PMC9073635/)
- [Khan Academy — Mastery Challenges use two questions per skill](https://support.khanacademy.org/hc/en-us/articles/360037494231-What-are-Mastery-Challenges)
- [Duolingo — partial-credit improvements to its adaptive placement test](https://blog.duolingo.com/partial-credit-improvements-to-duolingos-placement-test/)
- [Khan Academy — proficiency uses several items and external-validation framing](https://blog.khanacademy.org/why-khan-academy-will-be-using-skills-to-proficient-to-measure-learning-outcomes/)
