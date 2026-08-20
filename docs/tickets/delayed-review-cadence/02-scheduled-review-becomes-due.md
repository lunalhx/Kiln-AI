# 02 — 到期 Review 变为 Due 并在 UI 可识别

**What to build:** 已安排的 Review 到达 `dueAt` 后变为 `Due`，让学习者看到它已经可开始；逾期 Review 保持可开始而不会被自动失败或重复创建。

**Blocked by:** 01 — Independent PASS 后创建并展示 Review 1.

**Status:** done

- [x] 每分钟 scheduler 只将 `dueAt <= clock.instant()` 的 `Scheduled` Review 转为 `Due`；它不调用模型、不生成 Package、不创建 Attempt、不记录 Exposure 或 Evidence，也不恢复 Flow。
- [x] 提前的 Review 不会转为 `Due`；重复 scheduler tick 幂等；已经逾期的 `Due` Review 保持 `Due`。
- [x] Review collection 持续按 `dueAt` 排序，并把 `Due` 项安全地标记为可开始；reference UI 从 upcoming 状态切换为 ready-to-start 状态。
- [x] 并发或重复调度仍保留每个 learner/Concept 至多一个 unfinished Review 的持久化不变量。
