> **fix5 来源保留页（2026-09-14）：已融合；不再作为第二套实施入口。** 当前实施入口是[能力接线规划的原生申请章节](R66-CAPABILITY-WIRING-PLAN.md#native-negotiation)，逐节处置见[融合映射](RESTRUCTURE-MAP.md#fix5-addendum-merge)。
>
> 下方是收到的原文，保留其当时的结论、编号与“待确认”状态；它们不冒充本轮重新执行的结果。`can_retransform_any_class` 字面计数的推论收窄及 `AddCapabilities` 的外部核对见当前入口。**V1.1 的旧机制误读不恢复效力，V1.2 的重复部分不再另算一批工作。** 本页原文之外的横幅才是融合状态说明。

<a id="handoff-original"></a>
<!-- RONOVA-HANDOFF-ORIGINAL-BEGIN ADD12 -->
# Ronova 加强方案 · 增补 V1.2

日期：2026-09-14。性质：**设计增补（修正版）**。未修改任何生产代码，未构建，未启动游戏。

**本版取代 [`Ronova_Enhancement_Addendum_V1.1.md`](Ronova_Enhancement_Addendum_V1.1.md)。**
V1.1 有五处误读，本版逐条修正，并补入本轮新核实到的事实。

修正依据：外部核对 `R66-CAPABILITY-AUDIT.md`（对照 r66 源码、fix3 规划、JDK 17 规范与
OpenJDK 17u 实现）。**本文对其提出的争议点逐条独立复核，结论标注在下。**

**边界**：三条铁律（不改依赖/Mod/Forge 文件、不针对、不删对方文件）+ 不起外部进程。
**证据口径**：`jvmti.h` 行号取自本机 JDK 17 头文件
`D:\桌面\MOD\.build-tools\jdk17-headers\jvmti.h`（逐行实读）；源码行号取自工作区当前源码。

---

## 0. 本版修正摘要

| # | V1.1 的说法 | 实际 | 复核结论 |
|---|---|---|---|
| 1 | `SetNativeMethodPrefixes` **按 classloader** 分别设前缀 | **不是**。单 environment 内 N 个前缀 | 外部核对正确，已核实 |
| 2 | `NativeMethodBind` 是**观测**能力 | **不只是**。回调带 `void** new_address_ptr`，可重定向绑定 | 外部核对正确，已核实 |
| 3 | `DynamicCodeGenerated` 是"JIT 之外一切代码生成"的观测点 | **夸大**。它报的是 JVM 自身生成的原生代码段 | 外部核对正确，**接受收窄** |
| 4 | 48 个键无生产者 ⇒ 对应功能不可达 | **过宽**。局部执行通道存在，且**故意不发布**为全局 ceiling | 外部核对正确，已核实 |
| 5 | 14 个动作只接线 1 个 | 该数字来自**硬编码文案**；`STRONG_ATTACK` 是聚合控制 | 外部核对正确，已核实 |
| 6 | `TerminalLifecycle` 服务器分支整体不可达 | **`:56` 委派可达**；我把"守卫"读成了"不可达" | 外部核对正确，已核实 |
| 7 | "招数没接上电" | 应改为：**通用能力发布不完整、局部执行已存在、诊断落后于部分实现** | 接受 |

**另有 4 项是双方都没提的、我在复核中发现的** —— 见 §2。

---

## 1. 逐条修正与复核

### 1.1 `SetNativeMethodPrefixes` 不是"按 classloader"

V1.1 写的是「复数，按 classloader 分别设前缀」。**错。**

```
jvmti.h:1421   jvmtiError (JNICALL *SetNativeMethodPrefixes) (jvmtiEnv* env,
jvmti.h:1422     jint prefix_count,
jvmti.h:1423     char** prefixes);
```

签名是 **`(env, prefix_count, prefixes)`** —— **一个 environment 内的一组前缀**，没有 classloader 参数。

**同时接受更重要的机制收窄**：它解决的是
「类被改造成 **Java 包装方法 + 改名后的 native 方法** 后，仍能绑定原原生实现」，
**不是**"设个前缀，JVM 就执行你凭空提供的任意 Java 实现"。包装层本身仍需类转换建立。

**由此产生的实际限制**：包装通常要增加方法或改变方法形态，而 **JDK 17 的标准重转换不允许
随意增加、删除或改名方法** ⇒ **已加载的 LWJGL 类不能仅靠这个能力位变成所设想的包装结构。**

**对 V1.1 新 4 的处置**：`skills:352-356` 与本项目 B 块里的 **B1 就是这条 prefix 路线**。
新 4 **不是独立加强项**，应并入 B1 作为"具体应用与限制"。

⇒ 修正后定位：**"可能避免新增专用渲染 Hook DLL" 属待验证方案**；
"已有低成本路线"**不能下结论**。

### 1.2 `NativeMethodBind` 不只是观测

V1.1 把它定位成"观测能力"。**不完整。**

```
jvmti.h:875   typedef void (JNICALL *jvmtiEventNativeMethodBind)
jvmti.h:876       (jvmtiEnv *jvmti_env,
jvmti.h:877        JNIEnv* jni_env,
jvmti.h:878        jthread thread,
jvmti.h:879        jmethodID method,
jvmti.h:880        void* address,
jvmti.h:881        void** new_address_ptr);          ← 可写：重定向绑定
```

**`new_address_ptr` 是出参**，回调可以改写它把绑定指向代理函数。所以这项能力
**观测 + 控制**兼备。

**但必须接受两条收窄**（外部核对提出，我认同）：

1. 它观察的是**Java native 方法与实现地址的绑定事件**，不是每次原生调用，
   更不是任意 C/C++ 函数执行；
2. **不能默认补齐事件启用前已发生的绑定**。

⇒ 修正后定位：

> 先补原生绑定证据，再为**能确认签名、归属和生命周期**的入口提供控制；
> **仅收到绑定回调，不足以把 `intercept.jni_native` 宣称为完整拦截。**

### 1.3 `DynamicCodeGenerated` —— 我的覆盖范围说法被夸大

V1.1 写的是「`defineClass`、`Unsafe` 写码、JIT 之外一切代码生成的直接观测点」。**夸大，接受收窄。**

它报告的是 **JVM 自身动态生成的原生组件代码**（如解释器相关代码段），
**不是一个监视任意可执行内存写入的探针**，也没有对应的控制能力位。

**不能并成一个观测面的四件事**：

```
Java 类定义 / 转换
Java 方法的 JIT 编译
JVM 内部 stub / 解释器代码生成      ← DynamicCodeGenerated 在这里
第三方原生代码自行分配或写入可执行内存   ← 不在这里
```

⇒ 修正后定位：**可补运行时代码区段观察；不能据此宣称 `attack.dynamic_code` 的
"目标动态代码观察已覆盖"。** 在 outlet 控制之前还有一关：
**先证明观察到的是要处理的目标代码，而不是 JVM 自己的正常产物。**

### 1.4 「48 个键无生产者」过宽 —— 局部通道存在且是故意的

V1.1 由「48 键 `supportedCeiling = OFF`」推导到「三档在 `effective` 上不可区分 ⇒ 结构上无法通过」。
**前半成立，后半过宽。**

**核实到的局部通道**（`RecoveryChainPolicy.java`）：

| 方法 | 行 | 步骤 |
|---|---|---|
| `registeredAtomicSnapshot` | `:75` | `RECORD_DISPOSAL` / `DERIVED_SOURCE` |
| `registeredNbtOutlet` | `:94` | `REBUILD_OUTLET` / `DERIVED_SOURCE` |
| `registeredTaskDisposition` | `:107` | `DIRECT_TASK` / `DESCENDANT_TASK` |
| `registeredStorageCommit` | `:110` | `SAVED_RECORD_DISPOSAL` / `RELOAD_RECONSTRUCTION` |

而 `:93` 的注释是**明说**的：

```java
/** Scopes capability evidence to the registered NBT outlet; it is never published as a global key ceiling. */
```

**"never published as a global key ceiling" —— 这是设计，不是遗漏。**

加上 `TerminalActionPolicy.java:40-43` 为当前注册实体的执行步骤构造**局部能力视图**
（`:35-36` 注释："Only this registered executor receives the measured boundary capability.
Do not advertise these broad option keys as supported for other surfaces."）。

⇒ **准确表述**：

> 当前通用投影没有为另外 48 个键提供**全局支持上限**；部分具体功能另有
> **局部授权与执行通道**。**全局键不可用，不等于所有对应的局部功能都没有执行路径。**

这些局部通道是否在运行环境中真正成功仍需运行证据；但**静态审查不能把它们抹掉**。

### 1.5 「14 个动作只接线 1 个」来自硬编码文案

V1.1 沿用了 `DominionCommands.java:280-281`：

```java
action == AttackAction.TERMINALIZE
    ? "新终结提交门"
    : "未接线（仅保存请求）"
```

**这是硬编码文案，不是对执行器的动态清点。** 它没有反映 §1.4 的三个实体局部动作。

**另一个计数问题**（外部核对提出，已核实）：`StrengthPolicySnapshot.java:185-188`：

```java
if (actions.isEmpty() || actions.equals(java.util.Set.of(AttackAction.STRONG_ATTACK))) {
    return new AttackResolution(MechanismLevel.OFF, MechanismLevel.OFF, false,
        "NO_EXECUTABLE_ACTION");
}
```

`STRONG_ATTACK` 是**聚合控制**，单独的它被判为 `NO_EXECUTABLE_ACTION`。
⇒ **不能默认"14 个动作"必须对应"14 个独立执行器"。**

`StrengthPolicyConfiguration.permitsBaselineAction()` 只接受 `TERMINALIZE` 也是事实，
但该方法的职责是**旧基线提交门**，不是所有执行器的统一注册表。

⇒ **C11 继续做，但起点应是逐动作追踪"请求 → 授权 → 执行 → 结果"的真实路径，
不是按"剩余 13 个从零接线"估算。**

### 1.6 `TerminalLifecycle` 服务器分支：我把"守卫"读成了"不可达"

V1.1（及更早）写的是「`TerminalLifecycle.java:49-62` 服务器分支整体不可达」。**错。**

`TerminalLifecycle.java:44-64` 实际是：

```java
if (entity != null && entity.level() instanceof ServerLevel level) {
    var runtime = ServerRuntimeRegistry.getIfPresent(level.getServer());
    if (runtime != null && runtime.durableStore() != null) {
        var current = runtime.durableStore().subjectQualification(...);
        if (current == null || !current.terminal()) return;        // :54 未判决 → 不走旧写入
        if (current.revisions() != null) {
            runtime.terminalPlans().execute(entity, authorityView); return;   // :56 ← 委派，可达
        }
    }
    return;    // :61 ← 守卫：不回落到无限制旧写入路径
}
enforceLegacyBody(entity, processDeathOnce, authorityView, false, () -> true);   // :63
```

- **`:56` 是可达的委派路径**，进入新计划执行器；
- **`:61` 是守卫**，注释写明理由："Server mutations always need a current Root plan.
  Missing runtime or legacy unbound qualifications cannot re-enable the old unrestricted writer."
- `:66-68` `runPlannedIntegration` → `enforceLegacyBody(..., planned=true, ...)`，
  计划路径**会**带 `planned=true` 重入旧体。

⇒ 成立的说法只有：**旧的无计划服务器写入分支不可达**（这是 `D8`「不得先移除旧可靠机制」的正确做法）。
**不能扩大成"服务器终结链整体不可达"。** 也不应为了"接通"而恢复旧的无计划写入路径。

---

## 2. 本轮新核实到的四项（双方此前都没提）

### 2.1 能力申请链缺少回读 —— 四种失败原因在运行期不可区分

`ronova_native.cpp:817-826`：

```cpp
jvmtiCapabilities potential{};
jvmtiCapabilities requested{};
g_jvmti->GetPotentialCapabilities(&potential);                    // :819 返回值未检
requested.can_retransform_classes          = potential.can_retransform_classes;          // :820
requested.can_retransform_any_class        = potential.can_retransform_any_class;        // :821
requested.can_generate_all_class_hook_events = potential.can_generate_all_class_hook_events; // :822
requested.can_access_local_variables       = potential.can_access_local_variables;       // :823
if (g_jvmti->AddCapabilities(&requested) == JVMTI_ERROR_NONE && requested.can_retransform_classes) {
    mask |= CAP_RETRANSFORM;                                                                      // :824
}
```

三点事实：

1. **`GetPotentialCapabilities` 的返回值未检查**（`:819`）；
2. **没有 `GetCapabilities()` 回读** —— 全文件仅 `:819` 一处能力查询，
   实际**获授**的能力集从未被读取；
3. `:824` 只看 `AddCapabilities` 是否返回 `JVMTI_ERROR_NONE`，
   **失败时不记录是哪一项导致的**。

**一条需以规范原文确认的推论**：若 `AddCapabilities` 采用整体失败语义
（任一项不可加则全部不加），则**一项不可用会静默带走其余三项**，
包括核心的 `can_retransform_classes`，`mask` 直接丢失 `CAP_RETRANSFORM`，
`:839-846` 的急切重转换与 `CAP_SEMANTIC_LEASE` 全部跳过。
**我无法在本机访问 JVMTI 规范原文确认这一条**（GitHub 与 Oracle 均不可达），
故标为待确认，不作为结论。

**建议的接线形状**（与外部核对一致）：

```text
GetPotentialCapabilities（检查错误）
  → AddCapabilities（逐项或整组，记录哪几项被请求）
  → GetCapabilities 回读实际获授集        ← 当前缺失
  → 只对已获授的能力注册事件 / 启用执行入口
  → 行为验证真实触达
  → 按实际范围发布支持与证据
```

### 2.2 `can_retransform_any_class` 只写不读

全原生源码中该字段**只出现一次**：`:821` 的赋值。**从未被读取、验证或使用。**

也就是说：它被申请了，但没有任何代码依赖它，也没有任何地方报告它是否到手。
`B7`（用它在外部 transform 后强制重转换）目前**没有实现**——
这印证了 V1.1 的 B7 仍是待做项，但同时也说明**它的缺失不会导致任何现有路径失效**。

### 2.3 `can_access_local_variables` 缺失已有设计好的回退

这是**正面发现**，说明原生层比 V1.1 描述的更成熟。`ronova_native.cpp:966-975`：

```cpp
jvmtiError local=(bit==8||bit==64)?g_jvmti->GetLocalObject(thread,i,...):g_jvmti->GetLocalInstance(thread,i,&receiver);
matched=local==JVMTI_ERROR_NONE&&receiver!=nullptr&&env->IsSameObject(receiver,expected);
// Live JVMTI cannot acquire can_access_local_variables. In that mode only an
// entry captured at the certified JDK callsite can establish receiver identity.
if(local==JVMTI_ERROR_MUST_POSSESS_CAPABILITY&&g_task_depth>0) {
    const auto& captured=g_task_frames[g_task_depth-1];
    TaskGuard guard;
    matched=!g_task_cycle&&(g_task_verified&bit)==bit&&captured.changes==g_task_changes
        &&captured.boundary_bit==bit&&env->IsSameObject(captured.receiver,expected);
}
```

**`MUST_POSSESS_CAPABILITY` 被显式处理**，回退到"经认证 callsite 捕获的接收者身份"，
并带四个条件（无环、已验证位、变更计数未变、边界位匹配、同一对象）。
注释自己写明这是 live 模式下的唯一可行路径。

⇒ 也就是说：**四项申请里至少有一项已经具备设计好的降级路径**，
V1.1 把它们笼统当成"四项对等申请"是不准确的。

### 2.4 `ThreadEnd` 事件启用返回值未检

`:837`：

```cpp
g_jvmti->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_THREAD_END,nullptr);
```

与 `:834-835` 的 `SetEventCallbacks` / `CLASS_FILE_LOAD_HOOK` 不同（那两处有检查并置 `CAP_HOOK`），
**`ThreadEnd` 的返回值未检查**，也没有对应的 mask 位。

`ThreadEnd` 绑定 `task_thread_end`（`:830`），属任务观察链的一部分
（`:956-981` 的接收者身份核验依赖 `g_task_depth` / `g_task_frames`）。
若该事件未启用，**任务线程退出时的清理不会执行，且无任何信号**——与本项目
`D94`（能力按实际报告）取向相反。

---

## 3. 修正后的 C13

### 3.1 瓶颈的正确描述：三层没有贯通

不是"`OFF` 默认值要改"（默认 `OFF` 在无证据时是**对的**），也不是"局部通道不存在"，而是：

| 层 | 现状 |
|---|---|
| **全局能力投影** | 只覆盖 6 个键，且只发布 `PROTECT_RONOVA` / `OFF` 两级 |
| **局部执行能力** | 已存在（`TerminalActionPolicy` 四步、`RecoveryChainPolicy` 四个 `registered*` 通道），**故意不发布为全局 ceiling** |
| **诊断口径** | `DominionCommands:280-281` 硬编码文案，**落后于局部实现**，读起来像"全部未接线" |

**三层各说各话**：投影说不可用、执行其实在做、诊断说没接线。

### 3.2 第一批交付目标

**统一登记**现有实体、NBT、任务/存储通道的**支持范围与证明**；
让**执行器、界面、命令诊断读取同一事实**——不能一边已接线、一边显示全部未接线。

### 3.3 成功标准（采用外部核对提出的版本，比我原来的好）

**不要写成"`ACTIVE` 数量增加"。** 应写成：

> **每个被抬升的能力，都说得清：支持哪个执行范围、由谁举证、
> 什么变化会使证据失效、以及哪个实际执行器消费它。**

这样既不靠改枚举值制造强度，也不会因为全局键还没成熟，
就把已经能在注册对象上工作的局部能力一并压掉。

**C13 仍为第一优先**，理由收紧为：**没有贯通的登记与回读，
`D91` 的逐键矩阵无法被诊断，`C11` 也无法判断哪些动作真的缺执行器。**

---

## 4. 五条方向的修正后定位

| # | 项 | 修正后定位 | 归属 |
|---|---|---|---|
| **新 1** | `NativeMethodBind` | 观测 **+ 可重定向**；但只覆盖 Java native 绑定事件，不补齐启用前的绑定 | 独立加强项（保留） |
| **新 2** | `DynamicCodeGenerated` | **仅** JVM 自身生成代码段；**不承担**"任意动态代码覆盖"目标 | 降为**有限观察** |
| **新 3** | `ForceEarlyReturn*` | 帧级定向提前返回；**真正价值**是重转换后已执行旧帧仍跑旧字节码这一缺口 | 独立专项（保留） |
| **新 4** | `SetNativeMethodPrefixes` | **并入 B1**，不是独立加强项；需先验证包装建立时机与已加载类限制 | **并入 B1** |
| **新 5** | `GetConstantPool` + `GetBytecodes` | 限定为**带类身份与版本上下文的结构引用证据**；并入 B2/B3 的组合分析 | **并入 B2/B3** |

**新 3 的两条新约束**（外部核对提出，我认同并补记）：

- `can_force_early_return` 在 OpenJDK 17u 属 **`onload_capabilities`** 而非 `always_capabilities`，
  **通常需早期取得**，不能假定运行后 attach 一定能申请；
- r66 原生层提供的是 `JNI_OnLoad`，**没有 `Agent_OnLoad`** ——
  **`JNI_OnLoad` 不能当作 JVMTI 的 `OnLoad` 阶段**。所以这项不只是填能力位，**还涉及启动接线**。

**新 5 的三条新约束**：

- 常量池引用需**解析**；运行时类身份由**二进制名称 + 定义它的 classloader** 共同确定，
  **同名引用 ≠ 同一运行时对象**；
- **反例**：原常量池已有某 `Methodref`，修改只让已有调用更频繁执行它，**不必新增常量池项**；
  反之，正常兼容代码新增引用也不能判为攻击来源；
- **采样一致性**：`GetConstantPool()` 的布局可能与原始 class 文件不同；
  两次读取之间发生 redefine/retransform 会**破坏它与 `GetBytecodes()` 的对应关系**。
  **不能把原始字节差分直接当作语义差分。**

---

## 5. 修正后的实施顺序

| 顺序 | 工作 | 这一批应交付什么 |
|---|---|---|
| **第一批** | **C13 基础 + C11 现有局部路径对接** | 统一登记现有实体、NBT、任务/存储通道的支持范围与证明；执行器、界面、命令诊断**读取同一事实** |
| **第二批** | **C11 + D 块真正缺失的执行接线** | 按动作追踪"请求 → 授权 → 执行 → 结果"，补缺失分支；**不重复重写已有新终结链，不恢复旧路径绕开计划** |
| **第三批** | **新 1 + 新 5/B2+B3** | 增加原生绑定与结构引用证据并接到具体功能；证明只覆盖局部时**明确局部范围** |
| **条件批次** | **B1 + 新 4** | 先验证包装建立时机、已加载类限制、具体图形入口覆盖，再决定 Render Firewall 的形态与依赖 |
| **后续专项** | **新 3；新 2 的有限观察用途** | 前者先解决**能力取得阶段**与帧级适用条件；后者**不要背上"任意动态代码攻击覆盖"的错误目标** |

**第一批不等 54 个键补齐再交付。** 先把一组已有动作的**配置、证明、实际执行、结果回读**
贯通，再逐组扩展。

**另建议把 §2.1–§2.4 的能力回读与诊断修复并入第一批** —— 它们与"三层贯通"是同一件事。

---

## 6. 本增补不做什么

- 不修改任何生产代码、构建脚本或 `validation/`。
- 不创建外部进程，不修改第三方文件，不按第三方身份定向。
- 不声称任何能力已实现或验收已通过。
- **不把"头文件里存在"当作"运行时支持"** —— 每条能力位都要先探测。
- 不把 V1.1 的任何结论当作已核事实；本版以逐条复核为准。

---

## 附：本版未解决的两项

| 项 | 状态 |
|---|---|
| `AddCapabilities` 是否整体失败语义 | **未确认**。本机无法访问 JVMTI 规范原文（GitHub / Oracle 均不可达）。若为整体失败，§2.1 的静默丢失风险成立；若为逐项，则风险降为"无回读、无诊断" |
| `DynamicCodeGenerated` 在 HotSpot 17 的准确触发范围 | **按外部核对的收窄执行**，未独立复现。定位已从"JVM 外一切代码生成"降为"JVM 自身生成代码段" |

<!-- RONOVA-HANDOFF-ORIGINAL-END ADD12 -->
