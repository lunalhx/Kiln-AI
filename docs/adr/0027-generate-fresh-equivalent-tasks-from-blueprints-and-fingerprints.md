---
status: accepted
---

# Generate Fresh Equivalent Tasks from Blueprints and Fingerprints

Every generated Task Package will be constrained by a Task Blueprint that fixes Attempt Purpose, Mastery Rubric version and required criteria, difficulty band, allowed representations, source scope, and novelty exclusions. The Teaching Node produces the learner-visible task, hidden answer, Task Rubric mapping, source trace, and a structured Task Fingerprint in the same model call. Equivalence requires the same required Mastery Rubric criteria and comparable difficulty; freshness requires a materially different task instance across scenario, entities, parameters, representation, reasoning path, answer form, and exposed source combination rather than cosmetic rewording or changed numbers alone. Independent Test and Review Blueprints exclude all Task Packages whose task, hints, partial solution, or H5 answer has previously been exposed. The Output Gate validates complete Rubric mapping, sources, difficulty, Fingerprint novelty, and declared deterministic tool checks, using at most the existing single same-plan repair before failing without exposing or opening the task.
