# Task Plan: Ticket 01 — Gate-accepted Diagnostic Plan start

## Goal

Implement only ticket 01 from `docs/tickets/diagnostic-flow-optimization/01-gate-accepted-diagnostic-plan-start.md`: introduce an accepted, frozen, versioned Diagnostic Plan from approved normalized sources; bind it atomically to a newly started Target Concept Learning Flow; expose only the plan maximum and completed count to the learner; and verify rejection/source-gap atomicity. Do not implement later tickets' runtime multi-attempt routing, prerequisite probing, Direct Learning, resume, or adaptive stopping behavior.

## Scope boundary

- Normative source: `docs/specs/diagnostic-flow-optimization-spec.md` plus the ticket acceptance criteria.
- Existing ADR decisions remain authoritative; no product-semantic change or new ADR is allowed in this ticket.
- Tests must exercise public plan preparation/gate and Learning Flow start seams, not private implementation details.

## Phases

### Phase 1: Requirements and architecture discovery
- [x] Read the required repository docs, ticket/spec, and relevant ADRs completely.
- [x] Map existing source, Concept Contract, task preparation, flow-start, persistence, and public projection seams.
- [x] Identify the smallest ticket-owned files and record any blocker or scope conflict.
- **Status:** completed; no blocker. Current task generation remains fixture-backed by design for ticket 01; Plan-driven selection is ticket 02.

### Phase 2: TDD seams and red tests
- [x] Define the public seams for accepted/rejected Plan preparation and Flow start.
- [x] Add one failing test per in-scope acceptance slice before implementation.
- **Status:** focused Gate, preparation, atomic start/source-gap, public response, and UI projection tests are green.

### Phase 3: Minimal implementation
- [x] Implement the smallest end-to-end accepted Plan path with frozen/versioned binding.
- [x] Implement type-specific Gate rejection and Source Gap with no durable artifacts.
- [x] Project only completed/max attempts; keep plan internals/private traces out of learner responses and UI.

### Phase 4: Verification
- [x] Run focused tests after each vertical slice.
- [x] Run typecheck/lint commands if the Maven project defines them.
- [x] Run the repository-required `./mvnw clean test` with PostgreSQL-backed tests where available.
- [x] Check acceptance criteria and confirm no later-ticket behavior was added.

### Phase 5: Review and commit
- [x] Run the required two-axis code review against the pre-task fixed point.
- [x] Address only actionable findings within ticket scope.
- [x] Commit the implementation on the current branch.
- **Status:** completed against `aa7c310`; review follow-ups were limited to production Agent preparation wiring, frozen-version coverage, and provider-failure handling.

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| Focused `-pl kiln-ai-domain` compilation could not resolve existing types from `kiln-ai-types` | 1 | Re-run the focused test with `-am` so Maven builds reactor dependencies first |
| Focused `-am` test discovery included stale compiled `domain.preparation` classes from `target/test-classes` | 2 | Run the focused test after Maven `clean` removes build artifacts |
