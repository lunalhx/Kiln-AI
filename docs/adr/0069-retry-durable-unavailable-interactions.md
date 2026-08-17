---
status: accepted
---

# Retry durable unavailable interactions through a bounded pending operation

An unavailable result is a learner Interaction Boundary, not a terminal Flow
outcome. When an operation against an already durable Learning Flow cannot
complete because a model provider is unavailable, a required node cannot
produce a valid artifact, or a replacement task cannot be prepared, Kiln-AI
commits an `unavailable` interaction with `AWAITING_LEARNER_INPUT`. It persists
only the operation needed to resume safely, its retry-chain count, and any
already committed Attempt or saved submission it must use. It does not persist
a failed generated artifact, a new Attempt, exposure, Assessment, Evidence, or
cadence transition.

`retry_requested` is a closed Learning Flow command that is legal only on this
interaction. It carries the current interaction version and a new
Idempotency-Key, but no learner answer, prior request body, or client-selected
operation. The graph rehydrates the saved pending operation and resumes it from
durable state. A closed submitted Attempt is evaluated from its saved
submission, never from a retry payload. A successful retry commits the next
interaction and clears the pending operation; its original command is never
run again.

Each continuous unavailable chain permits at most three failed
`retry_requested` commands. The initial unavailable boundary has retry count
zero. A failed retry increments that chain; a successful next interaction ends
it. After the third failed retry, the Flow remains at the same recoverable
unavailable boundary but no longer advertises retry. Flow Control remains
available so the learner can explicitly leave. The limit bounds repeated model
cost without turning infrastructure failure into learner failure.

The initial Start command is different. It resolves the Model Profile and
generates, gates, and verifies the first Diagnostic before any durable mutation.
If that work fails, it returns a generic 503 and creates no Flow, Source Pack,
Task Package, Attempt, interaction, checkpoint, command, exposure, or audit
artifact. The client retries that uncommitted Start using its original
Idempotency-Key. `retry_requested` never creates a Flow that initial Start did
not bind atomically.
