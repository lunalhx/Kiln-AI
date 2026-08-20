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
- [ADR-0042](0042-confirm-independent-evidence-on-a-fresh-post-diagnostic-task.md): fresh independent confirmation after Diagnostic
- [ADR-0043](0043-use-a-neutral-transition-from-diagnostic-to-independent-test.md): neutral transition prevents diagnostic-feedback leakage

## Orchestration and interaction

- [ADR-0011](0011-use-a-guarded-adaptive-learning-state-graph.md): superseded — the Apply reference runs a direct durable flow without a graph runtime
- [ADR-0034](0034-use-spring-ai-alibaba-graph-as-the-phase-0-runtime.md): superseded — the graph runtime was removed in the destructive Apply cutover
- [ADR-0012](0012-pause-the-graph-at-learner-interaction-boundaries.md): clarification — interaction-boundary pausing is preserved through durable Apply interactions
- [ADR-0013](0013-normalize-learner-input-before-workflow-execution.md): typed learner input
- [ADR-0014](0014-protect-independent-evidence-from-substantive-clarification.md): clarification and evidence eligibility
- [ADR-0015](0015-abandon-open-attempts-when-leaving-a-learning-flow.md): leaving a Flow closes exposed attempts
- [ADR-0044](0044-use-learner-confirmed-canonical-mathematical-answers.md): confirmed canonical mathematical answers
- [ADR-0020](0020-use-a-bounded-pedagogy-agent-for-feedback-and-next-action.md): bounded Pedagogy Agent
- [ADR-0021](0021-coordinate-through-a-typed-blackboard-and-node-specific-context-views.md): clarification — the Apply reference replaces the Blackboard with closed execution context and durable typed stores; the minimal typed Blackboard of ADR-0072 covers this clarification for the multi-Profile Learning StateGraph
- [ADR-0022](0022-defer-cross-flow-learner-memory-in-phase-0.md): no cross-Flow Learner Memory
- [ADR-0030](0030-return-control-to-the-graph-through-interaction-contracts.md): clarification — the learner projection carries the allowed-events contract
- [ADR-0032](0032-bound-model-calls-per-graph-run.md): superseded by ADR-0036, which is itself superseded
- [ADR-0036](0036-separate-graph-run-node-budget-from-tool-budget.md): superseded — the Apply reference has no graph run or tool budget
- [ADR-0035](0035-keep-model-selection-operator-owned-and-freeze-it-on-each-flow.md): clarification — the Apply reference resolves the operator catalog per call
- [ADR-0037](0037-use-an-opencode-style-operator-provider-catalog.md): OpenCode-style Provider Catalog, Strong/Small Model slots
- [ADR-0038](0038-use-spring-ai-per-call-tools-from-the-authorized-set.md): superseded — the Apply stack is zero-tool
- [ADR-0039](0039-connect-every-existing-model-port-through-one-spring-ai-adapter.md): superseded — the spike ports were removed; the Apply ports use `ApplyModelAdapter`
- [ADR-0064](0064-restore-the-learning-state-graph-for-multi-profile-learning.md): restore the application-owned graph boundary for multi-Profile Learning and Practice
- [ADR-0065](0065-limit-the-reference-hint-ladder-to-apply-practice.md): the reference Hint Ladder serves Apply Practice, not Teach-back
- [ADR-0069](0069-retry-durable-unavailable-interactions.md): bounded retry of durable unavailable interactions
- [ADR-0070](0070-limit-active-learning-work-and-cancel-review-cadence-explicitly.md): one active Flow/Review cadence and explicit cancellation
- [ADR-0068](0068-cancel-started-review-when-leave-abandons-its-attempt.md): superseded by ADR-0073 — Started Review cancellation goes only through the independent cancel resource
- [ADR-0072](0072-realize-the-minimal-typed-blackboard-for-the-learning-stategraph.md): the durable Flow store and rehydrated Learning State are the minimal typed Blackboard
- [ADR-0073](0073-started-review-cancellation-only-through-the-independent-cancel-resource.md): Started Review cancellation only through the independent cancel resource

## Sources and Concept preparation

- [ADR-0016](0016-confirm-a-concept-contract-before-the-first-learning-flow.md): learner-confirmed Concept Contract
- [ADR-0017](0017-separate-concept-sources-from-teaching-skills.md): Concept Source Packs are distinct from Skills
- [ADR-0033](0033-normalize-source-formats-before-concept-preparation.md): format-neutral source normalization
- [ADR-0040](0040-keep-source-truth-outside-retrieval-indexes.md): source truth outside rebuildable retrieval indexes
- [ADR-0041](0041-restrict-phase-0-sources-to-operator-curated-material.md): operator-curated source boundary

## Skill architecture

- [ADR-0002](0002-compose-small-skills-with-action-owned-pedagogy.md): small composable Skills
- [ADR-0010](0010-route-primarily-by-capability-not-subject.md): capability-first routing
- [ADR-0018](0018-freeze-the-skill-stack-for-each-teaching-node-execution.md): immutable per-execution Skill Stack
- [ADR-0023](0023-separate-teaching-node-runtime-profiles-from-action-skills.md): Profile versus Action Skill boundary
- [ADR-0024](0024-select-action-skill-variants-deterministically-from-strategy-tags.md): Strategy Tag binding
- [ADR-0025](0025-compose-skill-stacks-through-bounded-slots.md): bounded Skill Slots and Prompt Compiler
- [ADR-0046](0046-keep-apply-task-first-and-separate-from-explanation.md): task-first Apply boundary
- [ADR-0047](0047-package-first-party-skills-as-lazily-loaded-bundles.md): first-party lazy Skill Bundles
- [ADR-0048](0048-use-immutable-semver-bundle-identities.md): immutable SemVer Bundle identities
- [ADR-0049](0049-activate-skill-resources-deterministically.md): deterministic lazy resource activation
- [ADR-0050](0050-separate-internal-bundle-language-from-learner-locale.md): internal instruction language versus learner locale
- [ADR-0051](0051-separate-system-instructions-from-execution-data.md): system instructions separated from execution data
- [ADR-0052](0052-assemble-apply-task-packages-from-typed-generation-drafts.md): typed Apply generation drafts
- [ADR-0053](0053-give-action-skills-exclusive-model-draft-field-ownership.md): Action Skill owns model-draft fields
- [ADR-0054](0054-use-a-closed-discriminated-apply-generation-draft.md): closed discriminated Apply draft
- [ADR-0055](0055-use-versioned-purpose-specific-apply-task-blueprints.md): versioned purpose-specific Apply Blueprints
- [ADR-0056](0056-bound-pre-delivery-apply-generation-retries.md): bounded pre-delivery Apply retries
- [ADR-0057](0057-separate-apply-final-expression-and-rationale-assessment-channels.md): Apply assessment channels
- [ADR-0058](0058-assemble-versioned-two-projection-apply-task-packages.md): two-projection Apply Task Packages
- [ADR-0059](0059-separate-profile-contract-tests-from-live-model-smoke-tests.md): deterministic Profile tests versus live smoke tests
- [ADR-0060](0060-use-one-neutral-message-for-unavailable-apply-tasks.md): neutral unavailable-task message
- [ADR-0061](0061-review-answer-rationale-contradiction-is-conclusive-fail.md): Review answer-rationale contradiction is a conclusive failure
- [ADR-0071](0071-fail-closed-on-model-contract-errors-with-type-specific-recovery.md): strict model contracts and safe recovery
- [ADR-0074](0074-request-json-object-on-every-model-call.md): every model call requests a JSON object; generation is warmer than judgment
- [ADR-0075](0075-compose-rationale-verification-through-an-evaluation-skill-stack.md): corroborated Diagnostic rationale rescue through separate Evaluation Skill Stacks
- [ADR-0076](0076-treat-post-submission-model-contract-failure-as-unavailable.md): post-submission model-contract failure resumes through a durable unavailable interaction
- [ADR-0077](0077-checkpoint-post-submission-evaluation-results.md): checkpoint role-keyed evaluation results between model calls and state transitions
- [ADR-0078](0078-close-rationale-evaluation-contract-details.md): close V1 rationale-evaluation reason codes and expected-answer facts

## Task generation, validation, and review

- [ADR-0019](0019-validate-and-persist-teaching-output-before-delivery.md): validate output before delivery
- [ADR-0026](0026-use-a-cross-subject-five-level-hint-ladder.md): five-level Hint Ladder
- [ADR-0027](0027-generate-fresh-equivalent-tasks-from-blueprints-and-fingerprints.md): fresh equivalent tasks
- [ADR-0028](0028-verify-independent-and-review-tasks-before-delivery.md): pre-delivery Task Verification
- [ADR-0029](0029-schedule-review-work-without-running-the-learning-graph.md): non-Agent review scheduling
- [ADR-0062](0062-represent-delayed-review-as-durable-review-tasks.md): durable Review Tasks with a fixed evidence cadence
- [ADR-0063](0063-commands-are-exactly-once-under-replay-and-crash-recovery.md): exactly-once replay and crash recovery for durable flow commands
- [ADR-0031](0031-reuse-a-typed-artifact-gate-pipeline-with-specialized-policies.md): shared typed validation pipeline
- [ADR-0045](0045-combine-proof-bounded-math-checks-with-isolated-model-assessment.md): proof-bounded math checks with isolated model assessment
