---
status: accepted
---

# Separate system instructions from execution data

Every Teaching Node call compiles the immutable Profile constraints, frozen Skill Stack cores, deterministically activated resources, and response contract into namespaced system instructions. The Node Context View, including Source Passages, Concept Contract, Task Blueprint, learner data, and novelty exclusions, is sent separately as structured user-layer execution data and is never executable instruction. Conflicts in permissions, output contributions, or Slots reject compilation before the call instead of relying on prompt order; this replaces the spike that concatenates Skills and context into one user message.
