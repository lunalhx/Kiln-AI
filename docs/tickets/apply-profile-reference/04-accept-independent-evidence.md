# 04 — 从 Independent 成功建立唯一的 Independent Evidence

**What to build:** 学习者在新的 Independent 任务中成功后，Concept 可安全晋升为 `INDEPENDENT`；不合格结果绝不会产生证据。

**Blocked by:** 03 — 处理 Cannot Decide 与理由政策，保证评估隔离。

**Status:** ready-for-agent

- [ ] 仅在最终导数通过且理由不矛盾时，接受一次 Independent Evidence 并更新 Concept Progress。
- [ ] Diagnostic、Inconclusive、矛盾理由、重复提交、未关闭 Attempt 均不能接受 Evidence。
- [ ] 每次实际展示任务都写入 task 与 solution fingerprints，后续 generation 继续应用 freshness exclusion。
- [ ] 学习者只看到安全的继续或结束状态；没有答案、评估结论、来源、Fingerprint 或审计信息泄漏。
- [ ] contract test 覆盖 Independent 成功、空理由、矛盾理由与重复提交。
