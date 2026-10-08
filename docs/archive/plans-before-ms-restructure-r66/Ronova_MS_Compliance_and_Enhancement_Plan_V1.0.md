# Ronova × MS 要求 合规与加强方案 V1.0

> **已取代（2026-09-12）**：本稿的 R-A1…R-S4 要求清单与 C1–C11 缺口已并入
> [`docs/DESIGN-SPEC.md`](../DESIGN-SPEC.md) 第二部分 §16–§17。
> 注意其中若干项按用户口径属 **L1 自定层**（MS 建议，非违规）。
> **本文件保留作历史，不再作为加强入口。**

要求来源：`SilverWolf-Lv-999/Minecraft-MS-Mod-Doc`（Apache 2.0）
- `base/minecraft-ms-1.20.1-skills.zh.md`（485 行，以下简称 `skills`）
- `base/item/minecraft-ms-msitem.zh.md`（418 行，以下简称 `msitem`）
- `base/entity/minecraft-ms-entitydefence.zh.md`（325 行，以下简称 `entitydefence`）
- `README.md`（61 行）

本地副本：`.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/`

性质：MS 文档是**秒杀（MS）圈的公开设计参考**，用户已指定其为实际 mod 对抗的要求来源。
本方案把它当作**外部要求**使用；Ronova 自身的内部规范（L6 V2.4 / Baseline V1.4 / L6E-0 V1.3 / V2.6 / P0-A V1.3）
**继续有效**，两者冲突时登记为设计缺口，不按"外部文档更新"覆盖内部冻结决策。

---

## 0. 本方案的定位

**目标：让 Ronova 满足 MS 文档的可验证要求，并把 Ronova 已有的强度落到 MS 的验收口径上。**

与另两份规划的关系：

| 文件 | 出发点 | 关系 |
|---|---|---|
| `Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md` | Ronova 内部规范（V2.6 §6.6、D91） | **互补**：那份解决"声明与实装的落差"，本方案解决"对抗要求与合规" |
| `docs/research/ronova-vs-ms-comparison.md` | 对照分析 | 本方案是其结论的执行版 |

---

## 1. 要求清单与现状判定

编号 `R-*` 为本方案自建，用于追溯；不引入新的能力键或动作。

### 1.1 纪律与边界

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-A1 | 不能修改文件（含依赖文件、Mod 文件） | `skills:62` | 未修改第三方文件 | ✅ |
| R-A2 | 不能针对（反射/MethodHandler/VarHandler/直接用对方的类） | `skills:63` | 明文"不按第三方 Boss、Mod ID、类名或包名编写特判" | ✅ |
| R-A3 | 不能删对方 Mod 文件 | `skills:64` | 无此行为 | ✅ |
| R-A4 | 目标分类不依赖其他 Mod 的包名/类名/文件/私有实现 | `msitem:41` | 同上 | ✅ |
| R-A5 | 不能修改 Forge 文件；正常对抗**不用 `-javaagent`**（无参数） | `skills:396`、`:398` | **`AutoAttach` 自挂 Agent = 违规** | ❌ **C1** |
| R-A6 | 不得通过修改他人文件/针对第三方/破坏他人 Mod 实现功能 | `README.md:57` | 同上 | ✅ |

### 1.2 状态模型

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-B1 | 动作与实体状态建模为**显式状态**：`READY`/`DISPATCHED`/`QUEUED`/`NORMAL_DEATH`/`ERASED`/`PROTECTED`/`IGNORED` | `msitem:75-87` | 有 `OperationReport.Status`、`EffectRecord.State`、`JudgmentState`，但**无统一七态模型** | ⚠️ 部分 |
| R-B2 | `ERASED` 与 `PROTECTED` **互斥**，冲突时确定优先级（通常先拒绝处理受保护对象） | `msitem:87` | 有 `OwnerImmunity`（免疫不变量），语义相近但未明文化为互斥优先级 | ⚠️ 部分 |
| R-B3 | 状态键 = UUID 或实体 ID + 维度；**不用 Java 对象引用作长期身份** | `skills:110` | `SubjectKey = (RealmId, subjectId)`，明文"运行时类/维度/位置/来源不参与相等" | ✅ 强于要求 |
| R-B4 | 卸载/切维度/世界关闭时清理过期条目 | `skills:110` | `RetiredWorldAdmission`（"弱物理世界退休"）、`ServerRuntimeRegistry.release` | ✅ |

### 1.3 统一动作调度

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-C1 | 全部入口汇入**同一** `ActionDispatcher` | `msitem:128`、`:140` | `JudgmentEngine` 三条入口统一收敛到 `judgeRootsReport` | ✅ |
| R-C2 | 只有**一个结算所有者**；其余路径只报告输入/补上下文/取消重复/播表现 | `msitem:140` | `onEntitySwing` 故意空实现（注释：避免跨 tick 重复裁决） | ✅ |
| R-C3 | `ActionContext` 含动作 ID/手/槽位/不可变快照；不用 `ItemStack` 引用作延迟身份 | `msitem:124` | `ActionRequest(realm, session, seq, action)` + `SessionJournal` | ✅ |
| R-C4 | 去重与幂等 | `msitem:168-179` | `RootCommandId` + canonical payload hash 幂等 | ✅ |
| R-C5 | 服务端**重新验证**后才执行 | `msitem:35` | `ServerActionService` 校验 + `RuntimeCoordinator` | ✅ |

### 1.4 目标与结果策略

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-D1 | 八维契约：物品/触发/目标/选取/结果/服务端/客户端/防御 | `msitem:28-37` | 分散在 `StrengthOptionKey`/`AttackAction`/README 操作表 | ⚠️ 未集中成契约表 |
| R-D2 | 目标选取用**稳定快照**，迭代期间不改集合 | `skills:37`、`msitem:412` | `TargetDiscovery` + 观察快照 | ✅ |
| R-D3 | 执行前重新验证（仍在目标世界/未清除/未受保护） | `skills:202` | 先 `observe` 后 `canJudge`，全部 `prepare` 后一次 `commit` | ✅ |
| R-D4 | 三种执行模式：标准死亡 / 实体清除 / 混合 | `skills:112-162` | `TerminalLifecycle`（标准）/`TerminalIndexRepair`（清除）/组合 | ✅ |
| R-D5 | 标准死亡保留归因、事件可观察可取消、**不重复掉落** | `skills:120` | 4 道防腐线（`mark` 首门、planned 路径不执行 `kill/discard`、唯一死亡入口、`LifecycleIntegrationReceipt` 持久预留） | ✅ **强于要求** |
| R-D6 | "走标准死亡"与"从查询/追踪/渲染消失"是**两件事** | `msitem:39` | `TerminalLifecycle` 与 `TerminalIndexRepair` 分离，后者自述"绝不调用实体生命周期方法" | ✅ |

### 1.5 线程与生命周期

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-E1 | 服务器世界改动提交逻辑服务器线程；禁止从渲染/网络/异步/Native 回调线程改 | `skills:189`、`:130` | `server.isSameThread()` 检查遍布；Native "不在任意线程修改世界" | ✅ |
| R-E2 | 待处理队列**串行**消费 | `skills:191-200` | `IngressWorkRegistry`（1024 owner/256 task/5s）+ `FrameBudgetGovernor` | ✅ |
| R-E3 | 客户端只负责表现 + 索引一致性；**不得成为唯一规则来源** | `skills:206` | 服务端权威；客户端只发请求 | ✅ |
| R-E4 | 客户端类用 `Dist.CLIENT` 边界；**专服不加载客户端类** | `skills:208-212` | `RonovaBladeItem` 用 `DistExecutor.unsafeRunWhenOn` | ✅ |

### 1.6 追踪回调双向规则

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-F1 | `PROTECTED`：阻止"结束 ticking / 结束追踪 / 销毁" | `skills:218` | `LifecycleContinuityModule` + `TopologyContinuityModule` | ✅ |
| R-F2 | `ERASED`：阻止"开始 ticking / 开始追踪" | `skills:219` | `PersistentEntityAdmissionMixin` 的 `startTracking`/`startTicking` 门 + `ServerLevelTickAdmissionMixin` | ✅ |
| R-F3 | 不用单一 `isRemoved()` 注入代替追踪层治理 | `skills:220` | 4 道 admission 边界 + 追踪门 | ✅ |

### 1.7 防御链五层

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-G1 | **唯一**分类入口 `isProtected(Object)` | `skills:228` | `AuthorityView.isProtected` + **三个** `QuarantineView` | ⚠️ **C5** |
| R-G2 | Forge 事件层 | `skills:237-239` | `DominionEvents` 三事件，`HIGHEST` + `receiveCanceled` | ✅ |
| R-G3 | 实体与生物方法层 7 类入口 | `skills:245-253` | 覆盖 `hurt`/`die`/`setHealth`/`remove`/`revive` | ⚠️ **缺** `getHealth`/`isAlive`/`isInvisible`/`shouldRender`/移动推挤 |
| R-G4 | `SynchedEntityData` 层：读侧固定值 + 写侧拒绝非法降值；**不冻结无关字段** | `skills:257-265` | `SynchedEntityDataStateAccessor` + `RegisteredOwnerVitals`（只碰血量单元） | ✅ |
| R-G5 | 周期修复层幂等；不每 tick 重建集合/刷包 | `skills:267-271` | `AuthorityEnforcement.maintain` + `StateFeedbackSchedule`（稳定退避/反弹加速） | ✅ |
| R-G6 | **普通 tick 前**应用状态修复；普通逻辑可能改动受保护值时 **tick 后**再修 | `entitydefence:252` | 只有 tick 前路径 | ⚠️ 缺 tick 后 |

### 1.8 规范实例与状态不变量

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-H1 | 唯一**规范实例**作恢复来源 | `entitydefence:58-60` | 有 `PhysicalIncarnationTracker`/`AdmissionPass`，但语义不同 | ⚠️ |
| R-H2 | 5 条规则（首占 / 构造与客户端同步不得替换 / **每个注册表接收同一对象** / 有意替换走显式生命周期 / 引用缺失不得造占位） | `entitydefence:62-68` | 无对应规则；Ronova 允许"later incarnation may represent the same identity" | ❌ **C6** |
| R-H3 | 通过正常生命周期初始化；内部集合修改**只用于修复** | `entitydefence:88-90` | `TerminalIndexRepair` 只做摘除；补回走 `TopologyContinuityModule` | ✅ |
| R-H4 | 不能把 A 发为规范却把字段相同的 B 注册进世界 | `entitydefence:101` | `bindingRevision` + `PhysicalIncarnationId.nonce` 可检测 | ⚠️ 未明文化 |
| R-I1 | 修复前状态不变量 7 项 | `entitydefence:72-86` | `TerminalStateSink` 只断言已登记 raw 字段 | ⚠️ **缺**旧坐标/旧旋转、包围盒、摔落 |
| R-I2 | 注册表修复只能恢复成员关系，不能让矛盾状态变安全 | `entitydefence:86` | `TerminalIndexRepair` 不碰生命周期方法 → 一致 | ✅ |

### 1.9 服务端与客户端注册表

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-J1 | 6 张服务端表**分别**可修 | `entitydefence:136-143` | accessor 齐（`EntityLookup`/`EntityTickList`/`EntitySection*`/`ClassInstanceMultiMap`/`PersistentEntityManager`）；`TerminalIndexRepair` 按 `Indexes` 分组 | ⚠️ **`ChunkMap` 追踪器独立修复未见** |
| R-J2 | 分区存储的完整视图与按类视图都可见；不改正在遍历的集合 | `entitydefence:147-149` | `RegisteredIntIndexSlots`（"视图是瞬态的，绝不跨修改保留迭代器"） | ✅ 强于要求 |
| R-J3 | **ID 与 UUID 修复是一个事务** | `entitydefence:153` | `TerminalIndexRepair` 有 `Key`/`Group`，但未见"事务"语义明文 | ⚠️ |
| R-J4 | 陈旧同键对象须经显式切换替换，不能共存 | `entitydefence:155` | `ConflictingPhysicalBinding` 检测 + `bindingRevision` | ✅ |
| R-J5 | 恢复真实缺失，**不无限重放生命周期回调**；执行前检查"缺失→存在"转换 | `entitydefence:172` | `LifecycleIntegrationReceipt` ONCE + `TerminalLifecycle` planned 路径不重放 | ✅ **强于要求** |
| R-J6 | 追踪器只在缺失或陈旧时创建/替换；只刷新该追踪的玩家 | `entitydefence:176-182` | `AuthorityRuntimeState` anchor/connection 弱绑定 + 追踪门 | ⚠️ 部分 |
| R-K1 | 6 条客户端路径修复 | `entitydefence:188-195` | `ClientRecoveryController`（含补回客户端实体表）+ `ClientLevelAccessor` | ⚠️ 部分 |
| R-K2 | 客户端收敛到服务端权威；**不建纯客户端重复对象** | `entitydefence:197` | 无纯客户端实体创建 | ✅ |

### 1.10 读取路径兜底与修复时机

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-L1 | 7 个查询族在底层陈旧时返回规范实体 | `entitydefence:203-211` | presence 投影过滤（**方向相反**：让读看不见被隔离对象） | ⚠️ 对 ERASED ✅ / 对 PROTECTED ❌ |
| R-L2 | 构造结果时按对象身份或规范运行时 ID **去重** | `entitydefence:213` | `TopologyIndex.related` 用 `IdentityHashMap` Set | ✅ |
| R-M1 | 修复时机：初次构造与正常生成 | `entitydefence:123` | `SpawnAdmissionRuntime.observeConstruction` | ✅ |
| R-M2 | 受保护实体自身 tick | `entitydefence:124` | `DominionEvents.onPlayerTick` | ✅ |
| R-M3 | 独立服务端 tick | `entitydefence:125` | `RuntimeCoordinator.serverTickHead` | ✅ |
| R-M4 | **注册表写入 hook** | `entitydefence:126` | 无 | ❌ **C7** |
| R-M5 | **注册表读取 hook** | `entitydefence:127` | 无（只靠 presence 投影） | ❌ **C7** |
| R-M6 | 客户端 Level 与渲染 hook | `entitydefence:128` | `MinecraftScreenGuardMixin` + `RenderLivingEvent` | ⚠️ 部分 |

### 1.11 幂等与内部集合

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-N1 | 修复幂等：结构正确时不新建/不重复/不重发/不重复回调 | `entitydefence:113` | `TerminalStateSink` 幂等 + 写后全量复验 | ✅ |
| R-N2 | 重入保护 | `entitydefence:115` | `CapabilityRuntime` ThreadLocal `repairing` + `ClientRecoveryGuard` | ✅ |
| R-N3 | 内部集合规则 9 条（按 ID/UUID/身份匹配；迭代敏感集合先快照；保留 active/passive 语义；条目缺失不新建替代对象；不重复添加按类成员关系；无真实状态切换不重放回调；追踪器缺失或指向错误才重建；客户端服务端分别验证；按类别记录修复） | `entitydefence:272-280` | 前 8 条基本满足；**第 9 条"按类别记录修复"部分满足**（`policyCounters` 只记策略计数） | ⚠️ |

### 1.12 独占列表防御

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-O1 | 独占过滤必须是**独立、明确的开关**，不藏在基础防御里 | `entitydefence:236` | **presence 隔离默认开启**，无独立开关 | ❌ **C3** |
| R-O2 | 启用前记录 5 条后果 | `entitydefence:228-234` | 无记录 | ❌ **C3** |
| R-O3 | 默认值是"完整列表 + 自我修复" | `entitydefence:236` | 默认是过滤 | ❌ **C3** |

### 1.13 转换器与 Native

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-P1 | 注入声明规范；**避免大范围 `@Overwrite`** | `skills:285-289` | 31 mixin，**`@Overwrite` 计数 = 0** | ✅ 强于要求 |
| R-P2 | 回调语义一致（`setBackValue` 只对有返回值生效；`cancel` 只在真跳过） | `skills:302`、`:307` | `SemanticGateVerifier` 的 `guarded`/`filteredReturn` 正检查此项 | ✅ |
| R-P3 | 转换器**有序去重**注册表 | `skills:328-334` | `RonovaTransformer` early/late + 幂等 `guarded` 跳过 | ✅ |
| R-P4 | 已加载类需 `RetransformClasses` | `skills:336` | bootstrap eager retransform + `ClassPrepare` 重校验 | ✅ |
| R-P5 | 重转换/重定义：单工作线程+阻塞队列、延迟启动、去重、**批量≤32**、失败输出类名/阶段/异常、降级逐个 | `skills:340-346` | `g_in_transform`（`thread_local`）重入闸 + 批量；**未验证批次上限是否为 32** | ⚠️ |
| R-P6 | 不对 JDK/Forge/LWJGL/游戏核心类做**全局全返回改写** | `skills:348` | `ALL_RETURN` 动作**未接线** | ✅（前瞻约束） |
| R-Q1 | Native 只在三条件下启用 | `skills:352-356` | 用于 retransform/class-file hook/底层能力 | ✅ |
| R-Q2 | **Native 不能成为业务规则的第二份实现** | `skills:358` | `native_root.cpp` 447 行四域判定 + epoch 序比较 + 漂移检测 | ❌ **C2** |
| R-Q3 | 命令桥接契约：窄命令 + 并发槽位保护 + 超时 + 不无限自旋 | `skills:360-374` | 10 个导出、明确返回码、`readFileImage0` 限 16 MiB | ✅ |
| R-Q4 | 启动顺序 8 步且**全部幂等** | `skills:376-389` | `NativeBridge.load` 顺序基本对应 | ✅ |
| R-Q5 | Native 实现规则 6 条，含"**不把 DLL 文件名/资源路径/Gradle 拷贝路径拆成三份手写常量**" | `skills:404-411` | `RESOURCE`/`ROOT_RESOURCE` 常量 + build.gradle 路径 + resources 路径**分离** | ❌ **C4** |

### 1.14 构建与验收

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-R1 | 不把 native 构建塞进 Java 编译任务 | `skills:438` | native 由独立脚本 + `reobfJar` 后的 `build_code_seals.py` 驱动 | ⚠️ 见 C4 |
| R-R2 | DLL 拷到 Jar 内 `META-INF` 资源位置 | `skills:451` | `META-INF/ronova/` ✓ | ✅ |
| R-R3 | 为 JNI/JVMTI 桥接、类恢复策略、**自映像保护**提供 CTest 或等价探针 | `skills:452` | `NativeRootWiringSelfTest`/`NativeSnapshotCodecSelfTest`（Java 侧）；**native 侧无 CTest** | ⚠️ |
| R-R4 | 验证序列：`git diff --check` → `compileJava` → `ctest` → 实机 | `skills:456-462` | 有 gradle 序列；**无 git**（工作区非仓库）；无 ctest | ⚠️ |
| R-R5 | 只通过 `compileJava` 不足以证明 descriptor/retransform/打包正确 | `skills:462` | 项目已有同义纪律 | ✅ |
| R-S1 | 完成标准三条：Native 正常 / Java 正常 / **字节码不会被别人改到** | `skills:478-482` | 283 类 CodeSeal + `build_id` 双向绑定 + 执行金丝雀 | ✅ **强于要求** |
| R-S2 | **完美击败肉眼标准 6 项**：血量显示全 0（含 data health/getHealth/后台）、死亡界面不闪烁不缺内容、GUI 血量 0；`say @e` 无、`forge entity list` 无、F3 的 E 无 | `skills:45-54` | **无任何对应验收** | ❌ **C8** |
| R-S3 | 两份验证矩阵（`msitem:378-388` 11 项 + `entitydefence:286-298` 11 项） | — | 68 项验收为内部证据型，口径不同 | ⚠️ |
| R-S4 | 三份故障定位表（约 27 种现象） | `skills:464-477`、`msitem:392-405`、`entitydefence:300-313` | 无对应诊断手册 | ⚠️ |

---

## 2. 合规缺口（C1–C8，必须先处理）

| # | 缺口 | 要求 | 严重度 | 处置 |
|---|---|---|---|---|
| **C1** | `AutoAttach` 代理注册 | R-A5（无外部/无参数） | **违规** | **移除**。主路径无需替代——`ronova.dll` 已自取 JVMTI（`ronova_native.cpp:780-786`）；`ImmediateWindowProvider`（Forge 47.4.11+，Ronova 在 47.4.22 满足）为**可选增强**。详见 [机制对照](../research/reference-priority-mechanisms.md) |
| **C2** | `native_root.cpp` 是业务规则的第二份实现 | R-Q2 | 架构张力 | 收窄为纯验证后端，或声明为有意设计并记录理由 |
| **C3** | 独占列表过滤默认开启、无独立开关、无后果记录 | R-O1/O2/O3 | **合规风险** | 开关化 + 记录 5 条后果 + 保持"完整列表 + 自我修复"为默认 |
| **C4** | DLL 文件名/资源路径/Gradle 拷贝路径三处手写常量 | R-Q5 | 维护风险 | 由构建任务从单一来源生成并校验 |
| **C5** | 三个 `QuarantineView` 而非唯一分类入口 | R-G1 | 判定漂移风险 | 增加单一门面并让三视图都经它（不删除分离设计） |
| **C6** | 无"规范实例"5 条规则 | R-H2 | 恢复正确性 | 引入规范实例契约（与 `bindingRevision` 对齐） |
| **C7** | 无注册表写入/读取 hook 作为修复触发点 | R-M4/M5 | 读侧时间窗 | 增加这两个触发点 |
| **C8** | 无肉眼验收标准 | R-S2 | **无法判断是否打赢** | 建立并在隔离专用服执行 |

**C1 是唯一的硬违规。C3 与 C8 是最高优先的实质缺口。**

---

## 3. 阶段设计

### M0 —— 合规整改（C1、C3、C4）

**入口**：无。可立即开始。

> **2026-09-11 修正（依据 [参考样本早期优先级获取机制对照](../research/reference-priority-mechanisms.md)）**
>
> 参考五个真实样本后，M0 的 C1 处置**从"需要寻找替代方案"降级为"删一段冗余回退"**：
>
> - Ronova 的 `ronova.dll` **已经在做**同一件事——`JNI_OnLoad` 内
>   `vm->GetEnv(&environment, JVMTI_VERSION_1_2)` 自取 JVMTI env，随后
>   `AddCapabilities` → `SetEventCallbacks(ClassFileLoadHook)`（`ronova_native.cpp:780-786, 817-846`）。
>   这与样本 himitsu 的机制相同，且合规（无外部进程、无启动参数、不碰 JDK 内部）。
> - 因此 `AutoAttach` 只剩 Java 侧 `Instrumentation` 回退职责，且**仅在 DLL 加载失败时触发**。
> - ⇒ **主路径无需任何替代**；`ImmediateWindowProvider` 由"必需前提"降为**可选增强**。
> - 副产品确认：Ronova 是本批实现中**唯一需要外部进程**的一个（永爱 2.6 用进程内 self-attach、
>   SuperSteve 6.9.13f 用定义 `sun.instrument` 包内类、himitsu 用自取 JVMTI、ArcaneVorte 用内嵌 agent）。

| 交付物 | 内容 |
|---|---|
| 移除 `AutoAttach` | 删除 helper JVM 自挂路径（`AutoAttach.java:26-33` + `AttachHelper`）；**受影响入口清单**；`MIXIN` 模式的降级语义。**主路径无需替代** |
| 能力未可用上报 | Native 加载失败时，在 `/ronova status` 中**显式**显示"JVMTI/Agent 层不可用"，不得静默降级。这是移除后的**真实能力降级**（只剩 Mixin），必须如实呈现 |
| D8 退休证据 | 证明等效能力已由 `ronova.dll` 提供（同机制、已接线），方可退休旧路径 |
| 可选增强（非前提） | 若要让 Java 侧回退也合规，可接 `ImmediateWindowProvider`（Forge 47.4.11+；Ronova 在 47.4.22 满足）；次选 `ITransformationService`。**不属于 M0 的完成条件** |
| 独占过滤开关化 | `PresenceSplitFlags` 增独立开关；关闭时走"完整列表 + 自我修复"；在文档中记录 5 条后果 |
| 收敛 native 常量 | 单一来源生成 DLL 名/资源路径/拷贝路径，构建期校验 |

**验收**：无外部进程、无启动参数即可运行；`/ronova status` 能显示各层真实可用性；
独占过滤关闭时查询返回完整列表且自我修复仍生效。

**明确不做**：M0 不改任何强度取值；不以"可选增强"为由推迟 C1 的移除。

### M1 —— 可见验收闭环（C8、R-S2/S3/S4）

**入口**：M0 完成（否则验收环境本身不合规）。

| 交付物 | 内容 |
|---|---|
| MS 肉眼验收清单 | 6 项判据落成可执行步骤（含 `/ronova` 指令序列、取样时机、多部位实体设置） |
| 两份验证矩阵 | `msitem:378-388` 11 项 + `entitydefence:286-298` 11 项，逐项映射到 Ronova 场景 |
| 故障定位手册 | 三份表约 27 种现象 → Ronova 对应的优先检查项 |

**关键**：这 6 项判据**可以完全对着原版实体测**（普通生物、末影龙多部位、掉落物），
**不需要第三方样本**，因此与项目"不运行原第三方样本"的纪律不冲突。

**验收**：肉眼清单在隔离专用服上逐项有结果（通过或不通过都记录）；两份矩阵逐项有判定。

### M2 —— 显式状态模型（R-B1、R-B2）

**入口**：M1 完成（先知道现状才能定义目标态）。

| 交付物 | 内容 |
|---|---|
| 七态模型 | `READY`/`DISPATCHED`/`QUEUED`/`NORMAL_DEATH`/`ERASED`/`PROTECTED`/`IGNORED`，映射到现有 `OperationReport.Status` / `EffectRecord.State` / `JudgmentState`，**不新增第二套真值** |
| 互斥与优先级 | `ERASED` 与 `PROTECTED` 互斥；冲突时**先拒绝处理受保护对象**；与现有 `OwnerImmunity` 对齐 |

**验收**：同一目标不会同时处于两态；冲突场景有确定结果且可复核。

### M3 —— 规范实例与状态不变量（C6、R-H1~H4、R-I1）

**入口**：M2 完成。

| 交付物 | 内容 |
|---|---|
| 规范实例契约 | 5 条规则；与 `SubjectKey`/`bindingRevision`/`PhysicalIncarnationId` 对齐；明确 Ronova 的"新 incarnation 代表同一身份"与 MS"不得新建字段相同对象"的调和规则 |
| 状态不变量 7 项 | 补齐旧坐标/旧旋转、包围盒、摔落状态；作为修复前置检查 |

**验收**：每条不变量有独立探测；违反时不进入注册表修复。

### M4 —— 注册表独立修复与读写窗口（C7、R-J1~J6、R-L1/L2、R-M4/M5/M6）

**入口**：M3 完成。

| 交付物 | 内容 |
|---|---|
| 逐表可修 | 六张服务端表分别验证可恢复（含 **`ChunkMap` 追踪器独立修复**） |
| ID/UUID 事务 | 两者作为一个事务提交，避免单侧恢复 |
| 读写 hook | 注册表**写入 hook** 与**读取 hook** 作为修复触发点 |
| 读侧兜底 | 7 个查询族：对 `PROTECTED` 保证"一定看得见"，对 `ERASED` 保证"一定看不见" |
| 去重 | 结果构造按对象身份/规范运行时 ID 去重 |

**验收**：`entitydefence:286-298` 的 11 项逐项通过；重复修复不产生重复实体/条目/追踪器/tick/渲染。

### M5 —— 防御链补齐（C5、R-G1、R-G3、R-G6）

**入口**：M4 完成。

| 交付物 | 内容 |
|---|---|
| 单入口分类 | 新增单一 `isProtected` 门面，三个 `QuarantineView` 都经它（保留 Raw/Presence 分离） |
| 方法层覆盖 | 补 `getHealth`/`getMaxHealth`、`isAlive`/`isDeadOrDying`、`isInvisible`/`shouldRender`、移动/推挤/速度/位置 |
| tick 后修复 | 在普通 tick **之后**再修一次（应对其他逻辑改动受保护值） |

**验收**：`skills:245-253` 的 7 类入口逐项有测试；tick 前后两段修复都幂等。

### M6 —— 转换器、Native 与构建收口（C2、R-P5、R-Q、R-R3/R4）

**入口**：M5 完成。

| 交付物 | 内容 |
|---|---|
| C2 裁决落地 | 二选一：(a) 把 `native_root.cpp` 收窄为纯验证后端，判定逻辑回 Java；(b) 保留并明文声明为有意设计 + 记录理由与边界 |
| 批次上限 | 验证并固定 retransform 批量上限（MS 建议 ≤32） |
| native 探针 | 为 JNI/JVMTI 桥接、类恢复策略、自映像保护提供 CTest 或等价探针 |
| 诊断手册 | 与 M1 的故障定位表合并 |

---

## 4. 阶段依赖

```
M0 合规整改 ──► M1 可见验收闭环 ──► M2 状态模型 ──► M3 规范实例/不变量
                                                          │
                                                          ▼
                                        M4 注册表修复与读写窗口 ──► M5 防御链补齐
                                                          │
                                                          ▼
                                                     M6 转换器/Native/构建收口
```

**建议实际顺序即 M0 → M6**，理由：M0 是唯一的硬违规，且不修就无法在合规环境里做后续验收；
M1 最便宜且直接回答"到底打赢没有"，其结果会决定 M4/M5 该优先补哪里。

---

## 5. 验收与证据要求

1. 每个 M 阶段结束时，其验收项必须有**可复核记录**（隔离专用服轮次 + 命令输出）。
2. 不新增 PASS 声明；不修改 `validation/7.17/acceptance-matrix.json` 的任何 `status`。
3. 保留全部失败记录；不把不同 JAR 哈希下的结果拼成某一版本的完整通过。
4. MS 肉眼清单的"不通过"也要记录——**这正是它的价值所在**。
5. 继续使用隔离的自有专用服与通用夹具；不运行原第三方样本，不替换原游戏与原存档。
6. 本方案的 `R-*` 编号是追溯用标签，**不是新的能力键、动作或验收编号**。

---

## 6. 红线

1. **不得为通过肉眼验收而改用违规手段**——尤其不得恢复代理注册、不得要求启动参数、不得修改任何第三方文件。
2. **不得把独占过滤藏进基础防御**（`entitydefence:236`）。
3. **不得对 JDK/Forge/LWJGL/游戏核心类做全局全返回改写**（`skills:348`）。
4. **不得让 Native 扩大为业务规则的执行者**（`skills:358`）——C2 未裁决前，不得在 native 侧新增判定逻辑。
5. **不得因为"MS 文档没提"就删除 Ronova 已有的更强机制**——尤其
   `LifecycleIntegrationReceipt` ONCE 语义、283 类 CodeSeal、Root 单一 writer、
   持久化正确性相关的一切。MS 的完成标准是下限，不是上限。
6. **不得让两套验收口径互相冒充**——内部证据型验收（68 项）与 MS 肉眼型验收是两件事，分别记录。

---

## 7. 与 Ronova 内部规范的关系

| 情形 | 处理 |
|---|---|
| MS 要求 Ronova 已有且更强 | 保持 Ronova 做法，在对照表中注明（如 R-D5、R-J5、R-S1） |
| MS 要求 Ronova 缺失 | 按本方案补（C3–C8） |
| MS 要求与 Ronova 内部规范冲突 | **登记为设计缺口**，不按"外部文档更新"覆盖内部冻结决策；由用户裁决 |
| Ronova 内部规范与 MS 无关的工程 | 不受本方案影响（如 ResultGraph 主线、逐键矩阵） |

**已知的实质冲突一处**：R-Q2（Native 不得是业务规则第二份实现）
vs Ronova 把 Root 决策内核放进 `ronova_root.dll`。**需用户裁决。**

---

*本方案为设计文档，未修改工作区任何既有文件。*
