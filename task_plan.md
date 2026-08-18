# Task Plan: Ticket 10 — destructive cutover and release verification

## Goal
Implement only the behavior required by `docs/tickets/learning-flow-reliability-and-reference-ui/10-destructive-cutover-and-release-verification.md`, using the referenced spec as the source of truth, and verify the release/cutover acceptance criteria.

## Current Phase
Phase 5 — Review and handoff

## Phases

### Phase 1: Requirements & discovery
- [x] Read AGENTS.md, README.md, CONTEXT.md, ticket 10, referenced spec, and related ADRs
- [x] Inspect current implementation, tests, scripts, and repository state
- **Status:** completed

### Phase 2: Plan and test seams
- [x] Identify the smallest public seams and ticket-owned files
- [x] Record any blocker if the ticket requires changing established product semantics
- **Status:** completed

### Phase 3: TDD implementation
- [x] Red → green vertical slices for each in-scope acceptance criterion
- **Status:** completed

### Phase 4: Verification
- [x] Run targeted tests, typecheck/lint if defined, and `./mvnw clean test`
- [x] Verify acceptance criteria and no later-ticket scope was implemented
- **Status:** completed

### Phase 5: Review and commit
- [x] Run two-axis code review
- [x] Address actionable findings
- [ ] Commit the implementation on the current branch
- **Status:** in_progress

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
