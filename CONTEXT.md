# Kiln-AI Learning

Kiln-AI turns source material into learning activities that progressively reduce assistance and produce evidence of independently usable capability.

## Language

**Topic**:
A learner-facing container that groups related Concepts but is too broad to assess or track as one capability.
_Avoid_: Concept, mastery unit

**Concept**:
The smallest unit that can be taught, independently assessed, and tracked with one coherent Mastery Criterion.
_Avoid_: Topic, chapter, Learning Objective

**Mastery Criterion**:
The user-readable standard describing what independent performance counts as mastery of a Concept. In Phase 0, each Concept has exactly one Mastery Criterion and it is not a separate stateful entity.
_Avoid_: Teaching Action, task, Skill, Learning Objective

**Concept Contract**:
The short, versioned learner-confirmed view of one Concept before its first Learning Flow: Concept name, included and excluded scope, one Mastery Criterion, and source basis. It is a confirmation snapshot, not another objective or progress entity; Skills, Capability Tags, and structured Rubrics remain internal by default.
_Avoid_: Learning Objective, Skill configuration, full Concept model

**Mastery Rubric**:
The stable, versioned, structured interpretation of a Concept's Mastery Criterion used to keep assessment comparable across different tasks.
_Avoid_: User-facing goal, answer key, Task Rubric

**Task Package**:
The validated output generated for one learner task in a single Teaching Node execution, containing the learner-visible task plus a hidden answer key, Task Rubric, source references, and version metadata.
_Avoid_: Additional assessment call, chat message

**Task Blueprint**:
The structured generation contract for a Task Package, containing Attempt Purpose, Mastery Rubric version and required criteria, difficulty band, allowed representations, source scope, and novelty exclusions. It constrains generation without being learner-visible.
_Avoid_: Task Package, prompt prose, Task Rubric

**Task Fingerprint**:
The structured identity of an exposed task across type, scenario, entities, parameters, representation, reasoning path, answer form, and source combination. It supports novelty checks beyond surface-text similarity.
_Avoid_: Task ID only, embedding similarity only, answer key

**Fresh Equivalent Task**:
A new Task Package that measures the same required Mastery Rubric criteria at a comparable difficulty while avoiding prior exposed task instances, solution paths, examples, and parameter fingerprints.
_Avoid_: Same task with cosmetic wording, harder transfer task, repeated Task Package

**Task Verification**:
The isolated pre-delivery evaluation of a generated Task Package for answer correctness, Mastery Rubric alignment, source support, difficulty alignment, and ambiguity. It is mandatory for Independent Test and Delayed Review tasks and is distinct from evaluating a learner response.
_Avoid_: Assessment, Output Gate, confidence voting

**Task Verifier**:
The bounded model component that performs Task Verification without receiving the generator's hidden reasoning and without rewriting the task, changing the Rubric, selecting Skills, or modifying Learning State. The same model provider may be used under an isolated context.
_Avoid_: Teaching Node Profile, Assessment Agent, automatic task editor

**Task Rubric**:
The task-specific evaluation criteria generated with a Task Package and mapped explicitly to the current Mastery Rubric. It may specialize the stable criteria but cannot introduce unrelated mastery requirements.
_Avoid_: Mastery Rubric, improvised assessment criteria

**Task Attempt**:
The complete, auditable attempt by one learner on one Task Package, from presentation until submission, abandonment, or conversion to practice. It is the atomic boundary for Assessment and Learning Evidence and may contain multiple response and assistance events.
_Avoid_: Chat message, answer revision, Learning Flow

**Attempt Purpose**:
The declared role of a Task Attempt: Diagnostic, Practice, Independent Test, or Review. Purpose determines which evidence rules apply and does not change retroactively to manufacture stronger evidence.
_Avoid_: Teaching Action, Task Rubric

**Abandoned Attempt**:
A closed Task Attempt that the learner explicitly left through pause, exit, or Target Concept switching before a valid submission. Its responses and Assistance Trace remain auditable, but it triggers no Assessment, creates no Learning Evidence, and its exposed Task Package cannot be reused for later evidence.
_Avoid_: Failed attempt, suspended task, negative Learning Evidence

**Solution Revealed Attempt**:
A closed Practice attempt whose complete reasoning or answer was exposed at Hint Level H5. It is not assessed and produces no performance evidence; subsequent checking requires Teach-back or a fresh equivalent task.
_Avoid_: Incorrect submission, Independent attempt, worked example as evidence

**Assistance Trace**:
The append-only record of hints, source exposure, answer requests, and other help used during a Task Attempt. Independent eligibility is determined from the entire trace, including its highest exposed hint level.
_Avoid_: Current hint level only, optional analytics

**Hint Level**:
The cross-subject assistance intensity exposed during a Task Attempt: H0 None, H1 Orient, H2 Cue, H3 Strategy, H4 Scaffold, or H5 Reveal. H1–H5 are assistance; H5 exposes the complete solution and closes the attempt as Solution Revealed.
_Avoid_: Difficulty level, learner ability score, procedural clarification

**Hint Ladder**:
The monotonic disclosure policy that normally advances a Practice attempt to its next unused Hint Level and records every exposed level and artifact. Subject and Capability Skills specialize content without changing level semantics.
_Avoid_: Repeated random hints, subject-specific mastery scale, assessment rubric

**Procedural Clarification**:
A response that only restates interface behavior, response format, symbol notation, or conditions already present in the Task Package without adding knowledge or narrowing the solution. It is recorded but does not by itself disqualify an Independent attempt.
_Avoid_: Concept explanation, method suggestion, new example

**Substantive Clarification**:
A response that explains relevant knowledge, suggests a method, exposes a reasoning step, supplies a new example, or narrows the possible answer. It is assistance and converts an open Independent Test or Review attempt to Practice before the help is delivered.
_Avoid_: Formatting clarification, neutral restatement

**Target Concept**:
The one Concept whose Mastery Criterion a Learning Flow is currently trying to satisfy. Every item of Learning Evidence belongs to exactly one Target Concept.
_Avoid_: Topic, chapter, concept set

**Concept Preparation**:
The pre-flow process that grounds a selected Topic or candidate Concept in knowledge-base sources, narrows it to an assessable Concept when necessary, drafts the Concept Contract, and prepares internal Mastery Rubric, Capability Tags, Supporting Concepts, subject metadata, and source metadata. It does not select the next Teaching Action.
_Avoid_: Learning Flow, Teaching Node execution, user Skill configuration

**Concept Source Pack**:
The versioned, Concept-scoped grounding artifact prepared from a Knowledge Base. It contains core source excerpts and anchors, source-version identities, scope metadata, and retrieval filters used to supply traceable content to Teaching Nodes without copying the whole Knowledge Base into context.
_Avoid_: Skill, model-only summary, full Knowledge Base export

**Source Adapter**:
An ingestion-boundary implementation that converts one external source format, such as PDF or Markdown, into a Normalized Source Document while preserving provenance and extraction warnings. Adapter selection is deferred in the first tracer bullet.
_Avoid_: Concept Preparation, document parser embedded in a Teaching Node, product Knowledge Base

**Normalized Source Document**:
The format-neutral internal contract emitted by a Source Adapter, containing source identity and version, hierarchical sections, text and formula blocks, media references, original-location anchors, and extraction warnings. Downstream Concept Preparation does not depend on the original file type.
_Avoid_: Concept Source Pack, PDF-specific model, learner-facing textbook view

**Source Gap**:
A preparation or execution outcome indicating that available source material is missing, contradictory, or insufficient to support the Concept boundary, Mastery Criterion, task answer, or assessment claim. It blocks unsupported evidence rather than being silently filled from model memory.
_Avoid_: Learner failure, low model confidence, automatic web search

**Concept Progress**:
The current projection of one learner's evidence for one Concept, including Current Milestone, Highest Milestone Reached, current Learning Stage, assistance history, recent results, and timing. It is derived from accepted Learning Evidence rather than treated as the source of truth.
_Avoid_: Single mastery score, event log

**Mastery Milestone**:
The coarse evidence-backed status of a learner's Concept Progress: Unassessed, Learning, Independent, or Durable. Understanding and assisted performance are evidence dimensions, not milestones.
_Avoid_: Understood state, Assisted state, current task status

**Current Milestone**:
The Mastery Milestone currently supported by accepted evidence. It may fall from Independent or Durable to Learning only after a verified no-hint failure on a valid Independent Test or Delayed Review.
_Avoid_: Permanent achievement, latest task result

**Highest Milestone Reached**:
The highest Mastery Milestone the learner has ever reached for a Concept. It never decreases and is displayed as historical achievement, not a claim of current capability.
_Avoid_: Current Milestone

**Supporting Concept**:
A prerequisite or related Concept supplied as context while learning a Target Concept. It receives no state change or Learning Evidence unless it becomes the Target Concept of a separate Learning Flow.
_Avoid_: Secondary target

**Learning Flow**:
A progression through Diagnostic, necessary learning and practice, Independent Test, and later Delayed Review for one Target Concept. The stages constrain the flow, but they do not require every Teaching Action.
_Avoid_: Course, chapter session

**Suspended Learning Flow**:
A resumable Learning Flow that is not the learner's current active interaction. It retains Learning State and Concept Progress but has no open Task Attempt; resuming it creates a fresh Task Package from the preserved state.
_Avoid_: Open background Agent, frozen Task Attempt, abandoned Learning Flow

**Review Task**:
The durable scheduled work item that makes one Concept's next Delayed Review discoverable without running a model or pre-generating a learner task. It progresses through Scheduled, Due, Started, Completed, or Cancelled and references the original Learning Flow.
_Avoid_: Task Package, background Agent, pre-generated review question

**Review Cadence**:
The Phase 0 sequence of 1, 3, 7, and 21 day delays measured from each successful Independent Test or Review completion. A verified Review failure cancels the remaining sequence; a new Independent pass restarts it at one day.
_Avoid_: Fixed calendar dates from first mastery, stacked overdue tasks, Agent memory

**Learning Context**:
The domain-oriented content within a Node Context View, including the permitted Target Concept, source, criterion, progress, attempt, and routing projections required for that execution. Different nodes receive different Learning Context projections from the same Learning State.
_Avoid_: Learning State, complete Learning Blackboard, Skill, Agent memory

**Learning State**:
The durable, application-owned state of one Learning Flow, reconstructed from domain records and graph checkpoints. It contains identifiers and execution facts needed to resume the flow, while accepted Learning Evidence remains the source of truth for Concept Progress.
_Avoid_: Chat history, private Agent memory, Concept Progress

**Learning Blackboard**:
The typed, checkpointed Graph State used to coordinate one Learning Flow. It holds compact execution status, identifiers, validated summaries, plans, legal candidates, and artifact references; it is neither a universal model prompt nor the canonical store for large domain records.
_Avoid_: Full chat transcript, database of record, shared chain of thought

**Learner Memory**:
Cross-Flow information retained for future personalization beyond required learning records, such as user preferences, inferred traits, or Agent-authored summaries. Phase 0 does not implement Learner Memory or a global Learner Context Store.
_Avoid_: Concept Progress, Learning Evidence, current-flow Blackboard

**Node Context View**:
The immutable, least-privilege projection assembled for one deterministic or model-backed node invocation from the Learning Blackboard, domain records, and referenced artifacts. It contains only the information and tools required by that node's contract and Token budget.
_Avoid_: Complete Learning Blackboard, private Agent memory, reusable global prompt

**Context Builder**:
The deterministic component that constructs a Node Context View according to the target node's declared read schema, visibility policy, artifact references, and context budget. It may retrieve approved data but cannot infer pedagogy or mutate state.
_Avoid_: Router, Agent, summarizing hidden reasoning

**State Reducer**:
The deterministic component that validates a node's typed result and applies only its authorized partial updates to the Learning Blackboard. Nodes never receive unrestricted write access to shared state.
_Avoid_: Assessment, model tool with arbitrary database writes, event source

**Artifact Store**:
The versioned storage for large or private execution artifacts such as source passages, Task Packages, Skill resources, raw interaction records, and model traces. The Learning Blackboard normally carries references and bounded summaries rather than copying these artifacts.
_Avoid_: Learning Blackboard, Concept Progress, model context window

**Learning StateGraph**:
The primary Phase 0 coordination runtime. It advances one Learning Flow through explicit deterministic gates and model-backed nodes, owns checkpoints and resumability, and returns control to the graph after every node execution.
_Avoid_: Group chat, persistent Supervisor, autonomous Agent network

**Graph Run**:
One wake-up of the Learning StateGraph after a learner action. It runs internal steps, then checkpoints and stops when it must show the learner something and wait, reach a terminal state, or hit an explicit asynchronous wait. The next learner action starts a new Graph Run. A Graph Run is not a lesson, a Concept, or a Learning Flow.
_Avoid_: One graph node, full Learning Flow, persistent Agent process, one study session

**Graph Run Budget**:
The hard ceiling on how many model-producing nodes one Graph Run may enter, plus traced Tokens, estimated cost, and elapsed time. An Ordinary Run may enter at most three such nodes; a High-Consequence Run may enter at most four. Guard, input, and other deterministic steps do not consume this ceiling. Exhaustion stops that wake-up with a declared safe outcome; it does not end the Learning Flow. The next learner action receives a fresh budget.
_Avoid_: Per-Concept lesson quota, tool-call count, mixed LLM-and-tool counter

**Tool Budget**:
The separate hard ceiling on authorized tool executions during one Graph Run. A tool execution is not a model-producing node entry and does not consume Graph Run Budget. The numeric ceiling is operator configuration. Exhaustion stops that wake-up with a declared safe outcome.
_Avoid_: Model call, node transition, Graph Run Budget, gate repair

**Provider Catalog**:
The operator-owned registry of model providers. Each entry has a protocol, endpoint, and listed models. An OpenAI-compatible vendor is a catalog entry, not new application code. Phase 0 does not use a live public model directory. Scripted test doubles are not catalog providers.
_Avoid_: Models.dev, per-vendor SDK, learner provider list, scripted fake as a provider

**Strong Model**:
The operator-facing Model Binding used by Teaching, Assessment, and Task Verification. It is a `providerId/modelId` from the Provider Catalog.
_Avoid_: ChatClient, learner-selected model, one model for every role

**Small Model**:
The operator-facing Model Binding used by Pedagogy, Input Interpreter, and format repair. It is a `providerId/modelId` from the Provider Catalog and may use a cheaper model than the Strong Model.
_Avoid_: ChatClient, learner-selected model

**Model Binding**:
The assignment of one model-producing responsibility to one `providerId/modelId` from the Provider Catalog. Operators set only the Strong Model and Small Model; each responsibility inherits the matching slot.
_Avoid_: ChatClient, ChatModel, learner-selected model

**Model Profile**:
The operator-owned Strong Model and Small Model copied onto a Learning Flow at start as a resolved snapshot of protocol, endpoint, and model identity, then frozen for that flow's lifetime. Secrets stay in the environment and are not copied. Learners never select or change it. Editing the catalog or defaults affects only new flows.
_Avoid_: User preference, Learner Memory, live catalog lookup after start

**Learner Interaction Boundary**:
The unified pause state reached after Kiln-AI presents learner-visible content that requires a response. The graph persists Learning State and its checkpoint as `Awaiting Learner Input`, releases execution resources, and resumes in a new Graph Run when real learner input arrives.
_Avoid_: Sleeping Agent, simulated learner response, chat message without persisted state

**Learner Input Event**:
The typed, immutable representation of one learner message entering a resumed Graph Run. Phase 0 event kinds are Answer Submitted, Continue Requested, Hint Requested, Clarification Asked, Flow Control Requested, and Unknown Input; the event records the original message and interpretation metadata.
_Avoid_: Raw chat text, Teaching Action, accepted state transition

**Learner Input Gate**:
The graph-entry boundary that converts a structured UI/API action or free-form learner message into a Learner Input Event, then asks the Workflow Guard to validate whether that event is legal in the current Learning State. It cannot assess an answer, select pedagogy, load Skills, or mutate state.
_Avoid_: Router, Pedagogy Agent, Assessment

**Learner Input Interpreter**:
The optional stateless model-backed component used by the Learner Input Gate only when a free-form message cannot be classified from structured request metadata. It returns a typed event candidate and confidence/reason metadata; uncertainty becomes Unknown Input rather than a guessed transition.
_Avoid_: Teaching Node, intent-driven state mutation, general chat Agent

**Clarification Gate**:
The interaction node that classifies a Clarification Asked event as Procedural or Substantive. It may answer a procedural request without loading a Teaching Node Profile; substantive or uncertain requests require an explicit assistance warning and learner consent before routing to Hint or Explain.
_Avoid_: Sixth Teaching Node Profile, Assessment, silent assistance

**Learning Stage**:
A major phase of a Learning Flow: Diagnostic, Learning and Practice, Independent Test, or Delayed Review. A stage may use different Teaching Actions depending on the Mastery Criterion and available Learning Evidence.
_Avoid_: Teaching Action, fixed prompt step

**Diagnostic**:
The initial, brief, no-hint attempt used to discover whether a learner already satisfies or partially satisfies a new Target Concept's Mastery Criterion. It uses a Retrieve or Apply action with diagnostic purpose and may be skipped in favor of direct instruction.
_Avoid_: Diagnose Agent, Diagnose Skill, mandatory exam

**Teaching Action**:
A single optional pedagogical intervention selected in response to the Mastery Criterion and current Learning Evidence, such as Explain, Retrieve, Apply, Teach-back, or Hint. Teaching Actions are tools, not mandatory stages.
_Avoid_: Agent, prompt

**Pedagogy Policy**:
The combined policy for choosing the next pedagogical move from the Mastery Criterion, Concept Progress, and recent Learning Evidence. In Phase 0 it is implemented as graph transitions governed by a deterministic Workflow Guard plus a bounded Pedagogy Agent that produces the next Pedagogy Plan.
_Avoid_: Skill routing, state reduction

**Workflow Guard**:
The deterministic graph layer that protects legal stage transitions, independent-test eligibility, evidence requirements, termination, and review rules. It returns the legal next moves and bypasses model-based pedagogy selection when only one move is valid.
_Avoid_: Teaching Node Profile, Skill Router, LLM planner

**Pedagogy Agent**:
The bounded, non-teaching model role invoked after accepted Assessment and, when several legal actions remain, after an explicit Continue Requested or initial direct-instruction choice. It turns sanitized learning signals and Workflow Guard candidates into a learner-facing feedback summary when applicable and a typed Pedagogy Plan. It cannot reassess the answer, name Skills, execute teaching, call other Agents, loop autonomously, or mutate Learning State.
_Avoid_: Supervisor, sixth Teaching Node Profile, Assessment, Skill Loader

**Feedback Facts**:
The sanitized, structured projection of accepted Assessment and evidence validation that identifies satisfied and missing Rubric criteria and error dimensions without exposing hidden reasoning or answer keys. It is the Pedagogy Agent's assessment input.
_Avoid_: Evidence Candidate, chain of thought, learner-facing teaching response

**Pedagogy Plan**:
The typed output of the Pedagogy Agent containing a concise learner-feedback summary, one legal next Teaching Action, teaching intent, required Capability Tags, preferred Strategy Tags, and reason code. The graph validates it before selecting a Teaching Node Profile.
_Avoid_: Execution Plan, Skill Stack, state mutation

**Teaching Node Profile**:
The stable execution sandbox for one Teaching Action. Phase 0 has exactly five profiles—Explain, Retrieve, Apply, Teach-back, and Hint—defining permitted context, tools, budgets, base envelope, validation, tracing, and state restrictions. A Profile contains no duplicate pedagogical method prompt and is invoked on demand without private state or inter-profile messaging.
_Avoid_: Teaching Agent, Worker Agent, autonomous process, private memory, microservice

**Router**:
The constrained Skill-routing pipeline invoked after the Learning StateGraph selects a Teaching Node Profile. It turns the selected profile, required Capability Tags, and current Learning Context into an Execution Plan; concrete Skill IDs and versions are always produced by the deterministic Skill Resolver.
_Avoid_: Graph router, Pedagogy Agent, Skill Loader, state transition

**Skill**:
A small, versioned module composed with other Skills for one execution. A Skill has one primary responsibility, normally a Teaching Action or a reusable capability; subject-specific Skills remain thin extensions.
_Avoid_: Agent, monolithic subject package, plugin

**Skill Manifest**:
The machine-readable declaration of a Skill's identity, version, eligible Teaching Node Profiles, supported Strategy and Capability Tags, applicability conditions, explicit priority and default status, dependencies, conflicts, resources, tools, and output contributions. The Skill Resolver selects Skills from Manifests without loading their full contents.
_Avoid_: Skill instructions, model-selected tool description

**Skill Resolver**:
The deterministic component that maps a selected Teaching Node Profile and required Capability Tags to registered Skill IDs and versions, validates applicability, dependencies, conflicts, and budgets, and provides a safe fallback when the proposed route is invalid.
_Avoid_: Pedagogy Agent, Skill Loader, free-form LLM choice

**Skill Loader**:
The mechanical component that loads the instructions, examples, schemas, and tool declarations for Skills already selected by the Skill Resolver. It makes no routing decisions.
_Avoid_: Router, LLM planner

**Action Skill**:
The Skill that defines one replaceable pedagogical method, action-specific quality rules, and payload-schema contribution for exactly one Teaching Action. Exactly one Action Skill is loaded for a Teaching Node execution and it cannot change the Profile's permissions, base envelope, or state policy.
_Avoid_: Teaching Node Profile, Subject Skill

**Capability Tag**:
A structured description of reasoning, representation, or task capability required by a Concept's Mastery Criterion, such as conceptual reasoning, graphical reasoning, causal analysis, source analysis, or symbolic verification. Tags may cross subject boundaries.
_Avoid_: Subject, Skill ID, user learning goal

**Strategy Tag**:
A registry-controlled description of a preferred pedagogical method, such as contrastive explanation, worked example, analogy, or Socratic prompt. The Pedagogy Agent may request Strategy Tags but cannot name the Skill implementations that provide them.
_Avoid_: Skill ID, Capability Tag, free-form prompt fragment

**Capability Skill**:
A reusable Skill selected primarily through Capability Tags that supplies a reasoning method, representation strategy, task technique, or verification capability across one or more subjects.
_Avoid_: Subject package, owning the Teaching Action contract

**Subject Skill**:
A small Skill that supplies subject terminology, conventions, examples, domain constraints, or tools without overriding the selected Action Skill's behavior or output contract.
_Avoid_: Course package, Action Skill

**Skill Stack**:
The ordered, version-pinned set of compatible Skills bound before one Teaching Node execution: exactly one Action Skill, at most one Skill in each supported Capability Slot, and at most one Subject Skill. Its membership cannot change during that execution or exceed its instruction and lazy-resource budgets.
_Avoid_: Mega-skill

**Skill Slot**:
A named composition position that limits one Skill Stack to at most one compatible Skill for a responsibility. Phase 0 slots are action, reasoning, representation, verification, and subject; action is required and the other four are optional.
_Avoid_: Skill priority, Capability Tag, arbitrary prompt section

**Prompt Compiler**:
The deterministic component that assembles Profile constraints, one Action Skill, occupied Capability and Subject Slots, approved Skill Resources, and Node Context View into namespaced model instructions. It rejects conflicts and budget overflow rather than relying on prompt order or silent truncation.
_Avoid_: LLM Router, raw string concatenation, context summarizer

**Skill Resource**:
A declared example, reference, schema, or tool description owned by a Skill already present in the frozen Skill Stack. It may be loaded lazily during execution and is traced, but loading it does not add or replace a Skill.
_Avoid_: New Skill, unregistered context, Concept source

**Capability Gap**:
A structured pre-execution or execution outcome indicating that the selected Teaching Node cannot satisfy a required capability under registered Skill dependencies, conflicts, source availability, tools, or budget. It returns control to the graph for fallback or replanning without silently changing the Skill Stack.
_Avoid_: Model improvisation, automatic Skill installation, learner failure

**Teaching Result Envelope**:
The typed output of one Teaching Node execution, with strictly separated learner-visible content, private artifacts, source trace, and action-specific structured fields. It is not delivered or allowed to advance state until it passes the Output Gate.
_Avoid_: Raw model stream, chat message, accepted Task Package

**Interaction Contract**:
The validated Teaching Result Envelope field declaring the learner-response event kinds permitted at the next Learner Interaction Boundary. Its allowed shapes are fixed by the Teaching Node Profile; it cannot select another Teaching Action or graph successor.
_Avoid_: Pedagogy Plan, free-form button list, workflow edge

**Output Gate**:
The Teaching Result-specific application of the Typed Artifact Gate Pipeline. Its Gate Policy validates schema, visibility separation, source identities, Rubric mapping, Action Skill contract, Interaction Contract, and declared tool checks before atomic persistence and exposure.
_Avoid_: Assessment, second Router, LLM Judge

**Typed Artifact Gate Pipeline**:
The reusable deterministic validation pipeline for model-produced typed artifacts. It performs common envelope and metadata checks, delegates domain rules to a type-specific Gate Policy, and returns Passed, Repairable, or Rejected without directly calling a model or mutating the Blackboard.
_Avoid_: One universal domain validator, model critic, State Reducer

**Gate Policy**:
The type-safe rules and final failure semantics for one artifact type, such as Teaching Result Envelope, Pedagogy Plan, Evidence Candidate, or Learner Input Event. Policies share pipeline infrastructure but do not collapse their domain invariants into artifact-type conditionals.
_Avoid_: Prompt, fallback table shared by all artifacts, model instruction

**Validated Node Executor**:
The common execution wrapper that calls a model-producing node, submits its artifact to the Gate Pipeline, and may perform the single allowed repair using structured violations before applying that artifact type's fallback or failure result.
_Avoid_: Gate Policy, graph coordinator, autonomous retry loop

**Node Execution Failed**:
The terminal result of a Teaching Node execution whose envelope still fails the Output Gate after its single allowed repair. The failure is traced but creates no Task Attempt, exposes no partial output, and does not advance Learning State.
_Avoid_: Learner failure, incorrect answer, automatic re-routing

**Execution Plan**:
The immutable, versioned Router output specifying the Teaching Node Profile, frozen Skill Stack, tools, retrieval policy, output schema, Rubric, budgets, and routing reasons for one execution.
_Avoid_: Learning Flow, prompt

**Retrieval Policy**:
The Execution Plan component that defines which Concept Source Pack, filters, query, and context budget a Teaching Node may use. Retrieved source identities and versions are recorded with the resulting output or Task Package.
_Avoid_: Skill selection, hidden model browsing, Knowledge Base

**Assessment**:
An evaluation node isolated from the Teaching Node execution that judges learner performance against the Task Rubric and produces an Evidence Candidate. It may use the same model provider but not the teaching execution's hidden reasoning.
_Avoid_: Self-grading Teaching Node, state transition

**Evidence Candidate**:
The structured result proposed by Assessment or Verification before deterministic validation. It is not yet accepted Learning Evidence and cannot change Concept state.
_Avoid_: Learning Evidence, model verdict as state

**Verification**:
A conditional second evaluation of the same performance, invoked for consequential state changes, ambiguous results, or internal inconsistencies. Conflicting evaluations produce an inconclusive outcome rather than an averaged score.
_Avoid_: Mandatory voting, confidence averaging

**Learning Evidence**:
A structured, auditable observation accepted after deterministic validation of one completed Task Attempt for exactly one Target Concept, including the result, complete Assistance Trace, and relevant task context.
_Avoid_: Chat message, raw model judgment
