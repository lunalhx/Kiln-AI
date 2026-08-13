---
status: accepted
---

# Use stable mastery and per-task rubrics

Each confirmed Concept will have a stable, versioned Mastery Rubric derived from its user-readable Mastery Criterion. Every generated learner task will belong to a Task Package whose hidden answer key and Task Rubric are produced in the same Teaching Node execution as the visible task, so no extra model call is required. The Task Rubric must map its criteria to the current Mastery Rubric and cannot introduce unrelated requirements. Assessment and Learning Evidence retain both rubric versions, keeping judgments comparable across task variants and explainable after Skills evolve.
