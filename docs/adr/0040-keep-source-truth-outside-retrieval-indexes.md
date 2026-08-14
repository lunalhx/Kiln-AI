---
status: accepted
---

# Keep source truth outside retrieval indexes

Kiln-AI will retain each supplied Source Original immutably and derive a versioned Normalized Source Document that preserves structural blocks, extraction warnings, and anchors back to that original. Concept Source Packs and Node Context Views use traceable Source Passages from these records. Vector and keyword Retrieval Indexes may be added later, but they are rebuildable infrastructure projections that only locate candidate passages; they never become the source of truth for course content, citations, Concept boundaries, task correctness, or evidence.
