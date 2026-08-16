# AGENTS.md

## 规则 (原版, side project 适用)

- Do not preserve backward compatibility. Remove obsolete paths instead of adding compatibility layers, fallbacks, or migrations.
  （不保留向后兼容。删除过时的代码路径，而不是添加兼容层、fallback 或 migration。）

- Choose the simplest implementation that fully meets the current requirements. Avoid speculative abstractions, configuration, and indirection.
  （选择完全满足当前需求的最简单实现。避免投机性的抽象、多余的配置和间接层。）

- Grow the system in layers. Start from the smallest version that works end to end, and add each new capability on top of a product that already works. Never trade a working product for unfinished complexity.
  （分层构建系统。从端到端可工作的最小版本开始，在已经可工作的产品之上逐层添加新能力。绝不用尚未完成的复杂度去换一个可用的产品。）

- Keep components modular and concerns clearly separated.
  （保持组件模块化，关注点清晰分离。）

- Prefer established, well-maintained libraries when they reduce overall complexity or improve reliability. Do not reimplement common functionality without a clear reason.
  （当成熟、维护良好的库能降低整体复杂度或提升可靠性时优先使用。没有明确理由，不要重新实现常见功能。）

- Lean on the dependencies already in the project before writing your own implementation or adding packages. Do not assume a library lacks a capability without checking its documentation and types.
  （先依靠项目已有的依赖，再自己实现或添加新 package。不查看库的文档和类型之前，不要假设它缺少某个能力。）

- Make architectural decisions for the long term. Do not accept a stopgap that only works for now and is meant to be replaced later.
  （架构决策要往长远做。不接受「只能现在用、以后要换」的权宜方案。）

- Study how established products solve the problem before designing a solution. Adopt their proven patterns and conventions rather than inventing an approach from scratch.
  （设计方案之前，先研究成熟产品如何解决这个问题。采用它们经过验证的 pattern 和约定，而不是从头发明。）

## 先读这些文档 (Read these before coding)

- README.md: module architecture, run commands, and verification commands.
- CONTEXT.md: the Phase 0 glossary; use its vocabulary and boundaries, do not invent synonyms.
- docs/adr/: the accepted decision baseline; new architectural decisions become a new numbered ADR, not a code comment or a compatibility layer.
- docs/specs/ and docs/tickets/: the current spec and tickets are normative; follow the spec's Implementation Decisions exactly and record any deviation explicitly.
  （README.md：模块架构、运行与验证命令。CONTEXT.md：Phase 0 术语表，使用其词汇与边界，不要自造同义词。docs/adr/：已接受的决策基线，新架构决策写成新编号 ADR，而不是代码注释或兼容层。docs/specs/ 与 docs/tickets/：当前 spec 与 ticket 是规范性的，逐条遵守 spec 的 Implementation Decisions，任何偏离都要显式记录。）

## 验证 (Verification)

- Run `./mvnw clean test` after changes; PostgreSQL-backed tests need Docker running (`docker compose --env-file deploy/local/.env -f deploy/local/compose.yaml up -d`).
- Contract tests with scripted fixtures are the stable regression oracle; live-model smoke tests are non-blocking and create no evidence.
- ArchUnit tests enforce module boundaries and the scheduler no-model-call invariant; keep them green.
  （修改后运行 `./mvnw clean test`；PostgreSQL 相关测试需要 Docker 已启动。脚本化 fixture 的契约测试是稳定回归基准；live-model smoke test 非阻塞且不产生 evidence。ArchUnit 测试强制模块边界与调度器无模型调用不变量，保持绿色。）

## 持久化命令不变量 (Persistence and command invariants)

- Generate and verify before any durable mutation; the claim, artifacts, Attempt, and state commit atomically in one transaction, so a failed generation leaves no trace.
  （生成与验证必须先于任何持久变更；claim、产物、Attempt 与状态在同一事务原子提交，生成失败不留任何痕迹。）

- Every command is exactly-once under replay: rehydrate committed state first — an already-produced outcome is ignored or returns its original result; a crash between two committed halves resumes from the saved Attempt; never re-run a committed transition.
  （每个命令在重放下 exactly-once：先读穿已提交状态——outcome 已产生则忽略或返回原结果；进程在两次提交之间崩溃则从已保存的 Attempt 恢复；绝不重跑已提交的 transition。）

- Reuse the existing Idempotency-Key contract, FlowCommandReplay, and submission contract; do not reimplement replay or response mapping per flow.
  （复用现有 Idempotency-Key 约定、FlowCommandReplay 与提交契约；不要按 flow 各自重新实现 replay 或响应映射。）

- Learner-visible responses are projections of committed durable state; never fabricate an interaction or message.
  （学习者可见的响应是已提交持久状态的投影；绝不伪造交互或消息。）
