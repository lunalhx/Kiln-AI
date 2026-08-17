---
schema: kiln.skill/v1
id: subject.calculus-notation
version: 1.0.0
slot: subject
summary: Apply the declared calculus notation convention in teaching content.

requires_context:
  - learner_locale
  - concept_contract

output_contribution: []

permissions:
  tools: []

compatibility:
  profiles:
    - explain
    - hint
    - teach_back
  response_draft: null

resources: []
---

# Calculus Notation

## Purpose

Apply the current fixture's declared calculus notation convention so its
teaching content is unambiguous. This Bundle supplies notation only; it
supplies no differentiation facts, solution method, or teaching strategy.

## Operating Contract

Read only the Concept Contract and Learner Locale. Apply this Bundle only
when the Concept Contract declares the function-prime derivative convention.

The Action Skill owns teaching content. This Bundle only constrains the
calculus notation used in that content.

## Procedure

1. State any function under discussion using `f(x) = ...` or the anchor's own
   declared function.
2. Refer to its derivative as `f'(x)`.
3. Render surrounding learner-visible prose in `learner_locale`.

## Non-Negotiables

- Do not mix `f'(x)` with `dy/dx`, `d/dx`, or dot notation in one piece of
  teaching content.
- Do not state a differentiation rule, method, worked step, or answer beyond
  the teaching content the Action Skill owns.
- Do not introduce a calculus topic outside the Concept Contract.
- Do not change Profile-owned answer fields or representation rules.

## Quality Checklist

Before returning, ensure function notation, derivative reference, variable,
and locale are mutually consistent; no alternate derivative notation appears;
and the teaching content carries no instructional cue beyond the Action
Skill's own contract.
