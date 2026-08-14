---
status: superseded
---

# Connect every existing model port through one Spring AI adapter

> Superseded by the destructive Apply cutover (ticket 06): the spike
> Teaching/Pedagogy/Assessment ports were removed. The shipped Apply ports
> (generation, Task Verification, Assessment, Response Verification) use one
> shared `ApplyModelAdapter` over Spring AI ChatClient and the operator
> Provider Catalog with zero tools.

Teaching, Pedagogy, and Assessment will share one infrastructure adapter over Spring AI ChatClient and the operator Provider Catalog. Tests continue to inject scripted fakes at the domain ports; scripted is not a catalog provider. A Learning Flow fails closed when the catalog, Strong/Small Model, or secret is missing. Phase 0 operator surface is the catalog configuration file, not an admin UI or `/connect` flow. Input Interpreter and Task Verifier are out of this slice but must use the same adapter contract when those nodes exist.
