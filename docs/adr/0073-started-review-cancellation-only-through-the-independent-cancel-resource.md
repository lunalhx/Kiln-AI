---
status: accepted
---

# Started Review cancellation goes only through the independent cancel resource

ADR-0068 said that when a Flow Control Requested leave abandons an open Review
Attempt, the graph also cancels the learner's Started Review Task in the same
deterministic step. With a dedicated idempotent cancellation resource at
`POST /api/review-tasks/{reviewId}/cancel` (ADR-0070), that implicit
leave-cancellation creates a second, competing command path for the same
durable state change.

Cancellation of a Started Review Task therefore goes only through the
independent cancel resource. The explicit leave (Flow Control Requested) still
abandons any open Attempt as Abandoned (ADR-0015) — no submission, Assessment,
or Evidence — but it no longer cancels the Review Task. A Started Review whose
open Attempt was abandoned stays Started and bound to the abandoned Attempt
until the learner explicitly cancels it through the cancel resource, which
atomically abandons the open Attempt (already abandoned in this case), marks
the Review Task Cancelled, and commits a terminal Flow transition for the old
Flow. Cancellation never accepts Learning Evidence and changes neither Current
Milestone nor Highest Milestone Reached; replaying cancellation after the
Review is terminal returns the committed state without another effect.

This decision supersedes the leave-cancels-Started-Review path of ADR-0068 and
extends ADR-0015 and ADR-0070 for the unified command surface. It is not a
compatibility layer, and the removal has no fallback.