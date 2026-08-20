---
status: accepted
---

# Treat post-submission model-contract failure as unavailable

Once a learner has formally submitted an Attempt, Assessment, Response Verification, Rationale Sufficiency Verification, and Teach-back Assessment must not turn a malformed model response into learner routing. If any such responsibility still returns Model Contract Invalid after its one same-profile repair, Kiln-AI commits an Unavailable Interaction with a Pending Operation that resumes from the saved submission; it creates no Learning Evidence, replacement task, remediation transition, or learner-failure signal. Provider, timeout, and configuration failures use the same learner-safe boundary while retaining distinct internal error categories, and a valid closed-contract `inconclusive` remains a semantic evaluation outcome rather than a technical failure. Pre-delivery Task Verification still treats an invalid contract as an inconclusive candidate verification under its bounded fresh-candidate policy, and Pedagogy or Clarification keep their declared safe fallbacks. This amends only the post-submission recovery rule of ADR-0071.
