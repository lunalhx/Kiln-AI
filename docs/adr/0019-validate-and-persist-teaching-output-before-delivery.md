---
status: accepted
---

# Validate and persist Teaching Node output before delivery

Every Teaching Node model call will return a complete typed Teaching Result Envelope that separates learner-visible content from private artifacts such as answer keys and Task Rubrics and includes its source trace and action-specific fields. Before delivery, the Teaching Result Gate Policy in the shared pipeline defined by ADR-0031 validates the schema, public/private separation, real source IDs and versions, Task-to-Mastery Rubric mapping, Action Skill contract, and any declared deterministic tool checks. A failed envelope may receive at most one repair call under the same frozen Execution Plan, using the validation errors without re-routing or changing Skills. A second failure becomes Node Execution Failed: no partial output is exposed, no Task Attempt is created or advanced, Learning State does not progress, and the trace is retained for a safe retry. A valid envelope and its checkpoint are persisted atomically before learner-visible delivery. Phase 0 therefore will not stream unchecked raw model tokens or add a routine LLM Judge to every teaching output.
