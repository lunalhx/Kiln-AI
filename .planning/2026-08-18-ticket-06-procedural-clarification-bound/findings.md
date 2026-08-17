# Findings: Ticket 06 procedural clarification boundary

## Requirements
- Diagnostic / Teach-back / standalone Explain: procedural clarification restates already displayed conditions and leaves an auditable record.
- Substantive or uncertain requests: no teaching content, no Attempt purpose change, no evidence-eligibility change.
- standalone Explain `clarification_asked` targets the current Interaction Boundary and does not require `attemptId`.
- Practice / Independent / Review clarification and consent stay unchanged.

## Spec source of truth
- `docs/specs/learning-flow-reliability-and-reference-ui-spec.md` stories 11–12, Implementation Decisions (attemptId vs Explain), Out of Scope (no substantive clarification for these three).
- `docs/specs/learning-practice-reference-spec.md` Explain/Teach-back/Diagnostic clarification paragraphs.
- ADR-0014 (Independent/Review consent — do not change), ADR-0030 (Explain events; Diagnostic/Teach-back reject substantive), ADR-0065 (Teach-back no Hint).

## Current behavior
- `LearningStateGraph.clarificationAsked` requires `attemptId` and returns `ClarificationIgnored(WRONG_ATTEMPT_PURPOSE)` for Diagnostic and Teach-back before classifying.
- Practice: procedural restates Task Package via `ClarificationGate.proceduralAnswer`; substantive delivers temporary Explain.
- Independent/Review: substantive/uncertain → assistance-consent; procedural → restatement without disqualification.
- HTTP `LearningFlowController` treats `clarification_asked` as an attempt command (`attemptId` required).
- Diagnostic and Explain interaction contracts already list `clarification_asked`.
- Graph contract test `clarificationAndAssistanceCommandsAreIgnoredForWrongOrClosedAttempts` asserts Diagnostic never takes clarification.

## Seams
1. `LearningFlowGraphContractTest` — public Learning Flow command + committed interaction + durable state.
2. `LearningFlowHttpTest` — discriminator, status codes, Explain without attemptId, Independent consent unchanged.

## Resources
- Ticket: `docs/tickets/learning-flow-reliability-and-reference-ui/06-procedural-clarification-boundary.md`
- Specs: `docs/specs/learning-flow-reliability-and-reference-ui-spec.md`, `docs/specs/learning-practice-reference-spec.md`
- ADRs: 0013, 0014, 0030, 0065
- Code: `LearningStateGraph.clarificationAsked`, `ClarificationGate`, `LearningFlowCommandUseCase`, `LearningFlowController`
