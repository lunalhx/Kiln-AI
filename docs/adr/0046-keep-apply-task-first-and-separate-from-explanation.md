---
status: accepted
---

# Keep Apply task-first and separate from explanation

The Apply Profile will only generate and deliver a bounded Task Package, open its Task Attempt, and return control to the graph. It will not explain a Concept, show a worked example or solution, assess a response, or accept evidence. Its one generic `apply.task-first` Action Skill supports both Diagnostic and Independent Test; those purposes differ only through their Task Blueprints and graph gates, not through separately forked Action Skills. A future worked-example Action Skill is limited to Practice and must not be selected for Diagnostic or Independent Test, preserving Explain as the owner of teaching explanation and preventing assistance leakage into evidence.
