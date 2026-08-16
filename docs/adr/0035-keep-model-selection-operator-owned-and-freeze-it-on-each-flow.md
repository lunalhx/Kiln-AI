---
status: accepted
---

# Keep model selection operator-owned and freeze it on each Learning Flow

Kiln-AI will select concrete providers and models only through an operator-owned Model Profile. Learners never choose or change models, and a later user system will not add that choice. Starting a Learning Flow copies the current profile onto that flow and freezes it for resume, repair, and Assessment; editing the operator default cannot rebind an in-flight flow or fail over to another provider mid-execution. This keeps evidence auditable without treating model choice as Learner Memory.
