---
schema: kiln.skill/v1
id: verification.rationale-sufficiency
version: 1.0.0
slot: verification
summary: Verify support, task connection, and coherence of one rationale.

requires_context:
  - task_text
  - rationale
  - task_rubric
  - expected_answer_facts
  - source_passages
  - learner_locale

output_contribution: []

permissions:
  tools: []

compatibility:
  profiles:
    - rationale_evaluation
  response_draft: rationale_evaluation/v1

resources: []
---

# Rationale Sufficiency Verification

## Purpose

Verify one isolated rationale judgment using only the supplied task-owned
facts. The method is reusable across domains; the Task Rubric and approved
Source Passages remain the source of truth.

## Operating Contract

Check Rubric support, connection to the task, and coherence independently.
Mark a dimension `pass` only when the supplied facts support it, `fail` when
a material gap or error is established, and `inconclusive` when the facts do
not establish a reliable result.

## Non-Negotiables

- Do not use keyword presence as proof of sufficiency.
- Do not invent missing facts or return model reasoning.
- Do not inspect a primary answer, a prior evaluator, feedback, or state.
- Do not choose routing or award Evidence.
