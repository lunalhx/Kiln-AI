---
status: accepted
---

# Restore the Learning StateGraph for multi-Profile learning

The destructive Apply cutover superseded the original graph runtime because one direct durable Apply flow did not need multi-node teaching orchestration. Learning and Practice now introduces Explain, Hint, Teach-back, Apply Practice, Assessment, and the bounded Pedagogy Agent, so continuing to enlarge `ApplyFlowUseCase` would give the Apply Teaching Action ownership of cross-Profile pedagogy and routing.

Multi-Profile Learning and Practice will therefore restore the application-owned Learning StateGraph boundary, with a deterministic Workflow Guard, typed Learning Blackboard, durable checkpoints, and node-specific Context Views. Existing durable Apply generation, verification, submission, replay, and evidence behavior is reused as Apply node capability rather than replaced or duplicated. The graph coordinates validated artifacts and committed state; it does not move Assessment into Teaching Profiles, let the Pedagogy Agent mutate state, or weaken the exactly-once command and atomic-persistence invariants.

This decision restores the domain architecture anticipated by ADR-0011 and ADR-0021 without selecting a graph framework. Framework adoption remains a separate implementation decision, and Delayed Review scheduling remains application-owned work that performs no model call.
