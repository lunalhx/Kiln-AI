# 12 — 统一 Learning API 与 reference UI 的破坏性切换

**What to build:** 学习者可通过统一 API 和 reference UI 完整使用 Learning/Practice loop；task、teaching、assistance consent、transition 与 unavailable 的 closed interaction union 都能被正确呈现和提交。

**Blocked by:** 11 — 冻结的 Strong/Small 模型运行时.

**Status:** ready-for-agent

- [ ] Learning Flow 与 Review Task resources 支持规范中的 closed command discriminator、Idempotency-Key、interactionVersion 和 attemptId 规则。
- [ ] UI 只呈现 committed interaction 与 allowedEvents，且不会暴露答案、Rubric、Source Passage、Feedback Facts、Blackboard 或 execution trace。
- [ ] `/api/apply/**`、`ApplyFlowResponse`、旧 controller/DTO/schema/Bundle fallback 已删除；HTTP contract 验证旧端点为 404，完整测试套件通过。
