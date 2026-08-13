---
status: accepted
---

# Reuse a Typed Artifact Gate Pipeline with specialized Policies

Model-produced structured outputs will share one deterministic Typed Artifact Gate Pipeline for parsing, schema and required-field validation, common visibility and metadata checks, normalized violations, Passed/Repairable/Rejected results, tracing, and the prohibition on Blackboard mutation before acceptance. Each artifact retains a type-safe Gate Policy for its domain invariants: Teaching Result validates sources, Rubrics, visibility, action payload, and Interaction Contract; Pedagogy Plan validates legal actions, registered Tags, feedback provenance, and absence of Skill IDs; Evidence Candidate and Learner Input Event retain their own evidence and legality rules. A common Validated Node Executor, rather than the Gate itself, owns the maximum one model repair using structured violations. Final behavior remains type-specific: repeated Teaching Result failure becomes Node Execution Failed, invalid Pedagogy Plan uses deterministic feedback and routing fallback, inconclusive evidence is not accepted, and unknown input requests clarification. Implementations must use policy polymorphism rather than a central artifact-type conditional, and graph frameworks may not merge raw model output into State before this pipeline and an authorized State Reducer succeed.
