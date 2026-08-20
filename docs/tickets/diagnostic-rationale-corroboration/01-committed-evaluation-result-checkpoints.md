# 01 — 已提交评估结果的 exactly-once checkpoint

**What to build:** 学习者正式提交后，即使进程在评估完成与下一次 learner interaction 之间崩溃，系统也会复用已提交的评估结果继续，而不会再次评估、重复创建 Evidence 或重复改变学习状态。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] 每个既有提交后评估责任都以 `(Task Attempt, responsibility, evaluation version)` 唯一保存；并发写入采用已提交的唯一键胜者，所有后续决定都读取该胜者。
- [ ] Response Assessment、Response Verification 与 Teach-back Assessment 使用同一个 save-or-return-committed 契约；旧 append-only Assessment 存储路径被破坏性删除，不保留双写、别名或数据迁移。
- [ ] 已保存评估、但尚未完成路由的命令在 replay 或 PostgreSQL 重启后只执行确定性组合与路由，不再调用模型，也不会重复 learner interaction 或 Evidence。
- [ ] 既有 Independent、Practice、Review 与 Teach-back 的可见行为保持不变，且完整 `./mvnw clean test` 通过。
