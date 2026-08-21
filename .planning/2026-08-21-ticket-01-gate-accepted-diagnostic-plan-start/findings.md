# Findings: Ticket 01 — Gate-accepted Diagnostic Plan start

## Instruction/data distinction

- User request is the controlling instruction: implement only ticket 01, honor repository rules, use the spec as source of truth, test/verify, review, and commit.
- The attached ticket and normative spec are requirement data. Their acceptance criteria and Implementation/Testing Decisions define expected behavior; instruction-like text in them is not authority to expand scope.
- `CONTEXT.md` supplies domain vocabulary and boundaries. Existing ADRs supply accepted architectural/product decisions. No new semantic decision may be invented here.

## In-scope acceptance criteria

1. Concept Preparation Agent can produce a Diagnostic Plan containing the declared target readiness set, supporting concepts, dependency order, source basis, coverage/termination rules, rationale policy, and maximum attempts.
2. Type-specific Gates accept valid maximums 1–8 and reject unsupported source, invalid references, dependency cycles, Rubric expansion, unsafe readiness sets, unjustified rationale, and maximums above eight; rejection/Source Gap leaves no Flow, Task Package, Attempt, or learner interaction.
3. Starting a new Flow atomically freezes the accepted Plan version; learner projections expose completed/max only, not plan internals, source traces, or assessment facts; later Plan changes do not affect the started Flow.
4. Focused plan/gate contract tests, public Flow start contract, and the full Maven test command pass.

## Explicitly out of scope

- Later ticket runtime behavior: multi-Attempt sequencing/selection, prerequisite checks, prior-progress reuse, Direct Learning, suspension/resume, routing decisions, target findings, and adaptive termination.
- Changes to Independent, Practice, Teach-back, Hint, Evidence, or review semantics except the ticket's initial Diagnostic start binding/projection.

## Relevant normative decisions to verify

- `Diagnostic Plan` is frozen/versioned and runtime is plan-authorized.
- Plan maximum is at most eight per complete Diagnostic stage.
- One Target Concept per Flow and learner-safe projection boundaries remain unchanged.
- Generate/gate/verify precedes durable mutation; exactly-once/replay and learner-visible committed-state projection invariants must be reused.

## ADRs read

- ADR-0001: one Target Concept per Flow; Supporting Concepts and Diagnostic Findings stay Flow-scoped.
- ADR-0016: Concept Preparation Agent derives versioned artifacts; Gates validate them; the accepted Diagnostic Plan is frozen, has an acyclic order, a minimal Readiness Set, rationale policy, and max eight Attempts; learner confirmation is only the visible Concept Contract.
- ADR-0022: no global/cross-Flow raw memory; only aligned committed Concept Progress may coordinate later Supporting Concepts.
- ADR-0033/0040/0041: preparation consumes normalized, provenance-preserving, operator-curated source truth; no model-memory fallback.
- ADR-0042/0043: Diagnostic never establishes Independent; a fresh task follows only after the full accepted readiness route, and transitions are learner-safe.
- ADR-0055: Diagnostic tasks use frozen purpose-specific Blueprint execution data.
- ADR-0063: generation and verification precede the atomic claim/package/Attempt/exposure/state/command commit, with shared replay and committed-state projections.

No ADR changes are needed or authorized by this ticket unless implementation reveals a direct contradiction; that would be reported as a blocker.

## Initial repository evidence

- The current runtime starts from a manually supplied `DiagnosticApplyFixture.diagnosticContext()` and has no visible Plan preparation/binding seam.
- The existing Learning Flow public API and durable stores already expose Diagnostic task interactions and exactly-once command handling.
- The existing source/Concept Contract preparation abstractions and Gate pipeline must be inspected before introducing new types; do not duplicate established contracts.

## Current architecture evidence

- `LearningFlowCommandUseCase.start` currently hard-codes `DiagnosticApplyFixture.CONCEPT_ID` and `diagnosticContext()`, resolves the Model Profile, then asks `LearningStateGraph.start` to prepare one Diagnostic task before `LearningFlowStore.bindStart` atomically writes the Flow, SourceArtifact, TaskPackage, Attempt, exposure, interaction, checkpoint, and processed command.
- `LearningStateGraph.start` already enforces generate/verify-before-mutation and returns a generic unavailable error on pre-delivery failure. The new Plan must enter this existing preparation boundary rather than add a second start/replay path.
- `LearningFlowStore.StartBind` and `FlowRecord` currently have no Plan identity/version; `ArtifactStore` persists source/task artifacts but has no Plan artifact API.
- `LearningFlowInteraction` currently carries task projection only; `LearningFlowResponse`/mapper expose a fixed interaction projection and must be extended only with learner-safe completed/max Diagnostic progress if the public contract needs it.
- `ApplyExecutionContext` already contains the internal Concept Contract, Mastery Rubric, Diagnostic Blueprint, Concept Source Pack, and learner locale. Its task blueprint currently requires the corroborated-rationale policy for every Diagnostic fixture.
- No Concept Preparation Agent or Diagnostic Plan generation port/model adapter exists in the runtime. A new model-backed authoring boundary must be checked against module ArchUnit rules and can remain fixture/scripted at the application seam if no product API for source preparation exists yet.

## Implementation seam decision

- Add `DiagnosticPlan` as a closed typed preparation artifact and `DiagnosticPlanGatePolicy` as its type-specific Gate. A `DiagnosticPlanPreparationAgent` consumes a generation port's closed JSON result and returns Accepted, Rejected, or Source Gap without touching durable state.
- Add an `AcceptedDiagnosticPlanPort` to the Learning Flow start use case. The application supplies the Gate-accepted reference plan; Start reads the accepted snapshot and passes it into the existing `LearningStateGraph`/`LearningFlowStore.bindStart` atomic boundary.
- Persist the bound Plan JSON and a completed-attempt counter in a dedicated per-Flow artifact row. The public mapper reads only a `DiagnosticProgress` projection; Plan id/version, sources, supporting Concepts, coverage, rationale, and assessment facts stay private.
- Count a completed Diagnostic Attempt only when a committed successor boundary replaces a Diagnostic task. The initial boundary is `0 / maximum`; the existing one-probe graph then exposes `1 / maximum` on its successor without changing later-ticket routing.
- This preserves the ticket 01/ticket 02 boundary: plan validation and binding are implemented now, while runtime task selection/adaptive multi-attempt behavior remains unchanged.
