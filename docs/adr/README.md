# Architecture Decision Records

This directory is the current accepted Phase 0 decision baseline. During pre-implementation design cleanup, rejected and superseded drafts were removed and the accepted records were renumbered consecutively. Future decisions should receive the next number and keep existing accepted IDs stable once implementation begins.

## Learning model and evidence

- [ADR-0001](0001-one-target-concept-per-learning-flow.md): one Target Concept per Learning Flow
- [ADR-0003](0003-use-one-mastery-criterion-per-concept-in-phase-0.md): one Mastery Criterion per Concept
- [ADR-0004](0004-use-stage-constraints-not-a-fixed-action-pipeline.md): stage constraints, optional Teaching Actions
- [ADR-0005](0005-separate-assessment-with-selective-verification.md): isolated Assessment and selective Verification
- [ADR-0006](0006-use-stable-mastery-and-per-task-rubrics.md): stable Mastery Rubric and per-task Rubric
- [ADR-0007](0007-project-coarse-milestones-from-multidimensional-evidence.md): rebuildable coarse milestones
- [ADR-0008](0008-separate-current-and-highest-mastery-milestones.md): current versus historical milestone
- [ADR-0009](0009-use-task-attempts-as-the-evidence-boundary.md): Task Attempt as evidence boundary

## Orchestration and interaction

- [ADR-0011](0011-use-a-guarded-adaptive-learning-state-graph.md): guarded adaptive Learning StateGraph
- [ADR-0012](0012-pause-the-graph-at-learner-interaction-boundaries.md): checkpoint at learner interaction
- [ADR-0013](0013-normalize-learner-input-before-workflow-execution.md): typed learner input
- [ADR-0014](0014-protect-independent-evidence-from-substantive-clarification.md): clarification and evidence eligibility
- [ADR-0015](0015-abandon-open-attempts-when-leaving-a-learning-flow.md): leaving a Flow closes exposed attempts
- [ADR-0020](0020-use-a-bounded-pedagogy-agent-for-feedback-and-next-action.md): bounded Pedagogy Agent
- [ADR-0021](0021-coordinate-through-a-typed-blackboard-and-node-specific-context-views.md): typed Blackboard and projected Context Views
- [ADR-0022](0022-defer-cross-flow-learner-memory-in-phase-0.md): no cross-Flow Learner Memory
- [ADR-0030](0030-return-control-to-the-graph-through-interaction-contracts.md): Interaction Contracts
- [ADR-0032](0032-bound-model-calls-per-graph-run.md): model-call and Token budgets

## Sources and Concept preparation

- [ADR-0016](0016-confirm-a-concept-contract-before-the-first-learning-flow.md): learner-confirmed Concept Contract
- [ADR-0017](0017-separate-concept-sources-from-teaching-skills.md): Concept Source Packs are distinct from Skills
- [ADR-0033](0033-normalize-source-formats-before-concept-preparation.md): format-neutral source normalization

## Skill architecture

- [ADR-0002](0002-compose-small-skills-with-action-owned-pedagogy.md): small composable Skills
- [ADR-0010](0010-route-primarily-by-capability-not-subject.md): capability-first routing
- [ADR-0018](0018-freeze-the-skill-stack-for-each-teaching-node-execution.md): immutable per-execution Skill Stack
- [ADR-0023](0023-separate-teaching-node-runtime-profiles-from-action-skills.md): Profile versus Action Skill boundary
- [ADR-0024](0024-select-action-skill-variants-deterministically-from-strategy-tags.md): Strategy Tag binding
- [ADR-0025](0025-compose-skill-stacks-through-bounded-slots.md): bounded Skill Slots and Prompt Compiler

## Task generation, validation, and review

- [ADR-0019](0019-validate-and-persist-teaching-output-before-delivery.md): validate output before delivery
- [ADR-0026](0026-use-a-cross-subject-five-level-hint-ladder.md): five-level Hint Ladder
- [ADR-0027](0027-generate-fresh-equivalent-tasks-from-blueprints-and-fingerprints.md): fresh equivalent tasks
- [ADR-0028](0028-verify-independent-and-review-tasks-before-delivery.md): pre-delivery Task Verification
- [ADR-0029](0029-schedule-review-work-without-running-the-learning-graph.md): non-Agent review scheduling
- [ADR-0031](0031-reuse-a-typed-artifact-gate-pipeline-with-specialized-policies.md): shared typed validation pipeline
