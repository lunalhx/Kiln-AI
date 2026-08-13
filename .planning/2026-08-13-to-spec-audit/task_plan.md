# Task Plan: Spec readiness audit

## Goal

Determine whether the already-confirmed Kiln-AI Phase 0 decisions are sufficient to produce and publish an implementation-ready spec without inventing product decisions.

## Current Phase

Complete

## Phases

### Phase 1: Repository and tracker audit
- [x] Inspect current code, build, tests, documentation, and issue-tracker configuration
- [x] Identify existing or proposed highest-level test seams
- **Status:** complete

### Phase 2: Decision-completeness audit
- [x] Compare the accepted ADR baseline and tracer-bullet plan against implementation needs
- [x] Separate explicitly deferred variables from implementation-blocking decisions
- [x] Record only material unresolved items
- **Status:** complete

### Phase 3: Spec or blocker handoff
- [x] Synthesize the Spring AI Alibaba Graph spike spec using only accepted decisions
- [x] Use the learner-facing HTTP/UI flow as the primary acceptance seam
- [x] Publish to `https://github.com/lunalhx/Kiln-AI.git` with `ready-for-agent`
- [x] If blockers remain, report them before drafting or publishing and make no guesses
- **Status:** complete

## Decisions Made

| Decision | Rationale |
|---|---|
| Do not draft a completeness-shaped spec before the audit | The user explicitly prohibited adding product decisions and requested material unresolved items first. |
| Stop before drafting or publishing | The spec scope, implementation sequence/runtime dependency, canonical acceptance seam, and tracker target are not all decided. |
| Scope the spec to the Spring AI Alibaba Graph validation spike | Explicit user confirmation. |
| Complete the spike before fixing the tracer runtime | Explicit user confirmation. |
| Use the learner-facing HTTP/UI complete flow as the highest acceptance seam | Explicit user confirmation. |
| Publish to GitHub repository `lunalhx/Kiln-AI` | Explicit user confirmation. |

## Errors Encountered

| Error | Attempt | Resolution |
|---|---:|---|
| Root-level `find src ...` failed because there is no root `src` | 1 | Inspected module source paths with `rg --files` and focused reads. |
| A broad parallel read was truncated | 1 | Repeated the audit as smaller focused reads. |
| GitHub CLI token was invalid and the sandboxed API query failed | 1 | Used the authenticated Chrome session after confirming no GitHub connector was available. |
| Initial Issue submit locator matched two Create buttons | 1 | Inspected locator diagnostics and submitted through the unique `create-issue-button`; verified Issue #1. |
