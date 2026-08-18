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
The validated `task_package/v1` output generated for one learner task in a single Teaching Node execution. Its learner projection contains only locale-rendered task text, permitted answer fields, allowed events, and the submission rule; its private assessor projection retains canonical expected-answer facts, Task Rubric mapping, source trace, equivalence declaration, Profile-derived Task Fingerprint, and execution trace. It contains no model chain-of-thought or reusable worked-solution prose. The private projection is never exposed to the learner and is supplied to later nodes only under an explicit Node Context View policy.
_Avoid_: Additional assessment call, chat message

**Task Blueprint**:
The frozen, versioned structured generation contract for a Task Package. For Apply it declares Attempt Purpose, Concept and Mastery Criterion references, approved source passages, one task shape and mathematical scope, notation and answer-representation contracts, Profile-owned response fields, an assessment-policy reference, and novelty policy. Diagnostic and Independent Test use distinct Blueprints rather than distinct Profiles or Skills. It constrains generation without being learner-visible.
_Avoid_: Task Package, prompt prose, Task Rubric

**Task Fingerprint**:
The Profile-derived structured identity of an exposed task across type, scenario, entities, parameters, representation, reasoning path, answer form, and source combination. It supports novelty checks beyond surface-text similarity; the generating model is never its final authority.
_Avoid_: Task ID only, embedding similarity only, answer key

**Fresh Equivalent Task**:
A new Task Package that measures the same required Mastery Rubric criteria at a comparable difficulty while avoiding prior exposed task instances, solution paths, examples, and parameter fingerprints.
_Avoid_: Same task with cosmetic wording, harder transfer task, repeated Task Package

**Task Verification**:
The isolated pre-delivery evaluation of a generated Task Package for answer correctness, Mastery Rubric alignment, source support, difficulty alignment, and ambiguity. It is mandatory for every Phase 0 formal Apply task, including Diagnostic and Independent Test, and is distinct from evaluating a learner response.
_Avoid_: Assessment, Output Gate, confidence voting

**Task Verifier**:
The bounded model component that performs Task Verification without receiving the generator's hidden reasoning and without rewriting the task, changing the Rubric, selecting Skills, or modifying Learning State. It returns only the closed `task_verification/v1` verdict (`pass`, `reject`, or `inconclusive`), check results, and reason codes; uncertainty never passes. The same model provider may be used under an isolated context.
_Avoid_: Teaching Node Profile, Assessment Agent, automatic task editor

**Task Generation Exhausted**:
The internal technical-recovery outcome reached after the maximum number of complete formal-task generation, Output Gate, and Task Verification cycles fail before learner exposure. It creates no Task Attempt or evidence and returns control to the Graph; it is distinct from a Source Gap, which ends generation immediately because approved material is insufficient. Apply and Teach-back may reach this outcome, while non-task Explain and Hint failures remain Node Execution Failed.
_Avoid_: Learner failure, partially repaired task, evidence loss

**Profile Contract Test**:
A deterministic end-to-end test of one Teaching Node Profile using scripted model-response fixtures. It validates the compiled Profile contract, typed gates, state transitions, visibility boundary, and evidence behavior without live-model variability. Every Profile requires one before it is considered implemented.
_Avoid_: Prompt snapshot only, live-model CI test, unit test of one Bundle

**Profile Live Smoke Test**:
An isolated, non-blocking integration check that runs a Profile's real compiled prompt against an operator-configured model in ephemeral storage. It detects Provider or prompt-compatibility regressions but is never the stable regression oracle.
_Avoid_: Required CI test, evidence-producing learning run, deterministic evaluation

**Task Rubric**:
The task-specific evaluation criteria generated with a Task Package and mapped explicitly to the current Mastery Rubric. It may specialize the stable criteria but cannot introduce unrelated mastery requirements.
_Avoid_: Mastery Rubric, improvised assessment criteria

**Task Attempt**:
The complete, auditable attempt by one learner on one Task Package, from presentation until submission, abandonment, or conversion to practice. It is the atomic boundary for Assessment and Learning Evidence and may contain multiple response and assistance events. Its raw responses, confirmations, assessment records, and assistance records are retained in the Artifact Store; later nodes receive them only through an explicitly permitted Node Context View projection.
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

**Source Original**:
The immutable, versioned external material supplied to Kiln-AI, such as a PDF, Markdown file, or captured web page, retained with its identity, media type, content hash, and provenance. It is the authoritative record of what was supplied, not a model context or retrieval index entry.
_Avoid_: Converted Markdown, vector record, Concept Source Pack

**Curated Source**:
A Source Original registered by the operator for system use under an understood provenance and usage basis. Phase 0 accepts only Curated Sources; learner-uploaded, private, and shared materials require a later source-ownership and access-control model.
_Avoid_: Learner upload, anonymous web result, user-owned library

**Source Ingestion**:
The pre-learning process that registers a Source Original, runs its format-specific Source Adapter, validates and stores the resulting Normalized Source Document, and records extraction warnings. It prepares source material for Concept Preparation but does not decide a Concept boundary or teach a learner.
_Avoid_: Concept Preparation, Teaching Node, RAG query

**Normalized Source Document**:
The versioned, structured canonical representation emitted by a Source Adapter, containing source identity and version, hierarchical sections, text and formula blocks, media references, original-location anchors, and extraction warnings. Downstream Concept Preparation does not depend on the original file type, and every element remains traceable to the Source Original.
_Avoid_: Concept Source Pack, PDF-specific model, learner-facing textbook view

**Source Passage**:
A bounded, immutable selection of one or more Normalized Source Document blocks supplied to Concept Preparation or a Node Context View, retaining the original document version and anchors.
_Avoid_: Whole textbook context, untraceable text chunk, model summary

**Retrieval Index**:
A rebuildable search projection derived from Normalized Source Documents, such as vector or keyword indexes, that can locate candidate Source Passages but is never the authoritative source of content or provenance.
_Avoid_: Knowledge Base of record, Concept Source Pack, source citation

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

**Active Learning Work**:
The one durable unit that prevents a learner from starting another Flow for the same Target Concept: either a non-terminal Learning Flow or an unfinished Review Task belonging to its terminal Flow. Phase 0 permits at most one Active Learning Work item per learner and Target Concept.
_Avoid_: Browser tab, local activeFlowId, parallel diagnostics

**Suspended Learning Flow**:
A resumable Learning Flow that is not the learner's current active interaction. It retains Learning State and Concept Progress but has no open Task Attempt; resuming it creates a fresh Task Package from the preserved state.
_Avoid_: Open background Agent, frozen Task Attempt, abandoned Learning Flow

**Review Task**:
The durable scheduled work item that makes one Concept's next Delayed Review discoverable without running a model or pre-generating a learner task. It progresses through Scheduled, Due, Started, Completed, or Cancelled and references the original Learning Flow. An explicit learner-confirmed cancellation may cancel any unfinished Review Task; cancelling Started work abandons its open Attempt atomically and produces no evidence.
_Avoid_: Task Package, background Agent, pre-generated review question

**Review Cadence**:
The Phase 0 sequence of 1, 3, 7, and 21 day delays measured from each successful Independent Test or Review completion. A verified Review failure cancels the remaining sequence; a new Independent pass restarts it at one day.
_Avoid_: Fixed calendar dates from first mastery, stacked overdue tasks, Agent memory

**Learning Context**:
The domain-oriented content within a Node Context View, including the permitted Target Concept, source, criterion, progress, attempt, and routing projections required for that execution. Different nodes receive different Learning Context projections from the same Learning State.
_Avoid_: Learning State, complete Learning Blackboard, Skill, Agent memory

**Learner Locale**:
The explicit locale in a Node Context View that controls all learner-visible task text, field labels, and flow messages. It is distinct from the internal English language of first-party Profile and Skill instructions and from the language of a Source Original; rendering a task in a Learner Locale never changes source provenance.
_Avoid_: Source language, Bundle authoring language, inferred model preference

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
The versioned storage for large or private execution artifacts such as source passages, Task Packages, Skill resources, raw interaction records, validated model artifacts, and model invocation metadata. It does not retain model chain-of-thought. The Learning Blackboard normally carries references and bounded summaries rather than copying these artifacts.
_Avoid_: Learning Blackboard, Concept Progress, model context window

**Learning StateGraph**:
The primary Phase 0 coordination runtime. It advances one Learning Flow through explicit deterministic gates and the implemented model-backed Profiles, owns checkpoints and resumability, and returns control to the graph after every node execution.
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
The typed, immutable representation of one learner message entering a resumed Graph Run. Phase 0 event kinds are Answer Submitted, Continue Requested, Hint Requested, Clarification Asked, Assistance Decided, Retry Requested, Flow Control Requested, and Unknown Input; the event records the original message and interpretation metadata.
_Avoid_: Raw chat text, Teaching Action, accepted state transition

**Mathematical Answer**:
The learner answer to a mathematical Task Attempt, retaining its original text, formula-editor structure, or handwritten image together with a learner-confirmed canonical mathematical expression when one can be formed. Assessment compares the confirmed expression under the Task Rubric; it never treats a raw string or an unconfirmed recognition result as the answer of record.
_Avoid_: String equality, OCR text alone, model-inferred final answer

**Answer Representation Contract**:
The Task Package contract that declares an answer's mathematical kind, permitted variables and notation families, entry mode, rendering rules, and canonicalization-confirmation policy. Phase 0 accepts conventional plain text, Unicode mathematical notation, and LaTeX-like expression text for the same formal-expression contract; it does not require a single keyboard syntax. The Contract defines representation only and never parses or assesses a submitted answer.
_Avoid_: Answer key, parser configuration as learner UX, assessment rule

**Answer Confirmation**:
The learner's explicit approval or correction of the rendered canonical expression derived from a Mathematical Answer. Formula-editor submission confirms the expression authored in the editor; parsed text and OCR require confirmation whenever their representation is transformed or uncertain. Without confirmation, an answer is not submitted for Assessment.
_Avoid_: High-confidence OCR auto-submit, parsing success as consent, model correction

**Mathematical Equivalence Check**:
A bounded deterministic evaluation of a confirmed Mathematical Answer against a task's expected mathematical result. It returns Proven Equivalent, Proven Not Equivalent, or Cannot Decide; unsupported or ambiguous expressions must return Cannot Decide rather than a guessed negative result.
_Avoid_: String equality, universal computer algebra claim, model verdict

**Inconclusive Assessment**:
The outcome when required evaluation sources disagree, a required closed evaluation contract remains invalid after its one repair, or a reliable result cannot otherwise be established. It accepts no Learning Evidence and is not recorded as learner failure; a later independent attempt requires a Fresh Equivalent Task.
_Avoid_: Averaged confidence, failed attempt, accepted evidence

**Model Contract Invalid**:
An internal result in which a model response violates its closed contract, including a missing or wrong schema, missing required field, invalid enum, invalid collection shape, null required value, or unknown field. It is distinct from Provider Unavailable and never reaches the learner as raw JSON or a parser error. Each model responsibility follows its declared bounded repair or safe fallback.
_Avoid_: Provider outage, learner input error, accepted model artifact

**Learner Input Gate**:
The graph-entry boundary that converts a structured UI/API action or free-form learner message into a Learner Input Event, then asks the Workflow Guard to validate whether that event is legal in the current Learning State. It cannot assess an answer, select pedagogy, load Skills, or mutate state.
_Avoid_: Router, Pedagogy Agent, Assessment

**Learner Input Interpreter**:
The optional stateless model-backed component used by the Learner Input Gate only when a free-form message cannot be classified from structured request metadata. It returns a typed event candidate and confidence/reason metadata; uncertainty becomes Unknown Input rather than a guessed transition.
_Avoid_: Teaching Node, intent-driven state mutation, general chat Agent

**Clarification Gate**:
The interaction node that classifies a Clarification Asked event as Procedural or Substantive. It may answer a procedural request without loading a Teaching Node Profile; substantive or uncertain requests require an explicit assistance warning and learner consent before routing to Hint or Explain.
_Avoid_: Sixth Teaching Node Profile, Assessment, silent assistance

**Unavailable Interaction**:
A committed, learner-safe Interaction Boundary shown when an operation against an existing Learning Flow cannot safely reach its next interaction. It remains Awaiting Learner Input and permits Retry Requested and Flow Control until its bounded retry chain is exhausted. It never contains source, provider, model-contract, or private-assessment details.
_Avoid_: HTTP error body, learner failure, terminal Flow by default

**Pending Operation**:
The durable, application-owned description of the operation that an Unavailable Interaction can resume. It carries only the identity and committed inputs needed to continue from durable state, such as a saved submission or Review Task, never a client-supplied replacement answer. It is cleared only when a successful next interaction commits or the Flow is explicitly left.
_Avoid_: Client request cache, replaying an arbitrary command body, background job

**Retry Chain**:
The bounded sequence beginning at one Unavailable Interaction. The initial boundary has zero retries; each failed Retry Requested increments its count; a successful next interaction ends the chain. Phase 0 permits three failed retries, then leaves only Flow Control.
_Avoid_: Unlimited automatic retry, a learner-failure count, one Flow-wide counter

**Learning Stage**:
A major phase of a Learning Flow: Diagnostic, Learning and Practice, Independent Test, or Delayed Review. A stage may use different Teaching Actions depending on the Mastery Criterion and available Learning Evidence.
_Avoid_: Teaching Action, fixed prompt step

**Diagnostic**:
The initial, brief, no-hint attempt used to discover whether a learner already satisfies or partially satisfies a new Target Concept's Mastery Criterion. It uses a Retrieve or Apply action with diagnostic purpose and may be skipped in favor of direct instruction. A passing Diagnostic never by itself establishes Independent; it routes the learner to a fresh Independent Test.
_Avoid_: Diagnose Agent, Diagnose Skill, mandatory exam

**Neutral Transition**:
The learner-visible transition from a passing Diagnostic to a fresh Independent Test that states only the next interaction and gives no correctness, solution, rule, or targeted feedback. It prevents diagnostic feedback from becoming assistance before independent evidence is collected.
_Avoid_: Assessment feedback, implicit hint, score reveal

**Teaching Action**:
A single optional pedagogical intervention selected in response to the Mastery Criterion and current Learning Evidence, such as Explain, Retrieve, Apply, Teach-back, or Hint. Teaching Actions are tools, not mandatory stages.
_Avoid_: Agent, prompt

**Explain**:
The Teaching Action that presents a source-grounded account of relevant Concept knowledge without assessing the learner or creating Learning Evidence. Any later understanding check or application is a separate Teach-back or Apply action.
_Avoid_: Teach-back, worked task submission, Assessment

**Hint**:
The Teaching Action that exposes assistance for the current open Apply Practice Task Attempt under the Hint Ladder. It records only assistance actually shown and never assesses the learner or creates Learning Evidence itself.
_Avoid_: General explanation, difficulty adjustment, Assessment

**Teach-back**:
The Teaching Action that asks the learner to explain relevant Concept reasoning against an explicit Rubric in a Practice Task Attempt. Its assessed result may provide understanding evidence but cannot establish Independent.
_Avoid_: Explain, Independent Test, unassessed reflection

**Apply**:
The Teaching Action that delivers one bounded task requiring a learner to use a Target Concept in a declared context. It creates a Task Package and opens the appropriate Task Attempt but does not explain the Concept, reveal a worked solution, assess the response, or create Learning Evidence. Diagnostic and Independent Test uses are distinguished by their Task Blueprints and gates, not by separate Apply Profiles.
_Avoid_: Explain, worked example, Assessment, Task Verifier

**Apply Generation Draft**:
The closed, model-produced `apply_generation/v1` input to the Apply Profile. Its discriminated `outcome` is either `task_ready` (learner task text plus a proposed expected expression, Rubric mapping, source trace, and equivalence declaration) or `source_gap` (a structured reason code and missing requirement IDs). It has no learner events, locale-rendered answer fields, generic private-artifact map, final canonical answer, Task Fingerprint, or model reasoning. The Apply Profile validates and normalizes a valid Draft before turning it into a Task Package.
_Avoid_: Task Package, generic Teaching Result Envelope, model-controlled interaction contract

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
The stable execution sandbox for one Teaching Action. Phase 0's reference path has four implemented Profiles—Explain, Apply, Teach-back, and Hint—defining permitted context, tools, budgets, base envelope, validation, tracing, and state restrictions. Retrieve remains outside this slice. A Profile contains no duplicate pedagogical method prompt and is invoked on demand without private state or inter-profile messaging.
_Avoid_: Teaching Agent, Worker Agent, autonomous process, private memory, microservice

**Router**:
The constrained Skill-routing pipeline invoked after the Learning StateGraph selects a Teaching Node Profile. It turns the selected profile, required Capability Tags, and current Learning Context into an Execution Plan; concrete Skill IDs and versions are always produced by the deterministic Skill Resolver.
_Avoid_: Graph router, Pedagogy Agent, Skill Loader, state transition

**Skill**:
A small, versioned module composed with other Skills for one execution. A Skill has one primary responsibility, normally a Teaching Action or a reusable capability; subject-specific Skills remain thin extensions.
_Avoid_: Agent, monolithic subject package, plugin

**Skill Bundle**:
The first-party directory that packages one Skill's short, always-loaded `SKILL.md` core instructions, machine-readable frontmatter Manifest, declared lazy resources, and evaluation cases. It has a stable semantic identity and immutable SemVer release version; the registry calculates a content hash for the entire release, and a pinned version is never edited. The core contains only the responsibility, operating contract, non-negotiables, and routine procedure needed in every execution; rare edge cases, long examples, background rationale, and evaluation fixtures are resources. The Execution Plan deterministically activates only the runtime resources whose declared conditions are met, while evaluation fixtures are never runtime-loadable. The registry reads only frontmatter before selection; the Loader reads instructions and selected resources only after the Execution Plan freezes that Skill version. External Skill files are research input, not executable Bundles.
_Avoid_: Hard-coded registry entry, model-discovered prompt, runtime external plugin

**Skill Manifest**:
The machine-readable declaration of a Skill Bundle's schema version, stable identity, immutable release version, occupied Slot, summary, eligible Teaching Node Profiles, minimum context requirements, approved output contributions, explicit tool permissions, and declared lazy resources. In a Profile call, only the Action Slot may contribute model-draft fields; Capability and Subject Bundles declare an empty contribution list and constrain that Action's generation. The Apply reference's `kiln.skill/v1` uses a fixed Profile composition and does not declare dependencies, conflicts, priority, or defaults; future Profile routing remains outside this reference's scope. It does not duplicate or redefine the Profile's complete input schema, base envelope, permissions, or state policy. The registry reads Manifests without loading their full contents.
_Avoid_: Skill instructions, model-selected tool description

**Skill Resolver**:
The deterministic fixed-stack binding used by the reference Profiles. It validates the selected immutable Skill Bundles and budgets before execution; dynamic Skill routing and model-selected Skill IDs remain outside this slice.
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
The deterministic component that assembles Profile constraints, one Action Skill, occupied Capability and Subject Slots, approved Skill Resources, and a response contract into namespaced system instructions. It passes the Node Context View separately as structured execution data, rejects conflicts and budget overflow before the model call, and never relies on prompt order or silent truncation.
_Avoid_: LLM Router, raw string concatenation, context summarizer

**Skill Resource**:
A declared example, reference, schema, or tool description owned by a Skill already present in the frozen Skill Stack. Its explicit activation conditions are deterministically evaluated from the frozen Execution Plan, Profile, Blueprint, and validated Context View; every runtime-loaded resource is traced, but the model cannot select an additional resource. Evaluation fixtures are not runtime resources. Loading a resource does not add or replace a Skill.
_Avoid_: New Skill, unregistered context, Concept source

**Capability Gap**:
A structured pre-execution or execution outcome indicating that the selected Teaching Node cannot satisfy a required capability under registered Skill dependencies, conflicts, source availability, tools, or budget. It returns control to the graph for fallback or replanning without silently changing the Skill Stack.
_Avoid_: Model improvisation, automatic Skill installation, learner failure

**Teaching Result Envelope**:
The Profile-assembled typed output of one Teaching Node execution, with strictly separated learner-visible content, private artifacts, source trace, and action-specific structured fields. It is not a raw model response, contains no model reasoning, and is not delivered or allowed to advance state until it passes the Output Gate.
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
The internal result of a Teaching Node execution whose envelope still fails the Output Gate after its single allowed repair. The failure is traced but creates no Task Attempt, exposes no partial output, and does not advance the intended Learning State transition; an existing Flow reaches an Unavailable Interaction for bounded learner-controlled retry.
_Avoid_: Learner failure, incorrect answer, automatic re-routing

**Execution Plan**:
The immutable, versioned Router output specifying the Teaching Node Profile, frozen Skill Stack, tools, retrieval policy, output schema, Rubric, budgets, and routing reasons for one execution.
_Avoid_: Learning Flow, prompt

**Retrieval Policy**:
The Execution Plan component that defines which Concept Source Pack, filters, query, and context budget a Teaching Node may use. Retrieved source identities and versions are recorded with the resulting output or Task Package.
_Avoid_: Skill selection, hidden model browsing, Knowledge Base

**Assessment**:
An evaluation node isolated from the Teaching Node execution that judges learner performance against the Task Rubric and produces an Evidence Candidate. For the Apply reference it separates a final-expression channel from a rationale channel, obeys proof-bounded deterministic mathematical results, and uses a closed model judgment only where semantic evaluation or deterministic `Cannot Decide` requires it. It may use the same model provider but not the teaching execution's hidden reasoning.
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
