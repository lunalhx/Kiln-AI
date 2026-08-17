# 07 — 显式取消未完成 Review Cadence

**What to build:** 学习者确认后，可通过 `POST /api/review-tasks/{reviewId}/cancel` 取消 Scheduled、Due 或 Started Review。取消不造 Evidence，也不改 Current / Highest Milestone。取消 Started Review 会在同一事务里放弃 open Attempt、取消 Review，并提交 terminal Flow；随后可以重新开始 Diagnostic。`flow_control_requested` 不再承担这条取消。

**Blocked by:** 02 — 原子 Start 与唯一 Active Learning Work.

**Status:** ready-for-agent

- [ ] Review 取消是独立幂等资源，使用独立 Idempotency ledger，不走 Learning Flow command discriminator。
- [ ] Scheduled / Due 取消后为 Cancelled；Started 取消原子放弃 Attempt、取消 Review、提交 terminal Flow interaction。
- [ ] 不接受 Learning Evidence，不改变 Current Milestone 或 Highest Milestone Reached。
- [ ] 已 Completed / Cancelled 的重放返回已提交终态，无第二次副作用；取消后 Active Learning Work 释放，允许新的 Diagnostic。
- [ ] ADR-0068 的 leave-cancels-Started-Review 路径已删除。
