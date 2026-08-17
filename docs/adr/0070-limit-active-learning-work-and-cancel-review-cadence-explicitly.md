---
status: accepted
---

# Limit active learning work and cancel Review Cadence explicitly

Phase 0 permits at most one active Learning Flow for a learner and Target
Concept. Active learning work includes either a non-terminal Flow or an
unfinished Review Task (`Scheduled`, `Due`, or `Started`) belonging to its
terminal Flow. This prevents concurrent diagnostics, competing state
transitions, and a second Flow from racing the one-unfinished-Review invariant.
The domain and durable store enforce the claim atomically; a UI button or local
storage is never the authority.

Starting while active learning work exists returns a learner-safe 409 carrying
the existing Flow ID, so a client can recover its committed interaction
instead of creating another Flow. Phase 0 retains its explicit trust boundary:
the API accepts the caller-supplied learner ID and possession of a Flow ID,
while authentication and authorization remain future work. Private assessor
data is never exposed by this recovery response or by Flow queries.

An unfinished Review Cadence is released only by its normal conclusive outcome
or an explicit, idempotent `POST /api/review-tasks/{reviewId}/cancel` command.
Cancellation is intentionally not implicit in Start. It may cancel a Scheduled,
Due, or Started Review Task after learner confirmation. Scheduled and Due work
is marked Cancelled. For Started work, one transaction abandons the open Review
Attempt, cancels the Review Task, and commits a terminal Flow transition. In all
cases it accepts no Evidence, changes neither Current Milestone nor Highest
Milestone Reached, keeps the audit trail, and releases the active-work claim.
Replaying cancellation, including with a new key after the task is terminal,
returns its committed terminal state without another effect.
