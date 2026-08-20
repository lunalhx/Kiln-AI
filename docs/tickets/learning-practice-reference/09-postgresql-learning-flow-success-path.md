# 09 — PostgreSQL 的 Learning Flow 成功路径持久化

**What to build:** 新的破坏性 Learning Flow 数据基线可在 PostgreSQL 上持久运行成功路径；重启后学习者仍可看到已提交状态，且 Review cadence 保持连续。

**Blocked by:** 08 — Clarification、assistance consent 与 Review-to-Practice conversion.

**Status:** done

- [x] 新 baseline 不保留 `apply_*` 表或旧 Flyway 兼容路径，并持久 Flow、Interaction、Checkpoint、Command、Attempt、Evidence 与 ReviewTask。
- [x] PostgreSQL 重启后可以恢复“Diagnostic PASS → Independent PASS → Review 1”的 committed state。
- [x] 并发条件不会创建重复 open Attempt、Evidence 或 unfinished ReviewTask。
