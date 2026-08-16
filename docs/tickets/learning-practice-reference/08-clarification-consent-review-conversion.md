# 08 — Clarification、assistance consent 与 Review-to-Practice conversion

**What to build:** 学习者在 Practice、Independent 或 Review 中请求帮助时，系统区分 procedural 和 substantive clarification；Independent/Review 在展示帮助前取得 consent 并安全地单向转换 Attempt。

**Blocked by:** 04 — Apply Practice 的持久化 Hint Ladder; 06 — Workflow Guard 与 Pedagogy Plan 的闭环选择; 07 — Fresh Independent 与失败后的重新 remediation.

**Status:** ready-for-agent

- [ ] Practice 中 substantive clarification 或 temporary Explain 被记录为 Assistance Trace，完成后返回同一打开的 Practice interaction。
- [ ] Independent/Review 拒绝帮助时 Attempt 不变；接受时先原子转换为 Practice，之后才可暴露帮助。
- [ ] 已开始 Review 的转换会取消 ReviewTask，不产生 Review Evidence 或 milestone 变化；后续 Independent PASS 从 Review 1 重启 cadence。
