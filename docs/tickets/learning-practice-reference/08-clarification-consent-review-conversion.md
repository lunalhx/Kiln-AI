# 08 — Clarification、assistance consent 与 Review-to-Practice conversion

**What to build:** 学习者在 Practice、Independent 或 Review 中请求帮助时，系统区分 procedural 和 substantive clarification；Independent/Review 在展示帮助前取得 consent 并安全地单向转换 Attempt。

**Blocked by:** 04 — Apply Practice 的持久化 Hint Ladder; 06 — Workflow Guard 与 Pedagogy Plan 的闭环选择; 07 — Fresh Independent 与失败后的重新 remediation.

**Status:** ready-for-agent

- [x] Practice 中 substantive clarification 或 temporary Explain 被记录为 Assistance Trace，完成后返回同一打开的 Practice interaction。
- [x] Independent/Review 拒绝帮助时 Attempt 不变；接受时先原子转换为 Practice，之后才可暴露帮助。
- [x] 已开始 Review 的转换会取消 ReviewTask，不产生 Review Evidence 或 milestone 变化；后续 Independent PASS 从 Review 1 重启 cadence。

## 显式记录的执行偏离 (Deviations recorded explicitly)

- **Teach-back 与 standalone Explain 边界上的 clarification 命令路由未实现。** Spec line 124 与 147 允许 Explain / Teach-back Interaction Contract 携带 Clarification 事件（learner projection 的 `allowedEvents` 已含 `CLARIFICATION_ASKED`），但 ticket 08 的验收标准只覆盖 Practice / Independent / Review 三个 Attempt purpose，spec 的 Implementation Decisions 也没有规定这两个边界的 clarification 图路由（Teach-back 的临时 Explain 恢复需要把 open-Attempt 的 flow-scoping 扩展到 teach-back package，超出本 ticket 范围）。因此 `clarification_asked` 对 Diagnostic 与 Teach-back Attempt 返回 `WRONG_ATTEMPT_PURPOSE`（与 hint 命令一致），对 teaching boundary（无 open Attempt）不可用。后续 ticket 需要在 Interaction Contract 层补上这两个边界的图行为。
- **Review conversion 的进程崩溃恢复是 ticket 10 的范围。** 转换、ReviewTask 取消与 boundary commit 在领域命令中顺序执行；in-memory 重试通过 Already-Practice 幂等路径恢复，但 PostgreSQL 上转换与取消之间的崩溃窗口、artifact 复用与完整 exactly-once 恢复契约由 ticket 10 提供。
