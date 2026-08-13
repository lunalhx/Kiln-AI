---
status: accepted
---

# Freeze the Skill Stack for each Teaching Node execution

Before a Teaching Node model call, the deterministic Skill Resolver will use registered Skill Manifests to produce an immutable Execution Plan with pinned Skill IDs and versions. Each plan contains exactly one Action Skill and at most one Skill in each optional reasoning, representation, verification, and subject Slot; the Skill Loader then loads their core instructions. During execution the model may lazily read declared examples, references, schemas, or tool descriptions belonging to those already selected Skills, and every loaded Skill Resource is traced, but it cannot search the Skill registry, add or replace Skills, or change versions. Missing required capability, dependency conflict, unavailable source or tool, or budget violation returns a structured Capability Gap to the Learning StateGraph for a declared fallback or new plan. This preserves progressive disclosure and Token control without sacrificing reproducibility through hidden mid-call Skill routing.
