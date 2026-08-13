# Evaluate Spring AI Alibaba Graph

## Status

Completed. All five hard gates passed. Phase 0 adopts Graph Core `1.1.2.2` as the runtime adapter under [ADR-0034](../adr/0034-use-spring-ai-alibaba-graph-as-the-phase-0-runtime.md). Evidence: `docs/spikes/spring-ai-alibaba-graph-evaluation-report.md`.

## Goal

Determine whether Spring AI Alibaba Graph can run one vertical Kiln-AI learning slice without owning domain semantics or weakening evidence guarantees.

## Vertical slice

The spike starts from a persisted Learning State, executes a deterministic Explain transition, exposes more than one legal next Teaching Action, invokes the bounded Pedagogy Agent to select Apply, resolves and progressively loads one Action Skill plus one Capability Skill, creates a Task Attempt, pauses for learner input, assesses the resumed attempt, accepts evidence once, and persists the resulting checkpoint and trace.

## Hard acceptance gates

The spike passes only if all five gates pass. These gates are not averaged or traded against developer convenience.

1. **Domain isolation** — Framework classes remain inside a Graph Adapter. Domain types, persistence records, API contracts, Workflow Guard rules, and evidence reducers do not depend on Spring AI Alibaba Graph.
2. **Routing correctness** — Deterministic transitions do not call a routing model. The Pedagogy Agent can select only from actions exposed by the Workflow Guard and does not own graph state or Skill resolution.
3. **Progressive Skill loading** — The selected Teaching Node loads only its required common constraints, exactly one Action Skill, matching Capability Skills, and optional Subject Skills. The trace records selected Skill IDs, versions, and routing reasons.
4. **Reliable recovery** — Execution can resume from a persisted checkpoint after interruption. Retries may repeat an external model request, but idempotency and evidence validation prevent duplicate accepted evidence, duplicated state advancement, or reuse of a completed Task Attempt.
5. **Testability and observability** — Graph transitions can be tested with model nodes replaced by deterministic fakes. A run exposes node transitions, candidate and selected routes, Skill versions, model and prompt identifiers, token usage, latency, checkpoint identity, and failure/retry information.

The spike must also demonstrate that a model node's raw result passes through the application-owned Typed Artifact Gate Pipeline and an authorized State Reducer before any value is merged into checkpointed graph State.

## Failure rule

Failure of any gate rejects framework adoption for Phase 0. If passing a gate requires framework types to leak into the domain or substantial lifecycle workarounds, that gate is considered failed even if the demonstration completes.

## Fallback

Use an application-owned lightweight Java transition engine with Spring AI limited to model and tool calls. Preserve the same graph port, node contracts, checkpoints, and observability fields so the architecture does not change with the runtime implementation.

## Non-goals

- Benchmarking model quality or learning efficacy
- Implementing all five Teaching Node Profiles
- Production deployment or horizontal scaling
- Multi-Agent messaging, A2A, group chat, or parallel workers
- Choosing a permanent database, model provider, or observability vendor
