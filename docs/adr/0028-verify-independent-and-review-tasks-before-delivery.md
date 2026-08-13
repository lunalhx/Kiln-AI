---
status: accepted
---

# Verify Independent Test and Delayed Review tasks before delivery

Diagnostic and Practice Task Packages normally rely on the deterministic Output Gate, source and Rubric validation, Fingerprint checks, and available deterministic verification tools. Every Independent Test and Delayed Review Task Package must additionally pass one isolated Task Verification model call before learner exposure; the same requirement applies when deterministic tools cannot sufficiently verify an answer, the source is ambiguous, a novel generated scenario carries factual risk, or the envelope is internally inconsistent. The Task Verifier receives the task, hidden answer, Rubrics, source evidence, and difficulty contract but not the generator's hidden reasoning, and returns only structured judgments for answer correctness, Rubric alignment, source support, difficulty alignment, ambiguity, and issues. It cannot rewrite the package or change workflow state. A specific repairable issue may consume the existing one same-plan repair and then be rechecked; a remaining failure or conflict discards the entire unexposed package and requires a fresh generation rather than confidence averaging. Task Verification validates the instrument before use and remains separate from Assessment and Verification of the learner's later response.
