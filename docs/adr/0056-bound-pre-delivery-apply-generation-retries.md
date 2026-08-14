---
status: accepted
---

# Bound pre-delivery Apply generation retries

For a Phase 0 formal Apply task, Source Gap ends generation immediately. Any invalid Output Gate result, Task Verification rejection, or inconclusive Task Verification discards the unexposed candidate and permits exactly one fresh generation cycle using the same frozen plan. The Task Verifier cannot repair the candidate; the runtime does not patch it or average confidence. If the second candidate fails, the Profile returns internal Task Generation Exhausted to the Graph without opening a Task Attempt, exposing learner content, or creating evidence.
