---
status: accepted
---

# Fail closed on model contract errors with type-specific recovery

The model adapter owns transport only. It returns raw model content to the
domain, which strictly parses every closed model contract: required schema and
fields, enum values, array element shapes, nullability, and unknown fields.
The domain normalizes violations before choosing the responsibility-specific
safe outcome. A model contract error is not a provider outage.

Generation Profiles keep their existing one same-plan repair through the Typed
Artifact Gate Pipeline. An invalid Task Verification result is an inconclusive
verification of that candidate, so normal fresh-candidate generation policy
applies. Response Assessment, Response Verification, and Teach-back Assessment
each receive one repair with the same frozen Model Profile, responsibility,
and evaluation context. A responsibility that remains invalid is Inconclusive:
it creates no evidence and follows the existing replacement policy. Pedagogy
and Clarification retain their existing safe fallback behavior. No responsibility
silently accepts malformed content or exposes it to a learner.

`MODEL_CONFIGURATION_INVALID` covers invalid operator catalog or secret setup;
`MODEL_PROVIDER_UNAVAILABLE` covers network, timeout, and upstream failures;
`MODEL_CONTRACT_INVALID` identifies strict contract violations;
`TASK_UNAVAILABLE` is the neutral learner-facing preparation outcome; and
`INTERNAL_ERROR` is an unexpected failure. Configuration and provider failures
on an existing Flow become the durable unavailable boundary of ADR-0069. The
same failures before the first Start binding return a generic 503. Contract
errors never become 503 merely because a model emitted invalid JSON.

Logs and durable audit metadata retain only the Flow or Attempt identity,
responsibility, normalized violation codes, repair count, correlation ID, and
provider-health category. They do not retain raw invalid JSON, prompts, or
learner responses. HTTP errors use generic learner-safe messages and never
contain provider endpoints, model IDs, secret names, parser exceptions, upstream
bodies, UUID internals, or stack traces.
