# 06 — Workflow Guard 与 Pedagogy Plan 的闭环选择

**What to build:** 在 Explain 完成、Practice 或 Teach-back 结果、H5 reveal 与 readiness 等节点，系统先从 committed state 推导合法 Teaching Actions，再由 Pedagogy Agent 只在该集合中选择下一步。

**Blocked by:** 03 — Explain Profile 的完整教学切片; 04 — Apply Practice 的持久化 Hint Ladder; 05 — 锚定的 Teach-back 切片.

**Status:** ready-for-agent

- [ ] Agent 只接收 sanitized Feedback Facts 和 closed legal-action set，不能获得答案、评估推理、Skill ID，也不能写状态。
- [ ] Plan 一次 repair 后仍无效时，丢弃完整无效输出并使用规范中的确定性 fallback；fallback 不可用时投影真实安全边界。
- [ ] whole-graph scripted contract 覆盖单候选、多候选及每种规定 fallback，且 scheduler 不会触发模型调用。
