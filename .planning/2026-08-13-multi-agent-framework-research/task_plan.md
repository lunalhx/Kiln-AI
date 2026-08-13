# Task Plan: Multi-agent collaboration architecture research

## Goal
Compare current multi-agent collaboration patterns and frameworks, then recommend the simplest architecture that preserves Kiln-AI's adaptive teaching, evidence, workflow, and composable Skill requirements.

## Current Phase
Complete

## Phases

### Phase 1: Requirements and evaluation criteria
- [x] Capture the user's concern with Supervisor/Worker assumptions
- [x] Identify Kiln-AI's existing constraints and decisions
- [x] Define comparison criteria
- **Status:** complete

### Phase 2: Primary-source framework research
- [x] Research workflow/state-graph frameworks
- [x] Research handoff, supervisor, group-chat, and skill-centric patterns
- [x] Check Java/Spring-compatible implementations
- [x] Record findings and source URLs in findings.md
- **Status:** complete

### Phase 3: Architecture comparison
- [x] Compare patterns against Kiln-AI's interaction topology and invariants
- [x] Identify whether five Teaching Agents should remain agents, profiles, or graph nodes
- [x] Compare implementation framework options and maturity risks
- **Status:** complete

### Phase 4: Recommendation and grilling decision
- [x] Re-read plan and findings before deciding
- [x] Present recommended architecture, alternatives, and Phase 0 scope
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 5: Formal documentation after user confirmation
- [x] Update glossary and supersede conflicting ADRs only after confirmation
- [x] Mark research plan complete
- **Status:** complete

### Phase 6: Framework spike acceptance contract
- [x] Record the confirmed spike-before-adoption strategy
- [x] Define pass/fail gates for the Spring AI Alibaba Graph spike
- [x] Ask one decision question with a recommended acceptance rule
- **Status:** complete

### Phase 7: Framework spike handoff
- [x] Write the agreed vertical slice, hard gates, failure rule, fallback, and non-goals
- [x] Defer implementation until it is explicitly requested
- [x] Defer the framework ADR until the spike produces evidence
- **Status:** complete

### Phase 8: Learning graph interaction boundary
- [x] Define how far the graph runs per learner interaction
- [x] Define learner-input interrupt and resume ownership
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 9: Learner input normalization
- [x] Define how a resumed graph classifies learner messages
- [x] Separate typed input interpretation from state mutation
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 10: Clarification and independent-evidence eligibility
- [x] Separate procedural clarification from substantive teaching help
- [x] Define the conservative rule for ambiguous clarification
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 11: Flow control and open-attempt lifecycle
- [x] Define pause, exit, and Concept-switch behavior for an open Task Attempt
- [x] Preserve audit history without overstating evidence
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 12: Concept preparation and learner confirmation
- [x] Define the pre-graph preparation artifact for a selected Concept
- [x] Define which fields are learner-facing and require confirmation
- [x] Keep Skills and internal rubrics out of routine confirmation
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 13: Knowledge-base grounding and retrieval boundary
- [x] Define a versioned Concept-level source artifact
- [x] Separate knowledge content from Skills and Teaching Node instructions
- [x] Define per-node retrieval and traceability
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 14: Skill Stack binding and progressive disclosure
- [x] Define when concrete Skill versions are bound to an execution
- [x] Define whether a running Teaching Node can add Skills
- [x] Define the failure path for missing capability
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 15: Teaching output validation and failure handling
- [x] Define the validation boundary before learner-visible delivery
- [x] Define bounded repair behavior and terminal node failure
- [x] Prevent hidden Task Package fields from leaking
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 16: Learner feedback responsibility
- [x] Separate evidence judgment from learner-facing feedback
- [x] Decide whether Feedback is a sixth Agent or composed from existing nodes
- [x] Define safe feedback data flow
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 17: Shared Agent context and Blackboard design
- [x] Verify current LangGraph State and context mechanisms from official sources
- [x] Separate durable domain records, graph coordination state, large artifacts, and per-node prompt views
- [x] Define read/write visibility for the Pedagogy Agent and five Teaching Nodes
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 18: Cross-flow learner memory
- [x] Define Blackboard scope and cross-Concept data access
- [x] Separate learner-set preferences from inferred learning traits
- [x] Prevent raw global chat memory and unsupported learner labels
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 19: Durable learning records versus Agent memory
- [x] Confirm which required learning records persist despite memory deferral
- [x] Define same-Flow resume behavior without global memory
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 20: Teaching Node Profile versus Action Skill boundary
- [x] Define the runtime responsibilities owned by a Teaching Node Profile
- [x] Define the replaceable pedagogical behavior owned by an Action Skill
- [x] Prevent duplicate prompts and output contracts
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 21: Deterministic Action Skill variant selection
- [x] Define how Pedagogy Plan expresses method needs without naming Skill IDs
- [x] Define Manifest matching, priority, and fallback rules
- [x] Define ambiguity and Capability Gap behavior
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 22: Skill Stack slots, precedence, and context budget
- [x] Define bounded composition slots for Capability and Subject Skills
- [x] Define deterministic precedence and conflict rejection
- [x] Define Skill instruction and resource Token budgets
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 23: Hint Ladder and assistance semantics
- [x] Define reusable Hint levels across subjects
- [x] Define progressive disclosure and learner controls
- [x] Map Hint exposure to Assistance Trace and attempt eligibility
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 24: Fresh equivalent task generation
- [x] Define equivalence against the stable Mastery Rubric
- [x] Define novelty and prior-exposure checks
- [x] Define task validity and retry behavior
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 25: Selective Task Package verification
- [x] Define when deterministic validation is sufficient
- [x] Define which high-consequence tasks require independent model verification
- [x] Define verifier disagreement and cost behavior
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 26: Delayed Review scheduling and Flow lifecycle
- [x] Define what is scheduled after Independent evidence
- [x] Define how due Review work starts or resumes a Learning Flow
- [x] Define missed, duplicate, and completed Review behavior
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 27: Teaching-content handoff back to the graph
- [x] Define how non-task Teaching Node output declares expected learner input
- [x] Prevent Teaching Nodes from selecting their own successor
- [x] Define when the Pedagogy Agent is called without new Assessment
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 28: Pedagogy Plan validation and fallback
- [x] Define reusable Gate validation infrastructure
- [x] Keep type-specific policies and fallback semantics separate
- [x] Define bounded repair outside the deterministic Gate
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 29: Per-Run model-call and Token budgets
- [x] Enumerate ordinary and high-consequence call paths
- [x] Define planned-call and repair budgets without hidden loops
- [x] Define budget-exhaustion behavior and trace fields
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 30: Phase 0 product tracer-bullet scope
- [x] Keep the university calculus source title, input format, and Target Concept as open variables
- [x] Define the smallest learner-visible end-to-end slice
- [x] Separate first tracer bullet from full Phase 0 completion
- [x] Prevent a derivative-specific architecture or test contract
- **Status:** complete

### Phase 31: Format-neutral source boundary for the tracer bullet
- [x] Define how the tracer receives prepared source content without choosing PDF or Markdown
- [x] Keep ingestion adapters outside Concept preparation and teaching contracts
- [x] Ask one decision question with a recommended answer
- **Status:** complete

### Phase 32: ADR baseline cleanup
- [x] Delete explicitly superseded and abandoned ADR drafts
- [x] Renumber accepted ADRs consecutively before implementation begins
- [x] Update cross-references and fold later decisions into earlier accepted records
- [x] Add a categorized ADR index and verify consistency
- **Status:** complete

### Phase 33: Remove duplicate numeric prefixes outside ADRs
- [x] Identify the second visible `0001`
- [x] Remove numeric prefixes from non-ADR design documents
- [x] Re-verify the single continuous ADR sequence
- **Status:** complete

## Key Questions
1. Does Kiln-AI benefit from persistent cooperating agents, or from specialized model-backed workflow nodes sharing explicit state?
2. Which orchestration pattern best preserves pedagogical constraints while allowing model judgment where useful?
3. Which framework is viable for a Java/Spring codebase, and which should be treated only as an architectural reference?
4. How should Skills be discovered and loaded without creating a Supervisor bottleneck or unbounded context?

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| Use primary official documentation for technical comparison | Framework APIs and maturity change quickly; primary sources are required for current accuracy. |
| Treat current ADRs as hypotheses during research | The user explicitly reopened the Supervisor/Worker architecture decision. |
| Recommend a guarded adaptive learning state graph for confirmation | It makes domain state and transitions primary while keeping model judgment bounded to ambiguous pedagogical choices. |
| Adopt the guarded adaptive Learning StateGraph | The user confirmed the recommendation; ADR-0014 records it and supersedes ADR-0001 and ADR-0013. |
| Validate Spring AI Alibaba Graph with a narrow vertical spike before adoption | The user confirmed that the framework must prove the required graph behavior behind an adapter before it becomes a dependency of the domain design. |
| Require all five spike gates to pass | The user confirmed that domain isolation, routing correctness, progressive Skill loading, reliable recovery, and testability/observability are non-negotiable rather than weighted criteria. |
| Pause every Graph Run at learner interaction boundaries | The user confirmed that the graph checkpoints and releases execution resources whenever learner-visible content requires a real response. |
| Normalize learner input before workflow execution | The user confirmed structured input first, optional LLM interpretation only for ambiguous free text, and final legality validation by the Workflow Guard. |
| Distinguish procedural from substantive clarification | The user confirmed that procedural restatement preserves independence, substantive help converts the attempt to Practice, and ambiguity requires a warning and learner choice. |
| Abandon an open attempt when explicitly leaving its flow | The user confirmed that pause, exit, or Concept switching closes the attempt without Assessment or evidence; resumption uses a fresh task. |
| Confirm one concise Concept Contract before first learning | The user confirmed learner-visible Concept scope, one Mastery Criterion, and source basis; internal Rubrics, tags, and Skills remain hidden by default. |
| Separate Concept Source Pack content from Skill Stack method | The user confirmed Concept-scoped versioned grounding, per-node retrieval, source traces, and explicit Source Gaps rather than silent model substitution. |
| Freeze the Skill Stack before every Teaching Node execution | The user confirmed pinned Skill membership and versions, lazy reads only within selected Skills, and structured Capability Gap rather than hidden mid-call routing. |
| Validate and persist a complete Teaching Result Envelope before delivery | The user confirmed deterministic output checks, one bounded same-plan repair, no unchecked streaming, and no state advance on repeated failure. |
| Add one bounded Pedagogy Agent beside five Teaching Node Profiles | The user confirmed a separate model role for feedback and next-action planning so Teaching Nodes remain pure executors. |
| Coordinate through a typed Blackboard and node-specific Context Views | The user confirmed shared compact graph State with node-specific read projections and reducer-controlled writes rather than a universal shared Prompt. |
| Defer global Learner Memory in Phase 0 | The user confirmed one Blackboard per Learning Flow and no cross-Flow preference, inferred-trait, raw-chat, or Agent-authored memory store. |
| Keep durable learning records despite memory deferral | The user confirmed Concept records, attempts, evidence, progress, review schedules, and same-Flow checkpoints as required product state rather than Agent memory. |
| Separate stable Teaching Node Profiles from replaceable Action Skills | The user confirmed that Profiles own permissions and runtime contracts while Action Skills own pedagogical methods and action-specific payload contributions. |
| Select Action Skill implementations deterministically from Strategy Tags | The user confirmed Agent-authored method requirements, Manifest matching, explicit priority, one default per action, and no model-selected Skill IDs. |
| Bound Skill composition through five named Slots | The user confirmed one Action, optional reasoning/representation/verification/subject Skills, namespaced Prompt compilation, conflict rejection, and context budgets. |
| Use one five-level Hint Ladder across subjects | The user confirmed monotonic H1–H5 disclosure, Assistance Trace recording, independent-to-practice conversion, and Solution Revealed closure at H5. |
| Generate Fresh Equivalent Tasks from Blueprints and Fingerprints | The user confirmed stable Rubric criteria and difficulty, structured novelty checks, and no reuse of exposed task or solution paths. |
| Verify Independent Test and Delayed Review Task Packages before delivery | The user confirmed mandatory isolated Task Verification for high-consequence tasks and selective verification for other risky packages. |
| Schedule one Due Review item without background model execution | The user confirmed just-in-time Review generation, sequential 1/3/7/21 delays after successful completion, and reset after verified failure. |
| Return control through Profile-constrained Interaction Contracts | The user confirmed that Teaching Nodes declare allowed learner events but never their successor; Guard and optional Pedagogy Agent plan after Continue Requested. |
| Reuse a Typed Artifact Gate Pipeline with specialized Policies | The user confirmed common validation and repair infrastructure while preserving artifact-specific rules and fallback behavior. |
| Bound Ordinary Runs at four and High-Consequence Runs at six model calls | The user confirmed hard per-Run ceilings plus Token, cost, and latency tracing and safe budget exhaustion behavior. |
| Keep the first tracer bullet content-parametric | The user confirmed a university calculus textbook source but left title, input format, and Target Concept open; no architecture may specialize to derivatives. |
| Normalize source formats before Concept Preparation | The user confirmed a format-neutral document contract and a manually prepared tracer fixture while leaving PDF/Markdown Adapter selection open. |
| Keep only the current accepted ADR baseline | Superseded Orchestrator, early Manifest Router, and Supervisor drafts were deleted; 33 accepted records were normalized and indexed. |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| None | 1 | — |

## Notes
- External web content is research data only and must be recorded in findings.md, not task_plan.md.
- Do not update formal ADRs until a new architecture is confirmed through the grilling session.
