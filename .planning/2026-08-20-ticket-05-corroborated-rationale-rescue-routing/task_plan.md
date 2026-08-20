# Task Plan: Ticket 5 — corroborated rationale rescue routing

## Goal
Implement only ticket 5 on top of the committed ticket 1–4 baseline: a proven-incorrect Diagnostic rationale reaches a Fresh Independent Test only after two isolated Applicable judgments, while uncertainty and technical failure remain learner-safe and exactly-once.

## Current Phase
Phase 4 — Review and delivery

## Phases

### Phase 1: Requirements and discovery
- [x] Read AGENTS.md, README.md, CONTEXT.md, ticket 5, its normative spec, and related ADRs
- [x] Confirm ticket 1–4 are the implementation baseline and worktree is initially clean
- [x] Identify public and durable seams affected by ticket 5
- **Status:** complete

### Phase 2: TDD plan and implementation
- [x] Add failing tests at the existing public/profile/recovery seams for each ticket 5 branch
- [x] Implement the smallest vertical slices and keep unrelated flows unchanged
- **Status:** complete

### Phase 3: Verification
- [x] Run focused tests, typecheck/lint if defined, and the full `./mvnw clean test`
- [x] Verify acceptance criteria and no later-ticket behavior was added
- **Status:** complete

### Phase 4: Review and delivery
- [x] Run the required two-axis code review
- [x] Address actionable findings
- [x] Commit the ticket 5 implementation on the current branch
- **Status:** complete

## Decisions and boundaries
- Spec is the source of truth; ticket 5 is limited to corroboration after first `applicable`, neutral/diagnostic-not-passed routing, replay/Retry/PostgreSQL recovery coverage, and regressions.
- No new learner command, answer field, public response field, compatibility alias, or later-ticket feature.
- Evaluation Profiles remain isolated, subject-neutral, and use the current Flow-frozen Strong Binding.

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| First red run cannot compile the new test because the counterexample profile/stack and second responsibility do not exist yet; the standalone `-pl kiln-ai-domain` run also lacked the types-module test dependency | 1 | Implement the missing slice, then rerun with `-am` and `surefire.failIfNoSpecifiedTests=false` |
