---
status: accepted
---

# Use a plain Java Learning StateGraph runner

ADR-0064 restores the application-owned Learning StateGraph as the Phase 0 coordination runtime for the multi-Profile Learning and Practice reference but leaves the graph framework open; ADR-0034's framework direction is already superseded and its adapter and dependency were removed from the product path. This implementation therefore runs the Learning StateGraph as an ordinary Java runner inside the domain layer, reusing the durable command, replay, and submission contracts that already exist.

A `LearningStateGraph` class owns one Graph Run per learner command: it rehydrates Learning State from the durable records and the saved checkpoint, runs the deterministic input gate, routes the command to the single legal Apply node, and commits the learner interaction, its checkpoint, and the processed command atomically at every Learner Interaction Boundary. The existing durable Apply generation, verification, submission, replay, and evidence behavior is reused as Apply node capability; no graph library, adapter layer, or framework type enters the domain.

The plain Java runner is the simplest implementation that meets the current Phase 0 requirements: a small set of deterministic gates and model-backed nodes, an ArchUnit-enforced framework-free domain, and the existing durable command contract reused without an adapter seam. A graph library remains an explicitly deferred implementation decision of the Learning/Practice reference spec; the domain graph and transition semantics stay application-owned either way.

The legacy `ApplyFlowUseCase` command seam remains in place until the Learning Flow public API cutover removes the Apply endpoints; the graph-backed `LearningFlowCommandUseCase` is the single new command surface, and the shared mapping it reuses lives on after the legacy seam is deleted. The start command keeps the legacy ordering in which the durable Flow record and source pack are written before the first generation: a crash inside the start window can leave an inert orphan Flow record with no interaction, and the same-key retry starts a fresh Flow from the committed records. This pre-existing limitation is inherited from the Apply seam, applies equally to both command surfaces, and is addressed when the command surface gains a single atomic start binding.
