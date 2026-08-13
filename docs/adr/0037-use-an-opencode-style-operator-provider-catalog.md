---
status: accepted
---

# Use an OpenCode-style operator Provider Catalog

Kiln-AI will register providers the way OpenCode does: an operator-owned catalog of protocol, endpoint, and listed models, with model identity written as `providerId/modelId`. The default protocol is OpenAI-compatible and maps to Spring AI's OpenAI ChatModel with a configurable base URL; DashScope is an exception only when that protocol cannot speak the API. Operators set two slots, Strong Model and Small Model, instead of one Binding per responsibility. Phase 0 will not use Models.dev, a learner model picker, or a `/connect` auth flow. Starting a Learning Flow freezes a resolved snapshot of protocol, endpoint, and model identity; environment secrets are not copied. A missing catalog entry, incomplete profile, or missing secret fails closed and does not fall back to scripted fakes.
