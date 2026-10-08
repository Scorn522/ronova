# Ronova 合规极限加强方案 V1.0

> **已取代（2026-09-12）**：本稿的红线（V1–V8）与分层已并入
> [`docs/DESIGN-SPEC.md`](../DESIGN-SPEC.md) 第二部分 §15 的三层规则源（L0 铁律 / P 项目自有规范 / UX1）。
> **本文件保留作历史，不再作为加强入口。**

日期：2026-09-11。性质：**设计文档**，未修改任何生产代码。

## 0. 本方案的判断

**核心判断：合规空间远大于当前实现所占据的部分。**

依据五个真实样本的静态审查（[机制对照](../research/reference-priority-mechanisms.md)）：

| 样本手法 | 是否合规 | 验证 |
|---|---|---|
| 进程内自 attach（自开 `allowAttachSelf`） | ✅ | 永爱 2.6 `JavaAgentUtil:547-626` |
| 手搓 `\\.\pipe\javatool<N>` 直连 attach listener | ✅ | 永爱 2.6 `:837-920` |
| **自取 JVMTI（加载 `jvm.dll` → `GetEnv` → `SetEventCallbacks`）** | ✅ | himitsu PE 字符串 |
| 四路 ModLauncher 服务注册 | ✅ | 永爱 2.6 `META-INF/services/` |
| JDK 自带 SPI 注册（`FileSystemProvider`、`Consumer`） | ✅ | 永爱 2.6 同上 |
| ServiceLoader 返回顺序重排 | ✅ | 永爱 2.6 `ServiceLoaderPriorityAgent:190-268` |
| 定义类进 `sun.instrument` 包 | ⚠️ 不违规但触碰 JDK 内部 | SuperSteve 6.9.13f（推断） |
| 直接 patch JAR 文件 | ❌ **违规** | ArcaneVortge `JarPatcher` |
| 起外部 helper 进程 | ❌ **违反部署约束** | Ronova `AutoAttach` |

**违规的只有三件事：改文件、针对、外部进程。** 其余全部在合规空间内。
⇒ **加强的路径不是"更隐蔽"，而是"更靠前、更深、更可证"。**

### 0.1 合规红线（本方案全程遵守）

| # | 红线 | 出处 |
|---|---|---|
| V1 | 不修改任何文件（含依赖文件、Mod 文件、Forge 文件、游戏核心文件） | `skills:62`、`:398` |
| V2 | 不针对：不按第三方的包名/类名/文件/私有实现判定 | `skills:63`、`msitem:41` |
| V3 | 不删对方 Mod 文件 | `skills:64` |
| V4 | **无外部**：不创建外部进程 | 用户部署约束 |
| V5 | **无参数**：不要求任何启动参数（`-javaagent` / `-agentlib` / `-D`） | `skills:396` |
| V6 | 不对 JDK/Forge/LWJGL/游戏核心类做全局全返回改写 | `skills:348` |
| V7 | 不破坏宿主稳定性、Authority 正确性、用户数据安全、JVM 可持续运行 | V2.6:449 |
| V8 | **自律项**：不触碰 JDK 内部实现（`sun.*` 定义、`InstrumentationImpl` 改写、私有字段反射） | 本方案 §6.2 说明 |

V8 是**自律**而非外部要求：样本普遍触碰它，但 Ronova 有官方路径达到同等效果（§1），
且触碰 JDK 内部会破坏 Ronova 最核心的资产——**可验证性**。

---

## 1. L1 前置层：抢到 Forge 与 Mixin 之前（替代被删的 AutoAttach，且更强）

### 1.1 为什么这一层最划算

`AutoAttach` 被移除后，Ronova 的早期能力只剩 `ronova.dll` 的 JVMTI。
而 DLL 由 mod 类初始化时加载（`RonovaDominion.java:24-25`），**已经晚于 ModLauncher 与部分 Mixin**。

永爱 2.6 证明：**ModLauncher 服务注册 + JDK SPI 注册是纯进程内、零参数、零外部**的前置手段。
MS 文档亦明确推荐 `ITransformationService`（`skills:395`）。

**这一层比 AutoAttach 更强**：AutoAttach 只在 DLL 失败时跑且依赖 attach；
L1 是**常驻前置**，不依赖任何降级条件。

### 1.2 交付物

| 交付物 | 内容 | 依据 |
|---|---|---|
| `ILaunchHandlerService` 注册 | 拿到 ModLauncher 最早期入口 | 永爱 2.6 `META-INF/services/` 原文 |
| `ITransformationService` 注册 | 类转换服务；`skills:395` 明确推荐 | 同上 + 永爱 3.2 |
| `ILaunchPluginService` 注册 | 在 Forge 之前进入转换链 | 永爱 2.6、3.2 均有 |
| `ITransformerDiscoveryService` 注册 | 更早发现自有转换器 | 永爱 2.6 |
| **ServiceLoader 优先级重排** | 重排 `getResources` 返回顺序使自身实现优先 | 永爱 2.6 `ServiceLoaderPriorityAgent:190-268` |
| JDK SPI 注册（**按需、最小**） | 经评估后选择性使用；不照搬 `Consumer` 这类通用接口 | 永爱 2.6 注册了 `java.util.function.Consumer` |

### 1.3 合规性论证

| 红线 | 是否触碰 | 说明 |
|---|---|---|
| V1 不改文件 | ❌ 不触碰 | 只新增自有 JAR 内的 `META-INF/services/` 条目 |
| V2 不针对 | ❌ 不触碰 | 注册的是 ModLauncher/JDK 的**公开服务接口**，不含任何第三方名称 |
| V3 不删文件 | ❌ 不触碰 | — |
| V4 无外部 | ❌ 不触碰 | 全进程内 |
| V5 无参数 | ❌ 不触碰 | ModLauncher 通过 ServiceLoader 发现，**不需要任何启动参数** |
| V6 不全返回改写 | ❌ 不触碰 | 只注册服务，不改写 JDK/Forge 类 |
| V7 不破坏宿主 | ⚠️ 需控制 | ServiceLoader 重排必须**有界、只影响自有条目序**，不得过滤或伪造他人的服务条目 |
| V8 不碰 JDK 内部 | ❌ 不触碰 | 全部走公开 SPI |

**唯一风险点**：ServiceLoader 重排若过度使用，会改变他人服务的发现顺序（等于干扰第三方）。
**限制**：只调整**自有条目**在枚举中的位置，不改动、不隐藏、不伪造任何第三方条目；
并对每次重排记录日志（`skills:41` 的"可审计"要求）。

### 1.4 验收

- 无任何启动参数下，Ronova 在 Forge 初始化前完成前置注册；
- ModLauncher 服务被实际发现（日志可证）；
- ServiceLoader 重排**未减少**任何第三方条目的数量（逐条比对）；
- 关闭该层后 mod 仍可运行（降级可用而非崩溃）。

---

## 2. L2 原生深化：扩充 JVMTI 能力位，覆盖 JIT 与字节码层

### 2.1 现状与缺口

Ronova 当前申请的能力（`ronova_native.cpp:817-823`）只有 4 项：

```
can_retransform_classes
can_retransform_any_class
can_generate_all_class_hook_events
can_access_local_variables
```

由此产生的已登记缺口：`ATTACK_DISPATCH` 的极限要求是"覆盖候选解释/**JIT**/native dispatch 与最终调用目标"，
而**没有 `can_generate_compiled_method_load_events` 就无法观察 JIT 编译产物**。

### 2.2 交付物

| 新增能力位 | 用途 | 关闭的缺口 |
|---|---|---|
| `can_generate_compiled_method_load_events` | 观察 JIT 编译/回退事件，关联实际执行代码 | `attack.dispatch` 的 JIT 部分 |
| `can_get_bytecodes` | 读取方法真实字节码，做**不变量级**校验（而非类哈希） | `AttestationManifest` 的实现基础 |
| `can_get_constant_pool` | 校验常量池引用目标 | `attack.dispatch`、`intercept.unsafe_reflection` |
| `can_tag_objects` | 跨事件关联同一对象（不依赖弱引用） | `protect.entity_record`、候选图节点标识 |
| `can_get_source_file_name` / `can_get_line_numbers` | 诊断归因 | `runtime.anomalous_call_log` |
| `can_generate_native_method_bindings` | 关联原生方法绑定 | `intercept.jni_native` |

### 2.3 为什么这条最"狠"而完全合规

- himitsu 已证明：**自取 JVMTI env 是社区深度上限**（PE 字符串 `GetEnv(JVMTI)` + `SetEventCallbacks`），
  而 Ronova **已经在做同一件事**（`ronova_native.cpp:780-786`）；
- JVMTI 是**官方接口**，不是内部实现；
- 不需外部进程、不需启动参数、不改任何文件；
- 能力位是**增量申请**，请求不到就如实降级（现有 `capabilityMask` 机制已支持）。

### 2.4 合规性论证

| 红线 | 是否触碰 |
|---|---|
| V1–V5 | ❌ 全部不触碰 |
| V6 不全返回改写 | ❌ 不触碰（只读观察，不写他人字节码） |
| V7 不破坏宿主 | ⚠️ **必须控制采样与事件开销**：新增事件回调一律走有界队列，不在回调内做重活（`skills:408` 明确警告 `ClassFileLoadHook` 中避免触发大量类加载） |
| V8 不碰 JDK 内部 | ❌ 不触碰（JVMTI 是公开 API） |

### 2.5 验收

- 未申请到的能力位在 `/ronova status` 中如实显示为不可用；
- JIT 事件可观察且**不影响 tick 时间预算**（沿用 `FrameBudgetGovernor`）；
- `can_get_bytecodes` 产出的不变量证明可用于 §4 的 `AttestationManifest`。

---

## 3. L3 反制面：把五个样本的手法变成检测项（纯防御）

### 3.1 这一层是"用样本的强度反哺防御"

五个样本给出了**真实存在的、可复现的**攻击手法。Ronova 不需要发明攻击面——只需要**能看见它们**。
这一层**完全是防御**，且不针对任何具名对象（只按机制特征判定，符合 V2）。

### 3.2 检测项清单

| # | 被检手法 | 样本依据 | 检测手段 | 对应能力键 |
|---|---|---|---|---|
| D1 | 自 attach 开洞 | 永爱 2.6 `JavaAgentUtil:592` | 监视 `jdk.attach.allowAttachSelf` 等属性的**变化**；监视 attach listener 管道的创建 | `intercept.instrumentation`、`protect.core_authority` |
| D2 | 手搓 attach 管道 | 永爱 2.6 `:886` `\\.\pipe\javatool<N>` | 命名管道创建观察（原生侧） | `runtime.native_module_watch` |
| D3 | 定义类进 JDK 包 | SuperSteve 6.9.13f（推断 `defineClassInPackage`） | `ClassFileLoadHook` 中识别**非 JDK 来源**却落在 `java.*`/`sun.*`/`jdk.*` 包内的类定义 | `attack.dynamic_code`、`runtime.dynamic_code_watch` |
| D4 | 改写 `InstrumentationImpl` transformer 链 | SuperSteve 6.9.13f | 记录自有 transformer 的链位置；链长度/内容变化即告警 | `intercept.instrumentation` |
| D5 | 二次 `GetEnv(JVMTI)` 抢事件回调 | himitsu PE | `SetEventCallbacks` 后自检回调指针是否被替换 | `runtime.jvmti` |
| D6 | 运行时解密后 `DefineClass` | himitsu（`BCryptDecrypt` + `DefineClass`） | JNI `DefineClass` 调用来源观察；熵突变后的类加载 | `attack.dynamic_code` |
| D7 | ServiceLoader 顺序重排 | 永爱 2.6 `reorderResources` | 观察 `ClassLoader.getResources` 被插桩；比对自有服务条目的相对序 | `intercept.asm`、`protect.call` |
| D8 | 运行时内存编译 | SuperSteve `HotCplr` | 已有 `runtime.dynamic_code_watch` 键，补实际生产者 | `runtime.dynamic_code_watch` |
| D9 | **改写他人 JAR 文件** | ArcaneVorte `JarPatcher` | 自有封印已覆盖自身；**补"文件被外部改写"的观察**（Ronova 作为受害者侧） | `attack.save_reconstruction`、`protect.code_integrity` |
| D10 | 隐藏类/匿名类承载实体逻辑 | SuperSteve、永爱 | 隐藏类定义事件 + loader 血缘解析 | `intercept.classloader`、`attack.target_class_lineage` |

### 3.3 合规性论证

- **纯观察**：检测项只记录与告警，**不主动改写他人**（V6 不触碰）；
- **不针对**（V2）：判定依据是**机制特征**（如"非 JDK 来源的 `sun.*` 包类定义"），
  **不是**包名/类名/MOD ID 白名单。这是与"针对"的分界线，必须在实现中显式保证；
- 不写文件（V1）、不起进程（V4）、不加参数（V5）、不碰 JDK 内部（V8）。

### 3.4 验收

- 每条检测项有**正例**（用一个自建的最小夹具复现该手法）与**负例**（正常加载不告警）；
- 夹具只做机制复现，**不针对任何具体第三方**；
- 误报率有记录；
- 检测项**不产生**任何写操作（可用字节码级检查证明）。

---

## 4. L4 证明强度：从"逐类哈希"升级为"不变量级证明"

### 4.1 现状与规范要求

Ronova 现有最强机制是 **283 类 CodeSeal**：逐类 SHA-256 + `ClassPrepare` 校验 + `build_id` 双向绑定 + 执行金丝雀。

而 L6 规范要求的是**更强**的形式（L6:262）：

> `AttestationManifest [N]` **语义不变量清单（非整类 hash）**

**逐类哈希只证明"字节没变"，不证明"语义仍成立"。** 一个把 `if (x) return A; else return B;` 改成
`return A;` 的补丁会改哈希（能被发现）；但一个**同时改写校验逻辑自身**的补丁，
在哈希层面是自洽的——除非校验逻辑本身也被封印（Ronova 做到了），
但**没有回答"这个类当前的行为是否仍满足它的契约"**。

### 4.2 交付物

| 交付物 | 内容 | 依据 |
|---|---|---|
| `AttestationManifest` | 每类一组**语义不变量**（控制流形状、关键调用目标、返回极性、字段读写方向），经 `can_get_bytecodes` 从**实际生效字节码**提取 | L6:262 + `skills:315-322` 的"精确 descriptor / 返回极性"要求 |
| `LoaderProofRegistry` | 多 ClassLoader **all-copies** 证明：同一逻辑类在所有 loader 副本上的不变量同时成立 | L6:263、L6:1092 |
| `EnforcementLease` 状态机 | proof → `ACTIVE / PROVISIONAL / FENCED`，**经 `RootCommandBus` 生效，不直写 store** | L6:264、L6:346 |
| 与现有 CodeSeal 叠加 | 哈希封印保留（防字节篡改）+ 不变量证明（防语义篡改），**两层独立计量** | — |

### 4.3 合规性论证

全部为**对自有代码的自我校验**：不写他人字节码（V6）、不读他人私有实现（V2）、
不用外部进程（V4）、不加参数（V5）、不碰 JDK 内部（V8）。
`can_get_bytecodes` 是官方 JVMTI 能力（V8 不触碰）。

### 4.4 验收

- 不变量在**实际生效字节码**上校验（不是磁盘 JAR）；
- 人为注入"控制流被改但语义等价"与"语义被改但哈希被同步更新"两类补丁，前者通过、后者失败；
- lease 的 `FENCED` 状态**只封新 mutation，不撤既有保护**（L6:346 原文要求）。

---

## 5. L5 执行深度：候选图 + 跨表面追踪 + 完成证书

### 5.1 现状

`TerminalActionPolicy` 的天花板是 `REGISTERED_ENTITY_ONLY`（单个已登记实体 + 精确索引引用）。
而 V2.6 §4.1 定义的 EXTREME 范围是：

> 当前 Judgment → **所有可能改变其最终结果的同 JVM 候选路径** → 已证明源 / 链 / 代码 /
> dispatch / task / callback / rebuild / publish → 最终完成与状态出口

**点是点，图是图。** 且 V2.6:90 明文"完整 ResultGraph、MutationTrace、
ExistenceTerminationCertificate 生产 schema 与 executor **从 7.17 起实现**"。

### 5.2 交付物

| 交付物 | 内容 | 依据 |
|---|---|---|
| 候选一等公民 | 统一候选登记 + 置信度模型（`CANDIDATE → PROVEN_RELATED`）；复用 `obligation/CandidateEvidence`（8 family / 3 strong）与 `CandidateRestrictions` | V2.6:1170 |
| `MutationTrace` | 五段管线：状态变化 → before/after → 关联 caller/task/callback/manager/dispatch/loader/native → 有界证据 → 供图使用 | V2.6:1158-1171 |
| `ResultGraph` | 候选关系图；分片 + 持久索引 + 冷热分离；**语义零截断**（预算只限每窗口处理量） | V2.6:343-350、:404-415 |
| `ExistenceTerminationCertificate` | **九项条件补齐**（现有 3 项有生产者，6 项零引用：`WORLD_RECORDS`/`OLD_EPOCH_REJECTED`/`RECOVERY_TASKS`/`REBUILD_ENTRYPOINTS`/`TRACKING`/`SERVER_PRESENCE`） | `CompletionTracker` + V2.6:352-357 |

### 5.3 样本反哺：必须能击败的具体恢复路径

SuperSteve 6.9.1/6.9.13f 与永爱 2.6 揭示了**真实的恢复链形态**，图必须能承载它们：

| 恢复路径 | 样本依据 | 图要求 |
|---|---|---|
| 静态并发表按 UUID 保留服务端/客户端实例 | SuperSteve `api/SSUtil.java:217-218` | 候选节点跨端绑定 |
| 两条裸线程每 ~10ms 重复维护 | `power/SSThread.java:34-114` | 任务域候选 + 共享线程不整体停 |
| 复制 section/UUID/ID/active/tracking 后**替换字段** | `api/SSUtil.java:398-450` | 容器级候选 + 迟到发布检测 |
| 同 UUID 新实例进入 → 重挂旧实例、移除新实例 | `entity/SuperSteveEntity.java:583-613` | lineage 节点 + 旧指针复用检测 |
| 隐藏类反射构造实体 | `init/SSModEntities.java:43-56` | loader/定义类血缘边 |
| 内存 Java 编译再定义隐藏类 | `power/HotCplr.java:55-81` | 动态代码节点 |
| 对象类指针改写 + 无构造分配 + 清反射缓存 | `api/PLZBase.java:129-146` | 字段级候选 |
| Native 解密后 `DefineClass` | himitsu PE | 原生来源边 |

### 5.4 合规性论证

`ResultGraph` 与 `CandidateRestrictions` 只作用于**与当前 Judgment 有实际关系**的路径，
判定依据是**证据**（复制来源、调用栈帧、任务关系），不是名称（V2 不触碰）。
规范亦明确其边界：**不能创造 Judgment、不能单独撤销 Tombstone、不能自己推进 TargetEpoch**
（V2.6:347-349）——即它不扩大 Authority，只扩大**可证范围**。

### 5.5 验收

同目标在 ENHANCED 与 EXTREME 下**实际纳入的对象集合不同**，差异可枚举；
每一份纳入都有独立证据；九项条件各有可复核生产者。

---

## 6. L6 原生强制：让强制不再回调 Java

### 6.1 现状

原生层的角色是**验证者 + 提交者 + 决策副本**，但**每一次强制判定都要回调 Java**：
事务上下文里是 `env->CallBooleanMethod(context->lease, context->valid, context->plan)`
（`ronova_native.cpp:135`）。

规范自承："完整 Native lease / WriteSink 尚未实现的部分继续作为明确工程项。"

### 6.2 交付物

| 交付物 | 内容 |
|---|---|
| 原生侧 `WriteSink` | 最终写入口在原生持有并校验 lease；**不回调 Java** |
| lease 状态原生持有 | `ACTIVE / PROVISIONAL / FENCED` 由原生维护，Java 只读 |
| 与 `RootCommandBus` 的边界 | Java 侧仍在 Root 提交入口查询 lease（L6:455），原生不直写 store |

### 6.3 合规性论证

不新增外部进程（V4，仍在同一进程内）、不加参数（V5）、不写他人文件（V1）。
**但须注意 §6.2 的张力**：MS `skills:358` 要求"Native 不能成为业务规则的第二份实现"。
本项的定位是**执行后端**（校验 + 提交），**不是业务规则**——判定逻辑仍全部在 Java。
这与现状（`native_root.cpp` 447 行四域判定）相反：**现状才是违规的那一侧。**

---

## 7. 明确不采纳（尽管样本在用）

| 不采纳 | 样本依据 | 理由 |
|---|---|---|
| 定义类进 `sun.instrument` 等 JDK 包 | SuperSteve 6.9.13f | V8 自律；破坏可验证性；跨 JDK 版本脆弱 |
| 反射改 `InstrumentationImpl` / transformer 链 | SuperSteve 6.9.13f | 同上 |
| 手搓 `\\.\pipe\javatool<N>` 直连 attach listener | 永爱 2.6 `:886` | 依赖 JDK 内部实现；且属于 attach 机制，本方案已有合规前置层（L1） |
| 起外部 helper 进程 | Ronova `AutoAttach` | V4 |
| 运行时解密载荷后 `DefineClass` | himitsu | 破坏可审计性；与 Ronova 的封印/证据路线相反 |
| 载荷伪装（DLL 改名、jar 加密） | himitsu | 与"公开可验证"取向相反 |
| 改写他人 JAR / 文件 | ArcaneVortge `JarPatcher` | V1、V3 |
| 按名称/包名判定目标 | — | V2 |
| 全局全返回改写 JDK/Forge/LWJGL/核心类 | — | V6 |

**共同点**：这些手段能提升短期对抗强度，但**都会降低 Ronova 最核心的资产——可验证性与可审计性**。
Ronova 的差异化优势是 283 类封印 + 证据链 + Root 单一 writer；用隐藏换强度是**拿长板换短板**。

---

## 8. 阶段与依赖

```
L1 前置层（替代 AutoAttach，常驻前置）
      │
      ├──► L2 原生深化（JVMTI 能力位）──► L3 反制面（检测项依赖 L2 的观察能力）
      │                                          │
      │                                          ▼
      └──► L4 证明强度（不变量证明依赖 can_get_bytecodes）
                                                 │
                                                 ▼
                                        L5 执行深度（候选图 / 追踪 / 证书）
                                                 │
                                                 ▼
                                        L6 原生强制（WriteSink / lease）
```

**建议顺序**：`L1 → L2 → L4 → L3 → L5 → L6`

理由：
- **L1 最先**——它是被删掉的 AutoAttach 的**替代**，不做则合规整改会留下能力真空；
- **L2 次之**——它是 L3/L4 的观察基础（没有 `can_get_bytecodes` 就没有不变量证明，
  没有 JIT 事件就看不全动态代码）；
- **L4 先于 L3**——证明强度是 Ronova 的既有长板，先加固长板；
- **L3 再次**——反制面需要 L2 的观察能力；
- **L5 与 L6 最后**——工程量最大，且 L5 的候选图需要 L4 的证据形式支撑。

---

## 9. 与既有三份规划的关系

| 文件 | 关系 |
|---|---|
| `Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md` | 本方案的 **M0 是 L1 的前置**（先删 AutoAttach，再建 L1 替代）；本方案 = M1–M6 + 额外三层 |
| `Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md` | 本方案的 **L5 与其实第二部（Q0–Q5）同源**；L4 与其 P0/P1（天花板管道）衔接 |
| `docs/research/ronova-vs-ms-comparison.md` | 本方案的合规判据来源 |

**不新增第四套编号的口径**：本方案的 `L1–L6` 与 `V1–V8` 是**追溯标签**，
不是新的能力键、动作、阶段或验收编号；落地时应并入既有 `M*` / `Q*` / `CF*` 体系。

---

## 10. 完成判定

| 层 | 判据 |
|---|---|
| L1 | 无参数无外部下前置注册生效；ServiceLoader 重排未减少任何第三方条目；关闭后仍可运行 |
| L2 | 新增能力位按实测可得性如实显示；JIT 事件可观察且不超预算；`can_get_bytecodes` 可用于不变量证明 |
| L3 | 10 项检测各有正负例；判定依据为机制特征而非名称；检测项零写操作 |
| L4 | 不变量在**实际生效字节码**上校验；等价改写通过、语义改写失败；lease `FENCED` 只封新 mutation |
| L5 | ENHANCED 与 EXTREME 实际纳入集合不同且逐条有证据；九项条件各有生产者 |
| L6 | 原生持有 lease 且强制不回调 Java；判定逻辑仍在 Java |

**任何一层未达成，不得据此宣称"合规极限"已实现。**
本方案不修改 `docs/CURRENT-PROGRESS.md` 的既有结论，不新增 PASS 声明。

---

*本方案为设计文档，未修改工作区任何既有文件。*
