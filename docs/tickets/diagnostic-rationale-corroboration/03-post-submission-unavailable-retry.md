# 03 — 提交后评估的 Unavailable 与精确 Retry

**What to build:** 学习者在正式提交后遇到 Provider、超时、配置或持续的模型契约失败时，会看到可恢复的 Unavailable；显式 Retry 只从保存的 Attempt 和缺失评估责任恢复，不会索要新答案或误判为能力不足。

**Blocked by:** 01 — 已提交评估结果的 exactly-once checkpoint.

**Status:** ready-for-agent

- [ ] 每项既有提交后评估最多进行一次相同 binding、Profile 与 Context 的 contract repair；Provider、超时或配置失败没有隐藏自动重试。
- [ ] Provider、超时、配置失败或第二次 Model Contract Invalid 均提交 Unavailable 与 Pending Operation；不创建 Evidence、替换任务、Diagnostic Not Passed 转换或 learner-failure signal。
- [ ] `retry_requested` 只重放保存 submission 的未完成或技术失败责任；原 Idempotency-Key replay 仍返回原有已提交 interaction，已完成责任绝不重跑。
- [ ] 通过脚本化 whole-flow 与 PostgreSQL 重启测试验证 Response Assessment、Response Verification 与 Teach-back Assessment 的上述行为；完整 `./mvnw clean test` 通过。
