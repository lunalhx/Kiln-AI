# Findings: Multi-agent collaboration architecture

## Requirements
- Walk through mature collaboration patterns without assuming Supervisor + Workers.
- Preserve five pedagogical responsibilities, composable on-demand Skills, explicit Learning Context, evidence integrity, and Phase 0 feasibility.
- Compare architecture separately from library choice.
- Consider Java/Spring compatibility.

## Existing Kiln-AI constraints
- User interaction is sequential around one Target Concept.
- Five teaching responsibilities are Explain, Retrieve, Apply, Teach-back, and Hint.
- These roles share Learning Context and have no private long-term memory.
- Task Attempt is the evidence boundary; Assessment is logically isolated.
- Independent-test and milestone rules require deterministic enforcement.
- Skill selection should favor cross-subject capability modules and progressive loading.

## Evaluation criteria
1. Pedagogical control: can hard evidence and test invariants remain deterministic?
2. Adaptive judgment: can model reasoning influence teaching without owning state?
3. Topology fit: does the pattern match sequential, direct learner interaction rather than parallel task decomposition?
4. Context and cost: can each call receive only the context and Skills it needs?
5. State and recovery: are attempts, checkpoints, resumability, and replay first-class?
6. Observability: can routes, prompts, Skills, transitions, and evidence be traced and evaluated?
7. Skill composition: can small cross-subject capabilities load progressively with conflict checks?
8. Phase 0 complexity: can the core loop be implemented without premature infrastructure?
9. Java/Spring viability: is there a production-credible implementation path for the current ecosystem?

## Research Findings
- LangGraph models workflows with explicit shared State, Nodes, and Edges. Nodes may contain plain functions, model calls, or full agents; conditional edges support dynamic routing. Its checkpointing persists state after steps and enables resume, human interrupts, replay/time travel, and fault recovery.
- LangGraph custom workflows explicitly target cases that mix deterministic logic with agentic behavior, including branches and loops. This pattern maps closely to Kiln-AI's guarded learning stages and model-backed teaching nodes.
- AutoGen GraphFlow also provides directed graph control with sequential, conditional, parallel, and looping behavior, but its current official documentation labels GraphFlow experimental.
- AutoGen SelectorGroupChat uses a model to choose the next speaker from shared conversation context. This is more suitable for ad-hoc collaborative conversation than for strict evidence transitions.
- Microsoft Semantic Kernel exposes concurrent, sequential, handoff, group-chat, and Magentic orchestration patterns, but current Agent Orchestration is experimental and not available in its Java SDK.
- Google ADK explicitly separates predictable workflow agents (Sequential, Parallel, Loop) from LLM-driven dynamic transfer. It validates the architectural spectrum but still requires a closer check of Java features and state durability.
- CrewAI separates Crews (autonomous collaboration) from Flows (stateful controlled orchestration). Its primary implementation ecosystem is Python, so it is more useful as a pattern reference than an immediate Java dependency.
- LangChain4j's `langchain4j-agentic` module provides deterministic workflows, a shared AgenticScope, a Supervisor, and an emerging goal-oriented middle ground based on agent pre/postconditions. The official tutorial labels the whole module experimental.
- Spring AI documents chaining, routing, orchestrator-workers, parallelization, and evaluator-optimizer as workflow patterns. It supplies model/tool primitives and examples, but the results reviewed so far do not show a mature durable state-graph runtime comparable to LangGraph.
- Initial inference: Kiln-AI's five responsibilities share state, interact serially with the learner, and do not decompose independent parallel subtasks. A stateful workflow with specialized model-backed nodes appears more natural than group chat or generic Supervisor/Workers.
- Official OpenAI developer guidance also distinguishes code-orchestrated bounded stages from model judgment: predictable processing should be explicitly bounded with allowed tools, schemas, stopping limits, and evidence, while fresh model decisions remain direct. Its currently documented built-in multi-agent feature is beta and oriented toward independent parallel workstreams, which is not Kiln-AI's primary topology.
- The official OpenAI developer-domain search did not expose a full Agents SDK orchestration guide suitable for citation under the OpenAI documentation source restrictions; no recommendation will depend on the separate SDK-hosted pages found by general search.
- Spring AI Alibaba Graph is the closest reviewed Java-native analogue to LangGraph. Its official materials describe a low-level `StateGraph` runtime with conditional routing, nested graphs, parallel execution, explicit state, persistence/checkpoints, streaming, human-in-the-loop, time travel, and fault recovery. The higher-level Agent Framework adds sequential, parallel, routing, and loop agents.
- Spring AI Alibaba's current release history is active and its latest listed release is `v1.1.2.2`; recent release notes include fixes to serialization and merge behavior as well as new graph/agent features. This makes it a credible spike candidate, but the rapid feature/fix cadence is also a maturity and upgrade-risk signal. It should not become the domain source of truth without an adapter boundary.
- Google ADK Java defines a session service contract that stores session state and append-only events, with in-memory and Vertex AI implementations in the reviewed API. It is a viable general agent runtime, but it does not offer a clearer fit than a state graph for Kiln-AI's guarded domain transitions.
- The persistence distinction matters: chat/session memory alone records conversation continuity, while Kiln-AI needs a durable, replayable domain transition log for Task Attempts, Assistance Traces, assessment outcomes, and milestone projections.
- Current LangGraph documentation defines State as a shared application snapshot with a schema; nodes receive State and return partial updates, while per-key reducers control how updates are merged. Runtime context is separate and intended for static execution configuration.
- LangGraph checkpointing persists State snapshots by thread at graph steps and supports resume, history, replay, human interrupts, and fault recovery. Its separate Store abstraction supports information shared across threads, reinforcing that not every durable domain record belongs in one graph State.
- LangGraph subgraphs may share selected parent-state keys or use different/private schemas with explicit wrapper mappings. This supports node-specific input/output projections instead of automatically exposing an entire shared state to every model call.
- LangChain's current multi-agent guidance makes context engineering—deciding what each Agent sees—central to multi-agent design. Its context guide distinguishes persistent State, Store, and runtime data from transient per-model context.

## Confirmed shared-context design
- Use the Learning StateGraph as a typed coordination Blackboard, not as a universal prompt or the sole database of record.
- Keep canonical and potentially large records in domain and artifact stores; place compact execution facts, identifiers, validated summaries, plans, and statuses in checkpointed graph State.
- Build a separate immutable Node Context View for each model call. Application code may read selected Blackboard channels without copying every channel into model tokens.
- Restrict writes through typed node results and deterministic reducers. Teaching Nodes must not write evidence, milestones, routing, or other nodes' private fields.
- Prefer references and bounded summaries over full chat history, Source Packs, Skill resources, Task Packages, or model traces in graph State.

## Candidate patterns
- Supervisor with subagents/tools
- Stateful handoffs
- Deterministic/custom state graph with agentic nodes
- Group chat / selector conversation
- Single adaptive agent with Skills
- Event-driven or blackboard coordination

## Architecture comparison

| Pattern | Fit for Kiln-AI | Main advantage | Main mismatch or risk |
|---|---|---|---|
| Supervisor + Workers | Low–medium | Useful when one model decomposes an open-ended goal into independent specialist work | Kiln-AI has a known topology, sequential learner turns, and hard evidence rules; a persistent supervisor adds cost and becomes an unnecessary decision/state bottleneck |
| Handoff network | Medium | Specialists can transfer direct conversational control without a central coordinator | Control and context ownership become distributed; legal transitions, auditability, and replay are harder to reason about |
| Selector/group chat | Low | Useful when specialists debate, critique, or jointly deliberate | The five teaching responsibilities do not need to converse with each other; shared-chat history inflates context and weakens isolation |
| Single adaptive Agent + Skills | Medium | Smallest runtime surface and easiest prototype | Role prompts, task contracts, assessment isolation, and routing behavior become entangled in one large policy; harder to test by pedagogical action |
| Blackboard/event-driven agents | Medium for later scale | Loose coupling and asynchronous reactions suit many independent producers and long-running events | Premature for a serial Phase 0 learning loop; introduces eventual-consistency and event-coordination complexity |
| Guarded adaptive state graph with model-backed nodes | High | Makes shared state, legal transitions, checkpoints, and replay explicit while allowing model judgment only at ambiguous edges | Requires graph/state design and a deliberate framework/adaptor boundary |

## Confirmed topology
- Treat the Learning Flow as the primary runtime, not a society of autonomous agents.
- The runtime owns one durable `LearningState` and moves through an explicit graph of deterministic gates and model-backed teaching/assessment nodes.
- Explain, Retrieve, Apply, Teach-back, and Hint become five reusable `Teaching Node Profiles`. They retain distinct prompts, contracts, metrics, and eligible Skills, but they are not persistent workers and do not message each other.
- A bounded `Pedagogy Agent` consumes sanitized Feedback Facts and legal candidates after Assessment, then returns a validated feedback summary and Pedagogy Plan without assessing, teaching, resolving Skills, or mutating state.
- Skill loading happens inside a selected Teaching Node execution: Action Profile/Skill -> capability resolution -> optional subject extensions -> mechanical load -> model call.
- Assessment, Verification, evidence acceptance, and milestone projection remain separate nodes/reducers with deterministic validation around model output.
- Persist domain events and graph checkpoints separately from raw chat memory so an interrupted Task Attempt can resume and Concept Progress can be rebuilt.

## Framework comparison

| Option | Role in the recommendation | Assessment |
|---|---|---|
| Application-owned Java state graph + Spring AI model/tool clients | Lowest-dependency Phase 0 implementation | Best control over domain semantics and upgrade risk; more checkpoint/runtime plumbing must be built explicitly |
| Spring AI Alibaba Graph behind a local adapter | Closest Java-native framework candidate | Strong capability fit, including graph state and persistence; active and relatively young, so validate serialization, resume, interrupts, observability, and provider neutrality in a spike |
| LangGraph | Architectural reference or separate Python/TS orchestration service | Most mature conceptual fit among reviewed graph frameworks, but a second runtime/service is unjustified for Phase 0 unless the team wants that ecosystem |
| Google ADK Java | General agent/session alternative | Java support and eventful sessions are useful, but its abstraction does not improve the domain fit over an explicit state graph |
| LangChain4j Agentic | Future re-evaluation candidate | Attractive Java APIs, but its official tutorial currently labels the module experimental |
| AutoGen / CrewAI / Semantic Kernel orchestration | Pattern references | Python/.NET ecosystem or experimental/Java gaps make them poor Phase 0 foundations for this repository |

## Why this is not merely renaming the Supervisor
- The Pedagogy Agent is bounded to one typed feedback/planning call after Assessment and receives only legal candidates from domain guards.
- The graph, not the model, is the long-lived coordinator and source of execution truth.
- A teaching node produces one typed result and returns control to the graph; no agent owns or hands off the conversation.
- Deterministic transitions still work when the decision node is absent, mocked, or fails.

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| Use a guarded adaptive Learning StateGraph | Confirmed by the user; it best matches the serial human-learning topology while preserving adaptive decisions, evidence invariants, Skill isolation, checkpoints, and replay. |
| Keep the architecture framework-independent | Domain state and transitions should not be coupled to a young agent framework; a Java framework can sit behind an adapter after a technical spike. |
| Run a narrow Spring AI Alibaba Graph spike before framework adoption | Confirmed by the user; the spike will exercise one vertical learning slice and compare observable behavior with a lightweight application-owned fallback. |
| Treat all five spike gates as mandatory | Confirmed by the user; failure or material workaround in domain isolation, routing, Skill loading, recovery, or observability rejects framework adoption for Phase 0. |
| Use `Awaiting Learner Input` as the unified interaction boundary | Confirmed by the user; a Graph Run may traverse several internal nodes but checkpoints and stops whenever real learner input is required. |
| Put a typed Learner Input Gate before workflow execution | Confirmed by the user; structured request metadata wins, ambiguous free text may use an LLM interpreter, and the Workflow Guard validates legality before any state change. |
| Protect independent evidence at the Clarification Gate | Confirmed by the user; procedural restatement is neutral, substantive help converts the attempt to Practice, and ambiguous help is offered only after an explicit consequence warning. |
| Close exposed tasks when the learner explicitly leaves a flow | Confirmed by the user; the attempt becomes Abandoned without Assessment or evidence, while passive disconnection leaves an awaiting attempt open. |
| Use a learner-confirmed Concept Contract | Confirmed by the user; it contains scope, one Mastery Criterion, and source basis without exposing internal Skill or Rubric configuration. |
| Separate versioned content grounding from teaching method | Confirmed by the user; Concept Source Packs feed bounded, traceable content while Skill Stacks define reusable pedagogy and reasoning methods. |
| Freeze Skill identity while allowing lazy internal resources | Confirmed by the user; the Execution Plan pins Skill IDs and versions, and insufficient capability returns to the graph instead of triggering model-selected Skills. |
| Put an Output Gate before learner delivery | Confirmed by the user; typed public/private output is deterministically validated, may receive one same-plan repair, and is persisted before exposure. |
| Use one bounded Pedagogy Agent plus five pure Teaching Node Profiles | Confirmed by the user; the Agent consumes sanitized Feedback Facts and legal candidates, while Teaching Nodes only execute a validated Pedagogy Plan. |
| Use a typed Learning Blackboard with node-specific Context Views | Confirmed by the user; Graph State coordinates compact artifacts, while Context Builders project least-privilege model inputs and reducers control writes. |
| Defer cross-Flow Learner Memory | Confirmed by the user; Phase 0 has no global Agent memory or learner-profile Store, while required evidence and progress remain domain records. |
| Distinguish durable learning records from Learner Memory | Confirmed by the user; evidence, progress, review schedules, and same-Flow checkpoints persist and can reconstruct context without a global profile. |
| Keep Teaching Node runtime and pedagogical method separate | Confirmed by the user; Profiles are stable execution sandboxes, while one versioned Action Skill plus capability/subject extensions supplies method behavior. |
| Bind Action Skill variants from Strategy Tags | Confirmed by the user; deterministic Manifest rules and unique defaults translate pedagogical method preferences into pinned Skill implementations. |
| Bound Skill Stacks with named Slots and budgets | Confirmed by the user; one Action and at most four extensions are compiled into namespaced sections with deterministic conflicts and no silent truncation. |
| Standardize Hint intensity across subjects | Confirmed by the user; H1 Orient through H5 Reveal preserve stable evidence semantics while Skills specialize the actual content. |
| Define task equivalence structurally | Confirmed by the user; Blueprints preserve Mastery criteria and difficulty, while Fingerprints prevent cosmetic reuse of exposed tasks and solution paths. |
| Verify high-consequence tasks before exposure | Confirmed by the user; Independent and Review packages receive an isolated Task Verifier, while ordinary Practice relies on deterministic gates unless risk triggers verification. |
| Keep Review scheduling outside the Agent runtime | Confirmed by the user; one durable Due item resumes the original Flow and generates a verified task just in time, with success-relative 1/3/7/21 intervals. |
| Use Interaction Contracts at teaching boundaries | Confirmed by the user; Profiles constrain allowed learner events, and the graph—not the Teaching Node—selects the successor after continuation. |
| Reuse Gate mechanics without collapsing domain rules | Confirmed by the user; one Typed Artifact Gate Pipeline supports type-safe Policies, a common repair wrapper, and specialized failure semantics. |
| Put hard cost ceilings on Graph Runs | Confirmed by the user; Ordinary Runs stop at four total calls and High-Consequence Runs at six, with traced Token/cost/latency and no recursive Agent loops. |
| Make the first tracer bullet independent of a specific calculus Concept | Confirmed by the user; the source will be a university calculus textbook, while its edition, input format, and selected Concept remain TBD. |
| Defer source-format choice behind a normalization boundary | Confirmed by the user; Concept Preparation consumes a Normalized Source Document, and the tracer uses a manual internal fixture rather than deciding PDF or Markdown. |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| None | — |

## Resources
- LangGraph Graph API: https://docs.langchain.com/oss/python/langgraph/graph-api
- LangGraph persistence: https://docs.langchain.com/oss/python/langgraph/persistence
- LangGraph custom workflows: https://docs.langchain.com/oss/python/langchain/multi-agent/custom-workflow
- AutoGen GraphFlow: https://microsoft.github.io/autogen/dev/user-guide/agentchat-user-guide/graph-flow.html
- AutoGen SelectorGroupChat: https://microsoft.github.io/autogen/dev/user-guide/agentchat-user-guide/selector-group-chat.html
- Semantic Kernel orchestration architecture: https://learn.microsoft.com/semantic-kernel/frameworks/agent/agent-orchestration/
- Semantic Kernel group chat and Java limitation: https://learn.microsoft.com/semantic-kernel/frameworks/agent/agent-orchestration/group-chat
- Google ADK introduction: https://developers.googleblog.com/agent-development-kit-easy-to-build-multi-agent-applications/
- CrewAI documentation: https://docs.crewai.com/
- LangChain4j agentic tutorial: https://github.com/langchain4j/langchain4j/blob/main/docs/docs/tutorials/agents.md
- Spring AI 1.0 workflow patterns: https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released
- Spring AI Alibaba overview: https://github.com/alibaba/spring-ai-alibaba/blob/main/README.md
- Spring AI Alibaba releases: https://github.com/alibaba/spring-ai-alibaba/releases
- Spring AI Alibaba Graph persistence: https://java2ai.com/en/docs/frameworks/graph-core/core/persistence/
- Spring AI Alibaba durable execution example: https://java2ai.com/docs/frameworks/graph-core/examples/long-time-running-task
- Google ADK Java session service: https://google.github.io/adk-docs/api-reference/java/com/google/adk/sessions/BaseSessionService.html
- LangGraph Graph API and State/reducers: https://docs.langchain.com/oss/python/langgraph/graph-api
- LangGraph persistence and Store distinction: https://docs.langchain.com/oss/python/langgraph/persistence
- LangGraph subgraph state communication: https://docs.langchain.com/oss/python/langgraph/use-subgraphs
- LangChain context engineering: https://docs.langchain.com/oss/python/langchain/context-engineering
- LangChain multi-agent context guidance: https://docs.langchain.com/oss/python/langchain/multi-agent
- OpenAI model guidance on bounded tool orchestration and multi-agent beta: https://developers.openai.com/api/docs/guides/latest-model

## Browser Findings
- Official framework documentation consistently distinguishes predictable workflow control from autonomous/dynamic agent collaboration rather than prescribing one universal pattern.
- LangGraph's explicit state and checkpoint model is the strongest reviewed fit for long-lived learner interactions, but it is not a native Java/Spring library.
- Java-native agent orchestration offerings reviewed so far are either low-level workflow examples (Spring AI) or explicitly experimental (LangChain4j agentic, Semantic Kernel Java lacking orchestration).
- OpenAI's current documented multi-agent beta targets parallelizable workstreams; it does not change the fit analysis for Kiln-AI's serial human-learning loop.
- Spring AI Alibaba Graph materially improves the Java implementation options: it can supply checkpointed graph runtime capabilities without moving orchestration to Python. Because it is newer and changing quickly, the architectural recommendation should remain framework-independent and require a narrow technical spike before adoption.
- LangGraph State is a strong coordination primitive for Kiln-AI, but its documented schema visibility should not be mistaken for an instruction to serialize the full State into every Agent prompt. Per-node context construction and private/subgraph schemas remain necessary.
