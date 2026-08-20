# 04 — Apply Practice 的持久化 Hint Ladder

**What to build:** 打开中的 Apply Practice Attempt 支持渐进式提示；首次请求生成并校验完整 H1–H5 Ladder，之后只暴露已持久化的下一层。

**Blocked by:** 02 — Apply Practice remediation.

**Status:** done

- [x] H1–H4 保持 Attempt 打开、允许后续正式提交，并只记录实际暴露的 Assistance Trace。
- [x] H5 reveal 原子地记录帮助并将 Attempt 关闭为 Solution Revealed，绝不触发 Assessment 或 Evidence。
- [x] Hint 不可用于 Diagnostic、Independent、Review 或 Teach-back；Ladder 失败不暴露部分内容。
