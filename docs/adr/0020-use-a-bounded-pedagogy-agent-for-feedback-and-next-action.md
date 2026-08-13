---
status: accepted
---

# Use a bounded Pedagogy Agent for feedback and next action

Phase 0 will use one non-teaching Pedagogy Agent alongside the five Teaching Node Profiles. After accepted Assessment, deterministic validation produces sanitized Feedback Facts and the Workflow Guard supplies legal next Teaching Actions; the Agent makes one bounded model call to create a concise learner-feedback summary and a typed Pedagogy Plan. It may also plan after Continue Requested or an initial direct-instruction choice when the Guard leaves multiple legal actions. The Plan contains one legal action, teaching intent, required Capability Tags, preferred Strategy Tags, and reason code. The Agent cannot reassess the learner answer, see Assessment hidden reasoning, choose Skill IDs, bypass graph guards, execute a Teaching Action, call another Agent, loop, or mutate Learning State. Its output passes the Typed Artifact Gate Pipeline before the graph invokes Explain, Retrieve, Apply, Teach-back, or Hint. This keeps all five Teaching Node Profiles focused on execution and makes adaptive feedback and planning a separately testable responsibility.
