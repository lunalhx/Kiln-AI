# 03 — 处理 Cannot Decide 与理由政策，保证评估隔离

**What to build:** 不确定或复杂的数学输入不会被误判为失败；系统公平地决定是否通过、重出题或安全结束。

**Blocked by:** 02 — 提交一次 Diagnostic 并中性转入新鲜的 Independent 任务。

**Status:** ready-for-agent

- [ ] 数学等价检查只返回 Proven Equivalent、Proven Not Equivalent、Cannot Decide；不支持或歧义语法绝不猜错。
- [ ] Cannot Decide 时，Assessment 与 Response Verification 使用相同原始/确认输入、彼此隔离；只有两者都判 equivalent 才通过最终答案通道。
- [ ] 分歧或任一非 equivalent 结果为 Inconclusive：不显示失败反馈、不接受 Evidence，并要求准备 fresh Independent task。
- [ ] Diagnostic 的适用理由可独立通过；Independent 的空白或非实质理由不阻断，明确矛盾理由会阻断 Evidence。
- [ ] 所有 assessment/verification 输出均为闭合 typed contracts，不能直接修改 Flow State 或接受 Evidence。
