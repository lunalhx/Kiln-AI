# 06 — Inconclusive Review 可替换并可恢复继续

**What to build:** Review Assessment 无法得出结论时，学习者不会被判失败；同一 Started Review 会获得新的已验证等价 Attempt，或在生成暂时不可用时可安全地稍后继续。

**Blocked by:** 04 — Review PASS 推进 cadence 并达到 Durable.

**Status:** done

- [x] Inconclusive 会关闭已提交的 Attempt，但不接受 Evidence、不改变 Milestone 或 cadence，也不把 learner 显示为失败。
- [x] 系统为同一 Started Review 准备一个新的、通过 freshness 与 Task Verification 的等价 Attempt；每个 ReviewTask 始终最多一个 OPEN Attempt。
- [x] replacement generation、Source Gap 或 verification 不可用时，Review 保持 `Started`，不产生不合格的 Attempt 或 Exposure；同一 start endpoint 能安全地 resume。
- [x] 重启后能够恢复精确的开放 Review interaction；重放、并发和重复 submission 不会创建重复的 replacement 或 Evidence。
- [x] reference UI 清楚显示可继续的状态，区分系统不确定性与 learner 的失败。
