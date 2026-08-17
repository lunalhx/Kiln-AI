---
status: accepted
---

# Coordinate through a typed Blackboard and node-specific Context Views

> Clarification (ticket 06): the Apply reference realizes the least-privilege
> Context View boundary with a closed `apply_execution_context/v1` JSON object
> and durable typed Apply stores instead of a Blackboard. Assessment keeps an
> isolated context; the Independent invocation receives only the
> Diagnostic-pass fact and exposed Fingerprints. The Blackboard itself returns
> when later multi-node Profiles need cross-node collaboration.
>
> Covered by ADR-0072: the multi-Profile Learning StateGraph now realizes the
> minimal typed Blackboard directly — the durable Flow store and the
> rehydrated `LearningState` snapshot, with node-specific Context Views as
> closed projections.

Each Learning Flow will use a compact, checkpointed Learning Blackboard as its graph coordination State, while canonical domain records and large or private artifacts remain in domain persistence and an Artifact Store. Nodes collaborate by writing validated typed artifacts—such as Feedback Facts, Pedagogy Plan, Execution Plan, and Teaching Result Envelope—to authorized Blackboard channels through deterministic State Reducers, not by messaging each other or sharing hidden reasoning. Before each invocation, a deterministic Context Builder constructs an immutable Node Context View from only the Blackboard fields, domain projections, artifact references, sources, Skills, and tools declared by that node's read contract and context budget. Therefore shared State does not mean shared Prompt: Assessment may access the private Task Package and Assistance Trace; the Pedagogy Agent sees sanitized Feedback Facts, legal actions, and recent teaching trace; a Teaching Node sees the validated plan, relevant learning summary, bounded source passages, and frozen Skill Stack. The Blackboard stores references and compact collaboration state rather than full chat history, Source Packs, Task Packages, Skill files, or model traces. This architecture follows State/Reducer and checkpoint concepts demonstrated by LangGraph but remains framework-independent.
