---
status: superseded
---

# Use a guarded adaptive Learning StateGraph

> Superseded: the shipped Apply reference (ADR-0046 through ADR-0060) runs one
> durable, idempotent flow directly through `ApplyFlowUseCase` with no graph
> runtime. A graph-based coordinator may return when later Teaching Node
> Profiles (Explain, Retrieve, Teach-back, Hint) need multi-node routing.

Phase 0 will use a Learning StateGraph as the primary coordination runtime and owner of durable Learning State. A deterministic Workflow Guard exposes only legal graph transitions; one legal Teaching Action proceeds without a routing model call, while the bounded Pedagogy Agent defined by ADR-0020 produces feedback after accepted Assessment and selects among multiple legal moves without retaining graph control. Explain, Retrieve, Apply, Teach-back, and Hint are five reusable Teaching Node Profiles with distinct contracts and eligible Skills: the selected Profile receives a bounded Learning Context, the deterministic Skill Resolver and mechanical Skill Loader assemble its Skill Stack, and the node returns one typed result without private memory or inter-node messaging. Assessment, evidence acceptance, milestone projection, checkpoints, and recovery remain separate graph nodes or deterministic reducers. The domain graph and transition semantics remain application-owned and framework-independent; adopting a Java graph library is a separate implementation decision.
