# Spec: Connect real models through Spring AI and an operator Provider Catalog

## Problem Statement

Kiln-AI can already run a Learning Flow through Spring AI Alibaba Graph, but Teaching, Pedagogy, and Assessment still sit on scripted fakes. Operators cannot attach a real provider, add another OpenAI-compatible vendor without new code, or keep a frozen model identity on a Learning Flow. The graph spike also mixed node work and later tool follow-up into one call counter, which would make real tool calling exhaust a Graph Run for the wrong reason.

## Solution

Put one infrastructure adapter in front of Spring AI `ChatClient` / `ChatModel` and an operator-owned Provider Catalog. Operators register providers as protocol plus endpoint plus listed models, then set a Strong Model and a Small Model as `providerId/modelId`. Starting a Learning Flow freezes a resolved snapshot of protocol, endpoint, and model identity onto that flow. Learners never choose or change models. Teaching, Pedagogy, and Assessment share this adapter. Tests keep injecting scripted fakes at the domain ports. Missing catalog, profile, or secret fails closed and does not fall back to fakes.

Graph Run Budget counts model-producing node entries only. Authorized tool executions have a separate Tool Budget. Teaching uses Spring AI per-call `.tools(...)` from the application ToolResolver; shared clients do not register `defaultTools`.

## User Stories

1. As an operator, I want to register an OpenAI-compatible provider by protocol, endpoint, and listed models, so that a new compatible vendor is a catalog entry rather than new application code.

2. As an operator, I want to set a Strong Model and a Small Model as `providerId/modelId`, so that Teaching, Assessment, and Task Verification can use a stronger model while Pedagogy, Input Interpreter, and format repair use a cheaper one without a Binding per responsibility.

3. As an operator, I want API secrets to live in environment variables, so that keys are not copied into Learning State or the Provider Catalog.

4. As an operator, I want to edit the catalog and default Strong/Small Model without an admin UI or `/connect` flow, so that Phase 0 stays a configuration-file operator surface.

5. As an operator, I want a Learning Flow to fail closed when the catalog, Strong/Small Model, or secret is missing, so that the system never silently teaches with scripted fakes.

6. As an operator, I want editing the catalog after a flow has started to affect only new flows, so that an in-flight Learning Flow cannot be rebound or failed over to another provider.

7. As a learner, I want to start, continue, and answer through the existing learner-facing flow without choosing a model, so that model selection is not part of the learning product.

8. As a learner, I want resume, repair, and Assessment on my current Learning Flow to use the same frozen provider and model identity that was copied at start, so that evidence for this flow stays auditable.

9. As a learner, I want a later user system to still be unable to choose models, so that model selection remains operator-owned.

10. As a learner, I want provider errors and budget exhaustion to stop only that Graph Run with an existing safe outcome, so that the Learning Flow is not ended and the system does not secretly retry on another provider.

11. As a domain maintainer, I want Teaching, Pedagogy, and Assessment to share one Spring AI adapter, so that a second integration style is not left behind for the remaining ports.

12. As a domain maintainer, I want domain ports and domain types to stay free of Spring AI types, so that ChatClient remains an infrastructure concern like Graph Core.

13. As a domain maintainer, I want Pedagogy to use the Small Model and Teaching and Assessment to use the Strong Model, so that the accepted size split is applied without learner choice.

14. As a domain maintainer, I want Assessment to keep an isolated context from Teaching even when both Bindings resolve to the same provider, so that assessment does not receive teaching hidden reasoning.

15. As a domain maintainer, I want each model call to receive a fresh Node Context View and compiled prompt, so that Spring AI Chat Memory is not used as cross-node memory.

16. As a domain maintainer, I want Teaching to use Spring AI ChatClient tool calling on that request only, so that the application does not invent a second tool protocol.

17. As a domain maintainer, I want the authorized tool set to come from the existing ToolResolver and frozen Skill Stack, so that a Teaching Node cannot see or gain tools outside its Profile.

18. As a domain maintainer, I want shared ChatClient beans to have no `defaultTools`, so that tools cannot leak across nodes.

19. As a domain maintainer, I want Pedagogy and Assessment to receive no tools, so that only Teaching in this slice can execute authorized tools.

20. As a domain maintainer, I want a tool execution to consume Tool Budget rather than Graph Run node budget, so that calculator follow-up cannot exhaust the node ceiling.

21. As a domain maintainer, I want an Ordinary Graph Run to enter at most three model-producing nodes and a High-Consequence Graph Run at most four, so that one wake-up cannot chain unbounded model-backed nodes.

22. As a domain maintainer, I want deterministic gates not to consume Graph Run Budget, so that input and Guard steps are not counted as model-producing nodes.

23. As a domain maintainer, I want gate repair to remain at most one extra attempt on the same node and not count as a new node entry, so that retry is bounded without being mixed into the tool counter.

24. As a domain maintainer, I want Tool Budget exhaustion to stop that Graph Run with a declared safe outcome and not end the Learning Flow, so that a tool loop cannot become a second Agent.

25. As a domain maintainer, I want Token, cost, and latency to remain traced, so that real provider calls are observable without making those ceilings product constants.

26. As a domain maintainer, I want the frozen provider and model identity to appear in the execution trace, so that a completed flow can answer which model produced Teaching or Assessment.

27. As a domain maintainer, I want Phase 0 to keep refusing unchecked raw token streaming to the learner, so that Output Gate still runs on a complete Teaching Result Envelope before delivery.

28. As a test maintainer, I want existing graph, HTTP, and UI tests to keep injecting scripted fakes at the domain ports, so that Learning StateGraph behavior stays deterministic.

29. As a test maintainer, I want scripted fakes not to be a Provider Catalog entry, so that tests cannot accidentally look like a production provider.

30. As a test maintainer, I want architecture tests to keep failing if Spring AI types enter the domain, so that the adapter boundary stays enforceable.

31. As an operator, I want Input Interpreter and Task Verifier left unimplemented in this slice but required to use the same adapter contract later, so that those nodes do not grow a second provider stack.

## Implementation Decisions

- Use Spring AI `ChatModel` / `ChatClient` as the provider layer. Do not adopt Spring AI Alibaba chat, RAG, or Agent Framework. Graph Core remains the graph runtime only (ADR-0034, ADR-0037).
- Align Spring AI usage with the Graph Core already on the classpath (Spring AI 1.1.2). Do not introduce a second Spring AI version.
- Place the adapter in infrastructure beside the Graph Adapter. Domain Teaching, Pedagogy, and Assessment ports remain the application contracts. Domain, API, and persistence records do not depend on `org.springframework.ai`.
- The default protocol is OpenAI-compatible: Spring AI's OpenAI ChatModel with a configurable base URL. This slice does not add a DashScope protocol implementation; that remains an exception only if an API cannot speak OpenAI-compatible later.
- Operator surface is a Provider Catalog configuration file plus environment secrets. No admin UI, no `/connect`, no Models.dev, no BYOK.
- Model identity is `providerId/modelId`. Operators set only Strong Model and Small Model. Teaching, Assessment, and Task Verification inherit Strong Model. Pedagogy, Input Interpreter, and format repair inherit Small Model.
- Starting a Learning Flow copies a resolved snapshot of protocol, endpoint, and model identity onto that flow and freezes it for resume, repair, and Assessment. Secrets are not copied. Live catalog edits do not rebind an in-flight flow. There is no mid-execution provider failover.
- Application runtime uses the adapter and fails closed when catalog, Strong/Small Model, or secret is missing. Test configurations continue to replace the three ports with scripted fakes. Scripted is not a catalog provider.
- Graph Run Budget counts model-producing node entries: at most three for an Ordinary Run and four for a High-Consequence Run. Repair remains at most one extra attempt on the same node and is not a new node entry. Tool executions have a separate Tool Budget whose numeric ceiling is operator configuration, not a product constant.
- Teaching maps authorized `ToolHandle`s from ToolResolver onto that ChatClient request via `.tools(...)`. Shared clients must not register `defaultTools`. Tool callbacks execute the application `ToolSession`. Pedagogy and Assessment pass no tools.
- Do not enable Spring AI Chat Memory or other advisors that accumulate cross-node conversation. Each call is built from the Node Context View and Prompt Compiler.
- Provider errors, node-budget exhaustion, and tool-budget exhaustion stop that Graph Run with the existing declared safe outcome and do not end the Learning Flow or switch provider.
- Learner-facing HTTP/UI contracts stay as they are. This slice does not add a learner model picker or a learner-visible provider chooser.

## Testing Decisions

A good test asserts observable behavior at an existing seam: flow start/resume, learner-visible content, traces, fail-closed rejection, architecture isolation, and authorized tools. It does not assert ChatClient internals, bean names, or a particular config-file syntax.

Preferred seams, in order:

1. Existing learner HTTP and UI tests, and Graph Adapter component tests, with scripted fakes injected at the domain ports. These must keep proving graph, gate, checkpoint, and privacy behavior without a live provider.
2. Infrastructure adapter tests that replace ChatModel with a deterministic test double: catalog resolution to Strong/Small Model, freeze snapshot used on resume, fail-closed when catalog/profile/secret is missing, per-call tools only from ToolResolver, no tools on Pedagogy/Assessment, node budget separate from Tool Budget, no Spring AI types leaking into domain.
3. Existing `DomainArchitectureTest` / graph isolation tests, extended only as needed to keep Spring AI out of domain and API.

Do not require a live vendor API key for CI. Prior art: `GraphAdapterComponentTest`, `LearningFlowUseCaseTest`, `SpikeLearnerHttpTest`, `SpikeLearnerUiTest`, `DomainArchitectureTest`, `ToolResolverTest`, `ValidatedNodeExecutorTest`.

## Out of Scope

- Admin UI, `/connect`, Models.dev, learner model picker, BYOK, and a user system.
- Implementing Input Interpreter or Task Verifier nodes.
- Implementing a DashScope-native protocol in this slice.
- Retrieve and Teach-back Teaching Node Profiles.
- Streaming unchecked model tokens to the learner.
- Cross-node Chat Memory or Learner Memory.
- Changing an in-flight flow's Model Profile.
- Replacing Spring AI Alibaba Graph Core.
- Choosing a permanent first vendor, a catalog file path/format, or a numeric Tool Budget default as product constants.

## Further Notes

These items affect implementation but were left unset on purpose. Do not invent values while implementing:

- Provider Catalog file format and location.
- The numeric Tool Budget default.
- Whether anyone performs a manual live call against a real vendor after CI (CI uses fakes and ChatModel test doubles).
- Exact persistence field layout for the frozen snapshot, so long as resume uses the snapshot rather than a live catalog lookup.

Glossary and ADRs for this slice: `CONTEXT.md`; ADR-0034, ADR-0035, ADR-0036, ADR-0037, ADR-0038, ADR-0039.
