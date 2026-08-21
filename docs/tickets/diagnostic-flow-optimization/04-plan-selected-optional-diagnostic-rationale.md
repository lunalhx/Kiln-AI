# 04 — Plan 选择的可选 Diagnostic rationale

**What to build:** 仅当冻结 Plan 和 Task Rubric 证明 rationale 有用时，学习者看到可选 rationale 字段；该字段按隔离、已检查点的评估流程影响 Diagnostic Finding，而绝不变成必答题或泄露评估内容。

**Blocked by:** 02 — Plan 驱动的单准则 Diagnostic tracer.

**Status:** ready-for-agent

- [ ] Gate 仅接受带 rationale-relevant Task Rubric mapping 的 rationale-enabled Blueprint；不满足条件的 Diagnostic task 不显示该字段，启用时 learner projection 说明其可选及可能的解释作用。
- [ ] 已证明正确的主答案直接通过且不调用 rationale evaluators；已证明错误的主答案只有在两次隔离判断均为 Applicable 时才被挽救。
- [ ] 缺失或 Not Applicable rationale 产生 Conclusive Diagnostic Gap；Cannot Decide、语义不确定或 corroboration 失败产生 Unconfirmed Diagnostic Performance，绝不制造确定性缺陷结论。
- [ ] 已提交的 evaluation result 可在 replay/retry 中复用；技术或 contract 失败走 Unavailable，不重复已提交任务或评估，也不向 API/UI 暴露 private evaluator facts。
