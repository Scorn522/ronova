# Ronova 功能全清单（只读梳理报告）

> 本文件由一次**只读调研**产出：未修改任何既有源码、配置、文档或验证产物。
> 唯一新增文件就是本文件。未运行任何构建、编译或验证命令。
>
> **方式声明**：项目规范（`docs/CURRENT-PROGRESS.md:59`、`docs/PHASE1-IMPLEMENTATION-STATE.md:111`）
> 明文要求"不使用子代理"。本次梳理为**只读盘点**，使用了并行子代理加速，不涉及任何实施改动，
> 亦未据此改动任何 PASS 状态或验收编号。若该约定同样涵盖只读调研，请告知，后续改为单代理。

## 0. 可信度分级

全文结论分三级，阅读时请按级别取舍：

| 级别 | 含义 | 标记方式 |
|---|---|---|
| **实测** | 本次由完整读取源码/文件/命令输出直接确认 | 给出 `文件:行号` |
| **脚本提取** | 由批量提取类签名与类头 javadoc 得到，未逐行通读方法体 | 标注"（签名级）" |
| **转述** | 来自项目自有文档或并行调研报告，本次未独立复核 | 标注来源路径 |

项目自身文档的可信度很高：它**主动记录失败、拒绝虚报完成**（详见 §15），
因此引用其"未完成"结论通常是可靠的；引用其"已完成"结论时本文一律回到源码复核。

---

## 1. 一句话定位

Ronova（若娜瓦）是 **Minecraft Forge 1.20.1 / Java 17 的"持久死亡裁决权威"模组**。
玩家用一把同名剑对实体下达"裁决"，被裁决的**身份/血缘**被持久判定为终结（Tombstone）；
即使物理实体对象消失、区块重载、存档重启或被第三方 Mod 复活重建，该裁决依然存在并**持续幂等重断言**。

架构上的核心主张是三个"分离"：

1. **裁决真值与物理对象分离** —— 对象消失 ≠ 裁决消失（`CONTEXT.md`）。
2. **Raw Observation 与 Public Presence 分离** —— 内部永远看得见，对外可以不可见；单向，presence 不得反向影响 raw（D1）。
3. **一次性生命周期集成与可重复状态重断言分离** —— `Lifecycle Integration` 只做一次（死亡/掉落等外部回调），
   `State Reassertion` 可无限幂等重放（移除状态、索引摘除、字段断言）。

规模：**378 个主源码 Java 文件 / 37,381 行**，41 个包；另加 C++ 原生层、31 个 Mixin、
95 个 Java 自测类；文档体系 60 份 Markdown（约 1.2 MB）。

---

## 2. 玩家可见功能

### 2.1 操作

| 操作 | 行为 | 关键实现 |
|---|---|---|
| **左键** | 排队 `PRIMARY` 裁决；按住时对 64 格射线命中方块持续发破坏请求（等同瞬时挖掘） | `client/ClientInputController.java:50-64` |
| **右键按住** | 进入格挡姿态（`UseAnim.BLOCK`，无盾牌视觉） | `content/RonovaBladeItem.java:38-46` |
| **右键松开** | 提交 64 格**范围**裁决 `AREA` | `ClientInputController.java:124-147` |
| **Shift + 右键** | 按下瞬间锁定模式，松开提交**当前维度全部已加载目标**裁决 `LOADED` | `client/BladeBlockGesture.java:8-11` |
| **`O` 键** | 打开强度设置界面 | `client/RonovaClient.java:102-104` |
| **`Q` 丢弃** | 对剑无效（`onDroppedByPlayer` 返回 false） | `RonovaBladeItem.java:59-62` |
| **悬停剑** | 紧凑 Tooltip；按住 Shift 展开长文案并触发边框"开眼"动画 | `client/tooltip/RonovaTooltipEvents.java:28-70` |

左键与右键各有**两条输入通道**：Mixin 捕获的真实鼠标边沿 + 每 tick 的 GLFW 状态轮询兜底，
用闩锁去重（`MouseHandlerInputMixin.java:12-21` + `MinecraftClientRuntimeMixin.java:12-15`）。

### 2.2 物品

`ronova_blade`：单堆叠、防火、`Rarity.EPIC`、附魔光效常亮、**无耐久**、
`hurtEnemy` 触发服务端 `executeTargeted(..., "item-hit")`、
`onEntitySwing` **故意空实现**（避免跨 tick 重复裁决，见 `RonovaBladeItem.java:30-35`）、
`inventoryTick` 每 tick 在**任意槽位**恢复 Owner 绑定（`:52-57`）。

### 2.3 GUI

**强度设置界面**（`O` 键，`client/RonovaStrengthScreen.java`）：
5 个页签（攻击 / 拦截 / 防护 / 运行时 / 功能）；
4 个预设按钮（兼容 / 强化 / 极限 / 自定义）；
攻击页 14 个普通动作行按钮 + 1 个来源追踪选项；
机制页各 54 个高级键行按钮；
普通攻击 / 高级攻击切换（仅本地状态，不持久化）；
翻页 / 刷新 / 完成。所有编辑走服务端版本化回执，客户端只持快照（5 秒新鲜度）。

**运行时主权控制台**（`client/RonovaControlScreen.java`，**只能从强度界面第 5 页进入**）：
核心防护范围（MIXIN ⇄ DUAL_AGENT）、遏制外部重转换开关、完整性重断言间隔（15/30/60 秒循环）、
视觉反馈（全部 / 仅 HUD / 关闭）、保存启动策略（写 `config/ronova-startup.properties`，**需重启生效**）。

### 2.4 HUD 与视觉

- 右上角"权威在线"卡片：显示最近裁决回执 `ACTION / count`，无回执时显示硬编码 `64 · 64 · LOADED`
  （`client/RonovaVisuals.java:21-28`）。
- 物品栏图标四角呼吸红光 + 白点闪烁（`client/render/RonovaItemDecorator.java`）。
- 剑体两遍渲染：原版底图（全亮度）+ 用 mask 裁剪的立绘层，带视角视差；
  手持/第一/第三人称各自变换表（`client/model/RonovaMaskedLayerBakedModel.java:51-116`）。
- Tooltip 完全替换：清空原版行 → 自定义组件 → GLSL 一次 pass 自绘整框
  （眼图集三只眼、雾、红粒子、四角辉光、呼吸描边，未按 Shift 时每 5.4 秒眨眼）。
- 被隔离实体在世界中**不渲染**（`RonovaClient.java:118-123`）。
- 服务端裁决粒子：`REVERSE_PORTAL`、`END_ROD`（`network/ServerActionService.java:222,328`）。
- **无任何音效**：全 `main` 源码无 `SoundEvent`/`playSound` 引用。

### 2.5 指令（`/ronova`，完整）

| 指令 | 权限 | 作用 |
|---|---|---|
| `give` | OP(2) | 发剑 + 绑定 Owner |
| `bind` / `unbind` | OP(2) | 绑定 / 解绑 Owner 连续性 |
| `status` | 所有人 | 打印约 60 个运行时诊断字段（Root/原生/投影/存储/完整性/强度/计数） |
| `diagnostics` | OP(2) | 义务台账 + 8 类运行表面 + 15 类宿主资源的逐行诊断 |
| `strength status` | 所有人 | 打印每个普通动作的**保存值 / 当前值 / 接线状态 / 高级解析原因** |
| `strength compatible|enhanced|extreme|custom` | OP(2) | 切换强度预设 |
| `strength attack <动作> <等级>` | OP(2) | 单动作等级（14 × 4 档，自动进入 CUSTOM） |
| `strength option <键> <等级>` | OP(2) | 单高级键等级（54 × 5 档，自动进入 CUSTOM） |
| `judge_loaded` | OP(2) | 对当前维度全部已加载实体执行裁决 |
| `diagnose_uuid <uuid>` | OP(2) | 打印某 UUID 的原始观察数、监视模式、调度/尝试/完成计数 |
| `clear_uuid <uuid>` | OP(2) | 经 Root 命令解除单个裁决（未提交则报"尚未确认解除"） |
| `clear_tombstones` | OP(2) | 解除全部裁决，并提示"独立恢复来源仍可能重建目标" |

---

## 3. 核心机制：裁决 → 终结 → 监视 → 重断言

### 3.1 语义模型（源码确认 + 规范文档）

```
Committed Tombstone（持久、UUID+lineage、无限逻辑寿命、语义不压缩）
│
├── Lifecycle Integration ── ONCE（仅一次）
│     ├── 死亡回调衔接、loot / effect 集成
│     └── 记 LifecycleIntegrationReceipt{judgment_id, first_physical_identity, integrated_epoch}
│
└── State Reassertion ── REPEATABLE / IDEMPOTENT（无限次幂等）
      ├── removal 状态（discard / remove）
      ├── entity-manager 摘除、tracking 摘除
      ├── raw terminal 字段断言（health / removalReason / callback）
      └── quarantine / presence 投影更新
```

来源：`docs/plans/Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md:637-661`（实测）。

**关键约束**：terminal 层**不得**调用任何让 raw 观测遗忘对象的操作。
规范明确点名旧实现的两处违规并已在迁移中移除：`TopologyIndex.forget(entity)` 与
`ExecutionTargetPolicy.shouldTrack` 的 `!isJudged` 条件（同文档 `:657`）。

### 3.2 终结执行族（engine 包，签名级 + 自述 javadoc）

| 类 | 自述职责 |
|---|---|
| `TerminalPlanExecutor` | Root 批准的终结计划的**生产执行端口**；"任何本地步骤都不写 Judgment" |
| `TerminalLifecycle` | 只断言**已登记的 raw 字段**；不断言索引缺失或整操作完成 |
| `TerminalStateSink` | 幂等的最终字段写入，与一次性的 death/removal 回调及其未知效果**分开** |
| `TerminalIndexRepair` | 可续接的对象身份摘除，"**绝不调用实体生命周期方法**" |
| `TerminalStateFeedback` | 跟踪已成功的状态写入；"**绝不重放** death / loot / removal 回调或指令" |
| `TerminalCompletionMonitor` | 有界、会过期的完成观察；"驱逐只移除证据，绝不移除义务或 Root 事实" |
| `JudgmentEngine` | 裁决入口（射线 / 范围 / 已加载 / 单目标） |
| `JudgmentExecutor` | 执行不可变裁决；"**从不**发现或选择目标" |
| `TargetDiscovery` | 只发现合格根对象；"策略承诺与执行在别处"；`shouldTrack` 不再受 `!isJudged` 影响 |
| `TopologyIndex` / `LineageIndex` | 单 UUID 下所有存活对象 / 原子替换投影 |
| `SpawnFirewall` | 组合根装配的生成准入运行时**兼容门面** |
| `AuthorityEnforcement` | Owner 物理防护（`isProtected` / `repair` / `maintain`） |

### 3.3 生成准入（spawn 子包）

多道边界拦截被裁决身份的重新出现：
`JOIN`（`EntityJoinLevelEvent`）、`LEVEL`（`ServerLevel.addEntity`）、
`FIRST_TICK`（`tickNonPassenger`/`tickPassenger`）、`ENTITY_MANAGER`（`PersistentEntitySectionManager`）
以及 NBT 反序列化路径（`EntityType.create(CompoundTag,Level)`）。
另有 `BoundedRetryQueue`（有限重试 + 有界死信）与跨服务器保留的 `SpawnAdmissionMetrics`。

### 3.4 终结监视状态机（terminal 包）

`HOT`（复活/重插刚检测到，1 tick 或极短退避）→ `WARM`（指数退避 2/5/10/20 tick）→
`COLD`（低频确认，无强引用，可被 GC）。
**成员不以重试次数结束，而以 authoritative tombstone 是否仍存在决定**；
COLD 对象在 raw 观测再次发现时立即升回 HOT。
预算约束：建议 ≤0.5 ms/tick，超预算按 HOT > WARM > COLD 优先级分帧，绝不在单 tick 无界循环。
（`L6 Master §6.2-6.3:661-683`，实测）

实现类：`TerminalWatchSet`（不可变 UUID 诊断视图）、`DurableTerminalWatchSet`、
`PassiveTerminalWatchSet`（**纯诊断镜像，从不调度/解析/重断言**）、`NoopTerminalWatchSet`（7.13 回滚路径）、
`TerminalWatchMetrics`（常数时间被动遥测）、`LifecycleIntegrationReceipt`、`LifecycleClaimResult`。

### 3.5 存在性与隔离（presence 包，3 个类）

`PresencePolicy`（quarantine 决策内化 + 自身免疫不变量）、`OwnerImmunity`
（**免疫判定单独显式化，以便在决策中给出理由**）、`PresenceSplitFlags`。
三套隔离视图：`ServerQuarantineView` / `ClientQuarantineView`（"应用完整服务端快照，不暴露半重建索引"）/ `LegacyQuarantineView`。

---

## 4. 权威层

### 4.1 数据流（规范原文）

```
Minecraft / Forge runtime
   → Execution / Judgment services
   → RootCommandEnvelope
   → InternalDominionRoot { 校验 epoch → 校验 writer fence / root instance
                            → dedup(command_id + payload_hash) → 策略/不变量资格
                            → 提交定序 → COMMIT_UNKNOWN 恢复 }
   → DurableRootStore → CommitReceipt
   → ProjectionCoordinator → 不可变投影
        ├─ AuthorityView  ├─ PresencePolicy  ├─ TerminalWatch  └─ Enforcement Domains
```

### 4.2 概念分工（不得互相兼职）

| 概念 | 语义 | 载体 |
|---|---|---|
| `DominionEpoch` | 权威真值顺序 `(generation, sequence)` | durable truth |
| `RootInstanceId` | 当前 JVM 内 Root 实例身份，重启后变化 | runtime + durable head |
| `FenceToken` | writer generation；拒绝陈旧 Root 实例/变更 | Root state + envelope |
| `command_id` | durable 变更的幂等键 | envelope + dedup store |
| `commit_index` | durable 提交的全局单调索引 | DurableRootStore |
| `realm_id` | 存储/世界血缘身份 | durable store |

### 4.3 十项 L6 不变量（规范原文摘要）

`L6-INV-1` 唯一逻辑 Authority writer / `-2` Native·Domain·Agent·Projection 都不能直接推进 epoch /
`-3` 热路径 0 同步 durable I/O / `-4` 陈旧实例·栅栏·epoch 变更 0 接受 /
`-5` `command_id + canonical hash` 幂等且冲突显式 / `-6` `COMMIT_UNKNOWN` 只能以原 id 恢复，
不得猜成功或拒绝 / `-7` durable commit 成功后才发布投影 / `-8` Raw 与 Presence 单向分离 /
`-9` Tombstone durable、无 FIFO 语义逐出、Terminal dominance / `-10` JVM 完全失陷不在隔离保证内。

### 4.4 身份与契约

- `SubjectKey = (RealmId, subjectId)` 是**唯一稳定逻辑键**；运行时类、维度、位置、来源、schema 都不参与相等。
- `SubjectIdentity` 只是以 `SubjectKey` 为锚点的**描述**，不是第二逻辑键。
- `BindingRevision` / `OwnershipRevision` / `ProjectionRevision` 三者互不替代，且各自只在自身范围内可比。
- `LifecycleTruth` 只有 `ACTIVE | TERMINAL` 两态；解析不确定性与准入隔离是**另外的轴**，
  绝不新增生命周期状态。
- `ResolutionState`：resolved / partial / ambiguous / opaque / stale / invalid / unknown ——
  **unknown 永远不等于 false，也不等于安全**。

### 4.5 Root 命令族与收据

`DominionCommand`（255 行）密封了全部 Root 可变操作；`CommitReceipt`（241 行）承载
committed / rejected / conflict / unknown 等状态与 `changed()`；
`JudgmentBatch`（229 行）是原子批；`RootCommandBus` 提供 `initialize` / `commit` / `recover(commandId)`，
并在 `receipt.unknown()` 时走恢复而非重放。

一次性许可：`EffectKey` / `EffectRecord` / `EffectClaim`（含 `LEGACY_UNQUALIFIED` 状态）；
`TerminalActionPolicy` 明确只对**已登记实体及其精确索引引用**发放执行能力（见 §11.3）。

---

## 5. 持久化层（persistence 包，26 个类，约 5,900 行，签名级）

| 类 | 职责 |
|---|---|
| `ShadowDurableRootStore`（2,484 行） | 权威 durable 存储主体：`SavedData` + `DurableRootStore` 双身份；realm、epoch、快照、TargetEpoch、SubjectQualification、readHead |
| `DominionLedger`（691 行） | 旧账本 `SavedData`：Owner 绑定/解绑、`record`/`recordBatch` 登记 tombstone、RealmId 迁移、`mirrorOnly` 只读镜像模式 |
| `RootCommandJournal`（642 行） | durable 命令日志：`commitIndex`、`find(commandId, payloadHash)`（哈希不符抛 `CommandIdConflictException`）、`append`/`restore`/`save`/`copy`/`replaceFrom`、`latestQualification(epoch)`、`ownsEffectAuthority`、`policyAt(revision)` |
| `LocalDominionStore`（521 行） | 权威适配器：`open(server)`/`openWorld(path)`，启动时校验 Root 与镜像标记；`approveTerminalPlan` / `acquireOwnerWrite` / `qualifyRecoverySource` / `approveStaticSnapshotPlan` / `approveStaticRecordPlan` / `approveLoadedRecordPlan` / `prepareRecoveryMutation` / `recoveryMutationLocation` / `approveRecoveryTaskPlan` |
| `EffectJournal`（321 行） | 效果记录 + 恢复工作描述符 + 源位置；`reservedGrowthBytes`、分页（单页 ≤64） |
| `DurableRootFileBackend`（312 行） | 文件后端：magic 校验、SHA-256 摘要、`prepare/write`（带预留增长字节与"是否接受新写入"）、Windows 平台句柄预算核算 |
| `TombstoneStore`（154 行） | tombstone 记录集：`matches` / `matchesDetachedLineage` / `toDecision` / NBT 往返 |
| `StrengthPolicyNbtCodec`（149 行） | 强度策略持久编解码 |
| 其余 18 个 | RealmId/EpochClock/OwnerRegistry/SnapshotAssembler/ShadowVerification/LegacyMirrorFacts/SavedDataMirrorFiles + 4 个 NBT codec + 7 个异常类 + 5 个故障注入点（`DurableWriteFailpoint`/`CrashFailpoint`/`RootCommitFailpoint`/`ShadowCrashException`…） |

**持久兼容**（转述 `docs/PHASE1-IMPLEMENTATION-STATE.md:51-57`）：Root 最大版本 14、
EffectJournal 最大版本 5；描述 v4 含 `RecoveryStorageGeneration`（后端 `windows-ntfs-txf/v1`、
保存登记 UUID 与代次）；同代原 key/attempt 保持，新代次使用独立 semantic slot；
旧 v1–v3 描述**不重写**；缺新执行依据的旧工作可查询但**不能取得新 Native 写许可**；
未来版本与伪降级拒绝写入。

**文件后端的一条诚实注释**（`DurableRootFileBackend`）：引导阶段早于运行时组合，
因此不声明历史资源核算（"Bootstrap precedes runtime composition; do not claim historical accounting"）。

---

## 6. 拦截与防护三层

### 6.1 三层分工（**已逐条实测修正**）

> **更正**：本节初版有三处错误、两处误导，现按实测重写。三层的真实关系**不是"分工"，而是"对同一批目标的三套冗余实现"**（见 §6.1.2）。

| 层 | 时机 | 覆盖（实测） | 能力 |
|---|---|---|---|
| **Mixin** | ModLauncher 加载期 | **31 个 mixin 类**（`ronova.mixins.json`：`mixins` 26 条 + `client` 5 条），**去重后指向 24 个原版类** | 唯一使用 `@Redirect`/`CallbackInfo` **注解语义**；SavedData 保存链、ItemRenderer 渲染改写只有它做 |
| **Java Agent**（early + late） | premain（`-javaagent`）或 agentmain（自挂） | 5 个原版类 + **11 个** accessor 契约类（`RawAccessorContract`） | 生命周期五门、查询结果过滤、外部类字段写拦截、JDK 任务边界注入、裸 ASM 调用改写 |
| **Native DLL**（2 个，但**只有 1 个被封印**） | 类初始化即加载 | **16 个**语义目标类（`SEMANTIC_TARGETS`）+ **7 个** JDK 任务类（`JdkTaskBoundaryContract.TARGETS`） | JVMTI `ClassFileLoadHook`（含 retransform 与新 loader 副本）、语义租约时间戳、CodeSeal、TxF 事务、句柄身份、任务 enter/exit |

**三处更正**：
1. "26 个原版目标"是把 **json 里 26 条通用条目**误当目标类数。去重后是 **24 个**原版类
   （`EntityType` / `Minecraft` / `ServerLevel` 各有 2–3 个 mixin 文件指向同一类）。
2. "10 个 accessor 类"实测是 **11 个**：`SynchedEntityData`、`SynchedEntityData$DataItem`、`ServerLevel`、
   `ClassInstanceMultiMap`、`Entity`、`LivingEntity`、`EntityLookup`、`EntitySection`、
   `EntitySectionStorage`、`EntityTickList`、`PersistentEntitySectionManager`。
3. "NBT 编码链**只有** Mixin 做"**是错的**——见下。

**两处误导**：
1. "唯一能用 `@Redirect`/`CallbackInfo`"——作为**注解用法**成立；作为**能力**不成立。
   Agent 直接用裸 ASM 做等价改写（`NbtCopyContract` 注入 `INVOKESTATIC`、
   外部类 `PUTFIELD` 替换、把 `FutureTask.cancel` 的 `VarHandle.compareAndSet` 换成许可检查）。
   准确表述是"唯一使用 Mixin 注解语义"。
2. "Native DLL（2 个）"未说明**只有 `ronova.dll` 在封印信任链内**：
   `build_code_seals.py:142-154` 只编译 `ronova_native.cpp`、只替换 `native/windows-x86_64/ronova.dll`；
   `ronova_root.dll`（415,744 B，SHA-256 `74AE9E90…`）**原样出厂**，构建期时间戳 2026/8/27（r66 JAR 为 2026/9/9）。
   → **做 Root 决策内核的那个 DLL 本身不受封印保护**，而封印保护的却是被它决策的 Java 真值链。

### 6.1.1 三层真实覆盖（实测交叠）

| 原版类 | Mixin | Java Agent | Native |
|---|:--:|:--:|:--:|
| `Entity` / `LivingEntity` / `ServerLevel` | ✅ | ✅ | ✅ |
| `ClientLevel` | ✅ | ✅ | ✅ |
| `CompoundTag` / `EntityType` | ✅ | ✅ | ✅ |
| `Level` | — | ✅ | ✅ |
| `SynchedEntityData`、`$DataItem`、`EntityLookup`、`EntityTickList`、`EntitySection(Storage)`、`ClassInstanceMultiMap`、`PersistentEntitySectionManager`、`EntityDataAccessor` | ✅（`@Accessor`） | ✅（`RawAccessorContract`） | ✅ |
| `SavedData` / `DimensionDataStorage` / `NbtIo` / `ItemRenderer` | ✅ | — | — |

**所以 §14.3 第 15 条"双份实现无断言固定"描述的正是这张表**——
本表初版却写成三层各管一块，与风险清单自相矛盾，现以交叠表为准。

### 6.1.2 这是有意设计，不是失误

交叠本身符合"拦截层冗余 = 抗绕过"的设计意图：任一层被取消、提前返回或替换，其余层仍有独立路径
（这正是 `intercept.mixin` / `intercept.asm` / `intercept.jvmti` 三项能力各自存在的理由）。
代价是**同一语义有三份实现、三份正确性负担**，而目前只有 Mixin 侧有结构性自测
（`MixinMetadataSelfTest`）、Agent 侧有 ASM 门禁自测，Native 侧的 `CompoundTag` 注入**没有对应行为测试**。

三层共用同一份 ASM 逻辑：Native 的 class-file-load hook 不自己实现注入，
而是回调 Java 的 `NativeTransformBridge.transform`（`ronova_native.cpp:806`），
Native 额外负责"是否值得回调"的字节预筛与"证明"（`has_semantic_proof`）。

### 6.1.3 三层协作机制（实测）

#### 6.1.3.1 启动时序：三次握手

| 阶段 | 触发 | 发生的事 |
|---|---|---|
| **① 类初始化**（`RonovaDominion` 静态字段，`:24-25`） | 加载 `RonovaDominion` 类 | `System.load(ronova.dll)` → `JNI_OnLoad` 抓 JVMTI（`ronova_native.cpp:780-786`）→ `bootstrap()`：<br>· `GetClassLoader(owner)` 存为 `g_root_loader`（`:793-796`）<br>· `FindClass(NativeTransformBridge)` 缓存 `transform`/`verify` 两个 methodID（`:798-815`）<br>· `AddCapabilities`（retransform 等 4 项，`:817-826`）<br>· 注册回调 `ClassFileLoadHook` + `ThreadEnd`（+ 封印时 `ClassPrepare`，`:828-833`）<br>· **启用 Hook 并立即做一次 eager retransform**（`:834-846`）<br>· `prepareTaskBridge0` → `AddToBootstrapClassLoaderSearch(task-hook-support.jar)` → `JdkTaskBoundaryContract.enable()`（`NativeBridge.java:45-46`）<br>· 读 `META-INF/ronova/code-seal-id.txt` → `enableCodeSeals0`（`:48-51`）<br>· `System.load(ronova_root.dll)`（**失败被吞**，`:52-60`） |
| **② 构造期**（`:27-41`） | Forge 调构造器 | 若 Native 不可用才走 `AutoAttach` 自挂（`:28-30`）→ **安装 4 个谓词**（`:34-37`），其中 `installPredicate` 额外反射调 `armLateGuard` 装上 **late transformer**（`AgentHooks.java:15,66-74`） |
| **③ 加载完成**（`:43-51`） | `FMLLoadCompleteEvent` | Native 可用 → 启动**原生看门狗线程**；否则 → `AgentHooks.markRuntimeReady()` 启动 **Java 看门狗线程** |

**这张时序表里有一个关键事实**：第 ① 步的 eager retransform **发生在任何谓词安装之前**。
当时 `BootstrapHooks` 的 8 个谓词槽还是默认空实现（`BootstrapHooks.java:21-29`）。
这不是 bug——**注入进去的门是永久字节码，谓词是运行期查表**：
门在 ① 就写进方法体了，但"门返回什么"要等 ② 才确定。所以不需要二次 retransform，
`volatile` 谓词槽正是为此存在。
**代价**：①→② 之间所有门都放行（见 §14.1 第 7 条 fail-open）。

#### 6.1.3.2 协作的真正含义：三层互不通信，汇聚到一个决策点

三层**之间没有任何直接调用**。它们的协作方式是：

```
Mixin 注入的门  ─┐
Agent 注入的门  ─┼─→ BootstrapHooks.shouldBlockRemoval/Damage/Death/Health/…
Native 注入的门 ─┘        （8 个 volatile 谓词槽）
                                ↓
                        AuthorityGuard / QuarantineView
                                ↓
                        AuthorityView（纯查询）
                                ↓
                        Root 发布的不可变投影
```

**冗余在覆盖，统一在决策。** 任一层被取消、提前返回或替换，其余层的门仍然调用同一个 `BootstrapHooks`，
拿到同一个答案。这才是"抗绕过"的实现方式——不是三层互相兜底，而是三层各自把门钉进不同位置，
门后接同一个判据。

#### 6.1.3.3 一次类加载的完整路径（Native `class_file_load_hook`）

`ronova_native.cpp:490-564`，按顺序：

1. **任务位**：是 7 个 JDK 类之一？→ 记录 ownership（`:495-496`）
2. **封印路径**（`:497-521`，仅 `RONOVA_CODE_SEALS` 且已启用）：
   命中 283 个封印镜像之一 → 失效语义状态 → 与构建期镜像逐字节比对 →
   **不一致则 `Allocate` 一块内存、把构建期镜像拷进去、把 `*new_class_data` 指向它、`g_code_replacements++`**
   （`:510-514`）→ 直接 `return`（注释：*"Hook output is a proposal, not a completed retransformation receipt"*）
3. **语义失效**（`:523-529`）：核心目标且非自有 retransform 周期 → 清 `g_last_success_tick`、
   置 `g_semantic_required_mask` 对应位、`request_integrity_refresh()`
4. **大小闸**：> 8 MiB 直接判失败（`:530`）
5. **字节预筛** `is_candidate(name, data, len)`（`:533`）→ 不合格就跳过，不回调 Java
6. **重入闸**：`g_transform_bridge`/`g_transform_method` 为空或 `g_in_transform`（**`thread_local`**，`:285`）→ 记失败并返回（`:534-536`）
7. **回调 Java 做转换**：`NativeTransformBridge.transform(String,[B)[B]`（`:543`）
8. **回调 Java 做证明**：`NativeTransformBridge.verify(String,[B,ClassLoader)Z`（`:552`）
9. **发布**：把 Java 返回的字节设为新类数据（`:553`）
10. `record_semantic_proof(name, verified, task_owned)`（`:562`）

**分工至此清楚**：**Native 驱动与发布，Java 决定写什么**。原生既不解析也不生成字节码。

**修正上一轮的两处低估**：
- 封印路径不只是"校验"——**字节不一致时它会替换回构建期镜像**（`:510-514`），是强制执行，不是只读比对。
- "新 loader 副本"这个说法**要限定范围**：封印路径有 loader 过滤（`registered_code_loader`）——
  只在**引导加载器**（`loader == nullptr`，仅 `BootstrapHooks` / `BootstrapTaskHooks`）或
  **mod 自己的加载器**（`g_root_loader`）上生效。跨加载器的同名副本走的是语义门路径，不受封印。

#### 6.1.3.4 Native 实际可达的类集合（`is_candidate` 实测）

| 类别 | 是否回调 Java | 说明 |
|---|---|---|
| 7 个 JDK 任务类 | 仅当任务桥就绪 | `FutureTask`、`ScheduledThreadPoolExecutor$ScheduledFutureTask`、`ForkJoinTask`、`ThreadPoolExecutor`、`ScheduledThreadPoolExecutor`、`ForkJoinPool`、`ThreadPoolExecutor$CallerRunsPolicy` |
| 16 个原版语义目标 | 恒是 | 见 §6.1 表 |
| `java/`、`jdk/`、`sun/` 前缀 | **恒否** | 正确排除 JDK |
| 第三方类（非 `net/minecraft/`、非 `dev/ronova/coremod/`） | 仅当**字节里出现** entity / entity-renderer / client-control 类型的引用 | 靠子串扫描判定 |
| 其它 `net/minecraft/*` | 否 | 只有那 16 个目标被覆盖 |
| 283 个封印类 | 走封印路径（`is_candidate` 之前就 return） | 但受 loader 过滤 |

→ **Native 并不转换任意第三方类**，只碰"引用到实体/渲染/客户端控制类型"的那些。
Agent 侧的外部类拦截也覆盖同一批（Agent 靠 ASM 遍历全部已加载类），**又是一处交叠**。

#### 6.1.3.5 谁叫醒谁

| 方向 | 通道 |
|---|---|
| Native → Java | `ClassFileLoadHook` 回调 `transform`/`verify`；`ThreadEnd` → `task_thread_end`；任务桥回调 → `BootstrapTaskHooks` |
| Native → 自身 | `request_integrity_refresh()` 唤醒原生看门狗线程重做完整性轮次；`ClassPrepare` 触发代码重校验 |
| Java → Native | `NativeBridge.refreshCodeSeals()` → `refreshCodeSeals0`；`EnforcementRuntime` 轮询 `NativeIntegrityBridge` 的 11 元素租约状态；`NativeDominionRoot.publish` 推快照 |
| Java → Java(agent) | `AgentHooks.invokeSystemAgent("armLateGuard")` —— 经**系统类加载器**反射调 `RonovaAgent`（agent 类被注入在那里，不在 mod 的加载器里） |

语义租约判据（原生侧）：`required != 0 && (verified & required) == required && 未过期 && 连续失败为 0`
（`CAP_SEMANTIC_LEASE = 0x40`，`:290`；`g_consecutive_lease_failures`，`:343`）。
该项投影到策略层时只影响 3 个键：`PROTECT_CODE_INTEGRITY`、`RUNTIME_CRITICAL_CLASS_WATCH`、
`INTERCEPT_INSTRUMENTATION`（`policy/RuntimeCapabilityProjection.java:11-17`）。

#### 6.1.3.6 六个耦合点：断链后会发生什么

| 耦合点 | 断链条件 | 后果 |
|---|---|---|
| `JNI_OnLoad` 抓不到 JVMTI | 非标准 JVM / JVMTI 不可用 | `bootstrap()` 直接 `return 0` → 只剩 `CAP_JVMTI` 都没有 → `LOAD_FAILED` 路径 |
| `FindClass(NativeTransformBridge)` 失败 | mod 类加载器未就绪 | `return mask`（只有 JVMTI 位）→ 无 `CAP_HOOK` → **无语义租约** → 上述 3 个键降为 OFF/FENCED |
| `transform` 返回 null 或异常 | Java 侧 ASM 出错 | `record_semantic_proof(false)` → 租约不可满足，**但类照常加载**（不阻断启动） |
| 任务桥 jar 提取/加载失败 | 平台编码或 bootstrap 搜索失败 | 打印 `RONOVA_TASK_BRIDGE_UNAVAILABLE`、`taskHooks = null` → **JDK 任务观测与 cancel 门静默消失** |
| `ronova_root.dll` 加载失败 | 缺库/架构不符 | **被 `catch (Throwable ignored)` 吞掉**（`NativeBridge.java:58-60`）→ 回落 Java 本地决策源 |
| `armLateGuard` 反射失败 | 系统加载器里没有 `RonovaAgent` | **被吞掉**（`AgentHooks.java:71-73`）→ 无 late transformer，Mixin + early 仍在 |

**注意第 3 条的行为选择**：语义证明失败**不会阻止类加载**，只让租约失效。
即"证明不了"降级为"少了一项能力"，而不是"停止运行"——这是刻意的可用性取舍，
但与"Fail-static / fail-closed"的规范要求存在张力（见 §14.2）。

### 6.2 四个谓词（`RonovaDominion.java:34-37`）

| 谓词 | 拦什么 | 注入点 |
|---|---|---|
| `installPredicate`（受保护） | 受保护玩家的伤害/死亡/血量归零 | `LivingEntity.hurt` / `die` / `setHealth` |
| `installRemovalPredicate` | 受保护玩家的移除（带 reason 判定） | `Entity.remove(RemovalReason)`；外部类 `removalReason`/`isAddedToWorld` 字段写 |
| `installQuarantinePredicate` | 被隔离实体的一切可见性 | `Entity.revive`；`Level`/`ServerLevel`/`ClientLevel` 的 List/Iterable/Entity 返回；外部 `dead`/`deathTime` 字段写 |
| `installEntityPredicate` | **不拦任何东西**，仅作"这是不是一个 Ronova 实体"的判据 | 供外部渲染器入口的 `shouldSuppressRender` 使用 |

**注意**：谓词槽默认是空实现，且谓词内部异常一律判定为"不保护"（fail-open）。
即在谓词安装完成之前（类初始化 → mod 构造之间）所有门都放行。

### 6.3 启动与降级链

```
RonovaDominion 类初始化
  → NativeBridge.load(gamedir)：校验 OS/架构 → SHA-256 摘要 → 落盘名 ronova-<sha16>.dll → 逐字节比对 → System.load
  → 抓 JVMTI → 能力协商 → 启用 ClassFileLoadHook → eager retransform → 加载 ronova_root.dll
  → 状态判定 (mask & 0x7)==0x7 ? ACTIVE : JVMTI_LIMITED
构造期
  → Native 可用 ? NATIVE_ACTIVE : AutoAttach.ensureAttached(modJar)
      AutoAttach 起短命 helper JVM，反射 VirtualMachine.loadAgent 自挂
      （仅 DUAL_AGENT 模式；MIXIN 模式下 install 只写标记、不注册任何 transformer）
FMLLoadComplete
  → Native 可用 ? startIntegrityWatchdog(reassertMillis) : AgentHooks.markRuntimeReady()
```

**降级链**：`NATIVE_ACTIVE` → `JVMTI_LIMITED`（保留存储事务/读文件/Hook，语义租约不可用）
→ `LOAD_FAILED`/`UNSUPPORTED_PLATFORM` → 纯 Java Agent + Mixin → `DISABLED_BY_POLICY` → 纯 Mixin。

能力位：`0x1` JVMTI、`0x2` Hook、`0x4` Retransform、`0x8/0x10/0x20` Root、`0x40` 语义执行租约。

### 6.4 语义证明（防"假门"）

`SemanticGateVerifier` 区分"真的注入了门"与"常量池里恰好有钩子名字"：
逐指令校验门的存在与控制流（`guarded`），并要求过滤返回点不得跳转进过滤区间（`filteredReturn`）。
测试固定"假常量池字符串不构成证明""跳转极性反转""全部 return 边"等负例。
自述边界：**只证明注册门，不构成任意 JVM 完整性**。

另有 `RawAccessorContract`：为 10 个原版类提供字段级 `ronova$*` accessor，
并在被篡改（全 return、读错字段、加异常处理器/`synchronized`）时重写方法体。

### 6.5 能力模块（capability 包）

已登记为模块并接线的 **6 项**：`LIFECYCLE_CONTINUITY`、`TOPOLOGY_CONTINUITY`、
`VITALS_RECOVERY`、`NEGATIVE_EFFECT_CLEANSE`、`FLIGHT`、`BLADE_CONTINUITY`
（`CapabilityRuntime.java:42-49`）。

`Capability` 枚举共 **11 项**，另 5 项**不经过本调度器**：
`CONNECTION_CONTINUITY`（由 `AuthorityEnforcement` 直接判定）、
`PRIMARY_ACTION` / `AREA_ACTION` / `LOADED_ACTION` / `INSTANT_MINING`（由 `ServerActionService` 判定）。

其中最有分量的是**裸血量单元写入**：`RegisteredOwnerVitals` 绕开 mod 的 setter 直接写同步数据单元，
前置条件为 Owner 写租约 + 弱引用身份复验 + data lock `tryLock` + **6 个机制位均达 `PROTECT_RONOVA`**，
写后全量复验。

### 6.6 原生层实际能力

`ronova_native.cpp`（15 个导出）：`JNI_OnLoad`（抓 JVMTI）、`bootstrap`（能力协商 + Hook 安装 +
eager retransform + 语义租约位）、看门狗生命周期、CodeSeal 三项、
任务桥五项（观测/刷新/状态/接收者/取消）、完整性租约状态、`savedFileIdentity0`（取 `FILE_ID_INFO`）、
`readFileImage0`（限 16 MiB，读后复验身份/大小/时间戳/路径，8 元素结果码）。

`native_root.cpp`（10 个导出）：决策内核 —— `install`（快照解析 + epoch 序比较 → Installed/Stale/Current/Conflict）、
`decide`（四域判定，只接受 current 或 next epoch 提案）、`status`（15 项计数）、
`acknowledge`、`watchdog_pulse`（对账漂移检测，latch 去重）、`reset`。

---

## 7. 观察层与通用恢复链（observation 包，41 类）

### 7.1 核心类

`RecoverySourceTracker`（**1,952 行**）是"通用恢复链处置"的主状态机：
从"实体被内存移除"一路推进到"存储事务提交 → 独立读回 → 资源结算"，
内部以**进度字符串**表达阶段（`WAITING_MEMORY_ACK` / `PREPARING` / `IMAGE_UNPROVEN` /
`OWNED_BY_CONSUMER` / `STORAGE_ACKNOWLEDGED` / `WAITING_RESULT_ACK` / `BINDING_LOST` /
`PREPARATION_UNAVAILABLE` / `WAITING_OBSERVED_SAVE` 等），
所有写入前置校验"本次运行的监视者计划 + 原登记/位置/版本/深度 + 现资格 + 当前绑定"。

### 7.2 事实词汇表（`RecoveryWorkResult.Fact`）

| 域 | 事实 |
|---|---|
| 任务域 | `CANCEL_REQUESTED`、`CANCEL_ACCEPTED`、`CANCEL_REJECTED`、`NOT_EXECUTED`、`EXECUTION_EXITED`、`SUCCESSORS_CLOSED`、`EFFECT_UNKNOWN`、`NOT_CURRENT` |
| 存储域 | `SERIALIZED`、`FILE_COMMITTED`、`READBACK_VERIFIED`、`PRESERVATION_VERIFIED`、`MEMORY_CHANGED`、`RESOURCES_RELEASED` |

**核实结论（实测）**：`RESOURCES_RELEASED` 在全部主源码中**只出现一次** ——
`RecoverySourceTracker.java:1596`，写入对象是**存储工作键**（`storageClosedKey`），
与 `MEMORY_CHANGED` 一起构成"存储处置已确认"。
`DEEPSEEK-CHANGELOG.md:121-126` 声称"`RESOURCES_RELEASED` 无生产写入路径"——
该表述对**任务域**成立（任务退出/取消**确实不得**充抵资源结算），
但对**存储域**已不成立：存储链现在会写这两个事实。文档与源码在此处存在**表述时差**，建议澄清。

### 7.3 观察类的自述约束（签名级，逐条摘自类头 javadoc）

这些注释本身构成了对"能证明什么"的严格边界声明：

- `SavedDataSaveTrace` —— "有界建议性转录，**永远不是 durable 提交收据**"
- `SavedDataEncodedWrite` —— "实际压缩输出流接受的字节摘要，**不是提交收据**"
- `NbtEncodingPaths` —— "**绝不从 NBT 值推断来源**"
- `NbtStoragePreservation` —— "比较私有解码图像，绝不比较可变的、世界持有的 tag；位置仍需独立生产来源；**不授予写权限**"
- `NbtStorageImage` —— "纯有界候选构造；捕获的位置仍需 Root 来源与提交授权"
- `RecoveryFileReadback` —— "原始图像相等**不是**持久性、所有权或语义收据"
- `RecoveryLocationResolver` —— "结果**永不**证明恢复因果，也不授权写入"
- `RecoveryStorageReadbackQuery` —— "查询一个已确认的原意图；**不创建**新意图、写权限或完成事实"
- `StorageImageTask` —— "请求不含世界对象，**永不写源文件**"
- `StorageTransactionTask` —— "私有暂存的 OS 事务；worker 上无世界访问；token 中无权限"
- `StorageMutationSequence` —— "只是观察戳，**从不锁**外部缓存，也不授权写它"
- `RecoveryTaskDisposition` —— "任务退出、后继与队列资源**保持独立**"
- `RecordMutationTask` —— "有界外部监视器等待；**取消绝不冒充线程终止**"
- `DeferredReceiptBuffer` —— "在开始变更前预留结果保留；**只重试收据，绝不重试变更**"

### 7.4 任务状态机（`TaskExecutionGraph`）

判据（据 `DEEPSEEK-CHANGELOG.md` 与本类 javadoc）：
- `executionsExited()`：`entries>0 && activeExecutions==0 && entries==exits`
- `queuedNeverEntered()`：已观察提交但从未进入执行
- `successorsClosed(...)`：registration 本图观察过 + 子树遍历完整 + `losses==0` +
  每个后代要么已退出要么排队未进入 + 已执行后代的调度器完整。
  以下任一情况**保持未决**：后代从未被观察提交 / 后代正在执行 / 覆盖丢失 / 遍历被截断 / registration 未知。
- `losses!=0` 是保守的全局判据：一旦本图任何位置丢失证据，本轮不再写后继闭合（宁缺勿假）。

### 7.5 快照与复制来源

`CopyProvenanceIndex`（"只在真实复制操作之后被调用；不返回被修改的对象"）、
`StaticSnapshotLocator`（"找到正在被复制的精确快照的活持有者，**不调用 record getter**"）、
`InstanceFieldCursor`（"每次访问一个字段发现步骤，含被跳过的字段；**不保留反射 Field 列表**"）、
`DiscoveryCursor`（"注册发现迭代器的短期空闲租约，上下文退出或不活跃即释放"）、
`RetiredWorldAdmission`（"弱物理世界退休，**没有可能重新打开正在卸载世界的 LRU 驱逐**"）。

---

## 8. 存储提交链（Windows NTFS TxF）

`native/src/storage_transaction.hpp` 的事务语义（转述 + 源码核对）：

1. 动态 `LoadLibraryExW("KtmW32.dll", LOAD_LIBRARY_SEARCH_SYSTEM32)` 探测
   `CreateTransaction`/`CommitTransaction`/`RollbackTransaction` 与
   `CreateFileTransactedW`，四者齐备才 `supported()`。
2. `prepareStorageTransaction0`：要求 request 是精确的 `StorageCommitRequest` 类；
   **并发上限 2**（超出返回 `ERROR_BUSY`）；路径必须是 `<盘符>:\` 绝对形式；
   校验 `encoded()` 的 SHA-256 == `expectedHash`；建 5 秒超时事务；
   以 `FILE_FLAG_OPEN_REPARSE_POINT` 打开；校验句柄身份（`FILE_TYPE_DISK`、单链接、
   非目录/重解析/加密/稀疏/压缩、NTFS、`GetFinalPathNameByHandleW` 必须等于 `\\?\` + 请求路径）；
   校验文件现状 SHA-256 == `baselineHash`；写镜像；`SetEndOfFile` + `FlushFileBuffers`；
   重校摘要与身份；**必须先关闭文件句柄再提交**。
3. `finishStorageTransaction0`：由 `StorageWritePermit.beginCommitFor(request)` 决定提交还是回滚。
4. **关键语义**：暂存永不提交公共数据；**回滚不需要 Root 许可**；过期槽 5 秒回收；
   析构时句柄仍存活则自动回滚。

项目自述的边界（转述 `docs/CURRENT-PROGRESS.md:40-41`）：首个提交后端**限定 Windows 本地 NTFS**
并要求运行时 TxF 能力可用；不支持的文件类型、存储语义、路径或能力**保持未决**，
**不回退普通覆盖**，不操作任意第三方文件。

---

## 9. 网络与客户端投影

协议：自定义 SimpleChannel，当前协议版本 14（转述 `README.md:9`；`BUILD-RESULT-r66.txt` 记 protocol 10 —— 两者不一致，见 §15）。

### 9.1 服务端 → 客户端

| 包 | 作用 |
|---|---|
| `OwnerState` | uuid、protectedState、realmId、streamId、contextRevision、ownerRevision |
| `QuarantineSync` | 分片投影事务（`ProjectionFrame` 为其中一片） |
| `JudgmentFeedback` | 裁决回执（含 count 与 candidates） |
| `StrengthControl` 回执 | 强度快照（configured/active/pendingRestart/editable） |

### 9.2 客户端 → 服务端

| 包 | 作用 |
|---|---|
| `ActionRequest` | realm、session、序号、动作（PRIMARY/AREA/LOADED） |
| `BlockBreakRequest` | 瞬时挖掘 |
| `ProjectionAcknowledgement` | 客户端报告已提交规范投影模型，绑定其实际连接 |
| `ProjectionResyncRequest` | 缺口或超时后请求重同步（每秒最多一次） |
| `StrengthControlPackets.RequestPacket` | 强度编辑请求 |

### 9.3 投影一致性机制（network + projection 包）

`ProjectionCoordinator` —— "原子发布一个一致的投影包，并拒绝 epoch 回滚"；
`ProjectionBundle` —— 由**恰好一个**不可变快照 epoch 产出的全部运行时投影；
`ProjectionReceiptState` —— 单个连接的期望图像与确认；**新发布使旧收据失效**；
`ProjectionDeliveryState` —— "有界连接本地投递证据；**不推断服务端真值或渲染器完成**"；
`ProjectionDigest` —— "规范有界视图摘要；是协议证据，**不是任意渲染像素的证明**"；
`ProjectionTickBudget` —— 整 tick 的传输尝试共享同一额度；
`ClientParticipationTracker` —— "稳定性取决于必需客户端模型收据；**可选断开不缩小必需成员集**"；
`SessionJournal` —— 有界"最后收据"日志，服务独立客户端连接会话。

客户端 `ProjectionStreamState` 维护 realm/stream/contextRevision/revision/authorityEpoch/ownerRevision、
活跃标记表、分片暂存与字节账本（默认 64 MiB），`COMMITTED` 时替换镜像并回 ACK。

### 9.4 服务端动作面

`ServerActionService` —— "服务端动作校验、幂等、执行与回执"；
`StrengthControlService` —— "GUI 编辑共享规范 Root 命令路径与操作者权限策略"；
`StrengthStatusDetail` —— "仅显示域共享既有 256 字符 detail 字段，**永不共享执行权限**"。

---

## 10. 调度、预算与资源治理

### 10.1 每 tick 维护顺序（`RuntimeCoordinator.serverTickHead`）

`MinecraftServer.tickServer` HEAD 触发（Mixin），依次：
1. `reconcileNative()`（计入 MAINTENANCE 预算）
2. `ingressWork().maintain(now, 32, …)`（入站工作登记维护）
3. `diagnostics().sampleIfDue(now)`（表面/资源采样，最小间隔 1 秒）
4. 每 16 tick：额外维护
5. 每 4 tick：`recoverySources().drainStorageCommits(server, observationBudget)`

`tickServer` TAIL 触发 `serverTickTail`；`stopServer` HEAD 触发 `authorityState().beginShutdown()`。

### 10.2 预算类别（`BudgetCategory`）

| 类别 | 默认预算(µs) | 默认计数 |
|---|---|---|
| TERMINAL | 500 | 256 |
| CAPABILITY | 400 | 64 |
| DISCOVERY | 300 | 无上限 |
| ADMISSION | 400 | 128 |
| OBSERVATION | 200 | 256 |
| MAINTENANCE | 200 | 64 |

`BudgetSplitFlags` 支持 JVM 属性 / 环境变量解析，且**区分"无效的 JVM 属性"**这一来源状态。
`BudgetReceipt` 有强校验（`processed + deferred == attempted`、`maxObservedBacklog` 不小于进出 carry）。
`FairLaneCursor` 轮流选道（≤30 道）保证公平。`WorkBudget` 提供 `tryConsumeCount` / `remainingCount` / `hasTimeRemaining`。

### 10.3 宿主资源治理（15 个域）

`JAVA_HEAP`、`DIRECT_BUFFER`、`MAPPED_BUFFER`、`METASPACE`、`CODE_CACHE`、`THREAD`、
`THREAD_STACK`、`CLASS`、`CLASSLOADER`、`JNI_JNA_NATIVE`、`JVMTI_OWNED`、
`FILE_DESCRIPTOR`、`PLATFORM_HANDLE`、`GPU_UPLOAD`、`RENDER_BUFFER`。

`HostResourceGovernor` 的三轴诚实模型：
`Accounting` = `UNOBSERVED | PARTIAL_ESTIMATE | PARTIAL_EXACT`；
`Denial` = `NONE | ACCOUNTING_UNAVAILABLE | SOFT_LIMIT | HARD_LIMIT | HOST_PRESSURE`；
`Budget(soft, hard, recoveryReserve)` 且校验 `soft <= hard - recoveryReserve`。
`Priority` 区分 `NORMAL` 与 `RECOVERY`。`JvmResourceSampler` 采样内存池、CodeHeap、BufferPool。

### 10.4 运行表面（8 类）

`RuntimeSurface` = `DATA` / `NETWORK` / `PRESENTATION` / `INPUT` / `EVENT` / `TEMPORAL` / `REGISTRY` / `RESOURCE`。

`SurfaceRegistry.Maturity` = `S0_ABSENT → S1_OBSERVE → S2_ATTRIBUTE → S3_ENFORCE → S4_REPAIR → S5_RECOVER → S6_COMPLETE`；
`State` = `UNAVAILABLE | MONITOR | FAILED`。

`RuntimeDiagnostics` 实际注册 6 个表面采样器，且**每个都自带诚实的范围限定字符串**，例如：
- EVENT："本 Realm 的 handler 条目/取消请求；**最终总线结果未证明**"
- TEMPORAL："仅 Realm 构造与 Ronova 自有入站；**外部任务未证明**"
- REGISTRY："Realm 准入与已观察静态来源登记；**来源处置未证明**"
- NETWORK："**进程级共享计数器；非按 Realm 或对手归属**"
- RESOURCE："仅已登记的持有许可；**未观察的域保持不可用**"

### 10.5 完成判定（`CompletionTracker`）

9 个条件：`BODY_DISABLED`、`WORLD_RECORDS`、`OLD_EPOCH_REJECTED`、`RECOVERY_TASKS`、
`REBUILD_ENTRYPOINTS`、`SAVE_QUALIFICATION`、`TRACKING`、`SERVER_PRESENCE`、`NO_PENDING_WORK`。
状态三态 `MISSING | VERIFIED | FAILED`。

转述 `docs/DEEPSEEK-PHASE1-BASELINE.md`：全仓只有 3 处 `Condition.` 写入
（`BODY_DISABLED` / `SAVE_QUALIFICATION` / `NO_PENDING_WORK`），
其余 6 项**无任何生产写入者**，因此"按自身逻辑当前不可能产生完整证书"——
文档明确称这是"**诚实缺口，不是缺陷伪装**"。

### 10.6 其他运行时设施

`DeferredAuthorityTask`（`EXECUTED | STALE | EXPIRED | CONTEXT_LOST | ALREADY_CONSUMED`）、
`PlanExecutionLease`（有时限、绑定执行线程）、`OperationPlanRunner`（带重入闸，逐步校验租约与当前性）、
`IngressWorkRegistry`（`QUEUED → RUNNING → CANCEL_REQUESTED → FINISHING → COMPLETED/CANCELLED/FAILED`，
1024 owner / 256 task / 5 秒上限）、`PhysicalIncarnationTracker`（identity hash + 弱引用队列）、
`StateFeedbackSchedule`（稳定退避、反弹下一 tick 再查、`horizon==0` 表示持续）、
`RuntimeEventLedger`（事件进入/取消计数）、`WeakIdentityMap`。

---

## 11. 强度策略体系

### 11.1 三轴模型（这是"配置已达最高但能力未接线"能被如实表达的关键）

| 轴 | 取值 |
|---|---|
| `MechanismLevel`（请求/天花板） | `FOLLOW_PROFILE`(语法) / `OFF` / `MONITOR` / `PROTECT_RONOVA` / `RELATED_PATHS` / `STRICT_ALL` |
| `EvidenceState` | `UNOBSERVED` / `OBSERVED` / `VERIFIED` / `DEGRADED` / `FAILED`（5 值，实测 `policy/EvidenceState.java:4-10`） |
| `ActivationState` | `INACTIVE` / `ACTIVE` / `PENDING_RESTART` / `UNAVAILABLE` / `REJECTED` / `FENCED` |

`RuntimeCapabilityProjection` 把实测支持度（完整性证明、强制执行证明）投影到策略展示上，
**"连接实测支持到策略呈现，而不授予新的攻击权限"**。

### 11.2 预设矩阵（`ProfilePresetMatrix`，revision 1）

| 预设 | 取值 |
|---|---|
| `COMPATIBLE` | 全平面 `MONITOR`，攻击平面 8 项 + 拦截/防护平面 + 7 项运行时提升到 `PROTECT_RONOVA` |
| `ENHANCED` | 全部 54 项 `RELATED_PATHS` |
| `EXTREME` | 全部 54 项 `STRICT_ALL` |
| `CUSTOM` | 全平面 `MONITOR`，逐项覆盖 |

**安全下限**（`SAFETY_FLOORS`，任何预设与自定义都不得低于）：`PROTECT_CORE_AUTHORITY`、
`PROTECT_RESTART_RECOVERY`、`PROTECT_ROLLBACK`、`PROTECT_LOOP_GUARD` ≥ `PROTECT_RONOVA`。
矩阵完整性在类初始化时校验，缺项直接 `ExceptionInInitializerError`。
进入 CUSTOM 时会把预设**冻结**为显式值，防止后续矩阵升版静默改变玩家未编辑的项。

### 11.3 执行能力天花板（`TerminalActionPolicy`）

四类步骤 `LIFE` / `REMOVAL` / `REMOVED_RECORDS` / `BYPASS_RECORDS`，各自绑定动作集与机制掩码。
类内注释明确："**选择极限档并不声称覆盖 producer、原生或任意资源**"，
解析结果的天花板被固定为 `REGISTERED_ENTITY_ONLY`（`TerminalActionPolicy.java:46-48`）。

### 11.4 54 项高级能力（`StrengthOptionKey`，实测枚举完整）

**攻击 15**：`attack.non_entity`、`attack.logic_source`、`attack.recovery_source`、`attack.state_source`、
`attack.registration_source`、`attack.task_chain`、`attack.callback_chain`、`attack.network_reconstruction`、
`attack.save_reconstruction`、`attack.dynamic_code`、`attack.dispatch`、`attack.runtime_patch_disablement`、
`attack.target_class_lineage`、`attack.final_result_takeover`、`attack.source_trace`

**拦截 10**：`intercept.mixin`、`.asm`、`.instrumentation`、`.jvmti`、`.unsafe_reflection`、
`.classloader`、`.jni_native`、`.modlauncher`、`.thread_task`、`.all_return`

**防护 17**：`protect.life`、`.death`、`.removal`、`.entity_record`、`.identity_respawn`、`.display`、
`.anti_interception`、`.anti_all_return`、`.input_communication`、`.call`、`.code_integrity`、
`.core_authority`、`.auto_repair`、`.state_recovery`、`.restart_recovery`、`.rollback`、`.loop_guard`

**运行时 12**：`runtime.java_agent`、`.instrumentation`、`.jvmti`、`.native`、`.native_loader`、
`.render_guard`、`.critical_class_watch`、`.dynamic_code_watch`、`.native_module_watch`、
`.thread_continuity`、`.background_integrity`、`.anomalous_call_log`

枚举类头注释本身即声明：**"一个 key 存在并不声称它的执行器是活跃的"**。

### 11.5 14 个普通动作（`AttackAction`，实测枚举完整）

`strongAttack`、`rewriteLife`、`terminalize`、`directClear`、`clearEntityRecords`、
`suppressRespawn`、`suppressRebuild`、`suppressAdmission`、`attackRender`、`clearTracking`、
`clearHud`、`stripCodeDefense`、`attackCallChain`、`allReturn`。

动作等级：`FOLLOW_PROFILE / DISABLED / COMPATIBLE / ENHANCED / EXTREME`。

### 11.6 接线状态的**权威来源**（实测）

`/ronova strength status` 会**逐动作**打印四项：保存值 / 当前值 / 接线 / 高级解析原因。
其中"接线"一栏的取值在源码里是硬编码的（`DominionCommands.java:280-281`）：

```java
", 接线=" + (action == AttackAction.TERMINALIZE ? "新终结提交门" : "未接线（仅保存请求）")
```

同时命令末尾固定输出（`:378`）：
"r54：未接线的普通项只保存请求；用 /ronova strength status 查看接线范围。"
以及（`:285-287`）："关闭终结不会解除已有裁决。其余普通项尚不改变旧终态物理链。"

**结论：14 个普通动作中，当前只有 `terminalize` 一个被声明为已接线到"新终结提交门"，
其余 13 个在游戏内自报"未接线（仅保存请求）"。**

---

## 12. 工程与验证基础设施

### 12.1 构建链路（`build.gradle` 全文实测）

`.\gradlew.bat build --no-daemon` → ForgeGradle 6.0.54 + MixinGradle 0.7-SNAPSHOT →
`jar` 任务除常规产物外额外塞入三样：
`META-INF/ronova/asm-support.jar`（ASM 9.7 的 `org/objectweb/asm/**`，供系统类加载器使用）、
`META-INF/ronova/hook-support.jar`（`BootstrapHooks.class`，供 bootstrap 类加载器使用）、
`META-INF/ronova/task-hook-support.jar`（`BootstrapTaskHooks*.class`）。
Manifest 声明 `Premain-Class` / `Agent-Class` = `dev.ronova.coremod.agent.RonovaAgent`，
`Can-Redefine-Classes` / `Can-Retransform-Classes` = true。

`reobfJar` 之后自动执行 `native/build_code_seals.py` 注入代码封印。
`processResources` 展开 `mods.toml` 的 `${}` 占位符。
离线构建支持 `RONOVA_OFFLINE_MAVEN` / `RONOVA_PLUGIN_MAVEN` 环境变量。
字体默认从 Adobe 官方获取，离线时用 `-PronovaFontMode=local` + `local-fonts/`。

`build-support/` 提供 4 个自研工具：`ExternalAbiParityAudit`（ABI 一致性审计）、
`OfflinePackager`、`OfflineReobfuscator`、`StubJarGenerator`（配套 `stub-sources/` 下数十个
Minecraft/Brigadier/Netty 桩类，用于离线编译与 ABI 校验）。

### 12.2 自测体系

| 项 | 数量 |
|---|---|
| `build.gradle` 声明的 standalone 自测 | **95** |
| 其中需用开发输出（映射版 Minecraft）运行 | 6 |
| `src/test` 下 Java 文件 | 107 = 95 `*SelfTest` + 12 `*Scenarios` 夹具 |

`selfTest` 任务依赖全部 95 个 `run*SelfTest`；`check` 依赖 `selfTest`。
每个测试类是**独立 `main`**（非 JUnit），分别针对**重新混淆后的生产 JAR** 或开发输出运行。
另有 `inspectDurableRoot` 只读校验工具（需 `-ProotImages=…`）。

**r66 构建实测结果**（`BUILD-RESULT-r66.txt`）：
版本 `0.2.0-alpha.7.17-r66`，产物 SHA-256 `fd4e19ff…b9fa3`，3,843,493 字节，543 个 zip 条目、
0 重复条目；**selftests = 52**（生产 JAR 上 50 + 开发输出上 2）；
构建 75 个任务（执行 67 + 复用 8），耗时 1 分 11 秒；
两轮游戏冒烟均 `exit_code=0`、`original_world_unchanged=true`，
标记含 `R63_PLAN_PASS`（但 `outcome=PARTIAL`）、`R60_BINDING_PASS`、`R66_PROJECTION_PASS`、
`R64_OBLIGATIONS_PASS`、`R65_CHECKPOINT_PASS`。

> **口径差异**：`build.gradle` 现在声明 95 个自测，而 r66 这轮只跑了 52 个，
> README 写的是"85 个 Java 自测通过"。三个数字互不相同，属于不同时间点的口径，建议统一。

### 12.3 代码封印（code seal）——**已实测确认生效，不是空转**

> **更正**：本报告初版曾写"该头文件缺失时 CodeSeal 全部 `return JNI_FALSE`，封印恒 `supported=false`"。
> 该结论**错误**，已实测推翻。守卫是编译期宏 `#ifdef RONOVA_CODE_SEALS`（构建期开关，不是死路），
> 而生产构建**会**写该宏。详见下。

`reobfJar` 之后由 `build.gradle:188-200` 自动执行 `native/build_code_seals.py`（158 行），完整链路：

1. 从**已构建的 JAR** 中按 123 条类名前缀（`build_code_seals.py:11-123`）取出对应 class 的**真实字节码**，
   展开为 **283 个 class**（含内部类）；
2. 逐类算 SHA-256，汇总成 manifest，再算出 `build_id`；
3. 生成 `build/native-code-seals/registered_code_images.hpp`（**5,253,650 B**），内容含
   `#define RONOVA_CODE_SEALS 1`、`CODE_SEAL_BUILD_ID`，以及 283 个内嵌字节数组；
4. **用 Zig 重新编译 `ronova_native.cpp`** 成 `ronova.dll`（1,951,232 B），并归一化 PE 时间戳以支持可复现构建；
5. **把新 DLL 写回 JAR**，同时写入 `META-INF/ronova/code-seal-id.txt`（64 B，即 build_id）
   与 `META-INF/ronova/code-seal-manifest.json`（25,992 B）。

**r66 发布 JAR 实测内容**：`native/windows-x86_64/ronova.dll` = **1,446,912 B**（封印重建版）、
`code-seal-id.txt` = 64 B、`code-seal-manifest.json` = 25,992 B。
对照：源码树里那个 `ronova.dll` 只有 186,880 B，是**未封印的开发版**——
这也解释了 JAR 体积从 r65 的 3,828,857 B 跳到 r66 的 4,678,695 B。

**运行时闭环**：Java 侧 `NativeBridge.java:50` 校验 `id.matches("[0-9a-f]{64}")` 后调用
`enableCodeSeals0(buildId)`；原生侧 `ronova_native.cpp:746` 用 `strcmp` 与编译进来的
`CODE_SEAL_BUILD_ID` 比对，**不一致直接拒绝启用**（这一条同时防止 DLL 与 JAR 混搭）；
成功后启用 `ClassPrepare` 事件，每个被封印类加载时校验字节。

**封印范围是刻意收敛的**：283 个类集中在 **真值链**——
persistence、obligation、observation、terminal、engine、root/api、runtime、nativebridge、integrity
（含 `SemanticGateVerifier`、`RawAccessorContract`、`RecoverySourceTracker`、`ShadowDurableRootStore` 等），
只含 2 个 mixin。**client / content / projection / presence / budget 完全不在封印范围内。**

另有一枚**执行金丝雀** `integrity/CodeSealProbe.evaluate(long)`：
纯函数挑战应答，用于验证"封印类不仅字节一致、而且真的能执行"。
`codeSealState0()` 回报 5 项：enabled / registered / loaded / verified / replacements。

### 12.4 验证体系

`validation/` 三类目录：`7.14a/`（夹具 sourceSet + Smoke 用例）、`7.17/`（主战场）、`r54/`（空壳）。

`validation/7.17/` 实测结构：**83 个子目录 + 70 个文件**。子目录主要是
`baseline-r55…r66`（12 轮）、`dedicated-*/instance/`（数十个**完整专用服务端实例**，
每个含 `libraries/`）、`phase1-*`（约 40 个场景）、`native-file-transaction-probes`、
`reference-study`、`phase1-checkpoints`（检查点 JAR + SHA-256）。

顶层 JSON 产物 60 个，分四类：`acceptance-matrix.json`（85 KB）、
`source-manifest-r56…r66.json`（每轮源码清单，各约 50 KB）、
各类 `*-evidence.json`（约 35 个，记录 version / artifact_sha256 / log / runtime_evidence / scope）、
`*-final-audit.json`（r58–r66 每轮终审，各约 0.5–1 KB）。

**一个场景的验证流程**：写夹具 → 起隔离专用服（`dedicated-*/instance/`）→
跑场景 → 产出 `round-N.json` → 汇总进 `*-evidence.json` → 绑定该轮 JAR 的 SHA-256。

### 12.5 参考 Mod 研究（转述 `docs/ALL-MODS-RESEARCH.md`）

研究了用户提供的 **ArcaneVorte 0.5.6**、**永爱之刃 2.6 fix / 3.2**、**SuperSteve 6.9.1**：
建立了 **4,430 个 class / 48,315 个方法**的全量静态字节码索引（解析错误 0），
产出五份专题（启动、攻击、防护、来源、任务、持久写入、网络、代码变更）。
结论已转入规划：**M01–M14 共 14 项加强要求**。
明确未闭合：全部方法逐项人工审查、两个受保护 Native 库的展开函数体、隔离环境实际运行。
`memoryRemove` 按要求**只分析、不集成**。

---

## 13. 完成度总表

### 13.1 验收台账（实测，`validation/7.17/acceptance-matrix.json`）

| 状态 | 数量 |
|---|---|
| **PASS** | **0** |
| PARTIAL | 28（B01 B03 B07 B09 B10 B17 B18 B19 B20 X31 X32 X34 X35 X36 X37 X46 X50 X53 X54 X56 X59 X60 X61 X63 X64 X66 X67 X68） |
| NOT_RUN | 40 |

用例构成：`B01–B23`（23 项，7.17 核心语义验收）+ `X24–X68`（45 项，运行时/故障/容量/文档验收）。
历史 `X01–X23` 状态为 `LEGACY_X01_X23_SOURCE_MISSING; historical equivalence unproven`。

### 13.2 四态分类

**A. 已接线且有实测证据（生产可跑）**
- 物品、命令、GUI、HUD、剑体与 Tooltip 渲染、隔离渲染抑制
- Root 命令总线、durable 存储与文件后端、命令日志与幂等去重
- Mixin 31 类注入 + Agent 五门 + 原生 JVMTI Hook（含语义门验证）
- 6 个能力模块（生命/拓扑/血量/负面效果/飞行/剑连续性）
- 帧预算 6 类、宿主资源 15 域采样、8 类表面中的 6 个采样器
- 客户端投影分片装配 + ACK + resync；动作发件箱与回执
- 基线遥测（默认关闭）
- 存储链的生产自动正例：内存移除 → 自动准备 → 事务提交 → 独立读回 → 存储读回与保留双验证
  （`RecoverySourceTracker.java:1636` 要求 `READBACK_VERIFIED` **与** `PRESERVATION_VERIFIED` 同时成立）

**B. 已实现但未接线 / 无生产调用者**
- `extension/` 四个类（版本化扩展信封）：生产代码零实例化，仅测试使用
- `Capability` 枚举中 5 项无 `ServerCapabilityModule` 实现
- `resetSession()`、`recoverIfInvalid()`、`getMaskSprites()`、`snapshotAgeMillis()` —— 无调用者
- `JudgmentFeedback.candidates`：协议里编解码完整，客户端只取 `count`
- `masked_layer` 的多 mask / 多 layer 数组能力：loader 与 bake 都支持，运行期只取 index 0
- `NativeBridge.State.RESOURCE_MISSING`：枚举声明，`load()` 从不赋值
- `RonovaAgent.hookSupport`：已赋值、从未读取
- `BootstrapTaskHooks.observerMatches` / `deliveringTo`：无调用点
- `RonovaNetwork.request(RonovaAction)`：无调用者
- `ClientPreferences.VisualMode.HUD_ONLY`：能存能读能循环，**无任何分支使用**（唯一判定是 `== OFF`）
- **`TerminalLifecycle.enforce` 的服务器分支整体不可达**（`engine/TerminalLifecycle.java:49-62`，实测：每条服务器路径都在 54/56/61 行 `return`）→
  连带 `integrateDeathOnce` / `writeLivingTerminal` / `invokeKill` / `invokeDiscard` 成为死代码；
  生产上唯一的一次性死亡集成走 `TerminalPlanExecutor` → `runPlannedIntegration(planned=true)`。
  该类的非 planned 部分（约 40% 代码）既无执行路径也无行为测试
- `JudgmentState.mark` 在服务器路径已不再被调用，`firstExecution` 语义迁到 `EffectClaim`/`TerminalStateSink`；
  但 `ExecutionTargetPolicy.shouldJudge` 仍在读 `isJudged`
- `SpawnFirewall` 的 `Boundary.CONSTRUCTION` / `NBT` / `FACTORY` **全仓零引用**（实际接线只有 JOIN / LEVEL / ENTITY_MANAGER / FIRST_TICK 四道）
- `SpawnFirewall.execute`、`JudgmentEngine.judge(...)` 两个重载、`rayJudgment`/`areaJudgment` int 重载、
  `ServerActionService.executeTargeted` —— 生产零调用
- `DurableTerminalWatchSet.observe*` 三个方法**空方法体**；`remove(uuid, completion)` 的 completion 参数被忽略；
  `metrics()` 的三个 notification 计数与接口入口无关
- `PassiveTerminalWatchSet` 生产零实例化；`attachReconstructionObserver` 未接线
- `TerminalIndexRepair.finish` **恒返回 `DISPATCHED`**（自承"reinsert 尚未证明"）
- `ObligationCoordinator.checkpointFailures` 只累加，**无上限、无出口、无告警**
- `engine/AuthorityIdentity`、`AuthorityDecisionResolver`、`ExecutionScopePolicy`：生产零调用，
  但被结构自测"钉住"以防误删 —— 即**测试在保护死代码**
- `LineageIndex.trimToSize` / `removeEldestToSize`：生产零调用

**C. 仅声明 / 占位**
- `CompletionTracker` 9 个条件中 6 个无生产写入者
- `root/api/` 三个扩展接口（`CompletionContractExtension` / `TargetEpochExtension` / `WriteSinkExtension`）
- `native_root.cpp` 的 `capability_mask` 恒返回 `0x8|0x10|0x20`，无降级分支

**D. 已知未完成（项目文档自述）**
- 四块工作均未闭合：Native 最终提交的完整资格/代码竞争与进程退出、
  外部保存与重启（静态持有者、未加载来源、回执丢失后的安全重试）、
  第三方任务与资源结算（后继/委托、共享队列引用、停服结算）、
  自动整链与"真正稳定完成"
- 当前自动准备限额为压缩 64 KiB / 解码 256 KiB / 4096 节点，是**临时限额**
- Completion 仍为 20 tick 稳定窗口 + 最大 10 tick 采样间隔
- 完整 7.17、FULL L5、L6、L6E **全部未宣告完成**

---

## 14. 发现的问题与风险清单

### 14.1 A 类：功能性缺陷或文档不实（建议处理）

| # | 问题 | 证据 |
|---|---|---|
| 1 | **按键文案与实际行为不符**：`O` 键的 lang 文案是"打开运行时主权控制台"，但实际打开的是**强度设置界面**；真正的控制台只能从强度界面第 5 页进入 | `lang/zh_cn.json:5` vs `client/RonovaClient.java:102-104`、`RonovaStrengthScreen.java:57` |
| 2 | **视觉选项半失效**：`HUD_ONLY` 与 `ALL` 行为完全相同；lang 声称该选项控制"服务端裁决粒子"，但服务端粒子**无条件发送**，客户端设 OFF 也照发 | `client/RonovaVisuals.java:15`、`network/ServerActionService.java:222,328`、`lang/zh_cn.json:17` |
| 3 | **协议版本口径冲突**：README 称"源码已推进到协议 14"，r66 构建结果记 `protocol: 10` | `README.md:9` vs `BUILD-RESULT-r66.txt:20` |
| 4 | **自测数量三个口径**：`build.gradle` 声明 95、r66 实跑 52、README 写 85 | `build.gradle:210-306`、`BUILD-RESULT-r66.txt:8`、`README.md:10` |
| 5 | **`RESOURCES_RELEASED` 表述过时**：changelog 称"无生产写入路径"，实际存储域已有唯一写入点（任务域确实没有） | `DEEPSEEK-CHANGELOG.md:121-126` vs `RecoverySourceTracker.java:1596` |
| 6 | **英文环境缺键**：强度设置界面全部文案硬编码中文，`en_us.json` 无对应键；Tooltip 正文同样是硬编码中文 | `client/RonovaStrengthScreen.java`、`client/tooltip/RonovaTooltipText.java:39-134` |
| 7 | **谓词 fail-open**：8 个谓词槽默认空实现，谓词内异常一律判定"不保护"；在类初始化到 mod 构造之间所有门都放行，且谓词侧无失败计数 | `agent/BootstrapHooks.java:21-29,241-263` |
| 8 | **注释与实现相反**：`ClientQuarantineView` 类头注释称 fail-static 到不可答时是"对渲染取消而言安全的方向"，但实现 fail-static 到 `present=true`，即**不取消渲染** —— 与注释描述的保证相反 | `engine/ClientQuarantineView.java:17-20` |
| 9 | **裁决未知结果对玩家静默**：`receipt.unknown()` 时上层直接丢弃且不发布任何反馈，玩家看不到"结果未定" | `network/ServerActionService.java:93-95` |
| 10 | **状态字段恒为 0**：`/ronova status` 输出的 `watch_notification_failures` 读的是 `passiveWatchNotificationFailures`，而自增方法 `recordPassiveWatchNotificationFailure` **零调用者** → 该诊断项永远是 0 | `runtime/DominionServerRuntime.java:278-284` vs `command/DominionCommands.java:89-90` |
| 11 | **防护失败方向反了**：`AuthorityEnforcement.isClosing` 用 `catch (Throwable) { return true; }`，即异常期间判定为"正在关闭"→ 放行移除/断开，owner 防护在异常时失效 | `engine/AuthorityEnforcement.java:129-140` |

### 14.2 B 类：静默降级与可观测性

| # | 问题 | 证据 |
|---|---|---|
| 8 | Native 不可用、Agent 自挂失败、DLL 缺失、任务桥提取失败，四者都只体现在日志/计数器，**没有一条路径把模组置为显式"降级运行"状态**供游戏内查询 | `agent/AgentHooks.java:71-73`、`NativeBridge.java:58-63` |
| 9 | Agent 反射失败被静默吞掉，`isAgentActive()` 返回 false 但无告警 | `AgentHooks.java:71-73` |
| 10 | 看门狗与 retransform 的 `Throwable` 全部吞掉（注释称 best effort），与"失败必须可见"的权威目标存在张力 | `RonovaAgent.java:107-109,129-131` |
| 11 | `CapabilityRuntime.maintainAnchors` 内层异常一律记为 `TOPOLOGY_CONTINUITY` 失败，不区分真实原因 | `CapabilityRuntime.java:105-107` |
| 12 | Native 决策桥对任何 `Throwable` 返回 null 并计入 `DEGRADED_DECISIONS`，Native 故障会**静默降级为本地策略** | `NativeRootBridge.java:56-58` |

### 14.3 C 类：结构性风险

| # | 问题 | 证据 |
|---|---|---|
| 12 | **执行器覆盖严重不足**：14 个普通动作中 **12 个没有对应的 `Step` 执行器**；`RecoveryChainPolicy` 的 8 个 Step + 4 个已登记门是**权限判定而非执行器**（自承需要当前来源与写租约）；`TerminalActionPolicy` 只提供 4 个 Step | `policy/TerminalActionPolicy.java:7-17`、`policy/RecoveryChainPolicy.java:57` |
| 13 | **策略双映射并存**：`ProfilePresetMatrix` 的逐键 54 项表与 `PolicyState.profileLevel` 的旧统一 r52 映射**同时存在**，前者注释承认后者需被替代 → 语义漂移风险 | `policy/ProfilePresetMatrix.java:12-14`、`policy/PolicyState.java:126-133` |
| 14 | **每 tick 重复维持**：`maintainAnchors()` 被 `RuntimeCoordinator` 每 tick 调用**两次**（注释承认是为"不管 Forge listener 顺序都赢过直接字段写入"），成本翻倍 | `runtime/RuntimeCoordinator.java:74-75,116-117` |
| 15 | **双份实现无断言固定**：`EntityTypeNbtAdmissionMixin`（Mixin）与 `NbtFactoryContract`（Agent/Native）对同一方法做同一件事；`shouldBlockRevive`（隔离实体不得复活）与 `LifecycleContinuityModule.revive()`（主动复活 Owner）意图相反，仅靠谓词不相交维持自洽。**完整的交叠矩阵见 §6.1.1** | 拦截层调研 + `BootstrapHooks.java:79-81`、`LifecycleContinuityModule.java:15` |
| 16 | **双 tick 脉动**：`ClientRuntime.pulse` 每 tick 被调用两次、`framePulse` 每帧两次；`feedbackTicks` 有 gameTime 去重，但输入边沿与恢复/界面维护的重复执行未被屏蔽 | `MinecraftClientRuntimeMixin.java:12-15` + `RonovaClient.java:96-116` |
| 17 | 客户端修复全程吞异常（8 处），失败只靠下一 tick/帧重试，无失败上报表 | `ClientRecoveryController.java:116,123,130,144,151,162,175,183` |
| 18 | 外部控制抑制靠**方法名前缀**与**类名子串**猜测第三方 guard/渲染条件，命名风格不同即失效 | `RonovaTransformer.java:502-538` |
| 19 | `SavedData.save` 观测绑定依赖注册的 `DimensionDataStorage` Mixin，**instrumentation 缺失时状态为"未知"而非"失败"** | `StorageMutationTracked.java` 类头注释 |
| 20 | `Tooltip` 的 `currentGraphics` 是静态字段，若事件在 Pre 与 Color 之间被取消会保留过期引用 | `RonovaTooltipEvents.java:24,42,54-55` |
| 21 | 失败隔离 lane 全部 `catch (Throwable ignored)` 且**无任何日志** | `engine/TerminalPlanExecutor` 失败隔离路径 |
| 22 | 死信日志用 `identityHashCode` 作 runtime 标识，**跨进程不稳定**，无法用于事后定位 | `engine/spawn/SpawnAdmissionRuntime.java:376` |

### 14.4 自测覆盖的可信度（决定"已验证"能信到什么程度）

| 覆盖类型 | 类 |
|---|---|
| **有真实行为测试** | `LineageIndex`、`SphericalRange`、`RegisteredIntIndexSlots`、`BoundedRetryQueue`、`TerminalWatchSet` 三个实现、`PresencePolicy`/`OwnerImmunity`/`PresenceSplitFlags`、`StrengthPolicySnapshot`/`ProfilePresetMatrix`/`RecoveryChainPolicy`/`TerminalActionPolicy`、`ObligationRegistry`/`CandidateRestrictions`/`RestorationGapBuffer` |
| **只有 ASM 结构门禁、无行为测试** | `JudgmentEngine` **逻辑本体**、`JudgmentExecutor`、`TerminalPlanExecutor`、`TerminalIndexRepair`、`TerminalStateSink`、`TerminalStateFeedback`、`TerminalLifecycle`、`TerminalCompletionMonitor`、`SpawnFirewall`、`TopologyIndex`、`TargetDiscovery` |

即：**终结语义最核心的那条链，其正确性主要由"注入的门存在且控制流正确"这类结构断言保证，
而不是由行为测试保证。** 这与项目"只有实际效果才算证据"的自我要求存在落差。

已知脆弱点：`RegisteredIntIndexSlotsSelfTest` 反射 fastutil 私有 `mask`/`size`（版本强耦合）；
`TerminalWatchSetSelfTest` 反射构造 MC `ResourceKey`；
`RuntimeIndexSelfTest` 压力用例**只打印耗时不设延迟阈值**；
`RawObservationSeparationSelfTest` 依赖可枚举的 production artifact。

正面事实：全部 95 个自测类**无 skipped / disabled / 空断言 / TODO**，
唯一的空 catch 体位于预期异常路径且其后均有独立断言。

### 14.5 D 类：工程卫生（量化实测）

| 项 | 实测 |
|---|---|
| **工作区不是 git 仓库** | 无 `.git` 目录；`.gitignore` 仅覆盖 `build/`、`.gradle/`、`run/`、`logs/`、`*.class`、字体 |
| 工作区体积 | **15,579 个文件 / 7.62 GB** |
| 根目录构建日志 | **200+ 个 `build-*.log`**（最大的 `build-7.17-server-install.log` 为 1.2 MB）+ 13 个 `BUILD-RESULT-r*.txt` |
| 根目录压缩包 | `mod.zip` 147 MB、`docs.zip` 626 KB |
| 其它未清理目录 | `错误报告-2026-8-29_10.29.27/`、`incoming-error-report/`、数十个 `validation/7.17/dedicated-*/instance/` 完整专用服实例（每个含 `libraries/`） |
| 源码整洁度 | **0 个 TODO/FIXME/HACK**；仅 2 处 `UnsupportedOperationException`（`RecoveryFileReadback.java:105`、`DurableRootFileBackend.java:294`）；1 处显式空实现 `legacyForgetOnTerminal`（`DominionServerRuntime.java:275`，附注释"Deliberate no-op"） |

**最值得注意的一条**：这不是 git 仓库。整个 7.62 GB 工程、200 多轮构建历史、
全部验证证据，**没有任何版本控制**——既无法回滚，也无法追溯"哪次改动引入了回归"。
配合项目文档里"不得虚报完成、保留全部失败日志"的严格纪律，却缺少最基本的代码历史保障，
这是当前最大的工程风险。

### 14.6 建议的处理优先级

| 优先级 | 事项 | 理由 |
|---|---|---|
| **P0** | 引入版本控制（git init + 合适的 `.gitignore`，把 `validation/`、`build-*/log`、`*.zip` 排除） | 7.62 GB 无历史、无回滚，任何一次改动都无法追溯；成本最低、收益最大 |
| **P0** | 修正 5 处文档/文案不实（`O` 键文案、视觉选项半失效、协议版本口径、自测数量口径、`RESOURCES_RELEASED` 表述） | 项目最大的资产是"文档可信"，这几处正在侵蚀它 |
| **P1** | 处理 `TerminalLifecycle` 服务器分支不可达 + 连带死代码 | 约 40% 的类无执行路径也无测试，容易被误读为"已有实现" |
| **P1** | 为终结主链补行为测试（`TerminalPlanExecutor` / `TerminalStateSink` / `TerminalIndexRepair` / `JudgmentEngine` 本体） | 目前只有 ASM 结构门禁，与"只有实际效果才算证据"的要求不匹配 |
| **P2** | 收敛死代码与双映射（`engine/AuthorityIdentity`、`AuthorityDecisionResolver`、`ExecutionScopePolicy`、`PolicyState.profileLevel`） | 结构自测在保护死代码，会持续误导读者 |
| **P2** | 修 `isClosing` 与 `ClientQuarantineView` 的失败方向 | 两处都在"异常时"走向了防护的反面 |
| **P2** | 补齐降级可见性（把 Native/Agent/DLL 降级暴露为游戏内可查状态） | 目前 4 条降级路径只体现在日志 |

---

## 15. 与项目自述的对照

项目文档的自我评价与本次源码核查**高度一致**，且明显偏向保守：

| 项目自述 | 本次核查 |
|---|---|
| "第一阶段未完成；完整 7.17、FULL L5 / L6 / L6E 未宣告完成" | 一致（68 项验收 0 PASS 实测确认） |
| "全部 54 项极限能力尚未完成" | 一致；且枚举类头注释明确"key 存在不声称执行器活跃" |
| "选择极限档不声称覆盖 producer、原生或任意资源" | 一致（`TerminalActionPolicy` 天花板 `REGISTERED_ENTITY_ONLY`） |
| "未接线的普通项只保存请求" | 一致（14 个动作仅 `terminalize` 声明接线） |
| "验收 28 PARTIAL / 40 NOT_RUN / 0 PASS" | 实测完全一致 |
| "不能把不同版本的子范围结果拼成某一个 JAR 的完整通过" | 一致（每份证据绑定 artifact_sha256） |
| "未知 ≠ 假 ≠ 安全" | 一致（三轴模型 + 大量"未证明/不推断"类注释） |
| "没有时间/代码量证据不估算已完成百分比" | 一致（全文无完成度百分比） |
| "本轮未宣称任何验收项转 PASS" | 一致 |

**唯一一处实质性落差**：项目要求"候选分数、源码中存在类名、接口返回 true、单次成功和局部通过
都不构成全体系完成"，但终结主链（`JudgmentEngine` 本体、`TerminalPlanExecutor`、`TerminalStateSink`、
`TerminalIndexRepair`、`TerminalLifecycle`、`JudgmentExecutor`）目前**只有 ASM 结构门禁，没有行为测试**
（见 §14.4）。结构门禁能证明"门被注入了"，不能证明"逻辑是对的"——这与项目自身对证据质量的要求不完全匹配。

**结论**：这个项目的文档纪律在整个模组开发生态里属于极罕见的严格水平——
它把"没做到"写进合同、把"不知道"建模成显式状态、把失败证据原样保留。
主要问题不在诚实度，而在**工程可维护性**（无版本控制、7.6 GB 无清理）与
**收尾能力**（54 项能力的执行器接线进度远落后于配置与规范，14 个普通动作只有 1 个接线）。

---

## 16. Native 层专项评估（"功能是不是有点少"）

### 16.0 先回答一个更基本的问题：Native 是不是只做拦截？

**不是。而且更准确地说：Native 几乎不做"拦截"——如果"拦截"指游戏行为拦截。**

**决定性证据（实测）**：枚举原生全部 `FindClass` 与 Java 回调目标后，它能碰到的 Java 类型只有这些：

| 原生 `FindClass` 的目标 | 用途 |
|---|---|
| `dev/ronova/coremod/agent/NativeTransformBridge` | 类转换桥（**唯一一个 Ronova 业务类**） |
| `java/util/concurrent/FutureTask`（3 处）、`ScheduledThreadPoolExecutor$ScheduledFutureTask` | 任务取消门 |
| `java/io/FileDescriptor` | 文件句柄身份 |
| `java/io/IOException`、`java/lang/Class`、`java/lang/Object` | 抛错与 bootstrap 模块解析 |

原生能回调的 Java 方法**全部只有 9 个**：`transform`、`verify`、`cancel`、`close`、`valid`、
`allow`、`accept`、`forName`、`getModule`。

**它从未碰到过 `Entity`、`LivingEntity`、`Level`、`World`、`AuthorityView`、`RootCommandBus`、
`LocalDominionStore` 或任何 Minecraft 类。**

> 也就是说——**原生在游戏行为层（谁被打、谁被移除、谁被复活、查询返回什么）的介入量是零。
> 它连一个 `Entity` 句柄都拿不到。**
> 真正拦游戏行为的是 Mixin 与 Agent 注入的门，那些门最终调用的是 Java 的 `BootstrapHooks`。

Native 唯一的"拦截"发生在**类加载层**，拦的对象是**字节码**而不是游戏状态；
而且即便在那一层，它的做法仍是"**回调 Java 决定写什么**"（见 §6.1.3.3）。

### 16.0.1 功能构成（按函数跨度实测，非精确归属）

`ronova_native.cpp`（1,114 行）内部分布：

| 职能 | 约略行数 | 是否"拦截" |
|---|---|---|
| JNI 导出层（bootstrap 能力协商、看门狗、封印开关 ×3、任务桥 ×5、租约状态、文件身份、读回） | ~368 | 混合 |
| 类加载拦截与转换（`is_candidate` / `class_file_load_hook` / 候选签名 / 语义证明 / retransform / reassert） | ~293 | 是（类层） |
| 完整性刷新与租约判据（`request_integrity_refresh` / `semantic_lease_current`） | ~153 | 否——是证明与唤醒 |
| 任务边界与取消门 | ~141 | 半——拦的是 JDK 任务取消 |
| 代码封印强制执行（镜像比对与替换、`ClassPrepare`、重断言） | ~122 | 特殊（完整性，非拦截） |
| 其余（全局状态、工具、`JNI_OnLoad`） | ~45 | — |

`ronova_native.cpp` 之外的 850 行：

| 文件 | 行数 | 职能 | 是否"拦截" |
|---|---|---|---|
| `native_root.cpp` | 447 | **Root 决策内核**（快照解析、epoch 序比较、四域判定、对账漂移检测） | **完全不是** |
| `native_root_jni.cpp` | 162 | Root 的 JNI 绑定（`installSnapshot`/`exportSnapshot`/`decide0`/`acknowledge0`/`watchdog`/`reset` 等 10 个导出） | **完全不是** |
| `storage_transaction.hpp` | 177 | NTFS TxF 事务（私有暂存、句柄身份、提交/回滚） | **完全不是** |
| `native_root.hpp` | 64 | Root 头文件 | **完全不是** |

**结论**：1,964 行原生代码里，"类加载拦截"约占 **22%**；剩下 **78% 是验证、提交、决策、取证、调度**。
其中光 Root 决策内核一族（673 行，占原生代码 34%）就与拦截毫无关系——
它是一套**用 C++ 独立重写的判定实现**，`NativeDominionRoot` 在首次接受真值后会把
`LocalDominionCore` 的决策源切成它。

### 16.0.2 所以三层的真实角色划分是

| 层 | 拦什么 | 拦在哪一层 |
|---|---|---|
| **Mixin** | 游戏行为（伤害、死亡、移除、复活、查询返回、保存、渲染） | 方法体 |
| **Java Agent** | 游戏行为 + 第三方类字段写与渲染入口 | 方法体 / 字段访问 |
| **Native** | **类**（字节码与加载时机）；游戏行为层为零 | 类加载 |

这也解释了为什么 `NativeTransformBridge.transform` 是个 **Java** 方法：
原生的定位从来不是"执行者"，而是"**独立验证者 + OS 级提交者 + 决策副本**"。

### 16.1 规模：源码小，发布产物不小

| 度量 | 数值 |
|---|---|
| `native/src` 合计 | **1,964 行**（`ronova_native.cpp` 1,114 / `native_root.cpp` 447 / `storage_transaction.hpp` 177 / `native_root_jni.cpp` 162 / `native_root.hpp` 64） |
| Java main 对照 | 37,381 行 |
| **源码占比** | **约 5%** |
| 构建期生成的封印头 | 5,253,650 B（283 个类的内嵌字节码） |
| 构建期重建的 DLL | 1,951,232 B（构建目录） |
| **发布 JAR 内的 DLL** | **1,446,912 B**（vs 源码树里未封印的开发版 186,880 B） |

**结论一：按源码行数看确实少，但"源码行数"对这个层是失真的度量**——
它的绝大部分体积由构建期生成（283 个类的字节码内嵌），而不是手写逻辑。

### 16.2 Native 的 5 项独占职责（Java 做不到的）

| 职责 | 为什么必须原生 |
|---|---|
| **覆盖补盲** | JVMTI `ClassFileLoadHook` 能看到 **新定义 loader 的副本**与新加载的类；Mixin 挂在 ModLauncher 加载期、Java Agent transformer 也依赖 `Instrumentation`，都有覆盖盲区 |
| **真值链字节封印** | 283 个类的字节码在构建期固定，运行期 `ClassPrepare` 逐类校验；`build_id` 双向绑定（DLL 与 JAR 必须同一次构建） |
| **执行金丝雀** | `CodeSealProbe.evaluate(challenge)` 验证封印类"不仅字节一致、而且真的能执行" |
| **OS 级提交** | NTFS TxF 私有事务、`FILE_ID_INFO` 句柄身份、提交前关句柄 |
| **决策副本** | `native_root.cpp` 独立读快照做 epoch 序 / 陈旧 / 冲突 / 漂移判定（domain 0–3 四域） |
| **任务边界** | `FutureTask.cancel` 的 `VarHandle.compareAndSet` 被替换为原生许可检查；`ThreadEnd` 提供真实线程退出事件 |

### 16.3 它**不**做什么（边界比想象中窄）

| 边界 | 事实 |
|---|---|
| **不改字节码** | `class_file_load_hook` 不自己注入，而是回调 Java 的 `NativeTransformBridge.transform(String,[B)[B`（`ronova_native.cpp:806`）；原生既不解析也不生成字节码 |
| **该改的时候才改** | 原生只决定"是否值得回调"（字节预筛）与"是否通过证明"（调 Java 的 `verify`） |
| **JVMTI 只申请 4 项能力** | `can_retransform_classes`、`can_retransform_any_class`、`can_generate_all_class_hook_events`、`can_access_local_variables`（`:817-823`） |
| **没有字段/方法级钩子** | 无 breakpoint、无 field watch、无 monitor 事件、无 method entry/exit、无 suspend/`GetAllThreads` → `protect.life` 这类**字段级强制仍然只能靠 Java** |
| **租约不是原生强制的** | 事务上下文里是 `env->CallBooleanMethod(context->lease, context->valid, context->plan)`（`:135`）——**回调 Java 问许可**，不是原生自己持有并校验 |

### 16.4 真正偏薄的地方

| # | 缺口 | 证据 |
|---|---|---|
| 1 | **没有原生 WriteSink / lease 强制** | 规范自承"完整 Native lease / WriteSink 尚未实现的部分继续作为明确工程项"；原生只查询 Java 的租约对象 |
| 2 | **不是独立的语义裁判** | 封印只保证"字节与构建期一致"；"注入的门是否有意义"由 Java 的 `SemanticGateVerifier` 判定。该项**本身也在封印清单内**（设计上补了一半），但判定逻辑与被执行体仍在同一 VM 里 |
| 3 | **无原生模块监视** | `runtime.native_module_watch` 是声明键，原生层没有 PE / 模块枚举代码 |
| 4 | **Root DLL 能力掩码写死** | `capability_mask` 恒返回 `0x8\|0x10\|0x20`，无降级分支 → 无法诚实上报"部分可用" |
| 5 | **决策可静默降级** | `NativeRootBridge.decide` 对任何 `Throwable` 返回 null 并计入 `DEGRADED_DECISIONS` → 回落 Java 本地策略，无玩家可见提示 |
| 6 | **枚举占位** | `NativeBridge.State.RESOURCE_MISSING` 声明但 `load()` 从不赋值 |
| 7 | **硬上限** | 并发事务 2、单次读回 16 MiB、看门狗间隔钳制 15–60 s、源文件读取仅限 `<盘符>:\` 绝对路径 |
| 8 | **Root 决策 DLL 不在封印链内** | `build_code_seals.py` 只重建并替换 `ronova.dll`；`ronova_root.dll`（415,744 B / `74AE9E90…`）原样出厂，构建期早于 r66 JAR 近两周。**做决策的内核本身不可验证，被验证的却是它决策的对象** |
| 9 | **同一语义三层各写一遍，只有两层有测试** | `CompoundTag` / `EntityType` 被 Mixin、Agent、Native **同时**注入（见 §6.1.1）；Native 侧那两份注入没有对应行为测试 |

### 16.5 结论

1. **源码行数少（5%），但这不是"功能少"的证据**——发布产物里 1.45 MB 的 DLL 携带 283 个类的封印镜像，
   这是一条完整且少见的构建期信任链（生成 → 重编译 → 回写 JAR → 运行期逐类校验 → 双向 build_id 绑定 → 执行金丝雀）。
2. **真正的薄弱点是它的两个招牌承诺各自只兑现了一半**：
   - "**独立**验证" → 独立在**字节层**，语义判定仍在 Java；
   - "**绕过 Java** 的执行边界" → **这条边界不存在**：每一次强制判定都要回调 Java 拿许可。
3. 因此正确的提问不是"Native 功能够不够多"，而是**"要不要把执行边界真正下沉到原生"**——
   这是一个架构决策（会显著提高复杂度与平台绑定度），不是加几个函数的事。
4. 就当下优先级而言，**扩展原生是排错的顺序**：68 项验收 0 PASS、14 个普通动作只有 1 个接线、
   终结主链缺少行为测试——瓶颈明确在 Java 侧执行器与验证覆盖，不在原生能力。
   若要动原生，收益最高的三件事依次是：
   ① 原生侧做**字节码语义校验**（而非仅哈希）② 原生持有并强制 **lease/WriteSink**（让强制不再回调 Java）
   ③ **诚实的能力降级**（`capability_mask` 与降级状态可查询）。

---

## 17. 极限档强度实测（EXTREME 预设到底强在哪）

### 17.1 配置层：确实是最高

| 项 | EXTREME 的取值 | 位置 |
|---|---|---|
| 54 个高级键 | **全部 `STRICT_ALL`** | `ProfilePresetMatrix.java:147-148` |
| 14 个普通动作 | **全部 `AttackActionLevel.EXTREME`** → `.depth()` = `STRICT_ALL` | `AttackActionLevel.java:12` |
| 对照 COMPATIBLE | 基线 `MONITOR` + 8 个 attack 键 + 全 INTERCEPTION + 全 PROTECTION + 7 个 runtime 键 → `PROTECT_RONOVA`；5 个动作禁用 | `ProfilePresetMatrix.java:101-144`、`AttackActionPolicy` |
| 对照 ENHANCED | 54 键全 `RELATED_PATHS` | `ProfilePresetMatrix.java:145-146` |

配置层面 EXTREME 确实拉满。**问题出在运行期。**

### 17.2 运行期：有效深度被硬钉在 `PROTECT_RONOVA`

```java
effective = lowerOf( lowerOf(activeRequested, supportedCeiling), evidenceCeiling )   // :230-233
permitted = effective.rank() >= PROTECT_RONOVA.rank()                                // :211
```

而两个天花板的来源是**决定性的**：

| 事实 | 证据 |
|---|---|
| `supportedCeilings` 默认 **`OFF`** | `StrengthPolicySnapshot.java:99`（`levels(..., OFF)` 兜底） |
| 快照由 `fromConfigurations` 构造，天花板传 `Map.of()` | `ShadowDurableRootStore.java:344`、`StrengthPolicySnapshot.java:137` |
| `withCapability` 全项目**只有 2 个调用者**，且**只传 `PROTECT_RONOVA` 或 `OFF`** | `RuntimeCapabilityProjection.java:13,20`；`TerminalActionPolicy.java:40` |
| `supportedCeiling == OFF` ⇒ `activationState = UNAVAILABLE` | `:383-387` |
| `activationState != ACTIVE` ⇒ `resolveAttack` 直接拒绝 | `:205-208` |

**于是得到一个反直觉的结论**：

- `RuntimeCapabilityProjection` 只抬升 **6 个键**（`PROTECT_CODE_INTEGRITY`、`RUNTIME_CRITICAL_CLASS_WATCH`、
  `INTERCEPT_INSTRUMENTATION`、`RUNTIME_JVMTI`、`RUNTIME_NATIVE`、`RUNTIME_BACKGROUND_INTEGRITY`），
  且仅在证明当前时抬到 `PROTECT_RONOVA`；
- 其余 **48 个键的 `supportedCeiling` 保持 `OFF`** ⇒ 它们的 `activationState` 恒为 `UNAVAILABLE`
  ⇒ **任何依赖它们的操作一律被拒，与是否选极限档无关**。

即：**EXTREME 下最多只有 6 个键可能达到 ACTIVE；54 键里有 48 键在实践中不可用。**

### 17.3 等级高低在运行期只影响两件事

把 54 键 + 14 动作的全部消费点扫完，只有两处按**等级大小**分支，
其余全部是二值阈值 `rank() >= PROTECT_RONOVA.rank()`：

| 位置 | 语义 |
|---|---|
| `runtime/StateFeedbackSchedule.java:23` | `horizon = STRICT_ALL ? 0 : RELATED_PATHS ? 100 : 20`；**`0` 表示持续反馈、永不结束** |
| `policy/RecoveryChainPolicy.java` `Step.minimum` | 8 个恢复链步骤中 5 个要求 `RELATED_PATHS`，**3 个要求 `STRICT_ALL`** |

所以 `STRICT_ALL` 相对 `RELATED_PATHS` 的**唯一实质差别**是：
① 状态反馈从"100 tick 有界"变成"无限期持续"；② 满足那 3 个步骤的最低深度要求。

### 17.4 那 3 个"只有极限档才该解锁"的步骤，在 `resolve()` 路径上恒不可执行

```java
boolean executes = resolution.permitted()
    && resolution.effectiveDepth().rank() >= step.minimum().rank();   // RecoveryChainPolicy:55
```

`effectiveDepth` 上限是 `PROTECT_RONOVA` < `STRICT_ALL` ⇒ **`executes` 恒为 `false`**，
reason 恒为 `RECOVERY_DEPTH_LIMITED_BY_DEPENDENCY`。

涉及的正是"派生来源 `DERIVED_SOURCE`、后继任务 `DESCENDANT_TASK`、重载重建 `RELOAD_RECONSTRUCTION`"
——客观上就是最该由极限档解锁的三项。

**但生产不受影响**，原因见下节。

### 17.5 两套权限路径并存（这才是全貌）

| 路径 | 判据 | 结果 |
|---|---|---|
| `RecoveryChainPolicy.resolve(step)` / `StrengthPolicySnapshot.resolveAttack` | **证据天花板**（`effective`） | 3 个 `STRICT_ALL` 步骤恒不可执行 |
| `RecoveryChainPolicy.registered*()`（`registeredStorageCommit` / `registeredTaskDisposition` / `registeredAtomicSnapshot` / `registeredNbtOutlet`） | **预设 `requestedLevel()`，不看天花板** | 只要 `!pendingRestart && bindingVerified` 即放行 |

`registered*` 是**生产恢复链实际走的路径**（`ShadowDurableRootStore` 5 处、`EffectJournal` 3 处、
`RecoverySourceTracker` 1 处）。也就是说：

> **"强度必须由证据决定"这条要求，对生产恢复链不生效**——
> `registered*` 只检查"绑定已验证 + 未等待重启"，不检查 `supportedCeiling`。

这不是缺陷，而是分层设计：javadoc 与 reason 字符串都明说 policy 许可只是第一层，
执行仍需"当前来源 + 写租约"（`POLICY_PERMITTED_REQUIRES_CURRENT_SOURCE_AND_WRITE_LEASE`）。
但它确实意味着**配置型许可与证据型许可在两条路径上分离**。

另有一处相关事实：`TERMINALIZE`（唯一被声明"接线"的动作）走的是**第三条简化门**
`StrengthPolicyConfiguration.permitsBaselineAction`（`:85-98`）——
它对非 `TERMINALIZE` **直接 `return false`**，对 `TERMINALIZE` 只看动作深度 + 两个 requiredKeys 的
**预设 `requestedLevel`** ≥ `PROTECT_RONOVA`，**同样不看证据天花板**。
这也解释了 `/ronova strength status` 为何只说它接线。

### 17.6 结论

**"极限档"提高的是覆盖面、持续性与依赖严格度，不是强制深度。**

| 维度 | EXTREME 相对 COMPATIBLE 的实际变化 |
|---|---|
| 开哪些门 | 更多（COMPATIBLE 禁用 5 个动作、多平面停在 `MONITOR`） |
| 反馈持续多久 | 从 20 / 100 tick → **无限期** |
| 依赖要求 | 3 个恢复链步骤升到 `STRICT_ALL` 最低要求 |
| **执行深度** | **不变**——`effective` 恒 ≤ `PROTECT_RONOVA` |

**评价（三面）**：

1. **诚实性上这是对的**。`withCapability` 只肯给 `PROTECT_RONOVA`，因为只有那一档有实测证据；
   不给 `STRICT_ALL` 等于"不声称未经证明的深度"。`StrengthResolutionReceipt` 的 reason
   会明说 `active at supported ceiling` / `active at evidence ceiling`（`:410-417`）。
   `policyCounters()`（`RecoverySourceTracker.java:1901-1909`）甚至把
   `requested rank` 与 `effective rank` **分别**作为诊断计数导出——请求 4、实际 0 的落差是自己报出来的。

2. **命名与文档有误导**。玩家选"极限"后实际有效深度与"强化"相同（都封顶 `PROTECT_RONOVA`），
   差别只在门槛、持续性与依赖要求。规划文档 §2 那 54 条"极限目标"是**目标**不是**实装**。

3. **可发现性有缺陷**。`/ronova strength status` 的"接线"列与"高级解析"列会对同一行给出矛盾信息：
   例如 `TERMINALIZE` 行显示"接线=新终结提交门"，而同行"高级解析"可能是
   `UNAVAILABLE:attack.final_result_takeover`。两列都没撒谎（一个说代码路径存在，一个说策略不允许），
   但并排显示会让人误读。

4. **结构性问题**：两套权限路径（证据型 `resolve` vs 预设型 `registered*`）意味着
   证据天花板对生产恢复链不生效。若要让"极限档"真正等于"更深"，需要的是**给这些键建立可测量的支持天花板**
   （即 `withCapability` 能合法地传出高于 `PROTECT_RONOVA` 的值），而不是改预设。

**增强方案见**：[`docs/plans/Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md`](docs/plans/RESTRUCTURE-MAP.md#source-tier)
（P0 管道 → P1 举证 → P2 表面成熟度 → P3 深度差异化 → P4 路径合并，含红线与逐键举证清单）。

---

*本报告为只读梳理，未改动任何既有文件。*
