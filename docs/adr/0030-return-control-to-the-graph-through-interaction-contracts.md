---
status: accepted
---

# Return control to the graph through Interaction Contracts

> Clarification (ticket 06): the Apply reference ships this contract in the
> learner projection of every `task_package/v1` — a closed `allowed_events`
> list (`answer_submitted`, `procedural_clarification`, `flow_control`) and a
> one-formal-submission rule. The Profile owns these boundaries; a Bundle or
> model cannot add a learner event. There is no graph successor to select.

Every Teaching Result Envelope will contain a Profile-constrained Interaction Contract declaring the learner event kinds accepted at the next Learner Interaction Boundary, without selecting a successor Teaching Node. Explain permits Continue Requested, procedural Clarification Asked, and Flow Control Requested; Retrieve, Apply, and Teach-back open a Task Attempt and permit only the events declared by their purpose-specific contract; Hint keeps the current Practice attempt open for an answer, further Hint, procedural clarification where allowed, or flow control. Diagnostic and Teach-back reject substantive or uncertain clarification without exposing teaching content or changing Attempt purpose. An Unavailable Interaction permits Retry Requested and Flow Control until its bounded retry chain is exhausted. The Output Gate rejects an event option not declared by the Profile. When Continue Requested resumes the graph, the Workflow Guard computes legal Teaching Actions; one candidate proceeds deterministically, while several candidates invoke the Pedagogy Agent with recent Teaching Trace and the continuation signal but no invented Assessment. Viewing content or continuing creates no Learning Evidence. This preserves pure Teaching Node execution while allowing the graph and bounded Pedagogy Agent to adapt the next move.
