# 01 — 编写并运行五个 Apply Bundle 与 Profile，交付已验证的 Diagnostic 任务

**What to build:** 学习者可启动一个中文 Diagnostic Apply flow，收到一题已验证的多项式求导题及 `f'(x)`、`理由（可选）` 字段。这个交付实际创建、加载并使用五个版本化 Apply Bundle 和其固定 Apply Profile，而不是复用旧 spike 的 Skill 或硬编码 prompt。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 创建并加载五个不可变、版本化的 `kiln.skill/v1` Bundle：Action、Reasoning、Representation、Verification、Subject；每个均声明 Slot、Profile compatibility、context requirements、资源、content hash 与 `tools: []`。
- [x] 实现 Apply Profile 的英文 immutable system instructions、固定五 Bundle Stack、Manifest/loader 和 namespaced prompt compiler；不再依赖动态 priority/tag resolver。
- [x] 仅 Action Bundle 声明 `apply_generation/v1` draft 字段；其余四个 Bundle 只约束生成，不可贡献或合并 draft 字段。
- [x] 模型调用将 system prompt 与闭合 execution-context JSON 分离，并拒绝未知字段、工具回调、model-owned learner events、Fingerprint、答案和 reasoning。
- [x] 仅当 Output Gate、表达式规范化、Fingerprint 生成和独立 Task Verification 均通过后，才原子保存 Package、打开 Diagnostic Attempt 并展示无私有字段的 learner projection。
- [x] Source Gap 或两次 generation cycle 失败时只返回中性 unavailable 状态，不产生 Attempt 或 Evidence。
- [x] Bundle/Profile focused tests 与脚本化启动 contract test 通过。
