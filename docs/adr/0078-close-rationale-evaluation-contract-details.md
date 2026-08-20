---
status: accepted
---

# Close the V1 rationale-evaluation contract details

The V1 `rationale_evaluation/v1` result uses one closed, deterministic
`reason_codes` wire enum. Its values are, in canonical serialization order,
`missing_support`, `misapplication`, `factual_error`, `material_gap`, and
`contradiction`. Codes are deduplicated. An `applicable` result must contain no
codes, a `not_applicable` result must contain at least one code, and an
`inconclusive` result must contain no codes because uncertainty is not a
learner-deficit reason. A missing rationale does not produce this result; the
outer Diagnostic policy handles it as `not_provided` without an evaluator
call.

The private V1 Rationale Evaluation Context uses a closed discriminated
`expected_answer_facts` record. The only permitted discriminator is
`kind: canonical_expression`; its required fields are `expression`,
`variables`, and `domain`, matching the task-owned canonical expected-answer
projection already used by the Trusted Primary-Answer Check. No open map,
additional fields, or generic checker-reference is allowed. The context does
not include the check identity or result, the learner's primary answer, either
evaluator's result, reason codes, prior feedback, or routing purpose. Future
answer-fact variants require a new decision rather than an implicit extension
of this contract.

This closes the two normative details required before implementing the
diagnostic-rationale-corroboration tickets. It does not add a new learner
field, public command, evaluator capability, or later-ticket behavior.
