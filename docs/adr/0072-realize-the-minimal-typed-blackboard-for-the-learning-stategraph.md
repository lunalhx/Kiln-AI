---
status: accepted
---

# Realize the minimal typed Blackboard for the Learning StateGraph

ADR-0021 clarified that the direct Apply reference coordinates through a
closed `apply_execution_context/v1` JSON object and durable typed Apply stores
instead of a Blackboard, and that "the Blackboard itself returns when later
multi-node Profiles need cross-node collaboration." ADR-0064 restored the
application-owned Learning StateGraph for multi-Profile Learning and Practice,
so that collaboration is needed now: one Graph Run routes a learner command
through Diagnostic, Explain, Apply Practice, Hint, Teach-back, Independent
Test, or Review, then stops at the next Learner Interaction Boundary.

The minimal typed Blackboard is therefore realized directly: the durable typed
store of one Learning Flow — the Flow record, every learner-visible
interaction, the graph checkpoint, the processed-command ledger, and the
exposure ledgers — IS the Blackboard. Before every Graph Run the graph
rehydrates a typed `LearningState` snapshot from those durable records and the
latest checkpoint, and every boundary commit atomically persists the learner
interaction, its checkpoint, and the processed command. Node-specific Context
Views remain least-privilege closed projections (`apply_execution_context/v1`,
`LearnerProjection`, `TeachingProjection`, `FeedbackFacts`), and Assessment
stays isolated. No node receives unrestricted write access; only the graph
runner and its nodes own the store.

This covers the ADR-0021 clarification: the deferred "Blackboard returns when
multi-node Profiles need cross-node collaboration" is now satisfied by this
minimal typed Blackboard. No separate Blackboard table, cache, or graph
framework is introduced, and no compatibility layer preserves the direct
Apply-only coordination of the earlier clarification.

Consistent with the Learning semantics of CONTEXT.md (Learning State, Learning
Blackboard), the graph-owned interaction, checkpoint, and result types carry
Learning names — `LearningFlowInteraction`, `LearningCheckpoint`, and
`LearningFlowResult` — while `Apply*` names remain reserved for task
generation, verification, and assessment.