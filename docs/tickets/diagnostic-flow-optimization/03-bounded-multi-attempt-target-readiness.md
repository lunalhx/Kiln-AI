# 03 — 有界的多 Attempt Target Readiness 覆盖

**What to build:** 学习者可通过多个新鲜 Diagnostic Task Attempt 覆盖冻结 Target Readiness Set；系统在 Plan 已取得足够事实时早停，或在 Plan-specific 上限内中性终止。

**Blocked by:** 02 — Plan 驱动的单准则 Diagnostic tracer.

**Status:** ready-for-agent

- [ ] 一个 Diagnostic task 可以覆盖多个 Plan 声明的 readiness criteria；只有整个 Target Readiness Set 被正向确认，才可路由到 Fresh Independent Test。
- [ ] 若需要继续，先提交只含完成次数/最大次数的 Neutral Transition，再生成、Gate 并验证一个 Fresh Equivalent Diagnostic task；学习者不得看到正确性、答案、规则、prerequisite Finding 或定向反馈。
- [ ] Conclusive Target gap 按 Plan 的早停规则进入 Target Learning and Practice；Unconfirmed Target performance 仅在预算剩余时取得 Plan-authorized fresh probe，达到上限后中性进入 Learning and Practice，Attempt 总数在 suspend/restart 后仍不超过八。
- [ ] whole-flow scripted contract、公开 API/参考 UI 与 PostgreSQL 恢复测试证明累计覆盖、早停、隐私与 exactly-once durable effects。
