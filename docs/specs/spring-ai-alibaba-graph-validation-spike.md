# Spec: Validate Spring AI Alibaba Graph through a learner-facing vertical spike

## Problem Statement

Kiln-AI has accepted an application-owned Learning StateGraph, deterministic Workflow Guard, bounded Pedagogy Agent, composable Skill Stack, typed Learning Blackboard, checkpointed learner interaction, and evidence-safety boundaries for Phase 0. It has not decided whether Spring AI Alibaba Graph can host that design without taking ownership of domain semantics, introducing model-based routing where deterministic routing is required, weakening progressive Skill loading, or making recovery and testing unreliable.

The graph runtime must therefore be validated before the first product tracer fixes its runtime implementation. Framework adoption cannot be inferred from a successful happy-path demo: all five accepted gates must pass independently.

## Solution

Build a narrow, disposable Spring AI Alibaba Graph spike behind an application-owned graph port and expose it through a minimal learner-facing HTTP/UI flow. The flow starts from persisted Learning State, executes Explain, reaches a learner interaction boundary, resumes from the saved checkpoint, exposes multiple legal Teaching Actions, lets the bounded Pedagogy Agent select Apply, resolves and progressively loads the frozen Skill Stack, opens a Task Attempt, accepts a learner answer, assesses it, accepts evidence exactly once, and persists the resulting state, checkpoint, and trace.

Spring AI Alibaba Graph is confined to a Graph Adapter. The Workflow Guard, Skill resolution, artifact validation, authorized State Reducers, evidence acceptance, persistence records, and learner-facing contracts remain application-owned. Model-producing nodes return raw typed candidates that must pass the application-owned Typed Artifact Gate Pipeline and an authorized reducer before they can affect checkpointed State.

The spike produces a pass/fail evidence report for the five hard gates: domain isolation, routing correctness, progressive Skill loading, reliable recovery, and testability/observability. All five must pass. A failed gate rejects Spring AI Alibaba Graph for Phase 0; the later tracer runtime must then use the already-decided application-owned lightweight Java transition-engine fallback.

## User Stories

### 1. Complete the spike through the learner-facing interface

**As a** learner exercising the spike  
**I want** to start, pause, resume, and complete one learning interaction through the HTTP/UI interface  
**So that** the framework is evaluated through the same external boundary the first product tracer will use

**Acceptance Criteria:**

- [ ] A minimal learner-facing UI can start the prepared spike flow without calling an internal graph API directly.
- [ ] Learner actions cross the system through the learner-facing HTTP boundary.
- [ ] The UI displays only validated learner-visible content; private answer keys, Rubrics, raw model results, and hidden reasoning are never returned.
- [ ] The flow demonstrates Explain followed by an adaptive Apply path and an assessed learner answer.
- [ ] The end state and execution trace can be inspected after the flow completes.
- [ ] The UI is a validation surface for the spike, not a permanent Phase 0 interface design.

### 2. Start from application-owned persisted Learning State

**As a** graph-runtime evaluator  
**I want** the spike to begin from persisted Learning State through an application-owned contract  
**So that** Spring AI Alibaba Graph does not become the owner of Kiln-AI domain state

**Acceptance Criteria:**

- [ ] The initial Learning State is created or loaded outside the framework-specific graph representation.
- [ ] Framework-specific state is mapped inside the Graph Adapter.
- [ ] Domain types, persistence records, HTTP contracts, Workflow Guard rules, and evidence reducers contain no Spring AI Alibaba Graph types.
- [ ] A dependency or architecture test fails if framework classes leak beyond the Graph Adapter boundary.

### 3. Keep deterministic routing outside model calls

**As a** learning-domain maintainer  
**I want** legal transitions to be computed deterministically  
**So that** a model cannot invent or bypass workflow rules

**Acceptance Criteria:**

- [ ] The Workflow Guard supplies the legal next Teaching Actions for the current state.
- [ ] When only one action is legal, the graph takes it without a routing-model call.
- [ ] The spike includes a state where more than one Teaching Action is legal.
- [ ] In that state, the bounded Pedagogy Agent receives only the legal actions and selects Apply through a typed Pedagogy Plan.
- [ ] An illegal action returned by the Pedagogy Agent is rejected and cannot advance graph State.
- [ ] The Pedagogy Agent does not select Skill IDs, mutate State, execute a Teaching Action, or retain control of the graph.

### 4. Resolve and progressively load a frozen Skill Stack

**As a** Skill-system maintainer  
**I want** the selected Teaching Node to receive only its resolved Skills  
**So that** the spike proves progressive disclosure and reproducible execution

**Acceptance Criteria:**

- [ ] The Pedagogy Plan expresses Capability Tags and preferred Strategy Tags rather than Skill IDs or versions.
- [ ] The deterministic Skill Resolver selects exactly one Action Skill plus the Capability Skill required by the spike.
- [ ] The resulting Execution Plan pins Skill IDs and versions before the Teaching Node model call.
- [ ] The Skill Loader loads only the common constraints and resources declared by the selected frozen Skill Stack.
- [ ] The model cannot search the registry or replace a Skill during execution.
- [ ] The trace records selected Skill IDs, versions, loaded resources, and routing reasons.
- [ ] Missing capability, dependency conflict, unavailable resource, or budget failure returns a structured Capability Gap rather than silently truncating or substituting instructions.

### 5. Pause and resume at real learner interaction boundaries

**As a** learner  
**I want** the system to stop while it waits for my response and resume later  
**So that** learning does not depend on an in-memory Agent process remaining alive

**Acceptance Criteria:**

- [ ] Before learner-visible content requiring a response is delivered, validated output, Learning State, and checkpoint are persisted atomically.
- [ ] The graph enters `Awaiting Learner Input` and stops consuming graph execution resources.
- [ ] A later real learner action begins a new Graph Run from the persisted checkpoint.
- [ ] The spike does not simulate learner input or keep a Teaching Node or Agent alive across the boundary.
- [ ] A restart or replacement of the graph-runtime instance between pause and resume does not lose the open interaction.

### 6. Normalize and guard resumed learner input

**As a** workflow maintainer  
**I want** resumed learner input converted into a typed event before graph execution continues  
**So that** free-form interpretation cannot become an unchecked domain command

**Acceptance Criteria:**

- [ ] Structured UI actions produce typed Learner Input Events deterministically.
- [ ] The submitted spike answer is represented as `Answer Submitted` and is accepted only while a Task Attempt is open.
- [ ] Illegal or uncertain input requests clarification without advancing State.
- [ ] The Learner Input Gate cannot select a Teaching Node, resolve Skills, assess the answer, or mutate Learning State.

### 7. Validate model artifacts before State mutation or delivery

**As a** learning-safety maintainer  
**I want** every model-produced artifact validated by application-owned policies  
**So that** raw framework or model output cannot corrupt graph State or leak private data

**Acceptance Criteria:**

- [ ] Explain, Pedagogy, Apply, and Assessment model nodes return typed candidate artifacts rather than directly mutating State.
- [ ] Candidates pass parsing, schema, visibility, metadata, and artifact-specific validation in the Typed Artifact Gate Pipeline.
- [ ] Only a passed artifact reaches an authorized State Reducer.
- [ ] A rejected artifact leaves checkpointed State unchanged and exposes no partial learner content.
- [ ] A repairable artifact receives at most the one repair allowed by its accepted execution budget and frozen Execution Plan.
- [ ] The framework never merges raw model output into checkpointed State.

### 8. Create and assess one Task Attempt without assistance leakage

**As a** learning-evidence maintainer  
**I want** Apply to open a Task Attempt that is assessed in an isolated context  
**So that** the spike exercises Kiln-AI's real evidence boundary

**Acceptance Criteria:**

- [ ] The validated Apply result creates one Task Attempt and persists its private Task Package separately from learner-visible content.
- [ ] The learner answer resumes the same open Task Attempt.
- [ ] Assessment receives the private Task Package and relevant Assistance Trace but does not receive unrelated Teaching Node hidden reasoning.
- [ ] Assessment produces an evidence candidate; it does not directly advance Concept progress.
- [ ] Deterministic validation accepts eligible evidence and projects the resulting state through an authorized reducer.
- [ ] Replaying the same answer or retrying the same external model request cannot accept the evidence twice.

### 9. Recover safely from interruption and retry

**As an** operator  
**I want** checkpoint recovery and idempotent retries  
**So that** infrastructure failures do not duplicate learning effects

**Acceptance Criteria:**

- [ ] The spike can resume after interruption from the last persisted checkpoint.
- [ ] A retry may repeat an external model request but cannot duplicate accepted evidence, advance state twice, or reuse a completed Task Attempt.
- [ ] Delivery does not occur before the corresponding valid artifact and checkpoint are persisted.
- [ ] Failure and retry information is recorded in the run trace.
- [ ] Recovery does not require framework types in domain idempotency or evidence rules.

### 10. Expose enough trace data to evaluate the framework

**As a** framework evaluator  
**I want** a complete, inspectable run trace  
**So that** each acceptance gate can be judged from evidence rather than from a successful screen demo

**Acceptance Criteria:**

- [ ] The trace includes graph node transitions, candidate routes, selected route, and Workflow Guard decision data.
- [ ] The trace includes selected Skill IDs and versions, loaded Skill resources, and selection reasons.
- [ ] The trace includes model and prompt identifiers, call counts, Token usage, estimated cost, and latency.
- [ ] The trace includes checkpoint identity, Task Attempt identity, artifact validation outcomes, reducer applications, failures, and retries.
- [ ] Private artifacts and hidden reasoning are not exposed through the learner UI or ordinary public trace view.

### 11. Test graph behavior with deterministic model fakes

**As a** developer  
**I want** model-backed nodes replaceable with deterministic fakes  
**So that** graph transitions, validation, recovery, and routing can be reproduced without network or model variance

**Acceptance Criteria:**

- [ ] Automated acceptance tests run the real Graph Adapter with model nodes replaced by deterministic fakes.
- [ ] Tests can force valid, repairable, rejected, illegal-route, interrupted, and retry outcomes.
- [ ] Deterministic fakes do not bypass the same typed artifact gates and reducers used by real model nodes.
- [ ] A test proves that deterministic graph transitions make no routing-model call.

### 12. Produce a binary adoption result

**As a** Phase 0 architecture owner  
**I want** the spike to report each hard gate separately  
**So that** framework convenience cannot compensate for a failed domain guarantee

**Acceptance Criteria:**

- [ ] The final report records pass/fail evidence for domain isolation, routing correctness, progressive Skill loading, reliable recovery, and testability/observability.
- [ ] Gates are not averaged or traded against developer convenience.
- [ ] Any framework-type leak or substantial lifecycle workaround required to pass a gate counts as failure.
- [ ] Failure of any gate rejects Spring AI Alibaba Graph for Phase 0 and records the application-owned Java transition engine as the tracer runtime direction.
- [ ] Passing all five gates makes Spring AI Alibaba Graph eligible for the subsequent tracer runtime decision; implementing the tracer is not part of this spike.

## Implementation Decisions

- Implement only the validation spike. Do not implement the Phase 0 product tracer in this work item.
- Put Spring AI Alibaba Graph behind an application-owned graph port and Graph Adapter. Framework classes must remain in that adapter.
- Keep graph semantics, legal transitions, typed contracts, checkpoints, artifact policies, reducers, Skill selection, evidence acceptance, and persistence records application-owned.
- Use the accepted typed Learning Blackboard as compact checkpointed coordination State. Keep canonical records and large/private artifacts outside it and reference them by identity.
- Construct node-specific immutable Context Views; do not share a universal prompt or full Blackboard with every node.
- Use the deterministic Workflow Guard for legal transitions and invoke the bounded Pedagogy Agent only when the accepted rules leave multiple legal actions.
- Freeze the Skill Stack before a Teaching Node call. For this vertical slice, load one Action Skill and one Capability Skill through the accepted bounded-slot and deterministic resolver design.
- Route every model-produced candidate through the shared Typed Artifact Gate Pipeline and an authorized State Reducer before State mutation.
- Persist validated learner-visible output and its checkpoint before delivery, then stop at the learner interaction boundary.
- Use a prepared, format-neutral internal source fixture. Do not choose a permanent textbook, Target Concept, PDF/Markdown adapter, or ingestion pipeline in this spike.
- Capture the evaluated Spring AI Alibaba Graph version and any required workarounds in the spike report; the version is evidence about the evaluation, not a permanent product dependency decision.
- Reuse current repository code only where it satisfies the accepted boundaries. The existing six-module skeleton, REST DTOs, PostgreSQL/MyBatis stack, and domain classes are not fixed foundations for the spike.

## Testing Decisions

- The primary acceptance seam is one learner-facing HTTP/UI flow that drives the actual Spring AI Alibaba Graph adapter from start through pause, resume, Apply answer submission, Assessment, evidence acceptance, and final trace inspection.
- The primary flow uses deterministic model fakes but does not fake or bypass the graph framework, Workflow Guard, Skill Resolver/Loader, Typed Artifact Gate Pipeline, reducers, checkpoint boundary, or learner HTTP boundary.
- Run the resume portion against a newly constructed graph-runtime instance using the persisted checkpoint so the test demonstrates recovery rather than in-memory continuation.
- Add focused failure-injection scenarios at the same seam for illegal Pedagogy selection, invalid model artifact, interruption/retry, and duplicate evidence submission.
- Use an architecture/dependency test to enforce domain isolation because framework leakage is more directly and reliably detected below the UI seam.
- Use focused unit or component tests only for deterministic rules that are awkward to diagnose through the complete flow, such as reducer idempotency, Skill registry collision, and artifact-policy outcomes.
- A green happy-path UI test is insufficient. Completion requires the evidence matrix for all five hard gates.

## Out of Scope

- Implementing the first Phase 0 product tracer or fixing its permanent graph runtime
- Proving learning efficacy or comparing model teaching quality
- Implementing all five Teaching Node Profiles; the spike exercises the accepted Explain and Apply path only
- Delayed Review, Durable milestone, Retrieve, Teach-back, or the complete Hint workflow
- Multi-Agent messaging, A2A, group chat, autonomous background Agents, or parallel workers
- Choosing a permanent database, model provider, observability vendor, UI framework, textbook, Target Concept, or source adapter
- Automated textbook ingestion, Concept extraction, or decomposition
- Production deployment, horizontal scaling, authentication, or polished learner-interface design
- Optimizing exact Token, cost, or latency ceilings beyond collecting the accepted traces and enforcing model-call budgets
- Implementing the lightweight Java transition-engine fallback inside this spike

## Further Notes

- The spike is an architecture experiment with production-shaped boundaries, not production functionality.
- All five gates must pass; there is no partial adoption score.
- The final spike report must cite observable test or trace evidence for each gate and identify the exact evaluated framework version.
- The subsequent tracer spec will be written only after this spike establishes whether Spring AI Alibaba Graph or the already-decided lightweight Java fallback owns the runtime adapter.
