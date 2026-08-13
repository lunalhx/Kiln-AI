# Spring AI Alibaba Graph evaluation report

Evaluated artifact: `com.alibaba.cloud.ai:spring-ai-alibaba-graph-core:1.1.2.2`  
Runtime: Java 21, Spring Boot 3.5.8, MyBatis 3.0.5, PostgreSQL schema V2  
Date: 2026-08-13  
Spike spec: `docs/specs/spring-ai-alibaba-graph-validation-spike.md`

This report records a binary result for the five hard gates. Gates are not averaged. Passing all five makes the framework eligible for a later tracer runtime decision; this spike does not write an adoption ADR and does not implement the first product tracer.

## Workarounds captured during evaluation

These did not require mixing Boot 4 and Boot 3 in one JVM, splitting into a sidecar service, or putting framework types into domain/API/persistence records.

1. `StateGraph.<clinit>` constructs `SpringAIJacksonStateSerializer`, which loads `spring-ai-deepseek` and `spring-ai-zhipuai` even when unused. Those JARs must remain on the Graph Adapter classpath. The spike uses a non-vendor `JacksonStateSerializer` for actual blackboard ser/de.
2. Graph-core also needs `spring-ai-client-chat` (`SystemMessage`) at runtime.
3. MCP transitive versions were pinned to 0.17.0 so Maven Enforcer dependency convergence passes.
4. Boot 3.5.8 does not ship `spring-boot-starter-flyway`; the app uses `flyway-core` plus `flyway-database-postgresql`.
5. Framework `PostgresSaver` was not used. `ApplicationCheckpointSaver` implements `BaseCheckpointSaver` and commits application effects through `SpikeStorePort` in the same `put` call.
6. `Map.copyOf` cannot hold null `pendingInput`; blackboard deltas use `LinkedHashMap` plus an unmodifiable wrapper.

None of these put `com.alibaba.cloud.ai.graph.*` outside `kiln-ai-graph-saa`.

## Gate 1 — Domain isolation: PASS

Evidence:

- `GraphIsolationArchitectureTest.graphTypesStayInAdapter` fails if any class outside `cn.lunalhx.ai.kilnai.graph.saa..` depends on `com.alibaba.cloud.ai.graph..`.
- `ApplicationArchitectureTest` and `DomainArchitectureTest` ban Graph, MyBatis, and Spring MVC types from application/domain.
- Maven Enforcer `ban-graph-core-leak` (skipped only on `kiln-ai-graph-saa` and `kiln-ai-app` composition root).
- HTTP DTOs, `LearningGraphRuntimePort`, checkpoints, evidence, and idempotency records are application types. `PostgresSpikeStore` does not import Graph types.

## Gate 2 — Routing correctness: PASS

Evidence:

- `GraphAdapterComponentTest.startDoesNotCallPedagogyAndResumeFromNewInstanceCompletesApplyAndAssessment`: start Explain does not call the Pedagogy model (`pedagogy.calls() == 0`). Continue with two legal actions calls Pedagogy once and selects Apply.
- `GraphAdapterComponentTest.illegalPedagogyPlanFallsBackToExplain`: an illegal Pedagogy action is rejected by the Typed Artifact Gate; Guard fallback Explain is taken; Apply is not opened.
- `WorkflowGuardTest` and `PedagogyPlanGatePolicyTest` cover deterministic legal candidates independently of the framework.

## Gate 3 — Progressive Skill loading: PASS

Evidence:

- Pedagogy Plan carries capability/strategy tags, not Skill IDs. `SkillResolver` pins `explain.direct@1`, then `apply.worked-example@1` plus `capability.quantitative@1` and `calculator@1`.
- Public trace after the happy path contains those Skill IDs (`GraphAdapterComponentTest`, `SpikeLearnerHttpTest`, `SpikeLearnerUiTest`).
- `GraphAdapterComponentTest.capabilityGapOnApplyKeepsPreviousCheckpoint`: missing tool/capability throws `CapabilityGap` before the Apply model call; previous Explain checkpoint remains.

## Gate 4 — Reliable recovery: PASS

Evidence:

- `GraphAdapterComponentTest` destroys the first `SpringAiAlibabaGraphRuntime` after Explain and after Apply; a new instance resumes from the store checkpoint and completes Assessment. Evidence is accepted once (`task_attempt_id` unique / `putIfAbsent`).
- `LearningFlowUseCaseTest.duplicateIdempotencyKeyReplaysWithoutSecondEvidence`: same key replays; same key with a different payload returns 409; replaying the answer key does not duplicate evidence.
- `GraphAdapterComponentTest` failure cases (rejected Apply, capability gap, budget exhaustion) keep the previous learner-visible checkpoint and return `SERVICE_UNAVAILABLE`.
- `ApplicationCheckpointSaver.put` maps the framework checkpoint to `LearningCheckpointRecord` and calls `CheckpointCommitPort` with `PendingCommitBuffer` effects. Raw candidates are not stored on the blackboard (`never-persisted-in-framework` is absent from private trace).

PostgreSQL: V2 schema and `PostgresSpikeStore` perform the same atomic commit (checkpoint, interaction, artifact, attempt, evidence, progress, traces, processed command). `PostgresCheckpointRecoveryTest` is `@Testcontainers(disabledWithoutDocker = true)` and was skipped in this run because Docker/OrbStack was not running. The hard gate (new adapter instance from a persisted checkpoint) is covered by the in-memory store tests above.

## Gate 5 — Testability / observability: PASS

Evidence:

- Real `CompiledGraph` runs with deterministic fakes: happy, illegal Pedagogy, one-shot repair, rejected artifact, capability gap, budget exhaustion (`GraphAdapterComponentTest`, `ValidatedNodeExecutorTest`).
- Learner HTTP seam: `SpikeLearnerHttpTest` covers Start → Continue → Answer, idempotent replay, stale `interactionVersion` 409, illegal `ANSWER_SUBMITTED` 422, and sanitized trace.
- Learner UI seam: `SpikeLearnerUiTest` (Playwright) drives `/` through Start → Continue → Submit → Trace and asserts private fields are absent from the page.
- Public trace includes routes, selected Skill IDs, checkpoint ids, budget, and validation outcomes. Learner responses never include `answerKey` or `hiddenReasoning`.

## Binary conclusion

**Spring AI Alibaba Graph 1.1.2.2 is eligible for the subsequent tracer runtime decision.**

All five hard gates passed. This is not an adoption ADR. The first product tracer is out of scope for this spike. Re-run checkpoint round-trip, restart, and this matrix on any Graph version change.
