# 05 — 从 Diagnostic 进入 Direct Learning

**What to build:** 学习者可在首个 Diagnostic 前、打开的 Diagnostic task 中或已有 committed Finding 后，选择 Direct Learning 并安全进入 Target Learning and Practice。

**Blocked by:** 02 — Plan 驱动的单准则 Diagnostic tracer.

**Status:** ready-for-agent

- [ ] Direct Learning 作为现有统一 Learning Flow command 和 committed-interaction contract 的一部分，在公开 API 与参考 UI 的每个 Diagnostic learner interaction 可用。
- [ ] 在打开但未提交的 Diagnostic Attempt 上选择 Direct Learning 会将该 Attempt 标为 Abandoned 且不创建 Finding；已提交 Attempt 与 Finding 保持不可变。
- [ ] 该选择原子地结束 Diagnostic 并进入 Target Learning and Practice，但不创建 prerequisite readiness、Learning Evidence、Independent milestone 或 direct Independent 资格；正常后续 Practice 仍可取得 Independent Test 资格。
- [ ] Idempotency replay、interaction-version 冲突与 PostgreSQL crash recovery 均不重复 abandonment、Finding、transition 或 learner interaction。
