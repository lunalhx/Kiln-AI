# Task Plan: Ticket 06 procedural clarification boundary

## Goal
Diagnostic, Teach-back, and standalone Explain accept only procedural clarification: restating already displayed format/notation/interface conditions, with an auditable record. Substantive or uncertain requests add no teaching content and do not change Attempt purpose or evidence eligibility. Explain `clarification_asked` targets the current Interaction Boundary without `attemptId`. Practice / Independent / Review clarification and consent stay unchanged.

## Current Phase
Phase 3

## Phases

### Phase 1: Requirements & Discovery
- [x] Read AGENTS.md, CONTEXT.md, ticket 06, spec, ADR-0014/0013/0030/0065
- [x] Map current clarification path (graph rejects Diagnostic/Teach-back; HTTP always requires attemptId)
- **Status:** complete

### Phase 2: Planning & Structure
- [x] Confirm seams from spec Testing Decisions
- [x] Vertical slices: Diagnostic → Teach-back → Explain → HTTP
- **Status:** complete

### Phase 3: Implementation (TDD)
- [x] Slice 1: Diagnostic procedural restatement + assistance record
- [x] Slice 2: Diagnostic substantive/uncertain refusal, purpose and evidence unchanged
- [x] Slice 3: Teach-back procedural + substantive/uncertain
- [x] Slice 4: standalone Explain without attemptId (procedural + substantive)
- [x] Slice 5: HTTP command contract (optional attemptId on Explain; Independent consent unchanged)
- **Status:** complete

### Phase 4: Testing & Verification
- [x] Targeted graph + HTTP tests
- [x] `./mvnw clean test`
- **Status:** complete

### Phase 5: Code review, commit, delivery
- [x] Two-axis code review
- [ ] Commit on current branch
- **Status:** in_progress

## Key Questions
1. Where is the auditable record for Explain (no Attempt)? Committed teaching interaction + processed command; Assistance Trace for Diagnostic/Teach-back.
2. Substantive on Diagnostic: 409 ignore vs committed refusal? Commit a same-task/teaching boundary with a refusal message so replay is exactly-once and the learner sees no conceptual help.

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Primary seams are `LearningFlowGraphContractTest` and `LearningFlowHttpTest` | Spec Testing Decisions: extend the whole-flow graph contract; HTTP verifies discriminators and attemptId rules |
| Explain clarification uses nullable `attemptId` | Spec: Explain clarification and `retry_requested` target the current Interaction Boundary without an Attempt ID |
| Substantive/uncertain on Diagnostic/Teach-back/Explain commits a refusal on the same boundary | No teaching content, purpose unchanged, durable exactly-once; Assistance Trace records only exposed procedural restatement |
| Do not add a new audit table | Ticket 08 owns PostgreSQL invariants; Assistance Trace + committed interaction/processed command are existing durable records |
| Do not change Practice/Independent/Review paths | Ticket AC and out-of-scope for later tickets |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
|       | 1       |            |

## Notes
- Ticket 07 (Review cancel), 08 (Postgres), 09 (UI) are out of scope.
- Interaction contracts already advertise `clarification_asked` on Diagnostic, Teach-back, and Explain; the graph currently rejects Diagnostic/Teach-back.
