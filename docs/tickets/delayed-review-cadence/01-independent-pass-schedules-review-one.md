# 01 — Independent PASS 后创建并展示 Review 1

**What to build:** 学习者完成新的 Independent PASS 后，系统以实际接受时间为基准，在同一原子提交中创建唯一的 `Scheduled` Review 1；学习者能看到即将到来的复习和安全的 Concept Progress。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] 新的 Independent PASS 会接受 Evidence、取消同一 learner 与 Concept 的旧 unfinished Review，并创建唯一的 Review 1；其 `dueAt` 为接受时间加 24 小时。
- [ ] learner 能通过 Review collection 和 reference UI 查看按 `dueAt` 排序的 unfinished Review；`Scheduled` 项显示为即将到来且不可操作。
- [ ] Apply 响应和 Review collection 仅暴露安全的 Current Milestone、Highest Milestone Reached 与 Stage；learner UUID 在刷新后仍被使用，私有评估事实不泄露。
- [ ] Concept Progress 从按 `acceptedAt + evidenceId` 排序的 Evidence 确定性投影；新的 Independent PASS 会重置 Review 成功计数，Highest Milestone Reached 不会下降。
- [ ] PostgreSQL 与 in-memory 路径都验证 Independent PASS 和首个 Review 调度的原子性及一项未完成 Review 的约束。
