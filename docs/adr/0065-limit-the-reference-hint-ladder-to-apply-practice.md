---
status: accepted
---

# Limit the reference Hint Ladder to Apply Practice

ADR-0030 described Teach-back as normally permitting Hint Requested, but the Learning and Practice reference gives Teach-back no canonical expected explanation: it semantically assesses a learner's own explanation of an already exposed Explain or H5 artifact. Extending H1-H5 to that interaction would require a second conceptual Hint contract, a model-verified H5 exemplar, and different failure semantics, while also risking that Teach-back measures reproduction of supplied wording rather than understanding.

The reference Hint Profile therefore serves only an open Apply Practice Task Attempt, whose private canonical expected answer permits deterministic validation of the complete H5 solution. A Teach-back Interaction Contract permits Answer Submitted, Clarification Asked, and Flow Control Requested, but not Hint Requested. Supporting hints inside Teach-back or another Teaching Action requires a later explicit contract and verification decision; the runtime must not silently reuse the Apply Hint Ladder.
