---
status: accepted
---

# Normalize source formats before Concept Preparation

Concept Preparation and all downstream learning components will depend on a format-neutral Normalized Source Document rather than PDF, Markdown, or another external representation. A Source Adapter owns format-specific extraction and emits source identity and version, hierarchy, text and formula blocks, media references, original-location anchors, and extraction warnings while preserving provenance. The first tracer bullet will use a manually prepared internal fixture that satisfies this contract and will not treat that fixture as a product input decision. The permanent textbook, edition, selected Concept, and PDF/Markdown/other adapter remain open; choosing or replacing an Adapter must not change Concept Contract, Concept Source Pack, Teaching Node, Skill, Assessment, or evidence contracts. This permits the learning architecture to run before ingestion uncertainty is resolved without hiding extraction loss.
