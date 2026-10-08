# Ronova 设计规范 · 合并导读

> **本文件是派生导读，不是规范。** Ronova 的规范性权威仍只属于 §1 列出的原始五份文档。
> 本文件只做**跨文档合并呈现**；任何冲突以原始文档为准。
> 它**不新增**第二套 Root、身份或规范顺序（`README.md:30`、`L6:6`、`BL:3022`、`V2.6:172`）。
>
> 生成：2026-09-12。**五份规范 + 词汇表已全部合并**；覆盖明细与已知文档级问题见 §13 / §14。

**引用缩写**：`L6` = L6 Architecture Master V2.4（最高权威）·
`BL` = 7.14A Baseline / All Enhancements V1.4 · `CF` = L6E-0 Contract Freeze V1.3 ·
`V2.6` = 7.16+ Strength Master V2.6 · `P0A` = P0-A Truth Authority Implementation Spec V1.3。

**行号口径更正**：本导读全部行号取自逐行实读。注意 `Measure-Object -Line` 只数非空行，
所以 `L6` 是 **1468 行**（非 1177）、`CF` 是 **1695 行**（非 1201）、`BL` 是 **3022 行**（非 2169）、
`V2.6` 是 **3460 行**（非 2596）。引用行号一律以实读为准。

---

## 1. 权威顺序与效力

Ronova 的规范是**有序的**，冲突时高位优先。四份文档各自表述一致
（`README.md:19-30`、`L6:6`、`CF:56-74`、`BL:3012-3022`、`V2.6:143-152`）：

| 序 | 文档 | 冻结什么 |
|---|---|---|
| 1 | **L6 Architecture Master V2.4** | 路线、**D1–D13**、单 writer、原子 batch、宿主边界、状态所有权、编译期依赖 |
| 2 | **7.14A Baseline / All Enhancements V1.4** | 阶段路线、已内联的 hardening closure、四链拓扑与 P0 顺序 |
| 3 | **L6E-0 Contract Freeze V1.3** | canonical identity、revision、side-effect、执行上下文与客户端边界 |
| 4 | **7.16+ Strength Master V2.6** | 在上述约束内的强度、执行与验收增量合同 |
| 5 | **P0-A Implementation Spec V1.3** 及各阶段实现规格 | Truth Authority 实施规格 |
| 6 | GUI 文案、示例、handoff、进度说明 | **不能反向修改合同** |

### 1.1 冲突处理规则（五条，跨文档一致）

1. **显式冻结决策 > 同文档历史叙述**（`L6:45`、`L6:47`、`L6:62`、`CF:21-23`）。
2. **实质冲突登记为设计缺口，不得用「版本号较新」静默覆盖上游**
   （`README.md:30`、`V2.6:152`）。
3. **禁止反向解释**：`CF:74`「**禁止**把 L6E-0 反向解释成对 Canonical L6 的替代」；
   `BL:2931`「Canonical L6 Master Plan 继续作为上位架构，不被后续加强稿覆盖」。
4. **V2.5/V2.4 加强稿只作历史，不与当前规范并行裁决**（`BL:3022`、`L6:6`）。
5. **源码/日志优先于参考报告**（`L6:40` 红线 2）；
   **实现状态只看 `docs/CURRENT-PROGRESS.md`**（`L6:3`、`L6:8`、`BL:3`、`CF:3`）。

### 1.2 违反顺序的典型错误（各处点名）

- 把设计要求写成已实现（`L6:19`、`V2.6:154`、`D86`/`D98`/`D106`）；
- 只引用不可得原文而宣称验收闭合（`V2.6:154`）；
- 用新文案篡改历史运行结果（`V2.6:154`）；
- 把 r52 的占位接口、统一 mapping 或 source-level 编译通过写成 exact-artifact 已完成（`V2.6:2885`、`D98`）。

---

## 2. 冻结词汇（`CONTEXT.md`）

`CONTEXT.md` 冻结架构与 L6E 术语；**不代表**能力已实现。每条带 _Avoid_ 列表（易混用词）。

### 2.1 观察与可见性

| 术语 | 定义 | 禁止混用 |
|---|---|---|
| **Raw Observation** | Dominion 内部「物理化身存在及其拓扑关系」的知识。**永不**被 Judgment 状态、quarantine、Public Presence 或 owner 状态过滤 | visible entities / active targets / unfiltered presence |
| **Public Presence** | 物理化身是否经 Dominion 控制的外部查询、渲染或追踪路径暴露。**可隐藏**被隔离对象，**不能改变 Raw Observation** | observation / existence / loaded state |
| **Physical Incarnation** | 某身份/lineage 在某一时刻的一个具体运行时实体对象。其消失**不擦除**持久 Judgment 真相 | tombstone / judgment record |

### 2.2 判定与终结语义

| 术语 | 定义 | 禁止混用 |
|---|---|---|
| **Judgment Eligibility** | raw-observed 化身是否**可以**进入 Judgment 关闭。是 Raw Observation **下游**的策略决定，**不是索引属性** | trackability / observability |
| **Tombstone** | 「身份/lineage 已被判定终结」的权威持久事实。**逻辑寿命只通过显式 clear 结束**，绝不仅因化身消失或重试用尽结束 | watch entry / retry record / weak reference |
| **Lifecycle Integration** | 把一次 Judgment **一次性**并入正常生命周期效果（如死亡整合） | terminal retry / continuous enforcement |
| **State Reassertion** | 可重复且幂等地断言化身仍符合终结真相。**重复不得重复一次性 Lifecycle Integration 效果** | re-judgment / repeated death |
| **Terminal Watch** | 对匹配 Tombstone 真相的化身的运行时感知，使 State Reassertion **有界且可恢复**。**永不创造** Judgment 真相，**永不决定** Public Presence | tombstone store / retry queue / judgment authority |

### 2.3 权威与免疫

| 术语 | 定义 | 禁止混用 |
|---|---|---|
| **SubjectKey** | 相等、哈希、Root 查找与持久外键的**唯一稳定逻辑键**：`(RealmId, subjectId)`。运行时类、维度、位置、来源、schema 元数据**永不参与**相等 | entity UUID alone / 整记录相等 / Java 对象同一性 |
| **SubjectIdentity** | 由 SubjectKey 锚定的不可变元数据描述符。**不是第二个逻辑键** | identity key / physical incarnation / 可变快照 |
| **BindingRevision** | Root 签发的 `SubjectKey ↔ PhysicalIncarnationId` **单调**修订号，**仅同一 SubjectKey 内可比** | OwnershipRevision / ProjectionRevision / 全局序号 |
| **OwnershipRevision** | Root 签发的 `(SubjectKey, StateDomainId) ↔ AuthorityOwner` **单调**修订号，**仅该对之内可比** | BindingRevision / policy version |
| **ProjectionRevision** | 一条不可变本地投影谱系的版本。**永不替代**前两者，**必须**经单个 ProjectionView 消费 | RootCommitIndex / DominionEpoch / 单独读的缓存版本 |
| **PolicyRevision** | Root 授权的 policy/mode bundle 的单调版本（Realm policy lineage）。**旧称 `ModeRevision` 不再作为独立类型**（`CF:319-321`） | mode version 独立类型 |
| **LifecycleTruth** | 持久两态 `ACTIVE \| TERMINAL`。Resolution 不确定性与 admission quarantine 是**另外的轴**，**永不新增**生命周期状态 | UNKNOWN lifecycle / QUARANTINED lifecycle |
| **ResolutionState** | 本地权威解析的显式质量：resolved / partial / ambiguous / opaque / stale / invalid / unknown。**Unknown 永不等于 false 或 safe** | boolean result / Optional.empty |
| **ProjectionView** | 不可变句柄，把 projection revision、epoch、policy revision、fence state 与 entries **一起固定**，**不得提供「先读 revision 再另查 entry」的撕裂式 API**（`CF:1334`） | independent revision read / mutable cache |
| **RootCommandId** | 一条 Root 命令的稳定幂等键。**同 id + 同 canonical payload hash** = 一次可重试 mutation；**不同 hash = 命令冲突** | message sequence / commit index / 换新 id 重试 |
| **COMMIT_UNKNOWN** | 持久写入或回执返回歧义的未决结果。**只允许**用**同一 command id 与 payload hash** 恢复或幂等重发；既非已提交也非已拒绝 | failure / false / 换新命令自动重试 |
| **Owner Immunity** | 「owner 或 Dominion 自身化身在 quarantine 说反话时**仍保持公开存在**」这一不变量。属 Public Presence 策略，**不依赖调用方侧捷径** | authority bypass / raw visibility |
| **DominionEpoch** | `(generation, sequence)`。与 FenceToken、RootInstanceId、CommandSequence、commandId、commitIndex、realmId、各 revision、incarnationSequence **严格分离**（`CF:383-394`） | 单一 sequence 兼职 |
| **AuthorityResult\<T\>** | 封闭结果类型：`RESOLVED(T) / PARTIALLY_RESOLVED(T,evidence) / NOT_FOUND(verifiedAtRevision) / AMBIGUOUS / OPAQUE / STALE / INVALID / UNKNOWN / FENCED`。**不得用 `boolean` / `null` / `Optional.empty()` 承载**（`CF:1332`） | boolean result |

**三条词汇层硬约束**：身份只有一个键（`D100`）；三类 revision 各有 scope、不可互替；
`unknown` **永不**等价于 `false` 或 `safe`（`CF:160-174` R5）。

---

## 3. 身份与修订合同（`CF` §3–§4）

### 3.1 身份成员与排除（`CF:248-254`）

- **只用 `SubjectKey`** 做 equality / hash / lookup；
- **不参与身份相等**：`originDescriptor`、schema version、dimension、坐标、runtime class、provenance；
- Minecraft UUID **只作为 `subjectId` 的来源证据**；**只有 Root binding 才确立完整 `SubjectKey`**；
- identity 一经 durable binding，**不因 metadata、物理 incarnation、reload 或 dimension transfer 改变**；
- **迁移强制**：现有 `dev.ronova.coremod.contract.SubjectIdentity` 必须迁移/适配，
  **不允许在新包并行维护第二套 logical identity**（与 `D11`、`L6:69` 一致）。

### 3.2 `RealmId`（`CF:213-222`）

全局稳定；**不从 Java object identity 推导**；**不与 Minecraft dimension id 混用**；
可跨重启与 world/store lineage 持久化；**不要求跨进程**。

### 3.3 `PhysicalIncarnationId`（`CF:258-278`）—— **不是身份**

| 规则 | 内容 |
|---|---|
| 构成 | `subjectKey + incarnationSequence + incarnationNonce/generation` |
| 多副本 | 同一 Subject 可有多个 incarnation 历史；**当前时刻只能有一个符合 Policy 的 active incarnation** |
| 权限 | incarnation **不继承**旧 incarnation 的 execution authority |
| 复活 | **stale incarnation 永远不能靠同 UUID 自动恢复 Authority** |
| 限定 | **必须 realm-qualified**；任何只接收 incarnation 的 lookup 都能从其中取得完整 `SubjectKey`，**不得依赖调用方另传 Realm** |

### 3.4 三个 revision 的规则（`CF:284-302`）

- 两类 revision **不得混用**；
- **只在自身 scope 内比较**；不同 Subject 或不同 Domain 的数字大小**没有语义**；
- **旧 revision 不能覆盖同一 scope 的新 revision**；
- stale binding **不参与** authoritative execution；
- incarnation rebind **必须产生新 `BindingRevision`**；ownership rebind/revoke **必须产生新 `OwnershipRevision`**；
- **TERMINAL 后出现的新 incarnation 不能通过旧 binding 恢复**。

### 3.5 `RevisionCrossingPolicy`（`CF:538-553`）—— 三模式，封闭

| 模式 | 含义 |
|---|---|
| `EXACT_SNAPSHOT` | completion 时四类 revision、epoch 与 context **完全一致** |
| `ALLOW_NEWER_PROJECTION_SAME_AUTHORITY` | **只允许 ProjectionRevision 前进**；SubjectKey、Incarnation、Binding/Ownership/PolicyRevision、Epoch、FenceState **必须不变** |
| `REVALIDATE_BEFORE_COMPLETION` | completion 前**必须从新的 immutable ProjectionView 重建 context，并重新执行 Judgment**；**不能只替换 revision 数字** |

**默认**：未声明的 CompletionContract **等同于 `EXACT_SNAPSHOT`**。

### 3.6 操作与完成标识（`CF:325-369`）

- **`CriticalOperationId`**：所有 Authority-sensitive operation 必须使用**稳定的操作类型标识**；
  **不使用方法名作为唯一语义**。类别：VITAL_WRITE / TERMINALIZE / ADMISSION_BIND / PRESENCE_PUBLISH /
  PRESENCE_REVOKE / INCARNATION_REBIND / EQUIPMENT_ENFORCE / MOVEMENT_ENFORCE / EFFECT_ENFORCE / SIDE_EFFECT_PUBLISH。
- **`CompletionContractId`**：所有 Authority-bearing completion **必须引用 CompletionContract**
  （TERMINAL_COMMIT_V1 / VITAL_WRITE_V1 / ADMISSION_BIND_V1 / PRESENCE_REVOKE_V1…）。
- **`CompletionContract`**（`CF:558-568`）：`allowedKinds` 是**封闭集合**，未列出的 completion 不合法；
  `failureMapping` **不得把 `UNKNOWN`/`STALE`/`FENCED` 映射为普通 `false`**。

### 3.7 §25 冻结接口集（`CF:1297-1334`）—— P0-A 前必须冻结的 30 个名字

RealmId · SubjectKey · SubjectIdentity · SubjectType · PhysicalIncarnationId · BindingRevision ·
OwnershipRevision · ProjectionRevision · PolicyRevision · DominionEpoch · CriticalOperationId ·
CompletionContractId · CompletionContract · RevisionCrossingPolicy · AuthorityExtension ·
AuthorityEnvelope · AuthorityResult\<T\> · AuthoritySnapshot · ProjectionView ·
ExecutionAuthorityContext · ResolverProvider · ResolverProviderState · AuthorityRelationshipEdge ·
PreAdmissionBindingState · DeviationAccumulator · CriticalInvariant · ModeSelector · PolicyOverlay ·
FenceState/FenceScope · SideEffectScope

---

## 4. 权威与状态所有权

### 4.1 Internal Root 不变（`L6:12-14`、`L6:174-181`、`V2.6:174-181`）

- **Authority 不移出当前游戏进程**；`InternalDominionRoot` 与 Minecraft **同 JVM**；
- 7.16：`DurableRootStore` = **sole durable writer**；
- FULL L6：`InternalDominionRoot` = **唯一逻辑 Authority writer**，`DurableRootStore` = 唯一 durable backend；
- Agent、Instrumentation、JVMTI、Native、恢复内核**只提供**证据、门控、执行和修复；
- 它们**不能成为第二 Judgment writer**；
- **明确不宣称** JVM 外故障域隔离（`L6:789`、`L6:925-926`、`L6-INV-10`）。

### 4.2 D1–D13（最高权威冻结决策，`L6:45-70`）

| 编号 | 决策 |
|---|---|
| **D1** | Dominion 内部的 Judgment/Reconcile/TerminalWatch/生命周期一致性观察**只来自 Raw Observation**；Presence/Quarantine **只控制外部读取路径可见性，不得反向过滤内部候选发现**；`TargetDiscovery` **不经过** Presence Filter；受保护对象排除**只依赖纯查询 `AuthorityView`** |
| **D2** | **逻辑 Judgment 语义与物理存储表示严格分离**。存储优化（紧凑表示、索引、分段冷热、checkpoint、compaction）**不得**扩大或缩小命中集合、**不得**改变 UUID/IDENTITY 语义、**不得**破坏 `ClearTombstone(uuid)`、**不得**使可撤销记录变为不可撤销；**禁止把精确 Tombstone 折叠为更宽的 class-lineage 规则作为容量手段** |
| **D3** | FULL L5 唯一权威 durable writer = **Java 进程内 `DurableRootStore`**；**8.x 不再做跨进程 durable authority 迁移**；FULL L6 = JVM 内 `InternalDominionRoot`；**Native 任何阶段都不做 authoritative durable write** |
| **D4** | `DominionEpoch` 保持 `(generation, sequence)`；`fence_token`/`root_instance_id`/`command_sequence`/`command_id`/`commit_index`/`realm_id` 是**独立概念**，**不进入** `DominionEpoch`，**不由单个 sequence 兼职** |
| **D5** | 终结语义拆为 **Lifecycle Integration（ONCE）** 与 **State Reassertion（REPEATABLE + IDEMPOTENT）**；终结层**不得**删除 Raw Observation 对物理对象的认知；`TerminalLifecycle.enforce` 里的 `TopologyIndex.forget(entity)` **必须移除** |
| **D6** | **Judgment commit 扩展性不变量**：一个 Action 的 N 条 Judgment **不得导致 N 次完整 authoritative projection rebuild**；单次代价**不得**是「本次 Judgment 数 × 历史 Tombstone 总数」；目标上界 `O(N_action) + O(Δ)` |
| **D7** | Judgment 属 **control-plane mutation**；L6 仍按 Action 批处理；**只有 Root 返回 durable `CommitReceipt` 后**才能进入不可逆 lifecycle transition；「可能已提交但 receipt 未返回」→ `COMMIT_UNKNOWN`，**保留原 `command_id`**，**禁止生成替代 `command_id`**；实时热路径只读 immutable projection，**禁止同步 durable I/O** |
| **D8** | **不得先移除旧的可靠机制、再等待后续版本补上替代机制**；旧 finite retry 只在新 durable Tombstone 驱动循环真正成立（7.16）后才删 |
| **D9** | `1 JudgmentBatch = 1 command_id = 1 atomic transaction = 1 DominionEpoch transition = 1 commit_index = 1 durable receipt = 至多 1 次 projection publication`；批内 Judgment **不各自推进** authoritative epoch；物理收敛仍可异步/分批 |
| **D10** | 7.15：`DominionLedger(SavedData)` 仍写权威 `LifecycleIntegrationReceipt`，Java `DurableRootStore` **只镜像并验证**；7.16 cutover 后 receipt 与其他 truth **只由 `DurableRootStore`** 写 |
| **D11** | 域类型沿既有 `dev.ronova.coremod` 骨架演进；`DominionCommand`/`CommitReceipt`/`DominionRootGateway` 的**唯一 canonical 归属为 `root/api`**，**不得在 `contract/` 或 L6E 新包复制第二套**；现有 `SubjectIdentity` 仅为迁移输入 |
| **D12** | 测试保留单位是**不变量覆盖与可追溯 evidence**，不是旧测试数/类名/调用图；等价覆盖建立后可删只保护废弃结构的测试 |
| **D13 / `PROC-INV-1`** | **Authority / 进程角色边界**：允许 bootstrap、attach、recovery、relaunch 的有界 helper/后继进程，**但** Judgment/Terminal/Ownership truth、durable Root mutation 与持续 enforcement ownership **必须留在当前有效 Minecraft JVM**；helper/sidecar/service **不得**成为 durable Authority writer、持续战斗执行面、独立 target-monitoring/attack plane 或 External Dominion Root。**`不再要求 child-process count 必须为 0`** |

> `D9`–`D13` 优先级与 `D1`–`D8` 相同，且**约束 Baseline V1.4、L6E-0 V1.3 与 P0-A V1.3**（`L6:60-62`）。

### 4.3 Single-Writer 不变量（`L6:415-436`）

- 任何时刻，**推进 authoritative epoch 的逻辑 writer 有且仅有一个**；
- 所有 mutation **必须**经 `InternalDominionRoot → RootCommandBus → DurableRootStore`；
  **任何直接写 Store 的旁路都属于 invariant violation**；
- **Native、Domain、Projection、Agent 均不得推进 authoritative epoch**；
- 不宣称能对抗「整个 JVM 已被完全控制」。

### 4.4 目标状态所有权（`L6:393-411`）

| 状态 | Owner | 权威性 | 持久化 |
|---|---|---|---|
| owner registry | `persistence.OwnerRegistry` | **durable truth** | 持久 |
| tombstone（UUID+lineage，语义不压缩） | `persistence.TombstoneStore` | **durable truth，无限期** | 持久 |
| epoch `(generation, sequence)` | `persistence.EpochClock` | durable truth，单调 | 持久 |
| realm identity | `contract.RealmId`（store-bound） | durable identity | 持久（非单调） |
| `fence_token` / `root_instance` | `root.internal.RootInstanceState`（L6） | Internal Root writer qualification | head 元数据 + 运行时 |
| 已判决身份 | `terminal.TombstoneAuthority` | **派生自 tombstone（非独立权威）** | — |
| physical detach watch | `terminal.TerminalWatchSet` | 运行时可重建 | **不持久化** |
| lifecycle integration receipt | `terminal.LifecycleIntegrationReceipt` | durable（防重复掉落） | 7.15 SavedData 权威 + shadow 镜像；7.16+ `DurableRootStore` 权威 |
| raw topology | `observation.TopologyGraph` | 运行时观察（**非权威**） | **不持久化** |
| presence decision | `presence.PresencePolicy` | 派生自 projection | — |
| Dominion-self 判定 | `presence.OwnerImmunity` | **无状态谓词**，仅引用自身 registry namespace | — |
| projection bundle | `projection.ProjectionCoordinator` | 只读快照（带 epoch） | — |
| semantic proof / lease | `integrity.EnforcementLease` | 运行时，**经 RootCommandBus 生效** | — |
| **最终 durable truth（L6）** | `root.internal.InternalDominionRoot` + `persistence.DurableRootStore` | **InternalRoot 唯一逻辑 writer；Store 唯一 durable backend** | 持久 |

**`JudgmentState.EXECUTED` 的"弱身份权威"降级为 `TombstoneStore` 的派生视图**（`L6:413`）——
**权威只有一个，就是 durable tombstone**。

### 4.5 模块「不做什么」清单（`L6:337-349`）

| 模块 | 禁止 |
|---|---|
| `observation.RawObservationSource` | 不判定 terminal、不做 presence、不对外暴露 |
| `presence.PresencePolicy` | 不影响 raw 观测；**自身保证**不隐藏 owner/Dominion-self（不依赖调用方短路） |
| `presence.OwnerImmunity` | 不依赖 authority resolution 成功与否（fail-static 到 present） |
| `authority.AuthorityView` | **零副作用**；不 bind、不写 tag、不 clearProjection |
| `authority.AuthorityBinder` | **不出现在任何高频读取路径** |
| `terminal.TombstoneAuthority` | 不用对象弱身份作权威 |
| `terminal.TerminalWatchSet` | 不以 retry 次数结束；不无界扫描 |
| `integrity.EnforcementLease` | 失效时**只 fence mutation，不撤既有保护**；**不直写 persistence** |
| `persistence.TombstoneStore` | **不做 FIFO 容量逐出；不做语义折叠** |
| `root.api.JudgmentBatch` | 不逐实体提交；不隐式拆分 |
| `root.internal.InternalDominionRootGateway` | realtime query 只读 projection；mutation 才进入 Root |

### 4.6 编译期依赖约束（`L6:487-494`）—— 可静态检查

- `observation/` **不得** import `presence/`；
- `presence/` 与 `engine.TargetDiscovery` **只得** import `authority.AuthorityView`，
  **不得** import `AuthorityBinder`；
- `persistence/` **不得** import `integrity/`；
- `contract/`、`wire/` **不得** import 任何 `net.minecraft.*` / `net.minecraftforge.*`；
- `capability/` 只 import `projection/` 与 `authority/`，**不得写 durable 状态**；
- MC 类型必须留在显式 `*.minecraft` adapter 或 `*Runtime` 边界内。

**分层规则**：依赖只能**自上而下**；同层不互相环依赖；跨界只经 bridge/gateway/port；
**mutation qualification 统一在 Layer 4 的 `RootCommandBus` / mutation admission boundary 执行**（`L6:440-455`）。

### 4.7 Presence 保证范围（`L6:351-357`，关闭 H6）

**强保证**：Dominion 控制的 MC/Forge 通用 observation/query/tracking/rendering sink。
**不保证**：第三方自行创建维护、绕过 MC/Forge 通用入口的私有 collection、自定义 manager、
独立 cache、直接对象引用读取。
**且不得**对这些路径通过第三方 Mod ID / class name / entity name / bytecode patch 建立特殊适配（`L6:355` 红线 3）。

---

## 5. 不变量全集

### 5.1 跨阶段必要条件 `PROC-INV-1`（= `D13`，`L6:543-545`）

允许 bootstrap/attach/recovery/relaunch 的有界 helper 与后继进程；
**但** Judgment/Terminal/Ownership truth、durable Root mutation、持续 enforcement ownership
**必须留在当前有效 Minecraft JVM**；helper/sidecar/service **不得**成为 durable Authority writer、
持续战斗执行平面或 External Dominion Root。

**PASS 判据五条**（`L6:545`）：(a) 生产源码 + 打包 JAR 中全部 `ProcessBuilder`/`Runtime.exec`/
attach-helper/relaunch 入口**已枚举并标注用途**；(b) helper 不持 durable Authority/持续战斗状态，
**handoff 后退出**；(c) Windows 进程树覆盖正常启动、运行期重断言、关服、Native 不可用、可选 relaunch，
证明**无并存的 Authority writer**；(d) Native 源码与 PE 导入表显示无隐藏 External Root transport/进程注入；
(e) bootstrap/relaunch 失败进入**已登记的 degraded/fenced**。

> `L6:874` 明确：「**运行期存在 `AutoAttach` helper 本身不导致 FAIL**」；
> `L6:15`/`L6:20`：D13 **不再要求 child-process count 必须为 0**。

### 5.2 FULL L5 不变量（`L6:504-541`）

八条领域必要条件 + `PROC-INV-1` 为 FULL L5 必要条件；`L5-INV-6` 为**候选强化项**（未达 → `L5_COMPAT`）。

| 编号 | 要求 | 要点 |
|---|---|---|
| **L5-INV-1** | 单一 durable writer（Java 固定，D3） | 静态扫描显示**只有一个** `EpochClock.advance` 调用点，位于 `DurableRootStore` commit 路径内；删掉 SavedData 镜像后 owner+tombstone 仍可恢复 |
| **L5-INV-2** | Tombstone 无限逻辑寿命（语义不压缩，D2） | **无 65536 FIFO 逐出**；只在显式 owner `ClearTombstone` 时消失；1/10/1000 次重插后仍拒绝；compaction 前后**逐项命中相同**；`ClearTombstone(uuid)` 在任何存储形态下可达 |
| **L5-INV-3** | 有限 retry 消除 | **无**「重断言 N 次后放弃」；`JudgmentEngine` 删 `RETRY_DELAYS` 有限数组语义；寿命由 tombstone 存在性决定 |
| **L5-INV-4** | Raw/Presence 完全分离 + immunity 内化 | `observation/` **不 import** `presence/`；被隔离对象 `raw=true, public=false`；直接调 `PresencePolicy.decide` 注入 `quarantined=true` 时 owner 与 non-Player Dominion-self **仍返回 present=true**；**authority resolution 被迫失败时也成立**（fail-static） |
| **L5-INV-5** | Bounded 物理工作 | 全部持续物理工作有每帧时间/数量预算 + 退避；terminal worker ≤ 预算/tick；live p95 tick ≤ 冻结基线 +5% |
| **L5-INV-6** | Critical truth 不依赖 Java predicate（**候选/INFERENCE**） | 关键门 truth 来自 native 不可变状态或 ≥ per-server 不可变投影；Java 只做非关键适配 |
| **L5-INV-7** | Fail-closed 语义证明 | 证明失败**fence 新 mutation**（经 RootCommandBus mutation qualification），但**保留**既有 protection/tombstone/watch |
| **L5-INV-8** | 收敛观测/保护入口 | 同一语义不由 Mixin+Event+Agent 三份实现；玩家保护收敛到**单一权威决策源**；生成拒绝保留 `NBT-gate + ENTITY_MANAGER-sink` **两个**门，其余委派 |
| **L5-INV-9** | Judgment 提交扩展性（D6） | **恰一次** durable 提交单元 + **至多一次**发布周期；1k/10k/100k 三档次线性；禁止 `O(N_action × N_total)` |

### 5.3 FULL L6 Internal 不变量（`L6:861-873`）

| 编号 | 要求 |
|---|---|
| **L6-INV-1** | `InternalDominionRoot` 唯一逻辑 Authority writer；`DurableRootStore` 唯一 durable backend |
| **L6-INV-2** | Native / Domain / Agent / Projection **均不能**直接推进 authoritative epoch |
| **L6-INV-3** | realtime query/tick/admission/presence 热路径 **0 同步 durable I/O；只读 immutable projection** |
| **L6-INV-4** | stale root instance / stale fence / stale epoch mutation **0 接受** |
| **L6-INV-5** | `command_id + canonical hash` 幂等且 **conflict 显式** |
| **L6-INV-6** | `COMMIT_UNKNOWN` **只能以原 command_id recovery，不得猜 success/reject** |
| **L6-INV-7** | **durable commit 成功后才 publish projection / 进入不可逆 lifecycle** |
| **L6-INV-8** | Raw Observation 与 Presence 单向分离继续成立 |
| **L6-INV-9** | Tombstone durable、无 FIFO 语义逐出、Terminal dominance 成立 |
| **L6-INV-10** | JVM 完全失陷**不属于** Internal Root 的隔离保证；**此限制不得在文档中隐藏** |

> ⚠️ **文档内部不一致（已核实，`L6`）**：`L6:1343-1354`「最终清单 §2」是**第二张 L6-INV 表**，
> ID 相同但 2、4–10 的内容不同（如 §8.6-6 = COMMIT_UNKNOWN，清单 §2-6 = 「0 acknowledged lost commit」）；
> 且 `L6:1200` 又引用了「L6-INV-10 = realm ownership 无法唯一证明时 mutation 必须 blocked」，
> 该含义**两张表都没有**。按 §1.1 规则 2，这是应登记的**设计缺口**，不是可以静默取一表的差异。

### 5.4 `D33` 修订：Final Write Sink 不得绝对化（`V2.6:472-501`）

> **所有已纳入 Ronova 仲裁能力范围的 Authority-bearing mutation，
> 必须经过登记 WriteSink 或等价的已证明最终提交门。**

若某 mutation ①能观察但不能前置阻止、②能穿透当前 gate、③能由未知 Native/JVMTI 直接修改、
④无法证明是否经过当前 sink，则：不得视为合法结果 / 不得宣布结果面闭环 /
撤销对应 coverage proof / 隔离相应结果授权 / 按 scope 进入 `PARTIAL` / `FENCED`。
——「这比『假装所有写入都必经 Ronova』优先级更高」。

### 5.5 `D53`–`D106`（`V2.6` 强不变量，全部继续有效）

**`D53`–`D65`（V2.2）**：default-in 候选面 · 归属证明只决定处置级别 · 预算耗尽只延后不删 obligation ·
FENCE 必须有 scope（TARGET 问题不得无条件升级 ROOT）· ROOT/REALM FENCE 只在核心可信性失证时升级 ·
TargetEpoch 是资格代际 · 重新激活必须用新 epoch · ResultGraph 是派生证据 · Certificate 是证明 ·
EXTREME 不设敌对 Mod 数量上限 · 兼容性可破坏但不得以无界资源/存档/OS 破坏增强 ·
无法前置 veto 时不得产生虚假 DENIED · 7.16 不因 V2.2 返工。

**`D66`–`D78`（V2.3）**：候选准入一条强证据或两条独立弱证据 · Candidate 只允许临时限制 ·
重复日志不得伪装独立证据 · 全部 accumulator 有容量与字节预算 · 义务归并 ·
细节淘汰前必须接力 · 汇总不证明闭合 · 四态相互分离（FENCED ≠ 攻击成功）·
APPLIED 需局部效果证据 · 历史 CLEARED 不得冒充当前 · 重复次数贡献封顶 ·
保留 Authority/repair 预算与公平 · 四项合同归 7.17/L6E。

**`D79`–`D86`（V2.4）**：资格变更单一 durable transaction · 整体可见/不可见 + commit 后发布 +
原 command id 恢复 · Host Safety 覆盖九类资源 · Ronova-owned 与 whole-host pressure 分开 ·
`completionSatisfied` 显式声明 · 证据结论与释放状态分离 · 防抖不得改变操作者命令语义 ·
不新增按钮/不缩小攻击域/不返工 7.16。

**`D87`–`D98`（V2.5）**：Root 唯一 writer + 八 Surfaces 只做 observer/adapter/executor/projection ·
八段链可检查 · 三平面封闭 · 不加顶层 key · 逐键矩阵从 7.17 起 ·
服务端 Root Truth 与客户端投影严格分离 · 跨表面操作绑定七要素 ·
缺任一证明必须如实报告 UNOBSERVED/UNAVAILABLE/PENDING_RESTART/PARTIAL/FENCED ·
跨表面统一计划 · Host Safety 在所有预设始终启用 · 同 JVM/Forge 边界 ·
7.16 Scope Lock 继续成立。

**`D99`–`D106`（V2.6）**：许可与 effective policy 取交集（OFF 不可被覆盖）· SubjectKey 唯一身份 ·
一次 JudgmentBatch 同一事务 · Completion 预声明 scope · Root COMMIT_UNKNOWN 与 step EFFECT_UNKNOWN 分离 ·
effectKey 语义稳定 · 持久空间有限但不删 Truth · 规范优先级唯一且历史缺件显式登记。

> **注意**：`D1`–`D32` 是**继承原则**（Raw/Presence 分离、Judgment/存储分离、单 writer、
> command idempotency、原 command id 恢复、不回退、worker 有界、Native/JVMTI 不是 Truth、
> 修复后重验证、新机制未证明前不删旧机制、关键 evidence 失效 fail-closed、
> **可牺牲兼容性不可牺牲持久化正确性**）——见 `V2.6:455-470`。

### 5.6 `D1`–`D32` 与 `D33`–`D106` 之外的两组宪法级不变量

**`CF` §13 Constitutional Invariants（`CF:826-843`）**——「普通 Policy / Overlay **不允许破坏**」：
`TERMINAL precedence` · `Epoch monotonicity` · `COMMIT_UNKNOWN semantics` ·
`Ownership revision ordering` · `Tombstone durability` · `Root writer uniqueness` ·
`Stale incarnation denial` · `Fence monotonic safety`。
Policy Compiler 必须支持：Static Analysis / Conflict Detection / Unreachable Rule Detection /
Invariant Validation / Dry Run / Signed Revision。

**`CF` §2 全局红线 `R1`–`R7`（`CF:78-207`）**：

| 编号 | 规则 |
|---|---|
| **R1** | **No Mod-ID Authority**——禁止 `if modId == X → allow/deny/terminal/hardened`；Mod/class/loader/transformer provenance **只能作为 evidence、incident attribution、selective fencing input**，**不能直接成为 Authority** |
| **R2** | **Non-Blocking ≠ Hookless**——`NON_BLOCKING` 只表示不依赖对 hostile mutation 的逐次主动拦截；仍可用 authority-critical seam（phase reconciliation、admission、presence publication、side-effect publication、raw observation、integrity verification） |
| **R3** | **Hardened 必须是 Non-Blocking Core 的超集**：`HARDENED = NON_BLOCKING_CORE + EXECUTION_AUTHORITY + INTEGRITY_HARDENING`；进入 Hardened **不能关闭** Raw Observation / Convergence / Admission / Presence / Root Truth / Reconstitution |
| **R4** | **Server Authority ≠ Client Pixel Authority**（见 §7） |
| **R5** | **Unknown ≠ False ≠ Safe**——任何未解析状态**不得默认为安全**；统一显式状态 `RESOLVED / PARTIALLY_RESOLVED / AMBIGUOUS / OPAQUE / STALE / INVALID / UNKNOWN` |
| **R6** | **Repair Success ≠ Trust Restored**——修完必须 repair → revalidate → PASS → ACTIVE，**否则继续 FENCED** |
| **R7** | **进程角色边界**（见 §6.1） |

### 5.7 P0-A 核心不变量 `A1`–`A8`（`P0A:92-197`）

> **编号更正**：P0-A 里**没有** `L6-INV-*`。它自己的不变量是 `A1`–`A8`；
> 全文件唯一的 `*-INV` 是继承来的 `PROC-INV-1`。
> §2 前言（`P0A:92`）：「以下不变量**必须成为代码级断言、测试和 Root schema 的共同基础**。」

| 编号 | 名称 | 要求 |
|---|---|---|
| **A1** | Subject Identity Stability | Logical Subject Identity ≠ Java Object Identity；物理 Entity 重建、reload、dimension transfer、manager replacement **不得自动改变 `SubjectKey`**；`SubjectIdentity` metadata 可更新但**不参与 equality/hash/Root lookup** |
| **A2** | Incarnation Non-Inheritance | **不得自动继承**旧 Incarnation 的 execution authority；旧 stale incarnation **永远不能仅靠 UUID 相同恢复 Authority** |
| **A3** | Binding Monotonicity | `BindingRevision(new) > BindingRevision(old)`；**旧 binding 不得覆盖新 binding** |
| **A4** | Tombstone Dominance | 若 Root Truth 为 `Subject X = TERMINAL`，则 new incarnation / rebind / reload / reconstruct **都不能使 Subject 自动恢复 ACTIVE** |
| **A5** | Root Writer Uniqueness | `InternalDominionRoot` 是唯一逻辑 Authority writer；`DurableRootStore` 是唯一 durable backend；Minecraft Domain、Agent、Native、Projection **不得直接推进 authoritative epoch，也不得绕过 Root 修改 durable truth** |
| **A6** | Projection Is Not Truth | Projection = Root Truth 的不可变本地视图；Projection stale / 损坏 / 延迟 **不得反向改写 Root** |
| **A7** | Graph Is Not Root | Authority Relationship Graph = 证据 / 已验证关系缓存，**不是第二个 Root** |
| **A8** | Production Process Boundary（= `PROC-INV-1`） | Truth service、projection publisher、durable mutation 与 continuing enforcement **必须留在当前有效 Minecraft JVM 内** |

**§32 宪法级不变量（冻结名字，`P0A:1261-1282`）**：
`TERMINAL precedence` / `Epoch monotonicity` / `COMMIT_UNKNOWN semantics` /
`BindingRevision monotonicity` / `Tombstone durability` / `Root writer uniqueness` /
`Stale incarnation denial` / `HARDENED superset rule`。
冲突时返回 `POLICY_REJECTED`，「**而不是 runtime 临时猜优先级**」；`P0A:2081`：
「**任何 flag 不得绕过 Constitutional Invariants**」。

**P0-A 的三条特有硬规则**：

1. **Truth only flows one way**（`P0A:2274-2280`）：`Root → Projection → Consumer`；
   「Physical Minecraft state、Resolver evidence、Java object identity、第三方 Manager field
   都只能成为观察或绑定证据，**不能反向偷偷升级成 durable Authority Truth**」。
2. **缺席证据禁止**（`P0A:1435`）：「`NOT_FOUND` **只允许**在指定 Root/Projection revision 上
   确认不存在时返回；**没有查到、Root 不可达或 projection 未覆盖都必须是 `UNKNOWN`**」。
   `P0A:998`：「**刻意不提供 `boolean isTerminal()`：`UNKNOWN ≠ false`**」。
3. **不新增平行体系**（`P0A:203`/`:308`）：必须沿 `dev.ronova.coremod` 演进，
   「**禁止另建 `com.<project>.ronova.authority.p0a` 平行体系**」；canonical 类型只能有一份。

**测试编号族**（按不变量而非旧测试数量组织，`P0A:1675`）：
`U-01`–`U-17`（不变量契约，含 U-17 Production Process Boundary）·
`I-01`–`I-08`（完整性）· `C-01`–`C-05`（并发/崩溃，C-01 迟到 revision 12 在 13 之后必须被拒、
C-03 重启后 BindingRevision 不重置 / Tombstone 保留 / stale incarnation 不复活）。

### 5.8 Baseline V1.4 的阶段与门（`BL`）

**三层内容分解**（`BL:34-44`）：「当前真实实现 → 正式升级路线 → FULL L6 Internal 之后的 Enhanced 强化」；
`BL:44`：「任何一项未来能力，在没有对应 Implementation Spec 和源码实现前，**都只能视为设计目标**」。

**两条核心原则**（`BL:31-32`）：

1. **不把未来 L6 / L6 Enhanced 的能力倒灌进 7.14A**；
2. 「所有加强必须服从正式 L6 的架构方向，而**不是靠不断堆 Mixin / Hook / Event / fallback
   形成"看起来很强"的屎山**」。

**每个阶段只迁移一个 Authority 边界**（`BL:316-328`）：Raw Observation → Authority split →
Presence split → Budget → `DurableRootStore` → TerminalWatch takeover → Internal Root；
「**不能把…一次性全部塞进同一个版本**」，且「当前路线必须保持可回滚窗口」。

**正式阶段路线**（`BL:2875-2901`）：
7.14A（Raw Observation 语义分离）→ 7.14B（Authority split）→ 7.14C（Presence / OwnerImmunity split）→
7.14D（Budget seam）→ 7.15（Java `DurableRootStore` shadow）→ 7.16（Durable cutover + TerminalWatch takeover）→
7.17（FULL L5 candidate）→ 8.x（Internal Dominion Root：8.0 Shadow → 8.1 Exclusive Gateway →
8.2 Authority Cutover → 8.3 Recovery/Presence Closure → 8.4 Release Gate）→ FULL L6。

**8.x 的硬前置**（`BL:2649`）：「进入本阶段的前提是 `PROC-INV-1` 已在 7.17 通过；
保留合规 bounded helper **不阻断** 8.x，但任何 helper/sidecar 越界为 Authority writer
或持续 combat plane **都会阻断**」。
`BL:2680`：「**明确限制：** FULL L6 Internal 与 Minecraft JVM **同故障域，不获得外部进程隔离**」。

**四条 P0 大链**（`BL:377-399`，而非横向大模块）：
P0-A Truth Authority（Identity → State Ownership → Durable Truth → Anti-Rollback）·
P0-B Execution Authority（Ownership → Dispatch → Return/Completion）·
P0-C Convergence Authority（Raw Observation → VITALS/TERMINAL → Resource-bounded Reassertion）·
P0-D Integrity Authority（Proof → Scoped Fence → Bounded Repair → Revalidation）。
`BL:444-451` **禁止**实现顺序「先把 A/B/C 全写完 → 最后再想 Integrity 怎么证明」；
`BL:498`：`P0-D` **贯穿 A/B/C**，不是末尾功能包。

---

## 6. 执行与宿主边界

### 6.1 进程边界：`R7`（`CF:193-207`）+ `D13`/`PROC-INV-1`

**禁止**：External Root / 常驻 Authority 服务或 daemon；Named Pipe / 跨进程 IPC 作为 Root 通道；
无界或常驻的外部 fallback；把 child process、external helper、OS 共享内存或本地回环
**提升为 Root transport**（`CF:435`）。

**允许**：`AutoAttach` / helper JVM = **`ALLOWED_FOR_BOUNDED_BOOTSTRAP_ATTACH`**；
进程内 JNI/JVMTI；同 JVM premain；degraded/fenced 的同 JVM fallback。
**条件**：helper **不得持有 Authority truth 或持续 enforcement，必须在 handoff 后退出**。

> ⚠️ **与用户约束 `UX1`（无外部）冲突，且冲突跨三个权威层级。**
> - `L6:15`/`L6:20`（D13）：**不再要求 child-process count 必须为 0**；
> - `L6:874`：**运行期存在 `AutoAttach` helper 本身不导致 FAIL**；
> - `BL:135` 与 `BL:2941`：当前条件式 `AutoAttach` fallback **可保留**，
>   **无需在 7.17 前删除**，但必须通过 `PROC-INV-1` 的用途/Authority-role/handoff/termination 验证；
> - `CF:193-207`（R7）与 `CF:1662-1664`（**ID-9，L6E-0 最终冻结的 9 个 Implementation Decisions 之一**）：
>   **`AutoAttach` / `AttachHelper` 可作为合规 bootstrap path 保留**。
>
> ⇒ `UX1` 是用户 2026-09-12 明示的约束，**效力在项目文档之上**，故 `AutoAttach`/`AttachHelper`
> 仍应移除（`plans/Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md` 的 **C15**）。
> **但代价必须如实记录**：这不是删掉野路子，而是**删掉一条被冻结合同认可的 bootstrap 路径**。
> 替代方案（A1 ModLauncher 服务 / A4 `ImmediateWindowProvider` / I7 进程内 attach 管道）
> 必须同样满足 R7 的边界——**进程内方案天然满足「不得提升为 Root transport」，
> 而重新引入任何外部 helper 来"补能力"会同时违反 `UX1` 与 `R7`。**

### 6.2 执行上下文（`CF:508-536`）

`ExecutionAuthorityContext` 五条硬规则：

1. 一次 critical operation **只从同一个 immutable `ProjectionView` handle 构建一次** context；
   **不得**分别读取 revision、binding 与 policy 造成 **tearing**；
2. subjectKey、incarnationId、operationId、completionContractId、**四类 revision**、epoch、
   mode、fence state+scope、crossing policy、context sequence 为 **native required fields**；
   缺失即 `INVALID_CONTEXT`；
3. context **不可被 Domain 在执行途中 mutate**，也**不得以 extension 覆盖 required field**；
4. 违反 crossing policy → `STALE_CONTEXT`，**不得默认为成功或失败**；
5. context **本身不是 Root Truth**，只是绑定单一 ProjectionRevision 的执行快照。

**并发发布**（`T-13`/`CF:1539-1541`）：旧 `ProjectionView` 的 entry/epoch/policy/fence/revision
**必须来自同一快照**；**禁止 torn read**。

### 6.3 同步查询 vs 可延期工作（`BL:2584`、`CF:1521-1523`、`L6:733`）

**不得**因预算耗尽而延迟、返回假值或临时改变可见性的**同步关键查询**：
`AuthorityView`、`PresencePolicy` / `QuarantineView`、当前对象的 admission decision。

**可以**在预算耗尽后顺延的：仅**可延期持续工作**；超预算时**只能记录积压并在后续 tick 继续**；
**不能把预算耗尽本身当作同步查询结果**。

**热路径硬规则**：Pre-Binding 热路径**不得同步执行 durable Root I/O**（`BL:1235`）；
realtime query **不创建** Root mutation envelope，只消费 `ProjectionView`（`CF:436`）；
admission hot path 只允许消费当前 immutable `ProjectionView`（`CF:684-690`）。

### 6.4 预算与资源（`L6:722-748`、`L6:1188-1200`）

| 项 | 值 |
|---|---|
| 每 tick 总预算 | **≈2000 µs**（per-server `FrameBudgetGovernor`，**进程内不共享预算**） |
| 分配 | terminal ≤500 · capability ≤400 · discovery ≤300 · admission ≤400 · observation ≤200 · maintenance ≤200 µs |
| 性能门 | tick p50/p95/p99 ≤ 冻结基线 **+5% / +5% / +10%** |
| Root commit（L6） | 本机 p99 ≤ **5 ms**；timeout = p99×4 |
| Judgment Action | ≤1 commit 单元 + ≤1 发布周期；事件驱动 control-plane，**不占 tick 预算** |
| terminal worker | ≤0.5 ms/tick |
| 全量 attestation | 事件驱动 + 低频 watchdog（**非每 tick**） |

**「不可调」的不变量**（`L6:1200`）：realtime query **零同步 durable I/O** ·
stale epoch/fence/realm **零接受** · acknowledged lost commit **零容忍** ·
Judgment 扩展性**次线性**（D6）· 未证明 `COMMITTED` 前**零本地不可逆效果** ·
`COMMIT_UNKNOWN` **不得视为 `REJECTED`** · 同一 `command_id` 重传**不得产生第二次 durable mutation** ·
realm ownership 无法证明 ⇒ **mutation blocked**。

**基线冻结**（`L6:1059-1072`）：7.14 之前必须在**未修改的 7.13 二进制**上采集冻结性能基线；
tick p50/p95/p99 明确**不是** `RONOVA_ACTION elapsed_us`；后续所有版本 gate 与该基线比较。

### 6.5 宿主安全域（`D81`/`D82`/`D96`）

必须覆盖：heap · direct/mapped buffer · metaspace · code cache · 线程与栈 · class/loader ·
JNI/JNA/JVMTI native allocation · 文件/平台句柄 · **可观测** GPU/render allocation。

**Ronova-owned usage 与 whole-host pressure 必须分开**：敌方制造的全局压力**只能**触发背压、分片和紧急预算，
**不能**删除攻击面、撤销 Judgment 或伪造成功。

**Host Safety Governor / Loop Guard / single-writer / bounded queue / 资源硬预算
在所有预设中始终启用；EXTREME 只提高覆盖、证据和处置能力，不能关闭宿主安全来换强度。**

### 6.6 活性与降级（`CF:858-903`、`CF:989-1007`）

- 状态：`NORMAL → DEGRADED → STORM → CRITICAL_LIVENESS_UNPROVABLE → FENCED / FAIL_STOP`；
- **可以**停：audit、diagnostics、低优先 reconciliation、非关键投影维护；
- **必须不停**：Root / fence 处理、Tombstone awareness、Identity safety、Admission safety、critical Presence revoke；
- 每个 accumulator 必须声明 `maxEntries` / `maxBytes` / `maxAge` / `overflowPolicy`；
  允许的溢出结果**只有** `COALESCE_WITH_SUMMARY` / `DROP_NON_CRITICAL_WITH_COUNTER` /
  `DEGRADE_DOMAIN` / `FENCE_DOMAIN`；
  **禁止**无计数覆盖、无界增长和把 overflow 解释为「无偏差」；
  计数器用 **saturating arithmetic** + `counterSaturated=true`，**禁止 long wrap 成负数或零**。

---

## 7. 极限档的绝对安全边界与代价（`V2.6` §4.4 / §22.4）

### 7.1 绝对安全边界 —— 13 条（`V2.6:431-445`）

> EXTREME **不允许**通过以下方式取得胜负：

1. 故意制造 OOM；
2. 无限创建线程；
3. 无限类生成；
4. 无限 retransform / redefine；
5. 无限每 tick JVM 全扫描；
6. 故意死循环吃满 CPU；
7. **破坏 JVM / native 内存来求强度**；
8. 删除第三方文件；
9. 损坏世界存档或区块文件；
10. 修改、删除用户文件；
11. 操纵无关进程；
12. OS / 内核破坏；
13. 故意蓝屏、死机、崩溃宿主电脑。

`V2.6:2044`：「任何 Extreme executor 均**不得**通过耗尽 heap、direct/native memory、metaspace、
code cache、线程栈、类加载容量、平台句柄或 GPU/render allocation 来获得对抗优势。」
`V2.6:2062`：「**不允许故意继续分配直至 JVM、游戏进程、显卡驱动或宿主系统崩溃。**」

### 7.2 必须计的代价（`V2.6:449`、`:3288`）

> **EXTREME 不计第三方兼容性代价，但必须计宿主稳定性、Authority 正确性、
> 用户数据安全和 JVM 可持续运行的代价。**
>
> **Ronova Extreme 可以不计敌对 Mod 的兼容性和功能存活代价，
> 但不能不计 Minecraft/JVM 宿主、用户数据和操作系统的安全代价。**

代价账本是 **10 个 `HostResourceDomain`**（`V2.6:2011-2022`）：
`JAVA_HEAP` · `DIRECT_BUFFER`/`MAPPED_BUFFER` · `METASPACE` · `CODE_CACHE` ·
`THREAD`/`THREAD_STACK` · `CLASS`/`CLASSLOADER` · `JNI_JNA_NATIVE` · `JVMTI_OWNED` ·
`FILE_DESCRIPTOR`/`PLATFORM_HANDLE` · `GPU_UPLOAD`/`RENDER_BUFFER`。

每个域必须报告（`:2027-2032`）：`supported/observed/unobserved` · `ownedUsage/ownedPeak/allocationRate` ·
`hostPressure` · `softBudget/hardBudget` · `throttleState/emergencyState` · `lastReleaseProgress`。

`V2.6:3185`：每个启用的 Extreme executor **必须声明**它可能消耗的 `HostResourceDomain`、
归属计数器、软/硬预算、释放路径**和不可观测项**；**禁止**用 `Runtime.freeMemory()`、
单次 heap 快照、捕获 `OutOfMemoryError` 或 `System.gc()` 来"证明"宿主安全。

### 7.3 明确不保证的（`V2.6:419-427`）

在「已证明属于当前敌对结果链 + 相应处置获得当前 Root 政策授权」之后，EXTREME **不保证**：

- 对方 Mod 的**功能完整性**；
- 对方 Mod 的**热补丁仍能工作**；
- 共享 helper / manager 在对抗后仍完整服务所有第三方功能；
- 对方自己的恢复器、listener、runtime patch、generated class 继续保持原语义；
- 对方 Mod 在当前会话内能够**恢复到"正常玩法状态"**。

`V2.6:427`：「**高度疑似"只授权临时可撤销限制**，「不自动授权永久、类级或高副作用改写」。

### 7.4 禁止写死的架构上限（`V2.6:404-415`）

**不允许**写成固定架构限制：最多 1/2/N 个敌对 Mod · 最多 N 个结果图节点 · 最多 N 层逻辑链深度 ·
最多 N 个 loader 副本 · 最多 N 个任务/回调来源 · "只攻击明确注册过的某几个 Mod"。
`V2.6:415`：「**资源预算限制每个窗口处理多少，不限制最终应该处理哪些。**」
`D62`：EXTREME **不设**敌对 Mod 数量上限。

### 7.5 资源与容量（`V2.6` §12.2 / §14.6 / §20）

**核心原则**（`V2.6:3131`）：

> **预算只决定「什么时候处理」，不决定「它是不是攻击面」。**

- `V2.6:3111`：「Extreme 可以更重，但 **更高预算 ≠ 无预算**」；
- **零语义截断四条**（`V2.6:2456-2461`）：
  ```
  记录精度允许下降
  未解决事实不能消失
  约束不能因压缩自动变弱
  攻击成功不能由容量变化推导
  ```
- governor 约束量（`:1998-2007`）：worker 数量 / task queue / evidence queue /
  **class·retransform rate** / repair rate / JIT·native verification rate / per-source flood /
  backlog age / memory cap / CPU time budget；「`memory cap` **不得只解释成 Java heap**」；
- 必须为 Authority command、durable receipt/recovery、必要 gate、最小 repair/reverify
  **保留独立应急预算**（`:2040`）；
- 敌方制造的全局压力**只能**触发背压、分片、延后或低分配替代路径，
  **不能**据此删除攻击面、撤销 Judgment、释放旧 TargetEpoch、清空 obligation 或报告 CLEARED（`:2039`）；
- 存储容量九条（`:3202-3218`），其中 `:3218`：「本合同是**可持续性要求**，
  **不是用磁盘耗尽对抗其他 Mod 的攻击方式**」。

### 7.6 极限档"非免疫"的明确声明

- `V2.6:3258`：「如果对方完全控制同地址空间，**不能宣称数学绝对不可篡改**」；`:3268`：「**不能虚构物理绝对免疫**」；
- `INTERCEPT_JNI_NATIVE`：「**不宣称对同权限 Native 绝对免疫**」；`INTERCEPT_JVMTI`：「不可观测部分**必须报告 `UNOBSERVED`**」；
- `V2.6:3272-3280`：原 JVM 死亡后**不能继续自救**；恢复只能靠已提交 durable truth + 干净重启 + 新 JVM 重建 Root 与证据；
- `V2.6:3284`：「OS / 文件 / 无关进程 — **不属于 Extreme 攻击面**」；
- `V2.6:3198`：「资源安全测试证明的是『**Ronova 不用摧毁宿主来赢**』，
  不是保证敌方代码无法自行崩溃进程」。

---

## 8. 客户端 / 服务端边界

| 规则 | 内容 | 来源 |
|---|---|---|
| **R4** | 服务端**强保证** server truth / logical identity / lifecycle / presence / server-owned projection；**不保证**任意恶意客户端的最终屏幕像素、外部 overlay、已失陷客户端进程 | `CF:140-157` |
| **§20** | 服务端主线**只**负责 Truth / Identity / Lifecycle / Presence / Server-owned Projection / Projection Revocation / Network Anti-Replay；**禁止**在 Server Core 里预留或硬依赖 GPU resource id、Visual capability、VAO/VBO lineage、Frame render token、Native render metadata | `CF:1149-1171` |
| **§21** | `TERMINAL(X)` 时服务端必须 stop authoritative presence → revoke tracking → stop entity updates → revoke server-owned BossBar → revoke known server-owned projections；**不保证**「恶意客户端不继续画 X」 | `CF:1194-1211` |
| **`T-07`** | 服务端**不宣称**其像素被清除；**不因此**把 Client Render 变成 FULL L6 必需条件 | `CF:1497-1505` |
| **`ID-6`** | **`VisualSubject` / Render Firewall 等全部归可选 Client Extension** | `CF:1634-1646` |
| **`D92`** | 服务端 Root Truth 与客户端投影**严格分离**；Presentation **只显示** accepted projection；Input **只生成**带身份、epoch、nonce 的请求；**专用服务端不得加载客户端渲染实现** | `V2.6:561` |
| **`BL:1971`/`BL:1988`** | 客户端只是 projection consumer，**不应产生新的 Authority intent**；Client Render 状态**不能反向决定** Server Truth | `BL` §17.3 |

客户端扩展命名空间（`CF:1175-1190`）：`client.visual.*` / `client.projection.*` /
`client.integrity.*` / `experimental.render.*` —— **默认 optional，不参与核心 Authority，
不影响 Server FULL L6 Internal 判定**。

---

## 9. 副作用合同（`CF` §16 + `ID-2`）

四类，**封闭**：

| 类别 | 处置 | 约束 |
|---|---|---|
| `RONOVA_CONTROLLED` | **suppress before publish** | 只限 Ronova 自有发布路径（自有包、服务端自有投影、自有 BossEvent、自有 presence 发布） |
| `REVERSIBLE` | rollback | **必须**：有 revision；可验证；**不产生二次不一致** |
| `COMPENSATABLE` | compensate | 不保证精确回滚，只做已知补偿；**补偿失败必须产生 Incident** |
| `EXTERNAL_OPAQUE` | observe → incident → 可选 degrade/fence | **不得盲目修改第三方系统** |

- **默认策略**（`ID-2`，`CF:1577-1588`）：Ronova-controlled → suppress / known reversible → rollback /
  known compensatable → compensate / external opaque → incident。
- **强制**：**不构建全局副作用总拦截器**；`sideEffectScope` 按 CompletionContract 逐合同限定。
- **压缩规则**（`CF:947-963`）：**"Coalesce work, not security evidence."** ——
  允许「10000 次写 → 1 个 repair job」；**不允许**「非法 death transition → 因最终 snapshot 恢复而丢证据」。
- **持久化归属**（`CF:1098-1104`）：本地缓存 Graph **可重建**；Root **只持久化**已确认的
  Authority Binding 与必要的 identity/ownership relationship；**不持久化所有临时 discovery noise**。

---

## 10. 强度模型（`V2.6`）

**三个平面**（`D89`）：Attack / Interception / Protection。**不得创造第四平面**或第二 Judgment 状态机。

| 目录 | 数量 | 构成 |
|---|---|---|
| `StrengthOptionKey` | **54** | 15 `attack.*` + 10 `intercept.*` + 17 `protect.*` + 12 `runtime.*` |
| `AttackAction` | **14** | strongAttack / rewriteLife / terminalize / directClear / clearEntityRecords / suppressRespawn / suppressRebuild / suppressAdmission / attackRender / clearTracking / clearHud / stripCodeDefense / attackCallChain / allReturn |
| `RuntimeSurface` | **8** | DATA / NETWORK / PRESENTATION / INPUT / EVENT / TEMPORAL / REGISTRY / RESOURCE |

### 10.1 三档正式预设（`V2.6:41-45`、`:600-604`）

`COMPATIBLE 兼容` / `ENHANCED 强化` / `EXTREME 极限代码对抗`，加 `CUSTOM`。
EXTREME「**不再被定义成"兼容档的更高数值版本"**」（`V2.6:47`）。

| 档 | 范围 | 兼容策略 | 失败策略 |
|---|---|---|---|
| COMPATIBLE | Ronova 核心 + 稳定实体路径 | 优先兼容 | 非关键路径记录/降级，**Authority 不失真** |
| ENHANCED | Ronova + 当前目标 + 已识别直接结果链 | 强度优先、尽量精确 | 对相关对象/类/调用隔离、修复或对象级 fence |
| EXTREME | 当前 Judgment 的全部候选 / 已证明同 JVM 结果攻击面 | **不保护敌对 Mod 兼容性** | 先限制未知结果资格；无法闭合时按**最小必要 scope** fence |

`V2.6:400`：「**胜负结果优先，第三方兼容性不是保护目标。**」

### 10.2 两套**不同**的层级值集（不要合并）

| 用于 | 取值（升序） |
|---|---|
| **Attack / Protection 子项** | `跟随预设 / 关闭 / 兼容 / 强化 / 极限`（FOLLOW_PROFILE / OFF / COMPATIBLE / ENHANCED / EXTREME） |
| **拦截机制子项** | `跟随预设 / 关闭 / 监控 / 保护若娜瓦 / 加强相关路径 / 全部严格`（FOLLOW_PROFILE / OFF / MONITOR / PROTECT_RONOVA / RELATED_PATHS / STRICT_ALL） |

预设→机制映射（`V2.6:640-642`）：兼容 → 保护若娜瓦；强化 → 加强相关路径；极限 → 全部严格。

### 10.3 五个策略字段与组合算子（`V2.6:649-673`、`:765-774`）

```
configuredLevel
supportedCeiling
effectiveLevel
evidenceState
activationState
```

组合**不是 `min(...)`，而是集合交集 `∩`**：

```
ProfilePresetMatrix(profile, optionKey, matrixRevision) → requestedLevel

requestedLevel ∩ supportedCeiling ∩ evidenceCeiling ∩ restart/activation state
    → effectiveLevel
```

等价推论（`V2.6:878`）：「`effectiveLevel` **永远不高于**运行时 ceiling 与证据 ceiling；
缺少 Native/JVMTI、需要重启或专用服务端不支持客户端模块时**必须真实降级并显示原因**。」

**四个正交状态轴**（`V2.6:677-698`、`:757-759`，`D73`）：

```
executionState:  ACTIVE | QUARANTINED | FENCED
attackOutcome:   APPLIED | PARTIAL | CLEARED | FAILED
fenceScope:      NONE | TARGET | OPERATION | PLANE | ROOT | REALM
```

- 「策略的 `activationState`/`evidenceState`、结果的 `executionState`/`attackOutcome`
  和 Authority 的 commit 状态**禁止互相覆盖**」；
- 「**FENCED 不能推导攻击成功**」（`D73`）；
- 旧字段 `clearanceState` 降级为只读兼容展示，**不再拥有独立决策逻辑**。

**完成声明**（`V2.6:714-727`）：非清除 CriticalOperation **只有 `attackOutcome=APPLIED AND
completionSatisfied=true`** 才能显示"攻击已完成"；**禁止**用 `isTerminalOutcome` 表达该含义
（`TERMINAL` 保留给目标存在性终结语义）。

### 10.4 表面成熟度阶梯（`V2.6:1259-1267`）

`S0 — Absent` / `S1 — Observe` / `S2 — Attribute` / `S3 — Enforce` / `S4 — Repair` /
`S5 — Recover` / `S6 — Complete`（源码枚举名 `S0_ABSENT`…`S6_COMPLETE`）。

| 级别 | 必须具备 | 可声明 |
|---|---|---|
| S0 | 无可靠 observer | `UNAVAILABLE` |
| S1 | 有界观察与 revision | `MONITOR`；**不能声称拦截/恢复** |
| S2 | 来源、上下文与证据独立性可验证 | 可进入 Candidate；不能自动高副作用处置 |
| S3 | 有真实 gate/executor 与 denial/effect receipt | 按实际报告 DENIED/APPLIED；**穿透不得伪报成功** |
| S4 | 能恢复受保护结果并重验 | 可报告恢复动作；未闭合仍为 PARTIAL |
| S5 | 重启、epoch、任务、网络、保存链可恢复 | 可承接跨时序 obligation |
| S6 | CompletionContract、稳定窗口与证据失效重评完成 | 才可声明该 Surface 对相应操作 `FULL` |

`V2.6:1269`：「EXTREME 要求当前操作所依赖的**所有** Surface 达到其 CompletionContract 指定成熟度；
一个表面只有 S1，**不得**因为其他表面 S6 就把整项能力显示 FULL。」

### 10.5 逐键矩阵与三档差异性（`D91`、`V2.6:877`）

- 三个正式预设**从 7.17 起**由 `ProfilePresetMatrix(profile, StrengthOptionKey)` 逐键解析；
  r52 的按-plane 统一映射「**仅是 7.16 policy foundation，不是 V2.5 最终矩阵**」；
- `V2.6:877`：**同一 `StrengthOptionKey` 的三档必须命中不同生产策略，
  或明确声明相同并给出原因；不得三个档位共用一个 executor 参数却显示三档**；
- `V2.6:925`：Host Safety、Loop Guard、single-writer、防回滚、基础 Admission 与资源安全
  **不是攻击许可的副作用，不可通过普通 OFF 关闭**；UI 必须说明"关闭主动加强，不解除已有裁决"。

### 10.6 `AttackAction` 的逐档许可（`V2.6:895-910`）

五个动作在 **COMPATIBLE 档为「关」**，ENHANCED / EXTREME 为「开」：
`directClear` · `suppressRebuild` · `stripCodeDefense` · `attackCallChain` · `allReturn`。
其余九个三档皆「开」。每个动作另有**所需高级键**的约束（例如 `allReturn` 需 `ATTACK_DISPATCH`；
改字节码时另需 `ATTACK_DYNAMIC_CODE` 或 `ATTACK_RUNTIME_PATCH_DISABLEMENT`）。

**六条冲突规则**（`V2.6:920-925`）中最关键的两条：

- 所需普通许可为 DISABLED → **拒绝该 step，报告 `ACTION_DISABLED`；不得自动升档或开启依赖**；
- 关闭能力**只约束新 step**；撤销已有 Judgment **必须另走显式 Authority command**，
  已提交义务不被删掉。

---

## 11. 非法组合（`V2.6:2828-2885`）

判定清单：出现下列组合即视为**设计错误**，不是「实现得不够好」。

**基础（`:2830-2843`）**：EXTREME 显示 ACTIVE 但关键 executor 不存在 ·
极限清除无 TargetEpoch/Admission/stability/recovery closure · 反全返回无 final state evidence ·
全返回无 final result takeover · 动态代码只有观察却显示已完成 · 只能观察却声称 DENIED ·
budget overflow 后删除 obligation · 单个 TARGET 失证直接 ROOT FENCE ·
合法 reactivation 复用旧 TargetEpoch · ResultGraph/Certificate 直接作为 Judgment/Admission Truth ·
7.16 为 V2.2–V2.5 重做完整 ResultGraph/MutationTrace/executor ·
以资源耗尽/无限扫描/文件存档 OS 破坏增强 EXTREME。

**V2.3（`:2845-2858`）**：重复日志凑独立弱证据 · 合规修复因触达 sink 被自动隔离 ·
仅因分数高/等待久获得高副作用权限 · 只合并重复 key 而允许无限增长 ·
先删细节后建汇总 · LRU/TTL/摘要变化直接解除义务 · 一个来源无关就解除整个汇总范围 ·
FENCED/QUARANTINED/函数返回成功直接映射 CLEARED · UI 显示历史 CLEARED 作为当前 ·
更换来源身份重置配额 · 无 Authority/repair 保留预算 · 以新增合同要求 7.16 完成全套。

**V2.4（`:2860-2871`）**：资格各要素分开提交或分开发布 · `COMMIT_UNKNOWN` 时新建第二 command 补写 epoch ·
qualification transaction 内调用第三方代码 · 仅凭 APPLIED 显示最终完成 ·
`isTerminalOutcome` 混淆 CriticalOperation 完成与实体 TERMINAL · 篡改证据结论实现防抖 ·
防抖偷偷延迟操作者 clear 命令 · 只监控 Java heap 却宣称全资源域安全 ·
把不可观测资源视为无限 · 以 OOM/句柄泄漏/驱动崩溃冒充攻击成功。

**V2.5（`:2873-2885`）**：任一 Surface 保存可与 Root 竞争的 Truth ·
新增八套互不兼容的攻击 key/GUI 页/状态机 · 仍按 plane 统一映射却宣称逐键矩阵 ·
只有 observer 就显示 FULL · 客户端 HUD/renderer/keybind/packet cache 直接提交 Judgment ·
专用服务端加载客户端渲染类再用异常降级掩盖 · 各 Surface executor 彼此调用/循环修复/覆盖 Root 仲裁 ·
跨表面计划不绑定七要素 · 一个表面完成就忽略 required surface 缺口 ·
扩大全局 Mixin/Unsafe/ClassLoader/JNI 禁用范围替代来源归属 · 把 r52 占位接口写成 exact-artifact 已完成。

---

## 12. 完成判定的元原则

Ronova 的完成判定不靠「功能看起来有了」，而靠下列元原则：

1. **能力按实际状态报告**（`D94`/`CF:160-174`）——UNOBSERVED 不许写成 ACTIVE；「发出补丁」不等于生效；
   `UNKNOWN` 不是 `ACTIVE`，不能通过布尔默认值变成「可执行」。
2. **不得把设计要求写成已实现**（`L6:19`/`D86`/`D98`/`D106`）——尤其 r52/r53 与 7.16/7.17 的边界。
   `BL:44`：「任何一项未来能力，在没有对应 Implementation Spec 和源码实现前，都只能视为**设计目标**」。
3. **不得删除未决事实换取整洁**（`D55`/`D71`/`D82`/`D105`/`CF:947-963`）——
   预算、压缩、LRU、TTL、背压都不是解除义务的理由。
4. **修复成功不等于信任恢复**（`R6`）——repair → revalidate → PASS → ACTIVE，否则继续 FENCED。
5. **不得只引用不可得原文而宣称验收闭合**（`V2.6:154`）；**不得用新文案篡改历史运行结果**。
6. **阶段性 gate 不得被后来者倒填**（`D65`/`D78`/`D86`/`D98`）——V2.2–V2.6 的设计不得写成 7.16 已实现。
7. **明确不宣称的边界要写进文档**（`L6-INV-10`）——JVM 完全失陷不属隔离保证，**此限制不得隐藏**。
8. **同类必须真不同**（`V2.6:877`）——同一 `StrengthOptionKey` 的三档**必须命中不同生产策略**，
   或明确声明相同并给出原因；「**不得三个档位共用一个 executor 参数却显示三档**」。

> **最终完成判定的量化门槛**（不逐条罗列，给出位置）：
> - `V2.6` §19.2 Completion Gate 11 条（`:3015-3028`）；
> - `V2.6` §24 最终完成判定 **58 条编号条件，全部必须成立**（`:3393-3452`）；
> - `L6` §12.3 PASS/FAIL Gate（`:1204-1211`）与 FULL L6 tag（`:1208`）；
> - `P0A` §60 退出条件（`:2158-2236`）。
> 其中最值得记的是 `V2.6` §24 #1「Compatible / Enhanced / Extreme 使用**真实不同** production policy」
> 与 #58「7.17 各批与 L6/L6E 的状态、schema 和成品证明**分开**，文档修改不改变生产实现/验证状态」。

---

## 13. 覆盖进度

| 来源 | 状态 |
|---|---|
| `CONTEXT.md`（冻结词汇） | ✅ **全文并入**（§2，19 条） |
| `docs/plans/README.md`（规范顺序） | ✅ 并入（§1） |
| `docs/CURRENT-PROGRESS.md` | ✅ 作为框架引用 |
| **`L6` V2.4（最高权威，1468 行）** | ✅ **已并入**：§1 顺序 · 行号口径 · §4.2 **D1–D13 逐条** · §4.3 Single-Writer · §4.4 状态所有权表 · §4.5 模块「不做什么」 · §4.6 编译期依赖 · §4.7 Presence 范围 · §5.1 `PROC-INV-1` · §5.2 **L5-INV-1…9** · §5.3 **L6-INV-1…10** · §6.4 预算与性能门 · §6.5 宿主安全域 · §12 元原则。**并记录其内部不一致（§14 #1）** |
| **`CF` L6E-0 V1.3（1695 行）** | ✅ **已并入**：§2 词汇补 `PolicyRevision`/`DominionEpoch`/`AuthorityResult`/`ProjectionView` · §3 身份与修订合同 · §3.5 CrossingPolicy · §3.6 操作/完成标识 · §3.7 冻结接口集 30 名 · §5.6 宪法级不变量 + **R1–R7** · §6.1 进程边界 · §6.2 执行上下文 · §6.6 活性与降级 · §8 客户端边界 · §9 副作用 |
| **`BL` V1.4（3022 行）** | ✅ **已并入**：§1 顺序 · §5.8 三层分解 / 两原则 / 一阶段一边界 / 正式阶段路线 / 8.x 硬前置 / 四条 P0 大链 · §6.1 `PROC-INV-1` 的 V1.4 措辞 · §6.3 同步查询 vs 可延期 · §6.4 性能基线（在未修改的 7.13 二进制上冻结） |
| **`V2.6` Strength Master（3460 行）** | ✅ **已并入**：§4.1 Internal Root 不变 · §5.4 `D33` 修订 · §5.5 **`D53`–`D106`** · §6.5 宿主安全域 · **§7 极限档绝对边界（13 条）+ 必须计的代价 + 非免疫声明 + 禁止写死的上限 + 资源容量** · §10 强度模型（三档 / 两套值集 / `∩` 组合 / 四正交轴 / S0–S6 / 逐键矩阵 / AttackAction 逐档许可） · §11 非法组合全文 |
| **`P0A` P0-A V1.3（2298 行）** | ✅ **已并入**：§5.7 **`A1`–`A8`** + §32 宪法级不变量 + Truth-only-one-way + 缺席证据禁止 + 不新增平行体系 + 测试编号族 `U`/`I`/`C` |
| `V2.6` §19.2 / §24 58 条 / `P0A` §60 | ✅ 已给位置，未逐条罗列（§12 注） |
| `BL` §24「明确淘汰 / 不进入主线的旧加强」14 行表 · §25.x 各阶段冻结清单全文 | ⏳ 未逐条并入（属实现范围，不属跨文档合并的必要项） |

**覆盖结论**：五份规范 + 词汇表 + 进度口径**已全部合并**；剩余 ⏳ 项是实现范围清单，不是规范缺口。

---

## 14. 已知的文档级问题（如实记录，不代为裁决）

| # | 问题 | 位置 |
|---|---|---|
| 1 | **`L6-INV` 两张表内容不同**（同 ID，2、4–10 不一致），且 `:1200` 引用了两张表都没有的 `L6-INV-10` 含义 | `L6:861-873` vs `L6:1343-1354`，另见 `L6:1200` |
| 2 | **`UX1`（无外部）覆盖四个层级上明确允许 `AutoAttach` 的条款** | `L6:15`/`L6:20`/`L6:874`、`BL:135`/`BL:2941`、`CF:193-207`/`CF:1662-1664`、`V2.6:181` |
| 3 | `L6E-0` 无 `CF-*` / `INV-*` 编号；宪法级不变量是**无编号名字** | `CF:826-843` |
| 4 | `L6E-0` **无显式线程/lane 规则**（无 server-thread、tick-lane 或「必须在 X 线程」陈述）；执行上下文只由同 JVM 局部性、单快照 `ProjectionView`、热路径禁同步 durable I/O 约束 | `CF` 全文 |
| 5 | **三个制品名在 `V2.6` 中不存在**：`AttestationManifest`、`EnforcementLease`、`LoaderProofRegistry` 各 **0 命中**。`V2.6` 用的是 `ExecutionLease`（§13.2）与 `LoaderProof`（§16.2，仅一次、未展开）。**本工作区的 `Ronova_NonViolating_Enhancement_Plan_V1.0.md` / `..._V2.0.md` 的 F 块引用了这三个名字并标注「L6:262/263/264」——出处待核，不得据此归属于任一已读规范** | `V2.6` 全文（grep） |
| 6 | `P0-A` **无 `L6-INV-*`、无 `P0-A-*` 编号**；其不变量是 `A1`–`A8`。任何把它写成 `L6-INV-*` 的引用都是错的 | `P0A:94-197` |
| 7 | **行号口径陷阱**：`Measure-Object -Line` 只数非空行，会把 `L6` 报成 1177（实为 1468）、`CF` 报成 1201（实为 1695）、`BL` 报成 2169（实为 3022）、`V2.6` 报成 2596（实为 3460）、`P0A` 报成 1631（实为 2298）。**引用行号必须用逐行实读值** | 全部五份 |
| 8 | `BL` §30 的映射表表头写作「历史加强项 / 历史 **V1.1** 最终归属」，而该节标为 V1.4——表体是历史→章节的指针表，**不是 V1.4 的权威声明** | `BL:2947-2961` |
| 9 | `CONTEXT.md` 19 条中有 8 条（`Owner Immunity`、`Public Presence`、`Terminal Watch`、`Judgment Eligibility`、`Lifecycle Integration`、`State Reassertion`、`Raw Observation`、`Physical Incarnation`）**在 P0-A 中完全不出现**；P0-A 有一批自己的平行词汇。词汇表的"冻结"与 P0-A 的实际用词存在**覆盖缺口** | `CONTEXT.md` vs `P0A` |
| 10 | `L5-INV-6` 自标为**候选/INFERENCE**，未达则 `L5_COMPAT`；不算 FULL L5 必要条件 | `L6:526-529`、`L6:335` |

---

## 附：本导读不做什么

- **不替代**任何原始规范；冲突以原始文档为准（§1.1）。
- **不新增**规范顺序、Root、身份或能力编号。
- **不记录实现状态**——只看 `docs/CURRENT-PROGRESS.md`。
- **不声称**任何能力已实现或验收已通过。
- **不裁决**§13 的文档级问题——按 §1.1 规则 2 登记为设计缺口。

---
---

# 第二部分 · 加强方案（**提案，非规范**）

> 第一部分的全部内容是**已冻结的规范**；从这里开始是**尚未落地的设计提案**。
> 二者放在同一文件只是为了能一次读完"现在是什么"和"下一步做什么"。
> 本部分**不修改**任何规范条款，也**不声称**任何能力已实现。
>
> 本部分**取代**下列文件（它们仍是历史，不再作为加强入口）：
> `Ronova_NonViolating_Enhancement_Plan_V1.0.md`（W4/W7/W8 无依据）·
> `Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md`（其 F 块引用了三个不存在的制品名，见 §14 #5）·
> `Ronova_MS_Remediation_Spec_V1.0.md`（并入 §18 的 C 块）·
> `Ronova_Compliant_Maximum_Strength_Plan_V1.0.md` · `Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md`。

---

## 15. 三层规则源（判定力完全不同）

| 层 | 内容 | 判定力 | 违反的后果 |
|---|---|---|---|
| **L0 · MS 铁律（共识）** | `skills:62-64` 三条 + `skills:398`（Forge 文件） | **强制** | `skills:66`：「不算个秒杀 Mod」 |
| **P · 项目自有规范** | §4–§12 的 `D1`–`D106`、`L5-INV`/`L6-INV`/`PROC-INV-1`、`A1`–`A8`、`R1`–`R7`、宪法级不变量 | **对 Ronova 强制**（自己冻结的，自己得守；`CF:21-23`「后续实现不得通过'局部方便'破坏全局语义」） | 架构不成立 / full L5、L6、L6E 不得宣告完成 |
| **UX · 用户明示约束** | **`UX1` 无外部**（不起子进程） | **强制，效力最高** | —— |

**两层的关键区别**：`L0` 决定 Ronova **算不算 MS Mod**；`P` 决定 Ronova **自己的架构是否成立**。
两者可以独立失败——**可以既不违铁律、又违反自己冻结的规范**。§16.2 就是这一类。

**MS 文档其余全部文本**（七字段契约、重转换纪律、启动顺序、失败策略…）按用户 2026-09-12 口径属
**L1 自定层**：MS 的「必须/不要」在那里只是**建议强度**，采纳与否 Ronova 自己决定，**不是违规**。

---

## 16. 合规判定

### 16.1 对外（L0 铁律 + UX1）

| 项 | 状态 | 依据 |
|---|---|---|
| **L0 · R1 不修改文件** | ✅ 干净 | 只写自有产物；跨所有权 `SavedData` 写入结论见 §16.3 |
| **L0 · R2 不针对** | ✅ 干净 | 全仓库无按第三方包名 / 类名 / Mod ID / 私有实现的判定路径 |
| **L0 · R3 不删文件** | ✅ 干净 | native 侧 `DeleteFile\|MoveFileEx\|ReplaceFile\|RemoveDirectory` = 0 命中 |
| **L0 · R4 不改 Forge 文件** | ✅ 干净 | —— |
| **UX1 无外部** | ❌ **一处违反** | `agent/AutoAttach.java:26-33` + `AttachHelper.java:13-21`（全主源码唯一子进程派生点）→ §18 的 **C15** |

**对外结论：铁律全部干净；唯一强制整改项是 `UX1` 的 C15。**

### 16.2 对内（P 项目自有规范）—— 本轮整合新增的维度

这一列以前从未核过。逐条对照五份规范的冻结条款：

**已满足（实测）**：

| 规范条款 | 实测结果 |
|---|---|
| `L6:487-494` 五条编译期依赖禁令 | ✅ **全部通过**：`observation/` 不 import `presence/`；`presence/` 与 `TargetDiscovery` 不 import `AuthorityBinder`；`persistence/` 不 import `integrity/`；`contract/` 不 import `net.minecraft.*`；`capability/` 不 import `persistence/`、`root/` |
| `L5-INV-4` Raw/Presence 完全分离 + immunity 内化 | ✅ 与 `D1`、`CF` §6 正交性一致 |
| `R1` No Mod-ID Authority（`CF:82-96`） | ✅ 无 `if modId == X → …` |
| `D5` Lifecycle Integration ONCE / State Reassertion REPEATABLE | ✅ 语义已建模 |
| `A1` / `A6` / `A7`（身份稳定 / Projection 非 Truth / Graph 非 Root） | ✅ 结构上成立 |

**违反或高风险（P 层，不是 L0）**：

| # | 条款 | 现状 | 严重度 |
|---|---|---|---|
| **P1** | `A5` + `L6-INV-1` + `D3` + `V2.6:179-180`：「Native **不能成为第二 Judgment writer**」「Native 任何阶段都不做 authoritative durable write」 | `native_root.cpp:353-410 decide()` 是独立判定内核，且 `NativeDominionDecisionSource` 是 **native 优先 + Java 兜底**；4 个判定域中 3 个在生产路径上是死代码（`Authority`/`Admission`/`Presence` 全走 Java）。**Java 还会二次裁决 native 的答复**（`LocalDominionCore:26-31`） | **高**——这是对自己冻结架构的偏离，不只是 MS L1 建议 |
| **P2** | `D91` + `V2.6:876-879` + `V2.6:2885`：三个预设**必须逐键解析**；同一 key 的三档**必须命中不同生产策略**；**不得**把 r52 的统一映射写成已完成 | `ProfilePresetMatrix:145-150` 只实现三种**统一填充**（`filled(STRICT_ALL/RELATED_PATHS/MONITOR)`）；`/ronova strength status` 对 14 个动作中的 13 个显示「未接线（仅保存请求）」 | **高**——铁律无关，但**直接卡住 FULL L5 / V2.5 的完成判定**（`V2.6` §24 #44、X51） |
| **P3** | `L5-INV-8`：「同一语义**不得**由 Mixin + Event + Agent 三份实现」；生成拒绝**只保留两个门**（`NBT-gate` + `ENTITY_MANAGER-sink`），其余委派 | **NBT 编码链三层都有**：Mixin（`CompoundTagWriteObservationMixin` / `ListTagWriteObservationMixin` / `NbtCompressedWriteObservationMixin`）+ Agent（`NbtCopyContract` / `NbtFactoryContract`）+ Native（`CompoundTag` 位 `0x2000`、`EntityType` 位 `0x4000`） | **中**——不是"三份实现同一语义"就是"三层冗余"，需逐个 chain 判明 |
| **P4** | `L6` §2.3.1（`:355-356`）：对第三方私有路径「**不通过**第三方 Mod ID / class name / entity name / **bytecode patch** 建立特殊适配」 | `RonovaTransformer.java:119-126` 对一切「非 `net/minecraft/` 且非 `dev/ronova/coremod/`」的类做 in-memory 改写；选取条件按**字节码内容**（`is_candidate`）而非 Mod ID | **中**——字面上是 bytecode patch；需给出「内容驱动、非 Mod 定向」的对齐说明，或收窄可达集合 |
| **P5** | `D6` / `L5-INV-9`（Judgment 提交次线性）· `D9`（1 batch = 1 事务 = 1 epoch = 1 commit_index = ≤1 发布）· `L6-INV-3`（热路径零同步 durable I/O）· `D7`（commit 后才进不可逆 lifecycle） | **未核** | 待核 |

### 16.3 跨所有权 `SavedData` 写入 —— 结论：不违反 L0

`Ronova` 的恢复链会按名字改写任意 `SavedData` 文件（`SavedDataRecoverySaveMixin` 拦截
`NbtIo.writeCompressed`，`DimensionDataStorageRecoveryMixin:16-22` 只做字符集过滤、
**无 `ronova` 前缀限制**，随后由 `storage_transaction.hpp:139/149/151` 用 NTFS 事务重写）。

**不违反 R1**，三条决定性理由：

1. **枚举范围**：`skills:62` 枚举的是「依赖文件、Mod 文件」——Mod 的**分发产物**；
   世界存档里的 `SavedData` 是**运行期数据**，不是任何人的分发产物；
2. **MS 自己要求写世界持久化**：`skills:137` 要求清理「ID/UUID 索引、tick 列表、ChunkMap 追踪记录和 section 存储」，
   `entitydefence:141` 要求修复「持久实体分区管理器」——**都在世界存档里**。
   若把存档写入算作 R1 违规，MS 自己的清除链与修复链就不成立；
3. **不触 R2**：拦截按调用点触发、按存储名解析路径，**不依赖任何第三方包名 / 类名 / Mod ID**。

---

## 17. 与项目自有规范对齐后的加强块

编排沿用 A–I；每块标注它服务的**规则层**（L0 / P / UX1）与**规范锚点**。

### A 块 —— 前置与入口（服务 `UX1` + `R7` + `PROC-INV-1`）

| ID | 项 | 规范锚点 |
|---|---|---|
| **A1** | 注册 4 个 ModLauncher 服务：`ILaunchHandlerService` / `ITransformationService` / `ILaunchPluginService` / `ITransformerDiscoveryService` | `skills:395`（MS L1，推荐）；`R7` 允许的 in-process 路径 |
| **A2** | ServiceLoader 返回顺序重排 | **限制**：只调整自有条目相对序；不隐藏、不伪造任何第三方条目；每次重排记日志 |
| **A3** | JNI `DefineClass`(loader=null) 替代「解压 jar + `AddToBootstrapClassLoaderSearch`」 | 少一次磁盘写入；引导层可见性相同 |
| **A4** | `ImmediateWindowProvider`（Forge 47.4.11+；Ronova 在 47.4.22） | `skills:394` 称其优先级「极其高」且 Mod File 可正常使用 |
| **A5** | 程序化打开 Java 模块访问边界 | `skills:384` 列为启动顺序第 5 步 |

**A 块的强制职责**：A1 / A4 / 进程内 attach 管道是删除 `AutoAttach` 之后**唯一**的前置入口，
且必须是**进程内**的——`R7` 与 `CF:435` 都禁止把 helper 提升为 Root transport。
**A 块未就绪前不得执行 C15。** 另需满足 `skills:389` 的幂等要求。

### B 块 —— 原生能力位扩展（纯增量）

现状只申请 4 项（`ronova_native.cpp:817-823`）。待加（全部经 JDK 17 `jvmti.h` 核实存在）：

| ID | 能力位 | 行 | 用途 |
|---|---|---|---|
| **B1** | `can_set_native_method_prefix` | `:706`（配套 `SetNativeMethodPrefix` `:1417` / **`SetNativeMethodPrefixes` `:1421`**） | **T0-② 完整形态，覆盖多 classloader**；正是 `skills:59` 点名的「JNCT/JLTN：不用 Native 的 Native」 |
| **B2** | `can_get_bytecodes` | `:673` | 语义不变量级证明（`LoaderProof` / 结构证明的基础） |
| **B3** | `can_get_constant_pool` | `:705` | 校验常量池引用目标 |
| **B4** | `can_generate_compiled_method_load_events` | `:697` | 观察 JIT 编译/回退，关闭 `attack.dispatch` 的 JIT 缺口 |
| **B5** | `can_tag_objects` | `:670` | 跨事件关联同一对象，不依赖弱引用 |
| **B6** | `can_get_source_file_name` / `can_get_line_numbers` | `:681` / `:682` | 诊断归因 |
| **B7** | `can_retransform_any_class`（已申请，启用未用部分） | `:708` | 在外部 transform 之后强制重转换 = T0-③ 等效形态 |

> **不得申请** `can_generate_native_method_bindings`——JDK 17 的 `jvmti.h` 中不存在（JDK 8 有，9 已移除）。

约束：新增事件回调一律走有界队列；**不得**在 `ClassFileLoadHook` 内做重活（`skills:408`）。
与 `P3`（`L5-INV-8` 不得三份实现）冲突的风险须在落地时判明。

### C 块 —— 收口项

> `C15` 为**强制**（`UX1`）；其余为 **L1 自定**或 **P 层对齐**，本方案建议全做。

| ID | 对应 | 动作 | 层 |
|---|---|---|---|
| **C1** | P1 | **判定收回 Java**：`A5`/`L6-INV-1`/`D3` 要求 Native 只提供证据、门控、执行、修复。三选一：<br>(a) `native_root` 收窄为状态保持 + 重放/对账后端，`decide()` 移出；<br>(b) 取消 Java 并行实现，把 native 定位在 `CF:362-370` 的窄命令集内；<br>(c) 删除 native 判定内核（4 个域中 3 个已是死代码）。<br>**三者都必须消除「同一判定两份实现」，并决定 `LocalDominionCore:26-31` 的 Java 二次裁决去留** | **P** |
| **C2** | L1 | 由构建任务从单一来源生成 DLL 名 / 资源路径 / 拷贝路径并校验（`skills:411`） | L1 |
| **C3** | L1 | 补 Java 17 前提检查与独立 `ping` 步（`skills:380-382`） | L1 |
| **C4** | L1 | 为每个注入点声明失败策略；顶层 `catch (Throwable ignored)` 改为记录原因并上报（`msitem:322`） | L1 |
| **C5** | L1 | 31 个 Mixin 补七字段契约，重点补**逻辑侧 / 顺序 / 逐站点失败策略 / 验证**，并把只写方法名的定位补成精确 descriptor（`skills:315`） | L1 |
| **C6** | L1 | 重写 `RonovaAgent:117-133` 的 `reassertTargets()`：单 worker + 阻塞队列、同类去重、每批 ≤32、单批失败记录类名/阶段/异常并降级逐个；**姿态对齐原生侧已有的 lease 记账**（`ronova_native.cpp:686-688`、`:694-706`）；原生侧同类缺失的批量上限（`:622-623`、`:588`、`:667`、`:226`）一并补 | L1 |
| **C7** | L1 | 为每个**关键**能力点名第二通道（非 Mixin），并在状态命令中如实显示生效层——`skills:279` 指出 Mixin 转换服务可被对手直接删除 | L1 |
| **C8** | L1 | 确认列表收窄的现状（已核实：**目前没有**对全局实体列表的独占过滤；客户端外部抑制只挂在 `RECOVERY::isAuthorityRecoveryActive`），记录 `entitydefence:228-234` 的 5 条后果 | L1 |
| **C9** | L1 | 对齐 `entitydefence:62-128` 的规范实例 5 规则、7 项修复前不变量、六类修复点（含注册表读写 hook）与读侧兜底 | L1 |
| **C10** | L1 | 原生构建接入 Gradle 包装任务 + 提供等价可执行探针（`skills:452` 覆盖桥接、类恢复策略、**自映像保护**） | L1 |
| **C11** | P2 | **14 个动作的执行器接线**与调度收敛（`msitem:411`：所有击杀入口收敛到一个可去重、可审计、服务端权威的调度器） | **P** |
| **C12** | P2 | **逐键矩阵 revision 2**：`ProfilePresetMatrix` 从三种统一填充改为按 `(profile, StrengthOptionKey)` 逐键解析，并保证**同一 key 的三档命中不同生产策略**（`D91`、`V2.6:877`） | **P** |
| **C13** | P2 | **天花板管道**：`supportedCeilings` 默认 `OFF`（`StrengthPolicySnapshot.java:99`），`withCapability` 只有 2 个调用点且都只传 `PROTECT_RONOVA`/`OFF` ⇒ 54 键中只有 6 键能到 `ACTIVE`，其余 48 键永久 `UNAVAILABLE`。按 `V2.6:664-673` 如实呈现，并补 `RuntimeCapabilityProjection` | **P** |
| **C14** | P3 | 判明 NBT 编码链的三层关系，按 `L5-INV-8` 收敛到**一份实现 + 两个门** | **P** |
| **C15** | **UX1** | **删除 `agent/AutoAttach.java` + `AttachHelper.java`**（唯一子进程派生点）。前置能力由 A1/A4/进程内 attach 管道承接；`StartupPolicy.ProtectionMode.DUAL_AGENT` 的默认值与语义按新入口重写，状态如实显示当前生效层。**保留 `NativeBridge` 进程内加载两个 DLL——`UX1` 只约束进程模型** | **UX（强制）** |
| **C16** | P4 | 改写可达集合**显式收窄**：排除 `net/minecraftforge/`、`cpw/mods/`、`org/lwjgl/`、`com/mojang/`、`io/netty/`（现仅排除 `java/` `jdk/` `sun/`）；或给出命中后的处置契约与「内容驱动、非 Mod 定向」的对齐说明 | **P** |
| **C17** | L1 | 落实 `skills:322` 的回滚：补丁前保存原始字节码，或明文声明以重启/重定义作为恢复方式 | L1 |
| **C18** | L1 | 把 `ronova_native.cpp:388-402` 的通用英文子串选取改为**显式清单** | L1 |
| **C19** | P5 | 核对 `D6`/`D9`/`L6-INV-3`/`D7`：扩展性、batch 原子性、热路径零同步 durable I/O、commit-then-publish | **P（待核）** |

### D 块 —— 执行器与结算（能力本体）

`DominionCommands.java:280-281` 对 14 个动作中的 13 个硬编码「未接线」（= C11）；
`CompletionTracker` 9 项条件中 6 项零引用；`TerminalLifecycle.java:49-62` 服务器分支整体不可达。
**注**：`D94` 要求能力按实际状态报告——**当前的"未接线"显示是合规的**，缺口是未接线本身。

### E 块 —— 图与证书

| ID | 项 | 规范锚点（`V2.6` §3.7 / §14） |
|---|---|---|
| **E1** | 候选一等公民 + 置信度模型 | `CANDIDATE → PROVEN_RELATED / PROVEN_UNRELATED / UNRESOLVED`（`:279-285`）；准入需一条强证据或两条独立弱证据（`D66`） |
| **E2** | `MutationTrace` | **不是 Authority**；不直接创造敌对关系；**不得凭字符串 / 包名 / 同父类判归属**（`:1168-1171`） |
| **E3** | `ResultGraph` | **不能创造 Judgment、不能单独撤销 Tombstone、不能自己推进 TargetEpoch**（`:347-349`）；重启后按当前 Root Truth + 可信证据重建 |
| **E4** | `ExistenceTerminationCertificate` | **不能直接成为 `denySpawn()` 的 Authority**（`:355`）；失效不自动回滚已提交 Judgment（`:357`） |
| **E5** | `ExecutionLease` | 高强操作必须绑定短期 lease（`:2246-2264`）；三处重验（签发前 / 提交前 / 完成后）；**签发必须在 durable-committed 资格事务回执之后**（`:2288-2304`）；`Root transaction 内禁止调用目标实体、敌方 manager、第三方 callback`（`:2300`） |
| **E6** | **九项存在性终结条件** | `V2.6:1510-1518` 九条；仅降低事件数量、清空样本、完成压缩、取消 Future、停止 Ronova 授权或短暂无漂移窗口，**都不能独立满足完成条件**（`:1540`） |

### F 块 —— 证明与策略收口

| ID | 项 |
|---|---|
| **F1** | 结构/语义证明：不变量在**实际生效字节码**上校验（`BL:1790`：「允许不同 bytecode variant，只要能够证明**相同关键语义**」；真正的检验是「DENY decision 是否仍支配 forbidden mutation edge」） |
| **F2** | 多 ClassLoader 副本证明（`CF:637` 的 edge `providerRevision` / `bindingRevision`；`CF` §8.1） |
| **F3** | 如实呈现：`PENDING_RESTART / REJECTED / FENCED / UNAVAILABLE`（`V2.6:664-673`、`D94`） |
| **F4** | 逐键矩阵 revision 2（= C12） |
| **F5** | 天花板管道（= C13） |
| **F6** | 两套权限路径合一：`registered*` 用预设 `requestedLevel`，`resolve()` 用 `effectiveLevel` |
| **F7** | **自映像保护可验证化**：CodeSeal 是其基础；`skills:482` 的完成标准即「字节码不会被别人修改到」 |

> **撤回**：旧方案 F 块引用的 `AttestationManifest` / `LoaderProofRegistry` / `EnforcementLease`
> 在五份规范中**均不存在**（§14 #5）。对应概念应改用规范自己的名字：
> **结构/语义证明**（`BL` §14.1、§15）、**`LoaderProof`**（`V2.6:2656`，7.17 交付物）、
> **`ExecutionLease`**（`V2.6` §13.2）。

### G 块 —— 原生强制下沉

`NativeBridge` 侧：`WriteSink` / lease 下沉（现状每次强制都回调 Java：
`ronova_native.cpp:135 CallBooleanMethod(lease, valid, plan)`）。
**但必须与 C1 一起裁决**：`A5` 要求 Native 不做 authoritative durable write，
所以下沉的只能是**执行与校验**，不是判定。二者合起来才让原生层回到 `CF` §9.1 的定位。

### H 块 —— 新维度

| ID | 项 | 锚点 |
|---|---|---|
| **H1** | COLD 休眠窗口的事件驱动唤醒（退避即攻击窗口） | 原生已有 `ClassPrepare` 触发刷新的先例；`L5-INV-5` 要求全部持续工作有预算 + 退避 |
| **H2** | 候选 flood 的配额分配：按证据强度分配，保证高置信候选的最低配额 | `D76`（重复次数贡献封顶）· `D77`（保留非零有界探索预算）· `V2.6:2126` |
| **H3** | 按需重建 incarnation | `A2`（不继承旧 authority）· `A4`（TERMINAL 不可自动复活）· `CF` §3.3 |

### I 块 —— MS 未禁止、但本方案**不建议**优先做的

| 项 | MS 口径 | 项目口径 |
|---|---|---|
| 精确 JDK 类改写 · `Unsafe` / `ReflectionFactory` 包访问 · 对象头 / klass 改写 · 图形栈 hook · 隐藏类 / 内存编译 · engine inline hook · attach 管道 · 解密载荷 `DefineClass` · 载荷伪装 | **MS 未禁止**（`skills:324` 甚至明许 `Unsafe` 路线并给出三条准入条件；`skills:355` 明许隐藏类 / 类恢复） | **`BL` §24「明确淘汰 / 不进入主线的旧加强」14 行表**明确淘汰其中多项，包括 **dynamic keys / pre-exec decryption**；`BL:1811` 代码多态「不进入 P0 主线」 |

⇒ **这一类不是合规问题，是项目自己的路线选择**。按 §15 的分层，Ronova 有权改，
但要改就得**显式改 `BL` §24**，不能绕过。

---

## 18. 实施顺序

```
强制（UX1）
  └── C15 删除 AutoAttach / AttachHelper —— **A 块未就绪前不得执行**

P 层对齐（自己冻结的规范，建议先做）
  ├── C1  判定收回 Java（A5 / L6-INV-1 / D3）—— 改动面最大
  ├── C12 逐键矩阵 revision 2（D91 / V2.6:877）
  ├── C13 天花板管道（V2.6:664-673）
  ├── C11 执行器接线（msitem:411）
  ├── C14 NBT 链收敛（L5-INV-8）
  └── C16 可达集合收窄 + C19 四项核对

L1 自定（低风险、无行为变化，可并行）
  ├── C2 / C3 / C4 / C17
  ├── C5 / C6 / C7 / C8 / C9 / C10 / C18
  └── A 块 → B 块 → D → E → F → G → H
```

**顺序理由**：C15 是唯一强制项且依赖 A 块；P 层对齐直接决定 FULL L5 / V2.5 的完成判定，
优先级高于 L1 自定项；C1 改动面最大（动 C++ 导出 + Java 装配），放在 P 层最后以免与其他项调试互相干扰。

---

## 19. 验证：全部后置

本方案**不含验证项**，也不建议在任何实现块完成前启动验证（用户明确指令）。
启动门槛：D 块（执行器）与 E 块（图与证书）接线完成。
届时可一次性对照 `V2.6` §19.2（11 条）· §24（58 条）· `L6` §12.3 · `P0A` §60，
不需要分批制造游戏 PASS。

---

## 20. 完成判定

| 层 | 判据 |
|---|---|
| **L0** | R1/R2/R3/R4 保持 §16.1 的干净状态 |
| **UX1** | 主源码中 `ProcessBuilder` / `Runtime.exec` / `ProcessHandle` 派生调用为 **0**；`AutoAttach` / `AttachHelper` 已删除且入口真空已由 A 块补齐 |
| **P** | `A5`/`L6-INV-1`/`D3` 成立（C1）· `D91`/`V2.6:877` 成立（C12）· `V2.6:664-673` 如实呈现（C13）· `msitem:411` 调度收敛（C11）· `L5-INV-8` 收敛（C14）· `C19` 四项核对完成 |
| **L1** | 已采纳的项各自达成；未采纳不算失败，但**不得声称拥有未实现的能力** |

**任何已声明采纳的项未达成，不得据此宣称加强已完成。**
