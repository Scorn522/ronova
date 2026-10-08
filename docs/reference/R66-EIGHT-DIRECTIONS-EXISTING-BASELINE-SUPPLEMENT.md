# R66 · 第二批八方向补遗：已有实现清单

**性质：** 对 fix10 已采纳的八方向（S1–S4 / A1–A4）**补一份"已有实现"基线**。**不新增方向、不新增批次、不改 fix10 的任何决策或编号。**
**方向：** Ronova 侧 → 外部研究方 / 实施方（**发出**）
**为什么需要它：** fix10 建立在 Ronova 侧核验稿之上，而**那份核验稿有两处盲区（S2、A1）fix10 未独立补上**（其复核证据文件中 `CopyProvenanceIndex`、`NbtWriteReferences`、`StorageMutationSequence`、`DeferredReceiptBuffer` **命中均为 0**）。直接实施会导致**重建已有机制**。

**本轮方法变更（重要）：** 前几轮的核验用的是"按猜测标识符 grep"，连续三次漏检（`ClassLineageResolver`、`EVIDENCE_LOST`、`NbtWriteReferences`）。**本轮改为先枚举子系统，再读每个类的类级说明。** `observation` 包 41 个类**全部读过类级说明**。

---

## 0. 最重要的发现：一条成文的架构纪律

**整个代码库里有一个反复出现、写进类级 javadoc 的显式立场：观察不授权。**

在 `observation` / `integrity` / `budget` / `runtime` / `surface` 五个包中，**约 28 个类**带着这类免责声明。抽样原文：

| 类 | 原文 |
|---|---|
| `CopyProvenanceIndex` | "its answer is **neither a Judgment nor permission to write**" |
| `NbtStoragePreservation` | "Positions require separate production provenance; **no write authority is granted**" |
| `StorageMutationSequence` | "Observation stamp only; it **never** locks a foreign cache or **grants permission to write it**" |
| `SavedDataEncodedWrite` | "Bounded digest of bytes accepted by the actual compressed output stream, **not a commit receipt**" |
| `SavedDataSaveTrace` | "Bounded advisory transcript of the vanilla save boundary. **Never a durable commit receipt.**" |
| `RecoveryFileReadback` | "Raw image equality **is not** a durability, ownership or semantic receipt" |
| `RecoveryLocationResolver` | "Results **never** prove recovery causality or **authorize a write**" |
| `RecoveryStorageReadbackQuery` | "**No new intent, write permission or completion fact is created**" |
| `ReflectionFieldLayout` | "Short-lived discovery metadata only; **never an ownership witness or write permission**" |
| `StorageAdapterFingerprint` | "Artifact fingerprint for persisted plan compatibility; **not an attestation of executing instructions**" |
| `RecoveryBoundaryProof` | "a direct call to the public observer **is not a boundary event**" |
| `TaskExecutionGraph` | "Volatile execution evidence. **Root alone owns durable intents and permissions.**" |
| `PlanExecutionLease` | "Short-lived, thread-confined permission for one exact Root plan. **Never grants Root authority.**" |
| `PhysicalIncarnationTracker` | "Observation nonce only. **Root alone assigns** a qualified incarnation and binding revision" |
| `CompletionTracker` | "**Never** changes a qualification or resolves an effect" |
| `OperationPlanRunner` | "ports **cannot** renew a stale Root plan or choose another effect identity" |
| `HostResourceGovernor` | "Accounting only for registered allocations. It **never removes Truth or infers opponent ownership**" |
| `CodeIntegrityProof` | "Evidence for registered class images only; it **does not attest** arbitrary memory or JIT targets" |
| `RawAccessorDispatch` | "Verifying a base method body **does not verify** a subclass's virtual override" |
| `SurfaceAdapter` | "Later execution ports require actual Root plans and receipts, **not a claimed level**" |
| `SurfaceRegistry` | "an observation-only adapter **cannot claim** enforcement or FULL" |

### 对八方向的直接含义

**八条里有四条（S2 / S3 / A1 / A4）本质上是"在观察层之上加授权"。** 而现有观察层是**刻意建成不授权的**，并且把这个立场写进了每个类的契约。

**因此三条硬约束：**

1. **新的授权语义必须落在 policy / root 层**，不能落进 `observation` 包的现有类；
2. **现有观察类的免责声明必须保持原样** —— 不能为了给 S2/S3 提供"证据"而把某个观察类的 javadoc 改成"现在它会授权了"；
3. **授权要引用观察结果，而不是把观察类本身升级** —— 即 `Root plan` 读 `NbtWriteReferences` 的 `Snapshot`，而不是让 `NbtWriteReferences` 自己变成许可。

**违反这三条会破坏一个横跨 28 个类的既有契约。**

---

## 0.5 第二项关键发现：**S2 与 S3 已有为 7.17 预留的接缝**

`root` 包里存在两组**具名抽象 + 显式 7.16→7.17 接缝**。这不是"类似做法"，是**已经为当前阶段设计好的扩展点**。

### 写入侧（对应 S2）

| 类 | 行 | 原文 |
|---|---|---|
| **`WriteSinkId`** | 18 | "Stable identifier for an **Authority-bearing final write boundary**" |
| **`WriteSinkEvidence`** | 36 | "Bounded evidence emitted by a **registered final write gate**" |
| **`WriteSinkExtension`** | 6 | "**7.16 seam; production sink evidence begins in 7.17**" |
| `DominionCommand` | 282 | "Loader-neutral mutation vocabulary. **Every durable write enters through this boundary.**" |
| `EffectKey` | 41 | "Stable semantic effect identity. Plan revisions and execution attempts are deliberately absent." |
| `RootExecutionFence` | 73 | "Excludes new canonical commits during one registered raw-write step of this Root instance." |

### 完成侧（对应 S3）

| 类 | 行 | 原文 |
|---|---|---|
| **`CompletionContractId`** | 18 | "Stable identity for a non-CLEARED operation's **finality contract**" |
| **`CompletionContractExtension`** | 7 | "**7.16 seam; production completion finality begins in 7.17**" |
| **`CompletionEvaluation`** | 31 | "Current, revisioned evaluation; **it is evidence and never a second Truth**" |
| `OperationReport` | 26 | "Execution evidence is **separate** from committed Truth and from whole-operation completion" |
| `EffectRecord` | 129 | "Durable execution fact. It neither issues Judgment nor proves arbitrary external effects." |

### 这两组发现的含义

**S2 与 S3 不是"从既有模式受启发"，而是"有指定接缝的待实施工作"。** 两处 seam 的注释都写着生产实现"**begins in 7.17**"——**当前阶段**。

**因此对实施者：**

- **S2 应接入 `WriteSinkId` / `WriteSinkEvidence` / `WriteSinkExtension`**，不要另造写入边界抽象；写集声明应附着在这组接缝上。
- **S3 应接入 `CompletionContractId` / `CompletionContractExtension` / `CompletionEvaluation`**，不要另造完成语义。
- 两组接缝都**已声明"是证据、不是第二 Truth"**——与 §0 的"观察不授权"纪律一致，新增授权仍须走 Root。

---

## 1. 逐方向的已有实现清单

### S2 · Write-Set —— **已有"实际写入"的完整一侧，缺"声明"侧与比对**

| 类 | 行 | 说明原文 / 关键结构 |
|---|---|---|
| **`NbtWriteReferences`** | 40 | "**Actual writer traversal**; published entries retain neither holders nor their object graphs."<br>`record Entry(WeakReference<Object> value, List<NbtStoragePreservation.Step> path)`<br>**`record Snapshot(boolean complete, List<Entry> entries)`** ← 带完整性标志 |
| `StorageMutationSequence` | 31 | "**Observation stamp only**; it never locks a foreign cache or grants permission to write it." |
| `NbtEncodingPaths` | 79 | "Bounded observation of references in the **actual writer input**; never infers provenance from NBT values." |
| `StorageMutationTracked` | 8 | "Implemented by the registered `DimensionDataStorage` mixin. **Missing instrumentation is unknown.**" |
| `StorageBindingObservation` | 18 | `enum { BOUND, DET… }` —— "An unstable read blocks execution but does not prove the previously observed holder disappeared." |
| `SavedDataEncodedWrite` | 101 | "Bounded digest of bytes accepted by the **actual compressed output stream**, not a commit receipt." |

**结论修正：** 前稿把 S2 定性为"没有完整统一形态"，**方向正确但基线偏低**。实际已存在：
- 一条**实际写入遍历**（`NbtWriteReferences`，带 `complete` 标志）
- 一个**观察戳**（`StorageMutationSequence`，**且明确声明自己永不授权**）
- 一条**mixin 侧插桩接口**（`StorageMutationTracked`）

**缺的是**：① 声明式 `allowedSurfaces/Objects/Operations`；② declared-vs-actual 比对；③ 未声明 mutation 的 fence 链路。
**不要重建 writer traversal —— 用 `NbtWriteReferences.Snapshot`。**
**不要另造写入边界抽象 —— 用 `WriteSinkId` / `WriteSinkEvidence` / `WriteSinkExtension`（见 §0.5）。**

---

### S3 · Independent Witness —— **已有"独立观察"的完整一侧，且全部声明"不是回执"**

| 类 | 行 | 说明原文 |
|---|---|---|
| `RecoveryFileReadback` | 111 | "Read only one observed location. Raw image equality is **not** a durability, ownership or semantic receipt." |
| `RecoveryStorageReadbackQuery` | 32 | "Query one acknowledged original intent. **No new intent, write permission or completion fact is created.**" |
| **`DeferredReceiptBuffer`** | 95 | "**Reserve result retention before starting a mutation; retry only the receipt, never the mutation.**" |
| `RecoveryBoundaryProof` | 54 | "Actual registered caller frames; a direct call to the public observer is not a boundary event." |
| `SavedDataSaveTrace` | 77 | "Bounded advisory transcript of the vanilla save boundary. **Never a durable commit receipt.**" |
| `RecoveryMutationIntent.Observation` | — | 位掩码 `WRITE_OBSERVED(1) / WRITE_UNVERIFIED(2) / NOT_CURRENT(4) / NOT_EXECUTED(8)`；**OR 累积**（`:58`） |

**这一侧比前稿描述得更完整。** 而且 `DeferredReceiptBuffer` 的"**只重试回执,不重试变更**"正是 S3 需要的纪律,已实现。

**关键张力：** S3 要的是**认证**（`RESULT_VERIFIED`），而现有每个观察类都**明确声明自己不是回执**。**所以 S3 不是"把现有读回升级成认证"，而是"在 Root 层新增一个消费读回结果的认证环节"。**

**接缝已指定：** `CompletionContractId` / `CompletionContractExtension`（"production completion finality begins in 7.17"）/ `CompletionEvaluation`（"evidence and never a second Truth"）。**接入这组，不要另造完成语义。**

---

### S4 · Observation Gap —— **已有连续性概念与失证传播，缺跨源序列/代际**

| 类 | 行 | 说明原文 / 关键结构 |
|---|---|---|
| **`TaskEventRun`** | 37 | "**Consecutive**, already receiver-verified events for one task. **Never spans another task, a copy event, a different parent/scheduler, or a changed proof.**" ← **这就是连续性语义** |
| **`RecoveryTaskObserver`** | 254 | 失证链 `:173-189`（install 失败 / `LOST` / proof-changes-dropped 异常 → `graph.evidenceLost()`）<br>原因与诊断 `:211-227`（`lossReasons()` 5 种 / `captureDiagnostics()` 5 项）<br>inbox 满 = reason 3（`:227`） |
| `TaskExecutionGraph` | 236 | `enum QueueState { …, EVIDENCE_LOST }`（`:10`）<br>`executionsExited()` 要求 `!evidenceLost`（`:18`）<br>`if(node.evidenceLost())return false;`（`:143`）← **依此拒绝执行退出** |
| `RecordRetrySchedule` | 11 | "Per-source retry cadence; it does not withdraw qualification or suppress other permitted paths." |
| `BootstrapTaskHooks` | 112 | `droppedEvents()`（`:78`） |

**结论修正：** 前稿说"只有 `BootstrapTaskHooks:78` 一个计数" —— **错误**（fix10 已纠正）。实际有一条完整的失证链 + **一个拒绝跨断裂的事件连续性类**。

**缺的是：** ① 跨源 `sourceGeneration` / `sequence` / `overflowGeneration`；② 按依赖 scope 撤销；③ 重新建立基线窗口。
**沿用 `TaskEventRun` 的"不跨断裂"语义 —— 不要另立一套连续性概念。**

---

### A1 · 证据依赖图 —— **已有关系索引一族，缺可传播撤销的 DAG**

| 类 | 行 | 说明原文 |
|---|---|---|
| **`CopyProvenanceIndex<K>`** | **248** | "Object-identity evidence for an observed seed and its actual copies. Equal content, UUIDs and class names **never** create a relationship. **This index observes data flow**; its answer is neither a Judgment nor permission to write." |
| `CompletionTracker` | 88 | 依赖快照（fix10 `:409` 已提） |
| `TaskExecutionGraph` | 236 | "Volatile execution evidence. **Root alone owns durable intents and permissions.**" |
| `TopologyGraph` | 395 | "Per-server weak raw graph. **Terminal state is deliberately outside this type.**"（实体拓扑，**非证据图**） |

**结论修正：** 前稿说"`TopologyGraph` 与 `TaskExecutionGraph` **都不是**证据图" —— **不完整**。`CopyProvenanceIndex`（248 行）**是一个数据流来源索引**，正是 A1 的同类。

**缺的是：** 从 grant → installed → proof → capability → executor 的**可传播失效 DAG**。
**先读 `CopyProvenanceIndex` —— 它可能已经提供了 A1 需要的关系模型。**

---

### S1 · Challenge Probe —— 原语齐备，载体引用需订正

| 类 / 位置 | 规模 | 作用 |
|---|---|---|
| `CodeSealProbe` | 10 | **挑战-应答金丝雀**：`evaluate(long challenge)` |
| `reassert_core_targets` | `ronova_native.cpp:629-692` | retransform + 逐目标验证；`:679` 要求 `g_semantic_cycle_callbacks == targets.size()` 且 `initial_changes == g_semantic_outside_changes` |
| `record_semantic_proof` | `:422` | 逐目标验证态记录 |
| `task_thread_end` / `native_task_exit` | `:106` / `:97` | 绑定见 `:830` / `:921` |
| **`RonovaTransformer`** | **553** | **实际变换器** ← 前稿只引了 `NativeTransformBridge`（37 行） |
| `NativeTransformBridge` | 37 | Java 侧桥接 |
| `SemanticGateVerifier` | 100 | 登记控制流校验 |
| `RawAccessorContract` / `EntityDataAccessorContract` / `NbtCopyContract` / `NbtFactoryContract` | 151 / 61 / 145 / 78 | 各域契约 |

**订正：** 前稿说"原语齐备"所依据的载体是 37 行的 bridge，**而真正干活的是 553 行的 `RonovaTransformer`**。结论（缺统一层）不变，但**实施者必须先读 `RonovaTransformer`**。

---

### A2 · Native ABI —— 已有构建身份与 capability 位

| 位置 | 原文 |
|---|---|
| `ronova_native.cpp:746` | `bool match=std::strcmp(text,CODE_SEAL_BUILD_ID)==0;` |
| `NativeBridge.java:50` | `if(id.matches("[0-9a-f]{64}"))enableCodeSeals0(id);}` ← **返回值被丢弃** |
| `NativeBridge.java:61` | `capabilities = integrityCapabilities \| rootCapabilities;` |
| `NativeBridge.java:119-120` | `public static int capabilityMask() { return capabilities; }` |

**缺的是：** `abiVersion` / `featureMask` / `callbackTableVersion` / `structureSizes`。

---

### A3 · Generic Shadow —— 缺口准确，**结果模型比前稿描述更成熟**

| 类 | 行 | 结构 |
|---|---|---|
| `ShadowDurableRootStore` | 2,624 | 影子根 |
| **`ShadowVerification`** | 50 | `record(Status, expectedEpoch, observedEpoch, expectedHash, observedHash, detail)`<br>**`enum Status` 10 态**：`VERIFIED / MIRRORED / RECEIPT_MIRRORED / REALM_MISMATCH / ROLLBACK_REJECTED / STATE_CONFLICT / RECEIPT_DRIFT / READ_ONLY / FAILPOINT / INVALID_INPUT`<br>三谓词 `accepted()` / `matches()` / `rejected()` —— **区分精确与"接受但非精确"** |

**不要重造判定词汇 —— `ShadowVerification.Status` 是现成模板。**

---

### A4 · 分能力熔断 —— 状态词汇已有，**但熔断机制确实没有**

| 类 | 行 | 内容 |
|---|---|---|
| `ActivationState` | 11 | **6 态**：`INACTIVE / ACTIVE / PENDING_RESTART / UNAVAILABLE / REJECTED / FENCED` |
| `EvidenceState` | 10 | **5 态**：`UNOBSERVED / OBSERVED / VERIFIED / **DEGRADED** / FAILED` |
| `RuntimeDiagnostics` | 76 | "Per-runtime bounded sampler. Host-wide network counters are explicitly labeled and **never attributed to this Realm**." |
| `SurfaceRegistry` | 55 | `enum Maturity { S0_ABSENT … S6_COMPLETE }`；**`snapshot()` 只产出 S0/S1** |
| `SurfaceAdapter` | 23 | "Later execution ports require actual Root plans and receipts, not a claimed level." |

**接受 fix10 的收窄：** 加枚举值 **≠** 熔断器。完整熔断仍需定义**窗口、阈值、撤销、有限重试、并发恢复**。
**但起点不是零 —— 而且有现成宿主：**

`capability` 包（13 个类）是一个**按模块的能力系统**，正是 A4 的落点：

| 类 | 行 | 原文 |
|---|---|---|
| **`ServerCapabilityModule`** | 10 | "**One isolated server capability implementation.** Modules never register their own tick loop." |
| **`CapabilityRuntime`** | 126 | "Explicit module dispatcher. **It is the only tick/repair owner for server capabilities.**" |
| **`CapabilityDiagnostics`** | 25 | "Counted compatibility-boundary failures; **capability recovery never fails silently**." |
| `CapabilityContext` | 48 | "Per-module execution context with **counted best-effort compatibility operations**" |
| `BladeContinuityModule` / `FlightModule` / `LifecycleContinuityModule` / `NegativeEffectCleanseModule` / `TopologyContinuityModule` / `VitalsRecoveryModule` | 23–62 | 六个已实现的模块 |

**另有 `engine/BoundedRetryQueue`（238 行）**："Thread-safe FIFO drain with **finite per-entry retries and bounded dead letters**" —— **A4 需要的"有限重试"已有实现。**

**所以 A4 = 在既有模块系统上接熔断，而不是新建。** 逐模块隔离、唯一 tick/repair owner、计数诊断三者都已就位。

---

## 2. 给实施者的六条不要

1. **不要在 `observation` 包里加授权。** 该包约 28 个类带"观察不授权"的成文契约；新增授权语义应落在 policy / root 层。
2. **不要重建实际写入遍历。** 用 `NbtWriteReferences.Snapshot(boolean complete, List<Entry>)`。
3. **不要另立连续性概念。** `TaskEventRun` 已定义"连续、已收方验证、不跨任务/副本/父/调度器/proof 变化"。
4. **不要重造影子判定词汇。** 用 `ShadowVerification.Status` 的 10 态与其 `accepted/matches/rejected` 区分。
5. **不要另造写入边界抽象。** 用 `WriteSinkId` / `WriteSinkEvidence` / `WriteSinkExtension`。
6. **不要另造完成语义，也不要另建模块宿主。** 用 `CompletionContractId` / `CompletionContractExtension` / `CompletionEvaluation`；能力熔断落在既有 `ServerCapabilityModule` / `CapabilityRuntime` / `CapabilityDiagnostics` 上。

## 3. 给实施者的四条先读

1. **`RonovaTransformer`（553 行）** —— S1/A3 的实际变换载体，前稿引错了（前稿引的是 37 行的 `NativeTransformBridge`）。
2. **`CopyProvenanceIndex`（248 行）** —— A1 的同类已存在，可能已提供所需关系模型。
3. **`WriteSinkEvidence` / `WriteSinkExtension`（36 + 6 行）** —— S2 的指定接缝，注释明写生产实现"begins in 7.17"。
4. **`CapabilityRuntime` / `ServerCapabilityModule`（126 + 10 行）** —— A4 的既有宿主与模块契约。

---

## 4. 边界与口径

- **L0 三条铁律与 UX1 不变。** 本文只读源码。
- **不改 fix10 的任何决策、批次、编号、回执。** 本文只补"已有实现"基线。
- 不新增 `SubjectKey` / Root / 平行 D*·INV*·A*·R*·ID*·T* 编号。
- **本轮方法：** 枚举子系统 → 读每个类的类级说明。`observation` 包 41 个类全部读过类级说明；`integrity`/`surface`/`budget`/`runtime` 四个包全部读过。
- **免责声明的计数（约 28）为人工核对，非正则穷举** —— 正则因措辞差异漏检（`observation` 包正则只命中 9，人工读数约 17）。**该数字是下界。**
- **未做：** 未编译、未启动 JVM/游戏、未改源码、未改 `validation/`、未回填 PASS。
