---
schema: kiln.skill/v1
id: verification.structured-task-contract
version: 0.1.0
slot: verification
summary: Require complete private task facts for later validation and assessment.

requires_context:
  - answer_representation_contract
  - concept_source_pack
  - mastery_rubric
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

# Structured Task Contract

## Purpose

Constrain task generation so the Action supplies complete, structured private
facts required by the Output Gate, isolated Task Verification, and later
Assessment.

## Operating Contract

Read only the Task Blueprint, Mastery Rubric, Answer Representation Contract,
and approved Concept Source Pack.

The Action Skill owns all draft fields. The Output Gate validates draft shape,
the Task Verifier validates the task, and Assessment judges learner input.
This Bundle performs none of those operations.

## Procedure

1. Require a task-level Rubric mapping for every required Mastery Rubric
   criterion.
2. Require one unambiguous canonical expected-answer fact and a declared
   equivalence domain compatible with the Answer Representation Contract.
3. Require a source trace containing only approved source identities and
   anchors sufficient to ground every required criterion.
4. Require controlled task facts sufficient for the Profile to derive a final
   Task Fingerprint and check novelty exclusions.
5. Require an equivalence declaration precise enough for the deterministic
   mathematical checker to select its supported check.
6. Direct the Action to return Source Gap when any required fact cannot be
   grounded or made unambiguous.

## Non-Negotiables

- Do not expose a private assessor field in learner-visible text.
- Do not create a worked solution, model reasoning, or teaching explanation.
- Do not judge learner input or claim a task has passed verification.
- Do not weaken, omit, or invent a Rubric criterion.
- Do not write an `ApplyGenerationDraft` field.

## Quality Checklist

Before returning, ensure every required criterion is mapped, expected-answer
and equivalence facts are complete, source trace is grounded and exact,
Fingerprint derivation inputs are internally consistent, and the
public/private boundary is preserved.
