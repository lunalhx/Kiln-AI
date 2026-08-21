# 01 — Gate-accepted Diagnostic Plan 启动 Learning Flow

**What to build:** 内容侧可从已批准的 Normalized Source Documents 生成并接受一个冻结、版本化的 Diagnostic Plan；学习者启动 Target Concept 的 Learning Flow 时，Flow 原子绑定该 Plan，并在首个 Diagnostic interaction 中看到 Plan-specific 最大 Attempt 数。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Concept Preparation Agent 为 Target Concept 生成包含 Target Readiness Set、Supporting Concepts、依赖顺序、source basis、coverage/termination 规则、rationale policy 与最大 Attempt 数的 Diagnostic Plan；运行时不得扩展或改写已冻结 Plan。
- [ ] Type-specific Gates 接受最大值为一至八的有效 Plan，并拒绝不支持的 source、无效引用、依赖环、Rubric 扩张、不安全 Readiness Set、无依据 rationale 与超过八题的 Plan；拒绝或 Source Gap 不得留下 Flow、Task Package、Attempt 或 learner interaction。
- [ ] 新 Flow 原子冻结已接受的 Plan 版本；公开 Learning Flow 响应和参考 UI 只显示完成次数与最大次数，不泄露 Plan 内部、private source trace 或评估事实，且 Plan 后续变更不影响已启动 Flow。
- [ ] 通过 focused Plan/Gate contract tests、公共 Flow start contract 和完整 `./mvnw clean test` 验证此端到端路径。
