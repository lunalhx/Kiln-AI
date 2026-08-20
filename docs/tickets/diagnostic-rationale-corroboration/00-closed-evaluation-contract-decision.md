# C0 — 封闭 Evaluation 契约决策门

**Purpose:** 这不是 implementation ticket。它记录 04 与 05 开始前必须由产品/架构决策确认的两项闭合契约，避免实施者自行发明枚举或开放数据结构。

**Blocked by:** None — requires a product/architecture decision, not implementation work.

**Status:** needs-decision

- [ ] 确认 `rationale_evaluation/v1` 的 `reason_codes` 精确闭合集合；它须覆盖已批准类别（缺少支持、错误适用、事实错误、实质缺口、矛盾），但不得使用开放字符串集合。
- [ ] 确认 Rationale Evaluation Context 中“必要私有 expected-answer facts”的精确闭合表示，包括当前 Trusted Primary-Answer Check 的事实基础；不得引入开放 map 或通用 checker-reference schema。
- [ ] 将这两项决定写入已批准的规范或 ADR 基线；04 与 05 必须按该基线实施。
