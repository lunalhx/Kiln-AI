# 05 — 双重佐证的理由救援与中性不确定性路由

**What to build:** 只有两次相互隔离的 Applicable Rationale 判断才能救援已证明错误的 Diagnostic 主答案并交付 Fresh Independent Test；评估分歧或语义不确定性会安全进入 Learning and Practice，且不声称学习者存在具体缺陷。

**Blocked by:** 04 — 首轮理由评估阻止空洞救援.

**Status:** ready-for-agent

- [ ] 仅当首轮为 `applicable` 时才运行 `evaluation.counterexample-review@1.0.0` 加同一 rationale-sufficiency Verification Skill；两轮使用同一 Flow-frozen Strong Model Binding 与相同允许事实，但第二轮看不到首轮结果。
- [ ] 两轮 `applicable` 经中性过渡交付 Fresh Independent Test，且不创建 Diagnostic Evidence；正常救援至多两次模型调用，两个责任均需一次 repair 的最坏路径至多四次。
- [ ] 首轮 `inconclusive`、第二轮 `not_applicable` 或第二轮 `inconclusive` 都成为 Unconfirmed Diagnostic Performance，使用中性 Feedback Facts，并只允许 Explain 与 Apply Practice；它们不得生成 learner deficit。
- [ ] `Cannot Decide` 只有既有主答案的两次隔离判断均确认正确时才通过；其他结果都不能借理由救援，且所有 Diagnostic 分支都不创建 Evidence。
- [ ] whole-flow、replay、Retry 和 PostgreSQL 重启测试覆盖两轮分支、contract repair 与状态/Evidence exactly-once；Independent、Practice、Review、Task Verification、Pedagogy 与 Clarification 回归保持通过，完整 `./mvnw clean test` 通过。
