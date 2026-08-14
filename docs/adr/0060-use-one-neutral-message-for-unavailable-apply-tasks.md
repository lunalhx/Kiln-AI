---
status: accepted
---

# Use one neutral message for unavailable Apply tasks

When Apply returns Source Gap or Task Generation Exhausted before learner exposure, the Graph presents the deterministic message “暂时无法准备一道可验证的题目。请稍后重试。” It exposes no source, model, validation, or technical details; those remain audit-only. The learner may retry or leave the Flow, and neither outcome creates a Task Attempt or Learning Evidence.
