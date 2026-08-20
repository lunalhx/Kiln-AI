# 06 — Diagnostic、Teach-back 与 standalone Explain 只允许程序性澄清

**What to build:** 在 Diagnostic、Teach-back 或单独 Explain 上问实质性或不确定问题时，系统不给概念帮助、不改 Attempt purpose、不影响证据资格。只有程序性澄清会重述已展示的格式、记号或界面条件。Explain 的澄清对着当前 teaching interaction，不带 Attempt ID。

**Blocked by:** 01 — 收口为唯一 Learning 命令面，并记录 Blackboard / Review 取消决策.

**Status:** done

- [x] Diagnostic / Teach-back / standalone Explain 的程序性澄清只重述已展示条件，并留下可审计记录。
- [x] 实质性或 uncertain 请求不提供教学内容，不改变 Attempt purpose 或证据资格。
- [x] standalone Explain 的 `clarification_asked` 面向当前 Interaction Boundary，不要求 `attemptId`。
- [x] Practice / Independent / Review 既有 clarification 与 consent 规则保持不变。
