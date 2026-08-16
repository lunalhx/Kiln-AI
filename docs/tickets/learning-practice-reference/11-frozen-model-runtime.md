# 11 — 冻结的 Strong/Small 模型运行时

**What to build:** 运维者启动 Learning Flow 时，Flow 冻结 Strong/Small 模型 binding、Profile/Bundle/Blueprint 版本和预算；其后模型调用使用冻结配置而非当前默认配置。

**Blocked by:** 10 — PostgreSQL 的恢复与 exactly-once 学习循环.

**Status:** ready-for-agent

- [ ] Strong/Small 职责、每节点 repair 上限、16,000 字符 instruction cap 与 operator-owned output ceiling 均被执行并记录。
- [ ] 缺失或无效的 operator 配置 fail closed；测试 fixture 不成为生产 fallback。
- [ ] Spring AI 仅存在于 infrastructure model adapter，live-model smoke 仍非阻塞且不产生 Evidence。
