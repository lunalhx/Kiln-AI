---
status: superseded
---

# Use Spring AI Alibaba Graph as the Phase 0 graph runtime

> Superseded by the destructive Apply cutover (ticket 06): the graph adapter,
> its dependency, and its evaluation spike were removed. The shipped Apply
> reference coordinates through `ApplyFlowUseCase` and the durable Apply
> stores; no graph library is on the classpath.

Phase 0 will execute the application-owned Learning StateGraph through Spring AI Alibaba Graph Core `1.1.2.2` behind the Graph Adapter in `kiln-ai-infrastructure`. This is the implementation decision left open by ADR-0011. Framework types remain inside that adapter; Workflow Guard, Pedagogy planning, Skill resolution, artifact gates, reducers, evidence acceptance, checkpoints, and HTTP contracts stay application-owned. The adapter uses a custom checkpoint commit that writes application effects atomically and does not use the framework Postgres saver. This decision adopts only the Graph Core runtime, not Spring AI Alibaba chat, RAG, or Agent Framework products.
