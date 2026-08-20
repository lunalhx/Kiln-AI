# Findings: Ticket 5 — corroborated rationale rescue routing

## Requirements
- Only a proven-incorrect primary answer with a first `applicable` rationale invokes the corroborating `evaluation.counterexample-review@1.0.0` plus the shared `verification.rationale-sufficiency@1.0.0` stack.
- Two isolated `applicable` results use the Neutral Transition to deliver a Fresh Independent Test and create no Diagnostic Evidence.
- First `inconclusive`, second `not_applicable`, or second `inconclusive` becomes Unconfirmed Diagnostic Performance with neutral Feedback Facts and only Explain/Apply Practice.
- `Cannot Decide` never enables rationale rescue; all Diagnostic branches create no Evidence.
- Technical failures and persistent contract invalidity use durable Unavailable/Retry; replay and PostgreSQL recovery skip committed responsibilities.

## Normative source decisions
- `docs/specs/diagnostic-rationale-corroboration-spec.md` is authoritative.
- Relevant decisions: ADR-0075 (separate Evaluation Skill Stacks and two-applicable rescue), ADR-0076 (post-submission contract failure is Unavailable), ADR-0077 (durable evaluation checkpoints), ADR-0078 (closed rationale result/context contract), with ADR-0057 and ADR-0071 amendments and ADR-0063/0069 replay/retry invariants.

## Baseline discoveries
- Ticket 1–4 are already committed on `main`; ticket 4 provides the first rationale-assessment gate and the current `EvaluationProfile`/rationale contract seam.
- The repository uses a six-module Maven architecture and `./mvnw clean test` is the required final verification.
- Root planning files belong to an older ticket 10 plan; this task uses this isolated plan directory to avoid overwriting them.

## Resolved implementation questions
- The existing `AssessmentRunner`/`DiagnosticFlow` seam owns the two sequential rationale responsibilities; each responsibility reuses `ModelContractRepair` and the existing `evaluation_results` key.
- `RationaleEvaluationContractTest` is the profile/routing red seam, while `LearningFlowPostgresRecoveryContractTest` covers the durable retry/replay boundary.

## Code baseline after discovery
- `AssessmentRunner` currently handles the first rationale Evaluation Profile but must be read together with `DiagnosticFlow` and `LearningStateGraph` to determine whether a second responsibility can be added without changing other purposes.
- `RationaleEvaluationProfile` is already a reusable profile executor; the missing ticket-5 piece is the frozen counterexample Evaluation Stack and its invocation/checkpoint path.
- Existing tests include `RationaleEvaluationContractTest`, `ApplyProfileContractTest`, `LearningFlowGraphContractTest`, and PostgreSQL recovery contracts; these are the likely red/green seams.
- The repository has already cut over durable `evaluation_results`, `CommittedEvaluationResult`, and `RESUME_SUBMISSION_EVALUATION` infrastructure from prior tickets.

## Verification evidence
- Domain: 434 tests passed, including ArchUnit boundaries.
- Infrastructure: 29 tests passed.
- App: 80 tests passed, 1 live-model smoke test skipped by configuration.
- Full command: `./mvnw clean test` passed with Docker/Testcontainers available in the approved execution environment.
