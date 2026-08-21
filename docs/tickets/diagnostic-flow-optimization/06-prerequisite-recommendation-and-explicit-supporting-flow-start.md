# 06 — Prerequisite 推荐与显式 Supporting Flow 启动

**What to build:** 当学习者对 Required Supporting Concept 报告 `not known` 或 `unsure` 时，Diagnostic 在首个充分 prerequisite 处中性停止，提供实际可执行的推荐选项，包括显式启动单独的 Supporting Concept Flow。

**Blocked by:** 05 — 从 Diagnostic 进入 Direct Learning.

**Status:** ready-for-agent

- [ ] Required Supporting Concepts 按冻结 Plan 的依赖顺序检查；首个 learner-declared unknown/unsure 立即停止剩余 probing，较低优先级 prerequisites 与 Target criteria 保持 Unknown。
- [ ] Prerequisite Learning Recommendation 只命名 Supporting Concept 并说明其相关性，提供 Direct Learning、离开、显式开始 Supporting Concept Flow 三种 learner-controlled 结果；不得自动创建、自动启动或在当前 Target Flow 内教学该概念。
- [ ] 显式启动单独 Supporting Flow 时，原 Target Flow 在无 open Attempt 的条件下 suspend；推荐、状态和用户选择均由 committed durable state 投影，不暴露原 task 的答案、解决方案、assessment output 或 source trace。
- [ ] prerequisite 自我报告和推荐不创建 Supporting Concept 的 Evidence、milestone 或 review，且命令重放不会启动重复 Flow 或重复 suspend Target Flow。
