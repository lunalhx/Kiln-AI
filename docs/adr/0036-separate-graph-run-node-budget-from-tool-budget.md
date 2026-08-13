---
status: accepted
---

# Separate Graph Run node budget from Tool Budget

ADR-0032 mixed planned node work, gate repair, and later tool follow-up into one "model call" counter, which made the ceiling both hard to explain and easy for tool loops to exhaust. A Graph Run now budgets model-producing node entries only: at most three for an Ordinary Run and four for a High-Consequence Run. Deterministic gates do not count. Gate repair remains at most one extra attempt on the same node and is not a new node entry. Authorized tool executions have a separate Tool Budget for that wake-up; the numeric ceiling is operator configuration and is not mixed into the node ceiling. Token, cost, and latency remain traced. Overflow still stops only that Graph Run with a declared safe outcome and does not end the Learning Flow.
