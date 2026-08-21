# Task Plan: Diagnostic Flow optimization discovery

## Goal
Reach a shared, evidence-backed product and design definition for an improved Diagnostic Flow, update the domain glossary and existing ADRs only when decisions crystallize, and stop before implementation.

## Current Phase
Complete — Spec published and verified

## Phases

### Phase 1: Repository and research discovery
- [x] Read AGENTS.md, README.md, CONTEXT.md, current specs/tickets/ADRs, implementation, and tests
- [x] Reconstruct the current durable state machine and identify contradictions or hidden decisions
- [x] Research established diagnostic-assessment patterns using primary/authoritative sources
- **Status:** complete

### Phase 2: Decision-tree grilling
- [x] Ask one genuinely human product/UX/architecture decision at a time, with a recommendation
- [x] Stress-test decisions with concrete edge cases and update findings immediately
- [x] Update CONTEXT.md inline for resolved domain-language changes
- **Status:** complete

### Phase 3: Decision documentation
- [x] Amend the narrowest existing ADRs for resolved architectural decisions
- [x] Create a new ADR only if the decision is hard to reverse, surprising, and a real trade-off
- [x] Keep implementation details for later planning
- **Status:** complete; no new ADR was required

### Phase 4: Convergence check and handoff
- [x] Confirm no high-impact Spec decisions remain unresolved
- [x] Summarize resolved decisions and non-blocking questions
- [x] State whether `/to-spec` is recommended and wait for user confirmation
- **Status:** complete; user confirmed `/to-spec`

### Phase 5: Spec synthesis and publication
- [x] Resolve the two implementation-affecting deferred policies through the user's delegated recommendations
- [x] Synthesize only accepted decisions into the repository Spec template
- [x] Configure the selected GitHub Issues tracker and repository triage/domain metadata
- [x] Publish the Spec with `ready-for-agent` and verify the durable issue
- **Status:** complete; GitHub Issue #7

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| This session is discovery only; no product-code implementation | Explicit user boundary |
| Prefer amendments to existing ADRs | Explicit user preference; new ADR threshold remains the domain-modeling three-part test |
| Diagnostic is a bounded pre-learning assessment stage, not one fixed Attempt | User clarified that it should probe Target Concept readiness and relevant prerequisite knowledge to adapt later learning |
| Keep one Target Concept per Flow; recommend a learner-started separate Flow for a missing Required Supporting Concept | Preserves evidence, mastery, and review ownership without auto-starting cross-Concept curriculum; the learner may instead choose Target learning directly |
| The target product's preparation artifacts are authored by a Concept Preparation Agent | The user expects agentic source decomposition and rubric/prerequisite/diagnostic design; current manual fixtures only stand in for the unimplemented capability |
| Agent-authored internal preparation artifacts publish after type-specific Gates; learners confirm only the visible Concept Contract | Preserves agentic content production and learner control without per-book human rubric authoring |
| Runtime Diagnostic adapts only within a frozen versioned Diagnostic Plan | Prevents a runtime model from silently rewriting Concepts or Rubrics; plan-external gaps return to Concept Preparation for a new version |
| Diagnostic seeks minimum sufficient routing information and may stop early | Avoids trapping learners in exhaustive pre-testing; unprobed or unresolved areas remain unknown |
| Required Supporting Concepts control the recommended path and direct post-Diagnostic Independent eligibility, not access to Target learning | User explicitly retained the right to skip Diagnostic or override a prerequisite recommendation and start Target Learning and Practice |
| Every Required Supporting Concept needs positive Prerequisite Readiness only for the recommended/direct-Independent route | Persistent Unconfirmed remains neutral, absence of a known failure is insufficient, and a learner override manufactures no readiness or Evidence |
| Direct Learning Choice lets the learner skip or override Diagnostic into Target Learning and Practice | Preserves learner control without creating readiness, Evidence, or direct Independent eligibility |
| Conclusive and Unconfirmed produce distinct Findings and continuation behavior | Unconfirmed consumes bounded fresh probes before a neutral terminal route; technical Unavailable remains separate |
| Agent-authored Target Readiness Set controls direct Independent eligibility | Keeps Diagnostic representative rather than exhaustive and lets confirmed strengths adapt later teaching |
| Platform Diagnostic hard ceiling is eight Attempts per frozen Plan version, including resume | Makes the learner promise, Gate, and terminal tests executable without runtime extension |
| Prior Concept Progress is reusable only under full relevant version alignment | Supporting Concept, Mastery Rubric/criterion, and source-basis changes trigger a brief recheck instead of stale readiness reuse |

## Errors Encountered
| Error | Resolution |
|-------|------------|
| Initial multi-operation patch tried to delete and add the same planning files in one patch | Switched to in-place update patches |
| Combined glossary/planning patch failed on an overly broad findings anchor | Re-read exact context and split the update into narrower hunks |
| Agent-authoring documentation patch used an overly broad findings anchor | Re-read the exact section and split the update |
| One diagnostic command contained an unsafe backtick in a double-quoted shell string | Re-ran the read with a single-quoted search pattern and no command substitution |
| ADR inspection reused stale descriptive filenames for ADR-0042/0043/0075 | Located the actual numbered files with `rg --files`; current ADR-0075 content does own the Conclusive/Unconfirmed route and was re-read before amendment |
| A combined documentation patch targeted `progress.md` in two separate update operations | The patch was rejected atomically; split documentation and planning-record updates, with one operation per file |
| A planning-record patch used an inexact progress-line anchor | Re-read the exact tail and reapplied against the current wording |
