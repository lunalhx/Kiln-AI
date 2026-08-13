---
status: accepted
---

# Separate assessment with selective verification

Teaching Node executions will not grade their own work or create Learning Evidence directly. A logically isolated Assessment node produces an Evidence Candidate against an explicit Rubric, and deterministic rules validate assistance and test conditions before accepting it as Learning Evidence. Phase 0 uses one Assessment call by default and adds one Verification call only for consequential Concept-state changes, ambiguous judgments, or internally inconsistent output. Agreement is accepted; disagreement becomes inconclusive and triggers a new equivalent task instead of averaging model confidence or running mandatory multi-model voting on every response.
