---
status: accepted
---

# Assemble Apply Task Packages from typed generation drafts

The Apply model call returns only a typed `ApplyGenerationDraft`: learner task text, structured private assessor facts, or a structured Source Gap. The Apply Profile—not the model—deterministically attaches the Answer Representation Contract, locale-rendered fields, Interaction Contract, one-submission rule, and final Task Fingerprint to form the Task Package. It derives the Fingerprint from validated, controlled task facts so the generating model is never the authority for Independent-Test novelty. This replaces the spike's generic envelope fields for free-form private artifacts, model-controlled allowed events, and `hiddenReasoning`; model chain-of-thought is neither an output field nor a stored artifact.
