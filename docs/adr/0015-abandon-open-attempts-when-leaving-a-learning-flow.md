---
status: accepted
---

# Abandon open attempts when leaving a Learning Flow

When a learner explicitly pauses, exits, or switches the Target Concept while a Task Attempt is open, Kiln-AI will close that attempt as Abandoned before suspending or leaving its Learning Flow. The attempt's responses, Assistance Trace, Task Package identity, and exit reason remain auditable, but it is not assessed, produces no Learning Evidence, and does not change a Mastery Milestone. Resuming the flow creates a fresh task from the preserved Concept Progress; an already exposed Task Package cannot be reopened for independent evidence. A network disconnect, closed browser, or ordinary delay while the graph is already `Awaiting Learner Input` is not an explicit Flow Control Requested event and leaves the current attempt open. This prevents multiple Concepts from accumulating live attempts and keeps deliberate off-task study from being submitted later as evidence on an old task.
