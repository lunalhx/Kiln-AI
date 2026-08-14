---
status: accepted
---

# Activate Skill Resources deterministically

Lazy runtime resources are selected by explicit activation conditions evaluated from the frozen Execution Plan, Profile, Task Blueprint, and validated Context View, not by a model deciding that a reference “looks useful.” Each selected resource ID and Bundle version is traced with the execution; evaluation fixtures are never runtime-loadable. This gives progressive disclosure without non-reproducible context expansion or covert model routing.
