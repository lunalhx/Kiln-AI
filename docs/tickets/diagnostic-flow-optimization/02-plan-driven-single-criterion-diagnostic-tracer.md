# 02 — Plan 驱动的单准则 Diagnostic tracer

**What to build:** 对无 Required Supporting Concept、单项 Target Readiness Set 的冻结 Plan，学习者完成一项 Diagnostic 后由新的 Diagnostic Routing Decision 安全地进入 Fresh Independent Test 或 Target Learning and Practice，破坏性替换旧的一题式路由。

**Blocked by:** 01 — Gate-accepted Diagnostic Plan 启动 Learning Flow.

**Status:** ready-for-agent

- [x] 正向确认唯一 Target readiness criterion 后，交付 Neutral Transition 和已生成、已 Gate、已验证的 Fresh Equivalent Independent Test；Diagnostic 不创建 Learning Evidence 或 Independent milestone。
- [x] Conclusive Diagnostic Gap 与 Unconfirmed Diagnostic Performance 作为不同的 Flow-scoped 事实持久化：前者进入带 learner-safe Diagnostic Summary 的 Target Learning and Practice，后者中性进入 Target Learning and Practice；两者均不作 Evidence 或 mastery claim。
- [x] 移除 `Diagnostic Not Passed` 及其旧路由、别名和响应映射，改由累计 Diagnostic facts 驱动 Routing Decision。
- [x] 提交、Finding、下一个 task 或 terminal interaction、checkpoint 和 processed command 原子提交；重放或崩溃恢复不重复 Attempt、Finding、task、transition 或 Evidence，且公开 API/UI 不泄露答案、解决方案或私有评估数据。
