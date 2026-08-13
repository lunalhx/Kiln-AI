---
status: accepted
---

# Select Action Skill variants deterministically from Strategy Tags

The Pedagogy Agent will express teaching-method preferences through registry-controlled Strategy Tags and hard requirements through Capability Tags; it cannot emit Skill IDs or versions. For the selected Teaching Action, the deterministic Skill Resolver filters compatible Action Skill Manifests by Profile, required capabilities, subject and source applicability, tools, dependencies, conflicts, and budget, then prefers exact Strategy Tag matches and uses explicit Manifest priority to select one implementation. Every Teaching Action must register exactly one default Action Skill; lack of a preferred strategy match may fall back to that default with a traced `strategy_fallback`, while a missing required capability returns Capability Gap. Overlapping candidates with equal winning priority are rejected as a registry configuration error before execution rather than resolved randomly or by another model call. The chosen version is pinned in the immutable Execution Plan.
