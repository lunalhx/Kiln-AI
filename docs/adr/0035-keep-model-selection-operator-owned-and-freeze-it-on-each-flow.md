---
status: accepted
---

# Keep model selection operator-owned and freeze it on each Learning Flow

> Clarification (ticket 06): the Apply reference resolves the operator catalog
> per model call (Strong binding) through `ApplyModelAdapter`; it does not yet
> copy a frozen Model Profile onto the durable Apply flow. The operator-owned
> catalog and fail-closed behavior of ADR-0037 stand; freezing per flow
> returns when later Profiles bind a profile to a flow.

Kiln-AI will select concrete providers and models only through an operator-owned Model Profile. Learners never choose or change models, and a later user system will not add that choice. Starting a Learning Flow copies the current profile onto that flow and freezes it for resume, repair, and Assessment; editing the operator default cannot rebind an in-flight flow or fail over to another provider mid-execution. This keeps evidence auditable without treating model choice as Learner Memory.
