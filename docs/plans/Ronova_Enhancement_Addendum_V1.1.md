> **fix5 来源保留页（2026-09-14）：已被 V1.2 取代，仅作历史对照。** 当前实施入口是[能力接线规划的原生申请章节](R66-CAPABILITY-WIRING-PLAN.md#native-negotiation)，逐节处置见[融合映射](RESTRUCTURE-MAP.md#fix5-addendum-merge)。
>
> 下方是收到的原文，保留其当时的结论、编号与“待确认”状态；它们不冒充本轮重新执行的结果。`can_retransform_any_class` 字面计数的推论收窄及 `AddCapabilities` 的外部核对见当前入口。**V1.1 的旧机制误读不恢复效力，V1.2 的重复部分不再另算一批工作。** 本页原文之外的横幅才是融合状态说明。

<a id="handoff-original"></a>
<!-- RONOVA-HANDOFF-ORIGINAL-BEGIN ADD11 -->
# Ronova 加强方案 · 增补 V1.1

> **已被取代（2026-09-14）**：本版有五处机制层误读，见
> [`Ronova_Enhancement_Addendum_V1.2.md`](Ronova_Enhancement_Addendum_V1.2.md) §0 修正摘要。
> 主要错误：`SetNativeMethodPrefixes` 被误读为"按 classloader"；
> `NativeMethodBind` 被误读为纯观测；`DynamicCodeGenerated` 覆盖范围被夸大；
> 「48 键无生产者 ⇒ 功能不可达」过宽（局部通道存在且故意不发布）；
> 「14 个动作只接线 1 个」沿用硬编码文案；`TerminalLifecycle` 把守卫读成不可达。
> **本文件保留作对照，不再作为加强入口。**

日期：2026-09-14。性质：**设计增补**。未修改任何生产代码，未构建，未启动游戏。

本文件是 [`docs/plans/`](README.md) MS 三书规划的**增量补充**，只处理三件事：

| 项 | 内容 | 性质 |
|---|---|---|
| **A** | **撤回一条错误结论** | 事实更正 |
| **B** | 五条此前未提出的加强方向 | 新提案 |
| **C** | `C13`（天花板管道）的准确表述 | 修正我自己的判断 |

**边界**：三条铁律（不改依赖/Mod/Forge 文件、不针对、不删对方文件）+ 不起外部进程。
本增补的全部内容均在该边界内；不含任何文件修改、进程派生或第三方定向。

**证据口径**：`jvmti.h` 行号取自本机 JDK 17 头文件
`D:\桌面\MOD\.build-tools\jdk17-headers\jvmti.h`（逐行实读）。
源码行号取自工作区当前源码。**头文件里存在 ≠ HotSpot 运行时支持**——按本方案自己的
「探测 → 验证 → 失败即拒绝」纪律，每条能力位都必须先探 `GetPotentialCapabilities`。

---

## A. 撤回：`can_generate_native_method_bind_events` **存在**

### A.1 我此前的错误结论

早先的方案里写过：

> **不得申请** `can_generate_native_method_bindings`——JDK 17 的 `jvmti.h` 中不存在
> （JDK 8 有，9 已移除）。

**这条是错的，现予撤回。**

### A.2 实测证据

```
jvmti.h:700   unsigned int can_generate_native_method_bind_events : 1;
jvmti.h:415   JVMTI_EVENT_NATIVE_METHOD_BIND = 67,
jvmti.h:875   typedef void (JNICALL *jvmtiEventNativeMethodBind) (...)
jvmti.h:978   jvmtiEventNativeMethodBind NativeMethodBind;      ← 回调槽位
```

### A.3 错在哪

我把字段名记成了 `can_generate_native_method_bindings`（多了 `ings`、少了 `_events`）。
拿一个**不存在的名字**去检索，查不到，就当成"能力不存在"。

这是**用检索结果替代了事实核查**。字段名的正确形式是
`can_generate_native_method_bind_events`，它在 JDK 17 中存在。

### A.4 为什么这条撤回重要

不撤回的话，后续任何人读到它都会**主动排除一个真实存在的能力位**。而 `NativeMethodBind`
正是 `INTERCEPT_JNI_NATIVE` 这个键缺的观测面——见 B.1。

---

## B. 五条新加强方向

全部经 JDK 17 头文件核实。当前 Ronova 只申请 **4 项**能力位
（`native/src/ronova_native.cpp:820-823`）：

```
can_retransform_classes · can_retransform_any_class
can_generate_all_class_hook_events · can_access_local_variables
```

### B.1 `NativeMethodBind` —— 补 `intercept.jni_native` 的观测缺口

| 项 | 值 |
|---|---|
| 能力位 | `can_generate_native_method_bind_events`（`:700`） |
| 事件 | `JVMTI_EVENT_NATIVE_METHOD_BIND = 67`（`:415`） |
| 回调 | `jvmtiEventNativeMethodBind`（`:875`），槽位 `:978` |

**能做什么**：观察**每一次原生方法绑定**，包括对手用 `RegisterNatives` 动态注册的原生方法。

**为什么现在做不到**：`INTERCEPT_JNI_NATIVE` 这个键对应的现有路径是 Java 侧
`SemanticGateVerifier` / `NativeTransformBridge`，走的是 ASM 类文件回调——
**看不到原生绑定事件**。

**边界**：这是**观测**能力。要满足 `V2.6:2832` 的 must-reject
（"全部严格只能观察却声称 DENIED"），观测之外仍需 gate；本项只解决"看不到"。

**必须探测**：HotSpot 对 `NativeMethodBind` 的支持是可选的。先探，不支持则如实报 `UNAVAILABLE`。

### B.2 `DynamicCodeGenerated` —— 直接命中 `attack.dynamic_code`

| 项 | 值 |
|---|---|
| 事件 | `JVMTI_EVENT_DYNAMIC_CODE_GENERATED = 70`（`:418`） |
| 回调 | `jvmtiEventDynamicCodeGenerated`（`:776-780`），槽位 `:984` |
| 回调签名 | `(jvmtiEnv*, const char* name, const void* address, jint length)` |

**能做什么**：拿到**动态生成代码的名字、地址、长度**——`defineClass`、`Unsafe` 写码、
JIT 之外一切代码生成的直接观测点。

**为什么重要**：`V2.6:2836` 的 must-reject 有一条：

> 动态代码 Extreme **只有观察**，没有 dispatch/result outlet 控制，却显示已完成。

这个事件把"观测"从没有变成有。**但要过关还得补 outlet 控制**（见 B.4），本项单独不够。

**与 B.4 的关系**：`can_generate_compiled_method_load_events`（`:697`）覆盖 JIT 编译的
`CompiledMethodLoad/Unload`；`DynamicCodeGenerated` 覆盖**非 JIT 的**内存代码生成。
两者是不同集合，不能互相替代。

### B.3 `ForceEarlyReturn*` —— 不依赖字节码所有权的定向提前返回

| 项 | 值 |
|---|---|
| 能力位 | `can_force_early_return`（`:703`） |
| API | `ForceEarlyReturn{Object,Int,Long,Float,Double,Void}`（`:1457-1482`） |

**能做什么**：在回调上下文里对**指定线程的当前栈帧**强制提前返回。

**为什么有价值**：现在的 AllReturn 类能力走 `BootstrapHooks` 谓词槽 + 字节码改写——
**依赖你拥有那块字节码**。而 `skills:279` 指出 Mixin 层可被对手的转换服务直接删除、
且"修改过一次无法二次修改"。`ForceEarlyReturn` 提供一条**帧级、不改字节码**的路径。

**必须说清的限制**：它是**逐线程当前帧**操作（需要一个正处于目标帧的线程），
**不是批量手段，不能替代 AllReturn**。定位是**定向**能力。

### B.4 `SetNativeMethodPrefixes` —— 可能让 Render Firewall 不需要额外 DLL

| 项 | 值 |
|---|---|
| 能力位 | `can_set_native_method_prefix`（`:706`） |
| API | `SetNativeMethodPrefix`（`:1417`）· **`SetNativeMethodPrefixes`（`:1421`，复数、按 classloader）** |

**能做什么**：设前缀后 JVM 去找 `prefix + 原方法名` 的原生符号——
**纯 Java 给任意原生方法挂影子实现**，包括 LWJGL 图形栈入口。

**为什么是新方向**：`BL:1939-1953` 把 **Native Render Firewall 列为可选 P1 客户端扩展**，
`BL:1965` 明确「**不是 FULL L6 Server 必需条件**」。按 `skills:394` 一系思路，
这类能力通常要做进原生 DLL。**B.1–B.4 的完整形态有可能让它不需要额外 DLL。**

**一鱼两吃**：
- 服务 `UX1`（少一个原生依赖）；
- 服务 `skills:358`（原生层不应是业务规则的第二实现）——影子实现写在 Java 侧。

**边界**：`SetNativeMethodPrefixes` 只改**符号解析**，不改原生代码本身。
它不是"改写对手的原生函数"，是"给自己的 Java 实现挂上对应符号"。

### B.5 `GetConstantPool` + `GetBytecodes` → 常量池级归属证据

| 项 | 值 |
|---|---|
| API | `GetBytecodes`（`:1426`）· `GetConstantPool`（`:1775`） |

**能做什么**：取**方法实际生效的字节码**与**类完整常量池**。

**关键用途——常量池引用差分**：对手即使把方法体形状改得一模一样，
只要它**新引用了你的类或方法**，常量池就会变。

**为什么正中要害**：`V2.6:1171` 给 `MutationTrace` 划的线是：

> 不能因为**字符串、包名或同父类**就独立判定目标归属。

常量池引用是**结构性证据**，不是字符串猜测。这正是 `E1 候选一等公民` 与
`E2 MutationTrace` 缺的那个归属证据来源。

**与现有实现的关系**：现有语义证明在 Java 侧用 ASM 做（`SemanticGateVerifier`）。
JVMTI 侧拿到的是**生效后**的字节码与常量池，不经过 ASM 的读取路径。

---

## C. `C13`（天花板管道）的准确表述 —— 修正我自己的判断

### C.1 我先前的说法不准确

我此前说 54 个键里有 48 个"**永久 UNAVAILABLE**"、"**假死**"。

**"假死"这个词是错的。** 逐行核实后，现状是**故意的、且有注释说明的设计**。

### C.2 实际机制（逐行核实）

**组合规则**（`StrengthPolicySnapshot.java:217-242`）：

```java
MechanismLevel effective = MechanismLevel.lowerOf(
    MechanismLevel.lowerOf(activeRequested, supportedCeiling),
    evidenceCeiling
);
```

**默认天花板为 `OFF`**（`StrengthPolicySnapshot.java:99`）：

```java
this.supportedCeilings = levels(supportedCeilings, MechanismLevel.OFF);
```

**`OFF` 天花板直接导致 `UNAVAILABLE`**（`StrengthPolicySnapshot.java:383-387`）：

```java
if (supportedCeiling == MechanismLevel.OFF
    || evidenceCeiling == MechanismLevel.OFF
    || evidence == EvidenceState.UNOBSERVED) {
    return ActivationState.UNAVAILABLE;
}
```

**只有 6 个键有生产者**（`RuntimeCapabilityProjection.java:11-22`）：

| # | 键 | 来源 |
|---|---|---|
| 1 | `PROTECT_CODE_INTEGRITY` | `code` 证明 |
| 2 | `RUNTIME_CRITICAL_CLASS_WATCH` | `code` 证明 |
| 3 | `INTERCEPT_INSTRUMENTATION` | `code` 证明 |
| 4 | `RUNTIME_JVMTI` | `enforcement` 证明 |
| 5 | `RUNTIME_NATIVE` | `enforcement` 证明 |
| 6 | `RUNTIME_BACKGROUND_INTEGRITY` | `enforcement` 证明 |

`TerminalActionPolicy.java:40-43` 是**另一个** `withCapability` 调用点，它只覆盖
`LIFE`/`REMOVAL`/`REMOVED_RECORDS`/`BYPASS_RECORDS` 四步所需的键
（`ATTACK_STATE_SOURCE`、`RUNTIME_NATIVE`、`RUNTIME_JVMTI`、
`RUNTIME_CRITICAL_CLASS_WATCH`、`PROTECT_CODE_INTEGRITY`）。

### C.3 它是故意的 —— 代码里写着原因

`TerminalActionPolicy.java:35-36`：

```java
// Only this registered executor receives the measured boundary capability.
// Do not advertise these broad option keys as supported for other surfaces.
```

`TerminalActionPolicy.java:46-47`：

```java
// These executors act on one registered entity and its exact index references.
// Selecting Extreme does not claim producer, native or arbitrary resource coverage.
```

**这两段注释是正确的做法**——符合 `D94`「能力必须按实际报告」与
`V2.6:2699`「只对已经真正具备 executor + evidence + repair + test 的选项显示 ACTIVE」。

### C.4 那真正的缺口是什么

**缺口不是"报告不实"，是"还没有生产者"。**

`D91` 要求三个预设**从 7.17 起逐键解析**，`V2.6:877` 要求
**同一键的三档必须命中不同生产策略**。而当前 48 个键的
`supportedCeiling` 恒为 `OFF` ⇒ 三档解析结果**在 `effective` 上不可区分**。

**推论**：只要没有生产者，`V2.6` §24 #44 与 X51（"54 keys × 三档命中各自真实策略"）
**在结构上无法通过**——不是因为实现不够，是因为**没有可差异化的输入**。

### C.5 所以 C13 的正确表述

| 旧表述（我的） | 新表述（修正后） |
|---|---|
| 48 键"永久 UNAVAILABLE / 假死"，是缺陷 | 48 键**尚无 capability producer**；当前 `UNAVAILABLE` 报告是**正确的** |
| 改 `withCapability` 调用点即可 | 需要**为每个键（或每组键）建立"实测支持 → ceiling"的来源** |
| 属"把已写好的接通" | 属**新增生产者**；工作量大于我先前估计 |

**C13 的正确内容**：为 48 个键补 capability producers，使每个键的
`supportedCeiling` 来自**实测**而非默认 `OFF`。补完之后 `D91` 的逐键矩阵才有可差异化的输入。

**这仍然是第一优先项**，但理由变了：不是"接通已写好的"，而是
**"没有它，逐键矩阵与三档差异性在结构上不可达"**。

---

## D. 建议顺序

| 序 | 项 | 理由 |
|---|---|---|
| 1 | **A（撤回错误结论）** | 零成本；不撤回会持续排除一个真实能力 |
| 2 | **C（补 48 键的 capability producers）** | 卡住 `D91` / `V2.6` §24 #44 / X51；是其他一切的前置 |
| 3 | **B.4（`SetNativeMethodPrefixes`）** | 纯增量，且可能同时减少原生依赖与 `skills:358` 风险 |
| 4 | **B.1 / B.2（`NativeMethodBind` / `DynamicCodeGenerated`）** | 补两个观测缺口 |
| 5 | **B.5（常量池归属证据）** | 服务 `E1` / `E2` |
| 6 | **B.3（`ForceEarlyReturn*`）** | 定向能力，价值明确但适用面窄 |

---

## 附：本增补不做什么

- 不修改任何生产代码、构建脚本或 `validation/`。
- 不创建外部进程，不修改第三方文件，不按第三方身份定向。
- 不声称任何能力已实现或验收已通过。
- 不把"头文件里存在"当作"运行时支持"——每条能力位都要先探测。
- 不修改 `docs/plans/` 下 MS 三书的任何迁移段落。

<!-- RONOVA-HANDOFF-ORIGINAL-END ADD11 -->
