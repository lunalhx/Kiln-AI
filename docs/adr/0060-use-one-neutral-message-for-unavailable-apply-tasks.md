---
status: accepted
---

# Use one neutral message for unavailable Apply tasks

When Apply returns Source Gap or Task Generation Exhausted before learner exposure, the Graph presents the deterministic message “暂时无法准备一道可验证的题目。请稍后重试。” It exposes no source, model, validation, or technical details; those remain audit-only. Before initial Flow binding, the Start command fails atomically and is retried with its original Idempotency-Key. On an existing Flow, the message is projected through the durable Unavailable Interaction and bounded `retry_requested` policy of ADR-0069; neither retry nor leaving creates a Task Attempt or Learning Evidence.
