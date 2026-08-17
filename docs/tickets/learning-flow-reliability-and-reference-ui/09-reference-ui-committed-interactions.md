# 09 — Reference UI 按已提交 Interaction 呈现完整生命周期

**What to build:** 学习者打开页面时先恢复已提交 Flow，而不是再开一个。页面按 `task` / `teaching` / `assistance_consent` / `transition` / `unavailable` 分区，按 Task Package 动态渲染字段，H5 与随后的 Teach-back 同时可见。网络失败复用原请求；durable retry 用新 key。409 拉最新 Flow，422 保留输入，只有 404 才清掉 active Flow。

**Blocked by:** 06 — Diagnostic、Teach-back 与 standalone Explain 只允许程序性澄清; 07 — 显式取消未完成 Review Cadence; 08 — PostgreSQL 上验证新的可靠性不变量.

**Status:** ready-for-agent

- [ ] 不再把 raw JSON 当主界面；stage 以服务端为准，不推断已完成阶段；`transition` / `unavailable` 不当作题面。
- [ ] 动态渲染每个 `answerField`；数学字段有 canonical 预览；Teach-back 为单一短文本；H5 与后续 Teach-back 同时可见。
- [ ] 持久化并恢复 `learnerId`、`activeFlowId`、`pendingMutation`；启动时先恢复 Flow；仅确认 404 清除 active Flow。
- [ ] 网络失败复用原 body 与 Idempotency-Key；durable `retry_requested` 使用新 key；写操作进行中禁用写入；离开 Flow 与取消 Review 需确认。
- [ ] 服务器文本用 `textContent`（或等价安全插入）；DOM 与响应都不暴露答案、Rubric、Source Passage、Feedback Facts、Blackboard 或 execution trace。
- [ ] Playwright 覆盖 Spec 中的 UI seam。
