---
status: accepted
---

# Defer cross-Flow learner memory in Phase 0

Phase 0 will not implement a global Learner Context Store, Agent-authored long-term memory, cross-Flow raw conversation memory, learned preference profile, or inferred learner traits. Every Learning Blackboard is scoped to exactly one Learning Flow, and Context Builders cannot search unrelated Flow Blackboards. This exclusion does not remove required product records: confirmed Concept Contracts, Task Attempts, Assistance Traces, accepted Learning Evidence, Concept Progress, review schedules, and the checkpoints needed to resume the same Flow remain durable domain state rather than Agent memory. A resumed Flow may reconstruct context for its own Target Concept from those records. Cross-Concept personalization and memory-writing policies require a later decision supported by real product need.
