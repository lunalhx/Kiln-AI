# 10 — 破坏性切换、transport-only adapter 与发布验证

**What to build:** 发布前只剩这一条 Learning 产品路径。模型 adapter 只做 transport；旧 direct-flow 写路径删除。ArchUnit、HTTP 旧端点 404、完整 `./mvnw clean test` 通过。live smoke 走 operator 解析的 Model Profile，非阻塞且不产生 Evidence。发布前提是 destructive fresh database/reset。

**Blocked by:** 05 — 模型契约失败闭合并按职责恢复; 09 — Reference UI 按已提交 Interaction 呈现完整生命周期.

**Status:** done

- [x] 每个真实 Model Port 都经 operator catalog 与冻结 Model Profile；adapter 不再解析或修补契约。
- [x] 旧 direct-flow 写路径与未再使用的兼容映射已删除；公开 HTTP 对旧 Apply 端点为 404。
- [x] ArchUnit 保持模块边界与 scheduler 无模型调用；whole-flow / HTTP / PostgreSQL / Playwright 契约均为绿色。
- [x] live smoke 非阻塞、使用 ephemeral state、至少覆盖一次真实生成与一次真实提交，且不创建 Evidence。
- [x] README / CONTEXT / 相关旧票状态与最终行为一致；不迁移旧 Flow 或旧 Review cadence。
