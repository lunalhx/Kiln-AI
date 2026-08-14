---
status: accepted
---

# Pause the graph at learner interaction boundaries

> Clarification (ticket 06): the Apply reference implements this boundary
> behavior without a graph. `ApplyFlowUseCase` persists each learner-visible
> interaction with its checkpoint and processed command atomically, enters
> `AWAITING_LEARNER_INPUT`, and resumes from the persisted state in a new
> invocation. ADR-0011 and ADR-0034 are superseded; the durable interaction
> contract remains.

A Learning StateGraph Graph Run may execute multiple internal deterministic and model-backed nodes, but whenever Kiln-AI emits learner-visible content that requires a response it must persist Learning State and a checkpoint, enter `Awaiting Learner Input`, and stop consuming execution resources. A later real learner message starts a new Graph Run from that checkpoint; the system will not keep a Teaching Node or Agent process alive while waiting, cross the boundary by simulating learner input, or treat every internal node as a separate API turn. Terminal outcomes and explicit asynchronous waits may also stop a run. This makes user interaction, recovery, Task Attempt continuity, and horizontal execution independent of in-memory Agent lifetimes.
