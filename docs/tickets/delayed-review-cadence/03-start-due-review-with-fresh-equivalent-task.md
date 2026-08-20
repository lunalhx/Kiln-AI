# 03 — 启动 Due Review 并交付新的等价任务

**What to build:** 学习者启动一个 Due Review 后，能在原有 Apply Flow 的 Delayed Review interaction 中获得刚刚生成、已验证且未曾暴露的无提示等价任务。

**Blocked by:** 02 — 到期 Review 变为 Due 并在 UI 可识别.

**Status:** done

- [x] 存在一个冻结、版本化的 `REVIEW` Task Blueprint，并使用升级后的同一 `apply.task-first` Bundle；它复用 Independent 的范围、难度和 response fields。
- [x] `POST /api/review-tasks/{reviewTaskId}/start` 使用 Idempotency-Key；成功时返回与现有 Apply 体验一致的 learner-safe Review interaction，reference UI 可以由 Due 项进入作答。
- [x] 任务仅在 start 时生成；完整排除原 Flow 已暴露的 task 与 solution fingerprints，并在 exposure 前通过既有 Output Gate 与 Task Verification。
- [x] 成功 start 原子地绑定 Package、Review Attempt、Exposure、Started 状态、Flow interaction 与幂等命令；同一 ReviewTask 最多有一个 OPEN Attempt。
- [x] generation、Source Gap 或 verification 不可用时，Review 保持 `Due`，且不创建 Attempt 或 Exposure；同 key replay 与并发 start 不会产生重复 Package 或 Attempt。
