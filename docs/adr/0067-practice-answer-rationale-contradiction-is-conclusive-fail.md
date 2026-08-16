---
status: accepted
---

# Treat the Practice answer-rationale contradiction as a conclusive failure

ADR-0061 scopes the `Blocked` outcome — the learner's final answer is correct but the substantive rationale is clearly contradictory — to conclusive failure "in Delayed Review only" and keeps Independent Test at no-Evidence. The Learning/Practice reference gives Apply Practice its own final-derivative assessment policy (`practice.final-derivative@1`) but does not prescribe its `Blocked` semantics. This implementation therefore extends the ADR-0061 mapping to Apply Practice: a `Blocked` outcome accepts exactly one assisted Practice FAIL Evidence record and delivers a fresh verified Practice replacement, because the practice policy retains only conclusive pass-or-fail outcomes as assisted application Evidence, and a clearly contradictory rationale over a correct final answer is a conclusive signal of misunderstanding, never evaluative uncertainty. Inconclusive — evaluative disagreement or unresolved channels — remains the only no-Evidence outcome besides the deterministic replace path. The mapping lives in `PracticeSubmissionFlow`, exactly as the Review mapping lives in `ReviewSubmissionFlow`; `ResponseAssessmentDecider` keeps `Blocked` as one closed outcome shared by Independent, Review, and Practice.

The Practice transition also does not yet supply the sanitized Feedback Facts that the Learning/Practice spec attributes to a conclusive Practice fail, because no guarded decision consumes them until the Workflow Guard and Pedagogy Agent arrive (ticket 06); the flow carries only the neutral replacement message today.
