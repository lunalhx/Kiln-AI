# 04 — 已关闭 Attempt 从保存的 submission 恢复，并强制 Attempt 归属

**What to build:** 学习者提交后，即使评估前进程崩溃，下一次恢复也只评估库里那份 submission。Retry 或重放不能换答案。已经被后续 Interaction 替换的 Attempt 不能再被路由。Diagnostic / Practice / Independent / Review / Teach-back 同一条规则。

**Blocked by:** 03 — 已有 Flow 的 Unavailable 与有界 `retry_requested`.

**Status:** ready-for-agent

- [ ] 评估类命令先原子关闭 Attempt 并保存 submission / pending assessment，再做模型评估。
- [ ] 关闭后、评估完成前的崩溃，从已保存 Attempt 继续；请求体不能替换该 submission。
- [ ] Attempt 必须属于当前 Flow，且是当前 Interaction 指向的 Attempt；被后续 Interaction 替换后不可再路由。
- [ ] 已完成命令重放返回原 interaction；Evidence 与 transition 仍然 exactly-once。
