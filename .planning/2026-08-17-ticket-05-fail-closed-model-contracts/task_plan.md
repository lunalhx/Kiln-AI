# Ticket 05 — Fail-closed model contracts

## Goal

Model contract errors are not provider outages. The adapter returns raw content; the Domain strictly parses closed contracts and applies responsibility-specific recovery. Learners never see 503, parser details, or raw invalid JSON.

## Seams (from spec Testing Decisions)

1. Focused domain parse tests: schema closure, missing fields, nulls, invalid enums, unknown fields, violation-code normalization.
2. Whole-flow `LearningFlowGraphContractTest`: malformed Task Verification → fresh candidate; Assessment/Verification one repair; two invalids → Inconclusive, no Evidence, no invalid persistence.
3. HTTP command seam: `MODEL_CONTRACT_INVALID` is not 503; body is learner-safe.

## Phases

| Phase | Status | Verify |
| --- | --- | --- |
| 1. Domain parse + violation codes | in_progress | `ResponseAssessment`/`TaskVerificationVerdict`/`TeachBackAssessment`/`ClarificationClassification` parse tests |
| 2. Adapter transport-only | pending | `ApplyModelAdapterTest` returns raw JSON; no parse-to-503 |
| 3. Assessment/TV/Teach-back recovery | pending | graph contract: one repair, two invalids → Inconclusive, no Evidence |
| 4. Audit + HTTP mapping | pending | audit has no raw JSON; HTTP not 503 |
| 5. Full suite + review + commit | pending | `./mvnw clean test` |

## Out of scope

Tickets 06–10. Pedagogy/Clarification keep existing fallbacks. No new graph framework.
