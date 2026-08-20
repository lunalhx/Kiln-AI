# 02 — Diagnostic 主答案的安全分流

**What to build:** 学习者提交 Diagnostic 时，已证明正确的主答案直接经中性过渡进入 Fresh Independent Test；已证明错误且没有理由的答案确定进入 Learning and Practice，而非靠可选字段意外通过。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] 旧 `diagnostic.final-or-applicable-rationale@1` 被破坏性替换为显式 opt-in 的 `diagnostic.primary-or-corroborated-rationale@1`，并为当前 Diagnostic 声明 Trusted Primary-Answer Check。
- [ ] 已证明正确的主答案忽略任何可选理由，不调用 Rationale Assessment 或 Rationale Sufficiency Verification，不创建 Diagnostic Evidence，并交付中性过渡后的 Fresh Independent Test。
- [ ] 已证明错误且理由为 `null`、空或纯空白时，不调用模型，形成 Conclusive Diagnostic Gap，并且 Workflow Guard 只允许 Explain 与 Apply Practice。
- [ ] 这两条公开 Learning Flow 命令路径不新增 learner command、答案字段或私有评估事实泄露，且完整 `./mvnw clean test` 通过。
