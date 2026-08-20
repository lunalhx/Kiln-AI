# 03 — 已有 Flow 的 Unavailable 与有界 `retry_requested`

**What to build:** 已有 Flow 上的 provider / 配置失败不再变成 terminal 失败。学习者看到 `unavailable`、状态为 `AWAITING_LEARNER_INPUT`，可以用无业务 payload 的 `retry_requested` 恢复。同一条 Retry Chain 最多失败三次，之后只能离开，不能再重试。成功 retry 清掉 Pending Operation，且不会改写已保存的答案。

**Blocked by:** 02 — 原子 Start 与唯一 Active Learning Work.

**Status:** done

- [x] 已有 Flow 的 provider 网络/超时/上游 5xx 与运行时 Model Profile 配置失败，提交 `unavailable` + Pending Operation；不产生半成品 artifact、新 Attempt、Evidence 或 cadence 变化。
- [x] `retry_requested` 只在 `unavailable` 上合法，不带答案或原命令体，使用新的 Idempotency-Key，只恢复服务端保存的 Pending Operation。
- [x] 失败 retry 递增链计数并产生新 interaction version；第三次后不再 advertise retry，但 Flow Control 仍可安全离开。
- [x] 成功 retry 提交下一 interaction 并清除 Pending Operation；初次 Start 失败仍走 503，绝不靠 `retry_requested` 造出 Flow。
