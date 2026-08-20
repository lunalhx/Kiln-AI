# 01 — Learning StateGraph 成功路径 tracer

**What to build:** 学习者通过新的领域 Learning Flow command seam 完成既有的 Diagnostic 通过、Independent Test 通过并安排 Review 1；全程由可暂停、可恢复的 Learning StateGraph 协调。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 成功路径在每个 learner interaction boundary 产生可恢复 checkpoint，并保留既有 Evidence 与 Review 1 行为。
- [x] 重放已完成命令返回原 interaction；Profile、Assessment 与 Pedagogy Agent 均不能直接写 Learning State。
- [x] ADR-0066 记录普通 Java graph runner 决定，且现有 Apply Profile contract tests 保持通过。
