# 02 — 提交一次 Diagnostic 并中性转入新鲜的 Independent 任务

**What to build:** 学习者可以正式提交一次 Diagnostic；通过后不获正确性反馈，直接进入一题新鲜、已验证的 Independent Apply 任务。

**Blocked by:** 01 — 编写并运行五个 Apply Bundle 与 Profile，交付已验证的 Diagnostic 任务。

**Status:** ready-for-agent

- [ ] 提交同时保留 raw derivative 和 learner-confirmed canonical expression，支持范围内普通文本、Unicode 与 LaTeX-like 导数写法。
- [ ] 一次正式提交先原子关闭 Attempt；重放、重复或过期提交不能形成第二次评估或第二份结果。
- [ ] 正确最终导数或适用理由均可使 Diagnostic 通过，但绝不创建 Evidence 或展示反馈。
- [ ] Independent 只接收 Diagnostic-pass 事实与 Exposure Ledger fingerprints；不得接收前一题答案、理由、key、评估或反馈。
- [ ] Independent task 排除既有 task/solution fingerprints；Diagnostic 未通过时安全结束。
