# Ronova 无违规加强方案 V1.0

> **已被取代（2026-09-12）**：本版的 W4 / W7 / W8 三条红线没有 MS 文档依据，§4「明确不采纳」
> 有 10 项因此翻案。规则来源唯一化后的版本见
> [`Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md`](Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md)。
> 本文件保留作对照，不再作为加强入口。

日期：2026-09-11。性质：**设计文档**。未修改任何生产代码，未运行任何样本。

本文**收拢**此前散在多份文档中的加强设计，作为唯一的加强入口。依据与详细证据仍在各来源文档中：

| 来源 | 用途 |
|---|---|
| [`docs/research/reference-priority-mechanisms.md`](../research/reference-priority-mechanisms.md) | 五个参考样本的优先级机制与哈希 |
| [`docs/research/ronova-vs-ms-comparison.md`](../research/ronova-vs-ms-comparison.md) | 与 MS 要求的逐项对照 |
| [`docs/plans/Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md`](Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md) | C1–C11 缺口与 M 阶段 |
| [`docs/plans/Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md`](Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md) | 逐键矩阵与第二部（候选图线） |
| [`FEATURES.md`](../../FEATURES.md) | 全功能梳理与 §17 极限档强度实测 |

**编号口径**：本文的 `A`–`H` 块与 `W1`–`W8` 红线是**追溯标签**，
不是新的能力键、普通动作、阶段或验收编号。落地时并入既有 `CF*` / `M*` / `Q*` / `B*` / `T*` 体系。

---

## 1. 红线（可判定）

| ID | 红线 | 判定方法 |
|---|---|---|
| **W1** | 不修改任何文件：第三方文件、依赖文件、Mod 文件、Forge 文件、游戏核心文件 | 写操作清单可审计；自有 Root 文件与自身临时产物除外 |
| **W2** | 不针对：不按第三方的包名 / 类名 / Mod ID / 私有实现判定 | 判定路径的字节码级审计：目标分类不得读取任何名称常量 |
| **W3** | 不删对方 Mod 文件 | 无删除操作 |
| **W4** | **无外部**：不创建任何外部进程 | 进程树中无本 mod 派生的子进程 |
| **W5** | **无参数**：不要求 `-javaagent` / `-agentlib` / `-D` / `--add-opens` 等任何启动参数 | 无参数启动即可完整运行 |
| **W6** | 不对 JDK / Forge / LWJGL / 游戏核心类做全局全返回式改写 | 改写目标清单 + 逐项契约 |
| **W7** | 不破坏宿主稳定性、Authority 正确性、用户数据安全、JVM 可持续运行 | 预算与资源治理实测（V2.6:449） |
| **W8** | **不篡改既有 JDK 类的行为**（改 `InstrumentationImpl` 链、往既有 JDK 类塞插桩） | 无对 `java.*` / `sun.*` / `jdk.*` **既有类**的字节码写入 |

### 1.1 W8 的边界（本轮实测修正）

**W8 只禁止"改写既有 JDK 类"，不禁止"定义自有新类"。** 但后者的实际可达性经头文件核实后是**有条件的**：

`jni.h:794` — `DefineClass(const char *name, jobject loader, const jbyte *buf, jsize len)`

`loader = null` ⇒ 类进引导加载器的**未命名模块**。在 JDK 9+ 的 JPMS 下：

| 目标 | 可达 | 原因 |
|---|---|---|
| 引导层可见、不可被任何 classloader 遮蔽 | ✅ | 类进引导加载器 |
| 取得 `sun.instrument` 等包的**包私有访问** | ❌ | 未命名模块 ≠ `java.base` 命名模块 ⇒ **不同运行时包** |
| 定义进 `java.*` | ❌ | prohibited package name（JDK 明文禁止） |

⇒ **本方案只取"引导层可见"这一半**（它能消掉一次磁盘写入，见 A3）；
不追求包访问——那需要 `Unsafe` / `ReflectionFactory` / 改私有字段，落回 W8 禁止区。

---

## 2. 可达上限：T0 三件套能到哪

MS 文档的 T0 门槛（`skills:41-43`）三项，**逐项经 JDK 17 头文件核实**：

| T0 项 | 可达性 | 依据 |
|---|---|---|
| **① 脱离 JDK 并定义一个 JDK 的类** | **一半**：引导层可见 ✅ / 包访问 ❌ | `jni.h:794` + JPMS 模块边界（§1.1） |
| **② 纯 Java Hook 你的 Native 函数** | ✅ **完整达到，且强于 Java 版** | `jvmti.h:706` `can_set_native_method_prefix`；`:1417` `SetNativeMethodPrefix`；**`:1421` `SetNativeMethodPrefixes`（复数，按 classloader 分别设前缀）** |
| **③ 纯 Java 杀死你的 JVMTI** | **效果可达（中和），不可销毁** | 公开 JVMTI 无枚举其它 `jvmtiEnv*` 的 API；但 `jvmti.h:708` `can_retransform_any_class` 可在对方 transform 后强制重转换 |

**总体：三件套里第 ② 条完整拿到且是超集，第 ③ 条达到等效，第 ① 条拿到一半。**

### 2.1 比 T0 更强的四个方向

| 方向 | 说明 |
|---|---|
| **更早** | T0-① 发生在运行期；A1 的 ModLauncher 服务注册在 **Forge 加载前** |
| **更深** | T0 三件套全在 JVM 层；Ronova 已有 **NTFS 事务 + 文件句柄身份**，在 JVM 层之下 |
| **更广** | `SetNativeMethodPrefixes` 覆盖**多 classloader**，`Instrumentation` 版只有单个 |
| **可验证** | T0 靠隐蔽；本方案靠**可验证的深度**——同样深，但每步留证 |

### 2.2 一条硬边界

**JIT 产物不能直接改。** JVMTI 只能观察编译事件（`jvmti.h:697`
`can_generate_compiled_method_load_events`）并通过 retransform 触发去优化，
**没有改写已编译代码的 API**。越过它只能走原生 inline hook（Hook `jvm.dll` 导出）——
纯原生、非官方、跨 JDK 脆弱，**本方案不采纳**。

---

## 3. 加强块（按实现顺序）

### A 块 —— 前置与入口

**目的**：抢到 Forge 与 Mixin 之前；补上删除 `AutoAttach` 后的入口能力。

| ID | 项 | 依据 | 备注 |
|---|---|---|---|
| **A1** | 注册 4 个 ModLauncher 服务：`ILaunchHandlerService` / `ITransformationService` / `ILaunchPluginService` / `ITransformerDiscoveryService` | 永爱 2.6 `META-INF/services/` 原文（四个全有）；永爱 3.2 只有后两个 | `ILaunchHandlerService` 最早；MS `skills:395` 明确推荐 `ITransformationService` |
| **A2** | ServiceLoader 返回顺序重排 | 永爱 2.6 `ServiceLoaderPriorityAgent:190-268` | **限制**：只调整自有条目的相对序，不隐藏、不伪造任何第三方条目；每次重排记日志 |
| **A3** | JNI `DefineClass`(loader=null) 替代"解压 jar + `AddToBootstrapClassLoaderSearch`" | 现状：`NativeBridge.java:45` + `ronova_native.cpp:903` | **消掉一次磁盘写入**（W1 更干净），拿到同样的引导层可见性 |
| **A4** | JDK SPI 注册（按需、最小） | 永爱 2.6 注册了 `java.nio.file.spi.FileSystemProvider` 与 `java.util.function.Consumer` | 需评估必要性；不照搬通用接口 |

**为什么 A 块先做**：它是被删 `AutoAttach` 的替代，且**不依赖任何降级条件**（比 AutoAttach 更强——后者只在 DLL 失败时才跑）。

### B 块 —— 原生能力扩展

**目的**：把 JVMTI 能力位从 4 项扩到 10 项。**这是整份方案性价比最高的一块**——纯增量。

**现状**（`ronova_native.cpp:817-823`，申请 4 项）：
`can_retransform_classes` / `can_retransform_any_class` / `can_generate_all_class_hook_events` / `can_access_local_variables`

**待加**（全部经 JDK 17 `jvmti.h` 核实存在）：

| ID | 能力位 | 行 | 用途 | 打开的能力 |
|---|---|---|---|---|
| **B1** | `can_set_native_method_prefix` | `:706` | `SetNativeMethodPrefix`（`:1417`）/ `SetNativeMethodPrefixes`（`:1421`） | **T0-② 完整形态**：原生方法层接管，覆盖多 classloader。直接命中 `intercept.jni_native` |
| **B2** | `can_get_bytecodes` | `:673` | `GetBytecodes`（`:1426`） | **不变量级证明的基础**（F1） |
| **B3** | `can_get_constant_pool` | `:705` | 校验常量池引用目标 | `attack.dispatch`、`intercept.unsafe_reflection` |
| **B4** | `can_generate_compiled_method_load_events` | `:697` | JIT 编译/回退事件 | **关闭 `attack.dispatch` 的 JIT 缺口** |
| **B5** | `can_tag_objects` | `:670` | 跨事件关联同一对象，不依赖弱引用 | `protect.entity_record`、E1 候选节点标识 |
| **B6** | `can_get_source_file_name` / `can_get_line_numbers` | `:681` / `:682` | 诊断归因 | `runtime.anomalous_call_log` |

**B7（已有，启用其未用部分）**：`can_retransform_any_class`（`:708`）已申请——
用它在**任何外部 transform 之后强制重转换**，即 T0-③ 的等效形态。

> **不得申请的**：`can_generate_native_method_bindings` —— **JDK 17 的 `jvmti.h` 中不存在**
> （JDK 8 有，9 已移除）。此前清单误列，此处更正。

**合规**：JVMTI 是官方 API（W8 不触碰）；不新增外部进程（W4）、不要求参数（W5）、不改文件（W1）。
**须控制**：新增事件回调一律走有界队列，不在回调内做重活（`skills:408` 警告 `ClassFileLoadHook` 中避免触发大量类加载）。

### C 块 —— 合规整改（减法，先做）

| ID | 项 | 依据 | 说明 |
|---|---|---|---|
| **C1** | 删除 `AutoAttach` + `AttachHelper` | `AutoAttach.java:26-33`、`AttachHelper.java:13-21` | **主路径无需替代**——`ronova.dll` 已在 `JNI_OnLoad` 自取 JVMTI（`ronova_native.cpp:780-786`），与 himitsu 同机制且合规。`ImmediateWindowProvider`（Forge 47.4.11+）为**可选增强**，非完成条件 |
| **C1b** | 降级上报 | `skills`/`msitem:322`「**不能静默假装能力仍完整**」 | Native 加载失败时 `/ronova status` 显式显示"JVMTI/Agent 层不可用"。这是**真实能力降级**（只剩 Mixin），必须如实呈现 |
| **C2** | `native_root.dll` 定位裁决 | MS `skills:358`「Native 不能成为业务规则的第二份实现」 | 二选一：(a) 收窄为纯验证后端；(b) 保留并明文声明为有意设计 + 记录边界。**需用户裁决** |
| **C3** | 独占列表过滤开关化 | `entitydefence:40` 与 `:236` **两处**强调；`:228-234` 列 5 条后果 | `PresenceSplitFlags` 加独立开关；关闭时走"完整列表 + 自我修复"；文档记录 5 条后果 |
| **C4** | native 常量单一来源 | `skills:411` | DLL 名 / 资源路径 / 拷贝路径由构建任务从单一来源生成并校验 |
| **C5** | 单一分类入口 | `skills:228` | 新增单入口门面，三个 `QuarantineView` 都经它（保留 Raw/Presence 分离） |
| **C6** | 规范实例 5 条规则 | `entitydefence:62-68` | 与 `SubjectKey` / `bindingRevision` / `PhysicalIncarnationId` 对齐；明确 Ronova "新 incarnation 代表同一身份"与 MS 规则的调和 |
| **C7** | 注册表读写 hook 作为修复触发点 | `entitydefence:126-127` | 关闭"被移除后、下次修复前"的读侧时间窗 |
| **C9** | 目标分类取向 | `msitem:212-218` 第 5 条：「未分类的新生物、Boss 或多部位对象：**保守地 `IGNORE`，直到完成兼容性测试**」 | Ronova 默认裁决一切合资格目标，**与 MS 取向相反**。需裁决：改默认，或明文声明取向差异 |
| **C10** | 七字段注入契约 | `msitem:313-324` | 31 个 mixin 各填：目标 / 位置 / 逻辑侧 / 条件 / 顺序 / **失败策略** / 验证 |
| **C11** | 策略集中 | `msitem:205-210`、`:146-156`、`:163-183`、`:220` | 补 `ItemPolicy`；`ActionContext` 加 `budget` 与 `candidates` 快照；补扇形选择器；多部位"转根 / 只作用部位 / 拒绝"三选一 |

### D 块 —— 执行器补齐（能力本体）

| ID | 项 | 现状 | 依据 |
|---|---|---|---|
| **D1** | 12 个普通动作的执行器 | 14 个动作**只有 1 个接线**（`DominionCommands.java:280-281` 硬编码） | 项目自身 |
| **D2** | `CompletionTracker` 补 6 项生产者 | 9 项条件中 6 项**零引用**（`WORLD_RECORDS` / `OLD_EPOCH_REJECTED` / `RECOVERY_TASKS` / `REBUILD_ENTRYPOINTS` / `TRACKING` / `SERVER_PRESENCE`） | `skills:139-140` 九项条件；V2.6:352-357 |
| **D3** | `TerminalLifecycle` 服务器分支 | 整体不可达（`TerminalLifecycle.java:49-62` 每条路径都 return） | 项目自身 |

### E 块 —— 图与证书（极限档本体）

依据 V2.6:90——"完整 ResultGraph、MutationTrace、ExistenceTerminationCertificate
生产 schema 与 executor **从 7.17 起实现**"。

| ID | 项 | 关键约束 |
|---|---|---|
| **E1** | 候选一等公民 + 置信度模型 | 复用 `obligation/CandidateEvidence`（8 family / 3 strong）与 `CandidateRestrictions`；提升须独立证据 |
| **E2** | `MutationTrace` | 五段管线（V2.6:1158-1164）；**不是 Authority**、不直接创造敌对关系、**不得凭字符串/包名/同父类判归属** |
| **E3** | `ResultGraph` | **不能创造 Judgment / 不能单独撤销 Tombstone / 不能自己推进 TargetEpoch / 重启后按 Root Truth 重建**（V2.6:347-350）；容量**语义零截断**（V2.6:404-415） |
| **E4** | `ExistenceTerminationCertificate` | **不能成为 `denySpawn()` 的 Authority**；失效不自动回滚已提交 Judgment（V2.6:352-357） |

### F 块 —— 证明与策略收口

| ID | 项 | 依据 |
|---|---|---|
| **F1** | `AttestationManifest`：**语义不变量清单（非整类 hash）** | L6:262；基础是 B2（`GetBytecodes`） |
| **F2** | `LoaderProofRegistry`：多 ClassLoader **all-copies** 证明 | L6:263、L6:1092 |
| **F3** | `EnforcementLease`：proof → `ACTIVE` / `PROVISIONAL` / `FENCED`，**经 `RootCommandBus` 生效，不直写 store** | L6:264、L6:346 |
| **F4** | 逐键矩阵 revision 2 | **D91**（"三个正式预设从 7.17 起逐键解析"）+ V2.6:776（统一映射"不是最终矩阵"） |
| **F5** | 天花板管道 | 现状：`withCapability` 只有 2 个硬编码调用点；48/54 键 `supportedCeiling = OFF` |
| **F6** | 两套权限路径合一 | `registered*` 用预设 `requestedLevel`，`resolve()` 用 `effectiveLevel` |

### G 块 —— 原生强制

| ID | 项 | 说明 |
|---|---|---|
| **G1** | `WriteSink` / lease 下沉到原生 | 现状每次强制都要回调 Java（`ronova_native.cpp:135` `CallBooleanMethod(lease, valid, plan)`）。规范自承"完整 Native lease / WriteSink 尚未实现" |

**与 C2 的区别**：C2 裁的是 `native_root.dll`（**判定**内核，是业务规则）；G1 是**执行后端**（校验 + 提交）。
**C2 若选 (a) 收窄，G1 正好接上——两者合起来才让原生层回到"小型受约束执行后端"的定位。**

### H 块 —— 新维度（样本与文档补读所得）

| ID | 项 | 依据 | 说明 |
|---|---|---|---|
| **H1** | COLD 休眠窗口的事件驱动唤醒 | L6 §6.3：COLD 每 100+ tick 低频确认 | **退避即攻击窗口**；用类加载/注册/保存事件唤醒，而非等轮询。Native 已有 `ClassPrepare` 触发刷新的先例 |
| **H2** | 候选 flood 的配额分配 | MS 验收 **X41**「轮换身份抢配额」、**X42** 均 NOT_RUN；`skills:413-415` | 按**证据强度**分配配额，保证高置信候选的最低配额不被伪候选挤占 |
| **H3** | 与另一个合规 MS Mod 共存 | **MS 文档未覆盖**（默认只有一方） | 现状对外部 transformer 的姿态是"遏制"；需区分"改写自有类"（威胁）与"改写其自身类"（无关）。按**被改写类的归属**判定 → W2 不触碰 |
| **H4** | 按需重建 incarnation | `entitydefence:62-68` 第 4 条：「有意替换必须走**显式生命周期**」 | 现有 `PhysicalIncarnationId` + `bindingRevision` + `SUPPRESS_REBUILD` 是基础，但无"重建"实现 |

---

## 4. 明确不采纳

| 不采纳 | 样本在用 | 理由 |
|---|---|---|
| 改写既有 JDK 类（`InstrumentationImpl` 链、`sun.instrument` 内插桩） | SuperSteve 6.9.13f | **W8**；破坏可验证性——283 类封印证明不了"我没动过别人的类" |
| 借 `Unsafe` / `ReflectionFactory` / 私有字段取得包访问 | SuperSteve 6.9.13f | W8；且 JDK 版本脆弱 |
| 手搓 `\\.\pipe\javatool<N>` 直连 attach listener | 永爱 2.6 `:886` | 依赖 JDK 内部；A 块已提供合规前置层 |
| 起外部 helper 进程 | Ronova `AutoAttach` | **W4** |
| 运行时解密载荷后 `DefineClass` | himitsu（`BCryptDecrypt` + `DefineClass`） | 破坏可审计性，与封印/证据路线相反 |
| 载荷伪装（DLL 改名 `.secret`、jar 加密） | himitsu（`himitsu.secret` 为 PE、`himitsu.jar` 熵 8.000） | 与"公开可验证"取向相反 |
| 改写他人 JAR / 文件 | ArcaneVorte `JarPatcher` | **W1、W3** |
| 按名称/包名判定目标 | — | **W2** |
| 全局全返回改写 JDK / Forge / LWJGL / 核心类 | — | **W6** |
| 原生 inline hook `jvm.dll` 导出（改 JIT 产物） | — | §2.2 硬边界；跨 JDK 极脆弱 |
| 靠资源耗尽取胜（OOM / 无限线程 / 无限 retransform） | — | **W7**；V2.6:431-445 |

**共同点**：这些手段能提升短期对抗强度，但都会**降低 Ronova 最核心的资产——可验证性与可审计性**。
Ronova 的差异化优势是 283 类封印 + 证据链 + Root 单一 writer；**用隐蔽换强度是拿长板换短板。**

---

## 5. 实施顺序与依赖

```
C 块（减法，先做）
  └─ C1/C1b 删 AutoAttach + 降级上报
  └─ C2 native_root 定位（需裁决）
        │
        ▼
A 块（前置）──► B 块（原生能力）
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
   D 块（执行器）           F 块（证明，F1 依赖 B2）
        │                       │
        └───────────┬───────────┘
                    ▼
              E 块（图与证书）
                    │
                    ▼
              G 块（原生强制）
                    │
                    ▼
              H 块（新维度）
```

**建议顺序**：`C1 → A → B → D → F → E → G → H`，其中 **C2 需先裁决**。

理由：
1. **C1 最先**——合规前提，且无依赖；
2. **A 块次之**——它是被删能力的替代，不做会留能力真空；
3. **B 块第三**——纯增量、成本极低、且 F1 依赖 B2；**B1 是本方案性价比最高的一项**；
4. **D 块**——能力本体，12 个执行器是最大工作量；
5. **F → E**——证明形式先于图（E4 的判据依赖图，F1 的基础是 B2，但 F 的其余项独立）；
6. **G、H 最后**——工程量最大，且 H 触及核心状态机。

---

## 6. 验证：全部后置

**本方案不包含验证项，也不建议在任何实现块完成前启动验证。**

依据（项目自身口径）：

- `docs/CURRENT-PROGRESS.md:60`：「**只在实现、失败原因或验收范围变化后重复相关验证。**」
- `docs/PHASE1-IMPLEMENTATION-STATE.md:112`：「完成四块后集中完整构建、旧回归、故障/重启/压力和稳定完成验收。」
- 收拢自 `Ronova_7.17_Current_Execution_Plan_V1.1`：「不以更多文档检查制造游戏 PASS。」

**以下三项此前被误列为"立即"，现全部移到最后：**

| 项 | 为什么后置 |
|---|---|
| **肉眼验收清单**（血量 0 / 死亡界面 / `say @e` / `forge entity list` / F3 E） | 12 个执行器未接线时测不出结果 |
| **对抗性验证夹具**（按样本手法各写一个） | 覆盖度取决于 E 块是否建成；现在测的是空气 |
| **合规自证**（静态审计"我没违规"） | 应在 W1–W8 的实现全部落地后做一次性审计 |

**启动验证的门槛**：D 块（执行器）与 E 块（图与证书）接线完成。

---

## 7. 完成判定

本方案**不定义新的"加强已完成"结论**。各块的完成判据：

| 块 | 判据 |
|---|---|
| C | W1–W8 全部可判定成立；`/ronova status` 如实显示各层可用性；独占过滤关闭时走完整列表 |
| A | 无参数无外部下前置注册生效；ServiceLoader 重排**未减少**任何第三方条目；A3 消除磁盘写入 |
| B | 未申请到的能力位如实显示不可用；新增事件不超 tick 预算；B7 能在外部 transform 后强制重转换 |
| D | 12 个动作各有执行器；`CompletionTracker` 9 项各有生产者；`TerminalLifecycle` 服务器分支可达 |
| E | 同目标在 ENHANCED 与 EXTREME 下实际纳入集合不同且逐条有证据 |
| F | 不变量在**实际生效字节码**上校验；lease `FENCED` 只封新 mutation，不撤既有保护 |
| G | 原生持有 lease 且强制**不回调 Java**；判定逻辑仍在 Java |
| H | 四项各自有契约与可复核行为记录 |

**任何一块未达成，不得据此宣称加强已完成。**
本方案不修改 `docs/CURRENT-PROGRESS.md` 的既有结论：完整 7.17、FULL L5 / L6 / L6E 仍未宣告完成。

---

*本方案为设计文档，未修改工作区任何既有文件。*
