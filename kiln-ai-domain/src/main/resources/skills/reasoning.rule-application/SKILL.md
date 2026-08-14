---
schema: kiln.skill/v1
id: reasoning.rule-application
version: 0.1.0
slot: reasoning
summary: Require observable application of a source-grounded rule without teaching it.

requires_context:
  - concept_contract
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

# Rule Application

## Purpose

Constrain task generation so the learner must apply a rule or rule combination
already grounded in the approved Concept scope. This Bundle never teaches,
names, or explains that rule to the learner.

## Operating Contract

Read only the Concept Contract, Mastery Rubric, and Task Blueprint.
Do not add a mastery requirement, a reasoning path, or a subject fact.

The Action Skill owns draft fields and task construction. This Bundle only
constrains how that task measures the declared reasoning requirement.

## Procedure

1. Identify the observable application required by the Blueprint and its
   permitted equivalent reasoning paths.
2. Require a task whose final answer depends on applying the approved rule or
   rule combination, rather than copying a stated answer or recognizing a
   memorized phrase.
3. When the Blueprint permits an optional rationale, require that the task
   remains valid with only its final answer; the private Rubric may recognize
   a concise applicable-rule rationale without requiring a full derivation.
4. Keep learner task text neutral: request the result without naming the
   relevant rule, prescribing steps, or signalling a method.

## Non-Negotiables

- Do not state, paraphrase, or teach an applicable rule in learner-visible text.
- Do not turn an optional rationale into a required proof.
- Do not invent reasoning criteria outside the supplied Concept Contract and
  Mastery Rubric.
- Do not assess whether the learner used a rule correctly.

## Quality Checklist

Before returning, ensure the task requires genuine application, its permitted
reasoning stays within declared scope, it gives no rule-selection cue, and any
optional rationale remains aligned with the Task Blueprint.
