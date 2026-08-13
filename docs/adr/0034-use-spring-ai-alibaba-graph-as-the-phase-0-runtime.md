---
status: accepted
---

# Use Spring AI Alibaba Graph as the Phase 0 graph runtime

Phase 0 will execute the application-owned Learning StateGraph through Spring AI Alibaba Graph Core `1.1.2.2` behind the Graph Adapter in `kiln-ai-infrastructure`. This is the implementation decision left open by ADR-0011. Framework types remain inside that adapter; Workflow Guard, Pedagogy planning, Skill resolution, artifact gates, reducers, evidence acceptance, checkpoints, and HTTP contracts stay application-owned. The adapter uses a custom checkpoint commit that writes application effects atomically and does not use the framework Postgres saver. This decision adopts only the Graph Core runtime, not Spring AI Alibaba chat, RAG, or Agent Framework products. A Graph version change must re-run the five hard gates in `docs/spikes/spring-ai-alibaba-graph-evaluation-report.md`; failure of any gate falls back to an application-owned lightweight Java transition engine behind the same graph port.
