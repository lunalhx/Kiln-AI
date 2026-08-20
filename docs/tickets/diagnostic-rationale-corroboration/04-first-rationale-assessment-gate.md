# 04 — 首轮理由评估阻止空洞救援

**What to build:** 已证明错误的 Diagnostic 主答案配合“我不知道”等空洞理由，会接受完整语义的首轮 Rationale Assessment，并进入 Learning and Practice；它不会因关键词或简单措辞而被放行到 Independent Test。

**Blocked by:** C0 — 封闭 Evaluation 契约决策门; 01 — 已提交评估结果的 exactly-once checkpoint; 02 — Diagnostic 主答案的安全分流; 03 — 提交后评估的 Unavailable 与精确 Retry.

**Status:** ready-for-agent

- [ ] 新建 subject-neutral 的 Evaluation Profile、`evaluation.rationale-assessment@1.0.0` 与共享的 rationale-sufficiency Verification Skill；它们复用现有 Manifest、Registry、Loader、SemVer 与 content hash，但不复用 Teaching Bundle Stack。
- [ ] Rationale Evaluation Context 仅投影允许的 learner task、完整 rationale、rationale-relevant Task Rubric、必要私有 expected-answer facts、限定 Source Passages 与 Learner Locale；它排除主答案、Trusted Primary-Answer Check、既有评估、反馈、Learning State 和 generator reasoning。
- [ ] 已证明错误且首轮得到 `not_applicable` 的理由只进行一次模型调用，形成 Conclusive Diagnostic Gap，并以可消毒 Feedback Facts 让 Workflow Guard 只提供 Explain 与 Apply Practice；不创建 Evidence 或 Independent Test。
- [ ] Evaluation Profile、Skill、Context 与 `rationale_evaluation/v1` 的结构测试证明没有 calculus、derivative 或 polynomial 专属契约/指令；技术失败使用 03 的 Unavailable 与 Retry 路径。
