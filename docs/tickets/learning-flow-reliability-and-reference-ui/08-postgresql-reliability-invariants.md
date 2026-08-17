# 08 — PostgreSQL 上验证新的可靠性不变量

**What to build:** 全新库（禁止迁移旧 non-terminal Flow / 旧 cadence）在进程重启后，仍保持：失败 Start 不留痕迹、Active Work 唯一、Pending Operation 可 retry、已保存 submission 可恢复、Review 取消 exactly-once、不会重复 Evidence 或后继 Review。

**Blocked by:** 02 — 原子 Start 与唯一 Active Learning Work; 03 — 已有 Flow 的 Unavailable 与有界 `retry_requested`; 04 — 已关闭 Attempt 从保存的 submission 恢复，并强制 Attempt 归属; 05 — 模型契约失败闭合并按职责恢复; 07 — 显式取消未完成 Review Cadence.

**Status:** ready-for-agent

- [ ] 破坏性 schema 包含 Pending Operation、Active Learning Work、最小 typed Blackboard 引用，以及不含 raw payload 的 contract-audit；没有历史兼容或数据迁移。
- [ ] PostgreSQL 重启后能恢复 02–05、07 的 committed 行为：原子 Start 缺席、claim 唯一、retry 链、saved-submission、Review 取消。
- [ ] 并发与重放不会产生重复 open Attempt、Evidence 或未完成 Review。
- [ ] 测试用 in-memory transition store 与 PostgreSQL adapter 使用同一套原子语义。
