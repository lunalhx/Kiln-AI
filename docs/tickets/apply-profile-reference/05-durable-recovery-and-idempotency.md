# 05 — 使完整 Apply flow 可持久恢复且幂等

**What to build:** 学习者在刷新页面或服务重启后，仍能继续同一个安全的 Apply flow；重放请求不会重复提交或重复建立 Evidence。

**Blocked by:** 04 — 从 Independent 成功建立唯一的 Independent Evidence。

**Status:** ready-for-agent

- [ ] 使用 typed LearningFlowStore 与 ArtifactStore 持久化 Source、Package、Attempt、Exposure、submission、assessment、verification、interaction 与 checkpoint。
- [ ] 创建 Package/open Attempt/interaction/checkpoint，以及关闭 Attempt/保存结果，均满足所需原子性。
- [ ] 重启后可恢复 open 或 closed Attempt；同一 idempotency key 返回原结果，绝不新增第二个 Attempt 或 Evidence。
- [ ] HTTP/UI 覆盖启动、查询、提交、版本冲突与恢复，公开响应始终没有 private projection。
- [ ] 核心 contract tests 不依赖 socket 或 Docker；Postgres recovery 在可用环境中验证。
