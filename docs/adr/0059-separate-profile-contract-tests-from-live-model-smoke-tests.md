---
status: accepted
---

# Separate Profile Contract Tests from live-model smoke tests

Every implemented Teaching Node Profile requires a deterministic end-to-end Profile Contract Test driven by scripted model-response fixtures. It validates the compiled prompt contract, typed artifacts, gate behavior, state transitions, visibility boundaries, and evidence policy without live-model variability. A separate ephemeral, non-blocking Live Smoke Test may invoke an operator-configured model to detect provider or prompt compatibility regressions, but it is never the stable CI oracle and creates no learning evidence. Apply is the first reference; the remaining four Profiles must adopt the same test layers when implemented.
