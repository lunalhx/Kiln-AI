---
status: accepted
---

# Cancel a Started Review Task when an explicit leave abandons its open Attempt

> Superseded by ADR-0073: Started Review cancellation goes only through the
> independent idempotent cancel resource. The explicit leave still abandons
> the open Attempt (ADR-0015) but no longer cancels the Review Task.

ADR-0015 closes any open Task Attempt as Abandoned when the learner explicitly
leaves the Learning Flow, but it does not say what happens to the Review Task
whose Attempt was abandoned. Without a rule, a Started Review Task would stay
bound to the abandoned Attempt's id forever: it is not startable (its open
Attempt pointer is set) and never completed or cancelled, so it would remain a
permanently stuck item in the unfinished Review collection.

When the Flow Control Requested leave abandons an open Review Attempt, the
graph therefore also cancels the learner's Started Review Task for that
Concept in the same deterministic step — no Review Evidence is accepted and no
Milestone changes, exactly like the assistance conversion of ADR-0062. The
cancelled Review Task leaves no unfinished work, and any later qualifying
Independent pass restarts the cadence at Review 1 through the existing
evidence transitions. The leave never fabricates an interaction: it commits a
terminal transition boundary carrying only the leave message, projected from
durable state.

This decision extends ADR-0015 for the unified command surface; it is not a
compatibility layer, and the cancellation has no fallback.
