---
schema: kiln.skill/v1
id: representation.formal-expression
version: 0.1.0
slot: representation
summary: Render formal-expression tasks unambiguously without imposing answer syntax.

requires_context:
  - answer_representation_contract
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

# Formal Expression

## Purpose

Constrain task rendering so its formal objects and requested answer are
unambiguous, without imposing one keyboard syntax or changing correctness
assessment.

## Operating Contract

Read only the Profile-supplied Answer Representation Contract, Learner Locale,
and Task Blueprint.

The Profile owns learner fields and interaction. The Learner Input Gate owns
parsing and confirmation. Assessment owns correctness. This Bundle only
constrains the Action's rendering of the learner task.

## Procedure

1. Render the task's formal objects with conventional, unambiguous notation
   appropriate to `learner_locale`.
2. Make the requested final answer match the declared representation kind and
   permitted variable set.
3. Respect the contract's accepted input families without requiring ASCII,
   Unicode, or LaTeX-like syntax in learner task text.
4. Preserve the boundary between a learner's raw entry and any later
   learner-confirmed canonical expression; do not mention that internal
   processing to the learner.

## Non-Negotiables

- Do not parse, normalize, repair, or assess the learner answer.
- Do not treat OCR text, parser output, or model inference as learner consent.
- Do not change Profile-owned fields, entry mode, or confirmation rules.
- Do not add mathematical facts, solution steps, or named-method cues.

## Quality Checklist

Before returning, ensure task notation, requested answer form, variables, and
locale match the representation contract; the task remains understandable
without special input syntax; and no wording leaks a solution path.
