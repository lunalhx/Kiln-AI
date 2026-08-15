# 05 — Review 失败与理由矛盾安全地终止 cadence

**What to build:** 普通 Review FAIL 和“最终答案正确、但实质理由矛盾”的 Review `Blocked` 都会以安全、明确的学习者结果结束当前 cadence：Current 回到 Learning，Highest 保留，未完成 Review 被取消。

**Blocked by:** 04 — Review PASS 推进 cadence 并达到 Durable.

**Status:** ready-for-agent

- [ ] 有效 no-hint Review FAIL 原子地接受一条 Review FAIL Evidence、完成当前 Review 并取消其余 unfinished Review；Current Milestone 变为 `LEARNING`，Highest Milestone Reached 不下降。
- [ ] 新增并登记 ADR-0061：仅在 Review 中将答案—理由矛盾产生的 `Blocked` 作为 conclusive no-hint FAIL；Independent 保留现有 `Blocked` 行为，且不引入通用 failure-code 层级。
- [ ] 理由矛盾时 learner 明确收到“最终答案与给出的理由不一致”的安全提示；普通失败和矛盾失败均不泄露 assessment facts 或 model reason codes。
- [ ] 失败后不会保留可行动的 Review；后续新的 Independent PASS 能重新开始从 Review 1 起的 cadence。
- [ ] 普通 FAIL 与 `Blocked` 分支通过公共契约验证其 Evidence、任务状态、里程碑投影和 cadence 终止行为。
