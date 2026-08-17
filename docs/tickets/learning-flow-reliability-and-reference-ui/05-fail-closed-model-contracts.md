# 05 — 模型契约失败闭合并按职责恢复

**What to build:** 模型吐出不合格 JSON 时，学习者不会看到 503，也不会看到 parser / provider 细节。Assessment / Response Verification / Teach-back Assessment 修一次，第二次仍无效则 Inconclusive，不产生 Evidence。不合格的 Task Verification 只作废该 candidate。审计只留规范化 violation codes。

**Blocked by:** 02 — 原子 Start 与唯一 Active Learning Work.

**Status:** ready-for-agent

- [ ] Infrastructure adapter 只做 transport，返回 raw content；Domain 拒绝缺字段、类型错误、null、非法 enum、非法集合和未知字段。
- [ ] 上述三个评估职责各一次 repair，第二次无效即为 Inconclusive，走既有 replacement，不接受 Evidence。
- [ ] 不合格 Task Verification 只作废该 candidate，并走既有 fresh-candidate 策略；Pedagogy / Clarification 保持既有安全 fallback。
- [ ] 不持久化、不返回 raw invalid JSON / prompt / 学习者作答；审计仅含 identity、responsibility、violation codes、repair count、correlation ID、provider category。
- [ ] `MODEL_CONTRACT_INVALID` 不映射成 503；HTTP 体保持学习者安全。
