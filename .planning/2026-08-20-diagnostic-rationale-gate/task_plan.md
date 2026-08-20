# Task Plan: Diagnostic rationale rescue gate

## Goal
Agree and implement a diagnostic policy in which a proven-wrong final answer can enter Independent only when two isolated semantic judgments confirm that the learner's complete rationale is substantively applicable; no keyword blacklist is used.

## Current Phase
Phase 2 — Design review

## Phases

### Phase 1: Requirements & Discovery
- [x] Preserve the existing product allowance for a wrong final answer with a genuinely correct rationale.
- [x] Confirm that rationale sufficiency is semantic and must be model-judged from the entire response, not keyword-matched.
- [x] Reproduce the real diagnostic pass and identify its persisted assessment decision.
- **Status:** completed

### Phase 2: Planning & Structure
- [x] Define the scoped policy and routing matrix for proven-wrong final answers.
- [x] Identify the Assessment, Response Verification, Diagnostic Flow, prompt, and contract-test seams.
- [ ] Obtain user approval of the policy before creating an ADR, ticket, or production-code change.
- **Status:** in_progress

### Phase 3: Implementation
- [ ] Add an ADR/spec amendment for the diagnostic rationale rescue rule and its failure semantics.
- [ ] Change the diagnostic assessment orchestration so a first `APPLICABLE` rationale verdict triggers a role-distinct, isolated Response Verification.
- [ ] Route semantic insufficiency or verifier disagreement after a proven-wrong answer to remediation, never to Independent.
- [ ] Preserve `unavailable`/retry for provider or model-contract failure, distinct from learner rationale insufficiency.
- [ ] Tighten the assessment prompt and add a dedicated adversarial rationale-verification prompt with full-response semantic examples; do not add phrase blacklists.
- [ ] Add focused scripted unit, graph, HTTP, and PostgreSQL recovery coverage.
- **Status:** pending

### Phase 4: Testing & Verification
- [ ] Run focused diagnostic rationale tests, then `./mvnw clean test` with PostgreSQL available.
- [ ] Verify that all learner-visible branches remain projections of committed state and replays remain exactly-once.
- [ ] Run the non-blocking live-model smoke only as supplemental evidence, not as the regression oracle.
- **Status:** pending

### Phase 5: Delivery
- [ ] Review the policy matrix and implementation against the approved ADR/spec.
- [ ] Summarize the behavior change, test evidence, and any residual false-negative tradeoff.
- **Status:** pending

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| No keyword blacklist | A qualifier such as “我不知道，不过……” must not erase a substantively correct explanation. |
| Scope the change to `PROVEN_NOT_EQUIVALENT` diagnostic answers | Only this state has a deterministically known wrong final expression and needs rationale rescue. |
| Require two role-distinct isolated `APPLICABLE` judgments before rationale rescue | A byte-for-byte repeated call to the current Strong model/prompt offers little protection against the reproduced semantic error. |
| Keep the current Strong binding for V1 but give Response Verification an adversarial prompt | The current frozen Model Profile exposes Strong and Small only. A third verifier binding is a larger operator-model design decision, not a prerequisite for this narrow fix. |
| Semantic insufficiency remediates; technical failure is unavailable/retry | An unproven rationale must not establish readiness, but provider or contract failure is never learner failure. |

## Errors Encountered
| Error | Resolution |
|-------|------------|
| Sandboxed Maven cannot write its local dependency metadata or attach test agents | Targeted verification was rerun in the approved local development environment. |
