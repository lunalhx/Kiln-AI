# 01 — 收口为唯一 Learning 命令面，并记录 Blackboard / Review 取消决策

**What to build:** 产品只剩一条 Learning Flow 命令入口。图谱拥有的 interaction / checkpoint / result 使用 Learning 语义名；`Apply*` 只保留给任务生成、验证、评估。ADR-0072 明确最小 typed Blackboard 覆盖 ADR-0021 的 Apply 澄清；ADR-0073 明确 Started Review 只能走独立 cancel resource，覆盖 ADR-0068。既有 Diagnostic → Independent → Review 成功路径在新名字下仍然可跑。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] ADR-0072、ADR-0073 已接受，且 ADR 索引与冲突记录已更新。
- [ ] 图谱拥有的 Flow 交互类型已改为 Learning 语义；`ApplyFlowUseCase` 已删除，不再存在第二条写 Learning State 的命令入口。
- [ ] 既有 scripted 成功路径与 replay 契约在新命令面上保持通过。
