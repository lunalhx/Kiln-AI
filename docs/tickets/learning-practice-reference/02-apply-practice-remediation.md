# 02 — Apply Practice remediation

**What to build:** Diagnostic 失败后，学习者获得经过验证的 Fresh Apply Practice Task；一次正式 Practice 提交的 PASS 会产生 assisted application Evidence，并使 Fresh Independent Test 合法。

**Blocked by:** 01 — Learning StateGraph 成功路径 tracer.

**Status:** ready-for-agent

- [ ] Practice 使用冻结的 Practice Blueprint 和独立 Attempt；正式提交后 Attempt 不能被改写。
- [ ] Practice PASS 和 FAIL 都产生 assisted application Evidence；Inconclusive 不产生 Evidence，改为 fresh replacement。
- [ ] 只有当前 remediation cycle 的 Apply Practice PASS 能使 Fresh Independent Test 合法。
