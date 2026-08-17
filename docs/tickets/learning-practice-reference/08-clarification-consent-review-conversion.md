# 08 — Clarification、assistance consent 与 Review-to-Practice conversion

**What to build:** 学习者在 Practice、Independent 或 Review 中请求帮助时，系统区分 procedural 和 substantive clarification；Independent/Review 在展示帮助前取得 consent 并安全地单向转换 Attempt。

**Blocked by:** 04 — Apply Practice 的持久化 Hint Ladder; 06 — Workflow Guard 与 Pedagogy Plan 的闭环选择; 07 — Fresh Independent 与失败后的重新 remediation.

**Status:** ready-for-agent

- [x] Practice 中 substantive clarification 或 temporary Explain 被记录为 Assistance Trace，完成后返回同一打开的 Practice interaction。
- [x] Independent/Review 拒绝帮助时 Attempt 不变；接受时先原子转换为 Practice，之后才可暴露帮助。
- [x] 已开始 Review 的转换会取消 ReviewTask，不产生 Review Evidence 或 milestone 变化；后续 Independent PASS 从 Review 1 重启 cadence。

## 已收敛的补充决策 (Not yet implemented)

- **Diagnostic、Teach-back 与 standalone Explain 的 clarification 边界已确定。** Diagnostic 与 Teach-back 只允许程序性澄清；substantive 或 uncertain 请求不提供内容、不改变 Attempt purpose 或证据资格。Standalone Explain 也只允许程序性澄清，命令面向当前 teaching interaction 而非 Attempt。该决定取代本 ticket 原先记录的未实现偏离；实现见 `learning-flow-reliability-and-reference-ui/06-procedural-clarification-boundary.md`。
- **Review conversion 的进程崩溃恢复是 ticket 10 的范围。** 转换、ReviewTask 取消与 boundary commit 在领域命令中顺序执行；in-memory 重试通过 Already-Practice 幂等路径恢复，但 PostgreSQL 上转换与取消之间的崩溃窗口、artifact 复用与完整 exactly-once 恢复契约由 ticket 10 提供。
