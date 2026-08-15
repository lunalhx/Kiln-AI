# 04 — Review PASS 推进 cadence 并达到 Durable

**What to build:** 学习者通过现有 Apply submission 完成有效无提示 Review 后，当前工作完成并调度下一次 Review；第四次连续通过后获得 Durable，且不再有后继工作。

**Blocked by:** 03 — 启动 Due Review 并交付新的等价任务.

**Status:** ready-for-agent

- [ ] Review submission 继续走既有 Apply endpoint；有效 no-hint PASS 原子地接受一条 Review PASS Evidence、完成当前 Review，并为 Review 1、2、3 分别在实际接受完成时间后 3、7、21 天创建后继。
- [ ] Review 4 PASS 不创建后继；Evidence 投影为 Current Milestone 与 Highest Milestone Reached 的 `DURABLE`，reference UI 显示该结果。
- [ ] 每次 Review 的 due time 均由该次实际完成时间计算，迟交不会压缩后续间隔。
- [ ] 同一 formal submission 只能贡献一次结果；重复提交和进程在 Attempt 关闭后重启都能恢复已保存的 submission 评估，且不会重复 Evidence、任务完成或 successor。
- [ ] 完整 1/3/7/21 天成功回路通过公共用例或 HTTP 合约验证，不暴露答案、assessment facts、reason codes 或 fingerprints。
