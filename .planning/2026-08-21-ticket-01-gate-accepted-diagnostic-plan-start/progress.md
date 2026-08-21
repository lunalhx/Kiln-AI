# Progress Log: Ticket 01 — Gate-accepted Diagnostic Plan start

## Session: 2026-08-21

### Initial setup

- Read the implement, planning-with-files, karpathy-guidelines, TDD, and code-review skill instructions.
- Read `AGENTS.md`, `README.md`, `CONTEXT.md`, ticket 01, and the complete diagnostic optimization spec.
- Restored the prior diagnostic discovery record; it confirms ticket 01 is a new preparation/start capability and later runtime behavior must remain separate.
- Worktree was clean before implementation work.
- Created this isolated plan to avoid mutating the completed discovery plan.
- Read the direct ADR baseline: ADR-0001, 0016, 0022, 0033, 0040, 0041, 0042, 0043, 0055, and 0063.
- The first attempt to update the plan pointer used an empty hunk and was rejected; the patch was retried with an exact line replacement and succeeded.
- Inspected the current start path: `LearningFlowCommandUseCase` hard-codes `DiagnosticApplyFixture`, `LearningStateGraph.start` prepares before mutation, and `LearningFlowStore.bindStart` owns the atomic Flow/package/Attempt/interaction commit.
- Confirmed there is no existing Diagnostic Plan model, preparation port, persisted Plan identity, or learner-visible completed/max projection.
- Mapped the minimal implementation seam: typed Plan + Gate + preparation Agent, accepted-plan lookup at Start, dedicated durable Plan/progress row, and a learner-safe progress projection; ticket 02 task selection is intentionally deferred.
- First focused Maven invocation without dependent modules failed before the new test because existing types from `kiln-ai-types` were not built; this is logged and the next attempt uses `-am`.
- The `-am` compile reached tests but Surefire discovered stale compiled classes from a different package in `target/test-classes`; the next run will clean targets first.
- Added the typed `DiagnosticPlan`, preparation facts, and type-specific Gate. The Gate contract now passes eight focused tests covering the 1/8 boundary, maximum overflow, source grounding, readiness safety, Rubric expansion, rationale policy, unknown Supporting Concepts, and dependency cycles.
- The first post-change focused run found a test-only generic-erasure helper collision; renamed the fixture copy helpers and re-ran successfully.
- Added the closed generation draft and `DiagnosticPlanPreparationAgent`; focused preparation tests pass for accepted Plan, Source Gap, and rejected over-ceiling output.
- Added the accepted-plan start seam, frozen Plan/progress persistence in both stores, atomic Source Gap/no-plan rejection coverage, and public/API/UI projection limited to completed/max attempts.
- Added PostgreSQL assertions for the `diagnostic_plans` schema, full Plan round-trip, and committed progress advancing from `0/3` to `1/3`; the focused PostgreSQL success-path suite passes 6 tests.
- Full verification passes: `./mvnw clean test` succeeds across all seven modules (domain 447, infrastructure 29, application 82 with the non-blocking live smoke skipped); `git diff --check` passes. No separate typecheck/lint goal is defined by the Maven project.
- Review follow-up: the reference accepted-plan provider now executes the closed Plan generation contract through `DiagnosticPlanPreparationAgent` and returns no plan on Source Gap, Gate rejection, or preparation failure, so the public start path cannot bind an unaccepted candidate.
- Added the public frozen-version contract: changing the provider's later accepted version leaves the started Flow's persisted Plan at its original version.
- Final focused regression after the provider-failure catch passes 137 tests; final full clean verification remains green.
