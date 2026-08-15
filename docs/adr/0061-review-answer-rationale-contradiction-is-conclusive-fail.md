---
status: accepted
---

# Treat the Review answer-rationale contradiction as a conclusive failure

In Delayed Review only, a `Blocked` outcome — the learner's final answer is correct but the substantive rationale is clearly contradictory — is treated as a conclusive no-hint Review FAIL: it accepts exactly one Review FAIL evidence record, completes the started Review Task at the acceptance time, cancels any other unfinished Review work defensively, projects Current Milestone to Learning while preserving Highest Milestone Reached, and stops the cadence with the safe learner notice that the final answer is inconsistent with the given rationale. Independent Test keeps its existing `Blocked` behavior: no Evidence and no milestone change, because the final expression alone cannot be trusted without a coherent rationale. No general failure-code hierarchy is introduced: `Blocked` remains one closed assessment outcome, only the Review submission flow maps it to conclusive failure, and assessment facts and model reason codes stay private in both branches.
