# Kiln-AI 代码地图

> 用途：让你在不读全部 20k 行的情况下，掌握每条流程从 HTTP 到持久化的完整调用链。
> 约定：所有引用都是 `文件:行号`，主文件在 `kiln-ai-*` 模块下，路径省略包前缀。
> 配套：README（模块职责）、CONTEXT（术语表）、docs/adr（决策基线）、docs/specs（规范）。

---

## 0. 一页纸心智模型

- **这是一个「验证系统」不是「聊天系统」**：学习者每步输入都是一条封闭命令（`answer_submitted`/`hint_requested`/…），每个模型输出都要过确定性门控，证据只在独立完成时产生。
- **一条主干**：HTTP → Controller → UseCase（幂等重放）→ LearningStateGraph（按 Attempt Purpose 路由）→ Flow → ProfileExecutor（有界生成+门控）→ Store（原子提交）→ 学习者可见投影。
- **两个不变量贯穿一切**：exactly-once（命令重放 + 事务原子提交，ADR-0063）与 fail-closed（模型契约不合法即修复一次→Unavailable，ADR-0071）。

---

## 1. 模块与依赖（hexagon）

```text
kiln-ai-app(装配/运行)
  -> kiln-ai-trigger(HTTP 控制器/调度) -> kiln-ai-api(请求响应 DTO)
  -> kiln-ai-infrastructure(Postgres/MyBatis/模型适配/Skill 加载)
  -> kiln-ai-domain(聚合/值对象/门控/状态机) -> kiln-ai-types(错误码)
```

- **domain 纯净**：不依赖 Spring/MyBatis/AI SDK，由 `DomainArchitectureTest`（domain 测试）强制。输出端口在 `domain/apply/port/`，infra 在 `infrastructure/adapter/` 实现。
- **主代码规模**：domain 16,333 行 / 179 文件（85 个 record 值对象）；infra 3,082 / 15；trigger 553 / 5；app 671 / 5。测试 20,417 行（≈1:1）。
- 规模分布：`LearningStateGraph.java` 1,776 行（唯一超大文件）、`InMemoryLearningFlowStore` 613、`TeachBackFlow` 370。

---

## 2. 装配与启动

| 组件 | 位置 | 作用 |
|---|---|---|
| `KilnAiApplication` | `kiln-ai-app/.../KilnAiApplication.java:17` | `main`，`@EnableScheduling`(:9)，读 `.env` |
| `DotEnv.read()` | `kiln-ai-app/.../DotEnv.java:14-16` | 解析 `deploy/local/.env`（从 CWD 向上找 6 层，:36-46） |
| `ApplyFlowConfiguration` | `kiln-ai-app/.../config/ApplyFlowConfiguration.java` | 装配各 BundleStack(:177-219)、各 Flow/UseCase、复习调度(:366-373)；`catalog.enabled=false` 时注册全端口 fail-closed lambda(:70-174) |
| `OperatorModelConfiguration` | `kiln-ai-app/.../config/OperatorModelConfiguration.java` | `OperatorCatalog`(:42-44) + `ApplyModelAdapter`(:46-50) + `OperatorModelProfileAdapter`(:53-55)；判断类端口包装 `Xxx.parse`(:63-117) |
| `DomainServiceConfiguration` | `kiln-ai-app/.../config/DomainServiceConfiguration.java` | 复习 UseCase / 进度投影装配 |

---

## 3. HTTP 入口

| 路由 | 方法 | 位置 |
|---|---|---|
| `POST /api/learning/flows` | start | `LearningFlowController.java:52-60` |
| `GET /api/learning/flows/{flowId}` | query 已提交状态 | `LearningFlowController.java:62-65` |
| `POST /api/learning/flows/{flowId}/commands` | 统一命令入口 | `LearningFlowController.java:67-121` |
| `GET /api/review-tasks?learnerId=` | 复习列表 | `ReviewTaskController.java:59-71` |
| `POST /api/review-tasks/{reviewId}/start` | 开始复习 | `ReviewTaskController.java:73-84` |
| `POST /api/review-tasks/{reviewId}/cancel` | 取消复习 | `ReviewTaskController.java:86-88` |
| 全局异常映射 | — | `GlobalExceptionHandler.java` |

命令分发（`LearningFlowController.java:74-96` 的 switch）→ UseCase 各方法；返回的 `LearningFlowResult` 由 `LearningFlowResponseMapper`(:99) 投影成学习者可见的封闭交互联合（`task`/`teaching`/`assistance_consent`/`transition`/`unavailable`），绝不泄露答案/来源/指纹/执行轨迹。

DTO 契约在 `kiln-ai-api`（`LearningFlowCommandRequest`、`LearningFlowResponse`、`StartLearningFlowRequest` 等）。

---

## 4. 命令主干（每条命令都走这条）

```
Controller:67
  → LearningFlowCommandUseCase.xxx()        # LearningFlowCommandUseCase.java:51/79/100/125/153/179/206/227
    → FlowCommandReplay.replayOrRun()        # apply/flow/FlowCommandReplay.java:35-40 幂等重放：
                                            #   旧 key 返回原已提交交互；同 key 不同 payload → CONFLICT
    → LearningStateGraph.xxx()              # 图运行入口
```

UseCase 各方法（都先算 `ApplyHash.sha256HexDelimited(...)` 请求哈希，再进重放边界）：

| 命令 | UseCase 方法 | 行号 |
|---|---|---|
| start | `start` | `:51-77`（resolveProfile :246 → graph.start） |
| answer_submitted | `submitAnswer` | `:79-98` |
| continue_requested | `continueRequested` | `:100-111` |
| hint_requested | `requestHint` | `:125-141` |
| clarification_asked | `clarificationAsked` | `:153-169` |
| assistance_decided | `assistanceDecided` | `:179-195` |
| flow_control_requested | `flowControlRequested` | `:206-217` |
| retry_requested | `retryRequested` | `:227-238` |

---

## 5. LearningStateGraph —— 图运行器（唯一超大类）

`domain/learning/graph/LearningStateGraph.java`（1,776 行）。每个 learner 命令 = 一次 Graph Run：**先重读已提交状态（rehydrate）→ 校验交互版本/Attempt 归属 → 路由到唯一合法节点 → 原子提交下一个交互边界**。

| 方法 | 行号 | 干什么 |
|---|---|---|
| `start` | `:217-242` | 先 prepareDiagnostic（生成+门控+验证全完成，不落库），成功才 `bindStart` 原子提交（Flow/包/Attempt/曝光/交互/检查点/命令，ADR-0063） |
| `submitAnswer` | `:250-295` | rehydrate(:262) → 版本冲突(:263) → Unavailable 拒收(:266) → Attempt 归属(:269) → **按 `AttemptPurpose` 路由**(:278-291) |
| `continueRequested` | `:353` | 显式继续，进 `executeMove`(:1256) |
| `requestHint` | `:385` | → `HintFlow.requestHint` |
| `clarificationAsked` | `:435` | → ClarificationGate 分类(:526-606) |
| `assistanceDecided` | `:609` | 接受帮助 → 独立/复习 Attempt 单向转 Practice(:726) |
| `flowControlRequested` | `:660` | 离开：开着的 Attempt 标记 Abandoned（ADR-0015） |
| `retryRequested` | `:692` | 恢复 Pending Operation(:1666-1698) |
| 提交路由 | `submitDiagnostic:928` / `submitIndependent:975` / `submitReview:1052` / `submitPractice:1096` / `submitTeachBack:1150` | 各自进入对应 Flow |
| `executeMove` | `:1256` | Guard(:102) 先给合法 move 集合 → PedagogyPlanner.plan(:44) 只在集合内选；只有一个合法 move 时 Guard 直接绕过模型 |
| `commitBoundary` | `:1761` | 唯一原子提交点 → `flowStore.commitBoundary` |

**路由原则**：`submitAnswer` 的 switch（`:278-291`）按 Attempt Purpose 选节点，不按「学习者说了什么」——这就是「把判定当证据而非聊天」的落点。

---

## 6. 各 Flow（按 Attempt Purpose 走查）

所有 Flow 在 `domain/apply/flow/`。每个提交类 Flow 都是「closeAttempt → assess → route」三段式，且都带 `recoverOrIgnore`（崩溃后从已提交 Attempt 恢复，不重跑）。

### 6.1 诊断 `DiagnosticFlow.java`
- `startDiagnostic` :78；`submitDiagnostic` :108；评估 `:155-176`（`AssessmentRunner` + Rationale 评估）；`failureFacts` :177、`neutralFacts` :187。
- 通过 → 中性过渡到 Fresh Independent Test；未通过 → 只允许 Explain / Apply Practice（WorkflowGuard）。

### 6.2 练习 `PracticeSubmissionFlow.java`
- `deliverPractice` :91 / `deliverIndependent` :104；`submitPractice` :110；`recoverOrIgnore` :142；`assessAndReturn` :152；`evidenceCandidate` :171。
- 练习通过 → 达到合格条件才放行独立测试。

### 6.3 提示 `HintFlow.java`
- `requestHint` :74。**一次模型调用生成整条 H1–H5 阶梯**（javadoc :38），过 HintLadderGatePolicy 后一次性持久化稳定阶梯，之后逐级确定性揭示；H5 → Solution Revealed 关闭 Attempt。跳级请求答案直达 H5。

### 6.4 Teach-back `TeachBackFlow.java`
- `deliverTeachBack` :95；`submitTeachBack` :114；`recoverOrIgnore` :224；`assessAndReturn` :234。锚定 Explain 或 H5 暴露内容（`:132-164`）。

### 6.5 独立测试 `IndependentSubmissionFlow.java`
- `submitIndependent` :82；`recoverOrIgnore` :113；`assessAndAcceptEvidence` :123；`acceptPass` :152（原子接受证据 + 安排第一个复习，:`167`）；`failureFacts` :204。
- 无提示独立通过 → Independent Learning Evidence → 安排 1 天后复习。

### 6.6 复习
- `ReviewStartFlow.start` :63（`replayOrRun` :66；prepareTask 用全 Exposure Ledger 做 novelty 排除，:82-88；`bindReviewAttempt` 原子 claim，:90-91）。
- `ReviewSubmissionFlow.submitReview` :105；`recoverOrIgnore` :142；`assessAndAdvanceCadence` :163；`acceptReviewPass` :189（推进 cadence）；`failAndStopCadence` :210（结论性失败停止 cadence，ADR-0061 答案-理由矛盾也在此，:174-181）；Inconclusive → `resolveInconclusive` :261（准备 Fresh Equivalent 替换）。

### 6.7 Explain `ExplainFlow.java`
- `deliverExplain` :53，仅教学不评估。

---

## 7. 评估与门控

### 7.1 评估 `domain/apply/flow/AssessmentRunner.java`
- `run` :89-169：先跑**确定性** `MathematicalEquivalenceCheck`(:106，证明有界，永不靠模型判对错)，构建 `ResponseAssessmentContext`(:107-114)；确定性无法判定时才调模型 `assessmentPort.assess`(:154)；`CANNOT_DECIDE` 再独立 `verificationPort.verify`(:159-163)。
- 评估结果**先持久化后路由**：`loadOrCommit` → `saveOrReturnCommittedEvaluationResult`(:222-227，幂等 key 是 `(attempt, responsibility, evaluation_version)`)。

### 7.2 共享门控管线 `domain/gate/`
- `TypedArtifactGatePipeline.validate` :5-13（只做 null 检查后委托 policy）；`GatePolicy`（函数式接口）、`GateResult`（PASSED/REPAIRABLE/REJECTED）、`GateOutcome`、`GateContext`、`GateViolation`。
- 具体 GatePolicy 在 `domain/apply/gate/`：`ApplyTaskPackageGatePolicy`（schema/答案域/隐私泄露/novelty/来源/指纹，evaluate :46-102）、`ExplainGatePolicy`、`TeachBackTaskPackageGatePolicy`、`HintLadderGatePolicy`（H1-H4 不得泄题、H5 必须等价于期望答案）、`ApplyGenerationDraftGatePolicy`、`ExplainGenerationDraftGatePolicy` 等。
- **「一次允许修复」**：没有独立 `ValidatedNodeExecutor` 类——它就是 Executor 里的 2 轮循环（`ApplyProfileExecutor.java:103-123`）+ 提交后评估的 `ModelContractRepair.once`（`apply/flow/ModelContractRepair.java:26-72`）。

### 7.3 教学决策 `domain/learning/`
- `WorkflowGuard.derive` :102 → 合法 move 集合；`PedagogyPlanner.plan` :44 → 只在集合内选一个；`ClarificationGate.classify` :36 → 程序性/实质性分类。

---

## 8. 模型调用层（infra）

| 组件 | 位置 | 作用 |
|---|---|---|
| `ApplyModelAdapter` | `infra/.../adapter/model/ApplyModelAdapter.java:45-46` | **唯一真实模型适配器**，直接实现 5 个 domain 端口；`complete` :413-443 是唯一 HTTP 路径（读 secret → 建 ChatClient → `.system().user().call()`）；**只返回原始字符串，不解析 JSON**（javadoc :39-41） |
| `OpenAiCompatibleChatClientFactory` | `infra/.../adapter/model/OpenAiCompatibleChatClientFactory.java` | `create` :7，校验协议，构造 OpenAI 兼容 client，**附加 JSON_OBJECT responseFormat**(:13-15, :36-43) |
| `OperatorCatalog` | `infra/.../adapter/model/OperatorCatalog.java` | `resolve` :55-66 / `bind` :77-111：`providerId/modelId` 解析、校验模型在列、secret 只留环境变量名（值不进 profile） |

**关键：契约解析在 domain**。`ModelContract`（`domain/apply/model/ModelContract.java:20-110`）严格解析 JSON；生成类端口由 domain Executor 内部 parse（如 `ApplyProfileExecutor.java:135`），判断类端口由 app 装配 lambda 包装 `Xxx.parse`（`OperatorModelConfiguration.java:63-117`）。

**冻结**：ModelProfile 在 Start 时解析并随 Flow 落库（`flows.model_profile JSONB`，V2 迁移），此后所有调用用它，永不查当前配置（ADR-0035）。

---

## 9. 持久化层（infra）

### 9.1 适配器
| 类 | 实现端口 |
|---|---|
| `PostgresApplyFlowStore.java:60` | `LearningFlowStore` + `ArtifactStore` + `ReviewTaskStore`（MyBatis `ApplyFlowMapper` 承载全部 SQL） |
| `InMemoryLearningFlowStore`（domain） | 测试用内存实现，`:37` 同实现三个端口 |
| `UuidTypeHandler` | UUID 类型处理 |

选型：`KilnAiPersistenceAutoConfiguration` 有 `DataSource` 才注册 Postgres，否则内存实现（:28-44）。

### 9.2 核心表（`V1__learning_flow_schema.sql` + 后续迁移）
`flows`(根) · `interactions`(UNIQUE(flow_id, interaction_version)) · `checkpoints` · `commands`(idempotency_key PK + request_hash + replayed response) · `sources` · `packages`(双投影 learner+assessor) · `attempts`(task_package_id UNIQUE) · `verifications` · `evidence`(task_attempt_id UNIQUE) · `exposures`/`example_exposures`/`hint_ladder_exposures`/`revealed_solution_exposures`(novelty 曝光账本) · `explain_artifacts` · `hint_ladders`/`hint_requests` · `teach_back_anchors`/`teach_back_packages` · `review_tasks`(部分唯一索引 `review_tasks_one_unfinished_per_learner_concept`)。

迁移链：V2 冻结 ModelProfile → V3 interaction kind → V4 每 learner+concept 一个 active flow → V5 pending_operations → V6 model_contract_audits → V7 复习取消幂等 → V8 active_learning_work claim → **V9 删除旧 append-only assessments 表，建 `evaluation_results`(UNIQUE(attempt_id, responsibility, evaluation_version))**。

### 9.3 exactly-once 在 SQL 里的落点
`commands` `ON CONFLICT (idempotency_key) DO NOTHING`（mapper :237-242）；`interactions` 去重 :101-113；`claimActiveWork` `ON CONFLICT (learner_id, concept_id) DO NOTHING` :37-48；evidence `UNIQUE(task_attempt_id)` :564-574；评估检查点 V9 :521-539；所有状态更新都带条件 WHERE（`status='OPEN'` 等）使重放/竞争变成 no-op。

### 9.4 复习调度（无模型调用）
`ReviewDueScheduler.java:24-25`（`@Scheduled(fixedDelay=60_000)`，无 cron）→ `ReviewDueTransitionUseCase.markDueReviewsDue` → `markDueReviewsDue(now)`（仅 SCHEDULED→DUE，幂等）。ArchUnit `schedulersNeverTouchApplyOrModel` 强制调度器不碰模型。

**Cadence 单一事实源**：`ReviewTaskScheduler.java:28` FIRST=24h、`:34-37` SUCCESSOR=[3,7,21] 天；`ConceptProgressProjector.java:22` QUALIFYING_REVIEW_COUNT=4（第 4 次复习不再排后续）。

---

## 10. Skill Bundle 基建

- **加载**：`infra/.../adapter/bundle/BundleLoader.load(pinnedId)` :19-56 → 读 `skills/{id}@{version}/SKILL.md`、拆 YAML frontmatter、校验 id/version 与 pin 一致（:47-54）、整文件 SHA-256 作为内容 hash（`SkillBundleSource.toBundle` :17-19）。
- **组合**：`domain/apply/bundle/BundleStack`（构造强制恰好一个 ACTION 槽，:10-23）；每个 Profile 的 `FIXED_STACK` 在 `ApplyProfile.java:9-15`、`ExplainProfile.java:9-12`、`TeachBackProfile.java:26-29`、`RationaleEvaluationProfile`/`CounterexampleReviewProfile`。
- **Bundle 文件**：`kiln-ai-domain/src/main/resources/skills/`，11 个（5 槽：action/reasoning/representation/verification/subject + 评估用 evaluation/verification）。`subject.calculus-notation` 同时存在 0.1.0 与 1.0.0 两个不可变版本。
- **编译**：`ApplyPromptCompiler.compile` :25-42 按固定槽序拼指令并查预算；`validateStack` :49-64 拒绝「非 action 声明 output_contribution / 声明工具 / 与 profile 不兼容」。

---

## 11. 测试地图（回归安全网）

| 测试 | 位置 | 作用 |
|---|---|---|
| `ApplyProfileContractTest`（38 用例） | domain | **稳定回归 oracle**：脚本化生成/验证/评估，无 live model |
| `ExplainProfileContractTest` / `TeachBackProfileContractTest` / `RationaleEvaluationContractTest` | domain | 各 Profile 契约 |
| `LearningFlowGraphContractTest`（124 用例） | domain | whole-flow 状态机行为 |
| `DelayedReviewCadenceContractTest` / `ReviewStartFlowTest` / `InconclusiveReviewReplacementContractTest` | domain | 复习 cadence |
| `DomainArchitectureTest`（ArchUnit） | domain | domain 不依赖框架 |
| `SpringAiIsolationArchitectureTest`（ArchUnit） | app | Spring AI 类型只进模型适配器；调度器无模型调用 |
| `ApplyPostgresRecoveryTest` / `LearningFlowPostgresRecoveryContractTest` / `LearningFlowPostgresSuccessPathTest` | app | 崩溃恢复 / exactly-once（需 Docker） |
| `LearningFlowHttpTest` / `LearningFlowUiTest` | app | HTTP 契约 + 参考 UI |
| `ApplyProfileLiveSmokeTest` | app | 非阻塞 live 冒烟，`KILN_LIVE_SMOKE=true` 才跑，无 evidence |

---

## 12. 重复簇定位（去重候选，先读地图再动手）

改任何一段前，先确认这里是不是「多份副本」：

1. **Prompt Compiler ×3**：`ApplyPromptCompiler.java:25-64` / `ExplainPromptCompiler.java:22-67` / `TeachBackPromptCompiler.java:22-67` 结构逐行相同，仅 FIXED_SLOT_ORDER、profile 名、draft 名不同。`HintPromptCompiler` 无 stack，独立。
2. **Executor 有界生成循环**：`ApplyProfileExecutor.java:81-198` / `ExplainProfileExecutor.java:69-149` 骨架相同（2 轮循环 + handleCandidate + validateContextCoverage）。`TeachBackProfileExecutor` 与 `RationaleEvaluationProfileExecutor` 同族。
3. **提交 Flow 的 `recoverOrIgnore` ×5**：`DiagnosticFlow.java:140` / `PracticeSubmissionFlow.java:142` / `IndependentSubmissionFlow.java:113` / `ReviewSubmissionFlow.java:142` / `TeachBackFlow.java:224`；`failureFacts` 在 `DiagnosticFlow:177` 与 `IndependentSubmissionFlow:204` 重复。`AssessmentRunner` / `SubmissionCloser` / `ModelContractRepair` 已是共享助手，本可以收纳这些。

> 去重安全网 = 第 11 节的契约测试。每改一步 `./mvnw clean test` 全绿再继续。
