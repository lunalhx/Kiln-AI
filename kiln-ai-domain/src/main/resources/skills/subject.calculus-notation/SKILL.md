---
schema: kiln.skill/v1
id: subject.calculus-notation
version: 0.1.0
slot: subject
summary: Apply the declared derivative function-prime notation for the current fixture.

requires_context:
  - learner_locale
  - task_blueprint

output_contribution: []

permissions:
  tools: []

compatibility:
  profiles:
    - apply
  response_draft: apply_generation/v1

resources: []
---

# Calculus Notation

## Purpose

Apply the current fixture's declared calculus notation convention so its task is
unambiguous. This Bundle supplies notation only; it supplies no differentiation
facts, solution method, or teaching strategy.

## Operating Contract

Read only the Task Blueprint and Learner Locale. Apply this Bundle only when
the Blueprint declares the function-prime derivative convention.

The Action Skill owns task text. The Representation Bundle owns general answer
rendering constraints. This Bundle only constrains the calculus notation used
in that text.

## Procedure

1. State the given function using `f(x) = ...`.
2. Request its derivative as `f'(x)`.
3. Keep the requested learner answer to the resulting derivative expression in
   the declared variable.
4. Render surrounding learner-visible prose in `learner_locale`.

## Non-Negotiables

- Do not mix `f'(x)` with `dy/dx`, `d/dx`, or dot notation in one task.
- Do not state a differentiation rule, method, worked step, or answer.
- Do not introduce a calculus topic outside the Task Blueprint.
- Do not change Profile-owned answer fields or representation rules.

## Quality Checklist

Before returning, ensure function notation, derivative request, answer
variable, and locale are mutually consistent; no alternate derivative notation
appears; and the task contains no instructional cue.
