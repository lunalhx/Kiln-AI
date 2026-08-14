# 06 — 破坏性切换为唯一的 Apply 产品路径并完成发布前验证

**What to build:** 产品只保留新的中文 Apply experience；旧 percent-change spike 及其泄漏风险、calculator 和兼容路径全部移除。

**Blocked by:** 01 — 编写并运行五个 Apply Bundle 与 Profile，交付已验证的 Diagnostic 任务; 02 — 提交一次 Diagnostic 并中性转入新鲜的 Independent 任务; 03 — 处理 Cannot Decide 与理由政策，保证评估隔离; 04 — 从 Independent 成功建立唯一的 Independent Evidence; 05 — 使完整 Apply flow 可持久恢复且幂等。

**Status:** done

- [x] 破坏性 Flyway reset 删除旧 spike schema、seed、checkpoint/envelope 数据路径；不迁移旧数据。
- [x] 删除旧 spike API/UI、trace endpoint、calculator、worked-example Bundle、generic Teaching Result Envelope、动态 Skill resolver 和相关测试。
- [x] 根界面和公开 API 只提供最终 Apply flow；旧路径不可访问，且没有公开 audit/trace 接口。
- [x] 更新冲突 ADR 为 superseded 或 clarification，使文档与最终 Spec 一致。
- [x] `ApplyProfileContractTest` 覆盖 Source Gap、一次失败后成功、两次失败、Diagnostic 两种通过方式、Independent 各结果、Cannot Decide 分歧和 least privilege。
- [x] 提供非 CI oracle、零工具的 real-model smoke test。
