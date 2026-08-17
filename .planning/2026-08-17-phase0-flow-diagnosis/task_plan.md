# Phase 0 Flow Diagnosis

## Goal

Determine why live model calls are blocked by fail-closed behavior, identify reproducible HTTP 503 paths, and explain which Learning Flow stages are implemented and exposed by the reference UI.

## Phases

1. [complete] Align the normative Phase 0 flow with the repository architecture and current worktree.
2. [complete] Build tight feedback loops for fail-closed model selection and HTTP 503 behavior.
3. [complete] Trace the backend Learning StateGraph and map every learner-visible interaction.
4. [complete] Inspect and exercise the index UI against the implemented API.
5. [complete] Report confirmed issues, root causes, implementation gaps, and recommended repair order.

## Constraints

- Preserve all pre-existing uncommitted changes.
- Diagnose current worktree behavior separately from the committed baseline where relevant.
- Do not change product code unless the user asks for fixes after diagnosis.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
