# 09 — 从 Supporting Flow 返回并恢复 Target Diagnostic

**What to build:** 学习者完成或离开单独的 Supporting Concept Flow 后，可自主返回已 suspend 的 Target Flow；Diagnostic 以冻结 Plan 和已提交事实继续，而不会重复已确认内容或重置预算。

**Blocked by:** 08 — “已知” prerequisite 的代表性 readiness check.

**Status:** ready-for-agent

- [ ] 返回是 learner-controlled；对齐的 Independent/Durable Supporting Concept Progress 直接满足 readiness，不对齐、缺失或较低 Progress 仅进行简短 recheck。
- [ ] 原 Target Flow 的冻结 Plan、已消耗 Diagnostic Attempt 数、committed Findings 与 Unknowns 在 suspension、return 和 PostgreSQL restart 后保持不变；已确认范围不得重复。
- [ ] 返回后继续按 Required Supporting Concepts 与 Target Readiness Set 的 Plan 规则执行，并且总 Diagnostic Attempt 数绝不因 resume 超过 Plan 上限或八题硬上限。
- [ ] suspend、return、recheck 和下一 learner interaction 保持 exactly-once；跨 Flow 只使用允许的 committed Concept Progress，且公开 API/参考 UI 不泄露任一 Flow 的私有数据。
