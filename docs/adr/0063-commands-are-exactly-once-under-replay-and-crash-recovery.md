---
status: accepted
---

# Commands are exactly-once under replay and crash recovery

The direct durable Apply architecture turns every learner command — starting a Flow, starting a Review, submitting a response — into an idempotent transition identified by the Idempotency-Key convention, and crash recovery is a replay of the last command. Every such command must therefore be exactly-once under replay, including replay across a process crash inside the transition.

Replay rehydrates the durable flow state first. If the outcome was already produced and committed — evidence accepted, successor scheduled, task completed — the command is a duplicate and is ignored or returns the original result. If the process crashed between the two committed halves of a transition, the replay resumes from the saved state, for example by re-evaluating the closed Attempt's saved submission, rather than re-running the whole command. The exactly-once guards on evidence acceptance and the open-attempt claim make the resumed transition idempotent; a committed transition is never executed a second time. A single shared replay primitive (`FlowCommandReplay`) and one submission contract implement this for every flow instead of a per-flow reimplementation.

Generation and verification always precede any durable mutation, and the binding transition itself commits atomically in one transaction: the claim, the Task Package, the Task Attempt, the exposure, the state change, the next interaction, the checkpoint, and the processed command. A failed generation therefore leaves no trace, and a racing or crashing start can never strand a claim, a package, or an attempt. Learner-visible responses are projections of committed durable state: when generation is unavailable the learner receives the neutral message rendered from the real durable state, never a fabricated interaction.
