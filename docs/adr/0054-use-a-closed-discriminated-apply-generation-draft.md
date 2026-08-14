---
status: accepted
---

# Use a closed discriminated Apply generation draft

The Apply model returns only `apply_generation/v1`, a closed discriminated JSON contract. `task_ready` contains learner task text and the minimum structured private facts: proposed expected expression, Rubric mapping, source trace, and equivalence declaration. `source_gap` contains only a structured reason code and missing requirement IDs. Attempt-purpose outcomes are not model fields: evidence channels are declarative, and Blueprint plus Assessment owns their use. The Profile validates and normalizes the proposed expression, derives the final Task Fingerprint, and creates the final Task Package. Unknown fields, model reasoning, learner events, interaction configuration, final canonical answers, and Task Fingerprints are rejected.
