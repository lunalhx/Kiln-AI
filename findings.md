# Findings: Ticket 10 — destructive cutover and release verification

## Scope
- Ticket 10 is a release/cutover verification ticket, not a new learner-flow feature. Its explicit scope is: transport-only model adapters, deletion of obsolete direct-flow write paths/compatibility mappings, 404 for old Apply HTTP endpoints, ArchUnit and contract-suite verification, non-blocking live smoke with ephemeral state and no Evidence, and documentation/state alignment for the destructive fresh-database release.
- It is blocked by ticket 05 (strict model-contract recovery) and ticket 09 (reference UI lifecycle). Those are prerequisites, not permission to reimplement their features here.

## Source-of-truth documents
- Ticket: `docs/tickets/learning-flow-reliability-and-reference-ui/10-destructive-cutover-and-release-verification.md`
- Normative spec: `docs/specs/learning-flow-reliability-and-reference-ui-spec.md`.
- Confirmed spec decisions: the public API is the destructive unified Learning Flow API; obsolete Apply endpoints and compatibility aliases are not preserved; adapters own transport while Domain parses closed model contracts; live smoke uses operator-resolved Model Profile and creates no Evidence; no historical Flow/Review migration is added.
- Related ADR baseline read/being applied: ADR-0035 (operator-owned Model Profile resolution), ADR-0037 (operator/provider catalog), ADR-0059 (scripted contract vs non-blocking live smoke), ADR-0063 (exactly-once persistence), ADR-0069 (durable unavailable retry), ADR-0070 (Active Learning Work/review cancellation), ADR-0071 (fail-closed model contracts), plus ADR-0064/0072 where the current graph/application boundary is involved.

## Current repository state
- Existing worktree changes are present and must be preserved unless they overlap ticket 10.
- An active planning pointer exists for an older audit plan; this task uses these root planning files explicitly.

## Initial evidence
- README defines `./mvnw clean test` as the verification command and identifies scripted profile contracts as the stable oracle; live smoke is separate and non-blocking.
- The ticket acceptance checklist is currently unchecked, so implementation status must be established from code/tests rather than assumed.

## Current code gaps to verify
- The unified `/api/learning/flows` and `/api/review-tasks` controllers are the only HTTP controllers; `LearningFlowHttpTest` already asserts the historical `/api/apply/**` routes return 404.
- The scheduler is a deterministic due-transition use case and has no model dependency in its constructor.
- `ApplyModelAdapter` currently has contract-specific system prompts, JSON serialization, and `extractJson`, which strips fences/extracts an object before returning model content. That violates the transport-only requirement even though the Domain parses the returned strings later.
- `OperatorModelConfiguration` currently maps typed ports through adapter convenience methods and parses typed contracts in the application configuration. The refactor must keep strict parsing in Domain and route every real port through the operator/frozen `ModelProfile` without introducing compatibility paths.
- `ApplyProfileLiveSmokeTest` is gated by `KILN_LIVE_SMOKE=true`, uses in-memory stores, resolves `OperatorCatalog` from dotenv, and asserts no Evidence, but currently exercises only Diagnostic Start; ticket 10 explicitly requires at least one real generation and one real submission. This is a likely implementation gap.
- README contains stale wording that Explain, Hint, and Teach-back are out of scope even though they are implemented. Several superseded/older ticket files still carry unchecked acceptance boxes; their status alignment needs to be handled narrowly and explicitly.
- Pre-existing worktree changes are ticket-09-style UI changes in three files plus older untracked planning directories. They must not be overwritten or included in the ticket-10 commit accidentally.
