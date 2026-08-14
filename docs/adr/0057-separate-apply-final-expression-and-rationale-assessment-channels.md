---
status: accepted
---

# Separate Apply final-expression and rationale assessment channels

The Apply reference evaluates final-expression correctness and rationale semantics as separate channels. A deterministic mathematical result is never overridden; only `Cannot Decide` invokes isolated Assessment and independent Response Verification, both of which must return equivalent for the final-expression channel to pass. Diagnostic passes if its final-expression channel passes or a substantive rationale is applicable. Independent Test requires a passing final-expression channel and permits omitted or non-substantive rationale unless a substantive rationale is clearly contradictory. A necessary unresolved channel is Inconclusive Assessment, never learner failure or evidence. Assessment models return only the closed `response_assessment/v1` judgment and reason codes, with no feedback, evidence, or reasoning.
