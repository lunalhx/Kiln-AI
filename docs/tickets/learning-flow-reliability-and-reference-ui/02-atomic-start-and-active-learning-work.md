# 02 — 原子 Start 与唯一 Active Learning Work

**What to build:** 学习者第一次开始诊断时，要么得到已提交的 Diagnostic interaction，要么得到泛化 503 且库里没有任何 Flow / Source Pack / Attempt。同一 learner + Target Concept 已有 Active Learning Work 时，另一把 Idempotency-Key 的 Start 只返回含既有 Flow ID 的 409。未完成 Review 会挡住新的 Diagnostic；没有未完成 Review 的 terminal Flow 会释放 claim。

**Blocked by:** 01 — 收口为唯一 Learning 命令面，并记录 Blackboard / Review 取消决策.

**Status:** ready-for-agent

- [ ] Start 在 resolve profile、生成、Gate、验证 Diagnostic 全部成功之前，不持久化 Flow、Source Pack、Active Work、Package、Attempt、Exposure、checkpoint、Interaction 或 processed command。
- [ ] 首次准备失败返回泛化 503，whole-flow 契约断言上述记录全部缺席；客户端复用原 Idempotency-Key。
- [ ] 成功 Start 在同一事务绑定上述记录，并占住该 learner + Concept 的唯一 Active Learning Work。
- [ ] 冲突 Start 返回学习者安全的 409，体里只有恢复所需的既有 Flow ID；未完成 Review（Scheduled / Due / Started）同样阻止新 Diagnostic。
