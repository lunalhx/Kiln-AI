# 10 — PostgreSQL 的恢复与 exactly-once 学习循环

**What to build:** 学习者在答案提交或接受 assistance conversion 中途发生进程崩溃后，重放同一命令会从保存的恢复点继续，不会重新生成、重复计 Evidence 或重复改变 ReviewTask。

**Blocked by:** 09 — PostgreSQL 的 Learning Flow 成功路径持久化.

**Status:** ready-for-agent

- [x] 失败生成不留下未接受 artifact、Attempt 或 Evidence；已完成命令始终返回原始 committed interaction。
- [x] submission 关闭 Attempt 后和 Review conversion 后的崩溃都可恢复，且每个副作用恰好发生一次。
- [x] PostgreSQL recovery contract 覆盖 Blackboard/artifact references、Hint/Teaching artifacts、novelty、Evidence 与 ReviewTask。
